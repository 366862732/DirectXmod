/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol.game;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public record ClientboundRemoveMobEffectPacket(int entityId, Holder<MobEffect> effect) implements Packet<ClientGamePacketListener>
{
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRemoveMobEffectPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ClientboundRemoveMobEffectPacket::entityId, MobEffect.STREAM_CODEC, ClientboundRemoveMobEffectPacket::effect, ClientboundRemoveMobEffectPacket::new);

    @Override
    public PacketType<ClientboundRemoveMobEffectPacket> type() {
        return GamePacketTypes.CLIENTBOUND_REMOVE_MOB_EFFECT;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleRemoveMobEffect(this);
    }

    public @Nullable Entity getEntity(Level level) {
        return level.getEntity(this.entityId);
    }
}

