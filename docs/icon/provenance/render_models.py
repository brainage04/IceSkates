"""Headless-only scene authoring. Run an image wrapper with Blender --background -noaudio --python.
All model textures are byte-identical repository snapshots. Current generated-item topology
and ModelPart atlas UVs follow the Minecraft 26.2 client. Improved geometry is a local proposal.
"""
from pathlib import Path
import bpy, bmesh, json, math, time, hashlib, os
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view
ROOT=Path(__file__).resolve().parent
PROVENANCE=json.loads((ROOT/'provenance.json').read_text())
PIXELS=json.loads((ROOT/'source-pixels.json').read_text())
ICE='common__src__main__resources__assets__ice_skates__textures__item__'
CARPET='common__src__main__resources__assets__magic_carpet__textures__entity__'
MATERIALS={}
OBJECTS=[]
PARTS=[]

def linear(c):
    return c/12.92 if c<=.04045 else ((c+.055)/1.055)**2.4

def tex_material(filename,tint=(1,1,1),metallic=0,roughness=.63):
    key=(filename,tint,metallic,roughness)
    if key in MATERIALS: return MATERIALS[key]
    mat=bpy.data.materials.new(filename+str(tint)); mat.use_nodes=True
    nodes=mat.node_tree.nodes; links=mat.node_tree.links
    bs=nodes.get('Principled BSDF'); bs.inputs['Roughness'].default_value=roughness
    bs.inputs['Metallic'].default_value=metallic
    im=bpy.data.images.load(str(ROOT/'textures'/filename),check_existing=True); im.pack()
    tex=nodes.new('ShaderNodeTexImage'); tex.image=im; tex.interpolation='Closest'; tex.extension='EXTEND'
    multiply=nodes.new('ShaderNodeMixRGB'); multiply.blend_type='MULTIPLY'; multiply.inputs[0].default_value=1
    multiply.inputs[2].default_value=(*[linear(v) for v in tint],1)
    links.new(tex.outputs['Color'],multiply.inputs[1]); links.new(multiply.outputs[0],bs.inputs['Base Color'])
    links.new(tex.outputs['Alpha'],bs.inputs['Alpha'])
    MATERIALS[key]=mat
    return mat

def layered_item_material(prefix,tint):
    mat=tex_material(ICE+prefix+'.png',tint).copy()
    mat.name=prefix+' vanilla layer alpha composition'
    nodes=mat.node_tree.nodes; links=mat.node_tree.links
    bs=nodes.get('Principled BSDF'); body=nodes.get('Image Texture')
    # Canonicalize vanilla's coincident raster layers into one alpha-over surface.
    # The visible plane locations, UVs, texels and default dye stay unchanged.
    tinted=next(n for n in nodes if n.type=='MIX_RGB')
    overlay=nodes.new('ShaderNodeTexImage')
    overlay.image=bpy.data.images.load(str(ROOT/'textures'/(ICE+prefix+'_overlay.png')),check_existing=True)
    overlay.image.pack(); overlay.interpolation='Closest'
    mix=nodes.new('ShaderNodeMixRGB')
    links.new(overlay.outputs['Alpha'],mix.inputs[0])
    links.new(tinted.outputs[0],mix.inputs[1]); links.new(overlay.outputs['Color'],mix.inputs[2])
    links.new(mix.outputs[0],bs.inputs['Base Color'])
    alpha=nodes.new('ShaderNodeMath'); alpha.operation='MAXIMUM'
    links.new(body.outputs['Alpha'],alpha.inputs[0]); links.new(overlay.outputs['Alpha'],alpha.inputs[1])
    links.new(alpha.outputs[0],bs.inputs['Alpha'])
    return mat

def flat_material(name,rgb):
    mat=bpy.data.materials.new(name); mat.diffuse_color=(*rgb,1); mat.use_nodes=True
    bs=mat.node_tree.nodes.get('Principled BSDF'); bs.inputs['Base Color'].default_value=(*rgb,1)
    bs.inputs['Roughness'].default_value=.85
    return mat

