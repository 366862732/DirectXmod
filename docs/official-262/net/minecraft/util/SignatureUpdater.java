/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import java.security.SignatureException;

@FunctionalInterface
public interface SignatureUpdater {
    public void update(Output var1) throws SignatureException;

    @FunctionalInterface
    public static interface Output {
        public void update(byte[] var1) throws SignatureException;
    }
}

