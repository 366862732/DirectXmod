/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.WorldData;
import org.slf4j.Logger;

public class ReloadCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void reloadPacks(Collection<String> selectedPacks, CommandSourceStack source) {
        source.getServer().reloadResources(selectedPacks).exceptionally(throwable -> {
            LOGGER.warn("Failed to execute reload", throwable);
            source.sendFailure(Component.translatable("commands.reload.failure"));
            return null;
        });
    }

    private static Collection<String> discoverNewPacks(PackRepository packRepository, WorldData worldData, Collection<String> currentPacks) {
        packRepository.reload();
        ArrayList selected = Lists.newArrayList(currentPacks);
        List<String> disabled = worldData.getDataConfiguration().dataPacks().getDisabled();
        for (String pack : packRepository.getAvailableIds()) {
            if (disabled.contains(pack) || selected.contains(pack)) continue;
            selected.add(pack);
        }
        return selected;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("reload").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).executes(s -> {
            CommandSourceStack source = (CommandSourceStack)s.getSource();
            MinecraftServer server = source.getServer();
            PackRepository packRepository = server.getPackRepository();
            WorldData worldData = server.getWorldData();
            Collection<String> currentPacks = packRepository.getSelectedIds();
            Collection<String> newSelectedPacks = ReloadCommand.discoverNewPacks(packRepository, worldData, currentPacks);
            source.sendSuccess(() -> Component.translatable("commands.reload.success"), true);
            ReloadCommand.reloadPacks(newSelectedPacks, source);
            return 0;
        }));
    }
}

