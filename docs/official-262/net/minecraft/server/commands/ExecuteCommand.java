/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.Command
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.RedirectModifier
 *  com.mojang.brigadier.arguments.DoubleArgumentType
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.context.ContextChain
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.tree.CommandNode
 *  com.mojang.brigadier.tree.LiteralCommandNode
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.IntList
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.commands;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HeightmapTypeArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.SlotsArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.BossBarCommands;
import net.minecraft.server.commands.FunctionCommand;
import net.minecraft.server.commands.InCommandFunction;
import net.minecraft.server.commands.ItemCommands;
import net.minecraft.server.commands.StopwatchCommand;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.Stopwatch;
import net.minecraft.world.Stopwatches;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ExecuteCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TEST_AREA = 32768;
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((max, count) -> Component.translatableEscape("commands.execute.blocks.toobig", max, count));
    private static final SimpleCommandExceptionType ERROR_CONDITIONAL_FAILED = new SimpleCommandExceptionType((Message)Component.translatable("commands.execute.conditional.fail"));
    private static final DynamicCommandExceptionType ERROR_CONDITIONAL_FAILED_COUNT = new DynamicCommandExceptionType(count -> Component.translatableEscape("commands.execute.conditional.fail_count", count));
    @VisibleForTesting
    public static final Dynamic2CommandExceptionType ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType((id, reason) -> Component.translatableEscape("commands.execute.function.instantiationFailure", id, reason));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        LiteralCommandNode execute = dispatcher.register((LiteralArgumentBuilder)Commands.literal("execute").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)));
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("execute").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("run").redirect((CommandNode)dispatcher.getRoot()))).then(ExecuteCommand.addConditionals((CommandNode<CommandSourceStack>)execute, Commands.literal("if"), true, context))).then(ExecuteCommand.addConditionals((CommandNode<CommandSourceStack>)execute, Commands.literal("unless"), false, context))).then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork((CommandNode)execute, c -> {
            ArrayList result = Lists.newArrayList();
            for (Entity entity : EntityArgument.getOptionalEntities((CommandContext<CommandSourceStack>)c, "targets")) {
                result.add(((CommandSourceStack)c.getSource()).withEntity(entity));
            }
            return result;
        })))).then(Commands.literal("at").then(Commands.argument("targets", EntityArgument.entities()).fork((CommandNode)execute, c -> {
            ArrayList result = Lists.newArrayList();
            for (Entity entity : EntityArgument.getOptionalEntities((CommandContext<CommandSourceStack>)c, "targets")) {
                result.add(((CommandSourceStack)c.getSource()).withLevel((ServerLevel)entity.level()).withPosition(entity.position()).withRotation(entity.getRotationVector()));
            }
            return result;
        })))).then(((LiteralArgumentBuilder)Commands.literal("store").then(ExecuteCommand.wrapStores((LiteralCommandNode<CommandSourceStack>)execute, Commands.literal("result"), true))).then(ExecuteCommand.wrapStores((LiteralCommandNode<CommandSourceStack>)execute, Commands.literal("success"), false)))).then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("positioned").then(Commands.argument("pos", Vec3Argument.vec3()).redirect((CommandNode)execute, c -> ((CommandSourceStack)c.getSource()).withPosition(Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos")).withAnchor(EntityAnchorArgument.Anchor.FEET)))).then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork((CommandNode)execute, c -> {
            ArrayList result = Lists.newArrayList();
            for (Entity entity : EntityArgument.getOptionalEntities((CommandContext<CommandSourceStack>)c, "targets")) {
                result.add(((CommandSourceStack)c.getSource()).withPosition(entity.position()));
            }
            return result;
        })))).then(Commands.literal("over").then(Commands.argument("heightmap", HeightmapTypeArgument.heightmap()).redirect((CommandNode)execute, c -> {
            Vec3 position = ((CommandSourceStack)c.getSource()).getPosition();
            ServerLevel level = ((CommandSourceStack)c.getSource()).getLevel();
            double x = position.x();
            double z = position.z();
            if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                throw BlockPosArgument.ERROR_NOT_LOADED.create();
            }
            int height = level.getHeight(HeightmapTypeArgument.getHeightmap((CommandContext<CommandSourceStack>)c, "heightmap"), Mth.floor(x), Mth.floor(z));
            return ((CommandSourceStack)c.getSource()).withPosition(new Vec3(x, height, z));
        }))))).then(((LiteralArgumentBuilder)Commands.literal("rotated").then(Commands.argument("rot", RotationArgument.rotation()).redirect((CommandNode)execute, c -> ((CommandSourceStack)c.getSource()).withRotation(RotationArgument.getRotation((CommandContext<CommandSourceStack>)c, "rot").getRotation((CommandSourceStack)c.getSource()))))).then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork((CommandNode)execute, c -> {
            ArrayList result = Lists.newArrayList();
            for (Entity entity : EntityArgument.getOptionalEntities((CommandContext<CommandSourceStack>)c, "targets")) {
                result.add(((CommandSourceStack)c.getSource()).withRotation(entity.getRotationVector()));
            }
            return result;
        }))))).then(((LiteralArgumentBuilder)Commands.literal("facing").then(Commands.literal("entity").then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("anchor", EntityAnchorArgument.anchor()).fork((CommandNode)execute, c -> {
            ArrayList result = Lists.newArrayList();
            EntityAnchorArgument.Anchor anchor = EntityAnchorArgument.getAnchor((CommandContext<CommandSourceStack>)c, "anchor");
            for (Entity entity : EntityArgument.getOptionalEntities((CommandContext<CommandSourceStack>)c, "targets")) {
                result.add(((CommandSourceStack)c.getSource()).facing(entity, anchor));
            }
            return result;
        }))))).then(Commands.argument("pos", Vec3Argument.vec3()).redirect((CommandNode)execute, c -> ((CommandSourceStack)c.getSource()).facing(Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "pos")))))).then(Commands.literal("align").then(Commands.argument("axes", SwizzleArgument.swizzle()).redirect((CommandNode)execute, c -> ((CommandSourceStack)c.getSource()).withPosition(((CommandSourceStack)c.getSource()).getPosition().align(SwizzleArgument.getSwizzle((CommandContext<CommandSourceStack>)c, "axes"))))))).then(Commands.literal("anchored").then(Commands.argument("anchor", EntityAnchorArgument.anchor()).redirect((CommandNode)execute, c -> ((CommandSourceStack)c.getSource()).withAnchor(EntityAnchorArgument.getAnchor((CommandContext<CommandSourceStack>)c, "anchor")))))).then(Commands.literal("in").then(Commands.argument("dimension", DimensionArgument.dimension()).redirect((CommandNode)execute, c -> ((CommandSourceStack)c.getSource()).withLevel(DimensionArgument.getDimension((CommandContext<CommandSourceStack>)c, "dimension")))))).then(Commands.literal("summon").then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE)).suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES)).redirect((CommandNode)execute, c -> ExecuteCommand.spawnEntityAndRedirect((CommandSourceStack)c.getSource(), ResourceArgument.getSummonableEntityType((CommandContext<CommandSourceStack>)c, "entity")))))).then(ExecuteCommand.createRelationOperations((CommandNode<CommandSourceStack>)execute, Commands.literal("on"))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> wrapStores(LiteralCommandNode<CommandSourceStack> execute, LiteralArgumentBuilder<CommandSourceStack> literal, boolean storeResult) {
        literal.then(Commands.literal("score").then(Commands.argument("targets", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("objective", ObjectiveArgument.objective()).redirect(execute, c -> ExecuteCommand.storeValue((CommandSourceStack)c.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard((CommandContext<CommandSourceStack>)c, "targets"), ObjectiveArgument.getObjective((CommandContext<CommandSourceStack>)c, "objective"), storeResult)))));
        literal.then(Commands.literal("bossbar").then(((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id()).suggests(BossBarCommands.SUGGEST_BOSS_BAR).then(Commands.literal("value").redirect(execute, c -> ExecuteCommand.storeValue((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), true, storeResult)))).then(Commands.literal("max").redirect(execute, c -> ExecuteCommand.storeValue((CommandSourceStack)c.getSource(), BossBarCommands.getBossBar((CommandContext<CommandSourceStack>)c), false, storeResult)))));
        for (DataCommands.DataProvider provider : DataCommands.TARGET_PROVIDERS) {
            provider.wrap((ArgumentBuilder<CommandSourceStack, ?>)literal, p -> p.then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("path", NbtPathArgument.nbtPath()).then(Commands.literal("int").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect((CommandNode)execute, c -> ExecuteCommand.storeData((CommandSourceStack)c.getSource(), provider.access((CommandContext<CommandSourceStack>)c), NbtPathArgument.getPath((CommandContext<CommandSourceStack>)c, "path"), v -> IntTag.valueOf((int)((double)v * DoubleArgumentType.getDouble((CommandContext)c, (String)"scale"))), storeResult))))).then(Commands.literal("float").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect((CommandNode)execute, c -> ExecuteCommand.storeData((CommandSourceStack)c.getSource(), provider.access((CommandContext<CommandSourceStack>)c), NbtPathArgument.getPath((CommandContext<CommandSourceStack>)c, "path"), v -> FloatTag.valueOf((float)((double)v * DoubleArgumentType.getDouble((CommandContext)c, (String)"scale"))), storeResult))))).then(Commands.literal("short").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect((CommandNode)execute, c -> ExecuteCommand.storeData((CommandSourceStack)c.getSource(), provider.access((CommandContext<CommandSourceStack>)c), NbtPathArgument.getPath((CommandContext<CommandSourceStack>)c, "path"), v -> ShortTag.valueOf((short)((double)v * DoubleArgumentType.getDouble((CommandContext)c, (String)"scale"))), storeResult))))).then(Commands.literal("long").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect((CommandNode)execute, c -> ExecuteCommand.storeData((CommandSourceStack)c.getSource(), provider.access((CommandContext<CommandSourceStack>)c), NbtPathArgument.getPath((CommandContext<CommandSourceStack>)c, "path"), v -> LongTag.valueOf((long)((double)v * DoubleArgumentType.getDouble((CommandContext)c, (String)"scale"))), storeResult))))).then(Commands.literal("double").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect((CommandNode)execute, c -> ExecuteCommand.storeData((CommandSourceStack)c.getSource(), provider.access((CommandContext<CommandSourceStack>)c), NbtPathArgument.getPath((CommandContext<CommandSourceStack>)c, "path"), v -> DoubleTag.valueOf((double)v * DoubleArgumentType.getDouble((CommandContext)c, (String)"scale")), storeResult))))).then(Commands.literal("byte").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect((CommandNode)execute, c -> ExecuteCommand.storeData((CommandSourceStack)c.getSource(), provider.access((CommandContext<CommandSourceStack>)c), NbtPathArgument.getPath((CommandContext<CommandSourceStack>)c, "path"), v -> ByteTag.valueOf((byte)((double)v * DoubleArgumentType.getDouble((CommandContext)c, (String)"scale"))), storeResult))))));
        }
        return literal;
    }

    private static CommandSourceStack storeValue(CommandSourceStack source, Collection<ScoreHolder> names, Objective objective, boolean storeResult) {
        ServerScoreboard scoreboard = source.getServer().getScoreboard();
        return source.withCallback((success, result) -> {
            for (ScoreHolder name : names) {
                ScoreAccess score = scoreboard.getOrCreatePlayerScore(name, objective);
                int value = storeResult ? result : (success ? 1 : 0);
                score.set(value);
            }
        }, CommandResultCallback::chain);
    }

    private static CommandSourceStack storeValue(CommandSourceStack source, CustomBossEvent event, boolean storeIntoValue, boolean storeResult) {
        return source.withCallback((success, result) -> {
            int value;
            int n = storeResult ? result : (value = success ? 1 : 0);
            if (storeIntoValue) {
                event.setValue(value);
            } else {
                event.setMax(value);
            }
        }, CommandResultCallback::chain);
    }

    private static CommandSourceStack storeData(CommandSourceStack source, DataAccessor accessor, NbtPathArgument.NbtPath path, IntFunction<Tag> constructor, boolean storeResult) {
        return source.withCallback((success, result) -> {
            try {
                CompoundTag data = accessor.getData();
                int value = storeResult ? result : (success ? 1 : 0);
                path.set(data, (Tag)constructor.apply(value));
                accessor.setData(data);
            }
            catch (CommandSyntaxException commandSyntaxException) {
                // empty catch block
            }
        }, CommandResultCallback::chain);
    }

    private static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = ChunkPos.containing(pos);
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
        if (chunk != null) {
            return chunk.getFullStatus() == FullChunkStatus.ENTITY_TICKING && level.areEntitiesLoaded(chunkPos.pack());
        }
        return false;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addConditionals(CommandNode<CommandSourceStack> execute, LiteralArgumentBuilder<CommandSourceStack> parent, boolean expected, CommandBuildContext context) {
        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)parent.then(Commands.literal("block").then(Commands.argument("pos", BlockPosArgument.blockPos()).then(ExecuteCommand.addConditional(execute, Commands.argument("block", BlockPredicateArgument.blockPredicate(context)), expected, c -> BlockPredicateArgument.getBlockPredicate((CommandContext<CommandSourceStack>)c, "block").test(new BlockInWorld(((CommandSourceStack)c.getSource()).getLevel(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), true))))))).then(Commands.literal("biome").then(Commands.argument("pos", BlockPosArgument.blockPos()).then(ExecuteCommand.addConditional(execute, Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(context, Registries.BIOME)), expected, c -> ResourceOrTagArgument.getResourceOrTag((CommandContext<CommandSourceStack>)c, "biome", Registries.BIOME).test(((CommandSourceStack)c.getSource()).getLevel().getBiome(BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos")))))))).then(Commands.literal("loaded").then(ExecuteCommand.addConditional(execute, Commands.argument("pos", BlockPosArgument.blockPos()), expected, c -> ExecuteCommand.isChunkLoaded(((CommandSourceStack)c.getSource()).getLevel(), BlockPosArgument.getBlockPos((CommandContext<CommandSourceStack>)c, "pos")))))).then(Commands.literal("dimension").then(ExecuteCommand.addConditional(execute, Commands.argument("dimension", DimensionArgument.dimension()), expected, c -> DimensionArgument.getDimension((CommandContext<CommandSourceStack>)c, "dimension") == ((CommandSourceStack)c.getSource()).getLevel())))).then(Commands.literal("score").then(Commands.argument("target", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("targetObjective", ObjectiveArgument.objective()).then(Commands.literal("=").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(ExecuteCommand.addConditional(execute, Commands.argument("sourceObjective", ObjectiveArgument.objective()), expected, c -> ExecuteCommand.checkScore((CommandContext<CommandSourceStack>)c, (int a, int b) -> a == b)))))).then(Commands.literal("<").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(ExecuteCommand.addConditional(execute, Commands.argument("sourceObjective", ObjectiveArgument.objective()), expected, c -> ExecuteCommand.checkScore((CommandContext<CommandSourceStack>)c, (int a, int b) -> a < b)))))).then(Commands.literal("<=").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(ExecuteCommand.addConditional(execute, Commands.argument("sourceObjective", ObjectiveArgument.objective()), expected, c -> ExecuteCommand.checkScore((CommandContext<CommandSourceStack>)c, (int a, int b) -> a <= b)))))).then(Commands.literal(">").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(ExecuteCommand.addConditional(execute, Commands.argument("sourceObjective", ObjectiveArgument.objective()), expected, c -> ExecuteCommand.checkScore((CommandContext<CommandSourceStack>)c, (int a, int b) -> a > b)))))).then(Commands.literal(">=").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(ExecuteCommand.addConditional(execute, Commands.argument("sourceObjective", ObjectiveArgument.objective()), expected, c -> ExecuteCommand.checkScore((CommandContext<CommandSourceStack>)c, (int a, int b) -> a >= b)))))).then(Commands.literal("matches").then(ExecuteCommand.addConditional(execute, Commands.argument("range", RangeArgument.intRange()), expected, c -> ExecuteCommand.checkScore((CommandContext<CommandSourceStack>)c, RangeArgument.Ints.getRange((CommandContext<CommandSourceStack>)c, "range"))))))))).then(Commands.literal("blocks").then(Commands.argument("start", BlockPosArgument.blockPos()).then(Commands.argument("end", BlockPosArgument.blockPos()).then(((RequiredArgumentBuilder)Commands.argument("destination", BlockPosArgument.blockPos()).then(ExecuteCommand.addIfBlocksConditional(execute, Commands.literal("all"), expected, false))).then(ExecuteCommand.addIfBlocksConditional(execute, Commands.literal("masked"), expected, true))))))).then(Commands.literal("entity").then(((RequiredArgumentBuilder)Commands.argument("entities", EntityArgument.entities()).fork(execute, c -> ExecuteCommand.expect((CommandContext<CommandSourceStack>)c, expected, !EntityArgument.getOptionalEntities((CommandContext<CommandSourceStack>)c, "entities").isEmpty()))).executes(ExecuteCommand.createNumericConditionalHandler(expected, c -> EntityArgument.getOptionalEntities((CommandContext<CommandSourceStack>)c, "entities").size()))))).then(Commands.literal("predicate").then(ExecuteCommand.addConditional(execute, Commands.argument("predicate", ResourceOrIdArgument.lootPredicate(context)), expected, c -> ExecuteCommand.checkCustomPredicate((CommandSourceStack)c.getSource(), ResourceOrIdArgument.getLootPredicate((CommandContext<CommandSourceStack>)c, "predicate")))))).then(Commands.literal("function").then(Commands.argument("name", FunctionArgument.functions()).suggests(FunctionCommand.SUGGEST_FUNCTION).fork(execute, (RedirectModifier)new ExecuteIfFunctionCustomModifier(expected))))).then(((LiteralArgumentBuilder)Commands.literal("items").then(Commands.literal("entity").then(Commands.argument("entities", EntityArgument.entities()).then(Commands.argument("slots", SlotsArgument.slots()).then(((RequiredArgumentBuilder)Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(context)).fork(execute, c -> ExecuteCommand.expect((CommandContext<CommandSourceStack>)c, expected, ExecuteCommand.countItems(EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "entities"), SlotsArgument.getSlots((CommandContext<CommandSourceStack>)c, "slots"), ItemPredicateArgument.getItemPredicate((CommandContext<CommandSourceStack>)c, "item_predicate")) > 0))).executes(ExecuteCommand.createNumericConditionalHandler(expected, c -> ExecuteCommand.countItems(EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "entities"), SlotsArgument.getSlots((CommandContext<CommandSourceStack>)c, "slots"), ItemPredicateArgument.getItemPredicate((CommandContext<CommandSourceStack>)c, "item_predicate"))))))))).then(Commands.literal("block").then(Commands.argument("pos", BlockPosArgument.blockPos()).then(Commands.argument("slots", SlotsArgument.slots()).then(((RequiredArgumentBuilder)Commands.argument("item_predicate", ItemPredicateArgument.itemPredicate(context)).fork(execute, c -> ExecuteCommand.expect((CommandContext<CommandSourceStack>)c, expected, ExecuteCommand.countItems((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), SlotsArgument.getSlots((CommandContext<CommandSourceStack>)c, "slots"), ItemPredicateArgument.getItemPredicate((CommandContext<CommandSourceStack>)c, "item_predicate")) > 0))).executes(ExecuteCommand.createNumericConditionalHandler(expected, c -> ExecuteCommand.countItems((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), SlotsArgument.getSlots((CommandContext<CommandSourceStack>)c, "slots"), ItemPredicateArgument.getItemPredicate((CommandContext<CommandSourceStack>)c, "item_predicate")))))))))).then(Commands.literal("stopwatch").then(Commands.argument("id", IdentifierArgument.id()).suggests(StopwatchCommand.SUGGEST_STOPWATCHES).then(ExecuteCommand.addConditional(execute, Commands.argument("range", RangeArgument.floatRange()), expected, c -> ExecuteCommand.checkStopwatch((CommandContext<CommandSourceStack>)c, RangeArgument.Floats.getRange((CommandContext<CommandSourceStack>)c, "range"))))));
        for (DataCommands.DataProvider provider : DataCommands.SOURCE_PROVIDERS) {
            parent.then(provider.wrap((ArgumentBuilder<CommandSourceStack, ?>)Commands.literal("data"), p -> p.then(((RequiredArgumentBuilder)Commands.argument("path", NbtPathArgument.nbtPath()).fork(execute, c -> ExecuteCommand.expect((CommandContext<CommandSourceStack>)c, expected, ExecuteCommand.checkMatchingData(provider.access((CommandContext<CommandSourceStack>)c), NbtPathArgument.getPath((CommandContext<CommandSourceStack>)c, "path")) > 0))).executes(ExecuteCommand.createNumericConditionalHandler(expected, c -> ExecuteCommand.checkMatchingData(provider.access((CommandContext<CommandSourceStack>)c), NbtPathArgument.getPath((CommandContext<CommandSourceStack>)c, "path")))))));
        }
        return parent;
    }

    private static int countItems(Iterable<? extends SlotProvider> sources, SlotRange slotRange, Predicate<ItemStack> predicate) {
        int count = 0;
        for (SlotProvider slotProvider : sources) {
            IntList slots = slotRange.slots();
            for (int i = 0; i < slots.size(); ++i) {
                ItemStack contents;
                int slotId = slots.getInt(i);
                SlotAccess slot = slotProvider.getSlot(slotId);
                if (slot == null || !predicate.test(contents = slot.get())) continue;
                count += contents.getCount();
            }
        }
        return count;
    }

    private static int countItems(CommandSourceStack source, BlockPos pos, SlotRange slotRange, Predicate<ItemStack> predicate) throws CommandSyntaxException {
        int count = 0;
        Container container = ItemCommands.getContainer(source, pos, ItemCommands.ERROR_SOURCE_NOT_A_CONTAINER);
        int containerSize = container.getContainerSize();
        IntList slots = slotRange.slots();
        for (int i = 0; i < slots.size(); ++i) {
            ItemStack contents;
            int slotId = slots.getInt(i);
            if (slotId < 0 || slotId >= containerSize || !predicate.test(contents = container.getItem(slotId))) continue;
            count += contents.getCount();
        }
        return count;
    }

    private static Command<CommandSourceStack> createNumericConditionalHandler(boolean expected, CommandNumericPredicate condition) {
        if (expected) {
            return c -> {
                int count = condition.test((CommandContext<CommandSourceStack>)c);
                if (count > 0) {
                    ((CommandSourceStack)c.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", count), false);
                    return count;
                }
                throw ERROR_CONDITIONAL_FAILED.create();
            };
        }
        return c -> {
            int count = condition.test((CommandContext<CommandSourceStack>)c);
            if (count == 0) {
                ((CommandSourceStack)c.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
                return 1;
            }
            throw ERROR_CONDITIONAL_FAILED_COUNT.create((Object)count);
        };
    }

    private static int checkMatchingData(DataAccessor accessor, NbtPathArgument.NbtPath path) throws CommandSyntaxException {
        return path.countMatching(accessor.getData());
    }

    private static boolean checkScore(CommandContext<CommandSourceStack> context, IntBiPredicate operation) throws CommandSyntaxException {
        ScoreHolder target = ScoreHolderArgument.getName(context, "target");
        Objective targetObjective = ObjectiveArgument.getObjective(context, "targetObjective");
        ScoreHolder source = ScoreHolderArgument.getName(context, "source");
        Objective sourceObjective = ObjectiveArgument.getObjective(context, "sourceObjective");
        ServerScoreboard scoreboard = ((CommandSourceStack)context.getSource()).getServer().getScoreboard();
        ReadOnlyScoreInfo a = scoreboard.getPlayerScoreInfo(target, targetObjective);
        ReadOnlyScoreInfo b = scoreboard.getPlayerScoreInfo(source, sourceObjective);
        if (a == null || b == null) {
            return false;
        }
        return operation.test(a.value(), b.value());
    }

    private static boolean checkScore(CommandContext<CommandSourceStack> context, MinMaxBounds.Ints range) throws CommandSyntaxException {
        ScoreHolder target = ScoreHolderArgument.getName(context, "target");
        Objective targetObjective = ObjectiveArgument.getObjective(context, "targetObjective");
        ServerScoreboard scoreboard = ((CommandSourceStack)context.getSource()).getServer().getScoreboard();
        ReadOnlyScoreInfo scoreInfo = scoreboard.getPlayerScoreInfo(target, targetObjective);
        if (scoreInfo == null) {
            return false;
        }
        return range.matches(scoreInfo.value());
    }

    private static boolean checkStopwatch(CommandContext<CommandSourceStack> context, MinMaxBounds.Doubles range) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(context, "id");
        Stopwatches stopwatches = ((CommandSourceStack)context.getSource()).getServer().getStopwatches();
        Stopwatch stopwatch = stopwatches.get(id);
        if (stopwatch == null) {
            throw StopwatchCommand.ERROR_DOES_NOT_EXIST.create((Object)id);
        }
        long currentTime = Stopwatches.currentTime();
        double elapsedSeconds = stopwatch.elapsedSeconds(currentTime);
        return range.matches(elapsedSeconds);
    }

    private static boolean checkCustomPredicate(CommandSourceStack source, Holder<LootItemCondition> predicate) {
        ServerLevel level = source.getLevel();
        LootParams lootParams = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, source.getPosition()).withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity()).create(LootContextParamSets.COMMAND);
        LootContext context = new LootContext.Builder(lootParams).create(Optional.empty());
        context.pushVisitedElement(LootContext.createVisitedEntry(predicate.value()));
        return predicate.value().test(context);
    }

    private static Collection<CommandSourceStack> expect(CommandContext<CommandSourceStack> context, boolean expected, boolean result) {
        if (result == expected) {
            return Collections.singleton((CommandSourceStack)context.getSource());
        }
        return Collections.emptyList();
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addConditional(CommandNode<CommandSourceStack> root, ArgumentBuilder<CommandSourceStack, ?> argument, boolean expected, CommandPredicate predicate) {
        return argument.fork(root, c -> ExecuteCommand.expect((CommandContext<CommandSourceStack>)c, expected, predicate.test((CommandContext<CommandSourceStack>)c))).executes(c -> {
            if (expected == predicate.test((CommandContext<CommandSourceStack>)c)) {
                ((CommandSourceStack)c.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
                return 1;
            }
            throw ERROR_CONDITIONAL_FAILED.create();
        });
    }

    private static ArgumentBuilder<CommandSourceStack, ?> addIfBlocksConditional(CommandNode<CommandSourceStack> root, ArgumentBuilder<CommandSourceStack, ?> argument, boolean expected, boolean skipAir) {
        return argument.fork(root, c -> ExecuteCommand.expect((CommandContext<CommandSourceStack>)c, expected, ExecuteCommand.checkRegions((CommandContext<CommandSourceStack>)c, skipAir).isPresent())).executes(expected ? c -> ExecuteCommand.checkIfRegions((CommandContext<CommandSourceStack>)c, skipAir) : c -> ExecuteCommand.checkUnlessRegions((CommandContext<CommandSourceStack>)c, skipAir));
    }

    private static int checkIfRegions(CommandContext<CommandSourceStack> context, boolean skipAir) throws CommandSyntaxException {
        OptionalInt count = ExecuteCommand.checkRegions(context, skipAir);
        if (count.isPresent()) {
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass_count", count.getAsInt()), false);
            return count.getAsInt();
        }
        throw ERROR_CONDITIONAL_FAILED.create();
    }

    private static int checkUnlessRegions(CommandContext<CommandSourceStack> context, boolean skipAir) throws CommandSyntaxException {
        OptionalInt count = ExecuteCommand.checkRegions(context, skipAir);
        if (count.isPresent()) {
            throw ERROR_CONDITIONAL_FAILED_COUNT.create((Object)count.getAsInt());
        }
        ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
        return 1;
    }

    private static OptionalInt checkRegions(CommandContext<CommandSourceStack> context, boolean skipAir) throws CommandSyntaxException {
        return ExecuteCommand.checkRegions(((CommandSourceStack)context.getSource()).getLevel(), BlockPosArgument.getLoadedBlockPos(context, "start"), BlockPosArgument.getLoadedBlockPos(context, "end"), BlockPosArgument.getLoadedBlockPos(context, "destination"), skipAir);
    }

    private static OptionalInt checkRegions(ServerLevel level, BlockPos startPos, BlockPos endPos, BlockPos destPos, boolean skipAir) throws CommandSyntaxException {
        BoundingBox from = BoundingBox.fromCorners(startPos, endPos);
        BoundingBox destination = BoundingBox.fromCorners(destPos, destPos.offset(from.getLength()));
        BlockPos offset = new BlockPos(destination.minX() - from.minX(), destination.minY() - from.minY(), destination.minZ() - from.minZ());
        long area = (long)from.getXSpan() * (long)from.getYSpan() * (long)from.getZSpan();
        if (area > 32768L) {
            throw ERROR_AREA_TOO_LARGE.create((Object)32768, (Object)area);
        }
        int count = 0;
        RegistryAccess registryAccess = level.registryAccess();
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER);){
            for (int z = from.minZ(); z <= from.maxZ(); ++z) {
                for (int y = from.minY(); y <= from.maxY(); ++y) {
                    for (int x = from.minX(); x <= from.maxX(); ++x) {
                        BlockPos sourcePos = new BlockPos(x, y, z);
                        BlockPos destinationPos = sourcePos.offset(offset);
                        BlockState sourceBlock = level.getBlockState(sourcePos);
                        if (skipAir && sourceBlock.is(Blocks.AIR)) continue;
                        if (sourceBlock != level.getBlockState(destinationPos)) {
                            OptionalInt optionalInt = OptionalInt.empty();
                            return optionalInt;
                        }
                        BlockEntity sourceBlockEntity = level.getBlockEntity(sourcePos);
                        BlockEntity destinationBlockEntity = level.getBlockEntity(destinationPos);
                        if (sourceBlockEntity != null) {
                            OptionalInt optionalInt;
                            if (destinationBlockEntity == null) {
                                optionalInt = OptionalInt.empty();
                                return optionalInt;
                            }
                            if (destinationBlockEntity.getType() != sourceBlockEntity.getType()) {
                                optionalInt = OptionalInt.empty();
                                return optionalInt;
                            }
                            if (!sourceBlockEntity.components().equals(destinationBlockEntity.components())) {
                                optionalInt = OptionalInt.empty();
                                return optionalInt;
                            }
                            TagValueOutput sourceOutput = TagValueOutput.createWithContext(reporter.forChild(sourceBlockEntity.problemPath()), registryAccess);
                            sourceBlockEntity.saveCustomOnly(sourceOutput);
                            CompoundTag sourceTag = sourceOutput.buildResult();
                            TagValueOutput destinationOutput = TagValueOutput.createWithContext(reporter.forChild(destinationBlockEntity.problemPath()), registryAccess);
                            destinationBlockEntity.saveCustomOnly(destinationOutput);
                            CompoundTag destinationTag = destinationOutput.buildResult();
                            if (!sourceTag.equals(destinationTag)) {
                                OptionalInt optionalInt2 = OptionalInt.empty();
                                return optionalInt2;
                            }
                        }
                        ++count;
                    }
                }
            }
        }
        return OptionalInt.of(count);
    }

    private static RedirectModifier<CommandSourceStack> expandOneToOneEntityRelation(Function<Entity, Optional<Entity>> unpacker) {
        return context -> {
            CommandSourceStack source = (CommandSourceStack)context.getSource();
            Entity entity = source.getEntity();
            if (entity == null) {
                return List.of();
            }
            return ((Optional)unpacker.apply(entity)).filter(e -> !e.isRemoved()).map(e -> List.of(source.withEntity((Entity)e))).orElse(List.of());
        };
    }

    private static RedirectModifier<CommandSourceStack> expandOneToManyEntityRelation(Function<Entity, Stream<Entity>> unpacker) {
        return context -> {
            CommandSourceStack source = (CommandSourceStack)context.getSource();
            Entity entity = source.getEntity();
            if (entity == null) {
                return List.of();
            }
            return ((Stream)unpacker.apply(entity)).filter(e -> !e.isRemoved()).map(source::withEntity).toList();
        };
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRelationOperations(CommandNode<CommandSourceStack> execute, LiteralArgumentBuilder<CommandSourceStack> on) {
        return (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)on.then(Commands.literal("owner").fork(execute, ExecuteCommand.expandOneToOneEntityRelation(e -> {
            Optional<Object> optional;
            if (e instanceof OwnableEntity) {
                OwnableEntity ownableEntity = (OwnableEntity)((Object)e);
                optional = Optional.ofNullable(ownableEntity.getOwner());
            } else {
                optional = Optional.empty();
            }
            return optional;
        })))).then(Commands.literal("leasher").fork(execute, ExecuteCommand.expandOneToOneEntityRelation(e -> {
            Optional<Object> optional;
            if (e instanceof Leashable) {
                Leashable leashable = (Leashable)((Object)e);
                optional = Optional.ofNullable(leashable.getLeashHolder());
            } else {
                optional = Optional.empty();
            }
            return optional;
        })))).then(Commands.literal("target").fork(execute, ExecuteCommand.expandOneToOneEntityRelation(e -> {
            Optional<Object> optional;
            if (e instanceof Targeting) {
                Targeting targeting = (Targeting)((Object)e);
                optional = Optional.ofNullable(targeting.getTarget());
            } else {
                optional = Optional.empty();
            }
            return optional;
        })))).then(Commands.literal("attacker").fork(execute, ExecuteCommand.expandOneToOneEntityRelation(e -> {
            Optional<Object> optional;
            if (e instanceof Attackable) {
                Attackable attackable = (Attackable)((Object)e);
                optional = Optional.ofNullable(attackable.getLastAttacker());
            } else {
                optional = Optional.empty();
            }
            return optional;
        })))).then(Commands.literal("vehicle").fork(execute, ExecuteCommand.expandOneToOneEntityRelation(e -> Optional.ofNullable(e.getVehicle()))))).then(Commands.literal("controller").fork(execute, ExecuteCommand.expandOneToOneEntityRelation(e -> Optional.ofNullable(e.getControllingPassenger()))))).then(Commands.literal("origin").fork(execute, ExecuteCommand.expandOneToOneEntityRelation(e -> {
            Optional<Object> optional;
            if (e instanceof TraceableEntity) {
                TraceableEntity traceable = (TraceableEntity)((Object)e);
                optional = Optional.ofNullable(traceable.getOwner());
            } else {
                optional = Optional.empty();
            }
            return optional;
        })))).then(Commands.literal("passengers").fork(execute, ExecuteCommand.expandOneToManyEntityRelation(e -> e.getPassengers().stream())));
    }

    private static CommandSourceStack spawnEntityAndRedirect(CommandSourceStack source, Holder.Reference<EntityType<?>> type) throws CommandSyntaxException {
        Entity entity = SummonCommand.createEntity(source, type, source.getPosition(), new CompoundTag(), true);
        return source.withEntity(entity);
    }

    /*
     * Exception decompiling
     */
    public static <T extends ExecutionCommandSource<T>> void scheduleFunctionConditionsAndTest(T originalSource, List<T> currentSources, Function<T, T> functionContextModifier, IntPredicate check, ContextChain<T> currentStep, @Nullable CompoundTag parameters, ExecutionControl<T> output, InCommandFunction<CommandContext<T>, Collection<CommandFunction<T>>> functionGetter, ChainModifiers modifiers) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static /* synthetic */ void lambda$scheduleFunctionConditionsAndTest$1(List instantiatedFunctions, ExecutionCommandSource newFunctionContext, ExecutionControl o) {
        for (InstantiatedFunction function : instantiatedFunctions) {
            o.queueNext(new CallFunction<ExecutionCommandSource>(function, o.currentFrame().returnValueConsumer(), true).bind(newFunctionContext));
        }
        o.queueNext(FallthroughTask.instance());
    }

    private static /* synthetic */ void lambda$scheduleFunctionConditionsAndTest$0(IntPredicate check, List filteredSources, ExecutionCommandSource source, boolean success, int result) {
        if (check.test(result)) {
            filteredSources.add(source);
        }
    }

    @FunctionalInterface
    private static interface CommandPredicate {
        public boolean test(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
    }

    @FunctionalInterface
    private static interface CommandNumericPredicate {
        public int test(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
    }

    private static class ExecuteIfFunctionCustomModifier
    implements CustomModifierExecutor.ModifierAdapter<CommandSourceStack> {
        private final IntPredicate check;

        private ExecuteIfFunctionCustomModifier(boolean check) {
            this.check = check ? value -> value != 0 : value -> value == 0;
        }

        @Override
        public void apply(CommandSourceStack originalSource, List<CommandSourceStack> currentSources, ContextChain<CommandSourceStack> currentStep, ChainModifiers modifiers, ExecutionControl<CommandSourceStack> output) {
            ExecuteCommand.scheduleFunctionConditionsAndTest(originalSource, currentSources, FunctionCommand::modifySenderForExecution, this.check, currentStep, null, output, c -> FunctionArgument.getFunctions((CommandContext<CommandSourceStack>)c, "name"), modifiers);
        }
    }

    @FunctionalInterface
    private static interface IntBiPredicate {
        public boolean test(int var1, int var2);
    }
}

