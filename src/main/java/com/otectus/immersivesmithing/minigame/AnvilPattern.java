package com.otectus.immersivesmithing.minigame;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * A data-driven strike pattern for the Anvil minigame. Target coordinates are normalised (0..1, y down) over
 * the workpiece silhouette; targets are visited in order and cycle when {@code strikes} exceeds the point count.
 */
public record AnvilPattern(ResourceLocation id, List<float[]> points, int strikes, float radius, int lifetimeMs,
                           float idealFraction, int gapMs, float jitter, float timeMultiplier, List<TagKey<Item>> categories) {

    public static AnvilPattern fallback(ResourceLocation id) {
        List<float[]> points = List.of(new float[]{0.3F, 0.3F}, new float[]{0.7F, 0.35F}, new float[]{0.5F, 0.5F},
                new float[]{0.3F, 0.7F}, new float[]{0.7F, 0.68F}, new float[]{0.5F, 0.3F}, new float[]{0.45F, 0.72F},
                new float[]{0.6F, 0.52F});
        return new AnvilPattern(id, points, 10, 0.075F, 1800, 0.6F, 250, 0.03F, 1.0F, List.of());
    }

    public float[] point(int index) {
        return points.get(Math.floorMod(index, points.size()));
    }

    public static AnvilPattern fromJson(ResourceLocation id, JsonObject json) {
        JsonArray arr = GsonHelper.getAsJsonArray(json, "targets");
        List<float[]> points = new ArrayList<>();
        for (JsonElement e : arr) {
            JsonArray p = e.getAsJsonArray();
            if (p.size() != 2) throw new JsonParseException("Each target is [x, y]");
            points.add(new float[]{Mth.clamp(p.get(0).getAsFloat(), 0F, 1F), Mth.clamp(p.get(1).getAsFloat(), 0F, 1F)});
        }
        if (points.isEmpty()) throw new JsonParseException("A pattern needs at least one target");
        if (points.size() > 64) throw new JsonParseException("A pattern may have at most 64 targets");
        List<TagKey<Item>> categories = new ArrayList<>();
        if (json.has("categories")) {
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "categories")) {
                categories.add(ItemTags.create(new ResourceLocation(e.getAsString())));
            }
        }
        return new AnvilPattern(id, List.copyOf(points),
                Mth.clamp(GsonHelper.getAsInt(json, "strikes", points.size()), 1, 64),
                Mth.clamp(GsonHelper.getAsFloat(json, "target_radius", 0.075F), 0.02F, 0.3F),
                Mth.clamp(Math.round(GsonHelper.getAsFloat(json, "target_lifetime", 1.8F) * 1000), 400, 10000),
                Mth.clamp(GsonHelper.getAsFloat(json, "ideal_fraction", 0.6F), 0.1F, 0.95F),
                Mth.clamp(Math.round(GsonHelper.getAsFloat(json, "gap", 0.25F) * 1000), 0, 5000),
                Mth.clamp(GsonHelper.getAsFloat(json, "jitter", 0.03F), 0F, 0.2F),
                Mth.clamp(GsonHelper.getAsFloat(json, "time_multiplier", 1.0F), 0.25F, 4F),
                List.copyOf(categories));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeVarInt(points.size());
        for (float[] p : points) {
            buf.writeFloat(p[0]);
            buf.writeFloat(p[1]);
        }
        buf.writeVarInt(strikes);
        buf.writeFloat(radius);
        buf.writeVarInt(lifetimeMs);
        buf.writeFloat(idealFraction);
        buf.writeVarInt(gapMs);
        buf.writeFloat(jitter);
        buf.writeFloat(timeMultiplier);
    }

    public static AnvilPattern read(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        int n = Mth.clamp(buf.readVarInt(), 1, 64);
        List<float[]> points = new ArrayList<>(n);
        for (int i = 0; i < n; i++) points.add(new float[]{buf.readFloat(), buf.readFloat()});
        return new AnvilPattern(id, List.copyOf(points), buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readFloat(),
                buf.readVarInt(), buf.readFloat(), buf.readFloat(), List.of());
    }
}