def mesh(name,verts,faces,uvs,mat,role):
    me=bpy.data.meshes.new(name); me.from_pydata(verts,[],faces); me.update()
    ob=bpy.data.objects.new(name,me); bpy.context.collection.objects.link(ob)
    me.materials.append(mat)
    layer=me.uv_layers.new(name='Minecraft UV')
    for poly,uv in zip(me.polygons,uvs):
        for idx,coord in zip(poly.loop_indices,uv): layer.data[idx].uv=coord
    # Recalculate normals while preserving loop UVs.
    bm=bmesh.new(); bm.from_mesh(me); bmesh.ops.recalc_face_normals(bm,faces=bm.faces); bm.to_mesh(me); bm.free()
    ob['role']=role; OBJECTS.append(ob)
    return ob

def quad_uv(rect,w=16,h=16):
    a,b,c,d=rect
    return [(a/w,1-d/h),(c/w,1-d/h),(c/w,1-b/h),(a/w,1-b/h)]

def box(name,low,high,mat,rect,root=None,role='improved'):
    x,y,z=low; X,Y,Z=high
    verts=[(x,y,z),(X,y,z),(X,Y,z),(x,Y,z),(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)]
    faces=[(0,3,2,1),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7),(4,5,6,7)]
    ob=mesh(name,verts,faces,[quad_uv(rect)]*6,mat,role)
    if root: ob.parent=root
    PARTS.append({'name':name,'from':list(low),'to':list(high),'texture':mat.node_tree.nodes.get('Image Texture').image.name,'uv_crop_top_left_pixels':rect,'parent':root.name if root else None})
    return ob

def current_item(kind):
    prefix=kind.replace('-','_')
    definition_file=next(ROOT/r['snapshot'] for r in PROVENANCE['sources'] if r['source'].endswith('/items/'+prefix+'.json'))
    item_definition=json.loads(definition_file.read_text())['model']['fallback']
    tint_integer=item_definition['tints'][0]['default'] & 0xFFFFFF
    tint=tuple(((tint_integer>>shift)&255)/255 for shift in [16,8,0])
    combined=layered_item_material(prefix,tint)
    record={'geometry':'Minecraft 26.2 generated item; alpha-cut front/back and exposed pixel-boundary side faces','bounds_model_units':[[0,0,7.5],[16,16,8.5]],'default_layer0_tint':f'#{tint_integer:06X}','item_definition_snapshot':str(definition_file),'layers':[]}
    for layer,suffix in enumerate(['','_overlay']):
        file=ICE+prefix+suffix+'.png'; rows=PIXELS[file]['rgba']; w,h=PIXELS[file]['size']
        mat=tex_material(file,tint if layer==0 else (1,1,1))
        verts=[]; faces=[]; uvs=[]
        def face(v,u):
            start=len(verts); verts.extend(v); faces.append(tuple(range(start,start+4))); uvs.append(u)
        # Coincident raster front/back pairs collapse to the same geometric surface.
        # This avoids coplanar self-shadowing in the path tracer; no depth offset.
        if layer==0:
            face([(-8,-.5,0),(8,-.5,0),(8,-.5,16),(-8,-.5,16)],quad_uv([0,0,16,16]))
            face([(8,.5,0),(-8,.5,0),(-8,.5,16),(8,.5,16)],[(0,0),(1,0),(1,1),(0,1)])
        count=0; opaque=0
        for y in range(h):
            for x in range(w):
                if rows[y][x][3]==0: continue
                opaque+=1; x0=x-8; x1=x0+1; z1=16-y; z0=z1-1
                uv=quad_uv([x+.1,y+.1,x+.9,y+.9])
                if x==0 or rows[y][x-1][3]==0:
                    face([(x0,.5,z0),(x0,-.5,z0),(x0,-.5,z1),(x0,.5,z1)],uv); count+=1
                if x==w-1 or rows[y][x+1][3]==0:
                    face([(x1,-.5,z0),(x1,.5,z0),(x1,.5,z1),(x1,-.5,z1)],uv); count+=1
                if y==0 or rows[y-1][x][3]==0:
                    face([(x0,-.5,z1),(x1,-.5,z1),(x1,.5,z1),(x0,.5,z1)],uv); count+=1
                if y==h-1 or rows[y+1][x][3]==0:
                    face([(x0,.5,z0),(x1,.5,z0),(x1,-.5,z0),(x0,-.5,z0)],uv); count+=1
        ob=mesh(f'{kind}-layer{layer}',verts,faces,uvs,mat,'current-generated-item')
        if layer==0:
            ob.data.materials.append(combined)
            ob.data.polygons[0].material_index=1
            ob.data.polygons[1].material_index=1
        record['layers'].append({'texture':file,'opaque_pixels':opaque,'vanilla_front_back_planes':2,'canonical_front_back_planes':2 if layer==0 else 0,'boundary_side_quads':count,'total_faces':len(faces)})
        assert len(ob.data.polygons)==count+(2 if layer==0 else 0)
        assert abs(ob.dimensions.y-1)<1e-6
    record['shape_change']='None: alpha silhouette, side geometry and one-unit thickness are unchanged. Duplicate coincident raster faces are canonicalized for Cycles.'
    record['layer_composition']='One canonical front/back pair uses alpha-over to reproduce the original layer ordering without z offsets, recolouring or silhouette changes.'
    return record

