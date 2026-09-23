package com.otectus.otessmithing.item;

import com.otectus.otessmithing.config.ServerConfig;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * Smithing tool tiers. Higher tiers give more minigame time and durability; they never add quality directly.
 */
public enum SmithingTier implements StringRepresentable {
    STONE("stone", 512, 5, () -> Ingredient.of(ItemTags.STONE_TOOL_MATERIALS), false),
    IRON("iron", 1024, 14, () -> Ingredient.of(Items.IRON_INGOT), false),
    DIAMOND("diamond", 2048, 10, () -> Ingredient.of(Items.DIAMOND), false),
    NETHERITE("netherite", 4096, 15, () -> Ingredient.of(Items.NETHERITE_INGOT), true);

    private final String name;
    private final int defaultDurability;
    private final int enchantability;
    private final Supplier<Ingredient> repair;
    private final boolean fireResistant;

    SmithingTier(String name, int defaultDurability, int enchantability, Supplier<Ingredient> repair, boolean fireResistant) {
        this.name = name;
        this.defaultDurability = defaultDurability;
        this.enchantability = enchantability;
        this.repair = repair;
        this.fireResistant = fireResistant;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Item.Properties itemProperties() {
        Item.Properties p = new Item.Properties().durability(defaultDurability);
        return fireResistant ? p.fireResistant() : p;
    }

    public int enchantability() {
        return enchantability;
    }

    public Ingredient repairIngredient() {
        return repair.get();
    }

    public int durability() {
        return switch (this) {
            case STONE -> ServerConfig.get(ServerConfig.STONE_TOOL_DURABILITY);
            case IRON -> ServerConfig.get(ServerConfig.IRON_TOOL_DURABILITY);
            case DIAMOND -> ServerConfig.get(ServerConfig.DIAMOND_TOOL_DURABILITY);
            case NETHERITE -> ServerConfig.get(ServerConfig.NETHERITE_TOOL_DURABILITY);
        };
    }

    public int forgeTimeMs() {
        return 1000 * switch (this) {
            case STONE -> ServerConfig.get(ServerConfig.STONE_FORGE_TIME_SECONDS);
            case IRON -> ServerConfig.get(ServerConfig.IRON_FORGE_TIME_SECONDS);
            case DIAMOND -> ServerConfig.get(ServerConfig.DIAMOND_FORGE_TIME_SECONDS);
            case NETHERITE -> ServerConfig.get(ServerConfig.NETHERITE_FORGE_TIME_SECONDS);
        };
    }

    public int anvilTimeMs() {
        return 1000 * switch (this) {
            case STONE -> ServerConfig.get(ServerConfig.STONE_ANVIL_TIME_SECONDS);
            case IRON -> ServerConfig.get(ServerConfig.IRON_ANVIL_TIME_SECONDS);
            case DIAMOND -> ServerConfig.get(ServerConfig.DIAMOND_ANVIL_TIME_SECONDS);
            case NETHERITE -> ServerConfig.get(ServerConfig.NETHERITE_ANVIL_TIME_SECONDS);
        };
    }
}
