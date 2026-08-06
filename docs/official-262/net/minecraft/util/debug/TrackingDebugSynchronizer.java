/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.debug;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugChunkValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.util.debug.DebugPoiInfo;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public abstract class TrackingDebugSynchronizer<T> {
    protected final DebugSubscription<T> subscription;
    private final Set<UUID> subscribedPlayers = new ObjectOpenHashSet();

    public TrackingDebugSynchronizer(DebugSubscription<T> subscription) {
        this.subscription = subscription;
    }

    public final void tick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            boolean wasSubscribed = this.subscribedPlayers.contains(player.getUUID());
            boolean isSubscribed = player.debugSubscriptions().contains(this.subscription);
            if (isSubscribed == wasSubscribed) continue;
            if (isSubscribed) {
                this.addSubscriber(player);
                continue;
            }
            this.subscribedPlayers.remove(player.getUUID());
        }
        this.subscribedPlayers.removeIf(id -> level.getPlayerByUUID((UUID)id) == null);
        if (!this.subscribedPlayers.isEmpty()) {
            this.pollAndSendUpdates(level);
        }
    }

    private void addSubscriber(ServerPlayer player) {
        this.subscribedPlayers.add(player.getUUID());
        player.getChunkTrackingView().forEach(chunkPos -> {
            if (!player.connection.chunkSender.isPending(chunkPos.pack())) {
                this.startTrackingChunk(player, (ChunkPos)chunkPos);
            }
        });
        player.level().getChunkSource().chunkMap.forEachEntityTrackedBy(player, entity -> this.startTrackingEntity(player, (Entity)entity));
    }

    protected final void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos trackedChunk, Packet<? super ClientGamePacketListener> packet) {
        ChunkMap chunkMap = level.getChunkSource().chunkMap;
        for (UUID playerId : this.subscribedPlayers) {
            ServerPlayer player;
            Player player2 = level.getPlayerByUUID(playerId);
            if (!(player2 instanceof ServerPlayer) || !chunkMap.isChunkTracked(player = (ServerPlayer)player2, trackedChunk.x(), trackedChunk.z())) continue;
            player.connection.send(packet);
        }
    }

    protected final void sendToPlayersTrackingEntity(ServerLevel level, Entity trackedEntity, Packet<? super ClientGamePacketListener> packet) {
        ChunkMap chunkMap = level.getChunkSource().chunkMap;
        chunkMap.sendToTrackingPlayersFiltered(trackedEntity, packet, player -> this.subscribedPlayers.contains(player.getUUID()));
    }

    public final void startTrackingChunk(ServerPlayer player, ChunkPos chunkPos) {
        if (this.subscribedPlayers.contains(player.getUUID())) {
            this.sendInitialChunk(player, chunkPos);
        }
    }

    public final void startTrackingEntity(ServerPlayer player, Entity entity) {
        if (this.subscribedPlayers.contains(player.getUUID())) {
            this.sendInitialEntity(player, entity);
        }
    }

    protected void clear() {
    }

    protected void pollAndSendUpdates(ServerLevel level) {
    }

    protected void sendInitialChunk(ServerPlayer player, ChunkPos chunkPos) {
    }

    protected void sendInitialEntity(ServerPlayer player, Entity entity) {
    }

    public static class VillageSectionSynchronizer
    extends TrackingDebugSynchronizer<Unit> {
        public VillageSectionSynchronizer() {
            super(DebugSubscriptions.VILLAGE_SECTIONS);
        }

        @Override
        protected void sendInitialChunk(ServerPlayer player, ChunkPos chunkPos) {
            ServerLevel level = player.level();
            PoiManager poiManager = level.getPoiManager();
            poiManager.getInChunk(t -> true, chunkPos, PoiManager.Occupancy.ANY).forEach(record -> {
                SectionPos centerSection = SectionPos.of(record.getPos());
                VillageSectionSynchronizer.forEachVillageSectionUpdate(level, centerSection, (sectionPos, isVillage) -> {
                    BlockPos sectionBlockPos = sectionPos.center();
                    player.connection.send(new ClientboundDebugBlockValuePacket(sectionBlockPos, this.subscription.packUpdate(isVillage != false ? Unit.INSTANCE : null)));
                });
            });
        }

        public void onPoiAdded(ServerLevel level, PoiRecord record) {
            this.sendVillageSectionsPacket(level, record.getPos());
        }

        public void onPoiRemoved(ServerLevel level, BlockPos poiPos) {
            this.sendVillageSectionsPacket(level, poiPos);
        }

        private void sendVillageSectionsPacket(ServerLevel level, BlockPos poiPos) {
            VillageSectionSynchronizer.forEachVillageSectionUpdate(level, SectionPos.of(poiPos), (sectionPos, isVillage) -> {
                BlockPos sectionBlockPos = sectionPos.center();
                if (isVillage.booleanValue()) {
                    this.sendToPlayersTrackingChunk(level, ChunkPos.containing(sectionBlockPos), new ClientboundDebugBlockValuePacket(sectionBlockPos, this.subscription.packUpdate(Unit.INSTANCE)));
                } else {
                    this.sendToPlayersTrackingChunk(level, ChunkPos.containing(sectionBlockPos), new ClientboundDebugBlockValuePacket(sectionBlockPos, this.subscription.emptyUpdate()));
                }
            });
        }

        private static void forEachVillageSectionUpdate(ServerLevel level, SectionPos centerSection, BiConsumer<SectionPos, Boolean> consumer) {
            for (int offsetZ = -1; offsetZ <= 1; ++offsetZ) {
                for (int offsetX = -1; offsetX <= 1; ++offsetX) {
                    for (int offsetY = -1; offsetY <= 1; ++offsetY) {
                        SectionPos sectionPos = centerSection.offset(offsetX, offsetY, offsetZ);
                        if (level.isVillage(sectionPos.center())) {
                            consumer.accept(sectionPos, true);
                            continue;
                        }
                        consumer.accept(sectionPos, false);
                    }
                }
            }
        }
    }

    public static class PoiSynchronizer
    extends TrackingDebugSynchronizer<DebugPoiInfo> {
        public PoiSynchronizer() {
            super(DebugSubscriptions.POIS);
        }

        @Override
        protected void sendInitialChunk(ServerPlayer player, ChunkPos chunkPos) {
            ServerLevel level = player.level();
            PoiManager poiManager = level.getPoiManager();
            poiManager.getInChunk(t -> true, chunkPos, PoiManager.Occupancy.ANY).forEach(record -> player.connection.send(new ClientboundDebugBlockValuePacket(record.getPos(), this.subscription.packUpdate(new DebugPoiInfo((PoiRecord)record)))));
        }

        public void onPoiAdded(ServerLevel level, PoiRecord record) {
            this.sendToPlayersTrackingChunk(level, ChunkPos.containing(record.getPos()), new ClientboundDebugBlockValuePacket(record.getPos(), this.subscription.packUpdate(new DebugPoiInfo(record))));
        }

        public void onPoiRemoved(ServerLevel level, BlockPos poiPos) {
            this.sendToPlayersTrackingChunk(level, ChunkPos.containing(poiPos), new ClientboundDebugBlockValuePacket(poiPos, this.subscription.emptyUpdate()));
        }

        public void onPoiTicketCountChanged(ServerLevel level, BlockPos poiPos) {
            this.sendToPlayersTrackingChunk(level, ChunkPos.containing(poiPos), new ClientboundDebugBlockValuePacket(poiPos, this.subscription.packUpdate(level.getPoiManager().getDebugPoiInfo(poiPos))));
        }
    }

    private static class ValueSource<T> {
        private final DebugValueSource.ValueGetter<T> getter;
        private @Nullable T lastSyncedValue;

        private ValueSource(DebugValueSource.ValueGetter<T> getter) {
            this.getter = getter;
        }

        public @Nullable DebugSubscription.Update<T> pollUpdate(DebugSubscription<T> subscription) {
            T newValue = this.getter.get();
            if (!Objects.equals(newValue, this.lastSyncedValue)) {
                this.lastSyncedValue = newValue;
                return subscription.packUpdate(newValue);
            }
            return null;
        }
    }

    public static class SourceSynchronizer<T>
    extends TrackingDebugSynchronizer<T> {
        private final Map<ChunkPos, ValueSource<T>> chunkSources = new HashMap<ChunkPos, ValueSource<T>>();
        private final Map<BlockPos, ValueSource<T>> blockEntitySources = new HashMap<BlockPos, ValueSource<T>>();
        private final Map<UUID, ValueSource<T>> entitySources = new HashMap<UUID, ValueSource<T>>();

        public SourceSynchronizer(DebugSubscription<T> subscription) {
            super(subscription);
        }

        @Override
        protected void clear() {
            this.chunkSources.clear();
            this.blockEntitySources.clear();
            this.entitySources.clear();
        }

        @Override
        protected void pollAndSendUpdates(ServerLevel level) {
            DebugSubscription.Update<T> update;
            for (Map.Entry<ChunkPos, ValueSource<T>> entry : this.chunkSources.entrySet()) {
                update = entry.getValue().pollUpdate(this.subscription);
                if (update == null) continue;
                ChunkPos chunkPos = entry.getKey();
                this.sendToPlayersTrackingChunk(level, chunkPos, new ClientboundDebugChunkValuePacket(chunkPos, update));
            }
            for (Map.Entry<Object, ValueSource<T>> entry : this.blockEntitySources.entrySet()) {
                update = entry.getValue().pollUpdate(this.subscription);
                if (update == null) continue;
                BlockPos blockPos = (BlockPos)entry.getKey();
                ChunkPos chunkPos = ChunkPos.containing(blockPos);
                this.sendToPlayersTrackingChunk(level, chunkPos, new ClientboundDebugBlockValuePacket(blockPos, update));
            }
            for (Map.Entry<Object, ValueSource<T>> entry : this.entitySources.entrySet()) {
                update = entry.getValue().pollUpdate(this.subscription);
                if (update == null) continue;
                Entity entity = Objects.requireNonNull(level.getEntity((UUID)entry.getKey()));
                this.sendToPlayersTrackingEntity(level, entity, new ClientboundDebugEntityValuePacket(entity.getId(), update));
            }
        }

        public void registerChunk(ChunkPos chunkPos, DebugValueSource.ValueGetter<T> getter) {
            this.chunkSources.put(chunkPos, new ValueSource<T>(getter));
        }

        public void registerBlockEntity(BlockPos blockPos, DebugValueSource.ValueGetter<T> getter) {
            this.blockEntitySources.put(blockPos, new ValueSource<T>(getter));
        }

        public void registerEntity(UUID entityId, DebugValueSource.ValueGetter<T> getter) {
            this.entitySources.put(entityId, new ValueSource<T>(getter));
        }

        public void dropChunk(ChunkPos chunkPos) {
            this.chunkSources.remove(chunkPos);
            this.blockEntitySources.keySet().removeIf(chunkPos::contains);
        }

        public void dropBlockEntity(ServerLevel level, BlockPos blockPos) {
            ValueSource<T> source = this.blockEntitySources.remove(blockPos);
            if (source != null) {
                ChunkPos chunkPos = ChunkPos.containing(blockPos);
                this.sendToPlayersTrackingChunk(level, chunkPos, new ClientboundDebugBlockValuePacket(blockPos, this.subscription.emptyUpdate()));
            }
        }

        public void dropEntity(Entity entity) {
            this.entitySources.remove(entity.getUUID());
        }

        @Override
        protected void sendInitialChunk(ServerPlayer player, ChunkPos chunkPos) {
            ValueSource<T> chunkSource = this.chunkSources.get(chunkPos);
            if (chunkSource != null && chunkSource.lastSyncedValue != null) {
                player.connection.send(new ClientboundDebugChunkValuePacket(chunkPos, this.subscription.packUpdate(chunkSource.lastSyncedValue)));
            }
            for (Map.Entry<BlockPos, ValueSource<T>> entry : this.blockEntitySources.entrySet()) {
                BlockPos blockPos;
                Object lastValue = entry.getValue().lastSyncedValue;
                if (lastValue == null || !chunkPos.contains(blockPos = entry.getKey())) continue;
                player.connection.send(new ClientboundDebugBlockValuePacket(blockPos, this.subscription.packUpdate(lastValue)));
            }
        }

        @Override
        protected void sendInitialEntity(ServerPlayer player, Entity entity) {
            ValueSource<T> source = this.entitySources.get(entity.getUUID());
            if (source != null && source.lastSyncedValue != null) {
                player.connection.send(new ClientboundDebugEntityValuePacket(entity.getId(), this.subscription.packUpdate(source.lastSyncedValue)));
            }
        }
    }
}

