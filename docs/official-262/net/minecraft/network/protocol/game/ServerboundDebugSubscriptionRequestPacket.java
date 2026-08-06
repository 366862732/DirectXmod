/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
 */
package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.util.debug.DebugSubscription;

public record ServerboundDebugSubscriptionRequestPacket(Set<DebugSubscription<?>> subscriptions) implements Packet<ServerGamePacketListener>
{
    private static final StreamCodec<RegistryFriendlyByteBuf, Set<DebugSubscription<?>>> SET_STREAM_CODEC = ByteBufCodecs.registry(Registries.DEBUG_SUBSCRIPTION).apply(ByteBufCodecs.collection(ReferenceOpenHashSet::new, 32));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDebugSubscriptionRequestPacket> STREAM_CODEC = SET_STREAM_CODEC.map(ServerboundDebugSubscriptionRequestPacket::new, ServerboundDebugSubscriptionRequestPacket::subscriptions);

    @Override
    public PacketType<ServerboundDebugSubscriptionRequestPacket> type() {
        return GamePacketTypes.SERVERBOUND_DEBUG_SUBSCRIPTION_REQUEST;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleDebugSubscriptionRequest(this);
    }
}

