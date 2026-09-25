# Immersive Smithing - Datapack and Java API

Immersive Smithing is configured almost entirely through datapacks. The Java API exists for items that datapacks
cannot describe. Everything below is re-read on `/reload` (or `/immersivesmithing reload`).

## Material units

All metal accounting uses integers: nugget = 1, ingot or raw ore = 9, storage block = 81.

## Smithing recipes

Recipe type `immersive_smithing:smithing`. Recipes are ordinary Minecraft recipes, so they live in
`data/<namespace>/recipes/` (any subfolder) and support Forge `conditions`, for example `forge:mod_loaded`.
They are synced to clients and shown in JEI.

```json
{
  "type": "immersive_smithing:smithing",
  "material": "examplemod:steel",
  "metal_units": 27,
  "auxiliary": [
    { "ingredient": { "item": "minecraft:stick" }, "count": 2 }
  ],
  "result": { "item": "examplemod:steel_pickaxe" },
  "forge_pattern": "immersive_smithing:standard",
  "anvil_pattern": "immersive_smithing:pickaxe"
}
```

| Field | Required | Notes |
|---|---|---|
| `material` | yes | Material family id. A recipe whose family does not exist is deactivated and reported. |
| `metal_units` | yes | Positive integer. |
| `auxiliary` | no | Non-metal ingredients taken from the smith's inventory when forging starts. `count` defaults to 1. With `"consume_equipment": true` the ingredient is a finished piece the recipe reworks (a netherite helmet becoming an ignitium helmet): it is matched by item, whatever its damage or quality, escrowed like any other auxiliary and replaced by the new piece with the quality of the new forging. |
| `result` | yes | Standard item-stack JSON; `nbt` is kept on the finished item. |
| `forge_pattern` | no | Defaults to `immersive_smithing:standard`. |
| `anvil_pattern` | no | Defaults to the closest pattern family for the result item. |

An explicit recipe always wins over automatic detection for the same result item. Results tagged
`immersive_smithing:non_smithable_equipment` are ignored.

## Material families

`data/<namespace>/immersive_smithing/materials/<name>.json`

```json
{
  "family": "examplemod:steel",
  "display_name": "material.examplemod.steel",
  "sources": [
    { "tag": "forge:ingots/steel", "units": 9 },
    { "tag": "forge:nuggets/steel", "units": 1 },
    { "tag": "forge:storage_blocks/steel", "units": 81 },
    { "item": "examplemod:steel_scrap", "units": 3 }
  ],
  "melt_time_multiplier": 1.0,
  "requires_ignition": true,
  "required_fuel_tag": "immersive_smithing:netherite_fuels",
  "tint": "#FF8A3D",
  "upgrade_policy": "shape"
}
```

| Field | Required | Notes |
|---|---|---|
| `family` | yes | The family id. Several files for the same id are merged in file-id order: sources add up, and the last file sets the other fields. |
| `display_name` | no | A translation key (the key itself is shown if untranslated) or a text component object. |
| `sources` | yes | Each entry has a `tag` or an `item`, plus positive `units`. |
| `melt_time_multiplier` | no | Multiplies the configured base melt time. Default 1.0. |
| `requires_ignition` | no | Default `true`. When `false`, solid fuel heats the forge without being lit. |
| `required_fuel_tag` | no | The heat source must be in this item tag. Lava counts as `minecraft:lava_bucket`. |
| `tint` | no | Colour of the molten metal, `"#RRGGBB"` or an integer. |
| `upgrade_policy` | no | How upgrades *into* this metal are priced when their base is another piece of equipment. `shape` (default): the base's whole shape in this metal, base not needed (vanilla netherite). `addition`: only the metal the original recipe asked for, and the base piece is consumed as an auxiliary; right for scarce boss metals. |

Files support Forge `conditions`. An item claimed by two families is ignored and reported as ambiguous.

When `autoDetectModdedMetals` is on, every `forge:ingots/<metal>` tag that no definition covers becomes the
family `forge:<metal>`, with nuggets (1), storage blocks (81), raw materials (9) and raw storage blocks (81)
taken from the matching `forge:` tags. Its display name uses the key `material.forge.<metal>`.

## Recycling overrides

`data/<namespace>/immersive_smithing/recycling/<name>.json`

```json
{ "item": "examplemod:steel_greatsword", "family": "examplemod:steel", "units": 45 }
```

```json
{ "tag": "examplemod:relics", "recyclable": false }
```

Use `item` or `tag`. With `"recyclable": false` the items can never be melted down. Items tagged
`immersive_smithing:non_recyclable` are excluded too.

Recycling values are resolved in this order: exclusions, explicit smithing recipes, recycling overrides, analysis
of the item's crafting recipe, then Java `IRecyclingValueProvider`s. The value returned is multiplied by
`recyclingEfficiency` and never exceeds the recognised value.

