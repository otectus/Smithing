package com.otectus.immersivesmithing.registry;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.blockentity.SmithsAnvilBlockEntity;
import com.otectus.immersivesmithing.blockentity.SmithsForgeBlockEntity;
import com.otectus.immersivesmithing.blockentity.SmithsTroughBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ImmersiveSmithing.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SmithsForgeBlockEntity>> SMITHS_FORGE = BLOCK_ENTITIES.register("smiths_forge",
            () -> BlockEntityType.Builder.of(SmithsForgeBlockEntity::new, ModBlocks.SMITHS_FORGE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SmithsAnvilBlockEntity>> SMITHS_ANVIL = BLOCK_ENTITIES.register("smiths_anvil",
            () -> BlockEntityType.Builder.of(SmithsAnvilBlockEntity::new, ModBlocks.SMITHS_ANVIL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<SmithsTroughBlockEntity>> SMITHS_TROUGH = BLOCK_ENTITIES.register("smiths_trough",
            () -> BlockEntityType.Builder.of(SmithsTroughBlockEntity::new, ModBlocks.SMITHS_TROUGH.get()).build(null));

    private ModBlockEntities() {}
}
