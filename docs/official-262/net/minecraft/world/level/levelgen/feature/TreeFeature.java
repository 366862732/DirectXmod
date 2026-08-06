/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Sets
 *  com.mojang.serialization.Codec
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 */
package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class TreeFeature
extends Feature<TreeConfiguration> {
    private static final @Block.UpdateFlags int BLOCK_UPDATE_FLAGS = 19;

    public TreeFeature(Codec<TreeConfiguration> codec) {
        super(codec);
    }

    public static boolean isVine(LevelSimulatedReader level, BlockPos pos) {
        return level.isStateAtPosition(pos, state -> state.is(Blocks.VINE));
    }

    public static boolean isAirOrLeaves(LevelSimulatedReader level, BlockPos pos) {
        return level.isStateAtPosition(pos, state -> state.isAir() || state.is(BlockTags.LEAVES));
    }

    private static void setBlockKnownShape(LevelWriter level, BlockPos pos, BlockState blockState) {
        level.setBlock(pos, blockState, 19);
    }

    public static boolean validTreePos(LevelSimulatedReader level, BlockPos pos) {
        return level.isStateAtPosition(pos, state -> state.isAir() || state.is(BlockTags.REPLACEABLE_BY_TREES));
    }

    private boolean doPlace(WorldGenLevel level, RandomSource random, BlockPos origin, BiConsumer<BlockPos, BlockState> rootSetter, BiConsumer<BlockPos, BlockState> trunkSetter, FoliagePlacer.FoliageSetter foliageSetter, TreeConfiguration config) {
        int treeHeight = config.trunkPlacer.getTreeHeight(random);
        int foliageHeight = config.foliagePlacer.foliageHeight(random, treeHeight, config);
        int trunkHeight = treeHeight - foliageHeight;
        int leafRadius = config.foliagePlacer.foliageRadius(random, trunkHeight);
        BlockPos trunkOrigin = config.rootPlacer.map(rootPlacer -> rootPlacer.getTrunkOrigin(origin, random)).orElse(origin);
        int minY = Math.min(origin.getY(), trunkOrigin.getY());
        int maxY = Math.max(origin.getY(), trunkOrigin.getY()) + treeHeight + 1;
        if (minY < level.getMinY() + 1 || maxY > level.getMaxY() + 1) {
            return false;
        }
        OptionalInt minClippedHeight = config.minimumSize.minClippedHeight();
        int clippedTreeHeight = this.getMaxFreeTreeHeight(level, treeHeight, trunkOrigin, config);
        if (clippedTreeHeight < treeHeight && (minClippedHeight.isEmpty() || clippedTreeHeight < minClippedHeight.getAsInt())) {
            return false;
        }
        if (config.rootPlacer.isPresent() && !config.rootPlacer.get().placeRoots(level, rootSetter, random, origin, trunkOrigin, config)) {
            return false;
        }
        List<FoliagePlacer.FoliageAttachment> foliageAttachments = config.trunkPlacer.placeTrunk(level, trunkSetter, random, clippedTreeHeight, trunkOrigin, config);
        foliageAttachments.forEach(foliageAttachment -> config.foliagePlacer.createFoliage(level, foliageSetter, random, config, clippedTreeHeight, (FoliagePlacer.FoliageAttachment)foliageAttachment, foliageHeight, leafRadius));
        return true;
    }

    private int getMaxFreeTreeHeight(WorldGenLevel level, int maxTreeHeight, BlockPos treePos, TreeConfiguration config) {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= maxTreeHeight + 1; ++y) {
            int r = config.minimumSize.getSizeAtHeight(maxTreeHeight, y);
            for (int x = -r; x <= r; ++x) {
                for (int z = -r; z <= r; ++z) {
                    blockPos.setWithOffset(treePos, x, y, z);
                    if (config.trunkPlacer.isFree(level, blockPos) && (config.ignoreVines || !TreeFeature.isVine(level, blockPos))) continue;
                    return y - 2;
                }
            }
        }
        return maxTreeHeight;
    }

    @Override
    protected void setBlock(LevelWriter level, BlockPos pos, BlockState blockState) {
        TreeFeature.setBlockKnownShape(level, pos, blockState);
    }

    @Override
    public final boolean place(FeaturePlaceContext<TreeConfiguration> context) {
        final WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        TreeConfiguration config = context.config();
        HashSet rootPositions = Sets.newHashSet();
        HashSet trunks = Sets.newHashSet();
        final HashSet foliage = Sets.newHashSet();
        HashSet decorations = Sets.newHashSet();
        BiConsumer<BlockPos, BlockState> rootSetter = (pos, state) -> {
            rootPositions.add(pos.immutable());
            level.setBlock((BlockPos)pos, (BlockState)state, 19);
        };
        BiConsumer<BlockPos, BlockState> trunkSetter = (pos, state) -> {
            trunks.add(pos.immutable());
            level.setBlock((BlockPos)pos, (BlockState)state, 19);
        };
        FoliagePlacer.FoliageSetter foliageSetter = new FoliagePlacer.FoliageSetter(){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public void set(BlockPos pos, BlockState state) {
                foliage.add(pos.immutable());
                level.setBlock(pos, state, 19);
            }

            @Override
            public boolean isSet(BlockPos pos) {
                return foliage.contains(pos);
            }
        };
        BiConsumer<BlockPos, BlockState> decorationSetter = (pos, state) -> {
            decorations.add(pos.immutable());
            level.setBlock((BlockPos)pos, (BlockState)state, 19);
        };
        boolean result = this.doPlace(level, random, origin, rootSetter, trunkSetter, foliageSetter, config);
        if (!result || trunks.isEmpty() && foliage.isEmpty()) {
            return false;
        }
        if (!config.decorators.isEmpty()) {
            TreeDecorator.Context decoratorContext = new TreeDecorator.Context(level, decorationSetter, random, trunks, foliage, rootPositions);
            config.decorators.forEach(decorator -> decorator.place(decoratorContext));
        }
        return BoundingBox.encapsulatingPositions(Iterables.concat((Iterable)rootPositions, (Iterable)trunks, (Iterable)foliage, (Iterable)decorations)).map(bounds -> {
            DiscreteVoxelShape shape = TreeFeature.updateLeaves(level, bounds, trunks, decorations, rootPositions);
            StructureTemplate.updateShapeAtEdge(level, 3, shape, bounds.minX(), bounds.minY(), bounds.minZ());
            return true;
        }).orElse(false);
    }

    /*
     * Unable to fully structure code
     */
    private static DiscreteVoxelShape updateLeaves(LevelAccessor level, BoundingBox bounds, Set<BlockPos> logs, Set<BlockPos> decorationSet, Set<BlockPos> rootPositions) {
        shape = new BitSetDiscreteVoxelShape(bounds.getXSpan(), bounds.getYSpan(), bounds.getZSpan());
        maxDistance = 7;
        toCheck = Lists.newArrayList();
        for (i = 0; i < 7; ++i) {
            toCheck.add(Sets.newHashSet());
        }
        for (BlockPos pos : Lists.newArrayList((Iterable)Sets.union(decorationSet, rootPositions))) {
            if (!bounds.isInside(pos)) continue;
            shape.fill(pos.getX() - bounds.minX(), pos.getY() - bounds.minY(), pos.getZ() - bounds.minZ());
        }
        neighborPos = new BlockPos.MutableBlockPos();
        smallestDistance = 0;
        ((Set)toCheck.get(0)).addAll(logs);
        block2: while (true) {
            if (smallestDistance < 7 && ((Set)toCheck.get(smallestDistance)).isEmpty()) {
                ++smallestDistance;
                continue;
            }
            if (smallestDistance >= 7) break;
            iterator = ((Set)toCheck.get(smallestDistance)).iterator();
            pos = (BlockPos)iterator.next();
            iterator.remove();
            if (!bounds.isInside(pos)) continue;
            if (smallestDistance != 0) {
                state = level.getBlockState(pos);
                TreeFeature.setBlockKnownShape(level, pos, (BlockState)state.setValue(BlockStateProperties.DISTANCE, smallestDistance));
            }
            shape.fill(pos.getX() - bounds.minX(), pos.getY() - bounds.minY(), pos.getZ() - bounds.minZ());
            var12_14 = Direction.values();
            var13_15 = var12_14.length;
            var14_16 = 0;
            while (true) {
                if (var14_16 < var13_15) ** break;
                continue block2;
                direction = var12_14[var14_16];
                neighborPos.setWithOffset((Vec3i)pos, direction);
                if (bounds.isInside(neighborPos) && !shape.isFull(xInShape = neighborPos.getX() - bounds.minX(), yInShape = neighborPos.getY() - bounds.minY(), zinShape = neighborPos.getZ() - bounds.minZ()) && !(distance = LeavesBlock.getOptionalDistanceAt(currentState = level.getBlockState(neighborPos))).isEmpty() && (newDistance = Math.min(distance.getAsInt(), smallestDistance + 1)) < 7) {
                    ((Set)toCheck.get(newDistance)).add(neighborPos.immutable());
                    smallestDistance = Math.min(smallestDistance, newDistance);
                }
                ++var14_16;
            }
            break;
        }
        return shape;
    }

    public static List<BlockPos> getLowestTrunkOrRootOfTree(TreeDecorator.Context context) {
        ArrayList blockPositions = Lists.newArrayList();
        ObjectArrayList<BlockPos> roots = context.roots();
        ObjectArrayList<BlockPos> logs = context.logs();
        if (roots.isEmpty()) {
            blockPositions.addAll(logs);
        } else if (!logs.isEmpty() && ((BlockPos)roots.get(0)).getY() == ((BlockPos)logs.get(0)).getY()) {
            blockPositions.addAll(logs);
            blockPositions.addAll(roots);
        } else {
            blockPositions.addAll(roots);
        }
        return blockPositions;
    }
}

