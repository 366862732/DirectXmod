/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol.game;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class ServerboundSeenAdvancementsPacket
implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundSeenAdvancementsPacket> STREAM_CODEC = Packet.codec(ServerboundSeenAdvancementsPacket::write, ServerboundSeenAdvancementsPacket::new);
    private final Action action;
    private final @Nullable Identifier tab;

    public ServerboundSeenAdvancementsPacket(Action action, @Nullable Identifier tab) {
        this.action = action;
        this.tab = tab;
    }

    public static ServerboundSeenAdvancementsPacket openedTab(AdvancementHolder tab) {
        return new ServerboundSeenAdvancementsPacket(Action.OPENED_TAB, tab.id());
    }

    public static ServerboundSeenAdvancementsPacket closedScreen() {
        return new ServerboundSeenAdvancementsPacket(Action.CLOSED_SCREEN, null);
    }

    private ServerboundSeenAdvancementsPacket(FriendlyByteBuf input) {
        this.action = input.readEnum(Action.class);
        this.tab = this.action == Action.OPENED_TAB ? input.readIdentifier() : null;
    }

    private void write(FriendlyByteBuf output) {
        output.writeEnum(this.action);
        if (this.action == Action.OPENED_TAB) {
            output.writeIdentifier(this.tab);
        }
    }

    @Override
    public PacketType<ServerboundSeenAdvancementsPacket> type() {
        return GamePacketTypes.SERVERBOUND_SEEN_ADVANCEMENTS;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleSeenAdvancements(this);
    }

    public Action getAction() {
        return this.action;
    }

    public @Nullable Identifier getTab() {
        return this.tab;
    }

    public static enum Action {
        OPENED_TAB,
        CLOSED_SCREEN;

    }
}

