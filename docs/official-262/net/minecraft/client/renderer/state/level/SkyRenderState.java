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
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;

@Environment(value=EnvType.CLIENT)
public class SkyRenderState {
    public DimensionType.Skybox skybox = DimensionType.Skybox.NONE;
    public boolean shouldRenderDarkDisc;
    public float sunAngle;
    public float moonAngle;
    public float starAngle;
    public float rainBrightness;
    public float starBrightness;
    public int sunriseAndSunsetColor;
    public MoonPhase moonPhase = MoonPhase.FULL_MOON;
    public int skyColor;
    public float endFlashIntensity;
    public float endFlashXAngle;
    public float endFlashYAngle;

    public void reset() {
        this.skybox = DimensionType.Skybox.NONE;
    }
}

