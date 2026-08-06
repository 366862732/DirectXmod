/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 */
package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record ClientboundLoginPacket(int playerId, boolean hardcore, Set<ResourceKey<Level>> levels, int maxPlayers, int chunkRadius, int simulationDistance, boolean reducedDebugInfo, boolean showDeathScreen, boolean doLimitedCrafting, CommonPlayerSpawnInfo commonPlayerSpawnInfo, boolean onlineMode, boolean enforcesSecureChat) implements Packet<ClientGamePacketListener>
{
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLoginPacket> STREAM_CODEC = Packet.codec(ClientboundLoginPacket::write, ClientboundLoginPacket::new);

    private ClientboundLoginPacket(RegistryFriendlyByteBuf input) {
        this(input.readInt(), input.readBoolean(), input.readCollection(Sets::newHashSetWithExpectedSize, buf -> buf.readResourceKey(Registries.DIMENSION)), input.readVarInt(), input.readVarInt(), input.readVarInt(), input.readBoolean(), input.readBoolean(), input.readBoolean(), new CommonPlayerSpawnInfo(input), input.readBoolean(), input.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf output) {
        output.writeInt(this.playerId);
        output.writeBoolean(this.hardcore);
        output.writeCollection(this.levels, FriendlyByteBuf::writeResourceKey);
        output.writeVarInt(this.maxPlayers);
        output.writeVarInt(this.chunkRadius);
        output.writeVarInt(this.simulationDistance);
        output.writeBoolean(this.reducedDebugInfo);
        output.writeBoolean(this.showDeathScreen);
        output.writeBoolean(this.doLimitedCrafting);
        this.commonPlayerSpawnInfo.write(output);
        output.writeBoolean(this.onlineMode);
        output.writeBoolean(this.enforcesSecureChat);
    }

    @Override
    public PacketType<ClientboundLoginPacket> type() {
        return GamePacketTypes.CLIENTBOUND_LOGIN;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleLogin(this);
    }
}

