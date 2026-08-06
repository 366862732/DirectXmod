/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectFunction
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongAVLTreeSet
 *  it.unimi.dsi.fastutil.longs.LongBidirectionalIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  it.unimi.dsi.fastutil.longs.LongSortedSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterators;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.Visibility;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class EntitySectionStorage<T extends EntityAccess> {
    public static final int CHONKY_ENTITY_SEARCH_GRACE = 2;
    public static final int MAX_NON_CHONKY_ENTITY_SIZE = 4;
    private final Class<T> entityClass;
    private final Long2ObjectFunction<Visibility> intialSectionVisibility;
    private final Long2ObjectMap<EntitySection<T>> sections = new Long2ObjectOpenHashMap();
    private final LongSortedSet sectionIds = new LongAVLTreeSet();

    public EntitySectionStorage(Class<T> entityClass, Long2ObjectFunction<Visibility> intialSectionVisibility) {
        this.entityClass = entityClass;
        this.intialSectionVisibility = intialSectionVisibility;
    }

    public void forEachAccessibleNonEmptySection(AABB bb, AbortableIterationConsumer<EntitySection<T>> output) {
        int xMin = SectionPos.posToSectionCoord(bb.minX - 2.0);
        int yMin = SectionPos.posToSectionCoord(bb.minY - 4.0);
        int zMin = SectionPos.posToSectionCoord(bb.minZ - 2.0);
        int xMax = SectionPos.posToSectionCoord(bb.maxX + 2.0);
        int yMax = SectionPos.posToSectionCoord(bb.maxY + 0.0);
        int zMax = SectionPos.posToSectionCoord(bb.maxZ + 2.0);
        for (int x = xMin; x <= xMax; ++x) {
            long lowestAbsoluteSectionKey = SectionPos.asLong(x, 0, 0);
            long highestAbsoluteSectionKey = SectionPos.asLong(x, -1, -1);
            LongBidirectionalIterator it = this.sectionIds.subSet(lowestAbsoluteSectionKey, highestAbsoluteSectionKey + 1L).iterator();
            while (it.hasNext()) {
                EntitySection entitySection;
                long sectionKey = it.nextLong();
                int y = SectionPos.y(sectionKey);
                int z = SectionPos.z(sectionKey);
                if (y < yMin || y > yMax || z < zMin || z > zMax || (entitySection = (EntitySection)this.sections.get(sectionKey)) == null || entitySection.isEmpty() || !entitySection.getStatus().isAccessible() || !output.accept(entitySection).shouldAbort()) continue;
                return;
            }
        }
    }

    public LongStream getExistingSectionPositionsInChunk(long chunkKey) {
        int z;
        int x = ChunkPos.getX(chunkKey);
        LongSortedSet chunkSections = this.getChunkSections(x, z = ChunkPos.getZ(chunkKey));
        if (chunkSections.isEmpty()) {
            return LongStream.empty();
        }
        LongBidirectionalIterator iterator = chunkSections.iterator();
        return StreamSupport.longStream(Spliterators.spliteratorUnknownSize((PrimitiveIterator.OfLong)iterator, 1301), false);
    }

    private LongSortedSet getChunkSections(int x, int z) {
        long lowestAbsoluteSectionKey = SectionPos.asLong(x, 0, z);
        long highestAbsoluteSectionKey = SectionPos.asLong(x, -1, z);
        return this.sectionIds.subSet(lowestAbsoluteSectionKey, highestAbsoluteSectionKey + 1L);
    }

    public Stream<EntitySection<T>> getExistingSectionsInChunk(long chunkKey) {
        return this.getExistingSectionPositionsInChunk(chunkKey).mapToObj(arg_0 -> this.sections.get(arg_0)).filter(Objects::nonNull);
    }

    private static long getChunkKeyFromSectionKey(long sectionPos) {
        return ChunkPos.pack(SectionPos.x(sectionPos), SectionPos.z(sectionPos));
    }

    public EntitySection<T> getOrCreateSection(long key) {
        return (EntitySection)this.sections.computeIfAbsent(key, this::createSection);
    }

    public @Nullable EntitySection<T> getSection(long key) {
        return (EntitySection)this.sections.get(key);
    }

    private EntitySection<T> createSection(long sectionPos) {
        long chunkPos = EntitySectionStorage.getChunkKeyFromSectionKey(sectionPos);
        Visibility chunkStatus = (Visibility)((Object)this.intialSectionVisibility.get(chunkPos));
        this.sectionIds.add(sectionPos);
        return new EntitySection<T>(this.entityClass, chunkStatus);
    }

    public LongSet getAllChunksWithExistingSections() {
        LongOpenHashSet chunks = new LongOpenHashSet();
        this.sections.keySet().forEach(arg_0 -> EntitySectionStorage.lambda$getAllChunksWithExistingSections$0((LongSet)chunks, arg_0));
        return chunks;
    }

    public void getEntities(AABB bb, AbortableIterationConsumer<T> output) {
        this.forEachAccessibleNonEmptySection(bb, section -> section.getEntities(bb, output));
    }

    public <U extends T> void getEntities(EntityTypeTest<T, U> type, AABB bb, AbortableIterationConsumer<U> consumer) {
        this.forEachAccessibleNonEmptySection(bb, section -> section.getEntities(type, bb, consumer));
    }

    public void remove(long sectionKey) {
        this.sections.remove(sectionKey);
        this.sectionIds.remove(sectionKey);
    }

    @VisibleForDebug
    public int count() {
        return this.sectionIds.size();
    }

    private static /* synthetic */ void lambda$getAllChunksWithExistingSections$0(LongSet chunks, long sectionKey) {
        chunks.add(EntitySectionStorage.getChunkKeyFromSectionKey(sectionKey));
    }
}

