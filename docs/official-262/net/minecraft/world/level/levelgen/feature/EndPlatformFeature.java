/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class EndPlatformFeature
extends Feature<NoneFeatureConfiguration> {
    public EndPlatformFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        EndPlatformFeature.createEndPlatform(context.level(), context.origin(), false);
        return true;
    }

    public static void createEndPlatform(ServerLevelAccessor newLevel, BlockPos origin, boolean dropResources) {
        BlockPos.MutableBlockPos pos = origin.mutable();
        for (int dz = -2; dz <= 2; ++dz) {
            for (int dx = -2; dx <= 2; ++dx) {
                for (int dy = -1; dy < 3; ++dy) {
                    Block block;
                    BlockPos.MutableBlockPos blockPos = pos.set(origin).move(dx, dy, dz);
                    Block block2 = block = dy == -1 ? Blocks.OBSIDIAN : Blocks.AIR;
                    if (newLevel.getBlockState(blockPos).is(block)) continue;
                    if (dropResources) {
                        newLevel.destroyBlock(blockPos, true, null);
                    }
                    newLevel.setBlock(blockPos, block.defaultBlockState(), 3);
                }
            }
        }
    }
}

