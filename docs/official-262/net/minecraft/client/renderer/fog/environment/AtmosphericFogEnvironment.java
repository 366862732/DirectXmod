/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.fog.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class AtmosphericFogEnvironment
extends FogEnvironment {
    private static final int MIN_RAIN_FOG_SKY_LIGHT = 8;
    private static final float RAIN_FOG_START_OFFSET = -160.0f;
    private static final float RAIN_FOG_END_OFFSET = -256.0f;
    private float rainFogMultiplier;

    @Override
    public int getBaseColor(ClientLevel level, Camera camera, int renderDistance, float partialTicks) {
        int fogColor = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_COLOR, partialTicks);
        if (renderDistance >= 4) {
            int color;
            float alpha;
            float sunAngle = camera.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE, partialTicks).floatValue() * ((float)Math.PI / 180);
            float sunX = Mth.sin(sunAngle) > 0.0f ? -1.0f : 1.0f;
            Vector3fc forwardVector = camera.isPanoramicMode() ? camera.panoramicForwards() : camera.forwardVector();
            float lookingAtTheSunFactor = forwardVector.dot(sunX, 0.0f, 0.0f);
            if (lookingAtTheSunFactor > 0.0f && (alpha = ARGB.alphaFloat(color = camera.attributeProbe().getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, partialTicks).intValue())) > 0.0f) {
                fogColor = ARGB.srgbLerp(lookingAtTheSunFactor * alpha, fogColor, ARGB.opaque(color));
            }
        }
        int skyColor = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_COLOR, partialTicks);
        skyColor = AtmosphericFogEnvironment.applyWeatherDarken(skyColor, level.getRainLevel(partialTicks), level.getThunderLevel(partialTicks));
        float skyFogEnd = Math.min(camera.attributeProbe().getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, partialTicks).floatValue() / 16.0f, (float)renderDistance);
        float skyColorMixFactor = Mth.clampedLerp(skyFogEnd / 32.0f, 0.25f, 1.0f);
        skyColorMixFactor = 1.0f - (float)Math.pow(skyColorMixFactor, 0.25);
        fogColor = ARGB.srgbLerp(skyColorMixFactor, fogColor, skyColor);
        return fogColor;
    }

    private static int applyWeatherDarken(int color, float rainLevel, float thunderLevel) {
        if (rainLevel > 0.0f) {
            float rainColorModifier = 1.0f - rainLevel * 0.5f;
            float rainBlueColorModifier = 1.0f - rainLevel * 0.4f;
            color = ARGB.scaleRGB(color, rainColorModifier, rainColorModifier, rainBlueColorModifier);
        }
        if (thunderLevel > 0.0f) {
            color = ARGB.scaleRGB(color, 1.0f - thunderLevel * 0.5f);
        }
        return color;
    }

    @Override
    public void setupFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker) {
        this.updateRainFogState(camera, level, deltaTracker);
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        fog.environmentalStart = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_START_DISTANCE, partialTicks).floatValue();
        fog.environmentalEnd = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_END_DISTANCE, partialTicks).floatValue();
        fog.environmentalStart += -160.0f * this.rainFogMultiplier;
        float minRainFogEnd = Math.min(96.0f, fog.environmentalEnd);
        fog.environmentalEnd = Math.max(minRainFogEnd, fog.environmentalEnd + -256.0f * this.rainFogMultiplier);
        fog.skyEnd = Math.min(renderDistance, camera.attributeProbe().getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, partialTicks).floatValue());
        fog.cloudEnd = Math.min((float)(Minecraft.getInstance().options.cloudRange().get() * 16), camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_FOG_END_DISTANCE, partialTicks).floatValue());
        if (Minecraft.getInstance().gui.hud.getBossOverlay().shouldCreateWorldFog()) {
            fog.environmentalStart = Math.min(fog.environmentalStart, 10.0f);
            fog.skyEnd = fog.environmentalEnd = Math.min(fog.environmentalEnd, 96.0f);
            fog.cloudEnd = fog.environmentalEnd;
        }
    }

    private void updateRainFogState(Camera camera, ClientLevel level, DeltaTracker deltaTracker) {
        BlockPos blockPos = camera.blockPosition();
        Biome biome = level.getBiome(blockPos).value();
        float deltaTicks = deltaTracker.getGameTimeDeltaTicks();
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
        boolean rainsInBiome = biome.hasPrecipitation();
        float skyLightLevelMultiplier = Mth.clamp(((float)level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(blockPos) - 8.0f) / 7.0f, 0.0f, 1.0f);
        float targetRainFogMultiplier = level.getRainLevel(partialTicks) * skyLightLevelMultiplier * (rainsInBiome ? 1.0f : 0.5f);
        this.rainFogMultiplier += (targetRainFogMultiplier - this.rainFogMultiplier) * deltaTicks * 0.2f;
    }

    @Override
    public boolean isApplicable(@Nullable FogType fogType, Entity entity) {
        return fogType == FogType.ATMOSPHERIC;
    }
}

