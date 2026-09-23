# Ote's Smithing
## Implementation-Ready Design and Development Plan
### Target: Minecraft Java Edition 1.20.1, Forge

## 1. Project Summary

**Ote's Smithing** is a Forge 1.20.1 mod that replaces the ordinary crafting-table creation of metal equipment with a short, physical, skill-based smithing process.

The goal is not to simulate real metallurgy in exhaustive detail. The goal is to make metal equipment feel *crafted*.

A normal item should take roughly one minute to create, split between two short active minigames:

1. **Smith's Forge**
   - Load compatible metal into a visible 3D pile.
   - Add fuel below the forge.
   - Ignite the fuel.
   - Wait for the metal to melt.
   - Use Smithing Tongs to choose an item and perform the Forge minigame.
   - Forge performance determines the item's **durability quality**.

2. **Smith's Anvil**
   - Carry the hot unfinished workpiece using Smithing Tongs.
   - Place it on the anvil.
   - Use a Smithing Hammer to begin the Anvil minigame.
   - Strike highlighted locations in a timed pattern.
   - Anvil performance determines the item's **efficacy quality**.

3. **Smith's Trough**
   - Pick the hammered workpiece back up with tongs.
   - Quench it in a water-filled Smith's Trough.
   - The final usable item is produced.

The mod should automatically support as much modded metal equipment as reasonably possible while providing a first-class datapack API for explicit compatibility.

The long-term direction is an RPG crafting system, but the initial implementation must remain easy to understand, quick to use and compatible with normal Minecraft gameplay.

---

# 2. Core Design Principles

## 2.1 Simple Process, Meaningful Interaction

The complete active process should remain:

**Load → Melt → Forge → Hammer → Quench → Use**

Avoid adding unnecessary intermediate stations, temperature simulation, multi-part armor assembly or metallurgy bookkeeping.

## 2.2 Player Skill Matters

The player should be able to become genuinely better at smithing.

Equipment quality should not be random.

The two active minigames determine two separate characteristics:

- **Forge score** → durability
- **Anvil score** → efficacy

Better smithing equipment gives the player more time and more durability on the smithing tools themselves, but it does not directly grant better item quality.

A sufficiently skilled player should be capable of producing Masterwork equipment even with basic smithing tools.

## 2.3 Failures Are Costly in Time, Not Materials

The player should not permanently lose valuable metal because of one bad minigame.

Poor performance produces poor equipment.

A timed-out Anvil job produces a **Faulty** item.

Faulty or undesirable items may be melted back down and reforged.

Normal reforging should preserve the metal value by default. Optional material-loss settings may exist for modpack authors.

## 2.4 Physical World Interaction First

Ordinary interactions should happen directly with blocks.

GUIs should be used only where they improve the minigames and recipe selection associated with those minigames.

The world should visibly communicate the smithing process through:

- placed metal piles
- fuel piles
- fire
- molten/ready state
- workpieces placed on anvils
- held tongs
- hammer swing animation
- sparks
- metal clangs
- quenching steam
- water consumption

## 2.5 Compatibility Is a Core Feature

Compatibility must be designed into the foundation instead of added later.

The system should support:

- vanilla metals
- modded metals
- modded weapons
- modded metal tools
- modded metal armor
- fully metallic shields
- custom datapack definitions
- tag-driven material families
- recipe-based automatic detection
- blacklists and overrides
- JEI

---

# 3. Initial Release Scope

## Required Blocks

- Smith's Forge
- Smith's Anvil
- Smith's Trough
- Smith's Grindstone

## Required Smithing Equipment

- Smithing Tongs
- Smithing Hammer

Each smithing tool should have material tiers:

- Stone
- Iron
- Diamond
- Netherite

Additional tiers should be data-driven where practical so integrations can add modded equivalents later.

## Required Systems

- material-family registry
- Forge material storage
- visible Forge piles
- fuel handling
- Forge ignition
- melting progress
- Forge recipe selection
- Forge minigame
- portable hot workpieces via tongs
- Anvil workpiece placement
- Anvil minigame
- quenching
- quality data
- dynamic item-stat modification
- equipment recycling/reforging
- automatic recipe detection
- datapack recipe API
- configuration
- JEI integration
- guide book
- villager integration
- moderate automation
- multiplayer synchronization
- server-authoritative validation

---

# 4. Explicit Non-Goals for 1.0

Do not implement the following in the first release unless required by architecture:

- numeric temperature
- cooling over time
- multiple heat stages
- oil quenching
- tempering
- metal alloy creation
- blade geometry customization
- armor plate assembly
- modular weapon parts
- attack-speed customization
- custom enchantment replacement
- custom repair system
- automated minigames
- NPC simulation of the minigames
- advanced metallurgy properties
- per-metal hardness systems
- custom netherite smithing templates
- permanent material destruction from normal minigame failure

Architect the system so some of these can be added later without a rewrite.

---

# 5. Canonical Gameplay Loop

## 5.1 Load the Smith's Forge

The player right-clicks the upper metal area of the Smith's Forge while holding a valid metal source.

Accepted sources may include:

- raw metal
- ingots
- nuggets
- storage blocks
- recognized metal equipment
- explicitly tagged or datapack-defined material sources

The first accepted item establishes the Forge's active **Material Family**.

Example:

- first item: Iron Ingot
- active family becomes `iron`
- Raw Iron is accepted
- Iron Nuggets are accepted
- Iron Blocks are accepted
- an Iron Sword may be accepted as recyclable iron
- Gold Ingots are rejected until the iron batch is emptied

All deposited material is represented as a visible 3D pile.

The pile does not need to render every item literally after large quantities. Use staged visual levels for performance.

Recommended visual stages:

- 1 item
- small pile
- medium pile
- large pile
- full pile

The Forge should have a configurable maximum material capacity.

## 5.2 Add Fuel

The lower portion of the block has a separate interaction region for fuel.

Default valid fuels:

- wooden fuel items accepted by the normal furnace fuel rules
- charcoal
- coal
- lava

Fuel should also be visually represented below the Forge.

The block must distinguish upper material interaction from lower fuel interaction based on hit position.

## 5.3 Ignite the Forge

Normal fuels do not begin melting material immediately.

The player must ignite the lower fuel area.

Default ignition methods:

- Flint and Steel
- Fire Charge

Provide a tag or integration hook for additional ignition items.

For normal fuel:

- fuel present + material present + valid ignition = active Forge

For lava:

- lava acts as an already-active heat source
- no manual ignition is required

## 5.4 Netherite Special Rule

Netherite equipment is treated like ordinary smithable metal equipment except for its heat source.

Requirements:

- no Netherite Upgrade Smithing Template
- no vanilla Smithing Table upgrade process required by Ote's Smithing recipes
- Netherite metal can be forged into items directly
- **lava is required as the Forge fuel/heat source for netherite**
- lava does not need ignition

This should be configurable as a material rule rather than hardcoded directly into every Netherite recipe.

The material definition should support:

```json
{
  "required_fuel_tag": "otes_smithing:netherite_fuels",
  "requires_ignition": false
}
```

The default Netherite fuel tag should contain lava-compatible input only.

## 5.5 Melt the Batch

The Forge uses simple melt progress.

Do not expose numeric temperature.

Each Material Family may define a melt-duration multiplier.

This exists solely to support the requested variation in heating speed.

Do not expand this into a full metallurgy-stat system in 1.0.

Suggested baseline:

- base melt time: configurable
- family multiplier: defaults to `1.0`
- material definitions may override

When the batch reaches 100 percent:

- the Forge enters `READY`
- deposited items become stored material units
- the visual should change from a solid pile to an appropriate glowing/molten/ready presentation
- a sound cue should make readiness obvious

The player should not need to watch a numeric timer.

## 5.6 Begin Forging

When the Forge is ready, right-clicking it with empty Smithing Tongs starts the forging interaction.

If no item can currently be produced, do not open the minigame.

Instead provide contextual feedback such as:

- not enough metal
- missing auxiliary component
- material unsupported
- Forge not ready

The Forge screen begins with recipe selection as part of the Forge minigame interface.

