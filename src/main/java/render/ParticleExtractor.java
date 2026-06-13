package com.dx12.render;

import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParticleExtractor {

    // 每个粒子的数据: x, y, z, rotX, rotY, rotZ, rotW, scale, u0, u1, v0, v1, color, light
    public static List<float[]> extractParticles(ParticlesRenderState state) {
        List<float[]> particles = new ArrayList<>();

        if (state == null || state.particles == null) {
            return particles;
        }

        for (ParticleGroupRenderState group : state.particles) {
            if (group instanceof QuadParticleRenderState) {
                extractFromQuadGroup((QuadParticleRenderState) group, particles);
            }
        }

        return particles;
    }

    private static void extractFromQuadGroup(QuadParticleRenderState quadState, List<float[]> out) {
        try {
            // 反射获取 particles 字段 (Map<Layer, Storage>)
            Field particlesField = QuadParticleRenderState.class.getDeclaredField("particles");
            particlesField.setAccessible(true);
            Map<?, ?> particlesMap = (Map<?, ?>) particlesField.get(quadState);

            if (particlesMap == null) return;

            for (Object storage : particlesMap.values()) {
                extractFromStorage(storage, out);
            }
        } catch (Exception e) {
            // 静默失败，粒子不是关键功能
        }
    }

    private static void extractFromStorage(Object storage, List<float[]> out) {
        try {
            // 获取 currentParticleIndex 和 floatValues/intValues
            Field currentField = storage.getClass().getDeclaredField("currentParticleIndex");
            currentField.setAccessible(true);
            int count = currentField.getInt(storage);

            Field floatField = storage.getClass().getDeclaredField("floatValues");
            floatField.setAccessible(true);
            float[] floats = (float[]) floatField.get(storage);

            Field intField = storage.getClass().getDeclaredField("intValues");
            intField.setAccessible(true);
            int[] ints = (int[]) intField.get(storage);

            for (int i = 0; i < count; i++) {
                int fi = i * 12;
                int ii = i * 2;
                float[] particle = new float[14];
                particle[0] = floats[fi];     // x
                particle[1] = floats[fi+1];   // y
                particle[2] = floats[fi+2];   // z
                particle[3] = floats[fi+3];   // xRot
                particle[4] = floats[fi+4];   // yRot
                particle[5] = floats[fi+5];   // zRot
                particle[6] = floats[fi+6];   // wRot
                particle[7] = floats[fi+7];   // scale
                particle[8] = floats[fi+8];   // u0
                particle[9] = floats[fi+9];   // u1
                particle[10] = floats[fi+10]; // v0
                particle[11] = floats[fi+11]; // v1
                particle[12] = ints[ii];      // color (ABGR)
                particle[13] = ints[ii+1];    // lightCoords
                out.add(particle);
            }
        } catch (Exception e) {
            // 忽略
        }
    }
}