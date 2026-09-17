from pathlib import Path
import bpy, json, math, hashlib
import numpy as np
from mathutils import Vector
ROOT=Path(__file__).resolve().parent
SOURCE_PIXELS=json.loads((ROOT/'source-pixels.json').read_text())
PROVENANCE=json.loads((ROOT/'provenance.json').read_text())
TEXTURE_HASHES={Path(r['snapshot']).name:r['sha256'] for r in PROVENANCE['sources'] if r['snapshot'].startswith('textures/')}
names=[f'{kind}-{state}-{candidate:02}' for kind in ['ice-skates','roller-skates'] for state in ['current','improved'] for candidate in [1,2]]+['magic-carpet-tiers','magic-carpet-tiers-02']
results=[]
for name in names:
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/(name+'.blend')))
    scene=bpy.context.scene
    assert scene.render.engine=='CYCLES' and scene.cycles.device=='CPU'
    assert scene.cycles.samples==64
    assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
    texture_checks={}
    for packed_image in [im for im in bpy.data.images if im.source=='FILE']:
        digest=hashlib.sha256(packed_image.packed_file.data).hexdigest()
        filename=Path(packed_image.filepath).name
        assert digest==TEXTURE_HASHES[filename],filename
        texture_checks[filename]=digest
    im=bpy.data.images.load(str(ROOT/(name+'.png')),check_existing=False)
    assert list(im.size)==[1024,1024]
    pixels=np.array(im.pixels[:]).reshape(1024,1024,4)
    camera=scene.camera; bpy.context.view_layer.update()
    deps=bpy.context.evaluated_depsgraph_get()
    direction=camera.matrix_world.to_quaternion()@Vector((0,0,-1))
    counts={}; examples={}
    # Ray-grid verifies actual visible model surfaces, not just declared object counts.
    for j in range(96):
        for i in range(96):
            local=Vector((((i+.5)/96-.5)*camera.data.ortho_scale,((j+.5)/96-.5)*camera.data.ortho_scale,0))
            origin=camera.matrix_world@local
            hit,point,normal,face,ob,matrix=scene.ray_cast(deps,origin,direction,distance=1000)
            if not hit or 'role' not in ob: continue
            if '-current-' in name:
                # Front planes are alpha-cut: reject the transparent texels from geometric ray hits.
                file=ob.data.materials[0].node_tree.nodes.get('Image Texture').image.name
                x=int(point.x+8); y=int(16-point.z)
                source=SOURCE_PIXELS[file]['rgba']
                if not (0<=x<16 and 0<=y<16): continue
                if face<2 and ob.name.endswith('layer0'):
                    overlay=SOURCE_PIXELS[file.replace('.png','_overlay.png')]['rgba']
                    if not (source[y][x][3] or overlay[y][x][3]): continue
                elif not source[y][x][3]: continue
                group='left sprite skate' if point.x<0 else 'right sprite skate'
            elif name.startswith('magic'):
                group=ob['tier']
            else:
                group=ob.name.split('-')[0]
                if '-blade-runner' in ob.name: counts[group+' visible runner']=counts.get(group+' visible runner',0)+1
                if '-wheel-' in ob.name or '-hub-' in ob.name: counts[group+' visible wheels']=counts.get(group+' visible wheels',0)+1
            counts[group]=counts.get(group,0)+1
            examples.setdefault(group,[])
            if len(examples[group])<4:
                xx=min(1023,int((i+.5)/96*1024)); yy=min(1023,int((j+.5)/96*1024))
                examples[group].append({'pixel':[xx,1023-yy],'rgba':pixels[yy,xx].tolist()})
    if name.startswith('magic'):
        assert all(counts.get(tier,0)>250 for tier in ['basic','advanced','legendary']),counts
        tier_objects=[ob for ob in scene.objects if ob.get('role')=='carpet-tier']
        assert len(tier_objects)==15
        for tier in ['basic','advanced','legendary']:
            assert len([ob for ob in tier_objects if ob['tier']==tier])==5
    elif '-improved-' in name:
        assert counts.get('boot1',0)>200 and counts.get('boot2',0)>200,counts
        suffix=' visible wheels' if name.startswith('roller') else ' visible runner'
        assert all(counts.get(boot+suffix,0)>0 for boot in ['boot1','boot2']),counts
        # Each cuff must have an actual cavity at the center from z=top down to the floor.
        for ob in [ob for ob in scene.objects if ob.type=='EMPTY']:
            origin=ob.matrix_world@Vector((0,3,30))
            hit,point,normal,face,target,matrix=scene.ray_cast(deps,origin,Vector((0,0,-1)),distance=50)
            assert hit and target.name.endswith('cuff-floor'),(ob.name,target.name)
    else:
        assert counts.get('left sprite skate',0)>50 and counts.get('right sprite skate',0)>50,counts
        assert all(abs(ob.dimensions.y-1)<1e-6 for ob in scene.objects if ob.get('role')=='current-generated-item')
        for ob in [ob for ob in scene.objects if ob.get('role')=='current-generated-item']:
            filename=ob.data.materials[0].node_tree.nodes.get('Image Texture').image.name
            rows=SOURCE_PIXELS[filename]['rgba']
            boundary_count=0
            for yy in range(16):
                for xx in range(16):
                    if not rows[yy][xx][3]: continue
                    for nx,ny in [(xx-1,yy),(xx+1,yy),(xx,yy-1),(xx,yy+1)]:
                        if not (0<=nx<16 and 0<=ny<16) or not rows[ny][nx][3]: boundary_count+=1
            assert len(ob.data.polygons)==boundary_count+(2 if ob.name.endswith('layer0') else 0)
    result={'name':name,'passed':True,'resolution':[1024,1024],'engine':scene.render.engine,'device':scene.cycles.device,'samples':scene.cycles.samples,'packed_textures_verified':True,'source_texture_sha256_matches':texture_checks,'visible_model_ray_samples':counts,'decoded_png_sample_evidence':examples}
    results.append(result)
    print('VERIFIED '+name+' '+json.dumps(counts),flush=True)
(ROOT/'verification.json').write_text(json.dumps(results,indent=2))
