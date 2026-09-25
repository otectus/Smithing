package com.otectus.immersivesmithing.client.compat.jei;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.material.MaterialUnits;
import com.otectus.immersivesmithing.recipe.AuxiliaryIngredient;
import com.otectus.immersivesmithing.recipe.SmithingRecipe;
import com.otectus.immersivesmithing.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shows the whole conceptual process: metal and auxiliary components, then Forge, hot workpiece, Anvil,
 * Trough, and the finished equipment, with material family, unit cost and patterns.
 */
public class SmithingRecipeCategory implements IRecipeCategory<SmithingRecipe> {
    public static final RecipeType<SmithingRecipe> TYPE = RecipeType.create(ImmersiveSmithing.MOD_ID, "smithing", SmithingRecipe.class);
    private static final int WIDTH = 176;
    private static final int HEIGHT = 106;
    private static final int SOOT = 0xFF24272B;
    private static final int TIMBER = 0xFF49382E;
    private static final int BRONZE = 0xFFA77D4F;
    private static final int IVORY = 0xFFF1E5CF;
    private static final int IVORY_DIM = 0xFFB8AA94;

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;
    private final IDrawable forge;
    private final IDrawable anvil;
    private final IDrawable trough;

    public SmithingRecipeCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(ModItems.SMITHS_ANVIL.get()));
        this.slot = helper.getSlotDrawable();
        this.arrow = helper.getRecipeArrow();
        this.forge = helper.createDrawableItemStack(new ItemStack(ModItems.SMITHS_FORGE.get()));
        this.anvil = helper.createDrawableItemStack(new ItemStack(ModItems.SMITHS_ANVIL.get()));
        this.trough = helper.createDrawableItemStack(new ItemStack(ModItems.SMITHS_TROUGH.get()));
    }

    @Override
    public RecipeType<SmithingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.immersive_smithing.category");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SmithingRecipe recipe, IFocusGroup focuses) {
        List<List<ItemStack>> metal = recipe.metalDisplay();
        for (int i = 0; i < Math.min(2, metal.size()); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, 1)
                    .addItemStacks(metal.get(i))
                    .setBackground(slot, -1, -1)
                    .addTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.immersive_smithing.metal",
                            recipe.familyName(), MaterialUnits.describe(recipe.metalUnits())).withStyle(ChatFormatting.GOLD)));
        }
        List<AuxiliaryIngredient> aux = recipe.auxiliary();
        for (int i = 0; i < Math.min(6, aux.size()); i++) {
            AuxiliaryIngredient a = aux.get(i);
            List<ItemStack> stacks = Arrays.stream(a.ingredient().getItems()).map(s -> s.copyWithCount(a.count())).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 3) * 18, 21 + (i / 3) * 18)
                    .addItemStacks(stacks)
                    .setBackground(slot, -1, -1)
                    .addTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(a.consumesEquipment()
                            ? "jei.immersive_smithing.equipment_consumed" : "jei.immersive_smithing.component_consumed")
                            .withStyle(ChatFormatting.GRAY)));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 19, 19)
                .addItemStack(recipe.result())
                .setOutputSlotBackground()
                .addTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.immersive_smithing.quality_note")
                        .withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void draw(SmithingRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        g.fill(0, 0, WIDTH, HEIGHT, SOOT);
        g.fill(0, 0, WIDTH, 1, BRONZE);
        g.fill(0, HEIGHT - 1, WIDTH, HEIGHT, TIMBER);
        g.fill(53, 14, 155, 44, 0xFF151719);
        g.fill(53, 42, 155, 44, TIMBER);
        g.fill(53, 14, 155, 15, BRONZE);
        int y = 19;
        forge.draw(g, 56, y);
        arrow.draw(g, 72, y);
        anvil.draw(g, 96, y);
        arrow.draw(g, 112, y);
        trough.draw(g, 136, y);
        Font font = Minecraft.getInstance().font;
        Component metal = Component.translatable("jei.immersive_smithing.material", recipe.familyName(), MaterialUnits.describe(recipe.metalUnits()));
        g.drawString(font, trim(font, metal, WIDTH), 0, 59, IVORY, false);
        Component pattern = Component.translatable("jei.immersive_smithing.pattern",
                Component.translatable("pattern." + recipe.anvilPattern().getNamespace() + "." + recipe.anvilPattern().getPath()));
        if (recipe.isAuto()) pattern = pattern.copy().append(Component.translatable("jei.immersive_smithing.auto"));
        g.drawString(font, trim(font, pattern, WIDTH), 0, 71, IVORY_DIM, false);
        g.drawString(font, Component.translatable("jei.immersive_smithing.quality_forge"), 0, 83, BRONZE, false);
        g.drawString(font, Component.translatable("jei.immersive_smithing.quality_anvil"), 0, 94, BRONZE, false);
        if (recipe.auxiliary().size() > 6) {
            g.drawString(font, Component.translatable("jei.immersive_smithing.more_components", recipe.auxiliary().size() - 6), 42, 48, BRONZE, false);
        }
    }

    private static String trim(Font font, Component text, int width) {
        if (font.width(text) <= width) return text.getString();
        return font.plainSubstrByWidth(text.getString(), width - font.width("...")) + "...";
    }

    @Override
    public List<Component> getTooltipStrings(SmithingRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        List<Component> tips = new ArrayList<>();
        if (mouseY >= 19 && mouseY < 35) {
            if (mouseX >= 56 && mouseX < 72) {
                tips.add(Component.translatable("block.immersive_smithing.smiths_forge"));
                tips.add(Component.translatable("jei.immersive_smithing.step.forge").withStyle(ChatFormatting.GRAY));
            } else if (mouseX >= 96 && mouseX < 112) {
                tips.add(Component.translatable("block.immersive_smithing.smiths_anvil"));
                tips.add(Component.translatable("jei.immersive_smithing.step.anvil").withStyle(ChatFormatting.GRAY));
            } else if (mouseX >= 136 && mouseX < 152) {
                tips.add(Component.translatable("block.immersive_smithing.smiths_trough"));
                tips.add(Component.translatable("jei.immersive_smithing.step.trough").withStyle(ChatFormatting.GRAY));
            }
        } else if (mouseY >= 58 && mouseY < 70) {
            tips.add(Component.translatable("jei.immersive_smithing.material", recipe.familyName(), MaterialUnits.describe(recipe.metalUnits())));
            tips.add(Component.translatable("jei.immersive_smithing.metal_cost_explainer").withStyle(ChatFormatting.GRAY));
            if (!recipe.auxiliary().isEmpty()) {
                tips.add(Component.translatable("jei.immersive_smithing.components").withStyle(ChatFormatting.GOLD));
                for (AuxiliaryIngredient auxiliary : recipe.auxiliary()) {
                    ItemStack[] options = auxiliary.ingredient().getItems();
                    Component name = options.length == 0 ? Component.translatable("jei.immersive_smithing.unknown_component") : options[0].getHoverName();
                    tips.add(Component.translatable("jei.immersive_smithing.component_line", auxiliary.count(), name).withStyle(ChatFormatting.GRAY));
                }
            }
        } else if (mouseY >= 70 && mouseY < 82) {
            tips.add(Component.translatable("jei.immersive_smithing.pattern",
                    Component.translatable("pattern." + recipe.anvilPattern().getNamespace() + "." + recipe.anvilPattern().getPath())));
        } else if (mouseY >= 82) {
            tips.add(Component.translatable("jei.immersive_smithing.quality_note").withStyle(ChatFormatting.GRAY));
        }
        return tips;
    }

    @Override
    public ResourceLocation getRegistryName(SmithingRecipe recipe) {
        return recipe.getId();
    }
}
