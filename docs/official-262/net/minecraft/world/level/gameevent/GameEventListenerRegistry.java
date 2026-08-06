/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.gameevent;

import net.minecraft.core.Holder;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.phys.Vec3;

public interface GameEventListenerRegistry {
    public static final GameEventListenerRegistry NOOP = new GameEventListenerRegistry(){

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void register(GameEventListener listener) {
        }

        @Override
        public void unregister(GameEventListener listener) {
        }

        @Override
        public boolean visitInRangeListeners(Holder<GameEvent> event, Vec3 sourcePosition, GameEvent.Context context, ListenerVisitor action) {
            return false;
        }
    };

    public boolean isEmpty();

    public void register(GameEventListener var1);

    public void unregister(GameEventListener var1);

    public boolean visitInRangeListeners(Holder<GameEvent> var1, Vec3 var2, GameEvent.Context var3, ListenerVisitor var4);

    @FunctionalInterface
    public static interface ListenerVisitor {
        public void visit(GameEventListener var1, Vec3 var2);
    }
}

