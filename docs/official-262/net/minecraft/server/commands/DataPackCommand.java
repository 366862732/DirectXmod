/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.stream.JsonWriter
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DataResult$Error
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  org.slf4j.Logger
 */
package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.util.FileUtil;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

public class DataPackCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PACK = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.datapack.unknown", id));
    private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_ENABLED = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.datapack.enable.failed", id));
    private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_DISABLED = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.datapack.disable.failed", id));
    private static final DynamicCommandExceptionType ERROR_CANNOT_DISABLE_FEATURE = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.datapack.disable.failed.feature", id));
    private static final Dynamic2CommandExceptionType ERROR_PACK_FEATURES_NOT_ENABLED = new Dynamic2CommandExceptionType((id, flags) -> Component.translatableEscape("commands.datapack.enable.failed.no_flags", id, flags));
    private static final DynamicCommandExceptionType ERROR_PACK_INVALID_NAME = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.datapack.create.invalid_name", id));
    private static final DynamicCommandExceptionType ERROR_PACK_INVALID_FULL_NAME = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.datapack.create.invalid_full_name", id));
    private static final DynamicCommandExceptionType ERROR_PACK_ALREADY_EXISTS = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.datapack.create.already_exists", id));
    private static final Dynamic2CommandExceptionType ERROR_PACK_METADATA_ENCODE_FAILURE = new Dynamic2CommandExceptionType((id, error) -> Component.translatableEscape("commands.datapack.create.metadata_encode_failure", id, error));
    private static final DynamicCommandExceptionType ERROR_PACK_IO_FAILURE = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.datapack.create.io_failure", id));
    private static final SuggestionProvider<CommandSourceStack> SELECTED_PACKS = (c, p) -> SharedSuggestionProvider.suggest(((CommandSourceStack)c.getSource()).getServer().getPackRepository().getSelectedIds().stream().map(StringArgumentType::escapeIfRequired), p);
    private static final SuggestionProvider<CommandSourceStack> UNSELECTED_PACKS = (c, p) -> {
        PackRepository packRepository = ((CommandSourceStack)c.getSource()).getServer().getPackRepository();
        Collection<String> selectedIds = packRepository.getSelectedIds();
        FeatureFlagSet enabledFeatures = ((CommandSourceStack)c.getSource()).enabledFeatures();
        return SharedSuggestionProvider.suggest(packRepository.getAvailablePacks().stream().filter(pack -> pack.getRequestedFeatures().isSubsetOf(enabledFeatures)).map(Pack::getId).filter(id -> !selectedIds.contains(id)).map(StringArgumentType::escapeIfRequired), p);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("datapack").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("enable").then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("name", StringArgumentType.string()).suggests(UNSELECTED_PACKS).executes(c -> DataPackCommand.enablePack((CommandSourceStack)c.getSource(), DataPackCommand.getPack((CommandContext<CommandSourceStack>)c, "name", true), (l, p) -> p.getDefaultPosition().insert(l, p, Pack::selectionConfig, false)))).then(Commands.literal("after").then(Commands.argument("existing", StringArgumentType.string()).suggests(SELECTED_PACKS).executes(c -> DataPackCommand.enablePack((CommandSourceStack)c.getSource(), DataPackCommand.getPack((CommandContext<CommandSourceStack>)c, "name", true), (l, p) -> l.add(l.indexOf(DataPackCommand.getPack((CommandContext<CommandSourceStack>)c, "existing", false)) + 1, p)))))).then(Commands.literal("before").then(Commands.argument("existing", StringArgumentType.string()).suggests(SELECTED_PACKS).executes(c -> DataPackCommand.enablePack((CommandSourceStack)c.getSource(), DataPackCommand.getPack((CommandContext<CommandSourceStack>)c, "name", true), (l, p) -> l.add(l.indexOf(DataPackCommand.getPack((CommandContext<CommandSourceStack>)c, "existing", false)), p)))))).then(Commands.literal("last").executes(c -> DataPackCommand.enablePack((CommandSourceStack)c.getSource(), DataPackCommand.getPack((CommandContext<CommandSourceStack>)c, "name", true), List::add)))).then(Commands.literal("first").executes(c -> DataPackCommand.enablePack((CommandSourceStack)c.getSource(), DataPackCommand.getPack((CommandContext<CommandSourceStack>)c, "name", true), (l, p) -> l.add(0, p))))))).then(Commands.literal("disable").then(Commands.argument("name", StringArgumentType.string()).suggests(SELECTED_PACKS).executes(c -> DataPackCommand.disablePack((CommandSourceStack)c.getSource(), DataPackCommand.getPack((CommandContext<CommandSourceStack>)c, "name", false)))))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("list").executes(c -> DataPackCommand.listPacks((CommandSourceStack)c.getSource()))).then(Commands.literal("available").executes(c -> DataPackCommand.listAvailablePacks((CommandSourceStack)c.getSource())))).then(Commands.literal("enabled").executes(c -> DataPackCommand.listEnabledPacks((CommandSourceStack)c.getSource()))))).then(((LiteralArgumentBuilder)Commands.literal("create").requires(Commands.hasPermission(Commands.LEVEL_OWNERS))).then(Commands.argument("id", StringArgumentType.string()).then(Commands.argument("description", ComponentArgument.textComponent(context)).executes(c -> DataPackCommand.createPack((CommandSourceStack)c.getSource(), StringArgumentType.getString((CommandContext)c, (String)"id"), ComponentArgument.getResolvedComponent((CommandContext<CommandSourceStack>)c, "description")))))));
    }

    private static int createPack(CommandSourceStack source, String id, Component description) throws CommandSyntaxException {
        Path datapackDir = source.getServer().getWorldPath(LevelResource.DATAPACK_DIR);
        if (!FileUtil.isValidPathSegment(id)) {
            throw ERROR_PACK_INVALID_NAME.create((Object)id);
        }
        if (!FileUtil.isPathPartPortable(id)) {
            throw ERROR_PACK_INVALID_FULL_NAME.create((Object)id);
        }
        Path packDir = datapackDir.resolve(id);
        if (Files.exists(packDir, new LinkOption[0])) {
            throw ERROR_PACK_ALREADY_EXISTS.create((Object)id);
        }
        PackMetadataSection packMetadataSection = new PackMetadataSection(description, SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).minorRange());
        DataResult encodedMeta = PackMetadataSection.SERVER_TYPE.codec().encodeStart((DynamicOps)JsonOps.INSTANCE, (Object)packMetadataSection);
        Optional error = encodedMeta.error();
        if (error.isPresent()) {
            throw ERROR_PACK_METADATA_ENCODE_FAILURE.create((Object)id, (Object)((DataResult.Error)error.get()).message());
        }
        JsonObject topMcmeta = new JsonObject();
        topMcmeta.add(PackMetadataSection.SERVER_TYPE.name(), (JsonElement)encodedMeta.getOrThrow());
        try {
            Files.createDirectory(packDir, new FileAttribute[0]);
            Files.createDirectory(packDir.resolve(PackType.SERVER_DATA.getDirectory()), new FileAttribute[0]);
            try (BufferedWriter mcmetaFile = Files.newBufferedWriter(packDir.resolve("pack.mcmeta"), StandardCharsets.UTF_8, new OpenOption[0]);
                 JsonWriter jsonWriter = new JsonWriter((Writer)mcmetaFile);){
                jsonWriter.setSerializeNulls(false);
                jsonWriter.setIndent("  ");
                GsonHelper.writeValue(jsonWriter, (JsonElement)topMcmeta, null);
            }
        }
        catch (IOException e) {
            LOGGER.warn("Failed to create pack at {}", (Object)datapackDir.toAbsolutePath(), (Object)e);
            throw ERROR_PACK_IO_FAILURE.create((Object)id);
        }
        source.sendSuccess(() -> Component.translatable("commands.datapack.create.success", id), true);
        return 1;
    }

    private static int enablePack(CommandSourceStack source, Pack unopened, Inserter inserter) throws CommandSyntaxException {
        PackRepository packRepository = source.getServer().getPackRepository();
        ArrayList selected = Lists.newArrayList(packRepository.getSelectedPacks());
        inserter.apply(selected, unopened);
        source.sendSuccess(() -> Component.translatable("commands.datapack.modify.enable", unopened.getChatLink(true)), true);
        ReloadCommand.reloadPacks(selected.stream().map(Pack::getId).collect(Collectors.toList()), source);
        return selected.size();
    }

    private static int disablePack(CommandSourceStack source, Pack unopened) {
        PackRepository packRepository = source.getServer().getPackRepository();
        ArrayList selected = Lists.newArrayList(packRepository.getSelectedPacks());
        selected.remove(unopened);
        source.sendSuccess(() -> Component.translatable("commands.datapack.modify.disable", unopened.getChatLink(true)), true);
        ReloadCommand.reloadPacks(selected.stream().map(Pack::getId).collect(Collectors.toList()), source);
        return selected.size();
    }

    private static int listPacks(CommandSourceStack source) {
        return DataPackCommand.listEnabledPacks(source) + DataPackCommand.listAvailablePacks(source);
    }

    private static int listAvailablePacks(CommandSourceStack source) {
        PackRepository repository = source.getServer().getPackRepository();
        repository.reload();
        Collection<Pack> selectedPacks = repository.getSelectedPacks();
        Collection<Pack> availablePacks = repository.getAvailablePacks();
        FeatureFlagSet enabledFeatures = source.enabledFeatures();
        List<Pack> unselectedPacks = availablePacks.stream().filter(p -> !selectedPacks.contains(p) && p.getRequestedFeatures().isSubsetOf(enabledFeatures)).toList();
        if (unselectedPacks.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.datapack.list.available.none"), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.datapack.list.available.success", unselectedPacks.size(), ComponentUtils.formatList(unselectedPacks, p -> p.getChatLink(false))), false);
        }
        return unselectedPacks.size();
    }

    private static int listEnabledPacks(CommandSourceStack source) {
        PackRepository repository = source.getServer().getPackRepository();
        repository.reload();
        Collection<Pack> selectedPacks = repository.getSelectedPacks();
        if (selectedPacks.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.datapack.list.enabled.none"), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.datapack.list.enabled.success", selectedPacks.size(), ComponentUtils.formatList(selectedPacks, p -> p.getChatLink(true))), false);
        }
        return selectedPacks.size();
    }

    private static Pack getPack(CommandContext<CommandSourceStack> context, String name, boolean enabling) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, (String)name);
        PackRepository repository = ((CommandSourceStack)context.getSource()).getServer().getPackRepository();
        Pack pack = repository.getPack(id);
        if (pack == null) {
            throw ERROR_UNKNOWN_PACK.create((Object)id);
        }
        boolean enabled = repository.getSelectedPacks().contains(pack);
        if (enabling && enabled) {
            throw ERROR_PACK_ALREADY_ENABLED.create((Object)id);
        }
        if (!enabling && !enabled) {
            throw ERROR_PACK_ALREADY_DISABLED.create((Object)id);
        }
        FeatureFlagSet availableFeatures = ((CommandSourceStack)context.getSource()).enabledFeatures();
        FeatureFlagSet requestedFeatures = pack.getRequestedFeatures();
        if (!enabling && !requestedFeatures.isEmpty() && pack.getPackSource() == PackSource.FEATURE) {
            throw ERROR_CANNOT_DISABLE_FEATURE.create((Object)id);
        }
        if (!requestedFeatures.isSubsetOf(availableFeatures)) {
            throw ERROR_PACK_FEATURES_NOT_ENABLED.create((Object)id, (Object)FeatureFlags.printMissingFlags(availableFeatures, requestedFeatures));
        }
        return pack;
    }

    private static interface Inserter {
        public void apply(List<Pack> var1, Pack var2) throws CommandSyntaxException;
    }
}

