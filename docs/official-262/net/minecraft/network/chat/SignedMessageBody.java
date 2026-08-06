/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.primitives.Ints
 *  com.google.common.primitives.Longs
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.network.chat;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.SignatureUpdater;

public record SignedMessageBody(String content, Instant timeStamp, long salt, LastSeenMessages lastSeen) {
    public static final MapCodec<SignedMessageBody> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.STRING.fieldOf("content").forGetter(SignedMessageBody::content), (App)ExtraCodecs.INSTANT_ISO8601.fieldOf("time_stamp").forGetter(SignedMessageBody::timeStamp), (App)Codec.LONG.fieldOf("salt").forGetter(SignedMessageBody::salt), (App)LastSeenMessages.CODEC.optionalFieldOf("last_seen", (Object)LastSeenMessages.EMPTY).forGetter(SignedMessageBody::lastSeen)).apply((Applicative)i, SignedMessageBody::new));

    public static SignedMessageBody unsigned(String content) {
        return new SignedMessageBody(content, Instant.now(), 0L, LastSeenMessages.EMPTY);
    }

    public void updateSignature(SignatureUpdater.Output output) throws SignatureException {
        output.update(Longs.toByteArray((long)this.salt));
        output.update(Longs.toByteArray((long)this.timeStamp.getEpochSecond()));
        byte[] contentBytes = this.content.getBytes(StandardCharsets.UTF_8);
        output.update(Ints.toByteArray((int)contentBytes.length));
        output.update(contentBytes);
        this.lastSeen.updateSignature(output);
    }

    public Packed pack(MessageSignatureCache cache) {
        return new Packed(this.content, this.timeStamp, this.salt, this.lastSeen.pack(cache));
    }

    public record Packed(String content, Instant timeStamp, long salt, LastSeenMessages.Packed lastSeen) {
        public Packed(FriendlyByteBuf input) {
            this(input.readUtf(256), input.readInstant(), input.readLong(), new LastSeenMessages.Packed(input));
        }

        public void write(FriendlyByteBuf output) {
            output.writeUtf(this.content, 256);
            output.writeInstant(this.timeStamp);
            output.writeLong(this.salt);
            this.lastSeen.write(output);
        }

        public Optional<SignedMessageBody> unpack(MessageSignatureCache cache) {
            return this.lastSeen.unpack(cache).map(lastSeen -> new SignedMessageBody(this.content, this.timeStamp, this.salt, (LastSeenMessages)lastSeen));
        }
    }
}

