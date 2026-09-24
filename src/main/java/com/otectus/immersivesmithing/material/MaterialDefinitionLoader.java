package com.otectus.immersivesmithing.material;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.recipe.SmithingData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads {@code data/<namespace>/immersive_smithing/materials/*.json}. Tags are not bound while reload listeners
 * run, so this only parses; {@link SmithingData} resolves the definitions once the reload completes.
 */
public class MaterialDefinitionLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile List<MaterialDefinition> definitions = List.of();

    private final ICondition.IContext context;

    public MaterialDefinitionLoader(ICondition.IContext context) {
        super(GSON, "immersive_smithing/materials");
        this.context = context;
    }

    public static Collection<MaterialDefinition> definitions() {
        return definitions;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, MaterialDefinition> byFamily = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : new TreeMap<>(files).entrySet()) {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "material");
                if (!CraftingHelper.processConditions(json, "conditions", context)) continue;
                MaterialDefinition def = MaterialDefinition.fromJson(json);
                byFamily.merge(def.family(), def, MaterialDefinition::merge);
            } catch (RuntimeException e) {
                ImmersiveSmithing.LOGGER.error("Skipping malformed smithing material {}: {}", entry.getKey(), e.getMessage());
            }
        }
        definitions = List.copyOf(byFamily.values());
        SmithingData.markDirty();
        ImmersiveSmithing.LOGGER.debug("Loaded {} smithing material definitions", definitions.size());
    }
}
