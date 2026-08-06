/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

public class ServerboundCommandSuggestionPacket
implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundCommandSuggestionPacket> STREAM_CODEC = Packet.codec(ServerboundCommandSuggestionPacket::write, ServerboundCommandSuggestionPacket::new);
    private final int id;
    private final String command;

    public ServerboundCommandSuggestionPacket(int id, String command) {
        this.id = id;
        this.command = command;
    }

    private ServerboundCommandSuggestionPacket(FriendlyByteBuf input) {
        this.id = input.readVarInt();
        this.command = input.readUtf(32500);
    }

    private void write(FriendlyByteBuf output) {
        output.writeVarInt(this.id);
        output.writeUtf(this.command, 32500);
    }

    @Override
    public PacketType<ServerboundCommandSuggestionPacket> type() {
        return GamePacketTypes.SERVERBOUND_COMMAND_SUGGESTION;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleCustomCommandSuggestions(this);
    }

    public int getId() {
        return this.id;
    }

    public String getCommand() {
        return this.command;
    }
}

