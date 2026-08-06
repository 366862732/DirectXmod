/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Sets
 */
package net.minecraft.world.level.gameevent;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.debug.DebugGameEventListenerInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

public class EuclideanGameEventListenerRegistry
implements GameEventListenerRegistry {
    private final List<GameEventListener> listeners = Lists.newArrayList();
    private final Set<GameEventListener> listenersToRemove = Sets.newHashSet();
    private final List<GameEventListener> listenersToAdd = Lists.newArrayList();
    private boolean processing;
    private final ServerLevel level;
    private final int sectionY;
    private final OnEmptyAction onEmptyAction;

    public EuclideanGameEventListenerRegistry(ServerLevel level, int sectionY, OnEmptyAction onEmptyAction) {
        this.level = level;
        this.sectionY = sectionY;
        this.onEmptyAction = onEmptyAction;
    }

    @Override
    public boolean isEmpty() {
        return this.listeners.isEmpty();
    }

    @Override
    public void register(GameEventListener listener) {
        if (this.processing) {
            this.listenersToAdd.add(listener);
        } else {
            this.listeners.add(listener);
        }
        EuclideanGameEventListenerRegistry.sendDebugInfo(this.level, listener);
    }

    private static void sendDebugInfo(ServerLevel level, GameEventListener listener) {
        EntityPositionSource entitySource;
        Entity entity;
        if (!level.debugSynchronizers().hasAnySubscriberFor(DebugSubscriptions.GAME_EVENT_LISTENERS)) {
            return;
        }
        DebugGameEventListenerInfo info = new DebugGameEventListenerInfo(listener.getListenerRadius());
        PositionSource listenerSource = listener.getListenerSource();
        if (listenerSource instanceof BlockPositionSource) {
            BlockPositionSource blockSource = (BlockPositionSource)listenerSource;
            level.debugSynchronizers().sendBlockValue(blockSource.pos(), DebugSubscriptions.GAME_EVENT_LISTENERS, info);
        } else if (listenerSource instanceof EntityPositionSource && (entity = level.getEntity((entitySource = (EntityPositionSource)listenerSource).getUuid())) != null) {
            level.debugSynchronizers().sendEntityValue(entity, DebugSubscriptions.GAME_EVENT_LISTENERS, info);
        }
    }

    @Override
    public void unregister(GameEventListener listener) {
        if (this.processing) {
            this.listenersToRemove.add(listener);
        } else {
            this.listeners.remove(listener);
        }
        if (this.listeners.isEmpty()) {
            this.onEmptyAction.apply(this.sectionY);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean visitInRangeListeners(Holder<GameEvent> event, Vec3 sourcePosition, GameEvent.Context context, GameEventListenerRegistry.ListenerVisitor action) {
        this.processing = true;
        boolean applicable = false;
        try {
            Iterator<GameEventListener> iterator = this.listeners.iterator();
            while (iterator.hasNext()) {
                GameEventListener listener = iterator.next();
                if (this.listenersToRemove.remove(listener)) {
                    iterator.remove();
                    continue;
                }
                Optional<Vec3> optionalPosition = EuclideanGameEventListenerRegistry.getPostableListenerPosition(this.level, sourcePosition, listener);
                if (!optionalPosition.isPresent()) continue;
                action.visit(listener, optionalPosition.get());
                applicable = true;
            }
        }
        finally {
            this.processing = false;
        }
        if (!this.listenersToAdd.isEmpty()) {
            this.listeners.addAll(this.listenersToAdd);
            this.listenersToAdd.clear();
        }
        if (!this.listenersToRemove.isEmpty()) {
            this.listeners.removeAll(this.listenersToRemove);
            this.listenersToRemove.clear();
        }
        return applicable;
    }

    private static Optional<Vec3> getPostableListenerPosition(ServerLevel level, Vec3 sourcePosition, GameEventListener listener) {
        int radiusSqr;
        Optional<Vec3> position = listener.getListenerSource().getPosition(level);
        if (position.isEmpty()) {
            return Optional.empty();
        }
        double distanceFromOrigin = BlockPos.containing(position.get()).distSqr(BlockPos.containing(sourcePosition));
        if (distanceFromOrigin > (double)(radiusSqr = listener.getListenerRadius() * listener.getListenerRadius())) {
            return Optional.empty();
        }
        return position;
    }

    @FunctionalInterface
    public static interface OnEmptyAction {
        public void apply(int var1);
    }
}

