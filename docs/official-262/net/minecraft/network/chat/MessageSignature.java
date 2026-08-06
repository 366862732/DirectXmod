/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.SignatureUpdater;
import net.minecraft.util.SignatureValidator;
import org.jspecify.annotations.Nullable;

public record MessageSignature(byte[] bytes) {
    public static final Codec<MessageSignature> CODEC = ExtraCodecs.BASE64_STRING.xmap(MessageSignature::new, MessageSignature::bytes);
    public static final int BYTES = 256;

    public MessageSignature {
        Preconditions.checkState((bytes.length == 256 ? 1 : 0) != 0, (Object)"Invalid message signature size");
    }

    public static MessageSignature read(FriendlyByteBuf input) {
        byte[] bytes = new byte[256];
        input.readBytes(bytes);
        return new MessageSignature(bytes);
    }

    public static void write(FriendlyByteBuf output, MessageSignature signature) {
        output.writeBytes(signature.bytes);
    }

    public boolean verify(SignatureValidator signature, SignatureUpdater updater) {
        return signature.validate(updater, this.bytes);
    }

    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(this.bytes);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageSignature)) return false;
        MessageSignature that = (MessageSignature)o;
        if (!Arrays.equals(this.bytes, that.bytes)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    @Override
    public String toString() {
        return Base64.getEncoder().encodeToString(this.bytes);
    }

    public static String describe(@Nullable MessageSignature signature) {
        if (signature == null) {
            return "<no signature>";
        }
        return signature.toString();
    }

    public Packed pack(MessageSignatureCache cache) {
        int packedId = cache.pack(this);
        return packedId != -1 ? new Packed(packedId) : new Packed(this);
    }

    public int checksum() {
        return Arrays.hashCode(this.bytes);
    }

    public record Packed(int id, @Nullable MessageSignature fullSignature) {
        public static final int FULL_SIGNATURE = -1;

        public Packed(MessageSignature signature) {
            this(-1, signature);
        }

        public Packed(int id) {
            this(id, null);
        }

        public static Packed read(FriendlyByteBuf input) {
            int id = input.readVarInt() - 1;
            if (id == -1) {
                return new Packed(MessageSignature.read(input));
            }
            return new Packed(id);
        }

        public static void write(FriendlyByteBuf output, Packed packed) {
            output.writeVarInt(packed.id() + 1);
            if (packed.fullSignature() != null) {
                MessageSignature.write(output, packed.fullSignature());
            }
        }

        public Optional<MessageSignature> unpack(MessageSignatureCache cache) {
            if (this.fullSignature != null) {
                return Optional.of(this.fullSignature);
            }
            return Optional.ofNullable(cache.unpack(this.id));
        }
    }
}

