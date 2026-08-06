/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol.game;

import java.time.Instant;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import org.jspecify.annotations.Nullable;

public record ServerboundChatPacket(String message, Instant timeStamp, long salt, @Nullable MessageSignature signature, LastSeenMessages.Update lastSeenMessages) implements Packet<ServerGamePacketListener>
{
    public static final StreamCodec<FriendlyByteBuf, ServerboundChatPacket> STREAM_CODEC = Packet.codec(ServerboundChatPacket::write, ServerboundChatPacket::new);

    private ServerboundChatPacket(FriendlyByteBuf input) {
        this(input.readUtf(256), input.readInstant(), input.readLong(), input.readNullable(MessageSignature::read), new LastSeenMessages.Update(input));
    }

    private void write(FriendlyByteBuf output) {
        output.writeUtf(this.message, 256);
        output.writeInstant(this.timeStamp);
        output.writeLong(this.salt);
        output.writeNullable(this.signature, MessageSignature::write);
        this.lastSeenMessages.write(output);
    }

    @Override
    public PacketType<ServerboundChatPacket> type() {
        return GamePacketTypes.SERVERBOUND_CHAT;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleChat(this);
    }
}

