package com.otectus.otessmithing.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.material.MaterialFamily;
import com.otectus.otessmithing.material.MaterialRegistry;
import com.otectus.otessmithing.material.MaterialResolver;
import com.otectus.otessmithing.material.RecyclingValue;
import com.otectus.otessmithing.minigame.SessionManager;
import com.otectus.otessmithing.minigame.SmithingSession;
import com.otectus.otessmithing.quality.QualityCalculator;
import com.otectus.otessmithing.quality.QualityData;
import com.otectus.otessmithing.recipe.AuxiliaryIngredient;
import com.otectus.otessmithing.recipe.CompatibilityReport;
import com.otectus.otessmithing.recipe.SmithingData;
import com.otectus.otessmithing.recipe.SmithingRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** {@code /otessmithing reload|material|recipe|quality|report|session} for administrators and pack authors. */
public final class SmithingCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("otessmithing")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("reload").executes(SmithingCommands::reload))
                .then(Commands.literal("material").then(Commands.argument("item", ItemArgument.item(context))
                        .executes(c -> material(c, ItemArgument.getItem(c, "item").getItem()))))
                .then(Commands.literal("recipe").then(Commands.argument("item", ItemArgument.item(context))
                        .executes(c -> recipe(c, ItemArgument.getItem(c, "item").getItem()))))
                .then(Commands.literal("quality").executes(SmithingCommands::quality))
                .then(Commands.literal("report").executes(SmithingCommands::report))
                .then(Commands.literal("session").executes(SmithingCommands::sessions)));
    }

    /** A full datapack reload: tags, materials, patterns, recipes and recipe suppression are all rebuilt. */
    private static int reload(CommandContext<CommandSourceStack> c) {
        CommandSourceStack source = c.getSource();
        source.sendSuccess(() -> Component.translatable("command.otes_smithing.reload"), true);
        ReloadCommand.reloadPacks(source.getServer().getPackRepository().getSelectedIds(), source);
        return 1;
    }

    private static int material(CommandContext<CommandSourceStack> c, Item item) {
        SmithingData data = SmithingData.current();
        MaterialRegistry.Entry entry = data.materials().lookup(item);
        ItemStack stack = new ItemStack(item);
        if (entry != null) {
            Component family = data.materials().family(entry.family()).map(MaterialFamily::displayName).orElse(Component.literal("?"));
            send(c, Component.translatable("command.otes_smithing.material.source", stack.getHoverName(), family,
                    entry.family().toString(), entry.units(), entry.rule()));
            return 1;
        }
        if (data.materials().isAmbiguous(item)) {
            send(c, Component.translatable("command.otes_smithing.material.ambiguous", stack.getHoverName()));
            return 0;
        }
        Optional<RecyclingValue> value = MaterialResolver.recyclingValue(stack);
        if (value.isPresent()) {
            send(c, Component.translatable("command.otes_smithing.material.recyclable", stack.getHoverName(),
                    value.get().family().toString(), MaterialResolver.recycledUnits(value.get().units()), value.get().units(), value.get().rule()));
            return 1;
        }
        send(c, Component.translatable("command.otes_smithing.material.none", stack.getHoverName()));
        return 0;
    }

    private static int recipe(CommandContext<CommandSourceStack> c, Item item) {
        SmithingData data = SmithingData.current();
        ItemStack stack = new ItemStack(item);
        List<SmithingRecipe> recipes = data.recipesProducing(item);
        if (recipes.isEmpty()) {
            send(c, Component.translatable("command.otes_smithing.recipe.none", stack.getHoverName()));
        }
        for (SmithingRecipe r : recipes) {
            send(c, Component.translatable(r.isAuto() ? "command.otes_smithing.recipe.auto" : "command.otes_smithing.recipe.explicit",
                    r.getId().toString(), r.family().toString(), r.metalUnits(),
                    r.forgePattern().toString(), r.anvilPattern().toString()));
            for (AuxiliaryIngredient aux : r.auxiliary()) {
                send(c, Component.translatable("command.otes_smithing.recipe.auxiliary", aux.count(), aux.displayStack().getHoverName()));
            }
            if (r.sourceRecipe() != null) {
                send(c, Component.translatable("command.otes_smithing.recipe.source", r.sourceRecipe().toString()));
            }
        }
        for (Map.Entry<ResourceLocation, Item> suppressed : data.suppressedRecipes().entrySet()) {
            if (suppressed.getValue() == item) {
                send(c, Component.translatable("command.otes_smithing.recipe.suppressed", suppressed.getKey().toString()));
            }
        }
        return recipes.size();
    }

    private static int quality(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        Optional<QualityData> data = QualityData.get(held);
        if (data.isEmpty()) {
            send(c, Component.translatable("command.otes_smithing.quality.none", held.getHoverName()));
            return 0;
        }
        QualityData q = data.get();
        send(c, Component.translatable("command.otes_smithing.quality.info", held.getHoverName(), q.quality().displayName(),
                q.forgeScore(), q.anvilScore(), q.faulty(),
                String.format("%.3f", QualityCalculator.durabilityMultiplier(q)),
                String.format("%.3f", QualityCalculator.efficacyMultiplier(q))));
        return 1;
    }

    private static int report(CommandContext<CommandSourceStack> c) {
        CompatibilityReport report = SmithingData.current().report();
        Path dir = c.getSource().getServer().getServerDirectory().toPath().resolve("logs");
        try {
            Path file = report.write(dir);
            report.lines().forEach(OtesSmithing.LOGGER::info);
            send(c, Component.translatable("command.otes_smithing.report", report.summary(), file.toString()));
            return 1;
        } catch (IOException e) {
            c.getSource().sendFailure(Component.translatable("command.otes_smithing.report.failed", e.getMessage()));
            return 0;
        }
    }

    private static int sessions(CommandContext<CommandSourceStack> c) {
        var sessions = SessionManager.all();
        if (sessions.isEmpty()) {
            send(c, Component.translatable("command.otes_smithing.session.none"));
            return 0;
        }
        for (SmithingSession s : sessions) {
            ServerPlayer player = c.getSource().getServer().getPlayerList().getPlayer(s.playerId);
            String name = player != null ? player.getGameProfile().getName() : s.playerId.toString();
            send(c, Component.translatable("command.otes_smithing.session.entry", name, s.kind().name().toLowerCase(),
                    s.pos.toShortString(), s.dimension.location().toString(), s.isPlaying()));
        }
        return sessions.size();
    }

    private static void send(CommandContext<CommandSourceStack> c, Component message) {
        c.getSource().sendSuccess(() -> message.copy().withStyle(ChatFormatting.GRAY), false);
    }

    private SmithingCommands() {}
}
