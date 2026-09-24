# Smithing artwork

`smithing_art.py` is the editable pixel-art source: named material ramps, hand-placed 16×16 item pixels, block material clusters, four molten frames, particles and GUI sheets. No stochastic noise, smooth resampling or placeholder synthesis is used.

- `python3 tools/generate_textures.py` (or `--check`) compares exports, without writing anything.
- `python3 tools/generate_textures.py --write` explicitly exports the finished artwork to the resource folder. Change the source first.
- Station and decorative mesh sources are the Java Block/Item JSON files in `src/main/resources/assets/immersive_smithing/models/block/`. Open these directly in Blockbench using **File → Import → Java Block/Item**. Each face has explicit UVs at one texel per model unit. North is the front; rotations live in blockstates. Keep the matching `StationShape` definitions in sync if changing geometry.
- All item transforms remain resource-pack replaceable. Loaded tongs use Forge's standard `forge:item_layers` with only layer 1 emissive. For Forge 47.4.23 the data is nested under `forge_data.layers`, keyed by the layer number; the validation command checks this contract. No shader pack is needed.
- Decorative meshes (`metal_billet`, `fuel_chunk`, `hot_billet`, `hot_plate`, `hot_blade`) are registered as additional baked models and resolved from the current model manager after reload. Textures likewise come from the current block atlas.
- GUI atlas contract: panel `(0,0,64,64)` with 8px borders; inset `(64,0,32,32)` with 4px borders; ring `(96,0,64,64)` and disc `(160,0,64,64)`. Guide: parchment `(0,0,64,64)` with 12px borders; chapter inset `(64,0,32,32)` with 6px borders.

Palette: blue-grey worked iron, soot-dark masonry, warm worn timber, small bronze fasteners, ivory/yellow/orange hot metal. Shapes, labels and timer marks accompany color. Station geometry supplies the silhouette; pixels supply grain, seams, wear and fasteners.

References: [Blockbench Minecraft Style Guide](https://blockbench.net/wiki/guides/minecraft-style-guide/), Forge 1.20.1 [block entity renderers](https://docs.minecraftforge.net/en/1.20.1/blockentities/ber/), [screens](https://docs.minecraftforge.net/en/1.20.1/gui/screens/), [emissive face data](https://docs.minecraftforge.net/en/1.20.1/rendering/modelextensions/facedata/).
