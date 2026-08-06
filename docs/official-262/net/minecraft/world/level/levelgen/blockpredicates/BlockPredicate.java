/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.AllOfPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.AnyOfPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.blockpredicates.HasSturdyFacePredicate;
import net.minecraft.world.level.levelgen.blockpredicates.InsideWorldBoundsPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.MatchingBiomesPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.MatchingBlockTagPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.MatchingBlocksPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.MatchingFluidsPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.NotPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.ReplaceablePredicate;
import net.minecraft.world.level.levelgen.blockpredicates.SolidPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.TrueBlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.UnobstructedPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.WouldSurvivePredicate;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public interface BlockPredicate
extends BiPredicate<WorldGenLevel, BlockPos> {
    public static final Codec<BlockPredicate> CODEC = BuiltInRegistries.BLOCK_PREDICATE_TYPE.byNameCodec().dispatch(BlockPredicate::type, BlockPredicateType::codec);
    public static final BlockPredicate ONLY_IN_AIR_PREDICATE = BlockPredicate.matchesTag(BlockTags.AIR);
    public static final BlockPredicate ONLY_IN_AIR_OR_WATER_PREDICATE = BlockPredicate.anyOf(ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(Blocks.WATER));

    public BlockPredicateType<?> type();

    public static BlockPredicate allOf(List<BlockPredicate> predicates) {
        return new AllOfPredicate(predicates);
    }

    public static BlockPredicate allOf(BlockPredicate ... predicates) {
        return BlockPredicate.allOf(List.of(predicates));
    }

    public static BlockPredicate allOf(BlockPredicate a, BlockPredicate b) {
        return BlockPredicate.allOf(List.of(a, b));
    }

    public static BlockPredicate anyOf(List<BlockPredicate> predicates) {
        return new AnyOfPredicate(predicates);
    }

    public static BlockPredicate anyOf(BlockPredicate ... predicates) {
        return BlockPredicate.anyOf(List.of(predicates));
    }

    public static BlockPredicate anyOf(BlockPredicate a, BlockPredicate b) {
        return BlockPredicate.anyOf(List.of(a, b));
    }

    public static BlockPredicate matchesBlocks(Vec3i offset, List<Block> blocks) {
        return new MatchingBlocksPredicate(offset, HolderSet.direct(Block::builtInRegistryHolder, blocks));
    }

    public static BlockPredicate matchesBlocks(List<Block> blocks) {
        return BlockPredicate.matchesBlocks(Vec3i.ZERO, blocks);
    }

    public static BlockPredicate matchesBlocks(Vec3i offset, Block ... blocks) {
        return BlockPredicate.matchesBlocks(offset, List.of(blocks));
    }

    public static BlockPredicate matchesBlocks(Block ... blocks) {
        return BlockPredicate.matchesBlocks(Vec3i.ZERO, blocks);
    }

    public static BlockPredicate matchesTag(Vec3i offset, TagKey<Block> tag) {
        return new MatchingBlockTagPredicate(offset, tag);
    }

    public static BlockPredicate matchesTag(TagKey<Block> tag) {
        return BlockPredicate.matchesTag(Vec3i.ZERO, tag);
    }

    public static BlockPredicate matchesFluids(Vec3i offset, List<Fluid> fluids) {
        return new MatchingFluidsPredicate(offset, HolderSet.direct(Fluid::builtInRegistryHolder, fluids));
    }

    public static BlockPredicate matchesFluids(Vec3i offset, Fluid ... fluids) {
        return BlockPredicate.matchesFluids(offset, List.of(fluids));
    }

    public static BlockPredicate matchesFluids(Fluid ... fluids) {
        return BlockPredicate.matchesFluids(Vec3i.ZERO, fluids);
    }

    public static BlockPredicate matchesBiomes(HolderSet<Biome> biomes) {
        return new MatchingBiomesPredicate(biomes);
    }

    public static BlockPredicate not(BlockPredicate predicate) {
        return new NotPredicate(predicate);
    }

    public static BlockPredicate replaceable(Vec3i offset) {
        return new ReplaceablePredicate(offset);
    }

    public static BlockPredicate replaceable() {
        return BlockPredicate.replaceable(Vec3i.ZERO);
    }

    public static BlockPredicate wouldSurvive(BlockState state, Vec3i offset) {
        return new WouldSurvivePredicate(offset, state);
    }

    public static BlockPredicate hasSturdyFace(Vec3i offset, Direction direction) {
        return new HasSturdyFacePredicate(offset, direction);
    }

    public static BlockPredicate hasSturdyFace(Direction direction) {
        return BlockPredicate.hasSturdyFace(Vec3i.ZERO, direction);
    }

    public static BlockPredicate solid(Vec3i offset) {
        return new SolidPredicate(offset);
    }

    public static BlockPredicate solid() {
        return BlockPredicate.solid(Vec3i.ZERO);
    }

    public static BlockPredicate noFluid() {
        return BlockPredicate.noFluid(Vec3i.ZERO);
    }

    public static BlockPredicate noFluid(Vec3i offset) {
        return BlockPredicate.matchesFluids(offset, Fluids.EMPTY);
    }

    public static BlockPredicate insideWorld(Vec3i offset) {
        return new InsideWorldBoundsPredicate(offset);
    }

    public static BlockPredicate alwaysTrue() {
        return TrueBlockPredicate.INSTANCE;
    }

    public static BlockPredicate unobstructed(Vec3i offset) {
        return new UnobstructedPredicate(offset);
    }

    public static BlockPredicate unobstructed() {
        return BlockPredicate.unobstructed(Vec3i.ZERO);
    }
}

