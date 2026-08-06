/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.world.effect.MobEffect;

public record ServerboundSetBeaconPacket(Optional<Holder<MobEffect>> primary, Optional<Holder<MobEffect>> secondary) implements Packet<ServerGamePacketListener>
{
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundSetBeaconPacket> STREAM_CODEC = StreamCodec.composite(MobEffect.STREAM_CODEC.apply(ByteBufCodecs::optional), ServerboundSetBeaconPacket::primary, MobEffect.STREAM_CODEC.apply(ByteBufCodecs::optional), ServerboundSetBeaconPacket::secondary, ServerboundSetBeaconPacket::new);

    @Override
    public PacketType<ServerboundSetBeaconPacket> type() {
        return GamePacketTypes.SERVERBOUND_SET_BEACON;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleSetBeaconPacket(this);
    }
}

