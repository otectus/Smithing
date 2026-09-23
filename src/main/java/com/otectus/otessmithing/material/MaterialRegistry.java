package com.otectus.otessmithing.material;

import com.otectus.otessmithing.config.ServerConfig;
import com.otectus.otessmithing.recipe.CompatibilityReport;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Immutable snapshot of every material family and the item-to-family lookup, rebuilt on each reload. */
public final class MaterialRegistry {
    public static final MaterialRegistry EMPTY = new MaterialRegistry(Map.of(), Map.of(), Set.of());

    /** Which family an item belongs to, and how many units one item is worth. */
    public record Entry(ResourceLocation family, int units, String rule) {}

    private final Map<ResourceLocation, MaterialFamily> families;
    private final Map<Item, Entry> lookup;
    private final Set<Item> ambiguous;

    private MaterialRegistry(Map<ResourceLocation, MaterialFamily> families, Map<Item, Entry> lookup, Set<Item> ambiguous) {
        this.families = families;
        this.lookup = lookup;
        this.ambiguous = ambiguous;
    }

    public Optional<MaterialFamily> family(@Nullable ResourceLocation id) {
        return id == null ? Optional.empty() : Optional.ofNullable(families.get(id));
    }

    public Collection<MaterialFamily> families() {
        return families.values();
    }

    @Nullable
    public Entry lookup(Item item) {
        return lookup.get(item);
    }

    public boolean isAmbiguous(Item item) {
        return ambiguous.contains(item);
    }

    public Set<Item> ambiguousItems() {
        return ambiguous;
    }

    /** Resolves datapack definitions (explicit) and, when enabled, forge:ingots/* tags (automatic) against bound tags. */
    public static MaterialRegistry build(Collection<MaterialDefinition> definitions, CompatibilityReport report) {
        Set<String> blacklist = new HashSet<>(ServerConfig.list(ServerConfig.MATERIAL_FAMILY_BLACKLIST));
        Builder builder = new Builder(report);

        for (MaterialDefinition def : definitions) {
            if (blacklist.contains(def.family().toString())) {
                report.skippedFamily(def.family(), "blacklisted by config");
                continue;
            }
            List<MaterialFamily.Source> sources = new ArrayList<>();
            for (MaterialDefinition.SourceDefinition src : def.sources()) {
                for (Item item : resolve(src)) {
                    sources.add(new MaterialFamily.Source(item, src.units(), src.describe()));
                }
            }
            if (sources.isEmpty()) {
                report.skippedFamily(def.family(), "no items match its sources");
                continue;
            }
            Component name = def.displayName() != null ? def.displayName() : MaterialFamily.defaultName(def.family());
            builder.add(new MaterialFamily(def.family(), name, def.meltMultiplier(), def.requiresIgnition(),
                    def.requiredFuelTag(), def.tint(), false, sources));
        }

        if (ServerConfig.get(ServerConfig.AUTO_DETECT_MODDED_METALS)) {
            Set<String> exclusions = new HashSet<>(ServerConfig.list(ServerConfig.AUTO_METAL_EXCLUSIONS));
            List<TagKey<Item>> ingotTags = BuiltInRegistries.ITEM.getTagNames()
                    .filter(t -> t.location().getNamespace().equals("forge"))
                    .filter(t -> t.location().getPath().startsWith("ingots/"))
                    .filter(t -> t.location().getPath().indexOf('/', 7) < 0)
                    .sorted(Comparator.comparing(t -> t.location().toString()))
                    .toList();
            for (TagKey<Item> ingotTag : ingotTags) {
                String metal = ingotTag.location().getPath().substring("ingots/".length());
                ResourceLocation familyId = new ResourceLocation("forge", metal);
                if (metal.isEmpty() || exclusions.contains(metal) || blacklist.contains(familyId.toString())) continue;
                List<Item> ingots = items(ingotTag);
                if (ingots.isEmpty()) continue;
                if (ingots.stream().anyMatch(builder::claimed)) continue; // an explicit family already covers this metal
                List<MaterialFamily.Source> sources = new ArrayList<>();
                addTag(sources, ingotTag, MaterialUnits.INGOT);
                addTag(sources, forgeTag("nuggets/" + metal), MaterialUnits.NUGGET);
                addTag(sources, forgeTag("storage_blocks/" + metal), MaterialUnits.BLOCK);
                addTag(sources, forgeTag("raw_materials/" + metal), MaterialUnits.INGOT);
                addTag(sources, forgeTag("storage_blocks/raw_" + metal), MaterialUnits.BLOCK);
                sources.removeIf(s -> builder.claimed(s.item()));
                builder.add(new MaterialFamily(familyId, MaterialFamily.defaultName(familyId), 1.0F, true, null,
                        MaterialFamily.DEFAULT_TINT, true, sources));
            }
        }
        return builder.build();
    }

