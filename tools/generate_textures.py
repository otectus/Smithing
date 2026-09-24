#!/usr/bin/env python3
"""Export or check the finished editable artwork; never regenerate placeholder textures.

Default / --check is read-only. --write explicitly exports art/smithing_art.py. Models are
hand-authored resource JSON, directly editable with Blockbench's Java Block/Item importer.
"""
import argparse
import importlib.util
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'src/main/resources/assets/immersive_smithing/textures'


def check_model_uvs():
    """Check the authored model/texture density contract independently of the PNG exporter."""
    count = 0
    for path in (OUTPUT.parent / 'models/block').glob('*.json'):
        for element in json.loads(path.read_text()).get('elements', []):
            x, y, z = [b - a for a, b in zip(element['from'], element['to'])]
            for face, data in element['faces'].items():
                u, v, end_u, end_v = data['uv']
                expected = (x, z) if face in ('up', 'down') else (x, y) if face in ('north', 'south') else (z, y)
                actual = (abs(end_u - u), abs(end_v - v))
                if actual != expected or min(data['uv']) < 0 or max(data['uv']) > 16:
                    raise SystemExit(f'{path.name}: {element.get("name")} {face} violates 1:1 UV density')
                count += 1
    return count


def check_emissive_items():
    """Forge 47.4.23 ItemLayerModel.Loader reads forge_data.layers, not direct layer keys."""
    directory = OUTPUT.parent / 'models/item'
    paths = list(directory.glob('*_tongs_loaded.json')) + [directory / 'hot_workpiece.json']
    for path in paths:
        model = json.loads(path.read_text())
        layer = '0' if path.stem == 'hot_workpiece' else '1'
        data = model.get('forge_data', {}).get('layers', {}).get(layer, {})
        if model.get('loader') != 'forge:item_layers' or data.get('block_light') != 15 or data.get('sky_light') != 15:
            raise SystemExit(f'{path.name}: hot layer is missing Forge item-layer emission data')
    return len(paths)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--write', action='store_true', help='explicitly export finished source artwork')
    mode.add_argument('--check', action='store_true', help='verify exports without writing (default)')
    args = parser.parse_args()
    spec = importlib.util.spec_from_file_location('smithing_art', ROOT / 'art/smithing_art.py')
    art = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(art)
    mismatches = []
    textures = art.textures()
    for name, expected in textures.items():
        path = OUTPUT / name
        if args.write:
            path.parent.mkdir(parents=True, exist_ok=True)
            expected.save(path)
        elif not path.exists():
            mismatches.append(name)
        else:
            with Image.open(path) as actual:
                if actual.size != expected.size or actual.convert('RGBA').tobytes() != expected.tobytes():
                    mismatches.append(name)
    if mismatches:
        raise SystemExit('Artwork differs from source (no files written): ' + ', '.join(mismatches))
    count = check_model_uvs()
    emissive = check_emissive_items()
    print(f'{"Exported" if args.write else "Verified"} {len(textures)} finished textures; {count} model faces have 1:1 UVs; {emissive} emissive item models.')


if __name__ == '__main__':
    main()
