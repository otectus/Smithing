package com.otectus.otessmithing.quality;

import com.google.common.collect.Multimap;
import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.api.IShieldSmithingHandler;
import com.otectus.otessmithing.api.ISmithingEfficacyHandler;
import com.otectus.otessmithing.api.OtesSmithingAPI;
import com.otectus.otessmithing.config.ServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies quality to arbitrary items without replacing their definitions: attribute differences are added
 * next to the item's own modifiers, mining speed is multiplied from the computed baseline, and durability is
 * converted per damage event (see {@link #adjustDamage}).
 */
@Mod.EventBusSubscriber(modid = OtesSmithing.MOD_ID)
public final class QualityHooks {
    private static final Map<EquipmentSlot, UUID> ATTACK_IDS = ids("attack_damage");
    private static final Map<EquipmentSlot, UUID> ARMOR_IDS = ids("armor");
    private static final Map<EquipmentSlot, UUID> TOUGHNESS_IDS = ids("armor_toughness");
    private static final String MODIFIER_NAME = "Smithing quality";

    private static Map<EquipmentSlot, UUID> ids(String attribute) {
        Map<EquipmentSlot, UUID> map = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            map.put(slot, UUID.nameUUIDFromBytes(("otes_smithing:" + attribute + ":" + slot.getName()).getBytes(StandardCharsets.UTF_8)));
        }
        return map;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        Optional<QualityData> data = QualityData.get(stack);
        if (data.isEmpty() || OtesSmithingAPI.isExcluded(stack)) return;
        QualityData quality = data.get();
        double efficacy = QualityCalculator.efficacyMultiplier(quality);
        if (Math.abs(efficacy - 1.0) < 1.0E-6) return;

        try {
            for (ISmithingEfficacyHandler handler : OtesSmithingAPI.efficacyHandlers()) {
                if (handler.handles(stack)) {
                    handler.modifyAttributes(event, quality, efficacy);
                    return;
                }
            }
            if (isShield(stack)) {
                for (IShieldSmithingHandler handler : OtesSmithingAPI.shieldHandlers()) {
                    if (handler.handles(stack)) {
                        handler.applyShieldEfficacy(event, quality, efficacy);
                        return;
                    }
                }
                return; // shields are efficacy-neutral unless a handler says otherwise
            }

            EquipmentSlot slot = event.getSlotType();
            Multimap<Attribute, AttributeModifier> original = event.getOriginalModifiers();

            double armor = additions(original, Attributes.ARMOR);
            if (armor > 0) {
                event.addModifier(Attributes.ARMOR, new AttributeModifier(ARMOR_IDS.get(slot), MODIFIER_NAME,
                        armor * (efficacy - 1.0), AttributeModifier.Operation.ADDITION));
                double toughness = additions(original, Attributes.ARMOR_TOUGHNESS);
                if (toughness > 0 && ServerConfig.get(ServerConfig.SCALE_ARMOR_TOUGHNESS)) {
                    event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(TOUGHNESS_IDS.get(slot), MODIFIER_NAME,
                            toughness * (efficacy - 1.0), AttributeModifier.Operation.ADDITION));
                }
            }

            if (slot == EquipmentSlot.MAINHAND) {
                double damage = additions(original, Attributes.ATTACK_DAMAGE);
                if (damage > 0) {
                    double multiplier = isMiningTool(stack) ? QualityCalculator.toolAttackMultiplier(efficacy) : efficacy;
                    // Scale the full displayed damage (the player's base 1.0 plus the item's bonus).
                    double delta = (damage + 1.0) * (multiplier - 1.0);
                    event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_IDS.get(slot), MODIFIER_NAME,
                            delta, AttributeModifier.Operation.ADDITION));
                }
            }
        } catch (RuntimeException e) {
            OtesSmithing.LOGGER.debug("Could not apply smithing efficacy to {}: {}", stack, e.toString());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        Optional<QualityData> data = QualityData.get(stack);
        if (data.isEmpty() || OtesSmithingAPI.isExcluded(stack)) return;
        double efficacy = QualityCalculator.efficacyMultiplier(data.get());
        try {
            for (ISmithingEfficacyHandler handler : OtesSmithingAPI.efficacyHandlers()) {
                if (handler.handles(stack)) {
                    event.setNewSpeed(handler.modifyBreakSpeed(stack, event.getState(), event.getNewSpeed(), efficacy));
                    return;
                }
            }
            if (stack.getDestroySpeed(event.getState()) > 1.0F) {
                event.setNewSpeed((float) (event.getNewSpeed() * efficacy));
            }
        } catch (RuntimeException e) {
            OtesSmithing.LOGGER.debug("Could not apply smithing mining speed to {}: {}", stack, e.toString());
        }
    }

    /**
     * Durability conversion: rather than changing an item's maximum durability (which arbitrary modded items
     * may read directly), each incoming damage point is scaled by 1 / multiplier. The fractional remainder is
     * kept in the stack's quality compound so the conversion is deterministic. Unbreaking and Mending run
     * unchanged on top of this. Called from {@code ItemStackMixin} at the head of {@code ItemStack#hurt}.
     */
    public static int adjustDamage(ItemStack stack, int amount) {
        if (amount <= 0) return amount;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(QualityData.ROOT)) return amount;
        Optional<QualityData> data = QualityData.get(stack);
        if (data.isEmpty() || OtesSmithingAPI.isExcluded(stack)) return amount;
        double multiplier = Math.max(0.01, QualityCalculator.durabilityMultiplier(data.get()));
        if (Math.abs(multiplier - 1.0) < 1.0E-6) return amount;
        CompoundTag root = tag.getCompound(QualityData.ROOT);
        double carry = root.getDouble(QualityData.CARRY) + amount / multiplier;
        int applied = (int) Math.floor(carry);
        root.putDouble(QualityData.CARRY, carry - applied);
        return applied;
    }

    private static double additions(Multimap<Attribute, AttributeModifier> modifiers, Attribute attribute) {
        double sum = 0;
        for (AttributeModifier m : modifiers.get(attribute)) {
            if (m.getOperation() == AttributeModifier.Operation.ADDITION) sum += m.getAmount();
        }
        return sum;
    }

    public static boolean isMiningTool(ItemStack stack) {
        return stack.getItem() instanceof DiggerItem
                || stack.canPerformAction(ToolActions.PICKAXE_DIG)
                || stack.canPerformAction(ToolActions.AXE_DIG)
                || stack.canPerformAction(ToolActions.SHOVEL_DIG)
                || stack.canPerformAction(ToolActions.HOE_DIG);
    }

    public static boolean isShield(ItemStack stack) {
        return stack.getItem() instanceof ShieldItem || stack.canPerformAction(ToolActions.SHIELD_BLOCK);
    }

    private QualityHooks() {}
}