def improved_item(kind,candidate):
    roller=kind=='roller-skates'; prefix=kind.replace('-','_')
    body=tex_material(ICE+prefix+'.png',(.84,.88,.94))
    dark=tex_material(ICE+'ice_skate_blades.png',(.64,.72,.85))
    white=tex_material(ICE+prefix+'.png',(1,1,1))
    accent_color=(.18,.82,.95) if not roller else (1,.52,.16)
    accent=tex_material(ICE+prefix+'.png',accent_color)
    hardware=tex_material(ICE+('roller_skate_wheels.png' if roller else 'ice_skate_blades.png'),metallic=.45,roughness=.32)
    tire=tex_material(ICE+'roller_skate_wheels.png',(.7,.76,.84))
    bright=tex_material(ICE+prefix+'_overlay.png',metallic=.3,roughness=.32)
    changes=[
        'Replace vanilla one-model-unit-thick paired sprite with two independently posed volumetric boots; proposal only, no source-mod changes.',
        'Use a 6 x 12 x 1 sole and a 5 x 11 boot upper; toe and instep top heights are respectively 3.1 and 4.1 above the sole base. Adjacent cuboids share boundaries without overlapping exterior faces.',
        'Build a real open cuff from four one-unit walls around a 3 x 3 opening; cavity floor is 3 units below the cuff wall tops and 3.45 below the 0.45-unit white rim top.',
        'Use two 0.65-unit-thick instep straps at y=-3.6 and y=-1.2, a 0.8-unit ankle band, and two small square metallic buckles per boot.',
        'Retain the mod grayscale leather texture: remap opaque body pixels [4,3,6,7] across boot panels; rim and straps sample [4,3,6,5]. Closest filtering, no painted texture.',
        'Define pale slate body tint #D6E0F0, graphite sole tint #A3B8D9, and '+('cyan #2ED1F2' if not roller else 'amber #FF8529')+' strap accents on real grayscale textures; preserve silver/white hardware. This replaces the current default '+('#191919' if not roller else '#E6E6E6')+' only in the new proposal.'
    ]
    if roller:
        changes += ['Separate four quad wheels per boot into two axles at y=-3.8,+3.5. Each wheel is a sharp stepped 3 x 3 voxel profile, 1.1 units wide, with a 0.9-unit square silver hub.', 'Lift the sole to z=3.25 over wheel centers z=1.5; leave 0.25-unit wheel/sole clearance. Add one compact front toe stop centered at y=-5.3. Keep wheels graphite using the real roller_skate_wheels texture, rather than turning them into blades.']
    else:
        changes += ['Replace solid white sprite undercarriage with a thin central silver runner 0.8 units wide, 12 units long, and 0.65 units high; ends step upward by 0.55 units rather than a thick wedge.', 'Use exactly two short blade supports per boot, y=-3.6,+3.4, connected to the sole. Preserve visible air between blade and sole; blade height is under one-tenth of boot upper height.']
    for side,(px,py,angle) in enumerate([(-4.0,-1.0,-7),(4.0,2.4,8)] if candidate==1 else [(-4.1,1.6,-11),(4.1,-1.2,6)]):
        parent=bpy.data.objects.new(f'{prefix}-boot-{side+1}',None); bpy.context.collection.objects.link(parent)
        parent.location=(px,py,0); parent.rotation_euler.z=math.radians(angle)
        def b(n,lo,hi,m=body,uv=(4.05,3.05,5.95,6.95)):
            return box(f'boot{side+1}-{n}',lo,hi,m,uv,parent)
        base=3.25 if roller else 2.4
        b('sole',(-3,-6,base),(3,6,base+1),dark,(4.1,4.1,7.9,5.9))
        b('toe',(-2.5,-5.5,base+1),(2.5,-3.6,base+3.1))
        b('instep',(-2.5,-3.6,base+1),(2.5,.5,base+4.1))
        b('heel',(-2.5,.5,base+1),(2.5,5.5,base+5.8))
        b('cuff-left',(-2.5,.5,base+5.8),(-1.5,5.5,base+9.4))
        b('cuff-right',(1.5,.5,base+5.8),(2.5,5.5,base+9.4))
        b('cuff-front',(-1.5,.5,base+5.8),(1.5,1.5,base+9.4))
        b('cuff-back',(-1.5,4.5,base+5.8),(1.5,5.5,base+9.4))
        b('cuff-floor',(-1.5,1.5,base+5.8),(1.5,4.5,base+6.4),dark,(5.1,4.1,7.9,5.9))
        # Four thin rim strips maintain the opening.
        for n,lo,hi in [('rim-left',(-2.65,.35,base+9.4),(-1.45,5.65,base+9.85)),('rim-right',(1.45,.35,base+9.4),(2.65,5.65,base+9.85)),('rim-front',(-1.45,.35,base+9.4),(1.45,1.55,base+9.85)),('rim-back',(-1.45,4.45,base+9.4),(1.45,5.65,base+9.85))]:
            b(n,lo,hi,white,(4.1,3.1,5.9,4.9))
        for i,(y,z) in enumerate([(-3.6,base+4.1),(-1.2,base+4.1)]):
            b(f'strap-{i}',(-2.7,y,z),(2.7,y+.8,z+.65),accent,(4.1,3.1,5.9,4.9))
            b(f'buckle-{i}',(1.2,y-.1,z+.65),(2.2,y+.9,z+.95),hardware,(7.1,6.1,8.9,7.9))
        b('ankle-band-front',(-2.7,.3,base+7.8),(2.7,.55,base+8.6),accent,(4.1,3.1,5.9,4.9))
        b('ankle-band-left',(-2.7,.55,base+7.8),(-2.45,5.45,base+8.6),accent,(4.1,3.1,5.9,4.9))
        b('ankle-band-right',(2.45,.55,base+7.8),(2.7,5.45,base+8.6),accent,(4.1,3.1,5.9,4.9))
        b('ankle-band-back',(-2.7,5.45,base+7.8),(2.7,5.7,base+8.6),accent,(4.1,3.1,5.9,4.9))
        if roller:
            for axle,y in enumerate([-3.8,3.5]):
                b(f'axle-{axle}',(-3.4,y-.35,1.15),(3.4,y+.35,1.85),hardware,(7.1,6.1,8.9,7.9))
                for sign in [-1,1]:
                    x=sign*3.05
                    b(f'wheel-{axle}-{sign}-front',(x-.55,y-1.5,.75),(x+.55,y-.75,2.25),tire,(5.1,4.1,6.9,5.9))
                    b(f'wheel-{axle}-{sign}-back',(x-.55,y+.75,.75),(x+.55,y+1.5,2.25),tire,(5.1,4.1,6.9,5.9))
                    b(f'wheel-{axle}-{sign}-vertical',(x-.55,y-.75,0),(x+.55,y+.75,3),tire,(5.1,4.1,6.9,5.9))
                    b(f'hub-{axle}-{sign}',(x-.65,y-.45,1.05),(x+.65,y+.45,1.95),hardware,(7.1,6.1,8.9,7.9))
            b('toe-stop',(-.65,-5.85,1.8),(.65,-4.7,3.25),tire,(5.1,4.1,6.9,5.9))
        else:
            for i,y in enumerate([-3.6,3.4]):
                b(f'blade-support-{i}',(-.65,y-.55,.65),(.65,y+.55,base),hardware,(4.1,8.1,6.9,8.9))
            b('blade-runner',(-.4,-6,0),(.4,6,.65),bright,(1.1,12.1,4.9,13.9))
            b('blade-toe-rise',(-.4,-6.6,.55),(.4,-5.8,1.3),bright,(1.1,12.1,4.9,13.9))
            b('blade-heel-rise',(-.4,5.8,.55),(.4,6.4,1.15),bright,(1.1,12.1,4.9,13.9))
    return {'geometry':'New local volumetric voxel proposal, authored in render_models.py; exact per-cuboid definition in parts below','changes':changes,'boots':2,'wheels':8 if roller else 0,'blade_runners':0 if roller else 2,'parts':PARTS.copy()}

