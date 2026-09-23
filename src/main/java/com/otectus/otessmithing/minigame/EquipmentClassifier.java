package com.otectus.otessmithing.minigame;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.api.IEquipmentClassifier;
import com.otectus.otessmithing.api.OtesSmithingAPI;
import com.otectus.otessmithing.registry.ModTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;

/** Decides which equipment is smithable and which anvil pattern family an item uses. */
public final class EquipmentClassifier {
    public static final ResourceLocation SWORD = OtesSmithing.id("sword");
    public static final ResourceLocation AXE = OtesSmithing.id("axe");
    public static final ResourceLocation PICKAXE = OtesSmithing.id("pickaxe");
    public static final ResourceLocation SHOVEL = OtesSmithing.id("shovel");
    public static final ResourceLocation HOE = OtesSmithing.id("hoe");
    public static final ResourceLocation GENERIC_TOOL = OtesSmithing.id("generic_tool");
    public static final ResourceLocation HELMET = OtesSmithing.id("helmet");
    public static final ResourceLocation CHESTPLATE = OtesSmithing.id("chestplate");
    public static final ResourceLocation LEGGINGS = OtesSmithing.id("leggings");
    public static final ResourceLocation BOOTS = OtesSmithing.id("boots");
    public static final ResourceLocation SHIELD = OtesSmithing.id("shield");
    public static final ResourceLocation GENERIC_WEAPON = OtesSmithing.id("generic_weapon");
    public static final ResourceLocation GENERIC_ARMOR = OtesSmithing.id("generic_armor");
    public static final ResourceLocation GENERIC_EQUIPMENT = OtesSmithing.id("generic_equipment");

    /** Closest anvil pattern family for an item. Integrations and pattern category tags take precedence. */
    public static ResourceLocation classify(ItemStack stack) {
        for (IEquipmentClassifier classifier : OtesSmithingAPI.classifiers()) {
            try {
                ResourceLocation id = classifier.classify(stack);
                if (id != null) return id;
            } catch (RuntimeException e) {
                OtesSmithing.LOGGER.debug("Equipment classifier failed for {}: {}", stack, e.toString());
            }
        }
        ResourceLocation byCategory = PatternRegistry.anvilPatternForCategories(stack);
        if (byCategory != null) return byCategory;

        Item item = stack.getItem();
        if (item instanceof SwordItem || stack.is(ItemTags.SWORDS)) return SWORD;
        if (item instanceof PickaxeItem || stack.is(ItemTags.PICKAXES)) return PICKAXE;
        if (item instanceof AxeItem || stack.is(ItemTags.AXES)) return AXE;
        if (item instanceof ShovelItem || stack.is(ItemTags.SHOVELS)) return SHOVEL;
        if (item instanceof HoeItem || stack.is(ItemTags.HOES)) return HOE;
        EquipmentSlot armorSlot = armorSlot(stack);
        if (armorSlot != null) {
            return switch (armorSlot) {
                case HEAD -> HELMET;
                case CHEST -> CHESTPLATE;
                case LEGS -> LEGGINGS;
                case FEET -> BOOTS;
                default -> GENERIC_ARMOR;
            };
        }
        if (item instanceof ShieldItem || stack.canPerformAction(ToolActions.SHIELD_BLOCK)) return SHIELD;
        if (stack.canPerformAction(ToolActions.SWORD_DIG)) return SWORD;
        if (stack.canPerformAction(ToolActions.PICKAXE_DIG)) return PICKAXE;
        if (stack.canPerformAction(ToolActions.AXE_DIG)) return AXE;
        if (stack.canPerformAction(ToolActions.SHOVEL_DIG)) return SHOVEL;
        if (stack.canPerformAction(ToolActions.HOE_DIG)) return HOE;
        if (item instanceof DiggerItem || item instanceof ShearsItem || stack.is(Tags.Items.TOOLS)) return GENERIC_TOOL;
        if (item instanceof TieredItem || hasAttackDamage(stack)) return GENERIC_WEAPON;
        if (stack.is(Tags.Items.ARMORS)) return GENERIC_ARMOR;
        return GENERIC_EQUIPMENT;
    }

    /**
     * Whether an item may be considered by automatic detection at all. Recipe analysis still has to find a
     * single metal family before anything is generated.
     */
    public static boolean isCandidate(ItemStack stack) {
        if (stack.isEmpty() || OtesSmithingAPI.isExcluded(stack)) return false;
        if (stack.is(ModTags.NON_SMITHABLE_EQUIPMENT)) return false;
        if (stack.is(ModTags.SMITHABLE_EQUIPMENT) || stack.is(ModTags.SMITHABLE_SHIELDS)) return true;
        Item item = stack.getItem();
        if (stack.getMaxStackSize() != 1 || !stack.isDamageableItem()) return false;
        if (item instanceof ProjectileWeaponItem || item instanceof FishingRodItem || item instanceof TridentItem) return false;
        if (stack.is(Tags.Items.TOOLS_BOWS) || stack.is(Tags.Items.TOOLS_CROSSBOWS)
                || stack.is(Tags.Items.TOOLS_FISHING_RODS) || stack.is(Tags.Items.TOOLS_TRIDENTS)) return false;
        if (isShield(stack)) return !stack.is(ModTags.NON_SMITHABLE_SHIELDS);
        return item instanceof TieredItem
                || item instanceof DiggerItem
                || item instanceof ShearsItem
                || armorSlot(stack) != null
                || stack.is(Tags.Items.TOOLS)
                || stack.is(Tags.Items.ARMORS)
                || stack.is(ItemTags.TOOLS)
                || stack.canPerformAction(ToolActions.SWORD_DIG)
                || stack.canPerformAction(ToolActions.PICKAXE_DIG)
                || stack.canPerformAction(ToolActions.AXE_DIG)
                || stack.canPerformAction(ToolActions.SHOVEL_DIG);
    }

    public static boolean isShield(ItemStack stack) {
        return stack.getItem() instanceof ShieldItem || stack.canPerformAction(ToolActions.SHIELD_BLOCK)
                || stack.is(Tags.Items.TOOLS_SHIELDS) || stack.is(ModTags.SMITHABLE_SHIELDS);
    }

    public static EquipmentSlot armorSlot(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armor) return armor.getEquipmentSlot();
        Equipable equipable = Equipable.get(stack);
        if (equipable != null && equipable.getEquipmentSlot().getType() == EquipmentSlot.Type.ARMOR
                && !stack.getAttributeModifiers(equipable.getEquipmentSlot()).get(Attributes.ARMOR).isEmpty()) {
            return equipable.getEquipmentSlot();
        }
        return null;
    }

    private static boolean hasAttackDamage(ItemStack stack) {
        return !stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty();
    }

    private EquipmentClassifier() {}
}
