package com.otectus.immersivesmithing.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** A material family as written in a datapack, before tags are resolved. */
public record MaterialDefinition(
        ResourceLocation family,
        @Nullable Component displayName,
        List<SourceDefinition> sources,
        float meltMultiplier,
        boolean requiresIgnition,
        @Nullable TagKey<Item> requiredFuelTag,
        int tint,
        UpgradePolicy upgradePolicy) {

    public record SourceDefinition(@Nullable TagKey<Item> tag, @Nullable ResourceLocation item, int units) {
        public String describe() {
            return tag != null ? "#" + tag.location() : String.valueOf(item);
        }
    }

    public static MaterialDefinition fromJson(JsonObject json) {
        ResourceLocation family = new ResourceLocation(GsonHelper.getAsString(json, "family"));
        Component name = null;
        if (json.has("display_name")) {
            JsonElement el = json.get("display_name");
            if (el.isJsonPrimitive()) {
                String s = el.getAsString();
                name = Component.translatableWithFallback(s, s);
            } else {
                name = Component.Serializer.fromJson(el);
            }
        }
        List<SourceDefinition> sources = new ArrayList<>();
        JsonArray arr = GsonHelper.getAsJsonArray(json, "sources");
        for (JsonElement e : arr) {
            JsonObject o = GsonHelper.convertToJsonObject(e, "source");
            int units = GsonHelper.getAsInt(o, "units");
            if (units <= 0) throw new JsonParseException("Source units must be positive");
            if (o.has("tag")) {
                sources.add(new SourceDefinition(ItemTags.create(new ResourceLocation(GsonHelper.getAsString(o, "tag"))), null, units));
            } else if (o.has("item")) {
                sources.add(new SourceDefinition(null, new ResourceLocation(GsonHelper.getAsString(o, "item")), units));
            } else {
                throw new JsonParseException("Source needs a 'tag' or an 'item'");
            }
        }
        float melt = GsonHelper.getAsFloat(json, "melt_time_multiplier", 1.0F);
        if (melt <= 0) throw new JsonParseException("melt_time_multiplier must be positive");
        boolean ignition = GsonHelper.getAsBoolean(json, "requires_ignition", true);
        TagKey<Item> fuelTag = json.has("required_fuel_tag")
                ? ItemTags.create(new ResourceLocation(GsonHelper.getAsString(json, "required_fuel_tag"))) : null;
        int tint = MaterialFamily.DEFAULT_TINT;
        if (json.has("tint")) {
            JsonElement t = json.get("tint");
            if (t.isJsonPrimitive() && t.getAsJsonPrimitive().isString()) {
                String s = t.getAsString().replace("#", "");
                tint = Integer.parseInt(s, 16);
            } else {
                tint = t.getAsInt();
            }
        }
        UpgradePolicy policy = json.has("upgrade_policy")
                ? UpgradePolicy.byId(GsonHelper.getAsString(json, "upgrade_policy")) : UpgradePolicy.SHAPE;
        return new MaterialDefinition(family, name, List.copyOf(sources), melt, ignition, fuelTag, tint & 0xFFFFFF, policy);
    }

    /** Combines two definitions of the same family: sources are merged, scalar fields come from {@code later}. */
    public MaterialDefinition merge(MaterialDefinition later) {
        List<SourceDefinition> merged = new ArrayList<>(sources);
        merged.addAll(later.sources);
        return new MaterialDefinition(family, later.displayName != null ? later.displayName : displayName, List.copyOf(merged),
                later.meltMultiplier, later.requiresIgnition, later.requiredFuelTag, later.tint, later.upgradePolicy);
    }
}
