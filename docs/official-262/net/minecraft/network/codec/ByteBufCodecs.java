/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMultimap
 *  com.google.common.collect.ImmutableMultimap$Builder
 *  com.google.common.collect.Multimap
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonSyntaxException
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.properties.Property
 *  com.mojang.authlib.properties.PropertyMap
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  io.netty.buffer.ByteBuf
 *  io.netty.handler.codec.DecoderException
 *  io.netty.handler.codec.EncoderException
 *  org.joml.Quaternionfc
 *  org.joml.Vector3fc
 */
package net.minecraft.network.codec;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public interface ByteBufCodecs {
    public static final int MAX_INITIAL_COLLECTION_SIZE = 65536;
    public static final StreamCodec<ByteBuf, Boolean> BOOL = new StreamCodec<ByteBuf, Boolean>(){

        @Override
        public Boolean decode(ByteBuf input) {
            return input.readBoolean();
        }

        @Override
        public void encode(ByteBuf output, Boolean value) {
            output.writeBoolean(value.booleanValue());
        }
    };
    public static final StreamCodec<ByteBuf, Byte> BYTE = new StreamCodec<ByteBuf, Byte>(){

        @Override
        public Byte decode(ByteBuf input) {
            return input.readByte();
        }

        @Override
        public void encode(ByteBuf output, Byte value) {
            output.writeByte((int)value.byteValue());
        }
    };
    public static final StreamCodec<ByteBuf, Float> ROTATION_BYTE = BYTE.map(Mth::unpackDegrees, Mth::packDegrees);
    public static final StreamCodec<ByteBuf, Short> SHORT = new StreamCodec<ByteBuf, Short>(){

        @Override
        public Short decode(ByteBuf input) {
            return input.readShort();
        }

        @Override
        public void encode(ByteBuf output, Short value) {
            output.writeShort((int)value.shortValue());
        }
    };
    public static final StreamCodec<ByteBuf, Integer> UNSIGNED_SHORT = new StreamCodec<ByteBuf, Integer>(){

        @Override
        public Integer decode(ByteBuf input) {
            return input.readUnsignedShort();
        }

        @Override
        public void encode(ByteBuf output, Integer value) {
            output.writeShort(value.intValue());
        }
    };
    public static final StreamCodec<ByteBuf, Integer> INT = new StreamCodec<ByteBuf, Integer>(){

        @Override
        public Integer decode(ByteBuf input) {
            return input.readInt();
        }

        @Override
        public void encode(ByteBuf output, Integer value) {
            output.writeInt(value.intValue());
        }
    };
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = new StreamCodec<ByteBuf, Integer>(){

        @Override
        public Integer decode(ByteBuf input) {
            return VarInt.read(input);
        }

        @Override
        public void encode(ByteBuf output, Integer value) {
            VarInt.write(output, value);
        }
    };
    public static final StreamCodec<ByteBuf, OptionalInt> OPTIONAL_VAR_INT = VAR_INT.map(i -> i == 0 ? OptionalInt.empty() : OptionalInt.of(i - 1), o -> o.isPresent() ? o.getAsInt() + 1 : 0);
    public static final StreamCodec<ByteBuf, Long> LONG = new StreamCodec<ByteBuf, Long>(){

        @Override
        public Long decode(ByteBuf input) {
            return input.readLong();
        }

        @Override
        public void encode(ByteBuf output, Long value) {
            output.writeLong(value.longValue());
        }
    };
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = new StreamCodec<ByteBuf, Long>(){

        @Override
        public Long decode(ByteBuf input) {
            return VarLong.read(input);
        }

        @Override
        public void encode(ByteBuf output, Long value) {
            VarLong.write(output, value);
        }
    };
    public static final StreamCodec<ByteBuf, Float> FLOAT = new StreamCodec<ByteBuf, Float>(){

        @Override
        public Float decode(ByteBuf input) {
            return Float.valueOf(input.readFloat());
        }

        @Override
        public void encode(ByteBuf output, Float value) {
            output.writeFloat(value.floatValue());
        }
    };
    public static final StreamCodec<ByteBuf, Double> DOUBLE = new StreamCodec<ByteBuf, Double>(){

        @Override
        public Double decode(ByteBuf input) {
            return input.readDouble();
        }

        @Override
        public void encode(ByteBuf output, Double value) {
            output.writeDouble(value.doubleValue());
        }
    };
    public static final StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = new StreamCodec<ByteBuf, byte[]>(){

        @Override
        public byte[] decode(ByteBuf input) {
            return FriendlyByteBuf.readByteArray(input);
        }

        @Override
        public void encode(ByteBuf output, byte[] value) {
            FriendlyByteBuf.writeByteArray(output, value);
        }
    };
    public static final StreamCodec<ByteBuf, long[]> LONG_ARRAY = new StreamCodec<ByteBuf, long[]>(){

        @Override
        public long[] decode(ByteBuf input) {
            return FriendlyByteBuf.readLongArray(input);
        }

        @Override
        public void encode(ByteBuf output, long[] value) {
            FriendlyByteBuf.writeLongArray(output, value);
        }
    };
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = ByteBufCodecs.stringUtf8(Short.MAX_VALUE);
    public static final StreamCodec<ByteBuf, Tag> TAG = ByteBufCodecs.tagCodec(NbtAccounter::defaultQuota);
    public static final StreamCodec<ByteBuf, Tag> TRUSTED_TAG = ByteBufCodecs.tagCodec(NbtAccounter::unlimitedHeap);
    public static final StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = ByteBufCodecs.compoundTagCodec(NbtAccounter::defaultQuota);
    public static final StreamCodec<ByteBuf, CompoundTag> TRUSTED_COMPOUND_TAG = ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap);
    public static final StreamCodec<ByteBuf, Optional<CompoundTag>> OPTIONAL_COMPOUND_TAG = new StreamCodec<ByteBuf, Optional<CompoundTag>>(){

        @Override
        public Optional<CompoundTag> decode(ByteBuf input) {
            return Optional.ofNullable(FriendlyByteBuf.readNbt(input));
        }

        @Override
        public void encode(ByteBuf output, Optional<CompoundTag> value) {
            FriendlyByteBuf.writeNbt(output, value.orElse(null));
        }
    };
    public static final StreamCodec<ByteBuf, Vector3fc> VECTOR3F = new StreamCodec<ByteBuf, Vector3fc>(){

        @Override
        public Vector3fc decode(ByteBuf input) {
            return FriendlyByteBuf.readVector3f(input);
        }

        @Override
        public void encode(ByteBuf output, Vector3fc value) {
            FriendlyByteBuf.writeVector3f(output, value);
        }
    };
    public static final StreamCodec<ByteBuf, Quaternionfc> QUATERNIONF = new StreamCodec<ByteBuf, Quaternionfc>(){

        @Override
        public Quaternionfc decode(ByteBuf input) {
            return FriendlyByteBuf.readQuaternion(input);
        }

        @Override
        public void encode(ByteBuf output, Quaternionfc value) {
            FriendlyByteBuf.writeQuaternion(output, value);
        }
    };
    public static final StreamCodec<ByteBuf, Integer> CONTAINER_ID = new StreamCodec<ByteBuf, Integer>(){

        @Override
        public Integer decode(ByteBuf input) {
            return FriendlyByteBuf.readContainerId(input);
        }

        @Override
        public void encode(ByteBuf output, Integer value) {
            FriendlyByteBuf.writeContainerId(output, value);
        }
    };
    public static final StreamCodec<ByteBuf, PropertyMap> GAME_PROFILE_PROPERTIES = new StreamCodec<ByteBuf, PropertyMap>(){

        @Override
        public PropertyMap decode(ByteBuf input) {
            int propertyCount = ByteBufCodecs.readCount(input, 16);
            ImmutableMultimap.Builder result = ImmutableMultimap.builder();
            for (int i = 0; i < propertyCount; ++i) {
                String name = Utf8String.read(input, 64);
                String value = Utf8String.read(input, Short.MAX_VALUE);
                String signature = FriendlyByteBuf.readNullable(input, in -> Utf8String.read(in, 1024));
                Property property = new Property(name, value, signature);
                result.put((Object)property.name(), (Object)property);
            }
            return new PropertyMap((Multimap)result.build());
        }

        @Override
        public void encode(ByteBuf output, PropertyMap properties) {
            ByteBufCodecs.writeCount(output, properties.size(), 16);
            for (Property property : properties.values()) {
                Utf8String.write(output, property.name(), 64);
                Utf8String.write(output, property.value(), Short.MAX_VALUE);
                FriendlyByteBuf.writeNullable(output, property.signature(), (out, signature) -> Utf8String.write(out, signature, 1024));
            }
        }
    };
    public static final StreamCodec<ByteBuf, String> PLAYER_NAME = ByteBufCodecs.stringUtf8(16);
    public static final StreamCodec<ByteBuf, GameProfile> GAME_PROFILE = StreamCodec.composite(UUIDUtil.STREAM_CODEC, GameProfile::id, PLAYER_NAME, GameProfile::name, GAME_PROFILE_PROPERTIES, GameProfile::properties, GameProfile::new);
    public static final StreamCodec<ByteBuf, Integer> RGB_COLOR = new StreamCodec<ByteBuf, Integer>(){

        @Override
        public Integer decode(ByteBuf input) {
            return ARGB.color(input.readByte() & 0xFF, input.readByte() & 0xFF, input.readByte() & 0xFF);
        }

        @Override
        public void encode(ByteBuf output, Integer value) {
            output.writeByte(ARGB.red(value));
            output.writeByte(ARGB.green(value));
            output.writeByte(ARGB.blue(value));
        }
    };

    public static StreamCodec<ByteBuf, byte[]> byteArray(final int maxSize) {
        return new StreamCodec<ByteBuf, byte[]>(){

            @Override
            public byte[] decode(ByteBuf input) {
                return FriendlyByteBuf.readByteArray(input, maxSize);
            }

            @Override
            public void encode(ByteBuf output, byte[] value) {
                if (value.length > maxSize) {
                    throw new EncoderException("ByteArray with size " + value.length + " is bigger than allowed " + maxSize);
                }
                FriendlyByteBuf.writeByteArray(output, value);
            }
        };
    }

    public static StreamCodec<ByteBuf, String> stringUtf8(final int maxStringLength) {
        return new StreamCodec<ByteBuf, String>(){

            @Override
            public String decode(ByteBuf input) {
                return Utf8String.read(input, maxStringLength);
            }

            @Override
            public void encode(ByteBuf output, String value) {
                Utf8String.write(output, value, maxStringLength);
            }
        };
    }

    public static StreamCodec<ByteBuf, Optional<Tag>> optionalTagCodec(final Supplier<NbtAccounter> accounter) {
        return new StreamCodec<ByteBuf, Optional<Tag>>(){

            @Override
            public Optional<Tag> decode(ByteBuf input) {
                return Optional.ofNullable(FriendlyByteBuf.readNbt(input, (NbtAccounter)accounter.get()));
            }

            @Override
            public void encode(ByteBuf output, Optional<Tag> value) {
                FriendlyByteBuf.writeNbt(output, value.orElse(null));
            }
        };
    }

    public static StreamCodec<ByteBuf, Tag> tagCodec(final Supplier<NbtAccounter> accounter) {
        return new StreamCodec<ByteBuf, Tag>(){

            @Override
            public Tag decode(ByteBuf input) {
                Tag result = FriendlyByteBuf.readNbt(input, (NbtAccounter)accounter.get());
                if (result == null) {
                    throw new DecoderException("Expected non-null compound tag");
                }
                return result;
            }

            @Override
            public void encode(ByteBuf output, Tag value) {
                if (value == EndTag.INSTANCE) {
                    throw new EncoderException("Expected non-null compound tag");
                }
                FriendlyByteBuf.writeNbt(output, value);
            }
        };
    }

    public static StreamCodec<ByteBuf, CompoundTag> compoundTagCodec(Supplier<NbtAccounter> accounter) {
        return ByteBufCodecs.tagCodec(accounter).map(tag -> {
            if (tag instanceof CompoundTag) {
                CompoundTag compoundTag = (CompoundTag)tag;
                return compoundTag;
            }
            throw new DecoderException("Not a compound tag: " + String.valueOf(tag));
        }, compoundTag -> compoundTag);
    }

    public static <T> StreamCodec<ByteBuf, T> fromCodecTrusted(Codec<T> codec) {
        return ByteBufCodecs.fromCodec(codec, NbtAccounter::unlimitedHeap);
    }

    public static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec) {
        return ByteBufCodecs.fromCodec(codec, NbtAccounter::defaultQuota);
    }

    public static <T, B extends ByteBuf, V> StreamCodec.CodecOperation<B, T, V> fromCodec(final DynamicOps<T> ops, final Codec<V> codec) {
        return original -> new StreamCodec<B, V>(){

            @Override
            public V decode(B input) {
                Object payload = original.decode(input);
                return codec.parse(ops, payload).getOrThrow(msg -> new DecoderException("Failed to decode: " + msg + " " + String.valueOf(payload)));
            }

            @Override
            public void encode(B output, V value) {
                Object payload = codec.encodeStart(ops, value).getOrThrow(msg -> new EncoderException("Failed to encode: " + msg + " " + String.valueOf(value)));
                original.encode(output, payload);
            }
        };
    }

    public static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec, Supplier<NbtAccounter> accounter) {
        return ByteBufCodecs.tagCodec(accounter).apply(ByteBufCodecs.fromCodec(NbtOps.INSTANCE, codec));
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistriesTrusted(Codec<T> codec) {
        return ByteBufCodecs.fromCodecWithRegistries(codec, NbtAccounter::unlimitedHeap);
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> codec) {
        return ByteBufCodecs.fromCodecWithRegistries(codec, NbtAccounter::defaultQuota);
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(final Codec<T> codec, Supplier<NbtAccounter> accounter) {
        final StreamCodec<ByteBuf, Tag> tagCodec = ByteBufCodecs.tagCodec(accounter);
        return new StreamCodec<RegistryFriendlyByteBuf, T>(){

            @Override
            public T decode(RegistryFriendlyByteBuf input) {
                Tag tag = (Tag)tagCodec.decode(input);
                RegistryOps<Tag> ops = input.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                return codec.parse(ops, (Object)tag).getOrThrow(msg -> new DecoderException("Failed to decode: " + msg + " " + String.valueOf(tag)));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf output, T value) {
                RegistryOps<Tag> ops = output.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                Tag tag = (Tag)codec.encodeStart(ops, value).getOrThrow(msg -> new EncoderException("Failed to encode: " + msg + " " + String.valueOf(value)));
                tagCodec.encode(output, tag);
            }
        };
    }

    public static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(final StreamCodec<? super B, V> original) {
        return new StreamCodec<B, Optional<V>>(){

            @Override
            public Optional<V> decode(B input) {
                if (input.readBoolean()) {
                    return Optional.of(original.decode(input));
                }
                return Optional.empty();
            }

            @Override
            public void encode(B output, Optional<V> value) {
                if (value.isPresent()) {
                    output.writeBoolean(true);
                    original.encode(output, value.get());
                } else {
                    output.writeBoolean(false);
                }
            }
        };
    }

    public static int readCount(ByteBuf input, int maxSize) {
        int count = VarInt.read(input);
        if (count > maxSize) {
            throw new DecoderException(count + " elements exceeded max size of: " + maxSize);
        }
        return count;
    }

    public static void writeCount(ByteBuf output, int count, int maxSize) {
        if (count > maxSize) {
            throw new EncoderException(count + " elements exceeded max size of: " + maxSize);
        }
        VarInt.write(output, count);
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> constructor, StreamCodec<? super B, V> elementCodec) {
        return ByteBufCodecs.collection(constructor, elementCodec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(final IntFunction<C> constructor, final StreamCodec<? super B, V> elementCodec, final int maxSize) {
        return new StreamCodec<B, C>(){

            @Override
            public C decode(B input) {
                int count = ByteBufCodecs.readCount(input, maxSize);
                Collection result = (Collection)constructor.apply(Math.min(count, 65536));
                for (int i = 0; i < count; ++i) {
                    result.add(elementCodec.decode(input));
                }
                return result;
            }

            @Override
            public void encode(B output, C value) {
                ByteBufCodecs.writeCount(output, value.size(), maxSize);
                for (Object element : value) {
                    elementCodec.encode(output, element);
                }
            }
        };
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> constructor) {
        return original -> ByteBufCodecs.collection(constructor, original);
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> constructor, int maxSize) {
        return original -> ByteBufCodecs.collection(constructor, original, maxSize);
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return original -> ByteBufCodecs.collection(ArrayList::new, original);
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int maxSize) {
        return original -> ByteBufCodecs.collection(ArrayList::new, original, maxSize);
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> constructor, StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec) {
        return ByteBufCodecs.map(constructor, keyCodec, valueCodec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(final IntFunction<? extends M> constructor, final StreamCodec<? super B, K> keyCodec, final StreamCodec<? super B, V> valueCodec, final int maxSize) {
        return new StreamCodec<B, M>(){

            @Override
            public void encode(B output, M map) {
                ByteBufCodecs.writeCount(output, map.size(), maxSize);
                map.forEach((k, v) -> {
                    keyCodec.encode(output, k);
                    valueCodec.encode(output, v);
                });
            }

            @Override
            public M decode(B input) {
                int count = ByteBufCodecs.readCount(input, maxSize);
                Map result = (Map)constructor.apply(Math.min(count, 65536));
                for (int i = 0; i < count; ++i) {
                    Object key = keyCodec.decode(input);
                    Object value = valueCodec.decode(input);
                    result.put(key, value);
                }
                return result;
            }
        };
    }

    public static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(final StreamCodec<? super B, L> leftCodec, final StreamCodec<? super B, R> rightCodec) {
        return new StreamCodec<B, Either<L, R>>(){

            @Override
            public Either<L, R> decode(B input) {
                if (input.readBoolean()) {
                    return Either.left(leftCodec.decode(input));
                }
                return Either.right(rightCodec.decode(input));
            }

            @Override
            public void encode(B output, Either<L, R> value) {
                value.ifLeft(left -> {
                    output.writeBoolean(true);
                    leftCodec.encode(output, left);
                }).ifRight(right -> {
                    output.writeBoolean(false);
                    rightCodec.encode(output, right);
                });
            }
        };
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, V> lengthPrefixed(final int maxSize, final BiFunction<B, ByteBuf, B> decorator) {
        return original -> new StreamCodec<B, V>(){

            @Override
            public V decode(B input) {
                int size = VarInt.read(input);
                if (size > maxSize) {
                    throw new DecoderException("Buffer size " + size + " is larger than allowed limit of " + maxSize);
                }
                int index = input.readerIndex();
                ByteBuf limitedSlice = (ByteBuf)decorator.apply(input, input.slice(index, size));
                input.readerIndex(index + size);
                return original.decode(limitedSlice);
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void encode(B output, V value) {
                ByteBuf scratchBuffer = (ByteBuf)decorator.apply(output, output.alloc().buffer());
                try {
                    original.encode(scratchBuffer, value);
                    int size = scratchBuffer.readableBytes();
                    if (size > maxSize) {
                        throw new EncoderException("Buffer size " + size + " is  larger than allowed limit of " + maxSize);
                    }
                    VarInt.write(output, size);
                    output.writeBytes(scratchBuffer);
                }
                finally {
                    scratchBuffer.release();
                }
            }
        };
    }

    public static <V> StreamCodec.CodecOperation<ByteBuf, V, V> lengthPrefixed(int maxSize) {
        return ByteBufCodecs.lengthPrefixed(maxSize, (parent, child) -> child);
    }

    public static <V> StreamCodec.CodecOperation<RegistryFriendlyByteBuf, V, V> registryFriendlyLengthPrefixed(int maxSize) {
        return ByteBufCodecs.lengthPrefixed(maxSize, (parent, child) -> new RegistryFriendlyByteBuf((ByteBuf)child, parent.registryAccess()));
    }

    public static <T> StreamCodec<ByteBuf, T> idMapper(final IntFunction<T> byId, final ToIntFunction<T> toId) {
        return new StreamCodec<ByteBuf, T>(){

            @Override
            public T decode(ByteBuf input) {
                int id = VarInt.read(input);
                return byId.apply(id);
            }

            @Override
            public void encode(ByteBuf output, T value) {
                int id = toId.applyAsInt(value);
                VarInt.write(output, id);
            }
        };
    }

    public static <T> StreamCodec<ByteBuf, T> idMapper(IdMap<T> mapper) {
        return ByteBufCodecs.idMapper(mapper::byIdOrThrow, mapper::getIdOrThrow);
    }

    private static <T, R> StreamCodec<RegistryFriendlyByteBuf, R> registry(final ResourceKey<? extends Registry<T>> registryKey, final Function<Registry<T>, IdMap<R>> mapExtractor) {
        return new StreamCodec<RegistryFriendlyByteBuf, R>(){

            private IdMap<R> getRegistryOrThrow(RegistryFriendlyByteBuf input) {
                return (IdMap)mapExtractor.apply(input.registryAccess().lookupOrThrow(registryKey));
            }

            @Override
            public R decode(RegistryFriendlyByteBuf input) {
                int id = VarInt.read(input);
                return this.getRegistryOrThrow(input).byIdOrThrow(id);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf output, R value) {
                int id = this.getRegistryOrThrow(output).getIdOrThrow(value);
                VarInt.write(output, id);
            }
        };
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> registry(ResourceKey<? extends Registry<T>> registryKey) {
        return ByteBufCodecs.registry(registryKey, r -> r);
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderRegistry(ResourceKey<? extends Registry<T>> registryKey) {
        return ByteBufCodecs.registry(registryKey, Registry::asHolderIdMap);
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holder(final ResourceKey<? extends Registry<T>> registryKey, final StreamCodec<? super RegistryFriendlyByteBuf, T> directCodec) {
        return new StreamCodec<RegistryFriendlyByteBuf, Holder<T>>(){
            private static final int DIRECT_HOLDER_ID = 0;

            private IdMap<Holder<T>> getRegistryOrThrow(RegistryFriendlyByteBuf input) {
                return input.registryAccess().lookupOrThrow(registryKey).asHolderIdMap();
            }

            @Override
            public Holder<T> decode(RegistryFriendlyByteBuf input) {
                int id = VarInt.read(input);
                if (id == 0) {
                    return Holder.direct(directCodec.decode(input));
                }
                return this.getRegistryOrThrow(input).byIdOrThrow(id - 1);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf output, Holder<T> holder) {
                switch (holder.kind()) {
                    case REFERENCE: {
                        int id = this.getRegistryOrThrow(output).getIdOrThrow(holder);
                        VarInt.write(output, id + 1);
                        break;
                    }
                    case DIRECT: {
                        VarInt.write(output, 0);
                        directCodec.encode(output, holder.value());
                    }
                }
            }
        };
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>> holderSet(final ResourceKey<? extends Registry<T>> registryKey) {
        return new StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>>(){
            private static final int NAMED_SET = -1;
            private final StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderCodec;
            {
                this.holderCodec = ByteBufCodecs.holderRegistry(registryKey);
            }

            @Override
            public HolderSet<T> decode(RegistryFriendlyByteBuf input) {
                int count = VarInt.read(input) - 1;
                if (count == -1) {
                    HolderLookup.RegistryLookup registry = input.registryAccess().lookupOrThrow(registryKey);
                    return (HolderSet)registry.get(TagKey.create(registryKey, (Identifier)Identifier.STREAM_CODEC.decode(input))).orElseThrow();
                }
                ArrayList<Holder> holders = new ArrayList<Holder>(Math.min(count, 65536));
                for (int i = 0; i < count; ++i) {
                    holders.add((Holder)this.holderCodec.decode(input));
                }
                return HolderSet.direct(holders);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf output, HolderSet<T> value) {
                Optional key = value.unwrapKey();
                if (key.isPresent()) {
                    VarInt.write(output, 0);
                    Identifier.STREAM_CODEC.encode(output, key.get().location());
                } else {
                    VarInt.write(output, value.size() + 1);
                    for (Holder holder : value) {
                        this.holderCodec.encode(output, holder);
                    }
                }
            }
        };
    }

    public static StreamCodec<ByteBuf, JsonElement> lenientJson(final int maxStringLength) {
        return new StreamCodec<ByteBuf, JsonElement>(){
            private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

            @Override
            public JsonElement decode(ByteBuf input) {
                String payload = Utf8String.read(input, maxStringLength);
                try {
                    return LenientJsonParser.parse(payload);
                }
                catch (JsonSyntaxException e) {
                    throw new DecoderException("Failed to parse JSON", (Throwable)e);
                }
            }

            @Override
            public void encode(ByteBuf output, JsonElement value) {
                String payload = GSON.toJson(value);
                Utf8String.write(output, payload, maxStringLength);
            }
        };
    }
}

