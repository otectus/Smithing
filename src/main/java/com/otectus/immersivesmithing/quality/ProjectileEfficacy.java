package com.otectus.immersivesmithing.quality;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.api.ISmithingEfficacyHandler;
import com.otectus.immersivesmithing.api.ImmersiveSmithingAPI;
import com.otectus.immersivesmithing.compat.spartanweaponry.SpartanCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Carries smithing efficacy to projectiles. A thrown Spartan Weaponry weapon uses its own quality; arrows and
 * bolts use the quality of the bow or crossbow that fired them. Base damage is scaled once, when the projectile
 * first enters a level; a persistent-data flag stops a second scaling after a dimension change.
 */
@Mod.EventBusSubscriber(modid = ImmersiveSmithing.MOD_ID)
public final class ProjectileEfficacy {
    private static final String SCALED = ImmersiveSmithing.MOD_ID + ":efficacy_scaled";

    private ProjectileEfficacy() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow) || arrow.getPersistentData().getBoolean(SCALED)) return;
        double efficacy = efficacy(sourceOf(arrow));
        if (Math.abs(efficacy - 1.0) < 1.0E-6) return;
        arrow.setBaseDamage(arrow.getBaseDamage() * efficacy);
        arrow.getPersistentData().putBoolean(SCALED, true);
    }

    /** The stack whose quality applies to the projectile, or empty. */
    static ItemStack sourceOf(AbstractArrow arrow) {
        if (ModList.get().isLoaded(SpartanCompat.MOD_ID)) {
            ItemStack thrown = SpartanCompat.thrownWeapon(arrow);
            if (!thrown.isEmpty()) return thrown;
        }
        if (!(arrow.getOwner() instanceof LivingEntity owner)) return ItemStack.EMPTY;
        // A drawn bow is still the use item while it releases; a charged crossbow fires from the hand.
        if (owner.isUsingItem()) return launcher(owner.getUseItem());
        ItemStack main = launcher(owner.getMainHandItem());
        return main.isEmpty() ? launcher(owner.getOffhandItem()) : main;
    }

    private static ItemStack launcher(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem && QualityData.has(stack) ? stack : ItemStack.EMPTY;
    }

    /** Damage multiplier for a projectile fired or thrown from this stack; 1.0 when quality does not apply. */
    public static double efficacy(ItemStack stack) {
        Optional<QualityData> data = QualityData.get(stack);
        if (data.isEmpty() || ImmersiveSmithingAPI.isExcluded(stack)) return 1.0;
        for (ISmithingEfficacyHandler handler : ImmersiveSmithingAPI.efficacyHandlers()) {
            if (handler.handles(stack)) return 1.0;
        }
        return QualityCalculator.efficacyMultiplier(data.get());
    }
}
