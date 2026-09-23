package com.otectus.otessmithing.api;

import com.otectus.otessmithing.OtesSmithing;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Public Java hooks for integrations whose items cannot be described by datapacks alone. Register during
 * {@code FMLCommonSetupEvent}, or send an IMC message to {@code otes_smithing} whose method is one of the
 * {@code IMC_*} names and whose payload is a {@link Supplier} of the matching interface.
 */
public final class OtesSmithingAPI {
    public static final String IMC_MATERIAL_PROVIDER = "material_provider";
    public static final String IMC_RECIPE_PROVIDER = "recipe_provider";
    public static final String IMC_EFFICACY_HANDLER = "efficacy_handler";
    public static final String IMC_SHIELD_HANDLER = "shield_handler";
    public static final String IMC_CLASSIFIER = "equipment_classifier";
    public static final String IMC_RECYCLING_PROVIDER = "recycling_provider";
    public static final String IMC_EXCLUSION = "exclusion";

    private static final List<ISmithingMaterialProvider> MATERIAL_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<ISmithingRecipeProvider> RECIPE_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<ISmithingEfficacyHandler> EFFICACY_HANDLERS = new CopyOnWriteArrayList<>();
    private static final List<IShieldSmithingHandler> SHIELD_HANDLERS = new CopyOnWriteArrayList<>();
    private static final List<IEquipmentClassifier> CLASSIFIERS = new CopyOnWriteArrayList<>();
    private static final List<IRecyclingValueProvider> RECYCLING_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<Predicate<ItemStack>> EXCLUSIONS = new CopyOnWriteArrayList<>();

    public static void registerMaterialProvider(ISmithingMaterialProvider provider) { MATERIAL_PROVIDERS.add(provider); }
    public static void registerRecipeProvider(ISmithingRecipeProvider provider) { RECIPE_PROVIDERS.add(provider); }
    public static void registerEfficacyHandler(ISmithingEfficacyHandler handler) { EFFICACY_HANDLERS.add(handler); }
    public static void registerShieldHandler(IShieldSmithingHandler handler) { SHIELD_HANDLERS.add(handler); }
    public static void registerClassifier(IEquipmentClassifier classifier) { CLASSIFIERS.add(classifier); }
    public static void registerRecyclingProvider(IRecyclingValueProvider provider) { RECYCLING_PROVIDERS.add(provider); }
    /** Items matching the predicate are never auto-detected, disabled, recycled or given quality. */
    public static void registerExclusion(Predicate<ItemStack> exclusion) { EXCLUSIONS.add(exclusion); }

    public static List<ISmithingMaterialProvider> materialProviders() { return MATERIAL_PROVIDERS; }
    public static List<ISmithingRecipeProvider> recipeProviders() { return RECIPE_PROVIDERS; }
    public static List<ISmithingEfficacyHandler> efficacyHandlers() { return EFFICACY_HANDLERS; }
    public static List<IShieldSmithingHandler> shieldHandlers() { return SHIELD_HANDLERS; }
    public static List<IEquipmentClassifier> classifiers() { return CLASSIFIERS; }
    public static List<IRecyclingValueProvider> recyclingProviders() { return RECYCLING_PROVIDERS; }

    public static boolean isExcluded(ItemStack stack) {
        for (Predicate<ItemStack> p : EXCLUSIONS) {
            if (p.test(stack)) return true;
        }
        return false;
    }

    /** Drains IMC messages. Called from the mod-bus {@link InterModProcessEvent}. */
    @SuppressWarnings("unchecked")
    public static void processIMC(InterModProcessEvent event) {
        InterModComms.getMessages(OtesSmithing.MOD_ID).forEach(msg -> {
            try {
                Object payload = ((Supplier<Object>) msg.messageSupplier()).get();
                switch (msg.method()) {
                    case IMC_MATERIAL_PROVIDER -> registerMaterialProvider((ISmithingMaterialProvider) payload);
                    case IMC_RECIPE_PROVIDER -> registerRecipeProvider((ISmithingRecipeProvider) payload);
                    case IMC_EFFICACY_HANDLER -> registerEfficacyHandler((ISmithingEfficacyHandler) payload);
                    case IMC_SHIELD_HANDLER -> registerShieldHandler((IShieldSmithingHandler) payload);
                    case IMC_CLASSIFIER -> registerClassifier((IEquipmentClassifier) payload);
                    case IMC_RECYCLING_PROVIDER -> registerRecyclingProvider((IRecyclingValueProvider) payload);
                    case IMC_EXCLUSION -> registerExclusion((Predicate<ItemStack>) payload);
                    default -> OtesSmithing.LOGGER.warn("Unknown IMC method '{}' from {}", msg.method(), msg.senderModId());
                }
            } catch (ClassCastException e) {
                OtesSmithing.LOGGER.error("IMC message '{}' from {} has the wrong payload type", msg.method(), msg.senderModId());
            }
        });
    }

    private OtesSmithingAPI() {}
}
