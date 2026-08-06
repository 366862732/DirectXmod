/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.longs.Long2ObjectFunction
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import net.minecraft.world.level.entity.Visibility;
import org.slf4j.Logger;

public class TransientEntitySectionManager<T extends EntityAccess> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final LevelCallback<T> callbacks;
    private final EntityLookup<T> entityStorage;
    private final EntitySectionStorage<T> sectionStorage;
    private final LongSet tickingChunks = new LongOpenHashSet();
    private final LevelEntityGetter<T> entityGetter;

    public TransientEntitySectionManager(Class<T> entityClass, LevelCallback<T> callbacks) {
        this.entityStorage = new EntityLookup();
        this.sectionStorage = new EntitySectionStorage<T>(entityClass, (Long2ObjectFunction<Visibility>)((Long2ObjectFunction)key -> this.tickingChunks.contains(key) ? Visibility.TICKING : Visibility.TRACKED));
        this.callbacks = callbacks;
        this.entityGetter = new LevelEntityGetterAdapter<T>(this.entityStorage, this.sectionStorage);
    }

    public void startTicking(ChunkPos pos) {
        long chunkKey = pos.pack();
        this.tickingChunks.add(chunkKey);
        this.sectionStorage.getExistingSectionsInChunk(chunkKey).forEach(section -> {
            Visibility previousStatus = section.updateChunkStatus(Visibility.TICKING);
            if (!previousStatus.isTicking()) {
                section.getEntities().filter(e -> !e.isAlwaysTicking()).forEach(this.callbacks::onTickingStart);
            }
        });
    }

    public void stopTicking(ChunkPos pos) {
        long chunkKey = pos.pack();
        this.tickingChunks.remove(chunkKey);
        this.sectionStorage.getExistingSectionsInChunk(chunkKey).forEach(section -> {
            Visibility previousStatus = section.updateChunkStatus(Visibility.TRACKED);
            if (previousStatus.isTicking()) {
                section.getEntities().filter(e -> !e.isAlwaysTicking()).forEach(this.callbacks::onTickingEnd);
            }
        });
    }

    public LevelEntityGetter<T> getEntityGetter() {
        return this.entityGetter;
    }

    public void addEntity(T entity) {
        this.entityStorage.add(entity);
        long sectionKey = SectionPos.asLong(entity.blockPosition());
        EntitySection<T> entitySection = this.sectionStorage.getOrCreateSection(sectionKey);
        entitySection.add(entity);
        entity.setLevelCallback(new Callback(this, entity, sectionKey, entitySection));
        this.callbacks.onCreated(entity);
        this.callbacks.onTrackingStart(entity);
        if (entity.isAlwaysTicking() || entitySection.getStatus().isTicking()) {
            this.callbacks.onTickingStart(entity);
        }
    }

    @VisibleForDebug
    public int count() {
        return this.entityStorage.count();
    }

    private void removeSectionIfEmpty(long sectionPos, EntitySection<T> section) {
        if (section.isEmpty()) {
            this.sectionStorage.remove(sectionPos);
        }
    }

    @VisibleForDebug
    public String gatherStats() {
        return this.entityStorage.count() + "," + this.sectionStorage.count() + "," + this.tickingChunks.size();
    }

    private class Callback
    implements EntityInLevelCallback {
        private final T entity;
        private long currentSectionKey;
        private EntitySection<T> currentSection;
        final /* synthetic */ TransientEntitySectionManager this$0;

        /*
         * WARNING - Possible parameter corruption
         * WARNING - void declaration
         */
        private Callback(T t, long currentSection, EntitySection<T> entitySection) {
            void var3_3;
            void entity;
            long l2 = l;
            Objects.requireNonNull(l2);
            this.this$0 = (TransientEntitySectionManager)l2;
            this.entity = entity;
            this.currentSectionKey = var3_3;
            this.currentSection = (EntitySection)currentSection;
        }

        @Override
        public void onMove() {
            BlockPos pos = this.entity.blockPosition();
            long newSectionPos = SectionPos.asLong(pos);
            if (newSectionPos != this.currentSectionKey) {
                Visibility previousStatus = this.currentSection.getStatus();
                if (!this.currentSection.remove(this.entity)) {
                    LOGGER.warn("Entity {} wasn't found in section {} (moving to {})", new Object[]{this.entity, SectionPos.of(this.currentSectionKey), newSectionPos});
                }
                this.this$0.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
                EntitySection newSection = this.this$0.sectionStorage.getOrCreateSection(newSectionPos);
                newSection.add(this.entity);
                this.currentSection = newSection;
                this.currentSectionKey = newSectionPos;
                this.this$0.callbacks.onSectionChange(this.entity);
                if (!this.entity.isAlwaysTicking()) {
                    boolean wasTicking = previousStatus.isTicking();
                    boolean isTicking = newSection.getStatus().isTicking();
                    if (wasTicking && !isTicking) {
                        this.this$0.callbacks.onTickingEnd(this.entity);
                    } else if (!wasTicking && isTicking) {
                        this.this$0.callbacks.onTickingStart(this.entity);
                    }
                }
            }
        }

        @Override
        public void onRemove(Entity.RemovalReason reason) {
            Visibility status;
            if (!this.currentSection.remove(this.entity)) {
                LOGGER.warn("Entity {} wasn't found in section {} (destroying due to {})", new Object[]{this.entity, SectionPos.of(this.currentSectionKey), reason});
            }
            if ((status = this.currentSection.getStatus()).isTicking() || this.entity.isAlwaysTicking()) {
                this.this$0.callbacks.onTickingEnd(this.entity);
            }
            this.this$0.callbacks.onTrackingEnd(this.entity);
            this.this$0.callbacks.onDestroyed(this.entity);
            this.this$0.entityStorage.remove(this.entity);
            this.entity.setLevelCallback(NULL);
            this.this$0.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
        }
    }
}

