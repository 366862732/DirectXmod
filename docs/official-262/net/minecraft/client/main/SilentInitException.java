/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class SilentInitException
extends RuntimeException {
    public SilentInitException(String message) {
        super(message);
    }

    public SilentInitException(String message, Throwable cause) {
        super(message, cause);
    }
}

