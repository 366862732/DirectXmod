/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol.login;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.LoginPacketTypes;

public record ClientboundLoginFinishedPacket(GameProfile gameProfile, UUID sessionId) implements Packet<ClientLoginPacketListener>
{
    public static final StreamCodec<ByteBuf, ClientboundLoginFinishedPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.GAME_PROFILE, ClientboundLoginFinishedPacket::gameProfile, UUIDUtil.STREAM_CODEC, ClientboundLoginFinishedPacket::sessionId, ClientboundLoginFinishedPacket::new);

    @Override
    public PacketType<ClientboundLoginFinishedPacket> type() {
        return LoginPacketTypes.CLIENTBOUND_LOGIN_FINISHED;
    }

    @Override
    public void handle(ClientLoginPacketListener listener) {
        listener.handleLoginFinished(this);
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}