    private static TagKey<Item> forgeTag(String path) {
        return ItemTags.create(new ResourceLocation("forge", path));
    }

    private static void addTag(List<MaterialFamily.Source> out, TagKey<Item> tag, int units) {
        for (Item item : items(tag)) out.add(new MaterialFamily.Source(item, units, "#" + tag.location()));
    }

    private static List<Item> items(TagKey<Item> tag) {
        return BuiltInRegistries.ITEM.getTag(tag)
                .map(set -> set.stream().map(Holder::value).toList())
                .orElse(List.of());
    }

    private static List<Item> resolve(MaterialDefinition.SourceDefinition src) {
        if (src.tag() != null) return items(src.tag());
        if (src.item() != null && BuiltInRegistries.ITEM.containsKey(src.item())) {
            return List.of(BuiltInRegistries.ITEM.get(src.item()));
        }
        return List.of();
    }

    private static final class Builder {
        private final CompatibilityReport report;
        private final Map<ResourceLocation, MaterialFamily> families = new LinkedHashMap<>();
        private final Map<Item, Entry> lookup = new HashMap<>();
        private final Set<Item> ambiguous = new HashSet<>();

        Builder(CompatibilityReport report) {
            this.report = report;
        }

        boolean claimed(Item item) {
            return lookup.containsKey(item) || ambiguous.contains(item);
        }

        void add(MaterialFamily family) {
            if (family.sources().isEmpty()) return;
            families.put(family.id(), family);
            for (MaterialFamily.Source source : family.sources()) {
                if (ambiguous.contains(source.item())) continue;
                Entry existing = lookup.get(source.item());
                if (existing == null) {
                    lookup.put(source.item(), new Entry(family.id(), source.units(), source.rule()));
                } else if (!existing.family().equals(family.id())) {
                    lookup.remove(source.item());
                    ambiguous.add(source.item());
                    report.ambiguousItem(source.item(), existing.family() + " and " + family.id());
                } else if (existing.units() != source.units()) {
                    report.note("Item " + BuiltInRegistries.ITEM.getKey(source.item()) + " has two unit values in "
                            + family.id() + "; keeping " + existing.units());
                }
            }
        }

        MaterialRegistry build() {
            Map<ResourceLocation, MaterialFamily> cleaned = new LinkedHashMap<>();
            for (MaterialFamily f : families.values()) {
                List<MaterialFamily.Source> sources = f.sources().stream()
                        .filter(s -> !ambiguous.contains(s.item()))
                        .filter(s -> {
                            Entry e = lookup.get(s.item());
                            return e != null && e.family().equals(f.id());
                        })
                        .toList();
                if (sources.isEmpty()) {
                    report.skippedFamily(f.id(), "every source item is claimed by another family");
                    continue;
                }
                cleaned.put(f.id(), new MaterialFamily(f.id(), f.displayName(), f.meltMultiplier(), f.requiresIgnition(),
                        f.requiredFuelTag(), f.tint(), f.auto(), sources));
                report.family(cleaned.get(f.id()));
            }
            return new MaterialRegistry(Map.copyOf(cleaned), Map.copyOf(lookup), Set.copyOf(ambiguous));
        }
    }
}
