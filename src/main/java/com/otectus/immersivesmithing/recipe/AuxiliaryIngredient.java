package com.otectus.immersivesmithing.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;

/**
 * A non-metal ingredient (sticks, leather, gems...) consumed from the smith's inventory when forging starts.
 * With {@code consumesEquipment} the ingredient is a finished piece of equipment that the recipe reworks (a
 * netherite helmet upgraded with a boss metal): it is matched by item regardless of damage or quality, escrowed
 * like any other auxiliary, and replaced by the new piece with the quality of the new forging.
 */
public record AuxiliaryIngredient(Ingredient ingredient, int count, boolean consumesEquipment) {

    public AuxiliaryIngredient(Ingredient ingredient, int count) {
        this(ingredient, count, false);
    }

    public static AuxiliaryIngredient fromJson(JsonObject json) {
        Ingredient ingredient = CraftingHelper.getIngredient(json.get("ingredient"), false);
        int count = GsonHelper.getAsInt(json, "count", 1);
        if (count <= 0) throw new JsonParseException("Auxiliary count must be positive");
        boolean equipment = GsonHelper.getAsBoolean(json, "consume_equipment", false);
        return new AuxiliaryIngredient(ingredient, count, equipment);
    }

    public void write(FriendlyByteBuf buf) {
        ingredient.toNetwork(buf);
        buf.writeVarInt(count);
        buf.writeBoolean(consumesEquipment);
    }

    public static AuxiliaryIngredient read(FriendlyByteBuf buf) {
        return new AuxiliaryIngredient(Ingredient.fromNetwork(buf), buf.readVarInt(), buf.readBoolean());
    }

    /** A representative stack for display, with this ingredient's count. */
    public ItemStack displayStack() {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return ItemStack.EMPTY;
        return items[0].copyWithCount(count);
    }
}
