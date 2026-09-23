package com.otectus.otessmithing.material;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * An explicit recycling value from {@code data/<namespace>/otes_smithing/recycling/*.json}:
 * {@code {"item": "mod:thing", "family": "minecraft:iron", "units": 18}} or
 * {@code {"tag": "mod:relics", "recyclable": false}}.
 */
public record RecyclingOverride(@Nullable ResourceLocation item, @Nullable TagKey<Item> tag, boolean recyclable,
                                @Nullable ResourceLocation family, int units) {

    public static RecyclingOverride fromJson(JsonObject json) {
        ResourceLocation item = json.has("item") ? new ResourceLocation(GsonHelper.getAsString(json, "item")) : null;
        TagKey<Item> tag = json.has("tag") ? ItemTags.create(new ResourceLocation(GsonHelper.getAsString(json, "tag"))) : null;
        if (item == null && tag == null) throw new JsonParseException("Recycling override needs an 'item' or a 'tag'");
        boolean recyclable = GsonHelper.getAsBoolean(json, "recyclable", true);
        ResourceLocation family = null;
        int units = 0;
        if (recyclable) {
            family = new ResourceLocation(GsonHelper.getAsString(json, "family"));
            units = GsonHelper.getAsInt(json, "units");
            if (units <= 0) throw new JsonParseException("Recycling units must be positive");
        }
        return new RecyclingOverride(item, tag, recyclable, family, units);
    }
}
