/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ComparisonChain
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.gson.annotations.JsonAdapter
 *  com.google.gson.annotations.SerializedName
 *  com.mojang.logging.LogUtils
 *  com.mojang.util.UUIDTypeAdapter
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.builder.EqualsBuilder
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.Exclude;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import com.mojang.realmsclient.dto.RegionSelectionPreferenceDto;
import com.mojang.realmsclient.dto.ValueObject;
import com.mojang.util.UUIDTypeAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsServer
extends ValueObject
implements ReflectionBasedSerialization {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NO_VALUE = -1;
    public static final Component WORLD_CLOSED_COMPONENT = Component.translatable("mco.play.button.realm.closed");
    @SerializedName(value="id")
    public long id = -1L;
    @SerializedName(value="remoteSubscriptionId")
    public @Nullable String remoteSubscriptionId;
    @SerializedName(value="name")
    public @Nullable String name;
    @SerializedName(value="motd")
    public String motd = "";
    @SerializedName(value="state")
    public State state = State.CLOSED;
    @SerializedName(value="owner")
    public @Nullable String owner;
    @SerializedName(value="ownerUUID")
    @JsonAdapter(value=UUIDTypeAdapter.class)
    public UUID ownerUUID = Util.NIL_UUID;
    @SerializedName(value="players")
    public List<PlayerInfo> players = Lists.newArrayList();
    @SerializedName(value="slots")
    private List<RealmsSlot> slotList = RealmsServer.createEmptySlots();
    @Exclude
    public Map<Integer, RealmsSlot> slots = new HashMap<Integer, RealmsSlot>();
    @SerializedName(value="expired")
    public boolean expired;
    @SerializedName(value="expiredTrial")
    public boolean expiredTrial = false;
    @SerializedName(value="daysLeft")
    public int daysLeft;
    @SerializedName(value="worldType")
    public WorldType worldType = WorldType.NORMAL;
    @SerializedName(value="isHardcore")
    public boolean isHardcore = false;
    @SerializedName(value="gameMode")
    public int gameMode = -1;
    @SerializedName(value="activeSlot")
    public int activeSlot = -1;
    @SerializedName(value="minigameName")
    public @Nullable String minigameName;
    @SerializedName(value="minigameId")
    public int minigameId = -1;
    @SerializedName(value="minigameImage")
    public @Nullable String minigameImage;
    @SerializedName(value="parentWorldId")
    public long parentRealmId = -1L;
    @SerializedName(value="parentWorldName")
    public @Nullable String parentWorldName;
    @SerializedName(value="activeVersion")
    public String activeVersion = "";
    @SerializedName(value="compatibility")
    public Compatibility compatibility = Compatibility.UNVERIFIABLE;
    @SerializedName(value="regionSelectionPreference")
    public @Nullable RegionSelectionPreferenceDto regionSelectionPreference;

    public String getDescription() {
        return this.motd;
    }

    public @Nullable String getName() {
        return this.name;
    }

    public @Nullable String getMinigameName() {
        return this.minigameName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String motd) {
        this.motd = motd;
    }

    public static RealmsServer parse(GuardedSerializer gson, String json) {
        try {
            RealmsServer server = gson.fromJson(json, RealmsServer.class);
            if (server == null) {
                LOGGER.error("Could not parse McoServer: {}", (Object)json);
                return new RealmsServer();
            }
            RealmsServer.finalize(server);
            return server;
        }
        catch (Exception e) {
            LOGGER.error("Could not parse McoServer", (Throwable)e);
            return new RealmsServer();
        }
    }

    public static void finalize(RealmsServer server) {
        if (server.players == null) {
            server.players = Lists.newArrayList();
        }
        if (server.slotList == null) {
            server.slotList = RealmsServer.createEmptySlots();
        }
        if (server.slots == null) {
            server.slots = new HashMap<Integer, RealmsSlot>();
        }
        if (server.worldType == null) {
            server.worldType = WorldType.NORMAL;
        }
        if (server.activeVersion == null) {
            server.activeVersion = "";
        }
        if (server.compatibility == null) {
            server.compatibility = Compatibility.UNVERIFIABLE;
        }
        if (server.regionSelectionPreference == null) {
            server.regionSelectionPreference = RegionSelectionPreferenceDto.DEFAULT;
        }
        RealmsServer.sortInvited(server);
        RealmsServer.finalizeSlots(server);
    }

    private static void sortInvited(RealmsServer server) {
        server.players.sort((o1, o2) -> ComparisonChain.start().compareFalseFirst(o2.accepted, o1.accepted).compare((Comparable)((Object)o1.name.toLowerCase(Locale.ROOT)), (Comparable)((Object)o2.name.toLowerCase(Locale.ROOT))).result());
    }

    private static void finalizeSlots(RealmsServer server) {
        server.slotList.forEach(s -> server.slots.put(s.slotId, (RealmsSlot)s));
        for (int i = 1; i <= 3; ++i) {
            if (server.slots.containsKey(i)) continue;
            server.slots.put(i, RealmsSlot.defaults(i));
        }
    }

    private static List<RealmsSlot> createEmptySlots() {
        ArrayList<RealmsSlot> slots = new ArrayList<RealmsSlot>();
        slots.add(RealmsSlot.defaults(1));
        slots.add(RealmsSlot.defaults(2));
        slots.add(RealmsSlot.defaults(3));
        return slots;
    }

    public boolean isCompatible() {
        return this.compatibility.isCompatible();
    }

    public boolean needsUpgrade() {
        return this.compatibility.needsUpgrade();
    }

    public boolean needsDowngrade() {
        return this.compatibility.needsDowngrade();
    }

    public boolean shouldPlayButtonBeActive() {
        boolean active = !this.expired && this.state == State.OPEN;
        return active && (this.isCompatible() || this.needsUpgrade() || this.isSelfOwnedServer());
    }

    private boolean isSelfOwnedServer() {
        return Minecraft.getInstance().isLocalPlayer(this.ownerUUID);
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.id, this.name, this.motd, this.state, this.owner, this.expired});
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        RealmsServer rhs = (RealmsServer)obj;
        return new EqualsBuilder().append(this.id, rhs.id).append((Object)this.name, (Object)rhs.name).append((Object)this.motd, (Object)rhs.motd).append((Object)this.state, (Object)rhs.state).append((Object)this.owner, (Object)rhs.owner).append(this.expired, rhs.expired).append((Object)this.worldType, (Object)this.worldType).isEquals();
    }

    public RealmsServer copy() {
        RealmsServer server = new RealmsServer();
        server.id = this.id;
        server.remoteSubscriptionId = this.remoteSubscriptionId;
        server.name = this.name;
        server.motd = this.motd;
        server.state = this.state;
        server.owner = this.owner;
        server.players = this.players;
        server.slotList = this.slotList.stream().map(RealmsSlot::copy).toList();
        server.slots = this.cloneSlots(this.slots);
        server.expired = this.expired;
        server.expiredTrial = this.expiredTrial;
        server.daysLeft = this.daysLeft;
        server.worldType = this.worldType;
        server.isHardcore = this.isHardcore;
        server.gameMode = this.gameMode;
        server.ownerUUID = this.ownerUUID;
        server.minigameName = this.minigameName;
        server.activeSlot = this.activeSlot;
        server.minigameId = this.minigameId;
        server.minigameImage = this.minigameImage;
        server.parentWorldName = this.parentWorldName;
        server.parentRealmId = this.parentRealmId;
        server.activeVersion = this.activeVersion;
        server.compatibility = this.compatibility;
        server.regionSelectionPreference = this.regionSelectionPreference != null ? this.regionSelectionPreference.copy() : null;
        return server;
    }

    public Map<Integer, RealmsSlot> cloneSlots(Map<Integer, RealmsSlot> slots) {
        HashMap newSlots = Maps.newHashMap();
        for (Map.Entry<Integer, RealmsSlot> entry : slots.entrySet()) {
            newSlots.put(entry.getKey(), new RealmsSlot(entry.getKey(), entry.getValue().options.copy(), entry.getValue().settings));
        }
        return newSlots;
    }

    public boolean isSnapshotRealm() {
        return this.parentRealmId != -1L;
    }

    public boolean isMinigameActive() {
        return this.worldType == WorldType.MINIGAME;
    }

    public String getWorldName(int slotId) {
        if (this.name == null) {
            return this.slots.get((Object)Integer.valueOf((int)slotId)).options.getSlotName(slotId);
        }
        return this.name + " (" + this.slots.get((Object)Integer.valueOf((int)slotId)).options.getSlotName(slotId) + ")";
    }

    public ServerData toServerData(String ip) {
        return new ServerData(Objects.requireNonNullElse(this.name, "unknown server"), ip, ServerData.Type.REALM);
    }

    @Environment(value=EnvType.CLIENT)
    public static enum State {
        CLOSED,
        OPEN,
        UNINITIALIZED;

    }

    @Environment(value=EnvType.CLIENT)
    public static enum WorldType {
        NORMAL("normal"),
        MINIGAME("minigame"),
        ADVENTUREMAP("adventureMap"),
        EXPERIENCE("experience"),
        INSPIRATION("inspiration"),
        UNKNOWN("unknown");

        private static final String TRANSLATION_PREFIX = "mco.backup.entry.worldType.";
        private final Component displayName;

        private WorldType(String translationKey) {
            this.displayName = Component.translatable(TRANSLATION_PREFIX + translationKey);
        }

        public Component getDisplayName() {
            return this.displayName;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Compatibility {
        UNVERIFIABLE,
        INCOMPATIBLE,
        RELEASE_TYPE_INCOMPATIBLE,
        NEEDS_DOWNGRADE,
        NEEDS_UPGRADE,
        COMPATIBLE;


        public boolean isCompatible() {
            return this == COMPATIBLE;
        }

        public boolean needsUpgrade() {
            return this == NEEDS_UPGRADE;
        }

        public boolean needsDowngrade() {
            return this == NEEDS_DOWNGRADE;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class McoServerComparator
    implements Comparator<RealmsServer> {
        private final String refOwner;

        public McoServerComparator(String owner) {
            this.refOwner = owner;
        }

        @Override
        public int compare(RealmsServer server1, RealmsServer server2) {
            return ComparisonChain.start().compareTrueFirst(server1.isSnapshotRealm(), server2.isSnapshotRealm()).compareTrueFirst(server1.state == State.UNINITIALIZED, server2.state == State.UNINITIALIZED).compareTrueFirst(server1.expiredTrial, server2.expiredTrial).compareTrueFirst(Objects.equals(server1.owner, this.refOwner), Objects.equals(server2.owner, this.refOwner)).compareFalseFirst(server1.expired, server2.expired).compareTrueFirst(server1.state == State.OPEN, server2.state == State.OPEN).compare(server1.id, server2.id).result();
        }
    }
}

