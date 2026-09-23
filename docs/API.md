# Ote's Smithing - Datapack and Java API

Ote's Smithing is configured almost entirely through datapacks. The Java API exists for items that datapacks
cannot describe. Everything below is re-read on `/reload` (or `/otessmithing reload`).

## Material units

All metal accounting uses integers: nugget = 1, ingot or raw ore = 9, storage block = 81.

## Smithing recipes

Recipe type `otes_smithing:smithing`. Recipes are ordinary Minecraft recipes, so they live in
`data/<namespace>/recipes/` (any subfolder) and support Forge `conditions`, for example `forge:mod_loaded`.
They are synced to clients and shown in JEI.

```json
{
  "type": "otes_smithing:smithing",
  "material": "examplemod:steel",
  "metal_units": 27,
  "auxiliary": [
    { "ingredient": { "item": "minecraft:stick" }, "count": 2 }
  ],
  "result": { "item": "examplemod:steel_pickaxe" },
  "forge_pattern": "otes_smithing:standard",
  "anvil_pattern": "otes_smithing:pickaxe"
}
```

| Field | Required | Notes |
|---|---|---|
| `material` | yes | Material family id. A recipe whose family does not exist is deactivated and reported. |
| `metal_units` | yes | Positive integer. |
| `auxiliary` | no | Non-metal ingredients taken from the smith's inventory when forging starts. `count` defaults to 1. |
| `result` | yes | Standard item-stack JSON; `nbt` is kept on the finished item. |
| `forge_pattern` | no | Defaults to `otes_smithing:standard`. |
| `anvil_pattern` | no | Defaults to the closest pattern family for the result item. |

An explicit recipe always wins over automatic detection for the same result item. Results tagged
`otes_smithing:non_smithable_equipment` are ignored.

## Material families

`data/<namespace>/otes_smithing/materials/<name>.json`

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
  "required_fuel_tag": "otes_smithing:netherite_fuels",
  "tint": "#FF8A3D"
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

Files support Forge `conditions`. An item claimed by two families is ignored and reported as ambiguous.

When `autoDetectModdedMetals` is on, every `forge:ingots/<metal>` tag that no definition covers becomes the
family `forge:<metal>`, with nuggets (1), storage blocks (81), raw materials (9) and raw storage blocks (81)
taken from the matching `forge:` tags. Its display name uses the key `material.forge.<metal>`.

## Recycling overrides

`data/<namespace>/otes_smithing/recycling/<name>.json`

```json
{ "item": "examplemod:steel_greatsword", "family": "examplemod:steel", "units": 45 }
```

```json
{ "tag": "examplemod:relics", "recyclable": false }
```

Use `item` or `tag`. With `"recyclable": false` the items can never be melted down. Items tagged
`otes_smithing:non_recyclable` are excluded too.

Recycling values are resolved in this order: exclusions, explicit smithing recipes, recycling overrides, analysis
of the item's crafting recipe, then Java `IRecyclingValueProvider`s. The value returned is multiplied by
`recyclingEfficiency` and never exceeds the recognised value.

## Anvil patterns

`data/<namespace>/otes_smithing/anvil_patterns/<name>.json`. The pattern id is the file id.

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
`boots`, `shield`, `generic_weapon`, `generic_armor`, `generic_equipment` (all in the `otes_smithing` namespace).
A missing pattern falls back to `otes_smithing:generic_equipment`.

The Spartan Weaponry patterns are named after its weapon types (`otes_smithing:longsword`, `otes_smithing:javelin`,
`otes_smithing:heavy_crossbow`, ... plus `club` and `cestus`). Their `categories` are Spartan's per-type item tags
(`spartanweaponry:longswords`, ...), so add-ons that tag their weapons the same way get the matching pattern.

## Forge patterns

