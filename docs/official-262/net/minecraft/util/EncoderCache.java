/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.cache.CacheBuilder
 *  com.google.common.cache.CacheLoader
 *  com.google.common.cache.LoadingCache
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Objects;
import net.minecraft.nbt.Tag;

public class EncoderCache {
    private final LoadingCache<Key<?, ?>, DataResult<?>> cache;

    public EncoderCache(int maximumSize) {
        this.cache = CacheBuilder.newBuilder().maximumSize((long)maximumSize).concurrencyLevel(1).softValues().build(new CacheLoader<Key<?, ?>, DataResult<?>>(this){
            {
                Objects.requireNonNull(this$0);
            }

            public DataResult<?> load(Key<?, ?> key) {
                return key.resolve();
            }
        });
    }

    public <A> Codec<A> wrap(final Codec<A> codec) {
        return new Codec<A>(this){
            final /* synthetic */ EncoderCache this$0;
            {
                EncoderCache encoderCache = this$0;
                Objects.requireNonNull(encoderCache);
                this.this$0 = encoderCache;
            }

            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                return codec.decode(ops, input);
            }

            public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
                return ((DataResult)this.this$0.cache.getUnchecked(new Key(codec, input, ops))).map(value -> {
                    if (value instanceof Tag) {
                        Tag tag = (Tag)value;
                        return tag.copy();
                    }
                    return value;
                });
            }
        };
    }

    private record Key<A, T>(Codec<A> codec, A value, DynamicOps<T> ops) {
        public DataResult<T> resolve() {
            return this.codec.encodeStart(this.ops, this.value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Key) {
                Key key = (Key)obj;
                return this.codec == key.codec && this.value.equals(key.value) && this.ops.equals(key.ops);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(this.codec);
            result = 31 * result + this.value.hashCode();
            result = 31 * result + this.ops.hashCode();
            return result;
        }
    }
}

