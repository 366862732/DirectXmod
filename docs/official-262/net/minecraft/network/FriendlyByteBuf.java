/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.ByteBufAllocator
 *  io.netty.buffer.ByteBufInputStream
 *  io.netty.buffer.ByteBufOutputStream
 *  io.netty.handler.codec.DecoderException
 *  io.netty.handler.codec.EncoderException
 *  io.netty.util.ByteProcessor
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntList
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Utf8String;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class FriendlyByteBuf
extends ByteBuf {
    private final ByteBuf source;
    public static final short MAX_STRING_LENGTH = Short.MAX_VALUE;
    public static final int MAX_COMPONENT_STRING_LENGTH = 262144;
    private static final int PUBLIC_KEY_SIZE = 256;
    private static final int MAX_PUBLIC_KEY_HEADER_SIZE = 256;
    private static final int MAX_PUBLIC_KEY_LENGTH = 512;
    private static final Gson GSON = new Gson();

    public FriendlyByteBuf(ByteBuf source) {
        this.source = source;
    }

    @Deprecated
    public <T> T readWithCodecTrusted(DynamicOps<Tag> ops, Codec<T> codec) {
        return this.readWithCodec(ops, codec, NbtAccounter.unlimitedHeap());
    }

    @Deprecated
    public <T> T readWithCodec(DynamicOps<Tag> ops, Codec<T> codec, NbtAccounter accounter) {
        Tag tag = this.readNbt(accounter);
        return (T)codec.parse(ops, (Object)tag).getOrThrow(msg -> new DecoderException("Failed to decode: " + msg + " " + String.valueOf(tag)));
    }

    @Deprecated
    public <T> FriendlyByteBuf writeWithCodec(DynamicOps<Tag> ops, Codec<T> codec, T value) {
        Tag tag = (Tag)codec.encodeStart(ops, value).getOrThrow(msg -> new EncoderException("Failed to encode: " + msg + " " + String.valueOf(value)));
        this.writeNbt(tag);
        return this;
    }

    public <T> T readLenientJsonWithCodec(Codec<T> codec) {
        JsonElement json = LenientJsonParser.parse(this.readUtf());
        DataResult result = codec.parse((DynamicOps)JsonOps.INSTANCE, (Object)json);
        return (T)result.getOrThrow(error -> new DecoderException("Failed to decode JSON: " + error));
    }

    public <T> void writeJsonWithCodec(Codec<T> codec, T value) {
        DataResult result = codec.encodeStart((DynamicOps)JsonOps.INSTANCE, value);
        this.writeUtf(GSON.toJson((JsonElement)result.getOrThrow(error -> new EncoderException("Failed to encode: " + error + " " + String.valueOf(value)))));
    }

    public static <T> IntFunction<T> limitValue(IntFunction<T> original, int limit) {
        return value -> {
            if (value > limit) {
                throw new DecoderException("Value " + value + " is larger than limit " + limit);
            }
            return original.apply(value);
        };
    }

    public <T, C extends Collection<T>> C readCollection(IntFunction<C> ctor, StreamDecoder<? super FriendlyByteBuf, T> elementDecoder) {
        int count = this.readVarInt();
        Collection result = (Collection)ctor.apply(count);
        for (int i = 0; i < count; ++i) {
            result.add(elementDecoder.decode(this));
        }
        return (C)result;
    }

    public <T> void writeCollection(Collection<T> collection, StreamEncoder<? super FriendlyByteBuf, T> encoder) {
        this.writeVarInt(collection.size());
        for (T element : collection) {
            encoder.encode(this, element);
        }
    }

    public <T> List<T> readList(StreamDecoder<? super FriendlyByteBuf, T> elementDecoder) {
        return this.readCollection(Lists::newArrayListWithCapacity, elementDecoder);
    }

    public IntList readIntIdList() {
        int count = this.readVarInt();
        IntArrayList result = new IntArrayList();
        for (int i = 0; i < count; ++i) {
            result.add(this.readVarInt());
        }
        return result;
    }

    public void writeIntIdList(IntList ids) {
        this.writeVarInt(ids.size());
        ids.forEach(this::writeVarInt);
    }

    public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> ctor, StreamDecoder<? super FriendlyByteBuf, K> keyDecoder, StreamDecoder<? super FriendlyByteBuf, V> valueDecoder) {
        int count = this.readVarInt();
        Map result = (Map)ctor.apply(count);
        for (int i = 0; i < count; ++i) {
            K key = keyDecoder.decode(this);
            V value = valueDecoder.decode(this);
            result.put(key, value);
        }
        return (M)result;
    }

    public <K, V> Map<K, V> readMap(StreamDecoder<? super FriendlyByteBuf, K> keyDecoder, StreamDecoder<? super FriendlyByteBuf, V> valueDecoder) {
        return this.readMap(Maps::newHashMapWithExpectedSize, keyDecoder, valueDecoder);
    }

    public <K, V> void writeMap(Map<K, V> map, StreamEncoder<? super FriendlyByteBuf, K> keyEncoder, StreamEncoder<? super FriendlyByteBuf, V> valueEncoder) {
        this.writeVarInt(map.size());
        map.forEach((k, v) -> {
            keyEncoder.encode(this, k);
            valueEncoder.encode(this, v);
        });
    }

    public void readWithCount(Consumer<FriendlyByteBuf> reader) {
        int count = this.readVarInt();
        for (int i = 0; i < count; ++i) {
            reader.accept(this);
        }
    }

    public <E extends Enum<E>> void writeEnumSet(EnumSet<E> set, Class<E> clazz) {
        Enum[] values = (Enum[])clazz.getEnumConstants();
        BitSet mask = new BitSet(values.length);
        for (int i = 0; i < values.length; ++i) {
            mask.set(i, set.contains(values[i]));
        }
        this.writeFixedBitSet(mask, values.length);
    }

    public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> clazz) {
        Enum[] values = (Enum[])clazz.getEnumConstants();
        BitSet mask = this.readFixedBitSet(values.length);
        EnumSet<Enum> result = EnumSet.noneOf(clazz);
        for (int i = 0; i < values.length; ++i) {
            if (!mask.get(i)) continue;
            result.add(values[i]);
        }
        return result;
    }

    public <T> void writeOptional(Optional<T> value, StreamEncoder<? super FriendlyByteBuf, T> valueWriter) {
        if (value.isPresent()) {
            this.writeBoolean(true);
            valueWriter.encode(this, value.get());
        } else {
            this.writeBoolean(false);
        }
    }

    public <T> Optional<T> readOptional(StreamDecoder<? super FriendlyByteBuf, T> valueReader) {
        if (this.readBoolean()) {
            return Optional.of(valueReader.decode(this));
        }
        return Optional.empty();
    }

    public <L, R> void writeEither(Either<L, R> value, StreamEncoder<? super FriendlyByteBuf, L> leftWriter, StreamEncoder<? super FriendlyByteBuf, R> rightWriter) {
        value.ifLeft(left -> {
            this.writeBoolean(true);
            leftWriter.encode(this, left);
        }).ifRight(right -> {
            this.writeBoolean(false);
            rightWriter.encode(this, right);
        });
    }

    public <L, R> Either<L, R> readEither(StreamDecoder<? super FriendlyByteBuf, L> leftReader, StreamDecoder<? super FriendlyByteBuf, R> rightReader) {
        if (this.readBoolean()) {
            return Either.left(leftReader.decode(this));
        }
        return Either.right(rightReader.decode(this));
    }

    public <T> @Nullable T readNullable(StreamDecoder<? super FriendlyByteBuf, T> valueDecoder) {
        return FriendlyByteBuf.readNullable(this, valueDecoder);
    }

    public static <T, B extends ByteBuf> @Nullable T readNullable(B input, StreamDecoder<? super B, T> valueDecoder) {
        if (input.readBoolean()) {
            return valueDecoder.decode(input);
        }
        return null;
    }

    public <T> void writeNullable(@Nullable T value, StreamEncoder<? super FriendlyByteBuf, T> valueEncoder) {
        FriendlyByteBuf.writeNullable(this, value, valueEncoder);
    }

    public static <T, B extends ByteBuf> void writeNullable(B output, @Nullable T value, StreamEncoder<? super B, T> valueEncoder) {
        if (value != null) {
            output.writeBoolean(true);
            valueEncoder.encode(output, value);
        } else {
            output.writeBoolean(false);
        }
    }

    public byte[] readByteArray() {
        return FriendlyByteBuf.readByteArray(this);
    }

    public static byte[] readByteArray(ByteBuf input) {
        return FriendlyByteBuf.readByteArray(input, input.readableBytes());
    }

    public FriendlyByteBuf writeByteArray(byte[] bytes) {
        FriendlyByteBuf.writeByteArray(this, bytes);
        return this;
    }

    public static void writeByteArray(ByteBuf output, byte[] bytes) {
        VarInt.write(output, bytes.length);
        output.writeBytes(bytes);
    }

    public byte[] readByteArray(int maxSize) {
        return FriendlyByteBuf.readByteArray(this, maxSize);
    }

    public static byte[] readByteArray(ByteBuf input, int maxSize) {
        int size = VarInt.read(input);
        if (size > maxSize) {
            throw new DecoderException("ByteArray with size " + size + " is bigger than allowed " + maxSize);
        }
        byte[] bytes = new byte[size];
        input.readBytes(bytes);
        return bytes;
    }

    public FriendlyByteBuf writeVarIntArray(int[] ints) {
        this.writeVarInt(ints.length);
        for (int i : ints) {
            this.writeVarInt(i);
        }
        return this;
    }

    public int[] readVarIntArray() {
        return this.readVarIntArray(this.readableBytes());
    }

    public int[] readVarIntArray(int maxSize) {
        int size = this.readVarInt();
        if (size > maxSize) {
            throw new DecoderException("VarIntArray with size " + size + " is bigger than allowed " + maxSize);
        }
        int[] ints = new int[size];
        for (int i = 0; i < ints.length; ++i) {
            ints[i] = this.readVarInt();
        }
        return ints;
    }

    public FriendlyByteBuf writeLongArray(long[] longs) {
        FriendlyByteBuf.writeLongArray(this, longs);
        return this;
    }

    public static void writeLongArray(ByteBuf output, long[] longs) {
        VarInt.write(output, longs.length);
        FriendlyByteBuf.writeFixedSizeLongArray(output, longs);
    }

    public FriendlyByteBuf writeFixedSizeLongArray(long[] longs) {
        FriendlyByteBuf.writeFixedSizeLongArray(this, longs);
        return this;
    }

    public static void writeFixedSizeLongArray(ByteBuf output, long[] longs) {
        for (long l : longs) {
            output.writeLong(l);
        }
    }

    public long[] readLongArray() {
        return FriendlyByteBuf.readLongArray(this);
    }

    public long[] readFixedSizeLongArray(long[] output) {
        return FriendlyByteBuf.readFixedSizeLongArray(this, output);
    }

    public static long[] readLongArray(ByteBuf input) {
        int maxSize;
        int size = VarInt.read(input);
        if (size > (maxSize = input.readableBytes() / 8)) {
            throw new DecoderException("LongArray with size " + size + " is bigger than allowed " + maxSize);
        }
        return FriendlyByteBuf.readFixedSizeLongArray(input, new long[size]);
    }

    public static long[] readFixedSizeLongArray(ByteBuf input, long[] output) {
        for (int i = 0; i < output.length; ++i) {
            output[i] = input.readLong();
        }
        return output;
    }

    public BlockPos readBlockPos() {
        return FriendlyByteBuf.readBlockPos(this);
    }

    public static BlockPos readBlockPos(ByteBuf input) {
        return BlockPos.of(input.readLong());
    }

    public FriendlyByteBuf writeBlockPos(BlockPos pos) {
        FriendlyByteBuf.writeBlockPos(this, pos);
        return this;
    }

    public static void writeBlockPos(ByteBuf output, BlockPos pos) {
        output.writeLong(pos.asLong());
    }

    public ChunkPos readChunkPos() {
        return ChunkPos.unpack(this.readLong());
    }

    public FriendlyByteBuf writeChunkPos(ChunkPos pos) {
        this.writeLong(pos.pack());
        return this;
    }

    public static ChunkPos readChunkPos(ByteBuf input) {
        return ChunkPos.unpack(input.readLong());
    }

    public static void writeChunkPos(ByteBuf output, ChunkPos chunkPos) {
        output.writeLong(chunkPos.pack());
    }

    public GlobalPos readGlobalPos() {
        ResourceKey<Level> dimension = this.readResourceKey(Registries.DIMENSION);
        BlockPos pos = this.readBlockPos();
        return GlobalPos.of(dimension, pos);
    }

    public void writeGlobalPos(GlobalPos globalPos) {
        this.writeResourceKey(globalPos.dimension());
        this.writeBlockPos(globalPos.pos());
    }

    public Vector3f readVector3f() {
        return FriendlyByteBuf.readVector3f(this);
    }

    public static Vector3f readVector3f(ByteBuf input) {
        return new Vector3f(input.readFloat(), input.readFloat(), input.readFloat());
    }

    public void writeVector3f(Vector3f v) {
        FriendlyByteBuf.writeVector3f(this, (Vector3fc)v);
    }

    public static void writeVector3f(ByteBuf output, Vector3fc v) {
        output.writeFloat(v.x());
        output.writeFloat(v.y());
        output.writeFloat(v.z());
    }

    public Quaternionf readQuaternion() {
        return FriendlyByteBuf.readQuaternion(this);
    }

    public static Quaternionf readQuaternion(ByteBuf input) {
        return new Quaternionf(input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat());
    }

    public void writeQuaternion(Quaternionf q) {
        FriendlyByteBuf.writeQuaternion(this, (Quaternionfc)q);
    }

    public static void writeQuaternion(ByteBuf output, Quaternionfc value) {
        output.writeFloat(value.x());
        output.writeFloat(value.y());
        output.writeFloat(value.z());
        output.writeFloat(value.w());
    }

    public <T extends Enum<T>> T readEnum(Class<T> clazz) {
        return (T)((Enum[])clazz.getEnumConstants())[this.readVarInt()];
    }

    public FriendlyByteBuf writeEnum(Enum<?> value) {
        return this.writeVarInt(value.ordinal());
    }

    public <T> T readById(IntFunction<T> converter) {
        int id = this.readVarInt();
        return converter.apply(id);
    }

    public <T> FriendlyByteBuf writeById(ToIntFunction<T> converter, T value) {
        int id = converter.applyAsInt(value);
        return this.writeVarInt(id);
    }

    public int readVarInt() {
        return VarInt.read(this.source);
    }

    public long readVarLong() {
        return VarLong.read(this.source);
    }

    public FriendlyByteBuf writeUUID(UUID uuid) {
        FriendlyByteBuf.writeUUID(this, uuid);
        return this;
    }

    public static void writeUUID(ByteBuf output, UUID uuid) {
        output.writeLong(uuid.getMostSignificantBits());
        output.writeLong(uuid.getLeastSignificantBits());
    }

    public UUID readUUID() {
        return FriendlyByteBuf.readUUID(this);
    }

    public static UUID readUUID(ByteBuf input) {
        return new UUID(input.readLong(), input.readLong());
    }

    public FriendlyByteBuf writeVarInt(int value) {
        VarInt.write(this.source, value);
        return this;
    }

    public FriendlyByteBuf writeVarLong(long value) {
        VarLong.write(this.source, value);
        return this;
    }

    public FriendlyByteBuf writeNbt(@Nullable Tag tag) {
        FriendlyByteBuf.writeNbt(this, tag);
        return this;
    }

    public static void writeNbt(ByteBuf output, @Nullable Tag tag) {
        if (tag == null) {
            tag = EndTag.INSTANCE;
        }
        try {
            NbtIo.writeAnyTag(tag, (DataOutput)new ByteBufOutputStream(output));
        }
        catch (IOException e) {
            throw new EncoderException((Throwable)e);
        }
    }

    public @Nullable CompoundTag readNbt() {
        return FriendlyByteBuf.readNbt(this);
    }

    public static @Nullable CompoundTag readNbt(ByteBuf input) {
        Tag result = FriendlyByteBuf.readNbt(input, NbtAccounter.defaultQuota());
        if (result == null || result instanceof CompoundTag) {
            return (CompoundTag)result;
        }
        throw new DecoderException("Not a compound tag: " + String.valueOf(result));
    }

    public static @Nullable Tag readNbt(ByteBuf input, NbtAccounter accounter) {
        try {
            Tag tag = NbtIo.readAnyTag((DataInput)new ByteBufInputStream(input), accounter);
            if (tag.getId() == 0) {
                return null;
            }
            return tag;
        }
        catch (IOException e) {
            throw new EncoderException((Throwable)e);
        }
    }

    public @Nullable Tag readNbt(NbtAccounter accounter) {
        return FriendlyByteBuf.readNbt(this, accounter);
    }

    public String readUtf() {
        return this.readUtf(Short.MAX_VALUE);
    }

    public String readUtf(int maxLength) {
        return Utf8String.read(this.source, maxLength);
    }

    public FriendlyByteBuf writeUtf(String value) {
        return this.writeUtf(value, Short.MAX_VALUE);
    }

    public FriendlyByteBuf writeUtf(String value, int maxLength) {
        Utf8String.write(this.source, value, maxLength);
        return this;
    }

    public Identifier readIdentifier() {
        return Identifier.parse(this.readUtf(Short.MAX_VALUE));
    }

    public FriendlyByteBuf writeIdentifier(Identifier identifier) {
        this.writeUtf(identifier.toString());
        return this;
    }

    public <T> ResourceKey<T> readResourceKey(ResourceKey<? extends Registry<T>> registry) {
        Identifier id = this.readIdentifier();
        return ResourceKey.create(registry, id);
    }

    public void writeResourceKey(ResourceKey<?> key) {
        this.writeIdentifier(key.identifier());
    }

    public <T> ResourceKey<? extends Registry<T>> readRegistryKey() {
        Identifier id = this.readIdentifier();
        return ResourceKey.createRegistryKey(id);
    }

    public Instant readInstant() {
        return Instant.ofEpochMilli(this.readLong());
    }

    public void writeInstant(Instant value) {
        this.writeLong(value.toEpochMilli());
    }

    public PublicKey readPublicKey() {
        try {
            return Crypt.byteToPublicKey(this.readByteArray(512));
        }
        catch (CryptException e) {
            throw new DecoderException("Malformed public key bytes", (Throwable)e);
        }
    }

    public FriendlyByteBuf writePublicKey(PublicKey publicKey) {
        this.writeByteArray(publicKey.getEncoded());
        return this;
    }

    public BlockHitResult readBlockHitResult() {
        BlockPos pos = this.readBlockPos();
        Direction face = this.readEnum(Direction.class);
        float clickX = this.readFloat();
        float clickY = this.readFloat();
        float clickZ = this.readFloat();
        boolean inside = this.readBoolean();
        boolean worldBorder = this.readBoolean();
        return new BlockHitResult(new Vec3((double)pos.getX() + (double)clickX, (double)pos.getY() + (double)clickY, (double)pos.getZ() + (double)clickZ), face, pos, inside, worldBorder);
    }

    public void writeBlockHitResult(BlockHitResult blockHit) {
        BlockPos blockPos = blockHit.getBlockPos();
        this.writeBlockPos(blockPos);
        this.writeEnum(blockHit.getDirection());
        Vec3 location = blockHit.getLocation();
        this.writeFloat((float)(location.x - (double)blockPos.getX()));
        this.writeFloat((float)(location.y - (double)blockPos.getY()));
        this.writeFloat((float)(location.z - (double)blockPos.getZ()));
        this.writeBoolean(blockHit.isInside());
        this.writeBoolean(blockHit.isWorldBorderHit());
    }

    public BitSet readBitSet() {
        return BitSet.valueOf(this.readLongArray());
    }

    public void writeBitSet(BitSet bitSet) {
        this.writeLongArray(bitSet.toLongArray());
    }

    public BitSet readFixedBitSet(int size) {
        byte[] bytes = new byte[Mth.positiveCeilDiv(size, 8)];
        this.readBytes(bytes);
        return BitSet.valueOf(bytes);
    }

    public void writeFixedBitSet(BitSet bitSet, int size) {
        if (bitSet.length() > size) {
            throw new EncoderException("BitSet is larger than expected size (" + bitSet.length() + ">" + size + ")");
        }
        byte[] bytes = bitSet.toByteArray();
        this.writeBytes(Arrays.copyOf(bytes, Mth.positiveCeilDiv(size, 8)));
    }

    public static int readContainerId(ByteBuf input) {
        return VarInt.read(input);
    }

    public int readContainerId() {
        return FriendlyByteBuf.readContainerId(this.source);
    }

    public static void writeContainerId(ByteBuf output, int id) {
        VarInt.write(output, id);
    }

    public void writeContainerId(int id) {
        FriendlyByteBuf.writeContainerId(this.source, id);
    }

    public boolean isContiguous() {
        return this.source.isContiguous();
    }

    public int maxFastWritableBytes() {
        return this.source.maxFastWritableBytes();
    }

    public int capacity() {
        return this.source.capacity();
    }

    public FriendlyByteBuf capacity(int newCapacity) {
        this.source.capacity(newCapacity);
        return this;
    }

    public int maxCapacity() {
        return this.source.maxCapacity();
    }

    public ByteBufAllocator alloc() {
        return this.source.alloc();
    }

    public ByteOrder order() {
        return this.source.order();
    }

    public ByteBuf order(ByteOrder endianness) {
        return this.source.order(endianness);
    }

    public ByteBuf unwrap() {
        return this.source;
    }

    public boolean isDirect() {
        return this.source.isDirect();
    }

    public boolean isReadOnly() {
        return this.source.isReadOnly();
    }

    public ByteBuf asReadOnly() {
        return this.source.asReadOnly();
    }

    public int readerIndex() {
        return this.source.readerIndex();
    }

    public FriendlyByteBuf readerIndex(int readerIndex) {
        this.source.readerIndex(readerIndex);
        return this;
    }

    public int writerIndex() {
        return this.source.writerIndex();
    }

    public FriendlyByteBuf writerIndex(int writerIndex) {
        this.source.writerIndex(writerIndex);
        return this;
    }

    public FriendlyByteBuf setIndex(int readerIndex, int writerIndex) {
        this.source.setIndex(readerIndex, writerIndex);
        return this;
    }

    public int readableBytes() {
        return this.source.readableBytes();
    }

    public int writableBytes() {
        return this.source.writableBytes();
    }

    public int maxWritableBytes() {
        return this.source.maxWritableBytes();
    }

    public boolean isReadable() {
        return this.source.isReadable();
    }

    public boolean isReadable(int size) {
        return this.source.isReadable(size);
    }

    public boolean isWritable() {
        return this.source.isWritable();
    }

    public boolean isWritable(int size) {
        return this.source.isWritable(size);
    }

    public FriendlyByteBuf clear() {
        this.source.clear();
        return this;
    }

    public FriendlyByteBuf markReaderIndex() {
        this.source.markReaderIndex();
        return this;
    }

    public FriendlyByteBuf resetReaderIndex() {
        this.source.resetReaderIndex();
        return this;
    }

    public FriendlyByteBuf markWriterIndex() {
        this.source.markWriterIndex();
        return this;
    }

    public FriendlyByteBuf resetWriterIndex() {
        this.source.resetWriterIndex();
        return this;
    }

    public FriendlyByteBuf discardReadBytes() {
        this.source.discardReadBytes();
        return this;
    }

    public FriendlyByteBuf discardSomeReadBytes() {
        this.source.discardSomeReadBytes();
        return this;
    }

    public FriendlyByteBuf ensureWritable(int minWritableBytes) {
        this.source.ensureWritable(minWritableBytes);
        return this;
    }

    public int ensureWritable(int minWritableBytes, boolean force) {
        return this.source.ensureWritable(minWritableBytes, force);
    }

    public boolean getBoolean(int index) {
        return this.source.getBoolean(index);
    }

    public byte getByte(int index) {
        return this.source.getByte(index);
    }

    public short getUnsignedByte(int index) {
        return this.source.getUnsignedByte(index);
    }

    public short getShort(int index) {
        return this.source.getShort(index);
    }

    public short getShortLE(int index) {
        return this.source.getShortLE(index);
    }

    public int getUnsignedShort(int index) {
        return this.source.getUnsignedShort(index);
    }

    public int getUnsignedShortLE(int index) {
        return this.source.getUnsignedShortLE(index);
    }

    public int getMedium(int index) {
        return this.source.getMedium(index);
    }

    public int getMediumLE(int index) {
        return this.source.getMediumLE(index);
    }

    public int getUnsignedMedium(int index) {
        return this.source.getUnsignedMedium(index);
    }

    public int getUnsignedMediumLE(int index) {
        return this.source.getUnsignedMediumLE(index);
    }

    public int getInt(int index) {
        return this.source.getInt(index);
    }

    public int getIntLE(int index) {
        return this.source.getIntLE(index);
    }

    public long getUnsignedInt(int index) {
        return this.source.getUnsignedInt(index);
    }

    public long getUnsignedIntLE(int index) {
        return this.source.getUnsignedIntLE(index);
    }

    public long getLong(int index) {
        return this.source.getLong(index);
    }

    public long getLongLE(int index) {
        return this.source.getLongLE(index);
    }

    public char getChar(int index) {
        return this.source.getChar(index);
    }

    public float getFloat(int index) {
        return this.source.getFloat(index);
    }

    public double getDouble(int index) {
        return this.source.getDouble(index);
    }

    public FriendlyByteBuf getBytes(int index, ByteBuf dst) {
        this.source.getBytes(index, dst);
        return this;
    }

    public FriendlyByteBuf getBytes(int index, ByteBuf dst, int length) {
        this.source.getBytes(index, dst, length);
        return this;
    }

    public FriendlyByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        this.source.getBytes(index, dst, dstIndex, length);
        return this;
    }

    public FriendlyByteBuf getBytes(int index, byte[] dst) {
        this.source.getBytes(index, dst);
        return this;
    }

    public FriendlyByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        this.source.getBytes(index, dst, dstIndex, length);
        return this;
    }

    public FriendlyByteBuf getBytes(int index, ByteBuffer dst) {
        this.source.getBytes(index, dst);
        return this;
    }

    public FriendlyByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        this.source.getBytes(index, out, length);
        return this;
    }

    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        return this.source.getBytes(index, out, length);
    }

    public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
        return this.source.getBytes(index, out, position, length);
    }

    public CharSequence getCharSequence(int index, int length, Charset charset) {
        return this.source.getCharSequence(index, length, charset);
    }

    public FriendlyByteBuf setBoolean(int index, boolean value) {
        this.source.setBoolean(index, value);
        return this;
    }

    public FriendlyByteBuf setByte(int index, int value) {
        this.source.setByte(index, value);
        return this;
    }

    public FriendlyByteBuf setShort(int index, int value) {
        this.source.setShort(index, value);
        return this;
    }

    public FriendlyByteBuf setShortLE(int index, int value) {
        this.source.setShortLE(index, value);
        return this;
    }

    public FriendlyByteBuf setMedium(int index, int value) {
        this.source.setMedium(index, value);
        return this;
    }

    public FriendlyByteBuf setMediumLE(int index, int value) {
        this.source.setMediumLE(index, value);
        return this;
    }

    public FriendlyByteBuf setInt(int index, int value) {
        this.source.setInt(index, value);
        return this;
    }

    public FriendlyByteBuf setIntLE(int index, int value) {
        this.source.setIntLE(index, value);
        return this;
    }

    public FriendlyByteBuf setLong(int index, long value) {
        this.source.setLong(index, value);
        return this;
    }

    public FriendlyByteBuf setLongLE(int index, long value) {
        this.source.setLongLE(index, value);
        return this;
    }

    public FriendlyByteBuf setChar(int index, int value) {
        this.source.setChar(index, value);
        return this;
    }

    public FriendlyByteBuf setFloat(int index, float value) {
        this.source.setFloat(index, value);
        return this;
    }

    public FriendlyByteBuf setDouble(int index, double value) {
        this.source.setDouble(index, value);
        return this;
    }

    public FriendlyByteBuf setBytes(int index, ByteBuf src) {
        this.source.setBytes(index, src);
        return this;
    }

    public FriendlyByteBuf setBytes(int index, ByteBuf src, int length) {
        this.source.setBytes(index, src, length);
        return this;
    }

    public FriendlyByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        this.source.setBytes(index, src, srcIndex, length);
        return this;
    }

    public FriendlyByteBuf setBytes(int index, byte[] src) {
        this.source.setBytes(index, src);
        return this;
    }

    public FriendlyByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        this.source.setBytes(index, src, srcIndex, length);
        return this;
    }

    public FriendlyByteBuf setBytes(int index, ByteBuffer src) {
        this.source.setBytes(index, src);
        return this;
    }

    public int setBytes(int index, InputStream in, int length) throws IOException {
        return this.source.setBytes(index, in, length);
    }

    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        return this.source.setBytes(index, in, length);
    }

    public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
        return this.source.setBytes(index, in, position, length);
    }

    public FriendlyByteBuf setZero(int index, int length) {
        this.source.setZero(index, length);
        return this;
    }

    public int setCharSequence(int index, CharSequence sequence, Charset charset) {
        return this.source.setCharSequence(index, sequence, charset);
    }

    public boolean readBoolean() {
        return this.source.readBoolean();
    }

    public byte readByte() {
        return this.source.readByte();
    }

    public short readUnsignedByte() {
        return this.source.readUnsignedByte();
    }

    public short readShort() {
        return this.source.readShort();
    }

    public short readShortLE() {
        return this.source.readShortLE();
    }

    public int readUnsignedShort() {
        return this.source.readUnsignedShort();
    }

    public int readUnsignedShortLE() {
        return this.source.readUnsignedShortLE();
    }

    public int readMedium() {
        return this.source.readMedium();
    }

    public int readMediumLE() {
        return this.source.readMediumLE();
    }

    public int readUnsignedMedium() {
        return this.source.readUnsignedMedium();
    }

    public int readUnsignedMediumLE() {
        return this.source.readUnsignedMediumLE();
    }

    public int readInt() {
        return this.source.readInt();
    }

    public int readIntLE() {
        return this.source.readIntLE();
    }

    public long readUnsignedInt() {
        return this.source.readUnsignedInt();
    }

    public long readUnsignedIntLE() {
        return this.source.readUnsignedIntLE();
    }

    public long readLong() {
        return this.source.readLong();
    }

    public long readLongLE() {
        return this.source.readLongLE();
    }

    public char readChar() {
        return this.source.readChar();
    }

    public float readFloat() {
        return this.source.readFloat();
    }

    public double readDouble() {
        return this.source.readDouble();
    }

    public ByteBuf readBytes(int length) {
        return this.source.readBytes(length);
    }

    public ByteBuf readSlice(int length) {
        return this.source.readSlice(length);
    }

    public ByteBuf readRetainedSlice(int length) {
        return this.source.readRetainedSlice(length);
    }

    public FriendlyByteBuf readBytes(ByteBuf dst) {
        this.source.readBytes(dst);
        return this;
    }

    public FriendlyByteBuf readBytes(ByteBuf dst, int length) {
        this.source.readBytes(dst, length);
        return this;
    }

    public FriendlyByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
        this.source.readBytes(dst, dstIndex, length);
        return this;
    }

    public FriendlyByteBuf readBytes(byte[] dst) {
        this.source.readBytes(dst);
        return this;
    }

    public FriendlyByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        this.source.readBytes(dst, dstIndex, length);
        return this;
    }

    public FriendlyByteBuf readBytes(ByteBuffer dst) {
        this.source.readBytes(dst);
        return this;
    }

    public FriendlyByteBuf readBytes(OutputStream out, int length) throws IOException {
        this.source.readBytes(out, length);
        return this;
    }

    public int readBytes(GatheringByteChannel out, int length) throws IOException {
        return this.source.readBytes(out, length);
    }

    public CharSequence readCharSequence(int length, Charset charset) {
        return this.source.readCharSequence(length, charset);
    }

    public String readString(int length, Charset charset) {
        return this.source.readString(length, charset);
    }

    public int readBytes(FileChannel out, long position, int length) throws IOException {
        return this.source.readBytes(out, position, length);
    }

    public FriendlyByteBuf skipBytes(int length) {
        this.source.skipBytes(length);
        return this;
    }

    public FriendlyByteBuf writeBoolean(boolean value) {
        this.source.writeBoolean(value);
        return this;
    }

    public FriendlyByteBuf writeByte(int value) {
        this.source.writeByte(value);
        return this;
    }

    public FriendlyByteBuf writeShort(int value) {
        this.source.writeShort(value);
        return this;
    }

    public FriendlyByteBuf writeShortLE(int value) {
        this.source.writeShortLE(value);
        return this;
    }

    public FriendlyByteBuf writeMedium(int value) {
        this.source.writeMedium(value);
        return this;
    }

    public FriendlyByteBuf writeMediumLE(int value) {
        this.source.writeMediumLE(value);
        return this;
    }

    public FriendlyByteBuf writeInt(int value) {
        this.source.writeInt(value);
        return this;
    }

    public FriendlyByteBuf writeIntLE(int value) {
        this.source.writeIntLE(value);
        return this;
    }

    public FriendlyByteBuf writeLong(long value) {
        this.source.writeLong(value);
        return this;
    }

    public FriendlyByteBuf writeLongLE(long value) {
        this.source.writeLongLE(value);
        return this;
    }

    public FriendlyByteBuf writeChar(int value) {
        this.source.writeChar(value);
        return this;
    }

    public FriendlyByteBuf writeFloat(float value) {
        this.source.writeFloat(value);
        return this;
    }

    public FriendlyByteBuf writeDouble(double value) {
        this.source.writeDouble(value);
        return this;
    }

    public FriendlyByteBuf writeBytes(ByteBuf src) {
        this.source.writeBytes(src);
        return this;
    }

    public FriendlyByteBuf writeBytes(ByteBuf src, int length) {
        this.source.writeBytes(src, length);
        return this;
    }

    public FriendlyByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
        this.source.writeBytes(src, srcIndex, length);
        return this;
    }

    public FriendlyByteBuf writeBytes(byte[] src) {
        this.source.writeBytes(src);
        return this;
    }

    public FriendlyByteBuf writeBytes(byte[] src, int srcIndex, int length) {
        this.source.writeBytes(src, srcIndex, length);
        return this;
    }

    public FriendlyByteBuf writeBytes(ByteBuffer src) {
        this.source.writeBytes(src);
        return this;
    }

    public int writeBytes(InputStream in, int length) throws IOException {
        return this.source.writeBytes(in, length);
    }

    public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
        return this.source.writeBytes(in, length);
    }

    public int writeBytes(FileChannel in, long position, int length) throws IOException {
        return this.source.writeBytes(in, position, length);
    }

    public FriendlyByteBuf writeZero(int length) {
        this.source.writeZero(length);
        return this;
    }

    public int writeCharSequence(CharSequence sequence, Charset charset) {
        return this.source.writeCharSequence(sequence, charset);
    }

    public int indexOf(int fromIndex, int toIndex, byte value) {
        return this.source.indexOf(fromIndex, toIndex, value);
    }

    public int bytesBefore(byte value) {
        return this.source.bytesBefore(value);
    }

    public int bytesBefore(int length, byte value) {
        return this.source.bytesBefore(length, value);
    }

    public int bytesBefore(int index, int length, byte value) {
        return this.source.bytesBefore(index, length, value);
    }

    public int forEachByte(ByteProcessor processor) {
        return this.source.forEachByte(processor);
    }

    public int forEachByte(int index, int length, ByteProcessor processor) {
        return this.source.forEachByte(index, length, processor);
    }

    public int forEachByteDesc(ByteProcessor processor) {
        return this.source.forEachByteDesc(processor);
    }

    public int forEachByteDesc(int index, int length, ByteProcessor processor) {
        return this.source.forEachByteDesc(index, length, processor);
    }

    public ByteBuf copy() {
        return this.source.copy();
    }

    public ByteBuf copy(int index, int length) {
        return this.source.copy(index, length);
    }

    public ByteBuf slice() {
        return this.source.slice();
    }

    public ByteBuf retainedSlice() {
        return this.source.retainedSlice();
    }

    public ByteBuf slice(int index, int length) {
        return this.source.slice(index, length);
    }

    public ByteBuf retainedSlice(int index, int length) {
        return this.source.retainedSlice(index, length);
    }

    public ByteBuf duplicate() {
        return this.source.duplicate();
    }

    public ByteBuf retainedDuplicate() {
        return this.source.retainedDuplicate();
    }

    public int nioBufferCount() {
        return this.source.nioBufferCount();
    }

    public ByteBuffer nioBuffer() {
        return this.source.nioBuffer();
    }

    public ByteBuffer nioBuffer(int index, int length) {
        return this.source.nioBuffer(index, length);
    }

    public ByteBuffer internalNioBuffer(int index, int length) {
        return this.source.internalNioBuffer(index, length);
    }

    public ByteBuffer[] nioBuffers() {
        return this.source.nioBuffers();
    }

    public ByteBuffer[] nioBuffers(int index, int length) {
        return this.source.nioBuffers(index, length);
    }

    public boolean hasArray() {
        return this.source.hasArray();
    }

    public byte[] array() {
        return this.source.array();
    }

    public int arrayOffset() {
        return this.source.arrayOffset();
    }

    public boolean hasMemoryAddress() {
        return this.source.hasMemoryAddress();
    }

    public long memoryAddress() {
        return this.source.memoryAddress();
    }

    public String toString(Charset charset) {
        return this.source.toString(charset);
    }

    public String toString(int index, int length, Charset charset) {
        return this.source.toString(index, length, charset);
    }

    public int hashCode() {
        return this.source.hashCode();
    }

    public boolean equals(Object obj) {
        return this.source.equals(obj);
    }

    public int compareTo(ByteBuf buffer) {
        return this.source.compareTo(buffer);
    }

    public String toString() {
        return this.source.toString();
    }

    public FriendlyByteBuf retain(int increment) {
        this.source.retain(increment);
        return this;
    }

    public FriendlyByteBuf retain() {
        this.source.retain();
        return this;
    }

    public FriendlyByteBuf touch() {
        this.source.touch();
        return this;
    }

    public FriendlyByteBuf touch(Object hint) {
        this.source.touch(hint);
        return this;
    }

    public int refCnt() {
        return this.source.refCnt();
    }

    public boolean release() {
        return this.source.release();
    }

    public boolean release(int decrement) {
        return this.source.release(decrement);
    }
}