def carpet_cube(name,x,y,z,dx,dy,dz,u,v,mat,height):
    # ModelPart.Cube vertex/UV layout (unmirrored), preserving the repository's addBox values.
    vertices=[(x,y,z),(x+dx,y,z),(x+dx,y+dy,z),(x,y+dy,z),(x,y,z+dz),(x+dx,y,z+dz),(x+dx,y+dy,z+dz),(x,y+dy,z+dz)]
    faces=[(5,4,0,1),(2,3,7,6),(0,4,7,3),(1,0,3,2),(5,1,2,6),(4,5,6,7)]
    rects=[(u+dz,v,u+dz+dx,v+dz),(u+dz+dx,v+dz,u+dz+2*dx,v),(u,v+dz,u+dz,v+dz+dy),(u+dz,v+dz,u+dz+dx,v+dz+dy),(u+dz+dx,v+dz,u+2*dz+dx,v+dz+dy),(u+2*dz+dx,v+dz,u+2*dz+2*dx,v+dz+dy)]
    uvs=[]
    for a,b,c,d in rects:
        uvs.append([(c/64,1-b/14),(a/64,1-b/14),(a/64,1-d/14),(c/64,1-d/14)])
    # Neutralize the model's +24 pivot and renderer -1.5 block translation; both cancel.
    ob=mesh(name,[(X,Z,Y+height) for X,Y,Z in vertices],faces,uvs,mat,'carpet-tier')
    ob['tier']=name.split('-')[0]
    return ob

