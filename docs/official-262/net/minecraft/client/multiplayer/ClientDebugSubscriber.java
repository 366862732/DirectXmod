/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.multiplayer;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundDebugSubscriptionRequestPacket;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ClientDebugSubscriber {
    private final ClientPacketListener connection;
    private final DebugScreenOverlay debugScreenOverlay;
    private Set<DebugSubscription<?>> remoteSubscriptions = Set.of();
    private final Map<DebugSubscription<?>, ValueMaps<?>> valuesBySubscription = new HashMap();

    public ClientDebugSubscriber(ClientPacketListener connection, DebugScreenOverlay debugScreenOverlay) {
        this.debugScreenOverlay = debugScreenOverlay;
        this.connection = connection;
    }

    private static void addFlag(Set<DebugSubscription<?>> output, DebugSubscription<?> subscription, boolean flag) {
        if (flag) {
            output.add(subscription);
        }
    }

    private Set<DebugSubscription<?>> requestedSubscriptions() {
        ReferenceOpenHashSet subscriptions = new ReferenceOpenHashSet();
        ClientDebugSubscriber.addFlag(subscriptions, RemoteDebugSampleType.TICK_TIME.subscription(), this.debugScreenOverlay.showFpsCharts());
        if (SharedConstants.DEBUG_ENABLED) {
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.BEES, SharedConstants.DEBUG_BEES);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.BEE_HIVES, SharedConstants.DEBUG_BEES);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.BRAINS, SharedConstants.DEBUG_BRAIN);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.BREEZES, SharedConstants.DEBUG_BREEZE_MOB);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.ENTITY_BLOCK_INTERSECTIONS, SharedConstants.DEBUG_ENTITY_BLOCK_INTERSECTION);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.ENTITY_PATHS, SharedConstants.DEBUG_PATHFINDING);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.GAME_EVENTS, SharedConstants.DEBUG_GAME_EVENT_LISTENERS);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.GAME_EVENT_LISTENERS, SharedConstants.DEBUG_GAME_EVENT_LISTENERS);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.GOAL_SELECTORS, SharedConstants.DEBUG_GOAL_SELECTOR || SharedConstants.DEBUG_BEES);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.NEIGHBOR_UPDATES, SharedConstants.DEBUG_NEIGHBORSUPDATE);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.POIS, SharedConstants.DEBUG_POI);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.RAIDS, SharedConstants.DEBUG_RAIDS);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, SharedConstants.DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.STRUCTURES, SharedConstants.DEBUG_STRUCTURES);
            ClientDebugSubscriber.addFlag(subscriptions, DebugSubscriptions.VILLAGE_SECTIONS, SharedConstants.DEBUG_VILLAGE_SECTIONS);
        }
        return subscriptions;
    }

    public void clear() {
        this.remoteSubscriptions = Set.of();
        this.dropLevel();
    }

    public void tick(long gameTime) {
        Set<DebugSubscription<?>> newSubscriptions = this.requestedSubscriptions();
        if (!newSubscriptions.equals(this.remoteSubscriptions)) {
            this.remoteSubscriptions = newSubscriptions;
            this.onSubscriptionsChanged(newSubscriptions);
        }
        this.valuesBySubscription.forEach((subscription, valueMaps) -> {
            if (subscription.expireAfterTicks() != 0) {
                valueMaps.purgeExpired(gameTime);
            }
        });
    }

    private void onSubscriptionsChanged(Set<DebugSubscription<?>> newSubscriptions) {
        this.valuesBySubscription.keySet().retainAll(newSubscriptions);
        this.initializeSubscriptions(newSubscriptions);
        this.connection.send(new ServerboundDebugSubscriptionRequestPacket(newSubscriptions));
    }

    private void initializeSubscriptions(Set<DebugSubscription<?>> newSubscriptions) {
        for (DebugSubscription<?> subscription : newSubscriptions) {
            this.valuesBySubscription.computeIfAbsent(subscription, s -> new ValueMaps());
        }
    }

    private <V> @Nullable ValueMaps<V> getValueMaps(DebugSubscription<V> subscription) {
        return this.valuesBySubscription.get(subscription);
    }

    private <K, V> @Nullable ValueMap<K, V> getValueMap(DebugSubscription<V> subscription, ValueMapType<K, V> mapType) {
        ValueMaps<V> maps = this.getValueMaps(subscription);
        return maps != null ? mapType.get(maps) : null;
    }

    private <K, V> @Nullable V getValue(DebugSubscription<V> subscription, K key, ValueMapType<K, V> type) {
        ValueMap<K, V> values = this.getValueMap(subscription, type);
        return values != null ? (V)values.getValue(key) : null;
    }

    public DebugValueAccess createDebugValueAccess(final Level level) {
        return new DebugValueAccess(){
            final /* synthetic */ ClientDebugSubscriber this$0;
            {
                ClientDebugSubscriber clientDebugSubscriber = this$0;
                Objects.requireNonNull(clientDebugSubscriber);
                this.this$0 = clientDebugSubscriber;
            }

            @Override
            public <T> void forEachChunk(DebugSubscription<T> subscription, BiConsumer<ChunkPos, T> consumer) {
                this.this$0.forEachValue(subscription, ClientDebugSubscriber.chunks(), consumer);
            }

            @Override
            public <T> @Nullable T getChunkValue(DebugSubscription<T> subscription, ChunkPos chunkPos) {
                return this.this$0.getValue(subscription, chunkPos, ClientDebugSubscriber.chunks());
            }

            @Override
            public <T> void forEachBlock(DebugSubscription<T> subscription, BiConsumer<BlockPos, T> consumer) {
                this.this$0.forEachValue(subscription, ClientDebugSubscriber.blocks(), consumer);
            }

            @Override
            public <T> @Nullable T getBlockValue(DebugSubscription<T> subscription, BlockPos blockPos) {
                return this.this$0.getValue(subscription, blockPos, ClientDebugSubscriber.blocks());
            }

            @Override
            public <T> void forEachEntity(DebugSubscription<T> subscription, BiConsumer<Entity, T> consumer) {
                this.this$0.forEachValue(subscription, ClientDebugSubscriber.entities(), (entityId, value) -> {
                    Entity entity = level.getEntity((UUID)entityId);
                    if (entity != null) {
                        consumer.accept(entity, value);
                    }
                });
            }

            @Override
            public <T> @Nullable T getEntityValue(DebugSubscription<T> subscription, Entity entity) {
                return this.this$0.getValue(subscription, entity.getUUID(), ClientDebugSubscriber.entities());
            }

            @Override
            public <T> void forEachEvent(DebugSubscription<T> subscription, DebugValueAccess.EventVisitor<T> visitor) {
                ValueMaps<T> values = this.this$0.getValueMaps(subscription);
                if (values == null) {
                    return;
                }
                long gameTime = level.getGameTime();
                for (ValueWrapper event : values.events) {
                    int remainingTicks = (int)(event.expiresAfterTime() - gameTime);
                    int totalLifetime = subscription.expireAfterTicks();
                    visitor.accept(event.value(), remainingTicks, totalLifetime);
                }
            }
        };
    }

    public <T> void updateChunk(long gameTime, ChunkPos chunkPos, DebugSubscription.Update<T> update) {
        this.updateMap(gameTime, chunkPos, update, ClientDebugSubscriber.chunks());
    }

    public <T> void updateBlock(long gameTime, BlockPos blockPos, DebugSubscription.Update<T> update) {
        this.updateMap(gameTime, blockPos, update, ClientDebugSubscriber.blocks());
    }

    public <T> void updateEntity(long gameTime, Entity entity, DebugSubscription.Update<T> update) {
        this.updateMap(gameTime, entity.getUUID(), update, ClientDebugSubscriber.entities());
    }

    public <T> void pushEvent(long gameTime, DebugSubscription.Event<T> event) {
        ValueMaps<T> values = this.getValueMaps(event.subscription());
        if (values != null) {
            values.events.add(new ValueWrapper<T>(event.value(), gameTime + (long)event.subscription().expireAfterTicks()));
        }
    }

    private <K, V> void updateMap(long gameTime, K key, DebugSubscription.Update<V> update, ValueMapType<K, V> type) {
        ValueMap<K, V> values = this.getValueMap(update.subscription(), type);
        if (values != null) {
            values.apply(gameTime, key, update);
        }
    }

    private <K, V> void forEachValue(DebugSubscription<V> subscription, ValueMapType<K, V> type, BiConsumer<K, V> consumer) {
        ValueMap<K, V> values = this.getValueMap(subscription, type);
        if (values != null) {
            values.forEach(consumer);
        }
    }

    public void dropLevel() {
        this.valuesBySubscription.clear();
        this.initializeSubscriptions(this.remoteSubscriptions);
    }

    public void dropChunk(ChunkPos chunkPos) {
        if (this.valuesBySubscription.isEmpty()) {
            return;
        }
        for (ValueMaps<?> values : this.valuesBySubscription.values()) {
            values.dropChunkAndBlocks(chunkPos);
        }
    }

    public void dropEntity(Entity entity) {
        if (this.valuesBySubscription.isEmpty()) {
            return;
        }
        for (ValueMaps<?> values : this.valuesBySubscription.values()) {
            values.entityValues.removeKey(entity.getUUID());
        }
    }

    private static <T> ValueMapType<UUID, T> entities() {
        return v -> v.entityValues;
    }

    private static <T> ValueMapType<BlockPos, T> blocks() {
        return v -> v.blockValues;
    }

    private static <T> ValueMapType<ChunkPos, T> chunks() {
        return v -> v.chunkValues;
    }

    @Environment(value=EnvType.CLIENT)
    private static class ValueMaps<V> {
        private final ValueMap<ChunkPos, V> chunkValues = new ValueMap();
        private final ValueMap<BlockPos, V> blockValues = new ValueMap();
        private final ValueMap<UUID, V> entityValues = new ValueMap();
        private final List<ValueWrapper<V>> events = new ArrayList<ValueWrapper<V>>();

        private ValueMaps() {
        }

        public void purgeExpired(long gameTime) {
            Predicate expiredPredicate = v -> v.hasExpired(gameTime);
            this.chunkValues.removeValues(expiredPredicate);
            this.blockValues.removeValues(expiredPredicate);
            this.entityValues.removeValues(expiredPredicate);
            this.events.removeIf(expiredPredicate);
        }

        public void dropChunkAndBlocks(ChunkPos chunkPos) {
            this.chunkValues.removeKey(chunkPos);
            this.blockValues.removeKeys(chunkPos::contains);
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    private static interface ValueMapType<K, V> {
        public ValueMap<K, V> get(ValueMaps<V> var1);
    }

    @Environment(value=EnvType.CLIENT)
    private static class ValueMap<K, V> {
        private final Map<K, ValueWrapper<V>> values = new HashMap<K, ValueWrapper<V>>();

        private ValueMap() {
        }

        public void removeValues(Predicate<ValueWrapper<V>> predicate) {
            this.values.values().removeIf(predicate);
        }

        public void removeKey(K key) {
            this.values.remove(key);
        }

        public void removeKeys(Predicate<K> predicate) {
            this.values.keySet().removeIf(predicate);
        }

        public @Nullable V getValue(K key) {
            ValueWrapper<V> result = this.values.get(key);
            return result != null ? (V)result.value() : null;
        }

        public void apply(long gameTime, K key, DebugSubscription.Update<V> update) {
            if (update.value().isPresent()) {
                this.values.put(key, new ValueWrapper<V>(update.value().get(), gameTime + (long)update.subscription().expireAfterTicks()));
            } else {
                this.values.remove(key);
            }
        }

        public void forEach(BiConsumer<K, V> output) {
            this.values.forEach((? super K k, ? super V v) -> output.accept(k, v.value()));
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record ValueWrapper<T>(T value, long expiresAfterTime) {
        private static final long NO_EXPIRY = -1L;

        public boolean hasExpired(long gameTime) {
            if (this.expiresAfterTime == -1L) {
                return false;
            }
            return gameTime >= this.expiresAfterTime;
        }
    }
}

