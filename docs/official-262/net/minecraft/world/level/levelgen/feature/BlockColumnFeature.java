/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;

public class BlockColumnFeature
extends Feature<BlockColumnConfiguration> {
    public BlockColumnFeature(Codec<BlockColumnConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockColumnConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockColumnConfiguration config = context.config();
        RandomSource random = context.random();
        int layerCount = config.layers().size();
        int[] layerHeights = new int[layerCount];
        int totalHeight = 0;
        for (int i = 0; i < layerCount; ++i) {
            layerHeights[i] = config.layers().get(i).height().sample(random);
            totalHeight += layerHeights[i];
        }
        if (totalHeight == 0) {
            return false;
        }
        BlockPos.MutableBlockPos placePos = context.origin().mutable();
        BlockPos.MutableBlockPos nextPos = placePos.mutable().move(config.direction());
        for (int y = 0; y < totalHeight; ++y) {
            if (!config.allowedPlacement().test(level, nextPos)) {
                BlockColumnFeature.truncate(layerHeights, totalHeight, y, config.prioritizeTip());
                break;
            }
            nextPos.move(config.direction());
        }
        for (int i = 0; i < layerCount; ++i) {
            int count = layerHeights[i];
            if (count == 0) continue;
            BlockColumnConfiguration.Layer layer = config.layers().get(i);
            for (int y = 0; y < count; ++y) {
                level.setBlock(placePos, layer.state().getState(level, random, placePos), 2);
                placePos.move(config.direction());
            }
        }
        return true;
    }

    private static void truncate(int[] layerHeights, int totalHeight, int newHeight, boolean prioritizeTip) {
        int toRemoveFromLayer;
        int amountToRemove = totalHeight - newHeight;
        int direction = prioritizeTip ? 1 : -1;
        int start = prioritizeTip ? 0 : layerHeights.length - 1;
        int end = prioritizeTip ? layerHeights.length : -1;
        for (int i = start; i != end && amountToRemove > 0; amountToRemove -= toRemoveFromLayer, i += direction) {
            int thisLayer = layerHeights[i];
            toRemoveFromLayer = Math.min(thisLayer, amountToRemove);
            int n = i;
            layerHeights[n] = layerHeights[n] - toRemoveFromLayer;
        }
    }
}

