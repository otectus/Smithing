#!/usr/bin/env python3
"""Generates Immersive Smithing recipes for every Spartan Weaponry metal weapon from Spartan's own recipe JSONs.

Run from the project root (the jar defaults to the Gradle cache copy):
  python3 tools/generate_spartan_compat.py [path/to/spartanweaponry.jar]

Output: src/main/resources/data/immersive_smithing/recipes/compat/spartanweaponry/<metal>_<type>.json. The output is
deterministic and committed; re-run it when the pinned Spartan Weaponry version changes.

Rules (mirroring Spartan's shapes exactly):
- metal_units = number of material slots x 9; every other key becomes an auxiliary ingredient with its count.
- Netherite weapons are forged directly: the diamond recipe's shape in netherite units, same auxiliaries.
- Studded club / cestus: only the iron is required; the base weapon is dismissed.
- Conditions: forge:mod_loaded first (so Spartan's own condition type is never parsed without Spartan), then
  Spartan's type_disabled condition copied from the source recipe, then any tag_empty guard for modded metals.
- Ammunition (arrows, bolts) is not smithed.
"""
import glob
import json
import os
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/data/immersive_smithing/recipes/compat/spartanweaponry'
TYPES = ['battle_hammer', 'battleaxe', 'boomerang', 'dagger', 'flanged_mace', 'glaive', 'greatsword', 'halberd',
         'heavy_crossbow', 'javelin', 'katana', 'lance', 'longbow', 'longsword', 'parrying_dagger', 'pike',
         'quarterstaff', 'rapier', 'saber', 'scythe', 'spear', 'throwing_knife', 'tomahawk', 'warhammer']
METALS = {  # Spartan recipe prefix -> Immersive Smithing material family
    'copper': 'minecraft:copper', 'golden': 'minecraft:gold', 'iron': 'minecraft:iron',
    'tin': 'forge:tin', 'bronze': 'forge:bronze', 'steel': 'forge:steel', 'silver': 'forge:silver',
    'electrum': 'forge:electrum', 'lead': 'forge:lead', 'nickel': 'forge:nickel', 'invar': 'forge:invar',
    'constantan': 'forge:constantan', 'platinum': 'forge:platinum', 'aluminum': 'forge:aluminum',
}
MOD_LOADED = {'type': 'forge:mod_loaded', 'modid': 'spartanweaponry'}


def find_jar():
    hits = glob.glob(os.path.expanduser('~/.gradle/caches/**/spartan-weaponry*official*.jar'), recursive=True)
    if not hits:
        raise SystemExit('Spartan Weaponry jar not found; pass its path')
    return hits[0]


def shape(recipe):
    """(material slot count, [(ingredient json, count)]) for a shaped recipe."""
    pattern = ''.join(recipe['pattern'])
    material_keys = [k for k, v in recipe['key'].items()
                     if 'tag' in v and v['tag'].split(':', 1)[1].startswith(('ingots/', 'gems/'))]
    if len(material_keys) != 1:
        raise ValueError(f'expected one material key, got {material_keys}')
    slots = pattern.count(material_keys[0])
    aux = [(v, pattern.count(k)) for k, v in sorted(recipe['key'].items()) if k not in material_keys and pattern.count(k)]
    return slots, aux


def conditions(recipe):
    out = [MOD_LOADED]
    out += [c for c in recipe.get('conditions', []) if c.get('type') == 'spartanweaponry:type_disabled']
    out += [c for c in recipe.get('conditions', []) if c.get('type') == 'forge:not']
    return out


def smithing(result, family, units, aux, pattern, conds):
    obj = {'conditions': conds, 'type': 'immersive_smithing:smithing', 'material': family, 'metal_units': units}
    if aux:
        obj['auxiliary'] = [{'ingredient': ing, 'count': count} for ing, count in aux]
    obj['result'] = {'item': result}
    obj['forge_pattern'] = 'immersive_smithing:standard'
    obj['anvil_pattern'] = f'immersive_smithing:{pattern}'
    return obj


def main():
    jar = sys.argv[1] if len(sys.argv) > 1 else find_jar()
    z = zipfile.ZipFile(jar)

    def recipe(name):
        return json.loads(z.read(f'data/spartanweaponry/recipes/{name}.json'))

    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob('*.json'):
        old.unlink()
    written = 0
    for wtype in TYPES:
        for prefix, family in METALS.items():
            src = recipe(f'{prefix}_{wtype}')
            slots, aux = shape(src)
            obj = smithing(src['result']['item'], family, slots * 9, aux, wtype, conditions(src))
            (OUT / f'{prefix}_{wtype}.json').write_text(json.dumps(obj, indent=2) + '\n')
            written += 1
        diamond = recipe(f'diamond_{wtype}')
        slots, aux = shape(diamond)
        obj = smithing(f'spartanweaponry:netherite_{wtype}', 'minecraft:netherite', slots * 9, aux, wtype, conditions(diamond))
        (OUT / f'netherite_{wtype}.json').write_text(json.dumps(obj, indent=2) + '\n')
        written += 1
    for studded, base_type in (('studded_club', 'club'), ('studded_cestus', 'cestus')):
        src = recipe(studded)
        pattern = ''.join(src['pattern'])
        iron = [k for k, v in src['key'].items() if v.get('tag') == 'forge:ingots/iron'][0]
        obj = smithing(src['result']['item'], 'minecraft:iron', pattern.count(iron) * 9, [], base_type, conditions(src))
        (OUT / f'{studded}.json').write_text(json.dumps(obj, indent=2) + '\n')
        written += 1
    print(f'{written} recipes written to {OUT.relative_to(ROOT)} from {os.path.basename(jar)}')


if __name__ == '__main__':
    main()
