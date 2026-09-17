from pathlib import Path
import runpy
scene = runpy.run_path(str(Path(__file__).resolve().parent/'render_models.py'))
scene['render']('ice-skates-improved-01')
