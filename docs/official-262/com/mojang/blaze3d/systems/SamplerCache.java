/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.systems;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class SamplerCache {
    private final GpuSampler[] samplers = new GpuSampler[32];

    public void initialize() {
        GpuDevice device = RenderSystem.getDevice();
        if (AddressMode.values().length != 2 || FilterMode.values().length != 2) {
            throw new IllegalStateException("AddressMode and FilterMode enum sizes must be 2 - if you expanded them, please update SamplerCache");
        }
        for (AddressMode addressModeU : AddressMode.values()) {
            for (AddressMode addressModeV : AddressMode.values()) {
                for (FilterMode minFilter : FilterMode.values()) {
                    for (FilterMode magFilter : FilterMode.values()) {
                        for (boolean useMipmaps : new boolean[]{true, false}) {
                            this.samplers[SamplerCache.encode((AddressMode)addressModeU, (AddressMode)addressModeV, (FilterMode)minFilter, (FilterMode)magFilter, (boolean)useMipmaps)] = device.createSampler(addressModeU, addressModeV, minFilter, magFilter, 1, useMipmaps ? OptionalDouble.empty() : OptionalDouble.of(0.0));
                        }
                    }
                }
            }
        }
    }

    public GpuSampler getSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, boolean useMipmaps) {
        return this.samplers[SamplerCache.encode(addressModeU, addressModeV, minFilter, magFilter, useMipmaps)];
    }

    public GpuSampler getClampToEdge(FilterMode minMag) {
        return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, minMag, minMag, false);
    }

    public GpuSampler getClampToEdge(FilterMode minMag, boolean mipmaps) {
        return this.getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, minMag, minMag, mipmaps);
    }

    public GpuSampler getRepeat(FilterMode minMag) {
        return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, minMag, minMag, false);
    }

    public GpuSampler getRepeat(FilterMode minMag, boolean mipmaps) {
        return this.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, minMag, minMag, mipmaps);
    }

    public void close() {
        for (GpuSampler sampler : this.samplers) {
            sampler.close();
        }
    }

    @VisibleForTesting
    static int encode(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, boolean useMipmaps) {
        int result = 0;
        result |= addressModeU.ordinal() & 1;
        result |= (addressModeV.ordinal() & 1) << 1;
        result |= (minFilter.ordinal() & 1) << 2;
        result |= (magFilter.ordinal() & 1) << 3;
        if (useMipmaps) {
            result |= 0x10;
        }
        return result;
    }
}

