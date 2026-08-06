/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.properties.PropertyMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol.game;

import com.google.common.base.MoreObjects;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.Optionull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

public class ClientboundPlayerInfoUpdatePacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPlayerInfoUpdatePacket> STREAM_CODEC = Packet.codec(ClientboundPlayerInfoUpdatePacket::write, ClientboundPlayerInfoUpdatePacket::new);
    private final EnumSet<Action> actions;
    private final List<Entry> entries;

    public ClientboundPlayerInfoUpdatePacket(EnumSet<Action> actions, Collection<ServerPlayer> players) {
        this.actions = actions;
        this.entries = players.stream().map(Entry::new).toList();
    }

    public ClientboundPlayerInfoUpdatePacket(Action action, ServerPlayer player) {
        this.actions = EnumSet.of(action);
        this.entries = List.of(new Entry(player));
    }

    public static ClientboundPlayerInfoUpdatePacket createPlayerInitializing(Collection<ServerPlayer> players) {
        EnumSet<Action[]> actions = EnumSet.of(Action.ADD_PLAYER, new Action[]{Action.INITIALIZE_CHAT, Action.UPDATE_GAME_MODE, Action.UPDATE_LISTED, Action.UPDATE_LATENCY, Action.UPDATE_DISPLAY_NAME, Action.UPDATE_HAT, Action.UPDATE_LIST_ORDER});
        return new ClientboundPlayerInfoUpdatePacket(actions, players);
    }

    private ClientboundPlayerInfoUpdatePacket(RegistryFriendlyByteBuf input) {
        this.actions = input.readEnumSet(Action.class);
        this.entries = input.readList(buf -> {
            EntryBuilder builder = new EntryBuilder(buf.readUUID());
            for (Action action : this.actions) {
                action.reader.read(builder, (RegistryFriendlyByteBuf)((Object)buf));
            }
            return builder.build();
        });
    }

    private void write(RegistryFriendlyByteBuf output) {
        output.writeEnumSet(this.actions, Action.class);
        output.writeCollection(this.entries, (buf, entry) -> {
            buf.writeUUID(entry.profileId());
            for (Action action : this.actions) {
                action.writer.write((RegistryFriendlyByteBuf)((Object)buf), (Entry)entry);
            }
        });
    }

    @Override
    public PacketType<ClientboundPlayerInfoUpdatePacket> type() {
        return GamePacketTypes.CLIENTBOUND_PLAYER_INFO_UPDATE;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handlePlayerInfoUpdate(this);
    }

    public EnumSet<Action> actions() {
        return this.actions;
    }

    public List<Entry> entries() {
        return this.entries;
    }

    public List<Entry> newEntries() {
        return this.actions.contains((Object)Action.ADD_PLAYER) ? this.entries : List.of();
    }

    public String toString() {
        return MoreObjects.toStringHelper((Object)this).add("actions", this.actions).add("entries", this.entries).toString();
    }

    public record Entry(UUID profileId, @Nullable GameProfile profile, boolean listed, int latency, GameType gameMode, @Nullable Component displayName, boolean showHat, int listOrder, @Nullable RemoteChatSession.Data chatSession) {
        private Entry(ServerPlayer player) {
            this(player.getUUID(), player.getGameProfile(), true, player.connection.latency(), player.gameMode(), player.getTabListDisplayName(), player.isModelPartShown(PlayerModelPart.HAT), player.getTabListOrder(), Optionull.map(player.getChatSession(), RemoteChatSession::asData));
        }
    }

    public static enum Action {
        ADD_PLAYER((entry, input) -> {
            String name = (String)ByteBufCodecs.PLAYER_NAME.decode(input);
            PropertyMap properties = (PropertyMap)ByteBufCodecs.GAME_PROFILE_PROPERTIES.decode(input);
            entry.profile = new GameProfile(entry.profileId, name, properties);
        }, (output, entry) -> {
            GameProfile profile = Objects.requireNonNull(entry.profile());
            ByteBufCodecs.PLAYER_NAME.encode(output, profile.name());
            ByteBufCodecs.GAME_PROFILE_PROPERTIES.encode(output, profile.properties());
        }),
        INITIALIZE_CHAT((entry, input) -> {
            entry.chatSession = input.readNullable(RemoteChatSession.Data::read);
        }, (output, entry) -> output.writeNullable(entry.chatSession, RemoteChatSession.Data::write)),
        UPDATE_GAME_MODE((entry, input) -> {
            entry.gameMode = GameType.byId(input.readVarInt());
        }, (output, entry) -> output.writeVarInt(entry.gameMode().getId())),
        UPDATE_LISTED((entry, input) -> {
            entry.listed = input.readBoolean();
        }, (output, entry) -> output.writeBoolean(entry.listed())),
        UPDATE_LATENCY((entry, input) -> {
            entry.latency = input.readVarInt();
        }, (output, entry) -> output.writeVarInt(entry.latency())),
        UPDATE_DISPLAY_NAME((entry, input) -> {
            entry.displayName = FriendlyByteBuf.readNullable(input, ComponentSerialization.TRUSTED_STREAM_CODEC);
        }, (output, entry) -> FriendlyByteBuf.writeNullable(output, entry.displayName(), ComponentSerialization.TRUSTED_STREAM_CODEC)),
        UPDATE_LIST_ORDER((entry, input) -> {
            entry.listOrder = input.readVarInt();
        }, (output, entry) -> output.writeVarInt(entry.listOrder)),
        UPDATE_HAT((entry, input) -> {
            entry.showHat = input.readBoolean();
        }, (output, entry) -> output.writeBoolean(entry.showHat));

        private final Reader reader;
        private final Writer writer;

        private Action(Reader reader, Writer writer) {
            this.reader = reader;
            this.writer = writer;
        }

        public static interface Reader {
            public void read(EntryBuilder var1, RegistryFriendlyByteBuf var2);
        }

        public static interface Writer {
            public void write(RegistryFriendlyByteBuf var1, Entry var2);
        }
    }

    private static class EntryBuilder {
        private final UUID profileId;
        private @Nullable GameProfile profile;
        private boolean listed;
        private int latency;
        private GameType gameMode = GameType.DEFAULT_MODE;
        private @Nullable Component displayName;
        private boolean showHat;
        private int listOrder;
        private @Nullable RemoteChatSession.Data chatSession;

        private EntryBuilder(UUID profileId) {
            this.profileId = profileId;
        }

        private Entry build() {
            return new Entry(this.profileId, this.profile, this.listed, this.latency, this.gameMode, this.displayName, this.showHat, this.listOrder, this.chatSession);
        }
    }
}