## Anvil patterns

`data/<namespace>/immersive_smithing/anvil_patterns/<name>.json`. The pattern id is the file id.

```json
{
  "targets": [[0.30, 0.70], [0.46, 0.54], [0.62, 0.38], [0.78, 0.22]],
  "strikes": 10,
  "target_radius": 0.075,
  "target_lifetime": 1.8,
  "ideal_fraction": 0.6,
  "gap": 0.25,
  "jitter": 0.03,
  "time_multiplier": 1.0,
  "categories": ["minecraft:swords"]
}
```

| Field | Default | Notes |
|---|---|---|
| `targets` | required | 1 to 64 points `[x, y]`, normalised 0..1 over the workpiece (y points down). Visited in order and repeated when `strikes` is larger. |
| `strikes` | number of targets | Strikes needed to finish (1 to 64). |
| `target_radius` | 0.075 | Radius as a fraction of the work area. |
| `target_lifetime` | 1.8 | Seconds a target stays before it expires and comes back. |
| `ideal_fraction` | 0.6 | When in the lifetime a strike is perfectly timed. |
| `gap` | 0.25 | Seconds between a hit and the next target. |
| `jitter` | 0.03 | Random offset applied to each target from the session seed. |
| `time_multiplier` | 1.0 | Multiplies the time granted by the hammer tier. |
| `categories` | none | Item tags; items in them use this pattern before class-based classification. |

Built-in patterns: `sword`, `axe`, `pickaxe`, `shovel`, `hoe`, `generic_tool`, `helmet`, `chestplate`, `leggings`,
`boots`, `shield`, `generic_weapon`, `generic_armor`, `generic_equipment` (all in the `immersive_smithing` namespace).
A missing pattern falls back to `immersive_smithing:generic_equipment`.

The Spartan Weaponry patterns are named after its weapon types (`immersive_smithing:longsword`, `immersive_smithing:javelin`,
`immersive_smithing:heavy_crossbow`, ... plus `club` and `cestus`). Their `categories` are Spartan's per-type item tags
(`spartanweaponry:longswords`, ...), so add-ons that tag their weapons the same way get the matching pattern.

## Forge patterns

`data/<namespace>/immersive_smithing/forge_patterns/<name>.json`

```json
{
  "base_phases": 3,
  "units_per_extra_phase": 24,
  "min_phases": 3,
  "max_phases": 7,
  "zone_half_width": 0.085,
  "min_speed": 0.45,
  "max_speed": 0.8,
  "acceleration": 0.35,
  "transition_ms": 450
}
```

Phases = `base_phases + metal_units / units_per_extra_phase`, clamped to `min_phases..max_phases`. Speeds are
track lengths per second; the marker speeds up by `acceleration` per second while the smith waits.

## Tags

| Tag | Meaning |
|---|---|
| `immersive_smithing:smithable_equipment` | Always a candidate for automatic detection. |
| `immersive_smithing:non_smithable_equipment` | Never detected, disabled or given a smithing recipe (explicit recipes included). |
| `immersive_smithing:smithable_shields` | Shields that may be detected even if not predominantly metal. |
| `immersive_smithing:non_smithable_shields` | Shields never redirected (contains `minecraft:shield`). |
| `immersive_smithing:non_recyclable` | Never melted down. |
| `immersive_smithing:forge_fuels` | Solid fuels the forge accepts (with a furnace burn time), plus `minecraft:lava_bucket`. |
| `immersive_smithing:forge_igniters` | Items that ignite the forge (flint and steel, fire charge). |
| `immersive_smithing:netherite_fuels` | Heat sources for netherite (lava only). |
| `immersive_smithing:tongs`, `immersive_smithing:hammers` | The smithing tools. |

## Automatic detection

With `autoDetectEquipment` on, each crafting recipe whose result is a single unstackable, damageable weapon, tool,
armor piece or metal shield is analysed. It becomes an automatic smithing recipe
(`immersive_smithing:auto/<namespace>/<item>`) only if:

- every ingredient is entirely one metal family or entirely non-metal,
- exactly one metal family is used,
- no ingredient is itself damageable equipment,
- the result is not blacklisted (tag, recipe id, mod id, family),
- no explicit recipe exists for the result.

An ingredient that is itself a shield is dismissed when the result is a shield: a metal shield built on a wooden base
shield costs only its metal, and the base is not required. Everything else is skipped and listed by
`/immersivesmithing report`. Bows, crossbows, fishing rods and tridents are not candidates unless tagged
`immersive_smithing:smithable_equipment`.