def carpets(candidate):
    model=next(ROOT/r['snapshot'] for r in PROVENANCE['sources'] if r['source'].endswith('MagicCarpetEntityModel.java'))
    import re
    pattern=r'texOffs\((\d+), (\d+)\)\.addBox\(([-\d.F, ]+), new CubeDeformation\(0.0F\)\)'
    definitions=[]
    for m in re.finditer(pattern,model.read_text()):
        u,v=map(int,m.group(1,2)); dims=[float(n.strip().replace('F','')) for n in m.group(3).split(',')]
        definitions.append((u,v,*dims))
    assert len(definitions)==5, definitions
    records=[]
    for i,tier in enumerate(['basic','advanced','legendary']):
        mat=tex_material(CARPET+tier+'_magic_carpet.png',roughness=.9)
        height=i*19
        for j,(u,v,x,y,z,dx,dy,dz) in enumerate(definitions):
            carpet_cube(f'{tier}-carpet-box-{j}',x,y,z,dx,dy,dz,u,v,mat,height)
        records.append({'tier':tier,'height':height,'box_count':5,'bounds':[[-12,-16,height],[12,16,height+1]],'texture':CARPET+tier+'_magic_carpet.png'})
    assert len(OBJECTS)==15
    return {'geometry_source':str(model),'exact_boxes_uv_origin_and_xyzwhd':definitions,'model_layer_size':[64,14],'tiers':records,'composition':'Vertical exploded stack, basic bottom, advanced middle, legendary top, identical centered footprint and no geometry alteration; 18-unit clear air between 1-unit-thick tiers.'}

