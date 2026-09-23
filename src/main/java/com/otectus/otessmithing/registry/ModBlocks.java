package com.otectus.otessmithing.registry;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.block.SmithsAnvilBlock;
import com.otectus.otessmithing.block.SmithsForgeBlock;
import com.otectus.otessmithing.block.SmithsGrindstoneBlock;
import com.otectus.otessmithing.block.SmithsTroughBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, OtesSmithing.MOD_ID);

    public static final RegistryObject<SmithsForgeBlock> SMITHS_FORGE = BLOCKS.register("smiths_forge",
            () -> new SmithsForgeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.5F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE_BRICKS)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(SmithsForgeBlock::lightLevel)));

    public static final RegistryObject<SmithsAnvilBlock> SMITHS_ANVIL = BLOCKS.register("smiths_anvil",
            () -> new SmithsAnvilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.ANVIL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<SmithsTroughBlock> SMITHS_TROUGH = BLOCKS.register("smiths_trough",
            () -> new SmithsTroughBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<SmithsGrindstoneBlock> SMITHS_GRINDSTONE = BLOCKS.register("smiths_grindstone",
            () -> new SmithsGrindstoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {}
}