Recipe selection is not a separate workstation GUI.

## 5.7 Select the Intended Item

The selection view shows only recipes that:

- match the active Material Family
- have enough material units
- have all required non-metal components available
- are not disabled by configuration
- are valid on the current server

Examples:

- Iron Sword
- Iron Pickaxe
- Iron Helmet
- modded Steel Halberd
- modded Silver Chestplate

Provide:

- item icon
- item name
- metal requirement
- auxiliary requirements
- Forge material remaining after creation

A search field is strongly recommended when many compatible mods are installed.

## 5.8 Forge Minigame

After recipe selection, begin the Forge minigame.

Target duration:

- Stone Tongs: approximately 30 seconds
- Iron Tongs: approximately 35 seconds
- Diamond Tongs: approximately 40 seconds
- Netherite Tongs: approximately 45 seconds

All timings must be configurable.

### Recommended Minigame: Forming Rhythm

Use a simple precision/timing mechanic that can support every modded item without requiring handmade art.

Suggested behavior:

- several sequential forming phases
- each phase contains a moving indicator
- a highlighted target zone appears along the path
- the player performs the input when the indicator is inside the target
- proximity to the center determines accuracy
- missed phases score poorly but do not destroy the job
- larger or more complex items can have more phases
- the sequence should remain readable and learnable

The visual presentation can imply:

- turning the metal
- controlling the workpiece
- drawing the shape
- working the heated mass

Do not present this as numeric temperature management.

### Forge Score

Result:

`forgeScore = 0..100`

This score is stored on the unfinished workpiece and later becomes the durability modifier.

The server must calculate the authoritative score from validated inputs.

The client may animate predicted feedback but cannot submit an arbitrary final score.

### Forge Timeout

Running out of Forge time should not delete the material.

Recommended result:

- the current job completes with a Forge score of `0`
- an unfinished workpiece is still produced
- the player can continue to the Anvil
- resulting durability will be poor

This distinguishes bad Forge performance from the special Faulty result caused by failing to complete the Anvil shaping process.

## 5.9 Produce the Unfinished Workpiece

On completion:

- required metal units are consumed
- auxiliary ingredients are consumed
- remaining metal stays in the Forge
- the Tongs become loaded with the new hot workpiece

Do not place the unfinished item directly into the player's inventory.

The workpiece should be conceptually held by the tongs.

Recommended implementation:

The `ItemStack` for Smithing Tongs stores a serialized workpiece payload in NBT.

Example structure:

```text
otes_smithing:
  HeldWorkpiece:
    TargetStack: ...
    RecipeId: ...
    MaterialFamily: ...
    ForgeScore: 87
    AnvilScore: -1
    Faulty: false
    State: FORGED
```

This ensures that:

- unfinished workpieces cannot be equipped
- unfinished workpieces cannot be casually moved through inventory
- dropping the tongs preserves the workpiece
- another player may recover the tongs and continue the job
- the physical process remains clear

Use server-side validation when reading this data.

## 5.10 Carry to the Smith's Anvil

Loaded Tongs are required to transport the hot workpiece.

The workpiece remains hot indefinitely until quenched.

There is no cooling timer.

There is no temperature value.

The player right-clicks the top of the Smith's Anvil with loaded tongs.

Result:

- workpiece is transferred from tongs to the Anvil BlockEntity
- workpiece is rendered on top
- tongs become empty

## 5.11 Begin the Anvil Minigame

Right-click a Smith's Anvil containing a valid workpiece while holding a Smithing Hammer.

Open the Anvil minigame.

Target duration:

- Stone Hammer: approximately 30 seconds
- Iron Hammer: approximately 35 seconds
- Diamond Hammer: approximately 40 seconds
- Netherite Hammer: approximately 45 seconds

All values configurable.

## 5.12 Anvil Minigame

### Recommended Minigame: Precision Strike Pattern

Show a simplified workpiece silhouette or category-appropriate background.

A sequence of strike targets appears.

The player must click or activate the strike while targets are active.

Each target should score based on:

- position accuracy
- timing accuracy
- whether it was missed

Use an overall timer in addition to per-target timing.

### Pattern Families

At minimum provide patterns for:

- sword
- axe
- pickaxe
- shovel
- hoe
- generic tool
- helmet
- chestplate
- leggings
- boots
- shield
- generic weapon
- generic armor
- generic equipment

Pattern definitions should be data-driven.

A modded weapon should be mapped to the closest pattern family automatically, but datapacks must be able to override the selected pattern.

### Anvil Score

Successful completion produces:

`anvilScore = 0..100`

This controls efficacy.

### Anvil Timeout

If the overall timer expires before all required strikes are completed:

- mark the workpiece `Faulty = true`
- preserve it
- do not delete metal
- finish the shaping state
- leave the hot Faulty workpiece on the Anvil

Faulty is therefore a distinct failure state rather than merely the lowest score tier.

### Minigame Completion

After success or timeout:

- close the screen
- leave the workpiece visibly on top of the Anvil
- emit final clang/particle feedback
- require Tongs to remove it

## 5.13 Transfer to the Smith's Trough

Right-click the completed hot workpiece on the Anvil using empty tongs.

The Tongs pick it up.

Carry it to a Smith's Trough.

The Trough contains water.

Right-click the water-filled Trough with loaded Tongs.

## 5.14 Quench

Quenching has no minigame in 1.0.

It is a quick finalization interaction.

On quench:

- play strong steam particles
- play quenching sound
- reduce Trough water level
- convert workpiece into its target usable ItemStack
- attach smithing-quality data
- return the finished item to the player
- clear the Tongs workpiece payload

If inventory is full:

- drop the item safely at the Trough

Suggested water capacity:

- multiple quenches per full Trough
- visually represented water level
- refill using Water Buckets
- optionally accept compatible fluid insertion from pipes if enabled

The exact number of quenches per bucket should be configurable.

---

# 6. Material Family System

Compatibility depends on a robust material abstraction.

## 6.1 Material Family

A Material Family represents one smithable metal.

Examples:

- `minecraft:iron`
- `minecraft:gold`
- `minecraft:netherite`
- `examplemod:steel`
- `examplemod:silver`

A family defines:

- identifier
- accepted material sources
- material-unit values
- melt-time multiplier
- valid fuel rules
- whether ignition is required
- optional display name
- optional rendering tint
- optional default smithing category metadata

Do not encode equipment stats in the Material Family in 1.0.

## 6.2 Internal Material Units

Use integer units to avoid floating-point material accounting.

Recommended scale:

- Nugget: `1`
- Ingot: `9`
- Raw Material: `9`
- Storage Block: `81`

This naturally matches Minecraft's 9:1 compression.

Equipment recipes consume the total number of units represented by their metal ingredients.

Examples:

- Iron Sword: 18 iron units
- Iron Pickaxe: 27 iron units
- Iron Chestplate: 72 iron units

Auxiliary ingredients such as sticks are handled separately.

## 6.3 Recycling Existing Equipment

Existing metal equipment may be inserted into the Forge if its material composition can be resolved.

Default behavior:

- recover 100 percent of recognized metal units
- discard enchantments
- discard smithing-quality bonuses
- discard repair cost
- discard arbitrary transient state unless explicitly preserved by an integration

This allows players to reforge unwanted or Faulty equipment without losing expensive metal.

Provide a server config:

```text
recyclingEfficiency = 1.0
```

Modpacks may reduce this value if desired.

Never allow recycling to return more metal than the recognized source recipe required.

## 6.4 Resolving Existing Equipment Value

Priority:

1. explicit Ote's Smithing recipe/material definition
2. explicit recycling override
3. original crafting recipe analysis
4. known vanilla mapping
5. auto-detection
6. reject if value cannot be safely determined

Do not guess a metal value from item name alone.

Log ambiguous cases at debug level.

Provide an optional command or debug report to list unresolved equipment.

---

# 7. Automatic Modded Equipment Detection

This is a major feature and should be implemented conservatively.

## 7.1 Candidate Equipment Detection

An item may be a candidate if it is recognized as:

