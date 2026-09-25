package com.otectus.immersivesmithing.quality;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The smith's mark on a finished piece: who forged it (stamped at the quench, never editable) and whether they
 * signed it with a title and an inscription. Both live in the {@code immersive_smithing} compound next to the
 * quality scores; the title and inscription themselves are written as the vanilla display name and lore so every
 * tooltip mod shows them.
 */
public record MakersMark(String smithName, UUID smithId, boolean signed) {
    public static final String SMITH_NAME = "SmithName";
    public static final String SMITH_UUID = "SmithUUID";
    public static final String SIGNED = "Signed";

    public static Optional<MakersMark> get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(QualityData.ROOT, Tag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag root = tag.getCompound(QualityData.ROOT);
        if (!root.hasUUID(SMITH_UUID)) return Optional.empty();
        return Optional.of(new MakersMark(root.getString(SMITH_NAME), root.getUUID(SMITH_UUID), root.getBoolean(SIGNED)));
    }

    /** Records who forged the piece. Called once, at the quench. */
    public static void stamp(ItemStack stack, Player smith) {
        CompoundTag root = stack.getOrCreateTagElement(QualityData.ROOT);
        root.putString(SMITH_NAME, smith.getGameProfile().getName());
        root.putUUID(SMITH_UUID, smith.getUUID());
        root.putBoolean(SIGNED, false);
    }

    /** Whether this player forged the piece and may sign or re-sign it. Faulty work is never signed. */
    public static boolean canSign(ItemStack stack, Player player, boolean allowRename) {
        Optional<MakersMark> mark = get(stack);
        Optional<QualityData> quality = QualityData.get(stack);
        if (mark.isEmpty() || quality.isEmpty() || quality.get().faulty()) return false;
        if (!mark.get().smithId().equals(player.getUUID())) return false;
        return allowRename || !mark.get().signed();
    }

    /**
     * Writes the title and inscription. An empty title restores the item's own name; an empty inscription clears
     * the lore. The strings must already be sanitised (see {@link #sanitise}).
     */
    public static void sign(ItemStack stack, String title, List<String> inscription) {
        SmithingQuality quality = QualityData.get(stack).map(QualityData::quality).orElse(SmithingQuality.STANDARD);
        if (title.isEmpty()) {
            stack.resetHoverName();
        } else {
            stack.setHoverName(Component.literal(title).withStyle(s -> s.withItalic(false).withColor(quality.color())));
        }
        CompoundTag display = stack.getOrCreateTagElement(ItemStack.TAG_DISPLAY);
        if (inscription.isEmpty()) {
            display.remove(ItemStack.TAG_LORE);
        } else {
            ListTag lore = new ListTag();
            for (String line : inscription) {
                lore.add(StringTag.valueOf(Component.Serializer.toJson(
                        Component.literal(line).withStyle(s -> s.withItalic(true).withColor(ChatFormatting.GRAY)))));
            }
            display.put(ItemStack.TAG_LORE, lore);
        }
        if (display.isEmpty()) stack.removeTagKey(ItemStack.TAG_DISPLAY);
        stack.getOrCreateTagElement(QualityData.ROOT).putBoolean(SIGNED, true);
    }

    /** Copies the mark and, when signed, the title and inscription from one stack to another (upgrades, dyeing). */
    public static void copy(ItemStack from, ItemStack to) {
        Optional<MakersMark> mark = get(from);
        if (mark.isEmpty()) return;
        CompoundTag root = to.getOrCreateTagElement(QualityData.ROOT);
        root.putString(SMITH_NAME, mark.get().smithName());
        root.putUUID(SMITH_UUID, mark.get().smithId());
        root.putBoolean(SIGNED, mark.get().signed());
        if (!mark.get().signed()) return;
        if (from.hasCustomHoverName()) to.setHoverName(from.getHoverName());
        CompoundTag display = from.getTagElement(ItemStack.TAG_DISPLAY);
        if (display != null && display.contains(ItemStack.TAG_LORE, Tag.TAG_LIST)) {
            to.getOrCreateTagElement(ItemStack.TAG_DISPLAY).put(ItemStack.TAG_LORE, display.getList(ItemStack.TAG_LORE, Tag.TAG_STRING).copy());
        }
    }

    /** The current title (empty when the item shows its own name) and inscription lines, for editing. */
    public static String title(ItemStack stack) {
        return stack.hasCustomHoverName() ? stack.getHoverName().getString() : "";
    }

    public static List<String> inscription(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        CompoundTag display = stack.getTagElement(ItemStack.TAG_DISPLAY);
        if (display == null || !display.contains(ItemStack.TAG_LORE, Tag.TAG_LIST)) return lines;
        for (Tag t : display.getList(ItemStack.TAG_LORE, Tag.TAG_STRING)) {
            try {
                Component c = Component.Serializer.fromJson(t.getAsString());
                lines.add(c == null ? "" : c.getString());
            } catch (RuntimeException e) {
                lines.add("");
            }
        }
        return lines;
    }

    /** Strips formatting codes and control characters, collapses whitespace and caps the length. */
    public static String sanitise(String text, int maxLength) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' || Character.isISOControl(c)) continue;
            if (Character.isWhitespace(c)) {
                space = true;
                continue;
            }
            if (space && !sb.isEmpty()) sb.append(' ');
            space = false;
            sb.append(c);
            if (sb.length() >= maxLength) break;
        }
        return sb.toString();
    }
}
