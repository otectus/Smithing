package com.otectus.immersivesmithing.api.event;

import com.otectus.immersivesmithing.quality.QualityData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Posted on the Forge bus when a smith quenches a workpiece into a finished item, after quality and the maker's
 * mark are written and before the item reaches their inventory. {@link #getItem()} is the live stack: changes
 * made by listeners (an extra enchantment, a skill's bonus) stay on the item. Not cancellable; server side only.
 * Progression mods and KubeJS scripts can listen to it to reward smithing like any other craft.
 */
public class ItemSmithedEvent extends PlayerEvent {
    private final ItemStack result;
    private final QualityData quality;
    private final ResourceLocation recipeId;
    private final ResourceLocation family;
    private final int metalUnits;
    private final BlockPos troughPos;

    public ItemSmithedEvent(ServerPlayer smith, ItemStack result, QualityData quality, ResourceLocation recipeId,
                            ResourceLocation family, int metalUnits, BlockPos troughPos) {
        super(smith);
        this.result = result;
        this.quality = quality;
        this.recipeId = recipeId;
        this.family = family;
        this.metalUnits = metalUnits;
        this.troughPos = troughPos;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    /** The finished item, mutable. */
    public ItemStack getItem() { return result; }
    public QualityData getQuality() { return quality; }
    /** The smithing recipe that was forged, explicit or automatic. */
    public ResourceLocation getRecipeId() { return recipeId; }
    /** The material family the metal came from. */
    public ResourceLocation getFamily() { return family; }
    /** Material units consumed (nugget 1, ingot 9, block 81). */
    public int getMetalUnits() { return metalUnits; }
    public BlockPos getTroughPos() { return troughPos; }
}
