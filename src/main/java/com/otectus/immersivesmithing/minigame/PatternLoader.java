package com.otectus.immersivesmithing.minigame;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.otectus.immersivesmithing.ImmersiveSmithing;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code data/<namespace>/immersive_smithing/anvil_patterns/*.json} and {@code .../forge_patterns/*.json}.
 * The file id is the pattern id.
 */
public class PatternLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public enum Kind { FORGE, ANVIL }

    private final Kind kind;

    public PatternLoader(Kind kind) {
        super(GSON, kind == Kind.FORGE ? "immersive_smithing/forge_patterns" : "immersive_smithing/anvil_patterns");
        this.kind = kind;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        if (kind == Kind.FORGE) {
            Map<ResourceLocation, ForgePattern> map = new HashMap<>();
            files.forEach((id, json) -> {
                try {
                    JsonObject obj = GsonHelper.convertToJsonObject(json, "forge pattern");
                    map.put(id, ForgePattern.fromJson(id, obj));
                } catch (RuntimeException e) {
                    ImmersiveSmithing.LOGGER.error("Skipping malformed forge pattern {}: {}", id, e.getMessage());
                }
            });
            PatternRegistry.setForgePatterns(map);
        } else {
            Map<ResourceLocation, AnvilPattern> map = new HashMap<>();
            files.forEach((id, json) -> {
                try {
                    JsonObject obj = GsonHelper.convertToJsonObject(json, "anvil pattern");
                    map.put(id, AnvilPattern.fromJson(id, obj));
                } catch (RuntimeException e) {
                    ImmersiveSmithing.LOGGER.error("Skipping malformed anvil pattern {}: {}", id, e.getMessage());
                }
            });
            PatternRegistry.setAnvilPatterns(map);
        }
    }
}
