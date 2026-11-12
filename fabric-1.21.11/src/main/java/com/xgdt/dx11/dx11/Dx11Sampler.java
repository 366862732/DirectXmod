package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.OptionalDouble;

/** {@link GpuSampler} backed by a native {@code ID3D11SamplerState}. */
final class Dx11Sampler extends GpuSampler implements Dx11Resource {
    private final Dx11Device device;
    private final AddressMode addressModeU;
    private final AddressMode addressModeV;
    private final FilterMode minFilter;
    private final FilterMode magFilter;
    private final int maxAnisotropy;
    private final OptionalDouble maxLod;
    private final long handle;
    private boolean closed;

    Dx11Sampler(
        Dx11Device device,
        AddressMode addressModeU,
        AddressMode addressModeV,
        FilterMode minFilter,
        FilterMode magFilter,
        int maxAnisotropy,
        OptionalDouble maxLod
    ) {
        this.device = device;
        this.addressModeU = addressModeU;
        this.addressModeV = addressModeV;
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.maxAnisotropy = maxAnisotropy;
        this.maxLod = maxLod;
        this.handle = Dx11Native.nativeCreateSampler(
            device.nativeHandle(),
            addressModeU.ordinal(),
            addressModeV.ordinal(),
            minFilter.ordinal(),
            magFilter.ordinal(),
            maxAnisotropy,
            maxLod.isPresent(),
            maxLod.orElse(0.0)
        );
        device.register(this);
    }

    long handle() {
        return this.handle;
    }

    boolean isClosed() {
        return this.closed;
    }

    @Override
    public AddressMode getAddressModeU() {
        return this.addressModeU;
    }

    @Override
    public AddressMode getAddressModeV() {
        return this.addressModeV;
    }

    @Override
    public FilterMode getMinFilter() {
        return this.minFilter;
    }

    @Override
    public FilterMode getMagFilter() {
        return this.magFilter;
    }

    @Override
    public int getMaxAnisotropy() {
        return this.maxAnisotropy;
    }

    @Override
    public OptionalDouble getMaxLod() {
        return this.maxLod;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.device.unregister(this);
            Dx11Native.nativeDestroySampler(this.handle);
        }
    }

    @Override
    public long nativeHandle() {
        return this.handle;
    }

    @Override
    public void destroyNative() {
        close();
    }
}