def add_area(name,loc,power,size,color,target):
    data=bpy.data.lights.new(name,'AREA'); data.energy=power; data.shape='DISK'; data.size=size; data.color=color
    ob=bpy.data.objects.new(name,data); bpy.context.collection.objects.link(ob); ob.location=loc
    ob.rotation_euler=(Vector(target)-ob.location).to_track_quat('-Z','Y').to_euler()

def configure(candidate,is_carpet,is_current):
    scene=bpy.context.scene; scene.render.engine='CYCLES'; scene.cycles.device='CPU'; scene.cycles.samples=64
    scene.cycles.use_denoising=True; scene.cycles.seed=20260912+candidate
    scene.cycles.max_bounces=6; scene.cycles.transparent_max_bounces=12
    scene.render.threads_mode='FIXED'; scene.render.threads=6
    scene.render.resolution_x=1024; scene.render.resolution_y=1024; scene.render.resolution_percentage=100
    scene.render.image_settings.file_format='PNG'; scene.render.image_settings.color_mode='RGBA'; scene.render.image_settings.color_depth='8'
    scene.render.film_transparent=False
    scene.world.use_nodes=True; scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.64,.7,.8,1)
    scene.world.node_tree.nodes['Background'].inputs[1].default_value=.65
    scene.view_settings.view_transform='Standard'; scene.view_settings.look='None'; scene.view_settings.exposure=0; scene.view_settings.gamma=1
    floor_color=(.63,.72,.8) if candidate==1 else (.79,.72,.62)
    ground=flat_material('Studio floor (not model geometry)',floor_color)
    bpy.ops.mesh.primitive_plane_add(size=1000,location=(0,0,-.08 if not is_current else .88))
    bpy.context.object.name='Studio ground'; bpy.context.object.data.materials.append(ground)
    camera_data=bpy.data.cameras.new('Isometric orthographic camera'); camera_data.type='ORTHO'
    cam=bpy.data.objects.new('Camera',camera_data); bpy.context.collection.objects.link(cam); scene.camera=cam
    bpy.context.view_layer.update()
    points=[ob.matrix_world@Vector(co) for ob in OBJECTS for co in ob.bound_box]
    center=sum(points,Vector())/len(points)
    # Exactly equal x/y/z view components: 45-degree azimuth and 35.264-degree elevation.
    direction=Vector((1 if candidate==1 else -1,-1,1)).normalized()
    cam.location=center+direction*150; cam.rotation_euler=(-direction).to_track_quat('-Z','Y').to_euler()
    bpy.context.view_layer.update()
    local=[cam.matrix_world.inverted()@p for p in points]
    mins=[min(p[i] for p in local) for i in [0,1]]; maxs=[max(p[i] for p in local) for i in [0,1]]
    shift=Vector(((mins[0]+maxs[0])/2,(mins[1]+maxs[1])/2,0))
    cam.location+=cam.rotation_euler.to_matrix()@shift
    camera_data.ortho_scale=max(maxs[0]-mins[0],maxs[1]-mins[1])*1.17
    scale=3.1 if is_carpet else 1
    target=(0,0,17 if is_carpet else 6)
    add_area('Large neutral key',(-18*scale,-26*scale,38*scale),4500*scale*scale,22*scale,(1,.94,.87),target)
    add_area('Cool soft fill',(26*scale,-8*scale,20*scale),2250*scale*scale,18*scale,(.76,.88,1),target)
    add_area('Clean rim',(-4*scale,26*scale,30*scale),5000*scale*scale,15*scale,(1,1,1),target)
    bpy.context.view_layer.update()
    projected=[world_to_camera_view(scene,cam,p) for p in points]
    bounds=[min(p.x for p in projected),min(p.y for p in projected),max(p.x for p in projected),max(p.y for p in projected)]
    assert min(bounds[:2])>.02 and max(bounds[2:])<.98,bounds
    return {'projection':'orthographic true isometric','azimuth_degrees':45 if candidate==1 else -45,'elevation_degrees':35.26438968,'camera_location':list(cam.location),'ortho_scale':camera_data.ortho_scale,'projected_model_bounds_normalized':bounds}

