package com.otectus.immersivesmithing.quality;

import com.otectus.immersivesmithing.advancement.ModAdvancements;
import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.network.ModNetwork;
import com.otectus.immersivesmithing.network.packet.OpenMakersMarkPacket;
import com.otectus.immersivesmithing.registry.ModSounds;
import com.otectus.immersivesmithing.util.Feedback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Server side of the Maker's Mark: which pieces may be signed, and the only path that writes a signature. */
public final class SigningService {

    public static boolean enabled() {
        return ServerConfig.get(ServerConfig.ENABLE_SIGNING);
    }

    /** Whether the player may open the signing screen for this piece right now. */
    public static boolean canSign(ItemStack stack, ServerPlayer player) {
        return enabled() && MakersMark.canSign(stack, player, ServerConfig.get(ServerConfig.ALLOW_RESIGNING));
    }

    /** Opens the screen for the piece in the given inventory slot; silently does nothing when it may not be signed. */
    public static void open(ServerPlayer player, int slot) {
        ItemStack stack = slotStack(player.getInventory(), slot);
        if (!canSign(stack, player)) return;
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenMakersMarkPacket(slot, stack.copy()));
    }

    public static void sign(ServerPlayer player, int slot, String rawTitle, List<String> rawInscription) {
        if (!enabled()) return;
        ItemStack stack = slotStack(player.getInventory(), slot);
        Optional<MakersMark> mark = MakersMark.get(stack);
        if (stack.isEmpty() || mark.isEmpty() || !mark.get().smithId().equals(player.getUUID())) {
            Feedback.fail(player, "sign_not_yours");
            return;
        }
        Optional<QualityData> quality = QualityData.get(stack);
        if (quality.isEmpty() || quality.get().faulty()) {
            Feedback.fail(player, "sign_faulty");
            return;
        }
        if (mark.get().signed() && !ServerConfig.get(ServerConfig.ALLOW_RESIGNING)) {
            Feedback.fail(player, "sign_already");
            return;
        }
        String title = MakersMark.sanitise(rawTitle, ServerConfig.get(ServerConfig.MAX_TITLE_LENGTH));
        List<String> inscription = new ArrayList<>();
        int maxLines = ServerConfig.get(ServerConfig.MAX_INSCRIPTION_LINES);
        int maxLength = ServerConfig.get(ServerConfig.MAX_INSCRIPTION_LINE_LENGTH);
        for (String line : rawInscription) {
            if (inscription.size() >= maxLines) break;
            String clean = MakersMark.sanitise(line, maxLength);
            if (!clean.isEmpty()) inscription.add(clean);
        }
        MakersMark.sign(stack, title, inscription);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.level().playSound(null, player.blockPosition(), ModSounds.ANVIL_STRIKE_PERFECT.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
        Feedback.info(player, "signed", stack.getHoverName());
        ModAdvancements.award(player, ModAdvancements.MAKERS_MARK);
    }

    private static ItemStack slotStack(Inventory inventory, int slot) {
        return slot >= 0 && slot < inventory.getContainerSize() ? inventory.getItem(slot) : ItemStack.EMPTY;
    }

    private SigningService() {}
}
