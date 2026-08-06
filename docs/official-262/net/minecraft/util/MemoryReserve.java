/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util;

import org.jspecify.annotations.Nullable;

public class MemoryReserve {
    private static byte @Nullable [] reserve;

    public static void allocate() {
        reserve = new byte[0xA00000];
    }

    public static void release() {
        if (reserve != null) {
            reserve = null;
            try {
                System.gc();
                System.gc();
                System.gc();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }
}

