/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

public class ClientboundStopSoundPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundStopSoundPacket> STREAM_CODEC = Packet.codec(ClientboundStopSoundPacket::write, ClientboundStopSoundPacket::new);
    private static final int HAS_SOURCE = 1;
    private static final int HAS_SOUND = 2;
    private final @Nullable Identifier name;
    private final @Nullable SoundSource source;

    public ClientboundStopSoundPacket(@Nullable Identifier name, @Nullable SoundSource source) {
        this.name = name;
        this.source = source;
    }

    private ClientboundStopSoundPacket(FriendlyByteBuf input) {
        byte flags = input.readByte();
        this.source = (flags & 1) > 0 ? input.readEnum(SoundSource.class) : null;
        this.name = (flags & 2) > 0 ? input.readIdentifier() : null;
    }

    private void write(FriendlyByteBuf output) {
        if (this.source != null) {
            if (this.name != null) {
                output.writeByte(3);
                output.writeEnum(this.source);
                output.writeIdentifier(this.name);
            } else {
                output.writeByte(1);
                output.writeEnum(this.source);
            }
        } else if (this.name != null) {
            output.writeByte(2);
            output.writeIdentifier(this.name);
        } else {
            output.writeByte(0);
        }
    }

    @Override
    public PacketType<ClientboundStopSoundPacket> type() {
        return GamePacketTypes.CLIENTBOUND_STOP_SOUND;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleStopSoundEvent(this);
    }

    public @Nullable Identifier getName() {
        return this.name;
    }

    public @Nullable SoundSource getSource() {
        return this.source;
    }
}

