package com.otectus.otessmithing.material;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.recipe.SmithingData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RecyclingOverrideLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile List<RecyclingOverride> overrides = List.of();

    private final ICondition.IContext context;

    public RecyclingOverrideLoader(ICondition.IContext context) {
        super(GSON, "otes_smithing/recycling");
        this.context = context;
    }

    public static List<RecyclingOverride> overrides() {
        return overrides;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<RecyclingOverride> list = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : new TreeMap<>(files).entrySet()) {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "recycling override");
                if (!CraftingHelper.processConditions(json, "conditions", context)) continue;
                list.add(RecyclingOverride.fromJson(json));
            } catch (RuntimeException e) {
                OtesSmithing.LOGGER.error("Skipping malformed recycling override {}: {}", entry.getKey(), e.getMessage());
            }
        }
        overrides = List.copyOf(list);
        SmithingData.markDirty();
    }
}
