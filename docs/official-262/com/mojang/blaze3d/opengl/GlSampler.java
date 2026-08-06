/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL33C
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL33C;

@Environment(value=EnvType.CLIENT)
public class GlSampler
extends GpuSampler {
    private final int id;
    private final AddressMode addressModeU;
    private final AddressMode addressModeV;
    private final FilterMode minFilter;
    private final FilterMode magFilter;
    private final int maxAnisotropy;
    private final OptionalDouble maxLod;
    private boolean closed;

    public GlSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter, int maxAnisotropy, OptionalDouble maxLod) {
        this.addressModeU = addressModeU;
        this.addressModeV = addressModeV;
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.maxAnisotropy = maxAnisotropy;
        this.maxLod = maxLod;
        this.id = GL33C.glGenSamplers();
        GL33C.glSamplerParameteri((int)this.id, (int)10242, (int)GlConst.toGl(addressModeU));
        GL33C.glSamplerParameteri((int)this.id, (int)10243, (int)GlConst.toGl(addressModeV));
        if (maxAnisotropy > 1) {
            GL33C.glSamplerParameterf((int)this.id, (int)34046, (float)maxAnisotropy);
        }
        switch (minFilter) {
            case NEAREST: {
                GL33C.glSamplerParameteri((int)this.id, (int)10241, (int)9986);
                break;
            }
            case LINEAR: {
                GL33C.glSamplerParameteri((int)this.id, (int)10241, (int)9987);
            }
        }
        switch (magFilter) {
            case NEAREST: {
                GL33C.glSamplerParameteri((int)this.id, (int)10240, (int)9728);
                break;
            }
            case LINEAR: {
                GL33C.glSamplerParameteri((int)this.id, (int)10240, (int)9729);
            }
        }
        if (maxLod.isPresent()) {
            GL33C.glSamplerParameterf((int)this.id, (int)33083, (float)((float)maxLod.getAsDouble()));
        }
    }

    public int getId() {
        return this.id;
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
            GL33C.glDeleteSamplers((int)this.id);
        }
    }

    public boolean isClosed() {
        return this.closed;
    }
}

