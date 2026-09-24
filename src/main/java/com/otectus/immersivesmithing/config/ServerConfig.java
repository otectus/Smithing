package com.otectus.immersivesmithing.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Gameplay configuration. SERVER configs are per world and synced to clients, so the client can read
 * these values once it has joined a world. Every read goes through the helpers at the bottom, which fall
 * back to defaults while no world is loaded.
 */
public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue REPLACE_METAL_EQUIPMENT_RECIPES;
    public static final ForgeConfigSpec.BooleanValue ALLOW_VANILLA_CRAFTING_ALONGSIDE_SMITHING;
    public static final ForgeConfigSpec.BooleanValue SUPPRESS_SMITHING_TABLE_UPGRADES;
    public static final ForgeConfigSpec.BooleanValue AUTO_DETECT_EQUIPMENT;
    public static final ForgeConfigSpec.BooleanValue AUTO_DETECT_MODDED_METALS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AUTO_METAL_EXCLUSIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RECIPE_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MATERIAL_FAMILY_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOD_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;

    public static final ForgeConfigSpec.IntValue BASE_MELT_TIME_TICKS;
    public static final ForgeConfigSpec.IntValue FORGE_CAPACITY_UNITS;
    public static final ForgeConfigSpec.DoubleValue FUEL_CONSUMPTION_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue ACCEPT_ANY_FURNACE_FUEL;
    public static final ForgeConfigSpec.IntValue LAVA_BUCKET_HEAT_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_LAVA_BUCKETS;

    public static final ForgeConfigSpec.IntValue STONE_FORGE_TIME_SECONDS;
    public static final ForgeConfigSpec.IntValue IRON_FORGE_TIME_SECONDS;
    public static final ForgeConfigSpec.IntValue DIAMOND_FORGE_TIME_SECONDS;
    public static final ForgeConfigSpec.IntValue NETHERITE_FORGE_TIME_SECONDS;
    public static final ForgeConfigSpec.IntValue STONE_ANVIL_TIME_SECONDS;
    public static final ForgeConfigSpec.IntValue IRON_ANVIL_TIME_SECONDS;
    public static final ForgeConfigSpec.IntValue DIAMOND_ANVIL_TIME_SECONDS;
    public static final ForgeConfigSpec.IntValue NETHERITE_ANVIL_TIME_SECONDS;
    public static final ForgeConfigSpec.IntValue STONE_TOOL_DURABILITY;
    public static final ForgeConfigSpec.IntValue IRON_TOOL_DURABILITY;
    public static final ForgeConfigSpec.IntValue DIAMOND_TOOL_DURABILITY;
    public static final ForgeConfigSpec.IntValue NETHERITE_TOOL_DURABILITY;

    public static final ForgeConfigSpec.IntValue READY_DELAY_MS;
    public static final ForgeConfigSpec.IntValue TIMING_TOLERANCE_MS;
    public static final ForgeConfigSpec.IntValue MAX_LATENCY_COMPENSATION_MS;
    public static final ForgeConfigSpec.DoubleValue MAX_STATION_DISTANCE;

    public static final ForgeConfigSpec.DoubleValue MINIMUM_DURABILITY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MAXIMUM_DURABILITY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FAULTY_DURABILITY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MINIMUM_EFFICACY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MAXIMUM_EFFICACY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FAULTY_EFFICACY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue TOOL_ATTACK_EFFICACY_FACTOR;
    public static final ForgeConfigSpec.BooleanValue SCALE_ARMOR_TOUGHNESS;

    public static final ForgeConfigSpec.DoubleValue RECYCLING_EFFICIENCY;

    public static final ForgeConfigSpec.IntValue TROUGH_QUENCHES_PER_BUCKET;
    public static final ForgeConfigSpec.IntValue TROUGH_CAPACITY_BUCKETS;

    public static final ForgeConfigSpec.BooleanValue ENABLE_SMITH_GRINDSTONE;
    public static final ForgeConfigSpec.IntValue GRINDSTONE_SCORE_STEP;
    public static final ForgeConfigSpec.IntValue GRINDSTONE_BASE_LEVEL_COST;
    public static final ForgeConfigSpec.IntValue GRINDSTONE_SCORE_PER_EXTRA_LEVEL;

    public static final ForgeConfigSpec.BooleanValue ENABLE_VILLAGER_TRADES;
    public static final ForgeConfigSpec.BooleanValue ALLOW_FAULTY_VILLAGER_ITEMS;

    public static final ForgeConfigSpec.BooleanValue ENABLE_AUTOMATION;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FORGE_METAL_FACES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FORGE_FUEL_FACES;
    public static final ForgeConfigSpec.BooleanValue TROUGH_FLUID_INPUT;

    public static final ForgeConfigSpec.BooleanValue GRANT_GUIDE_BOOK_AUTOMATICALLY;

    public static final ForgeConfigSpec.BooleanValue SPARTAN_WEAPONRY_INTEGRATION;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Recipe replacement and automatic compatibility").push("recipes");
        REPLACE_METAL_EQUIPMENT_RECIPES = b.comment("Disable ordinary crafting recipes for metal equipment that can be smithed.")
                .define("replaceMetalEquipmentRecipes", true);
        ALLOW_VANILLA_CRAFTING_ALONGSIDE_SMITHING = b.comment("Keep the original crafting recipes. Smithing remains available as an alternative.")
                .define("allowVanillaCraftingAlongsideSmithing", false);
        SUPPRESS_SMITHING_TABLE_UPGRADES = b.comment("Also disable smithing-table upgrade recipes (such as netherite upgrades) whose result can be smithed directly.")
                .define("suppressSmithingTableUpgrades", true);
        AUTO_DETECT_EQUIPMENT = b.comment("Generate smithing recipes by analysing crafting recipes of weapons, tools, armor and metal shields.")
                .define("autoDetectEquipment", true);
        AUTO_DETECT_MODDED_METALS = b.comment("Create material families from forge:ingots/<metal> tags that no datapack defines.")
                .define("autoDetectModdedMetals", true);
        AUTO_METAL_EXCLUSIONS = b.comment("forge:ingots/<name> tags that are never turned into material families.")
                .defineListAllowEmpty("autoMetalExclusions", List.of("brick", "nether_brick"), o -> o instanceof String);
        RECIPE_BLACKLIST = b.comment("Crafting recipe ids that automatic detection ignores and never disables.")
                .defineListAllowEmpty("recipeBlacklist", List.of(), o -> o instanceof String);
        MATERIAL_FAMILY_BLACKLIST = b.comment("Material family ids (e.g. forge:steel) that are ignored entirely.")
                .defineListAllowEmpty("materialFamilyBlacklist", List.of(), o -> o instanceof String);
        MOD_BLACKLIST = b.comment("Mod ids whose equipment is never auto-detected or disabled.")
                .defineListAllowEmpty("modBlacklist", List.of(), o -> o instanceof String);
        DEBUG_LOGGING = b.comment("Log every detection decision at INFO level instead of DEBUG.")
                .define("debugLogging", false);
        b.pop();

        b.comment("Smith's Forge").push("forge");
        BASE_MELT_TIME_TICKS = b.comment("Ticks needed to melt a batch before the material family multiplier is applied.")
                .defineInRange("baseMeltTimeTicks", 160, 20, 12000);
        FORGE_CAPACITY_UNITS = b.comment("Maximum material units (nugget = 1, ingot = 9, block = 81) a forge holds.")
                .defineInRange("forgeCapacityUnits", 648, 9, 100000);
        FUEL_CONSUMPTION_MULTIPLIER = b.comment("How much faster than a furnace the forge burns solid fuel.")
                .defineInRange("fuelConsumptionMultiplier", 4.0, 0.1, 100.0);
        ACCEPT_ANY_FURNACE_FUEL = b.comment("Accept every furnace fuel, not only items in #immersive_smithing:forge_fuels.")
                .define("acceptAnyFurnaceFuel", false);
        LAVA_BUCKET_HEAT_TICKS = b.comment("Melting ticks provided by one bucket of lava.")
                .defineInRange("lavaBucketHeatTicks", 4000, 20, 1000000);
        MAX_LAVA_BUCKETS = b.comment("How many buckets of lava the forge can hold.")
                .defineInRange("maxLavaBuckets", 4, 1, 64);
        b.pop();

        b.comment("Smithing tool tiers: minigame time and durability").push("tools");
        STONE_FORGE_TIME_SECONDS = b.defineInRange("stoneForgeTimeSeconds", 30, 5, 600);
        IRON_FORGE_TIME_SECONDS = b.defineInRange("ironForgeTimeSeconds", 35, 5, 600);
        DIAMOND_FORGE_TIME_SECONDS = b.defineInRange("diamondForgeTimeSeconds", 40, 5, 600);
        NETHERITE_FORGE_TIME_SECONDS = b.defineInRange("netheriteForgeTimeSeconds", 45, 5, 600);
        STONE_ANVIL_TIME_SECONDS = b.defineInRange("stoneAnvilTimeSeconds", 30, 5, 600);
        IRON_ANVIL_TIME_SECONDS = b.defineInRange("ironAnvilTimeSeconds", 35, 5, 600);
        DIAMOND_ANVIL_TIME_SECONDS = b.defineInRange("diamondAnvilTimeSeconds", 40, 5, 600);
        NETHERITE_ANVIL_TIME_SECONDS = b.defineInRange("netheriteAnvilTimeSeconds", 45, 5, 600);
        STONE_TOOL_DURABILITY = b.defineInRange("stoneToolDurability", 512, 1, 100000);
        IRON_TOOL_DURABILITY = b.defineInRange("ironToolDurability", 1024, 1, 100000);
        DIAMOND_TOOL_DURABILITY = b.defineInRange("diamondToolDurability", 2048, 1, 100000);
        NETHERITE_TOOL_DURABILITY = b.defineInRange("netheriteToolDurability", 4096, 1, 100000);
        b.pop();

        b.comment("Minigame timing and validation").push("minigames");
        READY_DELAY_MS = b.comment("Countdown before a minigame starts, in milliseconds.")
                .defineInRange("readyDelayMs", 1500, 0, 10000);
        TIMING_TOLERANCE_MS = b.comment("Jitter tolerance applied to client-reported input times.")
                .defineInRange("timingToleranceMs", 120, 0, 2000);
        MAX_LATENCY_COMPENSATION_MS = b.comment("Upper bound of the latency compensation granted to a player's inputs.")
                .defineInRange("maxLatencyCompensationMs", 400, 0, 5000);
        MAX_STATION_DISTANCE = b.comment("Maximum distance in blocks between a player and the station they are working at.")
                .defineInRange("maxStationDistance", 6.0, 2.0, 32.0);
        b.pop();

        b.comment("Quality multipliers. A score of 50 is the midpoint between minimum and maximum.").push("quality");
        MINIMUM_DURABILITY_MULTIPLIER = b.defineInRange("minimumDurabilityMultiplier", 0.75, 0.05, 10.0);
        MAXIMUM_DURABILITY_MULTIPLIER = b.defineInRange("maximumDurabilityMultiplier", 1.25, 0.05, 10.0);
        FAULTY_DURABILITY_MULTIPLIER = b.defineInRange("faultyDurabilityMultiplier", 0.50, 0.05, 10.0);
        MINIMUM_EFFICACY_MULTIPLIER = b.defineInRange("minimumEfficacyMultiplier", 0.85, 0.05, 10.0);
        MAXIMUM_EFFICACY_MULTIPLIER = b.defineInRange("maximumEfficacyMultiplier", 1.15, 0.05, 10.0);
        FAULTY_EFFICACY_MULTIPLIER = b.defineInRange("faultyEfficacyMultiplier", 0.70, 0.05, 10.0);
        TOOL_ATTACK_EFFICACY_FACTOR = b.comment("Share of the efficacy deviation applied to the attack damage of mining tools.")
                .defineInRange("toolAttackEfficacyFactor", 0.5, 0.0, 1.0);
        SCALE_ARMOR_TOUGHNESS = b.comment("Scale armor toughness with efficacy when the item already has toughness.")
                .define("scaleArmorToughness", true);
        b.pop();

        b.push("recycling");
        RECYCLING_EFFICIENCY = b.comment("Share of an item's metal recovered when it is melted down.")
                .defineInRange("recyclingEfficiency", 1.0, 0.0, 1.0);
        b.pop();

        b.push("trough");
        TROUGH_QUENCHES_PER_BUCKET = b.defineInRange("troughQuenchesPerBucket", 4, 1, 64);
        TROUGH_CAPACITY_BUCKETS = b.defineInRange("troughCapacityBuckets", 2, 1, 16);
        b.pop();

        b.push("grindstone");
        ENABLE_SMITH_GRINDSTONE = b.define("enableSmithGrindstone", true);
        GRINDSTONE_SCORE_STEP = b.comment("Score gained per refinement.").defineInRange("scorePerRefinement", 5, 1, 100);
        GRINDSTONE_BASE_LEVEL_COST = b.comment("XP levels charged for a refinement of a score of 0.").defineInRange("baseLevelCost", 1, 0, 100);
        GRINDSTONE_SCORE_PER_EXTRA_LEVEL = b.comment("Every this many score points add one XP level to the cost.").defineInRange("scorePerExtraLevel", 20, 1, 100);
        b.pop();

        b.push("villagers");
        ENABLE_VILLAGER_TRADES = b.define("enableVillagerTrades", true);
        ALLOW_FAULTY_VILLAGER_ITEMS = b.define("allowFaultyVillagerItems", false);
        b.pop();

        b.comment("Automation. Automation can prepare stations but never complete a minigame.").push("automation");
        ENABLE_AUTOMATION = b.define("enableAutomation", true);
        FORGE_METAL_FACES = b.comment("Faces of the Smith's Forge that accept metal (up, down, north, south, east, west, relative to a north-facing forge).")
                .defineListAllowEmpty("forgeMetalFaces", List.of("up", "east", "west"), o -> o instanceof String);
        FORGE_FUEL_FACES = b.comment("Faces of the Smith's Forge that accept fuel.")
                .defineListAllowEmpty("forgeFuelFaces", List.of("down", "south"), o -> o instanceof String);
        TROUGH_FLUID_INPUT = b.comment("Allow pipes to fill the Smith's Trough with water.")
                .define("troughFluidInput", true);
        b.pop();

        b.push("guide");
        GRANT_GUIDE_BOOK_AUTOMATICALLY = b.comment("Give players the Smithing Guide when they first obtain smithing tools.")
                .define("grantGuideBookAutomatically", true);
        b.pop();

        b.comment("Built-in support for other mods. Changes need /reload.").push("integrations");
        SPARTAN_WEAPONRY_INTEGRATION = b.comment("Smith every Spartan Weaponry metal weapon with the built-in recipes. When off, Spartan Weaponry is left to automatic detection like any other mod.")
                .define("spartanWeaponry", true);
        b.pop();

        SPEC = b.build();
    }

    public static boolean get(ForgeConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static int get(ForgeConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static double get(ForgeConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    @SuppressWarnings("unchecked")
    public static List<String> list(ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
        return (List<String>) (SPEC.isLoaded() ? value.get() : value.getDefault());
    }

    private ServerConfig() {}
}
