from pathlib import Path
import bpy, json
ROOT = Path(__file__).resolve().parent
result = {}
for path in sorted((ROOT/'textures').glob('*.png')):
    image=bpy.data.images.load(str(path))
    w,h=image.size
    pixels=list(image.pixels)
    rows=[]
    for y in range(h):
        row=[]
        for x in range(w):
            p=pixels[((h-1-y)*w+x)*4:((h-1-y)*w+x)*4+4]
            row.append([round(v*255) for v in p])
        rows.append(row)
    result[path.name]={'size':[w,h],'rgba':rows}
(ROOT/'source-pixels.json').write_text(json.dumps(result))
for name,data in result.items():
    if data['size']==[16,16]:
        print(name)
        for y,row in enumerate(data['rgba']):
            print(f'{y:02}', ' '.join('---' if p[3]==0 else f'{p[0]:03}' for p in row))
