/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class VoidStartPlatformFeature
extends Feature<NoneFeatureConfiguration> {
    private static final BlockPos PLATFORM_OFFSET = new BlockPos(8, 3, 8);
    private static final ChunkPos PLATFORM_ORIGIN_CHUNK = ChunkPos.containing(PLATFORM_OFFSET);
    private static final int PLATFORM_RADIUS = 16;
    private static final int PLATFORM_RADIUS_CHUNKS = 1;

    public VoidStartPlatformFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static int checkerboardDistance(int xa, int za, int xb, int zb) {
        return Math.max(Math.abs(xa - xb), Math.abs(za - zb));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos currentChunkPos = ChunkPos.containing(context.origin());
        if (VoidStartPlatformFeature.checkerboardDistance(currentChunkPos.x(), currentChunkPos.z(), PLATFORM_ORIGIN_CHUNK.x(), PLATFORM_ORIGIN_CHUNK.z()) > 1) {
            return true;
        }
        BlockPos platformOrigin = PLATFORM_OFFSET.atY(context.origin().getY() + PLATFORM_OFFSET.getY());
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int z = currentChunkPos.getMinBlockZ(); z <= currentChunkPos.getMaxBlockZ(); ++z) {
            for (int x = currentChunkPos.getMinBlockX(); x <= currentChunkPos.getMaxBlockX(); ++x) {
                if (VoidStartPlatformFeature.checkerboardDistance(platformOrigin.getX(), platformOrigin.getZ(), x, z) > 16) continue;
                blockPos.set(x, platformOrigin.getY(), z);
                if (blockPos.equals(platformOrigin)) {
                    level.setBlock(blockPos, Blocks.COBBLESTONE.defaultBlockState(), 2);
                    continue;
                }
                level.setBlock(blockPos, Blocks.STONE.defaultBlockState(), 2);
            }
        }
        return true;
    }
}

