/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.resource;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface ResourceDescriptor<T> {
    public T allocate();

    default public void prepare(T resource) {
    }

    public void free(T var1);

    default public boolean canUsePhysicalResource(ResourceDescriptor<?> other) {
        return this.equals(other);
    }
}

