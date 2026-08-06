/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.entity;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.stream.Stream;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.Visibility;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public class EntitySection<T extends EntityAccess> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ClassInstanceMultiMap<T> storage;
    private Visibility chunkStatus;

    public EntitySection(Class<T> entityClass, Visibility chunkStatus) {
        this.chunkStatus = chunkStatus;
        this.storage = new ClassInstanceMultiMap<T>(entityClass);
    }

    public void add(T entity) {
        this.storage.add(entity);
    }

    public boolean remove(T entity) {
        return this.storage.remove(entity);
    }

    public AbortableIterationConsumer.Continuation getEntities(AABB bb, AbortableIterationConsumer<T> entities) {
        for (EntityAccess entity : this.storage) {
            if (!entity.getBoundingBox().intersects(bb) || !entities.accept(entity).shouldAbort()) continue;
            return AbortableIterationConsumer.Continuation.ABORT;
        }
        return AbortableIterationConsumer.Continuation.CONTINUE;
    }

    public <U extends T> AbortableIterationConsumer.Continuation getEntities(EntityTypeTest<T, U> type, AABB bb, AbortableIterationConsumer<? super U> consumer) {
        Collection<T> foundEntities = this.storage.find(type.getBaseClass());
        if (foundEntities.isEmpty()) {
            return AbortableIterationConsumer.Continuation.CONTINUE;
        }
        for (EntityAccess entity : foundEntities) {
            EntityAccess maybeEntity = (EntityAccess)type.tryCast(entity);
            if (maybeEntity == null || !entity.getBoundingBox().intersects(bb) || !consumer.accept(maybeEntity).shouldAbort()) continue;
            return AbortableIterationConsumer.Continuation.ABORT;
        }
        return AbortableIterationConsumer.Continuation.CONTINUE;
    }

    public boolean isEmpty() {
        return this.storage.isEmpty();
    }

    public Stream<T> getEntities() {
        return this.storage.stream();
    }

    public Visibility getStatus() {
        return this.chunkStatus;
    }

    public Visibility updateChunkStatus(Visibility chunkStatus) {
        Visibility prev = this.chunkStatus;
        this.chunkStatus = chunkStatus;
        return prev;
    }

    @VisibleForDebug
    public int size() {
        return this.storage.size();
    }
}

