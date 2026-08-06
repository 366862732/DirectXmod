/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.core;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;

public record Rotations(float x, float y, float z) {
    public static final Codec<Rotations> CODEC = Codec.FLOAT.listOf().comapFlatMap(input -> Util.fixedSize(input, 3).map(floats -> new Rotations(((Float)floats.get(0)).floatValue(), ((Float)floats.get(1)).floatValue(), ((Float)floats.get(2)).floatValue())), rotations -> List.of(Float.valueOf(rotations.x()), Float.valueOf(rotations.y()), Float.valueOf(rotations.z())));
    public static final StreamCodec<ByteBuf, Rotations> STREAM_CODEC = new StreamCodec<ByteBuf, Rotations>(){

        @Override
        public Rotations decode(ByteBuf input) {
            return new Rotations(input.readFloat(), input.readFloat(), input.readFloat());
        }

        @Override
        public void encode(ByteBuf output, Rotations value) {
            output.writeFloat(value.x);
            output.writeFloat(value.y);
            output.writeFloat(value.z);
        }
    };

    public Rotations {
        x = Float.isInfinite(x) || Float.isNaN(x) ? 0.0f : x % 360.0f;
        y = Float.isInfinite(y) || Float.isNaN(y) ? 0.0f : y % 360.0f;
        z = Float.isInfinite(z) || Float.isNaN(z) ? 0.0f : z % 360.0f;
    }
}

