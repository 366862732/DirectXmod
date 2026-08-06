/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public enum GraphicsPreset implements StringRepresentable
{
    FAST("fast", "options.graphics.fast"),
    FANCY("fancy", "options.graphics.fancy"),
    FABULOUS("fabulous", "options.graphics.fabulous"),
    CUSTOM("custom", "options.graphics.custom");

    private final String serializedName;
    private final String key;
    public static final Codec<GraphicsPreset> CODEC;

    private GraphicsPreset(String serializedName, String key) {
        this.serializedName = serializedName;
        this.key = key;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String getKey() {
        return this.key;
    }

    public void apply(Minecraft minecraft) {
        OptionsSubScreen screen = minecraft.gui != null && minecraft.gui.screen() instanceof OptionsSubScreen ? (OptionsSubScreen)minecraft.gui.screen() : null;
        GpuDevice device = RenderSystem.getDevice();
        switch (this.ordinal()) {
            case 0: {
                int viewDistance = 8;
                GraphicsPreset.set(screen, minecraft.options.biomeBlendRadius(), 1);
                GraphicsPreset.set(screen, minecraft.options.renderDistance(), 8);
                GraphicsPreset.set(screen, minecraft.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.NONE);
                GraphicsPreset.set(screen, minecraft.options.simulationDistance(), 6);
                GraphicsPreset.set(screen, minecraft.options.ambientOcclusion(), false);
                GraphicsPreset.set(screen, minecraft.options.cloudStatus(), CloudStatus.FAST);
                GraphicsPreset.set(screen, minecraft.options.particles(), ParticleStatus.DECREASED);
                GraphicsPreset.set(screen, minecraft.options.mipmapLevels(), 2);
                GraphicsPreset.set(screen, minecraft.options.entityShadows(), false);
                GraphicsPreset.set(screen, minecraft.options.entityDistanceScaling(), 0.75);
                GraphicsPreset.set(screen, minecraft.options.menuBackgroundBlurriness(), 2);
                GraphicsPreset.set(screen, minecraft.options.cloudRange(), 32);
                GraphicsPreset.set(screen, minecraft.options.cutoutLeaves(), false);
                GraphicsPreset.set(screen, minecraft.options.improvedTransparency(), false);
                GraphicsPreset.set(screen, minecraft.options.weatherRadius(), 5);
                GraphicsPreset.set(screen, minecraft.options.maxAnisotropyBit(), 1);
                GraphicsPreset.set(screen, minecraft.options.textureFiltering(), TextureFilteringMethod.NONE);
                break;
            }
            case 1: {
                int viewDistance = 16;
                GraphicsPreset.set(screen, minecraft.options.biomeBlendRadius(), 2);
                GraphicsPreset.set(screen, minecraft.options.renderDistance(), 16);
                GraphicsPreset.set(screen, minecraft.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.PLAYER_AFFECTED);
                GraphicsPreset.set(screen, minecraft.options.simulationDistance(), 12);
                GraphicsPreset.set(screen, minecraft.options.ambientOcclusion(), true);
                GraphicsPreset.set(screen, minecraft.options.cloudStatus(), CloudStatus.FANCY);
                GraphicsPreset.set(screen, minecraft.options.particles(), ParticleStatus.ALL);
                GraphicsPreset.set(screen, minecraft.options.mipmapLevels(), 4);
                GraphicsPreset.set(screen, minecraft.options.entityShadows(), true);
                GraphicsPreset.set(screen, minecraft.options.entityDistanceScaling(), 1.0);
                GraphicsPreset.set(screen, minecraft.options.menuBackgroundBlurriness(), 5);
                GraphicsPreset.set(screen, minecraft.options.cloudRange(), 64);
                GraphicsPreset.set(screen, minecraft.options.cutoutLeaves(), true);
                GraphicsPreset.set(screen, minecraft.options.improvedTransparency(), false);
                GraphicsPreset.set(screen, minecraft.options.weatherRadius(), 10);
                GraphicsPreset.set(screen, minecraft.options.maxAnisotropyBit(), 1);
                GraphicsPreset.set(screen, minecraft.options.textureFiltering(), TextureFilteringMethod.RGSS);
                break;
            }
            case 2: {
                int viewDistance = 32;
                GraphicsPreset.set(screen, minecraft.options.biomeBlendRadius(), 2);
                GraphicsPreset.set(screen, minecraft.options.renderDistance(), 32);
                GraphicsPreset.set(screen, minecraft.options.prioritizeChunkUpdates(), PrioritizeChunkUpdates.PLAYER_AFFECTED);
                GraphicsPreset.set(screen, minecraft.options.simulationDistance(), 12);
                GraphicsPreset.set(screen, minecraft.options.ambientOcclusion(), true);
                GraphicsPreset.set(screen, minecraft.options.cloudStatus(), CloudStatus.FANCY);
                GraphicsPreset.set(screen, minecraft.options.particles(), ParticleStatus.ALL);
                GraphicsPreset.set(screen, minecraft.options.mipmapLevels(), 4);
                GraphicsPreset.set(screen, minecraft.options.entityShadows(), true);
                GraphicsPreset.set(screen, minecraft.options.entityDistanceScaling(), 1.25);
                GraphicsPreset.set(screen, minecraft.options.menuBackgroundBlurriness(), 5);
                GraphicsPreset.set(screen, minecraft.options.cloudRange(), 128);
                GraphicsPreset.set(screen, minecraft.options.cutoutLeaves(), true);
                GraphicsPreset.set(screen, minecraft.options.improvedTransparency(), Util.getPlatform() != Util.OS.OSX);
                GraphicsPreset.set(screen, minecraft.options.weatherRadius(), 10);
                GraphicsPreset.set(screen, minecraft.options.maxAnisotropyBit(), 2);
                if (device.getDeviceInfo().hintsAndWorkarounds().anisotropyHasKnownIssues()) {
                    GraphicsPreset.set(screen, minecraft.options.textureFiltering(), TextureFilteringMethod.RGSS);
                    break;
                }
                GraphicsPreset.set(screen, minecraft.options.textureFiltering(), TextureFilteringMethod.ANISOTROPIC);
            }
        }
    }

    private static <T> void set(@Nullable OptionsSubScreen screen, OptionInstance<T> option, T value) {
        if (option.get() != value) {
            option.set(value);
            if (screen != null) {
                screen.resetOption(option);
            }
        }
    }

    static {
        CODEC = StringRepresentable.fromEnum(GraphicsPreset::values);
    }
}

