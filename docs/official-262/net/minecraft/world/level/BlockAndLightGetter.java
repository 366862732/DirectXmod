/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;

public interface BlockAndLightGetter
extends BlockGetter {
    public LevelLightEngine getLightEngine();

    default public int getBrightness(LightLayer layer, BlockPos pos) {
        return this.getLightEngine().getLayerListener(layer).getLightValue(pos);
    }

    default public int getRawBrightness(BlockPos pos, int darkening) {
        return this.getLightEngine().getRawBrightness(pos, darkening);
    }

    default public boolean canSeeSky(BlockPos pos) {
        return this.getBrightness(LightLayer.SKY, pos) >= 15;
    }
}

