package com.dx12.render;

import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;

public class SkyboxExtractor {

    public static float[] extractSkyData(SkyRenderState state) {
        if (state == null || state.skybox == DimensionType.Skybox.NONE) {
            return null;
        }

        // 安全提取颜色：尝试将 skyColor 作为 ARGB int 处理
        float r, g, b, a;
        try {
            // 假设 skyColor 是 int（ARGB 格式）
            int colorInt = (int) state.skyColor;
            r = ((colorInt >> 16) & 0xFF) / 255.0f;
            g = ((colorInt >> 8) & 0xFF) / 255.0f;
            b = (colorInt & 0xFF) / 255.0f;
            a = ((colorInt >> 24) & 0xFF) / 255.0f;
        } catch (ClassCastException e) {
            // 如果 skyColor 实际上是 float，直接使用（但钳位到 0-1）
            float c = (float) state.skyColor;
            if (c > 1.0f && c <= 255.0f) c /= 255.0f;
            if (c < 0.0f) c = 0.0f;
            if (c > 1.0f) c = 1.0f;
            r = g = b = c;
            a = 1.0f;
        }

        // 返回 C++ 端期望的格式：[r, g, b, a, sunAngle, moonAngle]
        return new float[] {
            r, g, b, a,
            state.sunAngle,
            state.moonAngle
        };
    }

    public static boolean isEndSky(SkyRenderState state) {
        return state != null && state.skybox == DimensionType.Skybox.END;
    }

    public static float getEndFlashIntensity(SkyRenderState state) {
        return state != null ? state.endFlashIntensity : 0f;
    }
}