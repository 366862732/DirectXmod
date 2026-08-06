/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.login.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.Identifier;

public record DiscardedQueryPayload(Identifier id) implements CustomQueryPayload
{
    @Override
    public void write(FriendlyByteBuf output) {
    }
}