- a weapon
- a tool
- armor
- a fully metallic shield
- an item explicitly included in `otes_smithing:smithable_equipment`

Use multiple signals:

- vanilla item class inheritance where available
- Forge/common equipment tags
- Ote's Smithing inclusion tags
- recipe structure
- explicit compatibility definitions

Do not require every mod to subclass vanilla equipment classes correctly.

## 7.2 Recipe Analysis

During recipe reload:

1. scan candidate crafting recipes
2. inspect each ingredient
3. map ingredient possibilities to Material Families
4. identify the primary metal family
5. identify non-metal auxiliary ingredients
6. reject ambiguous multi-metal recipes unless explicitly defined
7. calculate material-unit cost
8. generate an in-memory smithing recipe

Example:

```text
Steel Sword
2 × Steel Ingot
1 × Stick
```

Generated smithing recipe:

```text
Material Family: steel
Metal Cost: 18
Auxiliary: 1 × Stick
Pattern: sword
Result: Steel Sword
```

## 7.3 Mixed Material Recipes

One metal family plus non-metal components is supported.

Examples:

- steel + stick
- silver + leather
- iron + gem
- bronze + wooden handle

Multiple independent metal families in the same recipe should not be auto-generated in 1.0.

Examples to skip automatically:

- iron + copper hybrid item
- steel + silver composite item

These can be supported through explicit datapack recipes later.

An alloy such as bronze is treated as its own family if the installed mod already provides Bronze Ingots.

Ote's Smithing does not manufacture bronze from copper and tin in 1.0.

## 7.4 Auxiliary Ingredients

Non-metal recipe ingredients remain required.

They should be automatically consumed from the player's inventory when a Forge job begins.

The recipe-selection view must show them.

Examples:

- sticks
- leather
- gems
- bindings
- special cores

Use a transactional reservation system.

Recommended flow:

1. player selects recipe
2. server validates material and auxiliary ingredients
3. auxiliary ingredients are moved into an escrow object tied to the active Forge session
4. Forge metal units are reserved
5. minigame begins
6. success/timeout consumes reserved inputs and produces workpiece
7. cancellation/disconnection safely returns escrowed auxiliary items
8. unrecoverable inventory overflow drops refunded items at the Forge

Persist active escrow if necessary to avoid item loss during server restart.

## 7.5 Detection Safeguards

Provide:

- global auto-detection toggle
- item blacklist tag
- item whitelist tag
- recipe blacklist
- material-family blacklist
- per-mod blacklist
- datapack override precedence
- debug logging
- generated compatibility report

Suggested config:

```text
autoDetectEquipment = true
autoDetectModdedMetals = true
replaceDetectedCraftingRecipes = true
```

---

# 8. Datapack API

Datapack support is required from the beginning.

## 8.1 Explicit Smithing Recipe

Suggested location:

```text
data/<namespace>/otes_smithing/recipes/<name>.json
```

Conceptual format:

```json
{
  "type": "otes_smithing:smithing",
  "material": "examplemod:steel",
  "metal_units": 27,
  "auxiliary": [
    {
      "ingredient": {
        "item": "minecraft:stick"
      },
      "count": 2
    }
  ],
  "result": {
    "item": "examplemod:steel_pickaxe",
    "count": 1
  },
  "forge_pattern": "otes_smithing:standard",
  "anvil_pattern": "otes_smithing:pickaxe"
}
```

The implementation should use a registered custom Recipe type and RecipeSerializer so recipes participate naturally in server datapack reload and synchronization.

## 8.2 Material Definition

Suggested location:

```text
data/<namespace>/otes_smithing/materials/<name>.json
```

Conceptual format:

```json
{
  "family": "examplemod:steel",
  "display_name": "Steel",
  "sources": [
    {
      "tag": "forge:ingots/steel",
      "units": 9
    },
    {
      "tag": "forge:nuggets/steel",
      "units": 1
    },
    {
      "tag": "forge:storage_blocks/steel",
      "units": 81
    },
    {
      "tag": "forge:raw_materials/steel",
      "units": 9
    }
  ],
  "melt_time_multiplier": 1.0,
  "requires_ignition": true
}
```

## 8.3 Pattern Definition

Anvil patterns should be data-driven where practical.

Conceptual fields:

- pattern id
- normalized target coordinates
- target radius
- target lifetime
- sequence order
- base time
- optional item-category tags

This allows resource/datapack creators to create custom patterns without Java integrations.

---

# 9. Vanilla Recipe Replacement

## 9.1 Default Behavior

By default, ordinary crafting recipes for detected smithable metal equipment are disabled.

The player must use Ote's Smithing.

## 9.2 Configurable Compatibility Mode

Provide a server option:

```text
allowVanillaCraftingAlongsideSmithing = false
```

If enabled:

- leave original recipes untouched
- Ote's Smithing remains available as an alternative
- crafted items from ordinary recipes should receive Standard-quality stats unless configured otherwise

Recommended default for non-smithing-created metal equipment:

**Standard**

This prevents compatibility problems with:

- loot
- commands
- mod machines
- villager trades
- structure generation
- quest rewards

Do not assume every instance of a smithable item must originate from Ote's Smithing.

## 9.3 Recipe Removal Strategy

Do not destructively modify other mods' source data.

At recipe reload, use the Forge-compatible runtime recipe filtering/replacement approach appropriate to Forge 1.20.1.

Keep explicit records of recipes hidden or disabled by Ote's Smithing for:

- debugging
- config reload
- JEI presentation
- compatibility reporting

---

# 10. Quality System

## 10.1 Two Independent Scores

Every forged item stores:

```text
ForgeScore: 0..100
AnvilScore: 0..100
Faulty: boolean
```

Forge and Anvil scores are independent.

## 10.2 Overall Quality Labels

Recommended default labels:

- Faulty
- Crude
- Standard
- Fine
- Masterwork

Suggested score ranges when not Faulty:

| Overall score | Quality |
|---:|---|
| 0-34 | Crude |
| 35-69 | Standard |
| 70-89 | Fine |
| 90-100 | Masterwork |

Overall score:

```text
round((ForgeScore + AnvilScore) / 2)
```

Faulty always overrides the calculated label.

## 10.3 Standard Is the Vanilla Baseline

A score around the center of Standard should reproduce the item's ordinary unmodified statistics.

This is important for compatibility and balance.

Recommended normalization point:

```text
50 = vanilla-equivalent performance
```

## 10.4 Durability Scaling

Recommended continuous default:

```text
durabilityMultiplier = 0.75 + (ForgeScore × 0.005)
```

Examples:

| Forge Score | Durability |
|---:|---:|
| 0 | 75% |
| 25 | 87.5% |
| 50 | 100% |
| 75 | 112.5% |
| 100 | 125% |

Faulty override:

```text
durabilityMultiplier = 0.50
```

This gives meaningful rewards without making ordinary equipment obsolete.

All multipliers configurable.

## 10.5 Efficacy Scaling

Recommended continuous default:

```text
efficacyMultiplier = 0.85 + (AnvilScore × 0.003)
```

Examples:

| Anvil Score | Efficacy |
|---:|---:|
| 0 | 85% |
| 25 | 92.5% |
| 50 | 100% |
| 75 | 107.5% |
| 100 | 115% |

Faulty override:

```text
efficacyMultiplier = 0.70
```

## 10.6 Efficacy by Equipment Type

### Weapons

Affect:

- attack damage

Do not affect attack speed in 1.0.

### Mining Tools

Affect:

- mining speed

Optionally apply a smaller configurable portion to attack damage.

Recommended default:

- full efficacy multiplier to mining speed
- half-strength efficacy deviation to tool attack damage

### Armor

Affect:

- armor value

Optionally affect armor toughness when the base item already has toughness.

Do not create toughness from nothing by default.

### Shields

Vanilla shields do not expose an ordinary "defense amount" stat comparable to armor.

Recommended efficacy behavior:

- quality modifies recovery/cooldown behavior when a shield is disabled, where technically safe
- if compatibility risk is high, leave shield efficacy neutral in 1.0 and let Forge score affect durability only
- modded shields with explicit supported attributes may receive custom efficacy integrations

