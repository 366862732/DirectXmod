/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.login.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public interface CustomQueryPayload {
    public Identifier id();

    public void write(FriendlyByteBuf var1);
}

