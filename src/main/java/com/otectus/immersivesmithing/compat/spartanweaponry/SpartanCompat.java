package com.otectus.immersivesmithing.compat.spartanweaponry;

import com.oblivioussp.spartanweaponry.entity.projectile.ThrowingWeaponEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Spartan Weaponry types. Only call this class after checking {@code ModList.get().isLoaded(MOD_ID)}; it links
 * against Spartan Weaponry classes, which are absent otherwise.
 */
public final class SpartanCompat {
    public static final String MOD_ID = "spartanweaponry";

    private SpartanCompat() {}

    /** The weapon a thrown Spartan projectile (javelin, tomahawk, throwing knife, boomerang) carries, or empty. */
    public static ItemStack thrownWeapon(Entity entity) {
        return entity instanceof ThrowingWeaponEntity thrown ? thrown.getWeaponItem() : ItemStack.EMPTY;
    }
}