Do not invent invasive combat rewrites just to force shield efficacy.

Expose a compatibility hook for shield mods.

## 10.7 Tooltips

Normal tooltip:

```text
Quality: Fine
```

Advanced tooltip while holding Shift:

```text
Durability Quality: Masterwork (94)
Efficacy Quality: Fine (82)
Forged by Ote's Smithing
```

Faulty:

```text
Quality: Faulty
This item was improperly shaped.
```

Provide config to hide raw numeric scores if desired.

---

# 11. Applying Quality to Arbitrary Modded Items

This is one of the most important technical challenges.

Do not create separate item IDs for every quality tier.

Store quality per ItemStack.

The target item remains the original vanilla or modded item.

## 11.1 Recommended Data

Use a namespaced compound:

```text
otes_smithing:
  Version: 1
  Forged: true
  ForgeScore: 94
  AnvilScore: 82
  Faulty: false
  Quality: "fine"
```

Do not overwrite another mod's root NBT.

## 11.2 Durability

Avoid replacing the item's base max-durability definition globally.

Implement per-stack durability scaling using the least invasive Forge 1.20.1 hook available after API verification.

Requirements:

- existing modded item remains the same Item
- quality changes effective max durability for that stack
- current damage scales safely when the item is created or reloaded
- Mending and Unbreaking continue to function
- no negative or zero maximum durability
- items with no durability are ignored

If direct dynamic maximum durability proves incompatible with arbitrary modded items, use a durability-damage conversion layer that adjusts incoming item damage while preserving the original maximum durability.

Example fallback:

- +25% effective durability means some durability damage is probabilistically or accumulatively absorbed
- -25% effective durability means additional damage is accumulated

Prefer deterministic fractional accumulation stored in NBT over random chance.

## 11.3 Attributes and Combat

Use Forge item-attribute/event hooks to add a per-stack difference from the item's ordinary attributes rather than replacing the entire modifier map.

Preserve:

- original modded attributes
- enchantment behavior
- equipment-slot restrictions
- custom item logic

Do not hardcode vanilla base attack values.

## 11.4 Mining Speed

Apply quality through the Forge block-break/mining-speed event path only while the relevant forged stack is the active tool.

Use the item's normal computed destroy speed as the baseline.

Multiply the baseline rather than replacing it.

## 11.5 Armor

Apply only the delta required by quality.

Preserve mod-added armor attributes.

Do not remove or reconstruct another mod's attribute map.

## 11.6 Compatibility Fallback

If Ote's Smithing cannot safely modify a particular item's efficacy:

- preserve the item
- preserve durability quality
- show a debug warning only when debug mode is enabled
- allow a datapack or integration plugin to define the correct behavior

Never crash or corrupt the stack because an exotic modded item uses unusual combat logic.

---

# 12. Smithing Equipment Tiers

## 12.1 Smithing Tongs

Tiers:

- Stone
- Iron
- Diamond
- Netherite

Functions:

- begin Forge minigame
- hold workpiece
- move workpiece from Forge to Anvil
- move workpiece from Anvil to Trough

Suggested default properties:

| Tier | Forge Time | Durability |
|---|---:|---:|
| Stone | 30s | 512 |
| Iron | 35s | 1024 |
| Diamond | 40s | 2048 |
| Netherite | 45s | 4096 |

Values configurable.

## 12.2 Smithing Hammer

Tiers:

- Stone
- Iron
- Diamond
- Netherite

Suggested default properties:

| Tier | Anvil Time | Durability |
|---|---:|---:|
| Stone | 30s | 512 |
| Iron | 35s | 1024 |
| Diamond | 40s | 2048 |
| Netherite | 45s | 4096 |

## 12.3 Tool Wear

Smithing equipment should be long-lasting.

Suggested wear:

- Tongs: 1 durability per Forge job plus 1 per completed transfer cycle
- Hammer: 1 durability per completed Anvil job

Avoid consuming durability per individual minigame click.

## 12.4 Repair

Smithing tools can be repaired using their base tier material.

Recommended:

- anvil repair remains valid
- crafting repair may be supported
- Mending may work if the item is enchantable under normal rules

Do not create a separate maintenance system.

## 12.5 Tier Effect

Higher tiers:

- increase allowed minigame time
- increase smithing-tool durability

Higher tiers do **not**:

- add free quality points
- widen target zones by default
- guarantee Masterwork
- alter the item's final stats directly

---

# 13. Smith's Forge

## 13.1 Block State

Suggested states:

- inactive
- burning
- ready

Optional properties:

- `LIT`
- `READY`

Do not store large data in BlockState.

Use a BlockEntity.

## 13.2 BlockEntity Data

Suggested fields:

```text
MaterialFamily activeFamily
int materialUnits
List<DisplayedDeposit> deposits or visual summary
ItemStack fuelVisual
int fuelTicksRemaining
int fuelTicksTotal
float meltProgress
ForgeState state
ActiveForgeSession activeSession
```

Keep exact inventory representation only if necessary.

Prefer converting inserted metal sources to units when safely possible while retaining enough information for cancellation/removal rules.

## 13.3 Extraction

Before ignition:

- player may remove deposited metal using an intuitive interaction such as Shift + empty-hand right-click
- remove most recently deposited stack first

After melting:

- do not allow direct recovery as original inputs
- molten material remains as units
- player can forge equipment from it

Optionally add a future "ingot mold" recipe if raw unit recovery is desired.

## 13.4 Visual Pile

Use a BlockEntityRenderer.

Avoid rendering one entity/model per deposited item.

Use a small fixed number of staged meshes or item-model instances.

Cap visual complexity.

## 13.5 Fuel Rendering

Likewise use staged representations:

- empty
- small fuel pile
- medium
- full

When lit:

- flames
- embers
- smoke

## 13.6 Sound

Suggested sound states:

- ignition
- fire loop
- low forge roar
- ready cue
- tongs grab
- metal movement

Looping sounds must stop immediately when the block unloads or state changes.

---

# 14. Smith's Anvil

## 14.1 Difference from Vanilla Anvil

The Smith's Anvil is a dedicated forging workstation.

It does not replace the vanilla Anvil's:

- repair UI
- enchantment combination
- renaming
- XP behavior

## 14.2 Workpiece Storage

The Anvil BlockEntity holds at most one active workpiece.

Suggested data:

```text
ItemStack serializedWorkpiece
UUID currentUser
boolean minigameActive
```

Ownership should not prevent other players from finishing the item after the active session ends.

## 14.3 Multiplayer Lock

While a player is actively using the Anvil minigame:

- lock the current workpiece
- reject attempts by other players to remove/start it

If the player disconnects:

- release the lock
- preserve the workpiece
- do not delete it

## 14.4 Rendering

Render the workpiece on top of the anvil.

No unique 3D model is required for every item in 1.0.

Use:

- generic heated-workpiece models by pattern category, or
- a flat/item-based representation with a glow treatment

The architecture should allow bespoke models later.

---

# 15. Smith's Trough

## 15.1 Purpose

The Trough is the final quenching station.

It does not cool items over time.

It does not contain a minigame.

## 15.2 Water

Support:

- Water Bucket insertion
- visible water level
- several quenches per fill

Recommended default:

- 4 quenches per bucket

Configurable.

Optionally expose Forge fluid capability for automation compatibility.

## 15.3 Invalid Interactions

Reject:

- empty tongs
- pre-Anvil workpieces
- ordinary items
- non-water fluid unless explicitly supported

## 15.4 Finalization

Quenching:

1. validates workpiece state
2. creates target output ItemStack
3. applies quality NBT
4. removes temporary workpiece metadata
5. transfers any explicitly preserved result NBT
6. plays sound/particles
7. consumes one water charge
8. returns finished item

---

# 16. Smith's Grindstone

## 16.1 Purpose

The Smith's Grindstone provides optional post-crafting refinement at an XP cost.

It should not replace the Forge or Anvil.

## 16.2 Function

Allow the player to choose one improvement:

