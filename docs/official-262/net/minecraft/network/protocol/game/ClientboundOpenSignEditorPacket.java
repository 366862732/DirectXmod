/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;

public class ClientboundOpenSignEditorPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundOpenSignEditorPacket> STREAM_CODEC = Packet.codec(ClientboundOpenSignEditorPacket::write, ClientboundOpenSignEditorPacket::new);
    private final BlockPos pos;
    private final boolean isFrontText;

    public ClientboundOpenSignEditorPacket(BlockPos pos, boolean isFrontText) {
        this.pos = pos;
        this.isFrontText = isFrontText;
    }

    private ClientboundOpenSignEditorPacket(FriendlyByteBuf input) {
        this.pos = input.readBlockPos();
        this.isFrontText = input.readBoolean();
    }

    private void write(FriendlyByteBuf output) {
        output.writeBlockPos(this.pos);
        output.writeBoolean(this.isFrontText);
    }

    @Override
    public PacketType<ClientboundOpenSignEditorPacket> type() {
        return GamePacketTypes.CLIENTBOUND_OPEN_SIGN_EDITOR;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleOpenSignEditor(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public boolean isFrontText() {
        return this.isFrontText;
    }
}

