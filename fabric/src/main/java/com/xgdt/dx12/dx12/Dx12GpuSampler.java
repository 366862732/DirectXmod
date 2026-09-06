package com.xgdt.dx12.dx12;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * D3D12-backed {@link GpuSampler}.
 *
 * Native mapping (see dx12_mc.dll {@code dx12CreateSampler}):
 * address 0=REPEAT/1=CLAMP_TO_EDGE, filter 0=NEAREST/1=LINEAR.
 */
@Environment(EnvType.CLIENT)
public class Dx12GpuSampler extends GpuSampler {
    private final long handle;
    private final AddressMode addressModeU;
    private final AddressMode addressModeV;
    private final FilterMode minFilter;
    private final FilterMode magFilter;
    private final int maxAnisotropy;
    private final OptionalDouble maxLod;
    private boolean closed;

    public Dx12GpuSampler(AddressMode addressModeU, AddressMode addressModeV,
        FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod) {
        this.addressModeU = addressModeU;
        this.addressModeV = addressModeV;
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.maxAnisotropy = maxAnisotropy;
        this.maxLod = maxLod;
        float lod = (float) maxLod.orElse(16.0);
        this.handle = Dx12Native.dx12CreateSampler(addressModeU.ordinal(), addressModeV.ordinal(),
            minFilter.ordinal(), magFilter.ordinal(), maxAnisotropy, lod);
        if (this.handle == 0) {
            throw new IllegalStateException("dx12CreateSampler returned a null handle");
        }
    }

    /** Native handle ({@code Dx12Object*} as long). */
    public long handle() {
        return this.handle;
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
        if (this.closed) {
            return;
        }
        this.closed = true;
        Dx12Native.dx12DestroyResource(this.handle);
    }
}
