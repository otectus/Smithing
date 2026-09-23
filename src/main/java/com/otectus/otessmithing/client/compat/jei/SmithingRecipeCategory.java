package com.otectus.otessmithing.client.compat.jei;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.material.MaterialUnits;
import com.otectus.otessmithing.recipe.AuxiliaryIngredient;
import com.otectus.otessmithing.recipe.SmithingRecipe;
import com.otectus.otessmithing.registry.ModItems;
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
    public static final RecipeType<SmithingRecipe> TYPE = RecipeType.create(OtesSmithing.MOD_ID, "smithing", SmithingRecipe.class);
    private static final int WIDTH = 176;
    private static final int HEIGHT = 86;

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
        return Component.translatable("jei.otes_smithing.category");
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
                    .addTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.otes_smithing.metal",
                            recipe.familyName(), recipe.metalUnits()).withStyle(ChatFormatting.GOLD)));
        }
        List<AuxiliaryIngredient> aux = recipe.auxiliary();
        for (int i = 0; i < Math.min(6, aux.size()); i++) {
            AuxiliaryIngredient a = aux.get(i);
            List<ItemStack> stacks = Arrays.stream(a.ingredient().getItems()).map(s -> s.copyWithCount(a.count())).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 3) * 18, 21 + (i / 3) * 18)
                    .addItemStacks(stacks)
                    .setBackground(slot, -1, -1);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 19, 19)
                .addItemStack(recipe.result())
                .setOutputSlotBackground()
                .addTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.otes_smithing.quality_note")
                        .withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void draw(SmithingRecipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        int y = 19;
        forge.draw(g, 56, y);
        arrow.draw(g, 72, y);
        anvil.draw(g, 96, y);
        arrow.draw(g, 112, y);
        trough.draw(g, 136, y);
        Font font = Minecraft.getInstance().font;
        Component metal = Component.translatable("jei.otes_smithing.material", recipe.familyName(), MaterialUnits.describe(recipe.metalUnits()));
        g.drawString(font, metal, 0, 60, 0xFF404040, false);
        Component pattern = Component.translatable("jei.otes_smithing.pattern",
                Component.translatable("pattern." + recipe.anvilPattern().getNamespace() + "." + recipe.anvilPattern().getPath()));
        if (recipe.isAuto()) pattern = pattern.copy().append(Component.translatable("jei.otes_smithing.auto"));
        g.drawString(font, pattern, 0, 72, 0xFF606060, false);
    }

    @Override
    public List<Component> getTooltipStrings(SmithingRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        List<Component> tips = new ArrayList<>();
        if (mouseY >= 19 && mouseY < 35) {
            if (mouseX >= 56 && mouseX < 72) {
                tips.add(Component.translatable("block.otes_smithing.smiths_forge"));
                tips.add(Component.translatable("jei.otes_smithing.step.forge").withStyle(ChatFormatting.GRAY));
            } else if (mouseX >= 96 && mouseX < 112) {
                tips.add(Component.translatable("block.otes_smithing.smiths_anvil"));
                tips.add(Component.translatable("jei.otes_smithing.step.anvil").withStyle(ChatFormatting.GRAY));
            } else if (mouseX >= 136 && mouseX < 152) {
                tips.add(Component.translatable("block.otes_smithing.smiths_trough"));
                tips.add(Component.translatable("jei.otes_smithing.step.trough").withStyle(ChatFormatting.GRAY));
            }
        }
        return tips;
    }

    @Override
    public ResourceLocation getRegistryName(SmithingRecipe recipe) {
        return recipe.getId();
    }
}
