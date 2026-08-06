/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.codec;

@FunctionalInterface
public interface StreamMemberEncoder<O, T> {
    public void encode(T var1, O var2);
}