`data/<namespace>/otes_smithing/forge_patterns/<name>.json`

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
| `otes_smithing:smithable_equipment` | Always a candidate for automatic detection. |
| `otes_smithing:non_smithable_equipment` | Never detected, disabled or given a smithing recipe (explicit recipes included). |
| `otes_smithing:smithable_shields` | Shields that may be detected even if not predominantly metal. |
| `otes_smithing:non_smithable_shields` | Shields never redirected (contains `minecraft:shield`). |
| `otes_smithing:non_recyclable` | Never melted down. |
| `otes_smithing:forge_fuels` | Solid fuels the forge accepts (with a furnace burn time), plus `minecraft:lava_bucket`. |
| `otes_smithing:forge_igniters` | Items that ignite the forge (flint and steel, fire charge). |
| `otes_smithing:netherite_fuels` | Heat sources for netherite (lava only). |
| `otes_smithing:tongs`, `otes_smithing:hammers` | The smithing tools. |

## Automatic detection

With `autoDetectEquipment` on, each crafting recipe whose result is a single unstackable, damageable weapon, tool,
armor piece or metal shield is analysed. It becomes an automatic smithing recipe
(`otes_smithing:auto/<namespace>/<item>`) only if:

- every ingredient is entirely one metal family or entirely non-metal,
- exactly one metal family is used,
- no ingredient is itself damageable equipment,
- the result is not blacklisted (tag, recipe id, mod id, family),
- no explicit recipe exists for the result.

An ingredient that is itself a shield is dismissed when the result is a shield: a metal shield built on a wooden base
shield costs only its metal, and the base is not required. Everything else is skipped and listed by
`/otessmithing report`. Bows, crossbows, fishing rods and tridents are not candidates unless tagged
`otes_smithing:smithable_equipment`.

Smithing-table upgrades (`minecraft:smithing_transform`) whose result is candidate equipment without a smithing
recipe are redirected too, when the addition is a single metal family (for example netherite from any mod). The
template is never required. The cost is the base item's shape in the addition's metal: the base's own smithing recipe
if it has one (units scaled by the addition's unit value), otherwise its crafting recipe, where ingredients the base
accepts as repair material (or `forge:gems`/`forge:ingots`) are the material slots, other components stay auxiliary
and a base shield is dismissed. The recipe id is `otes_smithing:auto/<namespace>/<item>`.

## Built-in mod support

Recipes for other mods live under `data/otes_smithing/recipes/compat/<modid>/` and start with a
`forge:mod_loaded` condition, so they are inert without that mod.

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

## Item data

Finished items keep quality in the `otes_smithing` compound of the stack's tag:

```
otes_smithing: { Version: 1, Forged: 1b, ForgeScore: 94, AnvilScore: 82, Faulty: 0b, Quality: "fine", DurabilityCarry: 0.4d }
```

A stack without this compound behaves exactly like Standard quality. Tongs and Hot Workpiece items carry
`otes_smithing.HeldWorkpiece` with `Version`, `TargetStack`, `RecipeId`, `MaterialFamily`, `MetalUnits`,
`ForgeScore`, `AnvilScore`, `Faulty`, `State` (`FORGED` or `SHAPED`) and `AnvilPattern`.

## Java API

`com.otectus.otessmithing.api.OtesSmithingAPI`. Register during `FMLCommonSetupEvent`, or send an IMC message to
`otes_smithing` whose payload is a `Supplier` of the interface.

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
InterModComms.sendTo("otes_smithing", OtesSmithingAPI.IMC_CLASSIFIER,
        () -> (IEquipmentClassifier) stack -> stack.is(MY_GLAIVES) ? new ResourceLocation("otes_smithing", "generic_weapon") : null);
```

## Commands

Operators (permission level 2):

- `/otessmithing reload` - full datapack reload.
- `/otessmithing material <item>` - family, units and the rule that matched, or the recycling value.
- `/otessmithing recipe <item>` - smithing recipes for the item and the conventional recipes they disabled.
- `/otessmithing quality` - quality and multipliers of the held item.
- `/otessmithing report` - writes `logs/otes_smithing_report.txt` with every detection decision.
- `/otessmithing session` - active minigame sessions.
