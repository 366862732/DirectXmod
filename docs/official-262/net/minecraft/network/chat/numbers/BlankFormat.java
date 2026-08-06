/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.network.chat.numbers;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatType;
import net.minecraft.network.codec.StreamCodec;

public class BlankFormat
implements NumberFormat {
    public static final BlankFormat INSTANCE = new BlankFormat();
    public static final NumberFormatType<BlankFormat> TYPE = new NumberFormatType<BlankFormat>(){
        private static final MapCodec<BlankFormat> CODEC = MapCodec.unit((Object)INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, BlankFormat> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public MapCodec<BlankFormat> mapCodec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlankFormat> streamCodec() {
            return STREAM_CODEC;
        }
    };

    private BlankFormat() {
    }

    @Override
    public MutableComponent format(int value) {
        return Component.empty();
    }

    public NumberFormatType<BlankFormat> type() {
        return TYPE;
    }
}

