/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class LightCoordsUtil {
    public static final int FULL_BRIGHT = 0xF000F0;
    public static final int FULL_SKY = 0xF00000;
    private static final int MAX_SMOOTH_LIGHT_LEVEL = 240;

    public static int pack(int block, int sky) {
        return block << 4 | sky << 20;
    }

    public static int block(int packed) {
        return packed >> 4 & 0xF;
    }

    public static int sky(int packed) {
        return packed >> 20 & 0xF;
    }

    public static int withBlock(int coords, int block) {
        return coords & 0xFF0000 | block << 4;
    }

    public static int smoothPack(int block, int sky) {
        return block & 0xFF | (sky & 0xFF) << 16;
    }

    public static int smoothBlock(int packed) {
        return packed & 0xFF;
    }

    public static int smoothSky(int packed) {
        return packed >> 16 & 0xFF;
    }

    public static int addSmoothBlockEmission(int lightCoords, float blockLightEmission) {
        blockLightEmission = Mth.clamp(blockLightEmission, 0.0f, 1.0f);
        int emittedBlock = (int)(Mth.clamp(blockLightEmission, 0.0f, 1.0f) * 240.0f);
        int block = Math.min(LightCoordsUtil.smoothBlock(lightCoords) + emittedBlock, 240);
        return LightCoordsUtil.smoothPack(block, LightCoordsUtil.smoothSky(lightCoords));
    }

    public static int max(int coords1, int coords2) {
        int block1 = LightCoordsUtil.block(coords1);
        int block2 = LightCoordsUtil.block(coords2);
        int sky1 = LightCoordsUtil.sky(coords1);
        int sky2 = LightCoordsUtil.sky(coords2);
        return LightCoordsUtil.pack(Math.max(block1, block2), Math.max(sky1, sky2));
    }

    public static int lightCoordsWithEmission(int lightCoords, int emission) {
        if (emission == 0) {
            return lightCoords;
        }
        int sky = Math.max(LightCoordsUtil.sky(lightCoords), emission);
        int block = Math.max(LightCoordsUtil.block(lightCoords), emission);
        return LightCoordsUtil.pack(block, sky);
    }

    public static int smoothBlend(int neighbor1, int neighbor2, int neighbor3, int center) {
        if (LightCoordsUtil.sky(center) > 2 || LightCoordsUtil.block(center) > 2) {
            if (neighbor1 == 0) {
                neighbor1 = center;
            } else if (LightCoordsUtil.sky(neighbor1) == 0) {
                neighbor1 |= center & 0xFF0000;
            }
            if (neighbor2 == 0) {
                neighbor2 = center;
            } else if (LightCoordsUtil.sky(neighbor2) == 0) {
                neighbor2 |= center & 0xFF0000;
            }
            if (neighbor3 == 0) {
                neighbor3 = center;
            } else if (LightCoordsUtil.sky(neighbor3) == 0) {
                neighbor3 |= center & 0xFF0000;
            }
        }
        return neighbor1 + neighbor2 + neighbor3 + center >> 2 & 0xFF00FF;
    }

    public static int smoothWeightedBlend(int coords1, int coords2, int coords3, int coords4, float weight1, float weight2, float weight3, float weight4) {
        int sky = (int)((float)LightCoordsUtil.smoothSky(coords1) * weight1 + (float)LightCoordsUtil.smoothSky(coords2) * weight2 + (float)LightCoordsUtil.smoothSky(coords3) * weight3 + (float)LightCoordsUtil.smoothSky(coords4) * weight4);
        int block = (int)((float)LightCoordsUtil.smoothBlock(coords1) * weight1 + (float)LightCoordsUtil.smoothBlock(coords2) * weight2 + (float)LightCoordsUtil.smoothBlock(coords3) * weight3 + (float)LightCoordsUtil.smoothBlock(coords4) * weight4);
        return LightCoordsUtil.smoothPack(block, sky);
    }

    public static int getLightCoords(BlockAndLightGetter level, BlockPos pos) {
        return LightCoordsUtil.getLightCoords(BrightnessGetter.DEFAULT, level, level.getBlockState(pos), pos);
    }

    public static int getLightCoords(BrightnessGetter brightnessGetter, BlockAndLightGetter level, BlockState state, BlockPos pos) {
        int blockSelfEmission;
        if (state.emissiveRendering()) {
            return 0xF000F0;
        }
        int packedBrightness = brightnessGetter.packedBrightness(level, pos);
        int block = LightCoordsUtil.block(packedBrightness);
        if (block < (blockSelfEmission = state.getLightEmission())) {
            return LightCoordsUtil.withBlock(packedBrightness, blockSelfEmission);
        }
        return packedBrightness;
    }

    @FunctionalInterface
    public static interface BrightnessGetter {
        public static final BrightnessGetter DEFAULT = (level, pos) -> {
            int sky = level.getBrightness(LightLayer.SKY, pos);
            int block = level.getBrightness(LightLayer.BLOCK, pos);
            return LightCoordsUtil.pack(block, sky);
        };

        public int packedBrightness(BlockAndLightGetter var1, BlockPos var2);
    }
}