Smithing-table upgrades (`minecraft:smithing_transform`) whose result is candidate equipment without a smithing
recipe are redirected too, when the addition is a single metal family (for example netherite from any mod). The
template is never required. Under the addition metal's `shape` policy the cost is the base item's shape in that
metal: the base's own smithing recipe if it has one (units scaled by the addition's unit value), otherwise its
crafting recipe, where ingredients the base accepts as repair material (or `forge:gems`/`forge:ingots`) are the
material slots, other components stay auxiliary and a base shield is dismissed. Under the `addition` policy, or
when the base has no metal shape of its own (a loot weapon, a cloth robe), the cost is the addition itself and the
base piece is consumed. The recipe id is `immersive_smithing:auto/<namespace>/<item>`.

**Upgrade chains in crafting.** A crafting recipe with exactly one equipment ingredient of the same class as its
result (same anvil pattern and armour slot), one metal family among the other ingredients and a base the forge
already makes is priced like a smithing-table upgrade (Botania's terrasteel armour from manasteel armour). Its other
components stay auxiliary. A recipe whose equipment ingredient is of another class is skipped and reported; one
without any metal (a bone sword given dragon blood) is not the forge's business and is not reported.

**Predominance.** A recipe with fewer metal slots than other components and less than one ingot of metal (a helmet
with a single gold nugget) is skipped as "not predominantly metal" unless the result is tagged
`immersive_smithing:smithable_equipment`.

## Built-in mod support

Recipes for other mods live under `data/immersive_smithing/recipes/compat/<modid>/` and start with a
`forge:mod_loaded` condition, so they are inert without that mod. Material definitions for other mods live under
`immersive_smithing/materials/compat/<modid>/` with the same condition and give the metal a name, tint, melt rule and
upgrade policy:

| Mod | Families | Notes |
|---|---|---|
| Cataclysm | `cataclysm:black_steel`, `cataclysm:ancient_metal`, `cataclysm:witherite`, `cataclysm:cursium`, `cataclysm:ignitium` | Cataclysm ships no `forge:ingots` tags, so these are listed item by item. The three boss metals need lava and use the `addition` policy: an ignitium helmet costs one ingot plus the netherite helmet it reworks. Ignitium and cursium armour is unbreakable, so it is tagged `smithable_equipment` to count as equipment at all. |
| Ice and Fire | `forge:fire_dragonsteel`, `forge:ice_dragonsteel`, `forge:lightning_dragonsteel` | Lava only. The ids match the tags Spartan Fire adds, so recipes are the same with or without it. |
| Spartan Fire | recipes under `compat/spartanfire/` | Every dragonsteel weapon of all 24 Spartan types, generated by `tools/generate_spartan_compat.py --spartanfire <jar>`; follows the `integrations.spartanWeaponry` toggle. |
| Iron's Spells 'n Spellbooks | `forge:mithril`, `forge:pyrium` | Pyrium needs lava and uses `addition`, so the hellrazor and legionnaire flamberge rework their loot base. The cloth wizard sets are tagged `non_smithable_equipment`; the netherite mage set is forged. |
| Botania | `forge:manasteel`, `forge:terrasteel`, `forge:elementium` | Terrasteel uses `addition`: terrasteel armour costs its three ingots plus the manasteel piece, as in Botania. |
| Minecraft Comes Alive | `forge:rose_gold` | Name and tint only. |

Fire Sticks, Lit It Up and Hardcore Torches fire starters ignite the forge (`forge_igniters`), and `#forge:coal_coke`
is a forge fuel. Ars Nouveau robes, Ice and Fire sheep armour and elytra-like items are tagged non-smithable.

Spartan Weaponry: every metal weapon of all 24 types (melee, throwing weapons, longbows, heavy crossbows) in copper,
gold, iron and netherite, and in tin, bronze, steel, silver, electrum, lead, nickel, invar, constantan, platinum and
aluminum when another mod supplies those ingots. Costs and components mirror Spartan's own recipes; netherite
weapons use the diamond shape in netherite. The studded club and cestus cost 9 iron units and no base weapon.
Spartan's `type_disabled` config conditions are honoured. Arrows and bolts are not smithed. The server config option
`integrations.spartanWeaponry` (default `true`) turns these recipes off; Spartan Weaponry is then handled by automatic
detection like any other mod.

Quality reaches projectiles: a thrown Spartan Weaponry weapon's base damage is multiplied by its efficacy, and any
arrow or bolt by the efficacy of the bow or crossbow that fired it.

When `replaceMetalEquipmentRecipes` is on and `allowVanillaCraftingAlongsideSmithing` is off, crafting recipes
that produce a smithable item from recognised metal are removed at reload, as are smithing-table upgrade recipes
(`suppressSmithingTableUpgrades`). Armor trim recipes are never touched. Changing these options needs a `/reload`.

## Loot quality

A global loot modifier (`immersive_smithing:forged_loot`) grades equipment that comes out of loot tables (chests,
mob loot tables, treasure bags) when the forge has a smithing recipe for it and it carries no quality yet. Server
config `lootQuality.mode`: `OFF`, `STANDARD` (default: both scores between 35 and 69, so Fine and Masterwork are
always someone's work) or `RANDOM` (the villager distribution). `lootQuality.excludedLootTables` lists table ids or
whole namespaces that stay ungraded. Equipment a mob drops from its own hands is not loot-table output and stays
ungraded.

## Quality inheritance

When a crafting or smithing-table recipe consumes exactly one graded piece and produces graded equipment without a
quality of its own (dyeing, an elemental upgrade of a dragonbone sword), the result keeps the quality, the maker's
mark, the title and the inscription (`inheritQualityOnCraft`, default on).

## Item data

Finished items keep quality and the maker's mark in the `immersive_smithing` compound of the stack's tag:

```
immersive_smithing: { Version: 1, Forged: 1b, ForgeScore: 94, AnvilScore: 82, Faulty: 0b, Quality: "fine", DurabilityCarry: 0.4d,
                      SmithName: "Otectus", SmithUUID: [I; ...], Signed: 1b }
```

`SmithName` and `SmithUUID` are written at the quench and never change. A signed piece stores its title as the
vanilla `display.Name` (non-italic, in the quality colour) and its inscription as `display.Lore`, so every tooltip
mod shows them. Only the smith named in the mark can sign, from the screen that opens after the quench or by
sneak-using the Smith's Anvil with the finished piece; Faulty work is never signed. The server sanitises titles and
inscriptions (formatting codes and control characters are stripped, lengths capped by the `signing` config).

A stack without this compound behaves exactly like Standard quality. Tongs and Hot Workpiece items carry
`immersive_smithing.HeldWorkpiece` with `Version`, `TargetStack`, `RecipeId`, `MaterialFamily`, `MetalUnits`,
`ForgeScore`, `AnvilScore`, `Faulty`, `State` (`FORGED` or `SHAPED`) and `AnvilPattern`.

## Events

`com.otectus.immersivesmithing.api.event.ItemSmithedEvent` is posted on the Forge bus (server side, not cancellable)
when a smith quenches a piece, after quality and the maker's mark are written and before the item reaches the
inventory. It carries the smith, the live result stack (changes stay on the item), the `QualityData`, the smithing
recipe id, the material family, the units consumed and the trough position. KubeJS scripts can subscribe with
`ForgeEvents.onEvent('com.otectus.immersivesmithing.api.event.ItemSmithedEvent', ...)`. With
`integrations.fireVanillaCraftEvent` (default on) the vanilla `PlayerEvent.ItemCraftedEvent` is fired too, with a
one-slot container, so mods that reward crafting see forged items.

## Java API

`com.otectus.immersivesmithing.api.ImmersiveSmithingAPI`. Register during `FMLCommonSetupEvent`, or send an IMC message to
`immersive_smithing` whose payload is a `Supplier` of the interface.

| Interface | IMC method | Purpose |
|---|---|---|
| `ISmithingMaterialProvider` | `material_provider` | Adds material definitions, resolved like datapack files. |
| `ISmithingRecipeProvider` | `recipe_provider` | Adds smithing recipes; they rank with explicit recipes. |
| `ISmithingEfficacyHandler` | `efficacy_handler` | Replaces the default attribute and mining-speed changes for items it claims. |
| `IShieldSmithingHandler` | `shield_handler` | Applies efficacy to shields (neutral by default). |
| `IEquipmentClassifier` | `equipment_classifier` | Chooses the anvil pattern for an item. |
| `IRecyclingValueProvider` | `recycling_provider` | Supplies metal content when nothing else does. |
| `Predicate<ItemStack>` | `exclusion` | Excludes items from detection, suppression, recycling and quality. |

```java
InterModComms.sendTo("immersive_smithing", ImmersiveSmithingAPI.IMC_CLASSIFIER,
        () -> (IEquipmentClassifier) stack -> stack.is(MY_GLAIVES) ? new ResourceLocation("immersive_smithing", "generic_weapon") : null);
```

## Commands

Operators (permission level 2):

- `/immersivesmithing reload` - full datapack reload.
- `/immersivesmithing material <item>` - family, units and the rule that matched, or the recycling value.
- `/immersivesmithing recipe <item>` - smithing recipes for the item and the conventional recipes they disabled.
- `/immersivesmithing quality` - quality and multipliers of the held item.
- `/immersivesmithing report` - writes `logs/immersive_smithing_report.txt` with every detection decision.
- `/immersivesmithing session` - active minigame sessions.