- improve durability score
- improve efficacy score

Each operation costs XP levels.

Suggested behavior:

- +5 score per operation
- cost rises with current score
- cannot exceed 100
- cannot push an item beyond the natural Masterwork ceiling

## 16.3 Faulty Items

Faulty equipment cannot be repaired into normal quality using the Smith's Grindstone.

To recover it:

- melt it down
- forge it again

This preserves the consequence of failing the Anvil process.

## 16.4 Repeated Use

Hard cap:

```text
ForgeScore <= 100
AnvilScore <= 100
```

No infinite stat scaling.

---

# 17. Minigame Architecture

Both minigames must be server-authoritative.

## 17.1 Session Model

Create a common `SmithingSession` abstraction.

Suggested fields:

```text
sessionId
playerUUID
blockPos
dimension
recipeId
startGameTime
allowedDuration
toolTier
seed
phase
inputEvents
reservedInputs
```

## 17.2 Server-Generated Seed

The server creates a random session seed.

The client generates target sequences deterministically from:

- session seed
- recipe pattern
- tool tier
- difficulty parameters

This reduces packet volume and prevents the client from choosing easy patterns.

## 17.3 Input Packets

The client sends events such as:

```text
ForgeAction(sessionId, phaseIndex, clientRelativeTime)
AnvilStrike(sessionId, targetIndex, normalizedX, normalizedY, clientRelativeTime)
```

The server validates:

- correct active session
- correct player
- valid block position
- player still near workstation
- tool still valid
- event order
- rate limits
- plausible timing
- target validity

Do not accept:

```text
finalScore = 100
```

from the client.

## 17.4 Latency Tolerance

Multiplayer must remain fair under ordinary latency.

Allow a small configurable timing tolerance.

Do not trust arbitrary client timestamps.

Use:

- server session start tick
- packet arrival tick
- bounded latency compensation

Avoid making perfect quality impossible at moderate ping.

## 17.5 Disconnects and Screen Closure

### Manual close

Recommended:

- pause/cancel the current attempt
- preserve/refund reserved ingredients safely
- do not consume metal if no workpiece was produced

### Disconnect during Forge minigame

- cancel session
- release Forge lock
- refund auxiliary escrow
- preserve Forge material

### Disconnect during Anvil minigame

- cancel active lock
- leave workpiece on Anvil
- player may retry

Do not automatically mark Faulty merely because someone disconnected.

Faulty should result from actually allowing the in-game timer to expire during an active attempt.

---

# 18. User Interface

## 18.1 Visual Language

The UI should feel like Minecraft, not a generic mobile rhythm game.

Use:

- dark iron/stone paneling
- warm forge glow
- restrained metallic trim
- large readable targets
- minimal text during active play

## 18.2 Forge Screen

Phases:

1. recipe selection
2. short ready transition
3. forming minigame
4. score/result

Display:

- selected item
- remaining time
- progress
- current accuracy feedback
- optional compact projected durability quality

Do not reveal formulas by default.

## 18.3 Anvil Screen

Display:

- workpiece
- strike targets
- overall timer
- strike progress
- concise accuracy feedback

Avoid excessive particle clutter over hit targets.

## 18.4 Accessibility

Provide client settings for:

- reduced screen shake
- reduced flashes
- reduced particles
- larger minigame targets
- high-contrast targets
- sound cue volume
- optional timing cue sound
- colorblind-safe target mode

Accessibility options must not increase the scoring window unless server config explicitly permits it.

---

# 19. Animation and Feedback

## 19.1 Forge

During minigame:

- tongs animate in first/third person
- Forge emits periodic sparks
- metal manipulation sounds correspond with player actions

## 19.2 Anvil

Each accepted strike:

- swing hammer
- play clang
- spawn sparks
- provide small screen/UI feedback based on accuracy

Miss:

- duller sound
- reduced particles

Perfect:

- sharper clang
- stronger but not excessive spark burst

## 19.3 Trough

Quench:

- steam cloud
- hiss sound
- brief particle burst
- water level drops

## 19.4 Multiplayer Visibility

Nearby players should see:

- hammer swings
- sparks
- Forge active state
- Anvil workpiece
- Trough steam

They do not need to see another player's minigame GUI.

---

# 20. Villager Integration

## 20.1 Initial Integration

Support:

- Armorer
- Toolsmith
- Weaponsmith

Do not replace their existing job-site blocks in 1.0.

## 20.2 Trades

Potential trades:

- Smithing Hammers
- Smithing Tongs
- fuel
- metal
- forged equipment

Forged equipment sold by villagers may have randomized quality.

Recommended distribution should center around Standard and Fine.

Masterwork should be uncommon.

Faulty should not normally be sold unless intentionally enabled.

## 20.3 Guide/Flavor

Smith villagers may occasionally sell the Ote's Smithing guide book or related supplies.

## 20.4 Future Expansion

Later versions may allow villagers to:

- visibly use stations
- commission work
- train smithing
- offer higher-quality gear based on villager level

Do not simulate minigames for villagers in 1.0.

---

# 21. Automation

Automation should assist preparation but never replace the player's smithing skill.

## 21.1 Allowed

Configurable support for:

- hopper/pipe fuel insertion
- hopper/pipe metal insertion
- water/fluid insertion into Trough
- extraction of ordinary non-active inventory contents if applicable

## 21.2 Disallowed

Automation cannot:

- select a Forge recipe
- start Forge minigame
- submit Forge actions
- remove newly created hot workpiece from the Forge
- place active workpiece on the Anvil automatically
- perform Anvil strikes
- quench and finalize an item without player interaction by default

A player must complete both minigames.

## 21.3 Capability Exposure

Expose item/fluid capabilities only on logical faces.

Example:

- Forge top/sides: metal insertion
- Forge bottom/back: fuel insertion
- Trough: water insertion
- Anvil: no automation capability for active workpiece

All sided behavior configurable.

---

# 22. JEI Integration

Full JEI support is required.

Create an Ote's Smithing recipe category.

## 22.1 Recipe Display

Show the entire conceptual process:

```text
Metal + Auxiliary Components
        ↓
    Smith's Forge
        ↓
 Hot Workpiece
        ↓
    Smith's Anvil
        ↓
  Smith's Trough
        ↓
 Finished Equipment
```

Display:

- material family
- metal units / ingot equivalent
- auxiliary ingredients
- resulting item
- Forge pattern
- Anvil pattern
- quality explanation

## 22.2 Catalysts

Register:

- Smith's Forge
- Smith's Anvil
- Smith's Trough

as appropriate catalysts.

## 22.3 Automatic Recipes

JEI must show runtime-generated auto-detected recipes in addition to explicit datapack recipes.

## 22.4 Hidden Vanilla Recipes

If Ote's Smithing disables an original crafting recipe, JEI should not misleadingly present the disabled crafting-table recipe.

---

# 23. Guide Book

Include a dedicated in-game guide.

Do not require another mod for basic documentation.

Optional integration with guide-book mods can be added later.

## 23.1 Acquisition

Recommended:

- cheap craftable recipe
- automatically award one on the player's first smithing-related advancement
- configurable automatic grant

## 23.2 Chapters

1. Getting Started
2. Smith's Forge
3. Fuels and Ignition
4. Material Families
5. Forge Minigame
6. Smithing Tongs
7. Smith's Anvil
8. Anvil Minigame
9. Smith's Trough
10. Equipment Quality
11. Faulty Equipment
12. Reforging
13. Smith's Grindstone
14. Smithing Tool Tiers
15. Netherite
16. Modded Equipment
17. Automation
18. JEI
19. Troubleshooting

## 23.3 Contextual Links

When practical, allow JEI or station tooltips to point players toward the relevant guide section.

---

# 24. Configuration

Separate server/common gameplay config from client presentation config.

## 24.1 Server Config

Suggested settings:

