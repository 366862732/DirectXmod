/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.packs.resources;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.packs.resources.ResourceManager;

@FunctionalInterface
public interface PreparableReloadListener {
    public CompletableFuture<Void> reload(SharedState var1, Executor var2, PreparationBarrier var3, Executor var4);

    default public void prepareSharedState(SharedState currentReload) {
    }

    default public String getName() {
        return this.getClass().getSimpleName();
    }

    public static final class SharedState {
        private final ResourceManager manager;
        private final Map<StateKey<?>, Object> state = new IdentityHashMap();

        public SharedState(ResourceManager manager) {
            this.manager = manager;
        }

        public ResourceManager resourceManager() {
            return this.manager;
        }

        public <T> void set(StateKey<T> key, T value) {
            this.state.put(key, value);
        }

        public <T> T get(StateKey<T> key) {
            return (T)Objects.requireNonNull(this.state.get(key));
        }
    }

    public static final class StateKey<T> {
    }

    @FunctionalInterface
    public static interface PreparationBarrier {
        public <T> CompletableFuture<T> wait(T var1);
    }
}

