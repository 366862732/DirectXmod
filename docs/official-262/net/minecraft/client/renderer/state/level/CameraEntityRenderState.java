/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.state.level;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class CameraEntityRenderState {
    public float hurtTime;
    public int hurtDuration;
    public float deathTime;
    public boolean isSleeping;
    public boolean isLiving;
    public boolean isPlayer;
    public boolean isDeadOrDying;
    public boolean doesMobEffectBlockSky;
    public float hurtDir;
    public float backwardsInterpolatedWalkDistance;
    public float bob;
}

