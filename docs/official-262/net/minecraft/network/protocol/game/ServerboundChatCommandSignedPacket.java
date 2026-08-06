/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import java.time.Instant;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

public record ServerboundChatCommandSignedPacket(String command, Instant timeStamp, long salt, ArgumentSignatures argumentSignatures, LastSeenMessages.Update lastSeenMessages) implements Packet<ServerGamePacketListener>
{
    public static final StreamCodec<FriendlyByteBuf, ServerboundChatCommandSignedPacket> STREAM_CODEC = Packet.codec(ServerboundChatCommandSignedPacket::write, ServerboundChatCommandSignedPacket::new);

    private ServerboundChatCommandSignedPacket(FriendlyByteBuf input) {
        this(input.readUtf(), input.readInstant(), input.readLong(), new ArgumentSignatures(input), new LastSeenMessages.Update(input));
    }

    private void write(FriendlyByteBuf output) {
        output.writeUtf(this.command);
        output.writeInstant(this.timeStamp);
        output.writeLong(this.salt);
        this.argumentSignatures.write(output);
        this.lastSeenMessages.write(output);
    }

    @Override
    public PacketType<ServerboundChatCommandSignedPacket> type() {
        return GamePacketTypes.SERVERBOUND_CHAT_COMMAND_SIGNED;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleSignedChatCommand(this);
    }
}

