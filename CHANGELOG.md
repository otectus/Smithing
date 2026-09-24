# Changelog

## 1.0.0 - 2026-09-23

First release as Immersive Smithing, for Minecraft 1.20.1 and Forge 47.

### Renamed from Ote's Smithing

This mod was previously called Ote's Smithing (builds 1.0.0 and 1.1.0, mod ID `otes_smithing`). Everything from
those builds is included here, and version numbering starts again at 1.0.0.

- The mod ID and datapack namespace are now `immersive_smithing`, the command is `/immersivesmithing`, the Java
  package is `com.otectus.immersivesmithing` and the jar is `immersive-smithing-1.0.0.jar`.
- **Not compatible with Ote's Smithing worlds or datapacks.** Its stations, tools, workpieces and item quality data
  do not carry over. Datapacks must use the `immersive_smithing` namespace and `data/<namespace>/immersive_smithing/`
  folders. The config files are now `immersive_smithing-server.toml` and `immersive_smithing-client.toml`.
- Remove the Ote's Smithing jar when installing this version; the two mods would otherwise both load.

### Smithing

- Four stations: the Smith's Forge melts metal, the Smith's Anvil shapes it, the Smith's Trough quenches it and the
  Smith's Grindstone refines finished equipment for experience levels.
- Smithing Tongs and Smithing Hammers in stone, iron, diamond and netherite. Higher tiers give more minigame time
  and durability, never extra quality.
- Forge minigame (timing on a sweeping marker) sets the Forge score and durability. Anvil minigame (strikes on
  closing rings) sets the Anvil score and efficacy: attack damage, mining speed or armor.
- Quality labels Crude, Standard, Fine and Masterwork; Faulty items when the anvil timer runs out.
- Material families measured in units (nugget 1, ingot 9, block 81); built-in iron, gold, copper and netherite,
  optional definitions for common modded metals, and automatic families from `forge:ingots` tags.
- Metal equipment can be melted back into the forge, recovering its metal.
- Netherite equipment is forged directly from netherite, and lava is the only heat that melts it.
- Hoppers and pipes can load forges and fill troughs; comparators read both. Minigames and quenching always need a
  player.
- The Smithing Guide in-game book, nine advancements, and Toolsmith, Weaponsmith and Armorer trades.

### Compatibility

- Automatic detection turns single-metal crafting recipes for weapons, tools, armor and metal shields from any mod
  into smithing recipes and disables the crafting recipe.
- Built-in Spartan Weaponry recipes and anvil patterns for every metal weapon of all 24 types. Thrown weapons keep
  their quality, and arrows and bolts take the quality of the bow or crossbow that fires them.
- Metal shields no longer need their wooden base shield.
- Smithing-table upgrades whose result can be smithed, modded netherite gear included, are forged from the metal
  instead. Armor trims are untouched.
- Optional JEI category for every smithing recipe.
- Datapack formats and a Java/IMC API for materials, recipes, recycling values, anvil patterns, efficacy and shield
  handlers ([docs/API.md](docs/API.md)).

### Presentation

- Custom station models with four rotations and matching collision shapes. Finished artwork for every tool tier,
  loaded tongs, hot workpieces, the guide and station items.
- Stations show their contents: billets and fuel in the forge, animated molten metal, heated stock on the anvil and
  the water level in the trough.
- Strike and grinding sparks, ignition embers, and quench splashes and steam. Tongs thrust and hammer strikes are
  animated in first and third person.
- Framed recipe and minigame screens, a parchment guide, a matching JEI category and clearer quality tooltips.
- Client options for numeric quality scores, reduced screen shake and flashes, particle amount, high-contrast
  minigames, larger or colorblind-safe targets and timing cue sounds. None of them widen scoring windows.

### Multiplayer

- Minigames are server-authoritative: clients send only their inputs, and the server checks timing with bounded
  latency compensation and calculates the scores.
- Only one player can work on an anvil's workpiece at a time. Components set aside for a forging are held by the
  forge and leave it exactly once: used on completion or returned on cancellation. If the forge's chunk unloads
  first, they drop at the forge when it loads again.

### Known issues

- Sounds reuse vanilla sound events; there is no custom audio yet.
