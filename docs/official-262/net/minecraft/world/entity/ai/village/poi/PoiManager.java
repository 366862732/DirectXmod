/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixer
 *  com.mojang.datafixers.util.Pair
 *  it.unimi.dsi.fastutil.longs.Long2ByteMap
 *  it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.village.poi;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.SectionTracker;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.debug.DebugPoiInfo;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkIOErrorReporter;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.jspecify.annotations.Nullable;

public class PoiManager
extends SectionStorage<PoiSection, PoiSection.Packed> {
    public static final int MAX_VILLAGE_DISTANCE = 6;
    public static final int VILLAGE_SECTION_SIZE = 1;
    private final DistanceTracker distanceTracker;
    private final LongSet loadedChunks = new LongOpenHashSet();

    public PoiManager(RegionStorageInfo info, Path folder, DataFixer fixerUpper, boolean sync, RegistryAccess registryAccess, ChunkIOErrorReporter errorReporter, LevelHeightAccessor levelHeightAccessor) {
        super(new SimpleRegionStorage(info, folder, fixerUpper, sync, DataFixTypes.POI_CHUNK), PoiSection.Packed.CODEC, PoiSection::pack, PoiSection.Packed::unpack, PoiSection::new, registryAccess, errorReporter, levelHeightAccessor);
        this.distanceTracker = new DistanceTracker(this);
    }

    public @Nullable PoiRecord add(BlockPos pos, Holder<PoiType> type) {
        return ((PoiSection)this.getOrCreate(SectionPos.asLong(pos))).add(pos, type);
    }

    public void remove(BlockPos pos) {
        this.getOrLoad(SectionPos.asLong(pos)).ifPresent(poiSection -> poiSection.remove(pos));
    }

    public long getCountInRange(Predicate<Holder<PoiType>> predicate, BlockPos center, int radius, Occupancy occupancy) {
        return this.getInRange(predicate, center, radius, occupancy).count();
    }

    public boolean existsAtPosition(ResourceKey<PoiType> poiType, BlockPos blockPos) {
        return this.exists(blockPos, p -> p.is(poiType));
    }

    public Stream<PoiRecord> getInSquare(Predicate<Holder<PoiType>> predicate, BlockPos center, int radius, Occupancy occupancy) {
        int chunkRadius = Math.floorDiv(radius, 16) + 1;
        return ChunkPos.rangeClosed(ChunkPos.containing(center), chunkRadius).flatMap(pos -> this.getInChunk(predicate, (ChunkPos)pos, occupancy)).filter(record -> {
            BlockPos pos = record.getPos();
            return Math.abs(pos.getX() - center.getX()) <= radius && Math.abs(pos.getZ() - center.getZ()) <= radius;
        });
    }

    public Stream<PoiRecord> getInRange(Predicate<Holder<PoiType>> predicate, BlockPos center, int radius, Occupancy occupancy) {
        int radiusSqr = radius * radius;
        return this.getInSquare(predicate, center, radius, occupancy).filter(r -> r.getPos().distSqr(center) <= (double)radiusSqr);
    }

    @VisibleForDebug
    public Stream<PoiRecord> getInChunk(Predicate<Holder<PoiType>> predicate, ChunkPos chunkPos, Occupancy occupancy) {
        return IntStream.rangeClosed(this.levelHeightAccessor.getMinSectionY(), this.levelHeightAccessor.getMaxSectionY()).boxed().map(sectionY -> this.getOrLoad(SectionPos.of(chunkPos, sectionY).asLong())).filter(Optional::isPresent).flatMap(poiSection -> ((PoiSection)poiSection.get()).getRecords(predicate, occupancy));
    }

    public Stream<BlockPos> findAll(Predicate<Holder<PoiType>> predicate, Predicate<BlockPos> filter, BlockPos center, int radius, Occupancy occupancy) {
        return this.getInRange(predicate, center, radius, occupancy).map(PoiRecord::getPos).filter(filter);
    }

    public Stream<Pair<Holder<PoiType>, BlockPos>> findAllWithType(Predicate<Holder<PoiType>> predicate, Predicate<BlockPos> filter, BlockPos center, int radius, Occupancy occupancy) {
        return this.getInRange(predicate, center, radius, occupancy).filter(p -> filter.test(p.getPos())).map(p -> Pair.of(p.getPoiType(), (Object)p.getPos()));
    }

    public Stream<Pair<Holder<PoiType>, BlockPos>> findAllClosestFirstWithType(Predicate<Holder<PoiType>> predicate, Predicate<BlockPos> filter, BlockPos center, int radius, Occupancy occupancy) {
        return this.findAllWithType(predicate, filter, center, radius, occupancy).sorted(Comparator.comparingDouble(p -> ((BlockPos)p.getSecond()).distSqr(center)));
    }

    public Optional<BlockPos> find(Predicate<Holder<PoiType>> predicate, Predicate<BlockPos> filter, BlockPos center, int radius, Occupancy occupancy) {
        return this.findAll(predicate, filter, center, radius, occupancy).findFirst();
    }

    public Optional<BlockPos> findClosest(Predicate<Holder<PoiType>> predicate, BlockPos center, int radius, Occupancy occupancy) {
        return this.getInRange(predicate, center, radius, occupancy).map(PoiRecord::getPos).min(Comparator.comparingDouble(pos -> pos.distSqr(center)));
    }

    public Optional<Pair<Holder<PoiType>, BlockPos>> findClosestWithType(Predicate<Holder<PoiType>> predicate, BlockPos center, int radius, Occupancy occupancy) {
        return this.getInRange(predicate, center, radius, occupancy).min(Comparator.comparingDouble(r -> r.getPos().distSqr(center))).map(p -> Pair.of(p.getPoiType(), (Object)p.getPos()));
    }

    public Optional<BlockPos> findClosest(Predicate<Holder<PoiType>> predicate, Predicate<BlockPos> filter, BlockPos center, int radius, Occupancy occupancy) {
        return this.getInRange(predicate, center, radius, occupancy).map(PoiRecord::getPos).filter(filter).min(Comparator.comparingDouble(pos -> pos.distSqr(center)));
    }

    public Optional<BlockPos> take(Predicate<Holder<PoiType>> predicate, BiPredicate<Holder<PoiType>, BlockPos> filter, BlockPos center, int radius) {
        return this.getInRange(predicate, center, radius, Occupancy.HAS_SPACE).filter(poi -> filter.test(poi.getPoiType(), poi.getPos())).findFirst().map(r -> {
            r.acquireTicket();
            return r.getPos();
        });
    }

    public Optional<BlockPos> getRandom(Predicate<Holder<PoiType>> predicate, Predicate<BlockPos> filter, Occupancy occupancy, BlockPos center, int radius, RandomSource random) {
        List<PoiRecord> collect = Util.toShuffledList(this.getInRange(predicate, center, radius, occupancy), random);
        return collect.stream().filter(poi -> filter.test(poi.getPos())).findFirst().map(PoiRecord::getPos);
    }

    public boolean release(BlockPos pos) {
        return this.getOrLoad(SectionPos.asLong(pos)).map(section -> section.release(pos)).orElseThrow(() -> Util.pauseInIde(new IllegalStateException("POI never registered at " + String.valueOf(pos))));
    }

    public boolean exists(BlockPos pos, Predicate<Holder<PoiType>> predicate) {
        return this.getOrLoad(SectionPos.asLong(pos)).map(s -> s.exists(pos, predicate)).orElse(false);
    }

    public Optional<Holder<PoiType>> getType(BlockPos pos) {
        return this.getOrLoad(SectionPos.asLong(pos)).flatMap(section -> section.getType(pos));
    }

    @VisibleForDebug
    public @Nullable DebugPoiInfo getDebugPoiInfo(BlockPos pos) {
        return this.getOrLoad(SectionPos.asLong(pos)).flatMap(section -> section.getDebugPoiInfo(pos)).orElse(null);
    }

    public int sectionsToVillage(SectionPos sectionPos) {
        this.distanceTracker.runAllUpdates();
        return this.distanceTracker.getLevel(sectionPos.asLong());
    }

    private boolean isVillageCenter(long sectionPos) {
        Optional section = this.get(sectionPos);
        if (section == null) {
            return false;
        }
        return section.map(s -> s.getRecords(e -> e.is(PoiTypeTags.VILLAGE), Occupancy.IS_OCCUPIED).findAny().isPresent()).orElse(false);
    }

    @Override
    public void tick(BooleanSupplier haveTime) {
        super.tick(haveTime);
        this.distanceTracker.runAllUpdates();
    }

    @Override
    protected void setDirty(long sectionPos) {
        super.setDirty(sectionPos);
        this.distanceTracker.update(sectionPos, this.distanceTracker.getLevelFromSource(sectionPos), false);
    }

    @Override
    protected void onSectionLoad(long sectionPos) {
        this.distanceTracker.update(sectionPos, this.distanceTracker.getLevelFromSource(sectionPos), false);
    }

    public void checkConsistencyWithBlocks(SectionPos sectionPos, LevelChunkSection blockSection) {
        Util.ifElse(this.getOrLoad(sectionPos.asLong()), section -> section.refresh(output -> {
            if (PoiManager.mayHavePoi(blockSection)) {
                this.updateFromSection(blockSection, sectionPos, (BiConsumer<BlockPos, Holder<PoiType>>)output);
            }
        }), () -> {
            if (PoiManager.mayHavePoi(blockSection)) {
                PoiSection newSection = (PoiSection)this.getOrCreate(sectionPos.asLong());
                this.updateFromSection(blockSection, sectionPos, newSection::add);
            }
        });
    }

    private static boolean mayHavePoi(LevelChunkSection blockSection) {
        return blockSection.maybeHas(PoiTypes::hasPoi);
    }

    private void updateFromSection(LevelChunkSection blockSection, SectionPos pos, BiConsumer<BlockPos, Holder<PoiType>> output) {
        pos.blocksInside().forEach(blockPos -> {
            BlockState state = blockSection.getBlockState(SectionPos.sectionRelative(blockPos.getX()), SectionPos.sectionRelative(blockPos.getY()), SectionPos.sectionRelative(blockPos.getZ()));
            PoiTypes.forState(state).ifPresent(type -> output.accept((BlockPos)blockPos, (Holder<PoiType>)type));
        });
    }

    public void ensureLoadedAndValid(LevelReader reader, BlockPos center, int radius) {
        SectionPos.aroundChunk(ChunkPos.containing(center), Math.floorDiv(radius, 16), this.levelHeightAccessor.getMinSectionY(), this.levelHeightAccessor.getMaxSectionY()).map(pos -> Pair.of((Object)pos, this.getOrLoad(pos.asLong()))).filter(poiSection -> ((Optional)poiSection.getSecond()).map(PoiSection::isValid).orElse(false) == false).map(p -> ((SectionPos)p.getFirst()).chunk()).filter(pos -> this.loadedChunks.add(pos.pack())).forEach(pos -> reader.getChunk(pos.x(), pos.z(), ChunkStatus.EMPTY));
    }

    private final class DistanceTracker
    extends SectionTracker {
        private final Long2ByteMap levels;
        final /* synthetic */ PoiManager this$0;

        DistanceTracker(PoiManager poiManager) {
            PoiManager poiManager2 = poiManager;
            Objects.requireNonNull(poiManager2);
            this.this$0 = poiManager2;
            super(7, 16, 256);
            this.levels = new Long2ByteOpenHashMap();
            this.levels.defaultReturnValue((byte)7);
        }

        @Override
        protected int getLevelFromSource(long to) {
            return this.this$0.isVillageCenter(to) ? 0 : 7;
        }

        @Override
        protected int getLevel(long node) {
            return this.levels.get(node);
        }

        @Override
        protected void setLevel(long node, int level) {
            if (level > 6) {
                this.levels.remove(node);
            } else {
                this.levels.put(node, (byte)level);
            }
        }

        public void runAllUpdates() {
            super.runUpdates(Integer.MAX_VALUE);
        }
    }

    public static enum Occupancy {
        HAS_SPACE(PoiRecord::hasSpace),
        IS_OCCUPIED(PoiRecord::isOccupied),
        ANY(poiRecord -> true);

        private final Predicate<? super PoiRecord> test;

        private Occupancy(Predicate<? super PoiRecord> test) {
            this.test = test;
        }

        public Predicate<? super PoiRecord> getTest() {
            return this.test;
        }
    }
}

