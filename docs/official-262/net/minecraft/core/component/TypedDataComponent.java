/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.core.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record TypedDataComponent<T>(DataComponentType<T> type, T value) {
    public static final StreamCodec<RegistryFriendlyByteBuf, TypedDataComponent<?>> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, TypedDataComponent<?>>(){

        @Override
        public TypedDataComponent<?> decode(RegistryFriendlyByteBuf input) {
            DataComponentType type = (DataComponentType)DataComponentType.STREAM_CODEC.decode(input);
            return 1.decodeTyped(input, type);
        }

        private static <T> TypedDataComponent<T> decodeTyped(RegistryFriendlyByteBuf input, DataComponentType<T> type) {
            return new TypedDataComponent<T>(type, type.streamCodec().decode(input));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf output, TypedDataComponent<?> value) {
            1.encodeCap(output, value);
        }

        private static <T> void encodeCap(RegistryFriendlyByteBuf output, TypedDataComponent<T> component) {
            DataComponentType.STREAM_CODEC.encode(output, component.type());
            component.type().streamCodec().encode(output, component.value());
        }
    };

    static TypedDataComponent<?> fromEntryUnchecked(Map.Entry<DataComponentType<?>, Object> entry) {
        return TypedDataComponent.createUnchecked(entry.getKey(), entry.getValue());
    }

    public static <T> TypedDataComponent<T> createUnchecked(DataComponentType<T> type, Object value) {
        return new TypedDataComponent<Object>(type, value);
    }

    public void applyTo(PatchedDataComponentMap components) {
        components.set(this.type, this.value);
    }

    public <D> DataResult<D> encodeValue(DynamicOps<D> ops) {
        Codec<T> codec = this.type.codec();
        if (codec == null) {
            return DataResult.error(() -> "Component of type " + String.valueOf(this.type) + " is not encodable");
        }
        return codec.encodeStart(ops, this.value);
    }

    @Override
    public String toString() {
        return String.valueOf(this.type) + "=>" + String.valueOf(this.value);
    }
}

