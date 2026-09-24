# Immersive Smithing

**Metal equipment is forged, not crafted.** Melt metal in the Smith's Forge, shape it on the Smith's Anvil and
quench it in the Smith's Trough. Your skill in two short minigames decides how long your gear lasts and how well it
performs.

![The smithing stations](https://raw.githubusercontent.com/otectus/Smithing/main/docs/visual-overhaul/after/ultima/packtest_world.png)

## Forge it yourself

1. Load metal into the top of a **Smith's Forge** and fuel into the bottom, then light it.
2. When the metal has melted, use **Smithing Tongs** on the forge, pick an item and play the **Forge minigame**:
   stop the sweeping marker in the glowing zone.
3. Set the hot workpiece on a **Smith's Anvil** and play the **Anvil minigame** with a **Smithing Hammer**: strike
   each target as the ring closes on it.
4. Quench the workpiece in a **Smith's Trough** filled with water.

Stone tongs and a stone hammer are enough to start, and one item takes about a minute. The **Smithing Guide**, an
in-game book, explains every step and is handed out when you first get smithing tools.

![Choosing a recipe at the forge](https://raw.githubusercontent.com/otectus/Smithing/main/docs/visual-overhaul/after/packtest_forge_select.png)

![The Forge minigame](https://raw.githubusercontent.com/otectus/Smithing/main/docs/visual-overhaul/after/packtest_forge_play.png)

![The Anvil minigame](https://raw.githubusercontent.com/otectus/Smithing/main/docs/visual-overhaul/after/packtest_anvil.png)

## Skill makes better gear

- Your **Forge score** sets durability: from 75% to 125% of normal.
- Your **Anvil score** sets efficacy: attack damage for weapons, mining speed for tools and armor points for armor,
  from 85% to 115% of normal.
- Items are labelled **Crude**, **Standard**, **Fine** or **Masterwork**. Let the anvil timer run out and the item
  is **Faulty**.
- Refine finished gear at the **Smith's Grindstone** for experience levels, or melt old and enchanted gear back
  into the forge to recover its metal.
- Tongs and hammers come in stone, iron, diamond and netherite. Better tools give more time in the minigames, never
  better quality on their own.

Netherite gear is forged straight from netherite ingots, no template or diamond gear needed, and netherite melts
only with lava.

## Made for modpacks

- **Automatic support for other mods.** Weapons, tools, armor and metal shields crafted from a single metal become
  smithing recipes, and their crafting recipes are turned off. Modded metals are picked up from their
  `forge:ingots` tags, and recipes that are unclear are left alone.
- **Spartan Weaponry built in.** Every metal weapon of all 24 types, including throwing weapons, longbows and heavy
  crossbows, has its own recipe and anvil pattern. Thrown weapons and fired arrows carry the weapon's quality.
- **Metal shields** no longer need a wooden base shield, and smithing-table upgrades such as modded netherite gear
  are forged from the metal instead.
- **JEI** shows every smithing recipe in its own category.
- **Datapacks and an API** can add or override metals, recipes, recycling values and minigame patterns.
- Tested with Spartan Weaponry, Spartan Shields and Immersive Armors, and in a 350+ mod pack.

## Made for servers

- Minigames are checked and scored by the server, with latency compensation.
- Hoppers and pipes can feed forges and fill troughs, but every item is still made by a player.
- Toolsmiths, Weaponsmiths and Armorers trade smithing gear.
- A server config covers recipe replacement, detection, forge fuel and capacity, minigame times, quality ranges,
  recycling, the grindstone, villager trades and automation. Operators get `/immersivesmithing` commands that
  explain every detection decision.

## Accessibility

Client options for reduced screen shake and flashes, fewer particles, high-contrast minigames, larger or
colorblind-safe targets, timing cue sounds and numeric quality scores. None of them make scoring easier.

## Requirements

- Minecraft 1.20.1 with Forge 47 or later.
- Required on both the client and the server.
- Optional: Just Enough Items.

## Links

- [Source code](https://github.com/otectus/Smithing)
- [Issue tracker](https://github.com/otectus/Smithing/issues)
- [Changelog](https://github.com/otectus/Smithing/blob/main/CHANGELOG.md)
- [Datapack and API documentation](https://github.com/otectus/Smithing/blob/main/docs/API.md)

Licensed under GPL-3.0.