def render(name):
    assert not os.environ.get('DISPLAY') and not os.environ.get('WAYLAND_DISPLAY'), 'Headless CLI only'
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.context.scene.world=bpy.data.worlds.new('Studio world')
    MATERIALS.clear(); OBJECTS.clear(); PARTS.clear()
    candidate=2 if name.endswith('-02') else 1
    is_carpet=name.startswith('magic-carpet')
    is_current='-current-' in name
    if is_carpet: geometry=carpets(candidate)
    else:
        kind='roller-skates' if name.startswith('roller') else 'ice-skates'
        geometry=current_item(kind) if is_current else improved_item(kind,candidate)
    camera=configure(candidate,is_carpet,is_current)
    scene=bpy.context.scene
    for im in bpy.data.images:
        if im.source=='FILE': im.pack()
    assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
    scene.render.filepath=str(ROOT/(name+'.png'))
    scene['source_script']=str(ROOT/(name+'.py'))
    scene['render_contract']='Blender headless, Cycles CPU, no display, no audio, no GPU, 1024x1024 PNG'
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/(name+'.blend')))
    start=time.monotonic(); bpy.ops.render.render(write_still=True); elapsed=time.monotonic()-start
    # Open the actual encoded PNG and verify its decoded pixels, not only scene settings.
    image=bpy.data.images.load(str(ROOT/(name+'.png')),check_existing=False)
    import numpy as np
    pixels=np.array(image.pixels[:],dtype=np.float32).reshape((1024,1024,4))
    assert list(image.size)==[1024,1024]
    assert np.isfinite(pixels).all() and float(pixels[:,:,:3].std())>.055
    metadata={'name':name,'blender_version':bpy.app.version_string,'engine':scene.render.engine,'device':scene.cycles.device,'samples':scene.cycles.samples,'threads':scene.render.threads,'duration_seconds':round(elapsed,3),'dimensions':[1024,1024],'geometry':geometry,'camera':camera,'packed_textures':[im.name for im in bpy.data.images if im.packed_file],'script':name+'.py','shared_authoring_script':'render_models.py','shared_script_sha256':hashlib.sha256((ROOT/'render_models.py').read_bytes()).hexdigest(),'blend':name+'.blend','png':name+'.png','png_sha256':hashlib.sha256((ROOT/(name+'.png')).read_bytes()).hexdigest(),'validation':{'opened_rendered_png':True,'rgba_standard_deviation':float(pixels[:,:,:3].std()),'geometry_part_count':len(OBJECTS),'frame_bounds_pass':True,'packed_texture_pass':True,'display_environment':os.environ.get('DISPLAY'),'wayland_environment':os.environ.get('WAYLAND_DISPLAY'),'cycles_device':scene.cycles.device}}
    (ROOT/(name+'-metadata.json')).write_text(json.dumps(metadata,indent=2))
    print('RENDER_COMPLETE '+json.dumps({'name':name,'seconds':elapsed,'bounds':camera['projected_model_bounds_normalized']}),flush=True)
