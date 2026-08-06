/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BrandPayload(String brand) implements CustomPacketPayload
{
    public static final StreamCodec<FriendlyByteBuf, BrandPayload> STREAM_CODEC = CustomPacketPayload.codec(BrandPayload::write, BrandPayload::new);
    public static final CustomPacketPayload.Type<BrandPayload> TYPE = CustomPacketPayload.createType("brand");

    private BrandPayload(FriendlyByteBuf input) {
        this(input.readUtf());
    }

    private void write(FriendlyByteBuf output) {
        output.writeUtf(this.brand);
    }

    public CustomPacketPayload.Type<BrandPayload> type() {
        return TYPE;
    }
}

