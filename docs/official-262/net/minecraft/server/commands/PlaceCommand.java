/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.FloatArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Optional;
import net.minecraft.IdentifierException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class PlaceCommand {
    private static final SimpleCommandExceptionType ERROR_FEATURE_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.place.feature.failed"));
    private static final SimpleCommandExceptionType ERROR_JIGSAW_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.place.jigsaw.failed"));
    private static final SimpleCommandExceptionType ERROR_STRUCTURE_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.place.structure.failed"));
    private static final DynamicCommandExceptionType ERROR_TEMPLATE_INVALID = new DynamicCommandExceptionType(value -> Component.translatableEscape("commands.place.template.invalid", value));
    private static final SimpleCommandExceptionType ERROR_TEMPLATE_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.place.template.failed"));
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEMPLATES = (context, builder) -> {
        StructureTemplateManager structureManager = ((CommandSourceStack)context.getSource()).getLevel().getStructureManager();
        return SharedSuggestionProvider.suggestResource(structureManager.listTemplates(), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("place").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("feature").then(((RequiredArgumentBuilder)Commands.argument("feature", ResourceKeyArgument.key(Registries.CONFIGURED_FEATURE)).executes(c -> PlaceCommand.placeFeature((CommandSourceStack)c.getSource(), ResourceKeyArgument.getConfiguredFeature((CommandContext<CommandSourceStack>)c, "feature"), BlockPos.containing(((CommandSourceStack)c.getSource()).getPosition())))).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> PlaceCommand.placeFeature((CommandSourceStack)c.getSource(), ResourceKeyArgument.getConfiguredFeature((CommandContext<CommandSourceStack>)c, "feature"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"))))))).then(Commands.literal("jigsaw").then(Commands.argument("pool", ResourceKeyArgument.key(Registries.TEMPLATE_POOL)).then(Commands.argument("target", IdentifierArgument.id()).then(((RequiredArgumentBuilder)Commands.argument("max_depth", IntegerArgumentType.integer((int)1, (int)20)).executes(c -> PlaceCommand.placeJigsaw((CommandSourceStack)c.getSource(), ResourceKeyArgument.getStructureTemplatePool((CommandContext<CommandSourceStack>)c, "pool"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "target"), IntegerArgumentType.getInteger((CommandContext)c, (String)"max_depth"), BlockPos.containing(((CommandSourceStack)c.getSource()).getPosition())))).then(Commands.argument("position", BlockPosArgument.blockPos()).executes(c -> PlaceCommand.placeJigsaw((CommandSourceStack)c.getSource(), ResourceKeyArgument.getStructureTemplatePool((CommandContext<CommandSourceStack>)c, "pool"), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "target"), IntegerArgumentType.getInteger((CommandContext)c, (String)"max_depth"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "position"))))))))).then(Commands.literal("structure").then(((RequiredArgumentBuilder)Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE)).executes(c -> PlaceCommand.placeStructure((CommandSourceStack)c.getSource(), ResourceKeyArgument.getStructure((CommandContext<CommandSourceStack>)c, "structure"), BlockPos.containing(((CommandSourceStack)c.getSource()).getPosition())))).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> PlaceCommand.placeStructure((CommandSourceStack)c.getSource(), ResourceKeyArgument.getStructure((CommandContext<CommandSourceStack>)c, "structure"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"))))))).then(Commands.literal("template").then(((RequiredArgumentBuilder)Commands.argument("template", IdentifierArgument.id()).suggests(SUGGEST_TEMPLATES).executes(c -> PlaceCommand.placeTemplate((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "template"), BlockPos.containing(((CommandSourceStack)c.getSource()).getPosition()), Rotation.NONE, Mirror.NONE, 1.0f, 0, false))).then(((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> PlaceCommand.placeTemplate((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "template"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), Rotation.NONE, Mirror.NONE, 1.0f, 0, false))).then(((RequiredArgumentBuilder)Commands.argument("rotation", TemplateRotationArgument.templateRotation()).executes(c -> PlaceCommand.placeTemplate((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "template"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), TemplateRotationArgument.getRotation((CommandContext<CommandSourceStack>)c, "rotation"), Mirror.NONE, 1.0f, 0, false))).then(((RequiredArgumentBuilder)Commands.argument("mirror", TemplateMirrorArgument.templateMirror()).executes(c -> PlaceCommand.placeTemplate((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "template"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), TemplateRotationArgument.getRotation((CommandContext<CommandSourceStack>)c, "rotation"), TemplateMirrorArgument.getMirror((CommandContext<CommandSourceStack>)c, "mirror"), 1.0f, 0, false))).then(((RequiredArgumentBuilder)Commands.argument("integrity", FloatArgumentType.floatArg((float)0.0f, (float)1.0f)).executes(c -> PlaceCommand.placeTemplate((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "template"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), TemplateRotationArgument.getRotation((CommandContext<CommandSourceStack>)c, "rotation"), TemplateMirrorArgument.getMirror((CommandContext<CommandSourceStack>)c, "mirror"), FloatArgumentType.getFloat((CommandContext)c, (String)"integrity"), 0, false))).then(((RequiredArgumentBuilder)Commands.argument("seed", IntegerArgumentType.integer()).executes(c -> PlaceCommand.placeTemplate((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "template"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), TemplateRotationArgument.getRotation((CommandContext<CommandSourceStack>)c, "rotation"), TemplateMirrorArgument.getMirror((CommandContext<CommandSourceStack>)c, "mirror"), FloatArgumentType.getFloat((CommandContext)c, (String)"integrity"), IntegerArgumentType.getInteger((CommandContext)c, (String)"seed"), false))).then(Commands.literal("strict").executes(c -> PlaceCommand.placeTemplate((CommandSourceStack)c.getSource(), IdentifierArgument.getId((CommandContext<CommandSourceStack>)c, "template"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), TemplateRotationArgument.getRotation((CommandContext<CommandSourceStack>)c, "rotation"), TemplateMirrorArgument.getMirror((CommandContext<CommandSourceStack>)c, "mirror"), FloatArgumentType.getFloat((CommandContext)c, (String)"integrity"), IntegerArgumentType.getInteger((CommandContext)c, (String)"seed"), true)))))))))));
    }

    public static int placeFeature(CommandSourceStack source, Holder.Reference<ConfiguredFeature<?, ?>> featureHolder, BlockPos pos) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        ConfiguredFeature<?, ?> feature = featureHolder.value();
        ChunkPos chunkPos = ChunkPos.containing(pos);
        PlaceCommand.checkLoaded(level, new ChunkPos(chunkPos.x() - 1, chunkPos.z() - 1), new ChunkPos(chunkPos.x() + 1, chunkPos.z() + 1));
        if (!feature.place(level, level.getChunkSource().getGenerator(), level.getRandom(), pos)) {
            throw ERROR_FEATURE_FAILED.create();
        }
        String id = featureHolder.key().identifier().toString();
        source.sendSuccess(() -> Component.translatable("commands.place.feature.success", id, pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    public static int placeJigsaw(CommandSourceStack source, Holder<StructureTemplatePool> pool, Identifier target, int maxDepth, BlockPos pos) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        ChunkPos chunk = ChunkPos.containing(pos);
        PlaceCommand.checkLoaded(level, chunk, chunk);
        if (!JigsawPlacement.generateJigsaw(level, pool, target, maxDepth, pos, false)) {
            throw ERROR_JIGSAW_FAILED.create();
        }
        source.sendSuccess(() -> Component.translatable("commands.place.jigsaw.success", pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    public static int placeStructure(CommandSourceStack source, Holder.Reference<Structure> structureHolder, BlockPos pos) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        Structure structure = structureHolder.value();
        ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
        StructureStart start = structure.generate(structureHolder, level.dimension(), source.registryAccess(), chunkGenerator, chunkGenerator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), ChunkPos.containing(pos), 0, level, b -> true);
        if (!start.isValid()) {
            throw ERROR_STRUCTURE_FAILED.create();
        }
        BoundingBox boundingBox = start.getBoundingBox();
        ChunkPos chunkMin = new ChunkPos(SectionPos.blockToSectionCoord(boundingBox.minX()), SectionPos.blockToSectionCoord(boundingBox.minZ()));
        ChunkPos chunkMax = new ChunkPos(SectionPos.blockToSectionCoord(boundingBox.maxX()), SectionPos.blockToSectionCoord(boundingBox.maxZ()));
        PlaceCommand.checkLoaded(level, chunkMin, chunkMax);
        ChunkPos.rangeClosed(chunkMin, chunkMax).forEach(c -> start.placeInChunk(level, level.structureManager(), chunkGenerator, level.getRandom(), new BoundingBox(c.getMinBlockX(), level.getMinY(), c.getMinBlockZ(), c.getMaxBlockX(), level.getMaxY() + 1, c.getMaxBlockZ()), (ChunkPos)c));
        String id = structureHolder.key().identifier().toString();
        source.sendSuccess(() -> Component.translatable("commands.place.structure.success", id, pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    public static int placeTemplate(CommandSourceStack source, Identifier template, BlockPos pos, Rotation rotation, Mirror mirror, float integrity, int seed, boolean strict) throws CommandSyntaxException {
        boolean placed;
        Optional<StructureTemplate> maybeStructureTemplate;
        ServerLevel level = source.getLevel();
        StructureTemplateManager manager = level.getStructureManager();
        try {
            maybeStructureTemplate = manager.get(template);
        }
        catch (IdentifierException e) {
            throw ERROR_TEMPLATE_INVALID.create((Object)template);
        }
        if (maybeStructureTemplate.isEmpty()) {
            throw ERROR_TEMPLATE_INVALID.create((Object)template);
        }
        StructureTemplate structureTemplate = maybeStructureTemplate.get();
        PlaceCommand.checkLoaded(level, ChunkPos.containing(pos), ChunkPos.containing(pos.offset(structureTemplate.getSize())));
        StructurePlaceSettings placeSettings = new StructurePlaceSettings().setMirror(mirror).setRotation(rotation).setKnownShape(strict);
        if (integrity < 1.0f) {
            placeSettings.clearProcessors().addProcessor(new BlockRotProcessor(integrity)).setRandom(StructureBlockEntity.createRandom(seed));
        }
        if (!(placed = structureTemplate.placeInWorld(level, pos, pos, placeSettings, StructureBlockEntity.createRandom(seed), 2 | (strict ? 816 : 0)))) {
            throw ERROR_TEMPLATE_FAILED.create();
        }
        source.sendSuccess(() -> Component.translatable("commands.place.template.success", Component.translationArg(template), pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    private static void checkLoaded(ServerLevel level, ChunkPos chunkMin, ChunkPos chunkMax) throws CommandSyntaxException {
        if (ChunkPos.rangeClosed(chunkMin, chunkMax).filter(c -> !level.isLoaded(c.getWorldPosition())).findAny().isPresent()) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
        }
    }
}

