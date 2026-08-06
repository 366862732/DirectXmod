/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector4f
 *  org.joml.Vector4fc
 *  org.lwjgl.system.MemoryStack
 */
package net.minecraft.client.renderer.fog;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;

@Environment(value=EnvType.CLIENT)
public class FogRenderer
implements AutoCloseable {
    public static final int FOG_UBO_SIZE = new Std140SizeCalculator().putVec4().putFloat().putFloat().putFloat().putFloat().putFloat().putFloat().get();
    private static final List<FogEnvironment> FOG_ENVIRONMENTS = Lists.newArrayList((Object[])new FogEnvironment[]{new LavaFogEnvironment(), new PowderedSnowFogEnvironment(), new BlindnessFogEnvironment(), new DarknessFogEnvironment(), new WaterFogEnvironment(), new AtmosphericFogEnvironment()});
    private static boolean fogEnabled = true;
    private final GpuBuffer emptyBuffer;
    private final MappableRingBuffer regularBuffer;

    public FogRenderer() {
        GpuDevice device = RenderSystem.getDevice();
        this.regularBuffer = new MappableRingBuffer(() -> "Fog UBO", 130, FOG_UBO_SIZE);
        try (MemoryStack stack = MemoryStack.stackPush();){
            ByteBuffer buffer = stack.malloc(FOG_UBO_SIZE);
            this.updateBuffer(buffer, 0, new Vector4f(0.0f), Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
            this.emptyBuffer = device.createBuffer(() -> "Empty fog", 128, buffer.flip());
        }
        RenderSystem.setShaderFog(this.getBuffer(FogMode.NONE));
    }

    @Override
    public void close() {
        this.emptyBuffer.close();
        this.regularBuffer.close();
    }

    public void endFrame() {
        this.regularBuffer.rotate();
    }

    public GpuBufferSlice getBuffer(FogMode mode) {
        if (!fogEnabled) {
            return this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
        }
        return switch (mode.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.emptyBuffer.slice(0L, FOG_UBO_SIZE);
            case 1 -> this.regularBuffer.currentBuffer().slice(0L, FOG_UBO_SIZE);
        };
    }

    private void computeFogColor(Camera camera, float partialTicks, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f dest) {
        float brightenFactor;
        FogType fogType = this.getFogType(camera);
        Entity entity = camera.entity();
        FogEnvironment colorSourceEnvironment = null;
        FogEnvironment darknessModifyingEnvironment = null;
        for (FogEnvironment fogEnvironment : FOG_ENVIRONMENTS) {
            if (!fogEnvironment.isApplicable(fogType, entity)) continue;
            if (colorSourceEnvironment == null && fogEnvironment.providesColor()) {
                colorSourceEnvironment = fogEnvironment;
            }
            if (darknessModifyingEnvironment != null || !fogEnvironment.modifiesDarkness()) continue;
            darknessModifyingEnvironment = fogEnvironment;
        }
        if (colorSourceEnvironment == null) {
            throw new IllegalStateException("No color source environment found");
        }
        int color = colorSourceEnvironment.getBaseColor(level, camera, renderDistance, partialTicks);
        float voidDarknessOnsetRange = level.getLevelData().voidDarknessOnsetRange();
        float darkness = Mth.clamp((voidDarknessOnsetRange + (float)level.getMinY() - (float)camera.position().y) / voidDarknessOnsetRange, 0.0f, 1.0f);
        if (darknessModifyingEnvironment != null) {
            LivingEntity livingEntity = (LivingEntity)entity;
            darkness = darknessModifyingEnvironment.getModifiedDarkness(livingEntity, darkness, partialTicks);
        }
        float fogRed = ARGB.redFloat(color);
        float fogGreen = ARGB.greenFloat(color);
        float fogBlue = ARGB.blueFloat(color);
        if (darkness > 0.0f && fogType != FogType.LAVA && fogType != FogType.POWDER_SNOW) {
            float brightness = Mth.square(1.0f - darkness);
            fogRed *= brightness;
            fogGreen *= brightness;
            fogBlue *= brightness;
        }
        if (darkenWorldAmount > 0.0f) {
            fogRed = Mth.lerp(darkenWorldAmount, fogRed, fogRed * 0.7f);
            fogGreen = Mth.lerp(darkenWorldAmount, fogGreen, fogGreen * 0.6f);
            fogBlue = Mth.lerp(darkenWorldAmount, fogBlue, fogBlue * 0.6f);
        }
        if (fogType == FogType.WATER) {
            if (entity instanceof LocalPlayer) {
                LocalPlayer localPlayer = (LocalPlayer)entity;
                brightenFactor = localPlayer.getWaterVision();
            } else {
                brightenFactor = 1.0f;
            }
        } else {
            LivingEntity livingEntity;
            brightenFactor = entity instanceof LivingEntity && (livingEntity = (LivingEntity)entity).hasEffect(MobEffects.NIGHT_VISION) && !livingEntity.hasEffect(MobEffects.DARKNESS) ? GameRenderer.nightVisionScale(livingEntity, partialTicks) : 0.0f;
        }
        if (fogRed != 0.0f && fogGreen != 0.0f && fogBlue != 0.0f) {
            float maxColor = Math.max(fogRed, Math.max(fogGreen, fogBlue));
            float targetScale = 1.0f / Math.clamp(maxColor, 0.07f, 1.0f);
            float targetScaleMax = 1.0f / maxColor;
            float scale = maxColor != fogRed ? targetScale : targetScaleMax;
            fogRed = Mth.lerp(brightenFactor, fogRed, fogRed * scale);
            scale = maxColor != fogGreen ? targetScale : targetScaleMax;
            fogGreen = Mth.lerp(brightenFactor, fogGreen, fogGreen * scale);
            scale = maxColor != fogBlue ? targetScale : targetScaleMax;
            fogBlue = Mth.lerp(brightenFactor, fogBlue, fogBlue * scale);
        }
        dest.set(fogRed, fogGreen, fogBlue, 1.0f);
    }

    public static boolean toggleFog() {
        fogEnabled = !fogEnabled;
        return fogEnabled;
    }

    public FogData setupFog(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level) {
        float partialTickTime = deltaTracker.getGameTimeDeltaPartialTick(false);
        float renderDistanceInBlocks = renderDistanceInChunks * 16;
        FogType fogType = this.getFogType(camera);
        Entity entity = camera.entity();
        FogData fog = new FogData();
        this.computeFogColor(camera, partialTickTime, level, renderDistanceInChunks, darkenWorldAmount, fog.color);
        for (FogEnvironment fogEnvironment : FOG_ENVIRONMENTS) {
            if (!fogEnvironment.isApplicable(fogType, entity)) continue;
            fogEnvironment.setupFog(fog, camera, level, renderDistanceInBlocks, deltaTracker);
            break;
        }
        float renderDistanceFogSpan = Mth.clamp(renderDistanceInBlocks / 10.0f, 4.0f, 64.0f);
        fog.renderDistanceStart = renderDistanceInBlocks - renderDistanceFogSpan;
        fog.renderDistanceEnd = renderDistanceInBlocks;
        return fog;
    }

    public void updateBuffer(FogData fog) {
        try (GpuBufferSlice.MappedView view = this.regularBuffer.currentBuffer().map(false, true);){
            this.updateBuffer(view.data(), 0, fog.color, fog.environmentalStart, fog.environmentalEnd, fog.renderDistanceStart, fog.renderDistanceEnd, fog.skyEnd, fog.cloudEnd);
        }
    }

    private FogType getFogType(Camera camera) {
        FogType blockFogType = camera.getFluidInCamera();
        if (blockFogType == FogType.NONE) {
            return FogType.ATMOSPHERIC;
        }
        return blockFogType;
    }

    private void updateBuffer(ByteBuffer byteBuffer, int offset, Vector4f fogColor, float environmentalStart, float environmentalEnd, float renderDistanceStart, float renderDistanceEnd, float skyEnd, float endClouds) {
        byteBuffer.position(offset);
        Std140Builder.intoBuffer(byteBuffer).putVec4((Vector4fc)fogColor).putFloat(environmentalStart).putFloat(environmentalEnd).putFloat(renderDistanceStart).putFloat(renderDistanceEnd).putFloat(skyEnd).putFloat(endClouds);
    }

    @Environment(value=EnvType.CLIENT)
    public static enum FogMode {
        NONE,
        WORLD;

    }
}

