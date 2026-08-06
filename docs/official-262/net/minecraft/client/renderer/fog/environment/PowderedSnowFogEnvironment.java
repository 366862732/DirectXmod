/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.fog.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class PowderedSnowFogEnvironment
extends FogEnvironment {
    private static final int COLOR = -6308916;

    @Override
    public int getBaseColor(ClientLevel level, Camera camera, int renderDistance, float partialTicks) {
        return -6308916;
    }

    @Override
    public void setupFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker) {
        if (camera.entity().isSpectator()) {
            fog.environmentalStart = -8.0f;
            fog.environmentalEnd = renderDistance * 0.5f;
        } else {
            fog.environmentalStart = 0.0f;
            fog.environmentalEnd = 2.0f;
        }
        fog.skyEnd = fog.environmentalEnd;
        fog.cloudEnd = fog.environmentalEnd;
    }

    @Override
    public boolean isApplicable(@Nullable FogType fogType, Entity entity) {
        return fogType == FogType.POWDER_SNOW;
    }
}

