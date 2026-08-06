/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.syncher;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;

public interface EntityDataSerializer<T> {
    public StreamCodec<? super RegistryFriendlyByteBuf, T> codec();

    default public EntityDataAccessor<T> createAccessor(int id) {
        return new EntityDataAccessor(id, this);
    }

    public T copy(T var1);

    public static <T> EntityDataSerializer<T> forValueType(StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return () -> codec;
    }

    public static interface ForValueType<T>
    extends EntityDataSerializer<T> {
        @Override
        default public T copy(T value) {
            return value;
        }
    }
}

