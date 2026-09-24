package com.otectus.immersivesmithing.test;

import com.mojang.authlib.GameProfile;
import com.otectus.immersivesmithing.workpiece.WorkpieceData;
import com.otectus.immersivesmithing.workpiece.WorkpieceState;
import com.otectus.immersivesmithing.minigame.EquipmentClassifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

final class TestSupport {
    static final BlockPos STATION = new BlockPos(2, 1, 2);

    static FakePlayer player(GameTestHelper h) {
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "SmithTest")) {
            @Override
            public boolean hasDisconnected() {
                return false;
            }
        };
        BlockPos pos = h.absolutePos(STATION);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5);
        return player;
    }

    /** Right-clicks the station at the given height (0..1 within the block) with the item in the main hand. */
    static InteractionResult use(GameTestHelper h, FakePlayer player, double heightInBlock) {
        BlockPos pos = h.absolutePos(STATION);
        Vec3 hit = new Vec3(pos.getX() + 0.5, pos.getY() + heightInBlock, pos.getZ() + 0.99);
        return h.getLevel().getBlockState(pos).use(h.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(hit, Direction.SOUTH, pos, false));
    }

    static WorkpieceData workpiece(ItemStack target, int forge, int anvil, boolean faulty, WorkpieceState state) {
        return new WorkpieceData(target, new ResourceLocation("immersive_smithing", "test"), new ResourceLocation("minecraft", "iron"),
                target.getItem() == net.minecraft.world.item.Items.IRON_PICKAXE ? 27 : 18,
                forge, anvil, faulty, state, EquipmentClassifier.classify(target));
    }

    private TestSupport() {}
}
