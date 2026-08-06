/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class LootParams {
    private final ServerLevel level;
    private final ContextMap params;
    private final Map<Identifier, DynamicDrop> dynamicDrops;
    private final float luck;

    public LootParams(ServerLevel level, ContextMap params, Map<Identifier, DynamicDrop> dynamicDrops, float luck) {
        this.level = level;
        this.params = params;
        this.dynamicDrops = dynamicDrops;
        this.luck = luck;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public ContextMap contextMap() {
        return this.params;
    }

    public void addDynamicDrops(Identifier location, Consumer<ItemStack> output) {
        DynamicDrop dynamicDrop = this.dynamicDrops.get(location);
        if (dynamicDrop != null) {
            dynamicDrop.add(output);
        }
    }

    public float getLuck() {
        return this.luck;
    }

    @FunctionalInterface
    public static interface DynamicDrop {
        public void add(Consumer<ItemStack> var1);
    }

    public static class Builder {
        private final ServerLevel level;
        private final ContextMap.Builder params = new ContextMap.Builder();
        private final Map<Identifier, DynamicDrop> dynamicDrops = Maps.newHashMap();
        private float luck;

        public Builder(ServerLevel level) {
            this.level = level;
        }

        public ServerLevel getLevel() {
            return this.level;
        }

        public <T> Builder withParameter(ContextKey<T> param, T value) {
            this.params.withParameter(param, value);
            return this;
        }

        public <T> Builder withOptionalParameter(ContextKey<T> param, @Nullable T value) {
            this.params.withOptionalParameter(param, value);
            return this;
        }

        public <T> T getParameter(ContextKey<T> param) {
            return this.params.getParameter(param);
        }

        public <T> @Nullable T getOptionalParameter(ContextKey<T> param) {
            return this.params.getOptionalParameter(param);
        }

        public Builder withDynamicDrop(Identifier location, DynamicDrop dynamicDrop) {
            DynamicDrop prev = this.dynamicDrops.put(location, dynamicDrop);
            if (prev != null) {
                throw new IllegalStateException("Duplicated dynamic drop '" + String.valueOf(this.dynamicDrops) + "'");
            }
            return this;
        }

        public Builder withLuck(float luck) {
            this.luck = luck;
            return this;
        }

        public LootParams create(ContextKeySet contextKeySet) {
            ContextMap keySet = this.params.create(contextKeySet);
            return new LootParams(this.level, keySet, this.dynamicDrops, this.luck);
        }
    }
}

