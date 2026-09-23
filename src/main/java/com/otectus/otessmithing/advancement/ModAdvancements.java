package com.otectus.otessmithing.advancement;

import com.otectus.otessmithing.OtesSmithing;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;

/**
 * Smithing milestones use advancements with a single {@code minecraft:impossible} criterion named "done",
 * awarded from code. Core functionality is never locked behind them.
 */
public final class ModAdvancements {
    public static final String STOKE_THE_FORGE = "stoke_the_forge";
    public static final String INTO_SHAPE = "into_shape";
    public static final String HAMMER_AND_STEEL = "hammer_and_steel";
    public static final String HISS = "hiss";
    public static final String FINE_WORK = "fine_work";
    public static final String MASTER_SMITH = "master_smith";
    public static final String THATLL_BUFF_OUT = "thatll_buff_out";
    public static final String AGAIN = "again";

    public static void award(ServerPlayer player, String name) {
        if (player.getServer() == null) return;
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(OtesSmithing.id(name));
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private ModAdvancements() {}
}
