/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

public record ClientboundRecipeBookRemovePacket(List<RecipeDisplayId> recipes) implements Packet<ClientGamePacketListener>
{
    public static final StreamCodec<ByteBuf, ClientboundRecipeBookRemovePacket> STREAM_CODEC = StreamCodec.composite(RecipeDisplayId.STREAM_CODEC.apply(ByteBufCodecs.list()), ClientboundRecipeBookRemovePacket::recipes, ClientboundRecipeBookRemovePacket::new);

    @Override
    public PacketType<ClientboundRecipeBookRemovePacket> type() {
        return GamePacketTypes.CLIENTBOUND_RECIPE_BOOK_REMOVE;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleRecipeBookRemove(this);
    }
}

