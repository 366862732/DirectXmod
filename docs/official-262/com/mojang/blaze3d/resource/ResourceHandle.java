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
public interface ResourceHandle<T> {
    public static final ResourceHandle<?> INVALID_HANDLE = () -> {
        throw new IllegalStateException("Cannot dereference handle with no underlying resource");
    };

    public static <T> ResourceHandle<T> invalid() {
        return INVALID_HANDLE;
    }

    public T get();
}

