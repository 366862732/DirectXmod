/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.login.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;

public record DiscardedQueryAnswerPayload() implements CustomQueryAnswerPayload
{
    public static final DiscardedQueryAnswerPayload INSTANCE = new DiscardedQueryAnswerPayload();

    @Override
    public void write(FriendlyByteBuf output) {
    }
}

