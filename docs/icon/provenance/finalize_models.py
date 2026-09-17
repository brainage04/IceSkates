from pathlib import Path
import json, hashlib
ROOT=Path(__file__).resolve().parent
names=[f'{kind}-{state}-{candidate:02}' for kind in ['ice-skates','roller-skates'] for state in ['current','improved'] for candidate in [1,2]]+['magic-carpet-tiers','magic-carpet-tiers-02']
provenance=json.loads((ROOT/'provenance.json').read_text())
verification={r['name']:r for r in json.loads((ROOT/'verification.json').read_text())}
manifest=[]; evidence=[]; changes={}
for name in names:
    meta=json.loads((ROOT/(name+'-metadata.json')).read_text())
    assert verification[name]['passed']
    is_carpet=name.startswith('magic')
    improved='-improved-' in name
    candidate=2 if name.endswith('-02') else 1
    kind='roller_skates' if name.startswith('roller') else 'ice_skates'
    project='MagicCarpet' if is_carpet else 'IceSkates'
    if is_carpet:
        source=[r['source'] for r in provenance['sources'] if r['source'].endswith('MagicCarpetEntityModel.java') or ('/MagicCarpet/' in r['source'] and '/textures/entity/' in r['source'])]
        description='All three exact entity models in one vertically stacked isometric scene: basic red/yellow bottom, advanced graphite/orange middle, legendary purple/lime top. Five original boxes per tier, 64x14 atlas UVs, unchanged 24x32x1 geometry.'
    else:
        source=[r['source'] for r in provenance['sources'] if r['source'].endswith('/models/item/'+kind+'.json') or r['source'].endswith('/items/'+kind+'.json') or ('/textures/item/' in r['source'] and (r['source'].endswith('/'+kind+'.png') or r['source'].endswith('/'+kind+'_overlay.png')))]
        if improved:
            source += [r['source'] for r in provenance['sources'] if r['source'].endswith('/ice_skate_blades.png') or r['source'].endswith('/roller_skate_wheels.png')]
            description='Explicit new volumetric voxel-model proposal, not current mod geometry. Open cuffs, restrained dyed real texture palette, two straps and buckles per boot, '+('four stepped quad wheels per boot.' if name.startswith('roller') else 'thin central silver blade and two supports per boot.')+' Exact changes and per-part UV/coordinates: blender-f/improvements.json and image metadata.'
            changes[name]={'changes':meta['geometry']['changes'],'parts':meta['geometry']['parts'],'source_geometry':'New geometry in blender-f/render_models.py; original item model and textures listed in manifest. No mod repository edits.','pair_pose_xyz_degrees':[[-4.0,-1.0,0,-7],[4.0,2.4,0,8]] if candidate==1 else [[-4.1,1.6,0,-11],[4.1,-1.2,0,6]],'texture_sources':source}
        else:
            description='Faithful current minecraft:item/generated item: original paired sprite, original alpha silhouette and 1-model-unit thickness, all exposed texel-edge side faces, default dye '+meta['geometry']['default_layer0_tint']+'. Raster-coincident front/back layers are canonicalized into one alpha-over pair for Cycles, without visible geometry changes. No invented volumetric boot geometry.'
    notes=description+f" Candidate {candidate}: {'cool slate, right isometric' if candidate==1 else 'warm neutral, left isometric'}. 1024x1024 PNG; Blender {meta['blender_version']}; {meta['engine']} {meta['device']}; {meta['samples']} samples; {meta['duration_seconds']} seconds. Scene blender-f/{name}.blend; script blender-f/{name}.py; metadata blender-f/{name}-metadata.json. Packed source texture hashes and ray-visible composition validated in blender-f/verification.json."
    manifest.append({'project':project,'label':name,'path':'blender-f/'+name+'.png','method':f"Blender {meta['blender_version']} / Cycles CPU / {meta['samples']} samples / headless -noaudio",'source':'; '.join(source),'notes':notes})
    evidence.append({k:meta[k] for k in ['name','blender_version','engine','device','samples','threads','duration_seconds','dimensions','png_sha256','shared_script_sha256']})
(ROOT/'manifest.json').write_text(json.dumps(manifest,indent=2))
(ROOT/'render-evidence.json').write_text(json.dumps(evidence,indent=2))
(ROOT/'improvements.json').write_text(json.dumps(changes,indent=2))
(ROOT/'blockers.json').write_text('[]\n')
(ROOT/'resource-evidence.json').write_text(json.dumps({'headless':True,'display_created':False,'display_environment':'','wayland_environment':'','audio_sink_created':False,'audio_device_allocated':False,'audio_argument':'-noaudio','gpu_allocated':False,'cycles_device':'CPU','blender_threads':6,'resources_remaining':[],'source_control':'Local-only batch requirement: reproducible Python scripts preserved but intentionally not committed or pushed. No tracked-file or source-mod edits.','foreign_files':'Other-agent telekinesis/vein-miner/toggle-sprint/vanilla_helpers files, if present, are not part of this manifest and were left untouched for Main.'},indent=2))
print(json.dumps(evidence,indent=2))
