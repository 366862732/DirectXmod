/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Stopwatch
 *  com.google.common.base.Ticker
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.server.commands;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;

public class LocateCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DynamicCommandExceptionType ERROR_STRUCTURE_NOT_FOUND = new DynamicCommandExceptionType(value -> Component.translatableEscape("commands.locate.structure.not_found", value));
    private static final DynamicCommandExceptionType ERROR_STRUCTURE_INVALID = new DynamicCommandExceptionType(value -> Component.translatableEscape("commands.locate.structure.invalid", value));
    private static final DynamicCommandExceptionType ERROR_BIOME_NOT_FOUND = new DynamicCommandExceptionType(value -> Component.translatableEscape("commands.locate.biome.not_found", value));
    private static final DynamicCommandExceptionType ERROR_POI_NOT_FOUND = new DynamicCommandExceptionType(value -> Component.translatableEscape("commands.locate.poi.not_found", value));
    private static final int MAX_STRUCTURE_SEARCH_RADIUS = 100;
    private static final int MAX_BIOME_SEARCH_RADIUS = 6400;
    private static final int BIOME_SAMPLE_RESOLUTION_HORIZONTAL = 32;
    private static final int BIOME_SAMPLE_RESOLUTION_VERTICAL = 64;
    private static final int POI_SEARCH_RADIUS = 256;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("locate").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("structure").then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE)).executes(c -> LocateCommand.locateStructure((CommandSourceStack)c.getSource(), ResourceOrTagKeyArgument.getResourceOrTagKey((CommandContext<CommandSourceStack>)c, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID)))))).then(Commands.literal("biome").then(Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(context, Registries.BIOME)).executes(c -> LocateCommand.locateBiome((CommandSourceStack)c.getSource(), ResourceOrTagArgument.getResourceOrTag((CommandContext<CommandSourceStack>)c, "biome", Registries.BIOME)))))).then(Commands.literal("poi").then(Commands.argument("poi", ResourceOrTagArgument.resourceOrTag(context, Registries.POINT_OF_INTEREST_TYPE)).executes(c -> LocateCommand.locatePoi((CommandSourceStack)c.getSource(), ResourceOrTagArgument.getResourceOrTag((CommandContext<CommandSourceStack>)c, "poi", Registries.POINT_OF_INTEREST_TYPE))))));
    }

    private static Optional<? extends HolderSet.ListBacked<Structure>> getHolders(ResourceOrTagKeyArgument.Result<Structure> resourceOrTag, Registry<Structure> registry) {
        return (Optional)resourceOrTag.unwrap().map(id -> registry.get((ResourceKey)id).map(xva$0 -> HolderSet.direct(xva$0)), registry::get);
    }

    private static int locateStructure(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> resourceOrTag) throws CommandSyntaxException {
        HolderLookup.RegistryLookup registry = source.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        HolderSet target = LocateCommand.getHolders(resourceOrTag, (Registry<Structure>)registry).orElseThrow(() -> ERROR_STRUCTURE_INVALID.create((Object)resourceOrTag.asPrintable()));
        BlockPos sourcePos = BlockPos.containing(source.getPosition());
        ServerLevel serverLevel = source.getLevel();
        Stopwatch stopwatch = Stopwatch.createStarted((Ticker)Util.TICKER);
        Pair<BlockPos, Holder<Structure>> nearest = serverLevel.getChunkSource().getGenerator().findNearestMapStructure(serverLevel, target, sourcePos, 100, false);
        stopwatch.stop();
        if (nearest == null) {
            throw ERROR_STRUCTURE_NOT_FOUND.create((Object)resourceOrTag.asPrintable());
        }
        return LocateCommand.showLocateResult(source, resourceOrTag, sourcePos, nearest, "commands.locate.structure.success", false, stopwatch.elapsed());
    }

    private static int locateBiome(CommandSourceStack source, ResourceOrTagArgument.Result<Biome> elementOrTag) throws CommandSyntaxException {
        BlockPos sourcePos = BlockPos.containing(source.getPosition());
        Stopwatch stopwatch = Stopwatch.createStarted((Ticker)Util.TICKER);
        Pair<BlockPos, Holder<Biome>> nearest = source.getLevel().findClosestBiome3d(elementOrTag, sourcePos, 6400, 32, 64);
        stopwatch.stop();
        if (nearest == null) {
            throw ERROR_BIOME_NOT_FOUND.create((Object)elementOrTag.asPrintable());
        }
        return LocateCommand.showLocateResult(source, elementOrTag, sourcePos, nearest, "commands.locate.biome.success", true, stopwatch.elapsed());
    }

    private static int locatePoi(CommandSourceStack source, ResourceOrTagArgument.Result<PoiType> resourceOrTag) throws CommandSyntaxException {
        BlockPos sourcePos = BlockPos.containing(source.getPosition());
        ServerLevel serverLevel = source.getLevel();
        Stopwatch stopwatch = Stopwatch.createStarted((Ticker)Util.TICKER);
        Optional<Pair<Holder<PoiType>, BlockPos>> closestWithType = serverLevel.getPoiManager().findClosestWithType(resourceOrTag, sourcePos, 256, PoiManager.Occupancy.ANY);
        stopwatch.stop();
        if (closestWithType.isEmpty()) {
            throw ERROR_POI_NOT_FOUND.create((Object)resourceOrTag.asPrintable());
        }
        return LocateCommand.showLocateResult(source, resourceOrTag, sourcePos, closestWithType.get().swap(), "commands.locate.poi.success", false, stopwatch.elapsed());
    }

    public static int showLocateResult(CommandSourceStack source, ResourceOrTagArgument.Result<?> name, BlockPos sourcePos, Pair<BlockPos, ? extends Holder<?>> found, String successMessageKey, boolean includeY, Duration taskDuration) {
        String foundName = (String)name.unwrap().map(element -> name.asPrintable(), tag -> name.asPrintable() + " (" + ((Holder)found.getSecond()).getRegisteredName() + ")");
        return LocateCommand.showLocateResult(source, sourcePos, found, successMessageKey, includeY, foundName, taskDuration);
    }

    public static int showLocateResult(CommandSourceStack source, ResourceOrTagKeyArgument.Result<?> name, BlockPos sourcePos, Pair<BlockPos, ? extends Holder<?>> found, String successMessageKey, boolean includeY, Duration taskDuration) {
        String foundName = (String)name.unwrap().map(element -> element.identifier().toString(), tag -> "#" + String.valueOf(tag.location()) + " (" + ((Holder)found.getSecond()).getRegisteredName() + ")");
        return LocateCommand.showLocateResult(source, sourcePos, found, successMessageKey, includeY, foundName, taskDuration);
    }

    private static int showLocateResult(CommandSourceStack source, BlockPos sourcePos, Pair<BlockPos, ? extends Holder<?>> found, String successMessageKey, boolean includeY, String foundName, Duration taskDuration) {
        BlockPos foundPos = (BlockPos)found.getFirst();
        int distance = includeY ? Mth.floor(Mth.sqrt((float)sourcePos.distSqr(foundPos))) : Mth.floor(LocateCommand.dist(sourcePos.getX(), sourcePos.getZ(), foundPos.getX(), foundPos.getZ()));
        String displayedY = includeY ? String.valueOf(foundPos.getY()) : "~";
        MutableComponent coordinates = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", foundPos.getX(), displayedY, foundPos.getZ())).withStyle(s -> s.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent.SuggestCommand("/tp @s " + foundPos.getX() + " " + displayedY + " " + foundPos.getZ())).withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip"))));
        source.sendSuccess(() -> Component.translatable(successMessageKey, foundName, coordinates, distance), false);
        LOGGER.info("Locating element {} took {} ms", (Object)foundName, (Object)taskDuration.toMillis());
        return distance;
    }

    private static float dist(int x1, int z1, int x2, int z2) {
        int dx = x2 - x1;
        int dz = z2 - z1;
        return Mth.sqrt(dx * dx + dz * dz);
    }
}

