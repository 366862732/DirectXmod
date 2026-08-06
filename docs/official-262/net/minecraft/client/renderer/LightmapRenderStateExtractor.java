/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.EndFlashState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class LightmapRenderStateExtractor {
    public static final Vector3fc WHITE = new Vector3f(1.0f, 1.0f, 1.0f);
    private boolean needsUpdate;
    private final GameRenderer renderer;
    private final Minecraft minecraft;
    private final RandomSource randomSource = RandomSource.create();
    private float blockLightFlicker;

    public LightmapRenderStateExtractor(GameRenderer renderer, Minecraft minecraft) {
        this.renderer = renderer;
        this.minecraft = minecraft;
    }

    public void tick() {
        this.blockLightFlicker += (this.randomSource.nextFloat() - this.randomSource.nextFloat()) * this.randomSource.nextFloat() * this.randomSource.nextFloat() * 0.1f;
        this.blockLightFlicker *= 0.9f;
        this.needsUpdate = true;
    }

    private float calculateDarknessScale(LivingEntity camera, float darknessGamma, float partialTickTime) {
        float darkness = 0.45f * darknessGamma;
        return Math.max(0.0f, Mth.cos(((float)camera.tickCount - partialTickTime) * (float)Math.PI * 0.025f) * darkness);
    }

    public void extract(LightmapRenderState renderState, float partialTicks) {
        renderState.needsUpdate = this.needsUpdate;
        if (!this.needsUpdate) {
            return;
        }
        ClientLevel level = this.minecraft.level;
        LocalPlayer player = this.minecraft.player;
        if (level == null || player == null) {
            return;
        }
        ProfilerFiller profiler = Profiler.get();
        profiler.push("lightmap");
        Camera camera = this.renderer.mainCamera();
        renderState.blockFactor = this.blockLightFlicker + 1.4f;
        renderState.blockLightTint = ARGB.vector3fFromRGB24(camera.attributeProbe().getValue(EnvironmentAttributes.BLOCK_LIGHT_TINT, partialTicks));
        renderState.skyFactor = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partialTicks).floatValue();
        renderState.skyLightColor = ARGB.vector3fFromRGB24(camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, partialTicks));
        EndFlashState endFlashState = level.endFlashState();
        if (endFlashState != null && !this.minecraft.options.hideLightningFlash().get().booleanValue()) {
            float intensity = endFlashState.getIntensity(partialTicks);
            renderState.skyFactor = this.minecraft.gui.hud.getBossOverlay().shouldCreateWorldFog() ? (renderState.skyFactor += intensity / 3.0f) : (renderState.skyFactor += intensity);
        }
        renderState.ambientColor = ARGB.vector3fFromRGB24(camera.attributeProbe().getValue(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, partialTicks));
        float brightnessOption = this.minecraft.options.gamma().get().floatValue();
        float darknessEffectScaleOption = this.minecraft.options.darknessEffectScale().get().floatValue();
        float darknessEffectBrightnessModifier = player.getEffectBlendFactor(MobEffects.DARKNESS, partialTicks) * darknessEffectScaleOption;
        renderState.brightness = Math.max(0.0f, brightnessOption - darknessEffectBrightnessModifier);
        renderState.darknessEffectScale = this.calculateDarknessScale(player, darknessEffectBrightnessModifier, partialTicks) * darknessEffectScaleOption;
        float waterVision = player.getWaterVision();
        renderState.nightVisionEffectIntensity = player.hasEffect(MobEffects.NIGHT_VISION) ? GameRenderer.nightVisionScale(player, partialTicks) : (waterVision > 0.0f && player.hasEffect(MobEffects.CONDUIT_POWER) ? waterVision : 0.0f);
        renderState.nightVisionColor = ARGB.vector3fFromRGB24(camera.attributeProbe().getValue(EnvironmentAttributes.NIGHT_VISION_COLOR, partialTicks));
        renderState.bossOverlayWorldDarkening = this.renderer.bossOverlayWorldDarkening(partialTicks);
        profiler.pop();
        this.needsUpdate = false;
    }
}