```text
replaceMetalEquipmentRecipes = true
allowVanillaCraftingAlongsideSmithing = false

autoDetectEquipment = true
autoDetectModdedMetals = true

baseMeltTimeTicks = ...
forgeCapacityUnits = ...

stoneForgeTimeSeconds = 30
ironForgeTimeSeconds = 35
diamondForgeTimeSeconds = 40
netheriteForgeTimeSeconds = 45

stoneAnvilTimeSeconds = 30
ironAnvilTimeSeconds = 35
diamondAnvilTimeSeconds = 40
netheriteAnvilTimeSeconds = 45

minimumDurabilityMultiplier = 0.75
maximumDurabilityMultiplier = 1.25
faultyDurabilityMultiplier = 0.50

minimumEfficacyMultiplier = 0.85
maximumEfficacyMultiplier = 1.15
faultyEfficacyMultiplier = 0.70

recyclingEfficiency = 1.0

troughQuenchesPerBucket = 4

enableVillagerTrades = true
enableAutomation = true
enableSmithGrindstone = true

grantGuideBookAutomatically = true
```

## 24.2 Client Config

Suggested:

```text
showNumericQualityScores = false
reducedScreenShake = false
reducedFlashes = false
particleAmount = normal
highContrastMinigames = false
largeMinigameTargets = false
timingCueSounds = true
```

## 24.3 Datapack Precedence

Recommended precedence from highest to lowest:

1. explicit blacklist
2. explicit smithing recipe
3. explicit material definition
4. explicit inclusion tag
5. automatic detection
6. fallback unsupported

---

# 25. Networking

Use Forge's SimpleChannel-style networking for the 1.20.1 target.

Packets should be small and purpose-specific.

Potential packets:

### Server → Client

- open Forge session
- open Anvil session
- session seed/settings
- session state correction
- minigame result
- compatibility/material sync if required

### Client → Server

- select Forge recipe
- Forge minigame input
- Anvil minigame strike
- cancel session

Never allow client packets to:

- grant items
- set quality directly
- set material units
- choose arbitrary recipe output
- claim completion without server validation

Validate player distance before every consequential action.

---

# 26. Registration and Code Organization

Use standard Forge 1.20.1 registration patterns and `DeferredRegister` for static registries.

Recommended package structure:

```text
otes_smithing/
  OtesSmithing.java

  registry/
    ModBlocks.java
    ModItems.java
    ModBlockEntities.java
    ModMenus.java
    ModRecipeTypes.java
    ModRecipeSerializers.java
    ModSounds.java
    ModParticles.java

  block/
    SmithsForgeBlock.java
    SmithsAnvilBlock.java
    SmithsTroughBlock.java
    SmithsGrindstoneBlock.java

  blockentity/
    SmithsForgeBlockEntity.java
    SmithsAnvilBlockEntity.java
    SmithsTroughBlockEntity.java

  item/
    SmithingTongsItem.java
    SmithingHammerItem.java
    SmithingGuideItem.java

  material/
    MaterialFamily.java
    MaterialRegistry.java
    MaterialResolver.java
    MaterialUnitResolver.java

  recipe/
    SmithingRecipe.java
    SmithingRecipeSerializer.java
    AutoRecipeDetector.java
    SmithingRecipeManager.java

  workpiece/
    WorkpieceData.java
    WorkpieceState.java
    WorkpieceCodec.java

  quality/
    SmithingQuality.java
    QualityData.java
    QualityCalculator.java
    QualityHooks.java

  minigame/
    SmithingSession.java
    ForgeSession.java
    AnvilSession.java
    ForgePattern.java
    AnvilPattern.java
    SessionManager.java

  network/
    ModNetwork.java
    packet/

  client/
    screen/
      ForgeMinigameScreen.java
      AnvilMinigameScreen.java
    renderer/
      SmithsForgeRenderer.java
      SmithsAnvilRenderer.java
      SmithsTroughRenderer.java
    animation/
    sound/

  compat/
    jei/
    villager/
    integration/

  config/
    ServerConfig.java
    ClientConfig.java

  guide/
    GuideData.java
```

Keep gameplay logic out of client classes.

---

# 27. Persistence

All persistent workstation state must survive:

- chunk unload/reload
- server restart
- dimension change
- player disconnect

Forge persistence:

- active material family
- material units
- fuel
- melting progress
- ready state
- legitimate reserved session data

Anvil persistence:

- workpiece
- session lock state should expire safely after restart

Trough persistence:

- water amount

Tongs persistence:

- held workpiece

Use a version field for workpiece/quality NBT so future migrations are possible.

---

# 28. Multiplayer Rules

## 28.1 Shared Physical Work

A workpiece belongs to the item/station, not permanently to its creator.

Another player may:

- pick up dropped loaded tongs
- carry the workpiece
- place it on an Anvil
- finish the Anvil process
- quench it

This makes cooperative smithing possible.

## 28.2 Active Session Ownership

Only one player may actively control a Forge or Anvil session at a time.

Locks must expire if:

- player disconnects
- player dies
- player leaves allowed range
- session becomes invalid

## 28.3 Chunk Unload

Do not keep chunks force-loaded for smithing.

If an active station unloads:

- safely cancel interactive session
- persist material/workpiece
- refund escrow where appropriate

---

# 29. Death and Item-Drop Handling

Loaded Tongs are ordinary droppable items.

If the player dies:

- tongs drop with workpiece NBT intact
- workpiece is recoverable

If loaded tongs are destroyed by lava/fire:

- follow normal item-destruction rules unless Netherite Tongs are naturally resistant
- no special guaranteed recovery is required

The mod's "failure should not destroy materials" principle applies to smithing mechanics, not all environmental item destruction.

---

# 30. Reforging

Any recognized metal item can be returned to a compatible Forge and converted to material units.

This includes:

- Crude equipment
- Standard equipment
- Fine equipment
- Masterwork equipment
- Faulty equipment
- enchanted equipment

Reforging destroys:

- enchantments
- smithing quality
- custom name by default
- repair history

The resulting metal can then be used for a new item.

Provide datapack hooks for mods whose special items must not be recyclable.

---

# 31. Enchantments

Smithing itself does not preserve enchantments when an item is melted.

Finished forged items can be enchanted normally.

Quality and enchantments coexist.

Quality is a property of physical craftsmanship.

Enchantments are a separate vanilla/modded layer.

The Smith's Grindstone should not strip enchantments unless explicitly designed later.

---

# 32. Fully Metallic Shields

Do not automatically redirect the vanilla wooden Shield recipe.

A shield qualifies if:

- explicitly tagged as smithable, or
- recipe analysis identifies it as predominantly/meaningfully metal and it maps to a supported metal family, or
- a datapack recipe explicitly defines it

Provide:

```text
otes_smithing:smithable_shields
otes_smithing:non_smithable_shields
```

for compatibility authors.

---

# 33. Netherite Recipe Handling

Because vanilla Netherite equipment normally upgrades Diamond equipment, Ote's Smithing must deliberately define its own path.

Recommended behavior:

- Netherite Ingot is the recognized base metal source
- direct forging recipes are generated/defined for vanilla Netherite equipment
- auxiliary ingredients mirror the conceptual non-metal requirements where appropriate
- lava is mandatory heat source
- no template is required
- Diamond equipment is not required as an ingredient

Example default:

```text
Netherite Sword
Metal: 18 netherite units
Auxiliary: 1 Stick
Fuel: Lava
```

Balance numbers should be configurable via datapack if direct Netherite costs need adjustment.

Do not add scrap + gold alloying in 1.0. Vanilla Netherite Ingot creation can remain separate.

---

# 34. Advancements

Suggested advancement chain:

## The Trade
Obtain a Smithing Hammer or Smithing Tongs.

## Stoke the Forge
Ignite a Smith's Forge.

## Into Shape
Complete a Forge minigame.

## Hammer and Steel
Complete an Anvil minigame.

## Hiss
Quench a finished workpiece.

## Fine Work
Create a Fine item.

## Master Smith
Create a Masterwork item.

## That'll Buff Out
Create a Faulty item.

## Again
Reforge a previously forged item.

Avoid locking core functionality behind advancements.

---

# 35. Commands and Debugging

Add administrator/developer commands.

Suggested:

```text
/otessmithing reload
/otessmithing material <item>
/otessmithing recipe <item>
/otessmithing quality <held item>
/otessmithing report
/otessmithing session
```

### `material`

Reports:

- resolved family
- material units
- source rule
- ambiguity

### `recipe`

Reports:

- explicit or auto-generated
- metal family
- unit cost
- auxiliary ingredients
- pattern
- whether original recipe was suppressed

### `report`

Generate a log/report containing:

- discovered material families
- auto-detected equipment
- skipped ambiguous equipment
- disabled recipes
- unsupported modded equipment

This will be extremely valuable for compatibility bug reports.

---

# 36. Error Handling

Never silently consume valuable items when validation fails.

Examples:

### Wrong Metal Added to Forge

- reject insertion
- play failure sound
- display short action-bar message

### No Fuel

- cannot ignite
- explain reason

### Wrong Fuel for Netherite

- do not consume it as valid Netherite heat source
- explain that lava is required

### Loaded Tongs Used on Occupied Anvil

- reject transfer
- preserve workpiece

### Anvil Started Without Hammer

- do not open minigame

### Trough Empty

- do not consume workpiece
- action-bar message: fill with water

### Recipe Becomes Invalid During Datapack Reload

- preserve existing workpiece
- attempt to resolve stored target ItemStack
- if impossible, allow safe recycling back into material where possible
- log warning

---

# 37. Performance Requirements

The mod should be inexpensive when nobody is actively smithing.

## 37.1 Forge Ticking

Do not perform expensive registry or recipe scans every tick.

Precompute:

- material mappings
- smithing recipes
- item-to-material lookup
- item-to-smithing-recipe lookup

Forge tick should be approximately:

- update fuel
- update melt progress
- state transition

No global searches.

## 37.2 Rendering

Do not create ItemEntities for decorative piles.

Use BlockEntity rendering.

Cap render instances.

## 37.3 Auto-Detection

Run during:

- server/datapack recipe reload
- initial recipe/material build

Not during gameplay ticks.

Cache results immutably until next reload.

## 37.4 Networking

Do not stream cursor position every frame.

Send only meaningful minigame input events.

---

# 38. Security and Exploit Prevention

The server is authoritative for:

- Forge contents
- material units
- recipe eligibility
- fuel
- melt completion
- item selection
- input reservation
- minigame seed
- timing
- scoring
- output
- quality
- quenching

Reject:

- duplicate session packets
- stale session IDs
- impossible target indices
- out-of-order strikes
- impossible click frequency
- invalid block distance
- changed/removed tools
- altered target item IDs
- client-supplied quality values

Protect against duplication on:

- disconnect
- death
- chunk unload
- screen cancel
- server restart
- hopper interaction
- rapid block breaking
- simultaneous multiplayer use

---

# 39. Block Breaking Rules

## Smith's Forge

If broken while containing unmelted material:

- drop recoverable original deposits if they are still represented

If broken while containing molten material units:

Recommended:

- prevent breaking unless empty, or
- drop a persistent "Forge Contents" recovery object

The safer initial design is:

**Do not allow survival-mode breaking of a non-empty active/ready Forge without confirmation-like feedback.**

Action-bar:

```text
Empty the Smith's Forge before breaking it.
```

Admins/creative mode may bypass.

## Smith's Anvil

If broken with workpiece:

- drop a recoverable workpiece container/tongs-safe representation

Prefer simply dropping a special temporary workpiece item that cannot be used and must be picked up with tongs or returned to a station.

## Smith's Trough

Breaking drops the block and discards water like an ordinary fluid container unless implementation supports otherwise.

---

# 40. Compatibility API

Expose a small public Java API.

Potential interfaces:

```java
ISmithingMaterialProvider
ISmithingRecipeProvider
ISmithingEfficacyHandler
IShieldSmithingHandler
```

Provide registration events or IMC-style hooks for:

- custom material families
- equipment classifiers
- efficacy handlers
- pattern mapping
- recycling value
- exclusion

Datapacks should remain sufficient for ordinary integrations.

Java API exists for exotic items.

---

# 41. Suggested Tags

Examples:

```text
otes_smithing:smithable_equipment
otes_smithing:non_smithable_equipment
otes_smithing:smithable_shields
otes_smithing:non_smithable_shields
otes_smithing:forge_fuels
otes_smithing:forge_igniters
otes_smithing:netherite_fuels
otes_smithing:hammers
otes_smithing:tongs
```

Also consume conventional Forge material tags where present.

Do not invent duplicate material tags if a standard Forge tag already exists and is adequate.

---

# 42. Resource Pack and Localization

All player-facing strings must be localized.

Never construct English item names manually.

Add translation keys for:

- qualities
- errors
- minigame prompts
- guide headings
- JEI labels
- block states
- tooltips
- villager trades

Resource packs should be able to replace:

- minigame textures
- station textures
- pile models
- sounds where normal resource systems permit
- guide graphics

---

# 43. Implementation Phases

## Phase 1 - Foundation

Implement:

- project setup for Forge 1.20.1
- registries
- base blocks/items
- Smith's Forge BlockEntity
- Smith's Anvil BlockEntity
- Smith's Trough BlockEntity
- Smithing Tongs
- Smithing Hammer
- workpiece NBT model
- basic networking
- config foundation

Acceptance:

- blocks place correctly
- state persists across restart
- tongs safely store a test workpiece
- multiplayer sync is correct

## Phase 2 - Material System

Implement:

- Material Family
- material resolver
- unit accounting
- vanilla iron/gold/netherite
- raw/ingot/nugget/block handling
- equipment recycling
- same-family Forge restriction
- fuel
- ignition
- melt progress
- lava Netherite rule

Acceptance:

- Iron sources combine correctly
- Gold cannot enter an Iron batch
- values remain exact after save/reload
- Netherite refuses non-lava heat
- recycling cannot duplicate material

## Phase 3 - Forge Rendering and Interaction

Implement:

- upper/lower hit regions
- visible material pile
- visible fuel
- lit effects
- ready state
- sounds
- extraction before melting

Acceptance:

- physical state is obvious without GUI
- no excessive renderer cost
- fuel and material interactions cannot cross-contaminate

## Phase 4 - Explicit Smithing Recipes

Implement:

- custom Recipe
- serializer
- datapack loading
- vanilla equipment definitions
- auxiliary ingredients
- recipe selection inside Forge screen

Acceptance:

- datapack reload works
- recipes sync to clients
- malformed recipes fail gracefully
- recipe results are deterministic

## Phase 5 - Forge Minigame

Implement:

- Forge session
- deterministic server seed
- client screen
- timing input
- server scoring
- tier-based time limits
- cancellation
- escrow
- Forge score
- workpiece creation

Acceptance:

- client cannot submit arbitrary score
- disconnect does not lose resources
- Stone through Netherite timing works
- Forge score survives on tongs

## Phase 6 - Anvil Minigame

Implement:

- Anvil placement
- workpiece renderer
- pattern families
- Anvil session
- targets
- score calculation
- timeout → Faulty
- loaded-tongs retrieval

Acceptance:

- each vanilla category maps correctly
- timeout produces Faulty
- disconnect does not produce Faulty
- another player can continue after session release

## Phase 7 - Trough and Finalization

Implement:

- water fill
- quench interaction
- steam/sound
- final result creation
- quality NBT

Acceptance:

- unshaped workpiece cannot be quenched
- water is consumed correctly
- result matches recipe
- full inventory safely drops result

## Phase 8 - Quality Hooks

Implement:

- quality tooltip
- durability modifier
- weapon efficacy
- tool efficacy
- armor efficacy
- Faulty overrides
- Standard baseline
- enchantment coexistence

Acceptance:

- score 50 behaves like ordinary item
- score 100 matches configured maximum
- score 0 matches configured minimum
- Faulty matches configured penalty
- vanilla and representative modded items retain their original special behavior

## Phase 9 - Automatic Detection

Implement:

- recipe scanner
- material-family inference
- auxiliary ingredient extraction
- candidate-equipment classifier
- automatic in-memory recipes
- ambiguity detection
- blacklists
- debug report

