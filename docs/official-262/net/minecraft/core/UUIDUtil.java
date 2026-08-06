/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Sets
 *  com.mojang.authlib.GameProfile
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.Lifecycle
 *  com.mojang.util.UndashedUuid
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.mojang.util.UndashedUuid;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;

public final class UUIDUtil {
    public static final Codec<UUID> CODEC = Codec.INT_STREAM.comapFlatMap(list -> Util.fixedSize(list, 4).map(UUIDUtil::uuidFromIntArray), uuid -> Arrays.stream(UUIDUtil.uuidToIntArray(uuid)));
    public static final Codec<Set<UUID>> CODEC_SET = Codec.list(CODEC).xmap(Sets::newHashSet, Lists::newArrayList);
    public static final Codec<Set<UUID>> CODEC_LINKED_SET = Codec.list(CODEC).xmap(Sets::newLinkedHashSet, Lists::newArrayList);
    public static final Codec<UUID> STRING_CODEC = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success((Object)UUID.fromString(s), (Lifecycle)Lifecycle.stable());
        }
        catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Invalid UUID " + s + ": " + e.getMessage());
        }
    }, UUID::toString);
    public static final Codec<UUID> AUTHLIB_CODEC = Codec.withAlternative((Codec)Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success((Object)UndashedUuid.fromStringLenient((String)s), (Lifecycle)Lifecycle.stable());
        }
        catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Invalid UUID " + s + ": " + e.getMessage());
        }
    }, UndashedUuid::toString), CODEC);
    public static final Codec<UUID> LENIENT_CODEC = Codec.withAlternative(CODEC, STRING_CODEC);
    public static final StreamCodec<ByteBuf, UUID> STREAM_CODEC = new StreamCodec<ByteBuf, UUID>(){

        @Override
        public UUID decode(ByteBuf input) {
            return FriendlyByteBuf.readUUID(input);
        }

        @Override
        public void encode(ByteBuf output, UUID value) {
            FriendlyByteBuf.writeUUID(output, value);
        }
    };
    public static final int UUID_BYTES = 16;
    private static final String UUID_PREFIX_OFFLINE_PLAYER = "OfflinePlayer:";

    private UUIDUtil() {
    }

    public static UUID uuidFromIntArray(int[] intArray) {
        return new UUID((long)intArray[0] << 32 | (long)intArray[1] & 0xFFFFFFFFL, (long)intArray[2] << 32 | (long)intArray[3] & 0xFFFFFFFFL);
    }

    public static int[] uuidToIntArray(UUID uuid) {
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        return UUIDUtil.leastMostToIntArray(mostSignificantBits, leastSignificantBits);
    }

    private static int[] leastMostToIntArray(long mostSignificantBits, long leastSignificantBits) {
        return new int[]{(int)(mostSignificantBits >> 32), (int)mostSignificantBits, (int)(leastSignificantBits >> 32), (int)leastSignificantBits};
    }

    public static byte[] uuidToByteArray(UUID uuid) {
        byte[] bytes = new byte[16];
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits());
        return bytes;
    }

    public static UUID readUUID(Dynamic<?> input) {
        int[] intArray = input.asIntStream().toArray();
        if (intArray.length != 4) {
            throw new IllegalArgumentException("Could not read UUID. Expected int-array of length 4, got " + intArray.length + ".");
        }
        return UUIDUtil.uuidFromIntArray(intArray);
    }

    public static UUID createOfflinePlayerUUID(String playerName) {
        return UUID.nameUUIDFromBytes((UUID_PREFIX_OFFLINE_PLAYER + playerName).getBytes(StandardCharsets.UTF_8));
    }

    public static GameProfile createOfflineProfile(String playerName) {
        UUID id = UUIDUtil.createOfflinePlayerUUID(playerName);
        return new GameProfile(id, playerName);
    }
}

