from pathlib import Path
import subprocess, os, sys
ROOT=Path(__file__).resolve().parent
BLENDER='/nix/store/j57qch19aq4rs4fg728vlilgb0dcxs7z-blender-5.1.1/bin/blender'
names=[f'{kind}-{state}-{candidate:02}' for kind in ['ice-skates','roller-skates'] for state in ['current','improved'] for candidate in [1,2]]+['magic-carpet-tiers','magic-carpet-tiers-02']
if len(sys.argv)>1: names=sys.argv[1:]
environment=dict(os.environ)
for key in ['DISPLAY','WAYLAND_DISPLAY','CUDA_VISIBLE_DEVICES','HIP_VISIBLE_DEVICES','ROCR_VISIBLE_DEVICES']:
    environment[key]=''
for name in names:
    with (ROOT/(name+'.log')).open('w') as log:
        subprocess.run([BLENDER,'--background','-noaudio','--python-exit-code','1','--python',str(ROOT/(name+'.py'))],env=environment,stdout=log,stderr=subprocess.STDOUT,timeout=600,check=True)
    print('COMPLETE '+name,flush=True)