Acceptance:

Test against several real Forge 1.20.1 content mods.

Verify:

- metal sword
- metal pickaxe
- armor
- custom weapon
- custom tool
- metallic shield
- mixed metal + stick recipe
- ambiguous two-metal recipe
- non-metal tool
- exotic item

No false-positive recipe removal should occur silently.

## Phase 10 - Recipe Replacement

Implement:

- default suppression of original crafting recipes
- config to preserve them
- Standard-quality fallback for externally created equipment

Acceptance:

- smithable metal equipment cannot be crafted normally by default
- config restores original recipe behavior
- loot/commands/trades still produce usable Standard items

## Phase 11 - JEI

Implement:

- plugin
- smithing category
- explicit recipes
- generated recipes
- catalysts
- material/auxiliary display
- hidden disabled crafting recipes where possible

Acceptance:

- every active smithing recipe is discoverable
- recipe display matches server rules

## Phase 12 - Smith's Grindstone

Implement:

- refinement interaction
- XP costs
- score cap
- Faulty restriction

Acceptance:

- no score exceeds 100
- XP cost is server-authoritative
- no duplication or enchantment loss

## Phase 13 - Villagers and Guide

Implement:

- smith villager trades
- guide book
- advancement acquisition
- basic tutorial flow

Acceptance:

- a new player can understand the complete process without external documentation

## Phase 14 - Automation and Capabilities

Implement:

- sided material insertion
- sided fuel insertion
- Trough fluid insertion
- config toggles

Acceptance:

- automation can prepare stations
- automation cannot bypass either minigame

## Phase 15 - Polish and Compatibility

Implement:

- final particles
- sound mix
- accessibility
- localization
- compatibility report
- API documentation
- crash hardening

---

# 44. Test Matrix

## 44.1 Forge

Test:

- empty
- one metal source
- mixed valid sources of same family
- invalid second family
- capacity full
- wood fuel
- charcoal
- coal
- lava
- no ignition
- invalid ignition
- valid ignition
- chunk unload
- server restart
- block break
- two players interact simultaneously

## 44.2 Workpiece

Test:

- tongs drop
- tongs move inventory slots
- player death
- player logout
- another player picks up
- place on Anvil
- retrieve from Anvil
- attempt normal use
- attempt enchantment
- attempt hopper movement

## 44.3 Minigames

Test:

- perfect
- average
- poor
- timeout
- close screen
- disconnect
- high latency
- duplicate packets
- out-of-order packets
- malicious impossible coordinates
- tool removed mid-session
- player walks away

## 44.4 Quality

Test each score boundary:

- 0
- 34
- 35
- 50
- 69
- 70
- 89
- 90
- 100
- Faulty

Across:

- sword
- pickaxe
- armor
- shield
- modded weapon
- modded tool
- modded armor

## 44.5 Compatibility

At minimum test against multiple Forge 1.20.1 mods containing:

- conventional `forge:ingots/<metal>` tags
- nonstandard metal tags
- custom weapon classes
- custom tool classes
- special armor
- custom recipe serializers
- custom NBT outputs
- non-metal equipment that must remain untouched

---

# 45. Acceptance Criteria for 1.0

The release is ready when all of the following are true.

## Gameplay

- A new player can forge Iron equipment from raw material or ingots.
- The normal active crafting process takes roughly 60-90 seconds total depending on tool tier and player speed.
- The only substantial minigames are Forge and Anvil.
- The Trough is quick and satisfying rather than tedious.
- Better performance reliably produces better equipment.
- Failure never silently deletes the metal.
- Faulty gear is meaningfully poor.
- Masterwork gear is desirable without invalidating enchantments.

## Compatibility

- Common modded metals can be detected through tags.
- Common modded metal equipment can be detected through recipes.
- Explicit datapack recipes override automatic behavior.
- Ambiguous recipes are skipped rather than guessed.
- Vanilla crafting replacement is configurable.
- JEI accurately displays the active smithing path.

## Multiplayer

- Scores are server-authoritative.
- Workpieces survive normal disconnects.
- Shared physical workpieces can be passed between players.
- No obvious duplication exists around cancellation, death, unload or automation.

## Technical

- No global per-tick recipe scanning.
- No per-item decorative entities for Forge piles.
- All workstation state persists.
- Client-only classes do not load on dedicated servers.
- Data has version fields for migration.
- Compatibility failures degrade gracefully.

---

# 46. Recommended First-Pass Balance

Use these only as starting values.

## Quality

| State | Durability | Efficacy |
|---|---:|---:|
| Faulty | 50% | 70% |
| Score 0 | 75% | 85% |
| Score 50 | 100% | 100% |
| Score 100 | 125% | 115% |

## Minigame Time

| Tool Tier | Forge | Anvil |
|---|---:|---:|
| Stone | 30s | 30s |
| Iron | 35s | 35s |
| Diamond | 40s | 40s |
| Netherite | 45s | 45s |

## Trough

- 4 quenches per Water Bucket

## Smith's Grindstone

- +5 relevant score per refinement
- escalating XP level cost
- score cap 100
- Faulty cannot be refined

## Recycling

- 100% metal recovery by default
- enchantments and prior quality destroyed

These values must remain server-configurable.

---

# 47. Design Decisions to Preserve During Implementation

The coding agent should treat the following as intentional requirements rather than implementation suggestions:

1. Target **Minecraft Forge 1.20.1**.
2. Metal equipment is smith-forged by default instead of conventionally crafted.
3. Vanilla recipes may be re-enabled through configuration.
4. Modded metals and modded metal equipment are first-class targets.
5. Forge and Anvil each use an active skill minigame.
6. Forge performance controls durability.
7. Anvil performance controls efficacy.
8. Anvil timeout creates Faulty equipment.
9. Smithing remains forgiving and should not destroy expensive material due to a bad attempt.
10. Smith's Tongs are required to begin forging and transport hot workpieces.
11. Smithing Hammers are required for the Anvil minigame.
12. Smithing tool tiers increase available minigame time and tool durability rather than directly increasing item quality.
13. There is no temperature simulation.
14. Hot workpieces do not cool over time.
15. The Smith's Trough uses water and finalizes the item.
16. The Trough has no minigame.
17. The Smith's Grindstone can improve durability or efficacy for XP but cannot exceed the natural quality ceiling.
18. Faulty equipment must be reforged rather than polished into good equipment.
19. Netherite does not require a Smithing Template in this system.
20. Netherite requires lava in the Forge.
21. Wooden vanilla shields are not the focus; fully metallic shields are.
22. GUIs are reserved for the skill interactions and their immediately necessary recipe selection.
23. Physical animation, particles and sound are important to the experience.
24. JEI support is required.
25. A dedicated guide book is required.
26. Villager integration is required but should remain lightweight in 1.0.
27. Moderate automation is allowed, but automation can never complete the skill process.
28. Existing equipment can be melted down and used as material for new equipment.
29. Reforging does not preserve enchantments.
30. The long-term design direction is RPG crafting, but the first release must remain simple.

---

# 48. Final Intended Player Experience

The player walks up to a Smith's Forge and drops iron into the top.

The iron physically piles inside.

They load coal beneath it and ignite the fire.

The Forge comes alive with flame, smoke and sound while the metal melts.

When it is ready, the player takes their Smithing Tongs and starts work.

They choose an Iron Sword and complete a short precision challenge. Their performance determines how durable that future sword will be.

The glowing unfinished sword is now held in their tongs.

They carry it to the Smith's Anvil and place it on top.

With a Smithing Hammer equipped, they begin shaping it.

Strike points appear over the workpiece. The player works through the pattern under a time limit. Clean, accurate strikes improve the weapon's efficacy. Running out of time does not erase the sword or the iron, but it does leave the result Faulty.

When the hammering ends, the glowing sword remains physically on the anvil.

The player grabs it with their tongs and brings it to a water-filled Smith's Trough.

They quench it.

Steam bursts upward.

The hiss fades.

The finished sword is placed into their hands with a quality that reflects how well they actually forged it.

That loop should be the identity of **Ote's Smithing**.
