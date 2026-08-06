/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.textures;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public abstract class GpuSampler
implements AutoCloseable {
    public abstract AddressMode getAddressModeU();

    public abstract AddressMode getAddressModeV();

    public abstract FilterMode getMinFilter();

    public abstract FilterMode getMagFilter();

    public abstract int getMaxAnisotropy();

    public abstract OptionalDouble getMaxLod();

    @Override
    public abstract void close();
}

