package com.dx12.render;

import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;

public class SkyboxExtractor {

    public static float[] extractSkyData(SkyRenderState state) {
        if (state == null || state.skybox == DimensionType.Skybox.NONE) {
            return null;
        }

        // 返回天空参数数组: [skyColor, sunAngle, moonAngle, starAngle, rainBrightness, starBrightness, sunriseColor]
        return new float[] {
                state.skyColor,
                state.sunAngle,
                state.moonAngle,
                state.starAngle,
                state.rainBrightness,
                state.starBrightness,
                state.sunriseAndSunsetColor
        };
    }

    public static boolean isEndSky(SkyRenderState state) {
        return state != null && state.skybox == DimensionType.Skybox.END;
    }

    public static float getEndFlashIntensity(SkyRenderState state) {
        return state != null ? state.endFlashIntensity : 0f;
    }
}