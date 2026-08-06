/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.slf4j.Logger;

public class MonsterRoomFeature
extends Feature<NoneFeatureConfiguration> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityType<?>[] MOBS = new EntityType[]{EntityTypes.SKELETON, EntityTypes.ZOMBIE, EntityTypes.ZOMBIE, EntityTypes.SPIDER};
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

    public MonsterRoomFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        int dz;
        int dy;
        int dx;
        Predicate<BlockState> replaceableTag = Feature.isReplaceable(BlockTags.FEATURES_CANNOT_REPLACE);
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        int hr = 3;
        int xr = random.nextInt(2) + 2;
        int minX = -xr - 1;
        int maxX = xr + 1;
        int minY = -1;
        int maxY = 4;
        int zr = random.nextInt(2) + 2;
        int minZ = -zr - 1;
        int maxZ = zr + 1;
        int holeCount = 0;
        for (dx = minX; dx <= maxX; ++dx) {
            for (dy = -1; dy <= 4; ++dy) {
                for (dz = minZ; dz <= maxZ; ++dz) {
                    BlockPos holePos = origin.offset(dx, dy, dz);
                    boolean solid = level.getBlockState(holePos).isSolid();
                    if (dy == -1 && !solid) {
                        return false;
                    }
                    if (dy == 4 && !solid) {
                        return false;
                    }
                    if (dx != minX && dx != maxX && dz != minZ && dz != maxZ || dy != 0 || !level.isEmptyBlock(holePos) || !level.isEmptyBlock(holePos.above())) continue;
                    ++holeCount;
                }
            }
        }
        if (holeCount < 1 || holeCount > 5) {
            return false;
        }
        for (dx = minX; dx <= maxX; ++dx) {
            for (dy = 3; dy >= -1; --dy) {
                for (dz = minZ; dz <= maxZ; ++dz) {
                    BlockPos wallBlock = origin.offset(dx, dy, dz);
                    BlockState wallState = level.getBlockState(wallBlock);
                    if (dx == minX || dy == -1 || dz == minZ || dx == maxX || dy == 4 || dz == maxZ) {
                        if (wallBlock.getY() >= level.getMinY() && !level.getBlockState(wallBlock.below()).isSolid()) {
                            level.setBlock(wallBlock, AIR, 2);
                            continue;
                        }
                        if (!wallState.isSolid() || wallState.is(Blocks.CHEST)) continue;
                        if (dy == -1 && random.nextInt(4) != 0) {
                            this.safeSetBlock(level, wallBlock, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), replaceableTag);
                            continue;
                        }
                        this.safeSetBlock(level, wallBlock, Blocks.COBBLESTONE.defaultBlockState(), replaceableTag);
                        continue;
                    }
                    if (wallState.is(Blocks.CHEST) || wallState.is(Blocks.SPAWNER)) continue;
                    this.safeSetBlock(level, wallBlock, AIR, replaceableTag);
                }
            }
        }
        block6: for (int cc = 0; cc < 2; ++cc) {
            for (int i = 0; i < 3; ++i) {
                int zc;
                int yc;
                int xc = origin.getX() + random.nextInt(xr * 2 + 1) - xr;
                BlockPos chestPos = new BlockPos(xc, yc = origin.getY(), zc = origin.getZ() + random.nextInt(zr * 2 + 1) - zr);
                if (!level.isEmptyBlock(chestPos)) continue;
                int wallCount = 0;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    if (!level.getBlockState(chestPos.relative(direction)).isSolid()) continue;
                    ++wallCount;
                }
                if (wallCount != 1) continue;
                this.safeSetBlock(level, chestPos, StructurePiece.reorient(level, chestPos, Blocks.CHEST.defaultBlockState()), replaceableTag);
                RandomizableContainer.setBlockEntityLootTable(level, random, chestPos, BuiltInLootTables.SIMPLE_DUNGEON);
                continue block6;
            }
        }
        this.safeSetBlock(level, origin, Blocks.SPAWNER.defaultBlockState(), replaceableTag);
        BlockEntity blockEntity = level.getBlockEntity(origin);
        if (blockEntity instanceof SpawnerBlockEntity) {
            SpawnerBlockEntity spawner = (SpawnerBlockEntity)blockEntity;
            spawner.setEntityId(this.randomEntityId(random), random);
        } else {
            LOGGER.error("Failed to fetch mob spawner entity at ({}, {}, {})", new Object[]{origin.getX(), origin.getY(), origin.getZ()});
        }
        return true;
    }

    private EntityType<?> randomEntityId(RandomSource random) {
        return Util.getRandom(MOBS, random);
    }
}

