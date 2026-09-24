package com.otectus.immersivesmithing.test;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Picking must agree with the new open cavities and the work surface in every orientation. */
@GameTestHolder(ImmersiveSmithing.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StationVisualGameTests {
    @GameTest(template = "empty")
    public static void stationSurfacesAndOpenings(GameTestHelper h) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var forge = ModBlocks.SMITHS_FORGE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
            Vec3 front = new Vec3(0.5 + facing.getStepX() * 0.8, 0.25, 0.5 + facing.getStepZ() * 0.8);
            Vec3 back = new Vec3(0.5 - facing.getStepX() * 0.8, 0.25, 0.5 - facing.getStepZ() * 0.8);
            BlockHitResult fuel = forge.getShape(h.getLevel(), BlockPos.ZERO).clip(front, back, BlockPos.ZERO);
            h.assertTrue(fuel != null, "Firebox back can be targeted: " + facing);
            double projection = (fuel.getLocation().x - 0.5) * facing.getStepX() + (fuel.getLocation().z - 0.5) * facing.getStepZ();
            h.assertTrue(Math.abs(projection + 5 / 16D) < 0.0001, "Firebox opening has no invisible front wall: " + facing);

            var trough = ModBlocks.SMITHS_TROUGH.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
            BlockHitResult basin = trough.getShape(h.getLevel(), BlockPos.ZERO).clip(new Vec3(0.5, 1, 0.5), new Vec3(0.5, 0, 0.5), BlockPos.ZERO);
            h.assertTrue(basin != null && Math.abs(basin.getLocation().y - 3 / 16D) < 0.0001, "Trough basin is open to its floor: " + facing);

            var anvil = ModBlocks.SMITHS_ANVIL.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
            BlockHitResult face = anvil.getShape(h.getLevel(), BlockPos.ZERO).clip(new Vec3(0.5, 2, 0.5), new Vec3(0.5, 0, 0.5), BlockPos.ZERO);
            h.assertTrue(face != null && Math.abs(face.getLocation().y - 1) < 0.0001, "Workpiece rests at the actual anvil face: " + facing);
        }
        h.succeed();
    }

    private StationVisualGameTests() {}
}
