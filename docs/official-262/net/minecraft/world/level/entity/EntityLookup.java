/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.entity;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class EntityLookup<T extends EntityAccess> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Int2ObjectMap<T> byId = new Int2ObjectLinkedOpenHashMap();
    private final Map<UUID, T> byUuid = Maps.newHashMap();

    public <U extends T> void getEntities(EntityTypeTest<T, U> type, AbortableIterationConsumer<U> consumer) {
        for (EntityAccess entity : this.byId.values()) {
            EntityAccess maybeEntity = (EntityAccess)type.tryCast(entity);
            if (maybeEntity == null || !consumer.accept(maybeEntity).shouldAbort()) continue;
            return;
        }
    }

    public Iterable<T> getAllEntities() {
        return Iterables.unmodifiableIterable((Iterable)this.byId.values());
    }

    public void add(T entity) {
        UUID uuid = entity.getUUID();
        if (this.byUuid.containsKey(uuid)) {
            LOGGER.warn("Duplicate entity UUID {}: {}", (Object)uuid, entity);
            return;
        }
        this.byUuid.put(uuid, entity);
        this.byId.put(entity.getId(), entity);
    }

    public void remove(T entity) {
        this.byUuid.remove(entity.getUUID());
        this.byId.remove(entity.getId());
    }

    public @Nullable T getEntity(int id) {
        return (T)((EntityAccess)this.byId.get(id));
    }

    public @Nullable T getEntity(UUID id) {
        return (T)((EntityAccess)this.byUuid.get(id));
    }

    public int count() {
        return this.byUuid.size();
    }
}

