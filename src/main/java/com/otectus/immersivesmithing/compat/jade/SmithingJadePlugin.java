package com.otectus.immersivesmithing.compat.jade;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.block.SmithsAnvilBlock;
import com.otectus.immersivesmithing.block.SmithsForgeBlock;
import com.otectus.immersivesmithing.block.SmithsGrindstoneBlock;
import com.otectus.immersivesmithing.block.SmithsTroughBlock;
import com.otectus.immersivesmithing.blockentity.SmithsAnvilBlockEntity;
import com.otectus.immersivesmithing.blockentity.SmithsForgeBlockEntity;
import com.otectus.immersivesmithing.blockentity.SmithsTroughBlockEntity;
import com.otectus.immersivesmithing.material.MaterialUnits;
import com.otectus.immersivesmithing.quality.QualityData;
import com.otectus.immersivesmithing.workpiece.WorkpieceData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade overlays for the four stations. Discovered by Jade through the annotation, so nothing here loads without
 * it. The server providers write what the block entity knows into the tooltip's server data; the client providers
 * only format it, so the overlay is right even where a block entity does not sync a field.
 */
@WailaPlugin
public class SmithingJadePlugin implements IWailaPlugin {
    static final ResourceLocation FORGE = ImmersiveSmithing.id("forge");
    static final ResourceLocation ANVIL = ImmersiveSmithing.id("anvil");
    static final ResourceLocation TROUGH = ImmersiveSmithing.id("trough");
    static final ResourceLocation GRINDSTONE = ImmersiveSmithing.id("grindstone");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ForgeProvider.INSTANCE, SmithsForgeBlockEntity.class);
        registration.registerBlockDataProvider(AnvilProvider.INSTANCE, SmithsAnvilBlockEntity.class);
        registration.registerBlockDataProvider(TroughProvider.INSTANCE, SmithsTroughBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ForgeProvider.INSTANCE, SmithsForgeBlock.class);
        registration.registerBlockComponent(AnvilProvider.INSTANCE, SmithsAnvilBlock.class);
        registration.registerBlockComponent(TroughProvider.INSTANCE, SmithsTroughBlock.class);
        registration.registerBlockComponent(GrindstoneProvider.INSTANCE, SmithsGrindstoneBlock.class);
    }

    private static MutableComponent line(String key, Object... args) {
        return Component.translatable("jade.immersive_smithing." + key, args);
    }

    enum ForgeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof SmithsForgeBlockEntity forge)) return;
            data.putInt("Molten", forge.moltenUnits());
            data.putInt("Deposits", forge.depositUnits());
            data.putInt("Capacity", forge.capacity());
            data.putBoolean("Ready", forge.isReady());
            data.putBoolean("Burning", forge.isBurning());
            data.putBoolean("Lava", forge.hasLava());
            forge.familyData().ifPresent(f -> data.putString("Family", Component.Serializer.toJson(f.displayName())));
            ItemStack fuel = forge.fuel();
            if (!fuel.isEmpty()) data.put("Fuel", fuel.save(new CompoundTag()));
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Capacity")) return;
            int molten = data.getInt("Molten");
            int deposits = data.getInt("Deposits");
            if (molten + deposits == 0) {
                tooltip.add(line("forge.empty").withStyle(ChatFormatting.GRAY));
            } else {
                Component family = data.contains("Family") ? Component.Serializer.fromJson(data.getString("Family")) : Component.empty();
                if (molten > 0) tooltip.add(line("forge.molten", family, MaterialUnits.describe(molten)).withStyle(ChatFormatting.GOLD));
                if (deposits > 0) tooltip.add(line("forge.deposits", family, MaterialUnits.describe(deposits)).withStyle(ChatFormatting.GRAY));
            }
            if (data.getBoolean("Ready")) tooltip.add(line("forge.ready").withStyle(ChatFormatting.GREEN));
            else if (data.getBoolean("Burning")) tooltip.add(line("forge.heating").withStyle(ChatFormatting.YELLOW));
            if (data.getBoolean("Lava")) tooltip.add(line("forge.lava").withStyle(ChatFormatting.RED));
            else if (data.contains("Fuel")) {
                ItemStack fuel = ItemStack.of(data.getCompound("Fuel"));
                tooltip.add(line("forge.fuel", fuel.getCount(), fuel.getHoverName()).withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(line("forge.no_fuel").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return FORGE;
        }
    }

    enum AnvilProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof SmithsAnvilBlockEntity anvil)) return;
            WorkpieceData workpiece = anvil.workpiece();
            data.putBoolean("Locked", anvil.isLocked());
            if (workpiece == null) return;
            data.put("Target", workpiece.target().save(new CompoundTag()));
            data.putBoolean("Shaped", workpiece.isShaped());
            data.putInt("ForgeScore", workpiece.forgeScore());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Locked")) return;
            if (!data.contains("Target")) {
                tooltip.add(line("anvil.empty").withStyle(ChatFormatting.GRAY));
            } else {
                ItemStack target = ItemStack.of(data.getCompound("Target"));
                tooltip.add(line(data.getBoolean("Shaped") ? "anvil.shaped" : "anvil.forged", target.getHoverName()).withStyle(ChatFormatting.GOLD));
                tooltip.add(line("anvil.forge_score", data.getInt("ForgeScore")).withStyle(ChatFormatting.GRAY));
            }
            if (data.getBoolean("Locked")) tooltip.add(line("anvil.in_use").withStyle(ChatFormatting.RED));
        }

        @Override
        public ResourceLocation getUid() {
            return ANVIL;
        }
    }

    enum TroughProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof SmithsTroughBlockEntity trough)) return;
            data.putInt("Quenches", trough.quenchesLeft());
            data.putInt("Water", trough.water());
            data.putInt("WaterCapacity", trough.capacity());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("Quenches")) return;
            tooltip.add(line("trough.quenches", data.getInt("Quenches"), data.getInt("Water"), data.getInt("WaterCapacity"))
                    .withStyle(data.getInt("Quenches") > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        }

        @Override
        public ResourceLocation getUid() {
            return TROUGH;
        }
    }

    /** The grindstone keeps no state: the overlay explains the held item's next refinement. */
    enum GrindstoneProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ItemStack held = accessor.getPlayer().getMainHandItem();
            QualityData quality = QualityData.get(held).orElse(null);
            if (quality == null) {
                tooltip.add(line("grindstone.hint").withStyle(ChatFormatting.GRAY));
                return;
            }
            if (quality.faulty()) {
                tooltip.add(line("grindstone.faulty").withStyle(ChatFormatting.RED));
                return;
            }
            tooltip.add(line("grindstone.durability", quality.forgeScore(), SmithsGrindstoneBlock.levelCost(quality.forgeScore())).withStyle(ChatFormatting.GRAY));
            tooltip.add(line("grindstone.efficacy", quality.anvilScore(), SmithsGrindstoneBlock.levelCost(quality.anvilScore())).withStyle(ChatFormatting.GRAY));
        }

        @Override
        public ResourceLocation getUid() {
            return GRINDSTONE;
        }
    }
}
