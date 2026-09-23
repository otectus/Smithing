package com.otectus.otessmithing.recipe;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.config.ServerConfig;
import com.otectus.otessmithing.material.MaterialFamily;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Everything the last rebuild decided, for {@code /otessmithing report} and compatibility bug reports. */
public final class CompatibilityReport {
    private final List<String> families = new ArrayList<>();
    private final List<String> skippedFamilies = new ArrayList<>();
    private final List<String> ambiguousItems = new ArrayList<>();
    private final List<String> explicitRecipes = new ArrayList<>();
    private final List<String> invalidRecipes = new ArrayList<>();
    private final List<String> autoRecipes = new ArrayList<>();
    private final List<String> skippedEquipment = new ArrayList<>();
    private final List<String> suppressed = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();

    public void family(MaterialFamily f) {
        families.add(f.id() + (f.auto() ? " (auto)" : "") + ": " + f.sources().size() + " source items");
    }

    public void skippedFamily(ResourceLocation id, String reason) {
        skippedFamilies.add(id + ": " + reason);
        log("Skipped material family {}: {}", id, reason);
    }

    public void ambiguousItem(Item item, String detail) {
        ambiguousItems.add(BuiltInRegistries.ITEM.getKey(item) + " claimed by " + detail);
        log("Ambiguous material item {} ({})", BuiltInRegistries.ITEM.getKey(item), detail);
    }

    public void explicitRecipe(SmithingRecipe r) {
        explicitRecipes.add(r.getId() + " -> " + BuiltInRegistries.ITEM.getKey(r.resultItem()) + " [" + r.family() + ", " + r.metalUnits() + " units]");
    }

    public void invalidRecipe(ResourceLocation id, String reason) {
        invalidRecipes.add(id + ": " + reason);
        OtesSmithing.LOGGER.warn("Smithing recipe {} is inactive: {}", id, reason);
    }

    public void autoRecipe(SmithingRecipe r) {
        autoRecipes.add(BuiltInRegistries.ITEM.getKey(r.resultItem()) + " from " + r.sourceRecipe()
                + " [" + r.family() + ", " + r.metalUnits() + " units, " + r.auxiliary().size() + " auxiliary]");
        log("Auto-detected smithing recipe for {} from {}", BuiltInRegistries.ITEM.getKey(r.resultItem()), r.sourceRecipe());
    }

    public void skippedEquipment(Item item, ResourceLocation recipe, String reason) {
        skippedEquipment.add(BuiltInRegistries.ITEM.getKey(item) + " (" + recipe + "): " + reason);
        log("Not auto-detecting {} from {}: {}", BuiltInRegistries.ITEM.getKey(item), recipe, reason);
    }

    public void suppressed(ResourceLocation recipe, Item result) {
        suppressed.add(recipe + " -> " + BuiltInRegistries.ITEM.getKey(result));
    }

    public void note(String note) {
        notes.add(note);
        log(note);
    }

    public int autoCount() { return autoRecipes.size(); }
    public int explicitCount() { return explicitRecipes.size(); }
    public int suppressedCount() { return suppressed.size(); }
    public int familyCount() { return families.size(); }
    public int skippedCount() { return skippedEquipment.size(); }
    public List<String> skippedEquipment() { return List.copyOf(skippedEquipment); }

    public String summary() {
        return families.size() + " material families, " + explicitRecipes.size() + " explicit and " + autoRecipes.size()
                + " automatic smithing recipes, " + suppressed.size() + " crafting recipes disabled, "
                + skippedEquipment.size() + " equipment recipes skipped";
    }

    public List<String> lines() {
        List<String> out = new ArrayList<>();
        out.add("Ote's Smithing compatibility report - " + LocalDateTime.now());
        out.add(summary());
        section(out, "Material families", families);
        section(out, "Skipped material families", skippedFamilies);
        section(out, "Ambiguous material items (claimed by several families, ignored)", ambiguousItems);
        section(out, "Explicit smithing recipes", explicitRecipes);
        section(out, "Inactive smithing recipes", invalidRecipes);
        section(out, "Automatically detected equipment", autoRecipes);
        section(out, "Skipped or unsupported equipment", skippedEquipment);
        section(out, "Disabled crafting recipes", suppressed);
        section(out, "Notes", notes);
        return out;
    }

    public Path write(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve("otes_smithing_report.txt");
        Files.write(file, lines(), StandardCharsets.UTF_8);
        return file;
    }

    private static void section(List<String> out, String title, List<String> lines) {
        out.add("");
        out.add("== " + title + " (" + lines.size() + ")");
        lines.stream().sorted().forEach(l -> out.add("  " + l));
    }

    private static void log(String message, Object... args) {
        if (ServerConfig.get(ServerConfig.DEBUG_LOGGING)) {
            OtesSmithing.LOGGER.info(message, args);
        } else {
            OtesSmithing.LOGGER.debug(message, args);
        }
    }
}
