/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.systems;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class SurfaceException
extends Exception {
    public SurfaceException(String message) {
        super(message);
    }

    public SurfaceException(Throwable cause) {
        super(cause);
    }
}

