/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.bossevents;

import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public class CustomBossEvent
extends ServerBossEvent {
    private static final int DEFAULT_MAX = 100;
    private final Identifier customId;
    private final Set<UUID> players = Sets.newHashSet();
    private int value;
    private int max = 100;
    private final Runnable dirtyCallback;

    public CustomBossEvent(UUID id, Identifier customId, Component name, Runnable dirtyCallback) {
        super(id, name, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
        this.dirtyCallback = dirtyCallback;
        this.customId = customId;
        this.setProgress(0.0f);
    }

    public Identifier customId() {
        return this.customId;
    }

    @Override
    public void addPlayer(ServerPlayer player) {
        super.addPlayer(player);
        if (this.players.add(player.getUUID())) {
            this.setDirty();
        }
    }

    @Override
    public void removePlayer(ServerPlayer player) {
        super.removePlayer(player);
        if (this.players.remove(player.getUUID())) {
            this.setDirty();
        }
    }

    @Override
    public void removeAllPlayers() {
        super.removeAllPlayers();
        if (!this.players.isEmpty()) {
            this.players.clear();
            this.setDirty();
        }
    }

    public int value() {
        return this.value;
    }

    public int max() {
        return this.max;
    }

    public void setValue(int value) {
        this.value = value;
        this.setProgress(Mth.clamp((float)value / (float)this.max, 0.0f, 1.0f));
        this.setDirty();
    }

    public void setMax(int max) {
        this.max = max;
        this.setProgress(Mth.clamp((float)this.value / (float)max, 0.0f, 1.0f));
        this.setDirty();
    }

    public final Component getDisplayName() {
        return ComponentUtils.wrapInSquareBrackets(this.getName()).withStyle(s -> s.withColor(this.getColor().getFormatting()).withHoverEvent(new HoverEvent.ShowText(Component.literal(this.customId().toString()))).withInsertion(this.customId().toString()));
    }

    public boolean setPlayers(Collection<ServerPlayer> players) {
        boolean playersChanged;
        boolean found;
        HashSet toRemove = Sets.newHashSet();
        HashSet toAdd = Sets.newHashSet();
        for (UUID uuid : this.players) {
            found = false;
            for (ServerPlayer player : players) {
                if (!player.getUUID().equals(uuid)) continue;
                found = true;
                break;
            }
            if (found) continue;
            toRemove.add(uuid);
        }
        for (ServerPlayer player : players) {
            found = false;
            for (UUID uuid : this.players) {
                if (!player.getUUID().equals(uuid)) continue;
                found = true;
                break;
            }
            if (found) continue;
            toAdd.add(player);
        }
        for (UUID uuid : toRemove) {
            for (ServerPlayer player : this.getPlayers()) {
                if (!player.getUUID().equals(uuid)) continue;
                this.removePlayer(player);
                break;
            }
            this.players.remove(uuid);
        }
        for (ServerPlayer player : toAdd) {
            this.addPlayer(player);
        }
        boolean bl = playersChanged = !toRemove.isEmpty() || !toAdd.isEmpty();
        if (playersChanged) {
            this.setDirty();
        }
        return playersChanged;
    }

    public static CustomBossEvent load(UUID id, Identifier customId, Packed packed, Runnable setDirty) {
        CustomBossEvent event = new CustomBossEvent(id, customId, packed.name, setDirty);
        event.setVisible(packed.visible);
        event.setValue(packed.value);
        event.setMax(packed.max);
        event.setColor(packed.color);
        event.setOverlay(packed.overlay);
        event.setDarkenScreen(packed.darkenScreen);
        event.setPlayBossMusic(packed.playBossMusic);
        event.setCreateWorldFog(packed.createWorldFog);
        event.players.addAll(packed.players);
        return event;
    }

    public Packed pack() {
        return new Packed(this.getName(), this.isVisible(), this.value(), this.max(), this.getColor(), this.getOverlay(), this.shouldDarkenScreen(), this.shouldPlayBossMusic(), this.shouldCreateWorldFog(), Set.copyOf(this.players));
    }

    public void onPlayerConnect(ServerPlayer player) {
        if (this.players.contains(player.getUUID())) {
            this.addPlayer(player);
        }
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        super.removePlayer(player);
    }

    @Override
    public void setDirty() {
        this.dirtyCallback.run();
    }

    public record Packed(Component name, boolean visible, int value, int max, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay, boolean darkenScreen, boolean playBossMusic, boolean createWorldFog, Set<UUID> players) {
        public static final Codec<Packed> CODEC = RecordCodecBuilder.create(i -> i.group((App)ComponentSerialization.CODEC.fieldOf("Name").forGetter(Packed::name), (App)Codec.BOOL.optionalFieldOf("Visible", (Object)false).forGetter(Packed::visible), (App)Codec.INT.optionalFieldOf("Value", (Object)0).forGetter(Packed::value), (App)Codec.INT.optionalFieldOf("Max", (Object)100).forGetter(Packed::max), (App)BossEvent.BossBarColor.CODEC.optionalFieldOf("Color", (Object)BossEvent.BossBarColor.WHITE).forGetter(Packed::color), (App)BossEvent.BossBarOverlay.CODEC.optionalFieldOf("Overlay", (Object)BossEvent.BossBarOverlay.PROGRESS).forGetter(Packed::overlay), (App)Codec.BOOL.optionalFieldOf("DarkenScreen", (Object)false).forGetter(Packed::darkenScreen), (App)Codec.BOOL.optionalFieldOf("PlayBossMusic", (Object)false).forGetter(Packed::playBossMusic), (App)Codec.BOOL.optionalFieldOf("CreateWorldFog", (Object)false).forGetter(Packed::createWorldFog), (App)UUIDUtil.CODEC_SET.optionalFieldOf("Players", Set.of()).forGetter(Packed::players)).apply((Applicative)i, Packed::new));
    }
}

