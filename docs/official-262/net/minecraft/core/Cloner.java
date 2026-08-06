/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.JavaOps
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JavaOps;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

public class Cloner<T> {
    private final Codec<T> directCodec;

    private Cloner(Codec<T> directCodec) {
        this.directCodec = directCodec;
    }

    public T clone(T value, HolderLookup.Provider from, HolderLookup.Provider to) {
        RegistryOps sourceOps = from.createSerializationContext(JavaOps.INSTANCE);
        RegistryOps targetOps = to.createSerializationContext(JavaOps.INSTANCE);
        Object serialized = this.directCodec.encodeStart(sourceOps, value).getOrThrow(error -> new IllegalStateException("Failed to encode: " + error));
        return (T)this.directCodec.parse(targetOps, serialized).getOrThrow(error -> new IllegalStateException("Failed to decode: " + error));
    }

    public static class Factory {
        private final Map<ResourceKey<? extends Registry<?>>, Cloner<?>> codecs = new HashMap();

        public <T> Factory addCodec(ResourceKey<? extends Registry<? extends T>> key, Codec<T> codec) {
            this.codecs.put(key, new Cloner<T>(codec));
            return this;
        }

        public <T> @Nullable Cloner<T> cloner(ResourceKey<? extends Registry<? extends T>> key) {
            return this.codecs.get(key);
        }
    }
}

