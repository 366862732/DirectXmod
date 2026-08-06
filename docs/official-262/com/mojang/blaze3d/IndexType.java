/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public enum IndexType {
    SHORT(2),
    INT(4);

    public final int bytes;

    private IndexType(int bytes) {
        this.bytes = bytes;
    }

    public static IndexType least(int length) {
        if ((length & 0xFFFF0000) != 0) {
            return INT;
        }
        return SHORT;
    }
}

