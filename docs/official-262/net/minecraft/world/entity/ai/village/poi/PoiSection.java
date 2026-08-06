/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.shorts.Short2ObjectMap
 *  it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.entity.ai.village.poi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.debug.DebugPoiInfo;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class PoiSection {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Short2ObjectMap<PoiRecord> records = new Short2ObjectOpenHashMap();
    private final Map<Holder<PoiType>, Set<PoiRecord>> byType = Maps.newHashMap();
    private final Runnable setDirty;
    private boolean isValid;

    public PoiSection(Runnable setDirty) {
        this(setDirty, true, (List<PoiRecord>)ImmutableList.of());
    }

    private PoiSection(Runnable setDirty, boolean isValid, List<PoiRecord> records) {
        this.setDirty = setDirty;
        this.isValid = isValid;
        records.forEach(this::add);
    }

    public Packed pack() {
        return new Packed(this.isValid, this.records.values().stream().map(PoiRecord::pack).toList());
    }

    public Stream<PoiRecord> getRecords(Predicate<Holder<PoiType>> predicate, PoiManager.Occupancy occupancy) {
        return this.byType.entrySet().stream().filter(e -> predicate.test((Holder)e.getKey())).flatMap(e -> ((Set)e.getValue()).stream()).filter(occupancy.getTest());
    }

    public @Nullable PoiRecord add(BlockPos blockPos, Holder<PoiType> type) {
        PoiRecord record = new PoiRecord(blockPos, type, this.setDirty);
        if (this.add(record)) {
            LOGGER.debug("Added POI of type {} @ {}", (Object)type.getRegisteredName(), (Object)blockPos);
            this.setDirty.run();
            return record;
        }
        return null;
    }

    private boolean add(PoiRecord record) {
        BlockPos blockPos = record.getPos();
        Holder<PoiType> poiType = record.getPoiType();
        short key = SectionPos.sectionRelativePos(blockPos);
        PoiRecord oldRecord = (PoiRecord)this.records.get(key);
        if (oldRecord != null) {
            if (poiType.equals(oldRecord.getPoiType())) {
                return false;
            }
            Util.logAndPauseIfInIde("POI data mismatch: already registered at " + String.valueOf(blockPos));
        }
        this.records.put(key, (Object)record);
        this.byType.computeIfAbsent(poiType, k -> Sets.newHashSet()).add(record);
        return true;
    }

    public void remove(BlockPos pos) {
        PoiRecord poiRecord = (PoiRecord)this.records.remove(SectionPos.sectionRelativePos(pos));
        if (poiRecord == null) {
            LOGGER.error("POI data mismatch: never registered at {}", (Object)pos);
            return;
        }
        this.byType.get(poiRecord.getPoiType()).remove(poiRecord);
        LOGGER.debug("Removed POI of type {} @ {}", LogUtils.defer(poiRecord::getPoiType), LogUtils.defer(poiRecord::getPos));
        this.setDirty.run();
    }

    @Deprecated
    @VisibleForDebug
    public int getFreeTickets(BlockPos pos) {
        return this.getPoiRecord(pos).map(PoiRecord::getFreeTickets).orElse(0);
    }

    public boolean release(BlockPos pos) {
        PoiRecord record = (PoiRecord)this.records.get(SectionPos.sectionRelativePos(pos));
        if (record == null) {
            throw Util.pauseInIde(new IllegalStateException("POI never registered at " + String.valueOf(pos)));
        }
        boolean success = record.releaseTicket();
        this.setDirty.run();
        return success;
    }

    public boolean exists(BlockPos pos, Predicate<Holder<PoiType>> predicate) {
        return this.getType(pos).filter(predicate).isPresent();
    }

    public Optional<Holder<PoiType>> getType(BlockPos pos) {
        return this.getPoiRecord(pos).map(PoiRecord::getPoiType);
    }

    private Optional<PoiRecord> getPoiRecord(BlockPos pos) {
        return Optional.ofNullable((PoiRecord)this.records.get(SectionPos.sectionRelativePos(pos)));
    }

    public Optional<DebugPoiInfo> getDebugPoiInfo(BlockPos pos) {
        return this.getPoiRecord(pos).map(DebugPoiInfo::new);
    }

    public void refresh(Consumer<BiConsumer<BlockPos, Holder<PoiType>>> updater) {
        if (!this.isValid) {
            Short2ObjectOpenHashMap oldRecords = new Short2ObjectOpenHashMap(this.records);
            this.clear();
            updater.accept((arg_0, arg_1) -> this.lambda$refresh$0((Short2ObjectMap)oldRecords, arg_0, arg_1));
            this.isValid = true;
            this.setDirty.run();
        }
    }

    private void clear() {
        this.records.clear();
        this.byType.clear();
    }

    public boolean isValid() {
        return this.isValid;
    }

    private /* synthetic */ void lambda$refresh$0(Short2ObjectMap oldRecords, BlockPos blockPos, Holder poiType) {
        short key = SectionPos.sectionRelativePos(blockPos);
        PoiRecord newRecord = (PoiRecord)oldRecords.computeIfAbsent(key, k -> new PoiRecord(blockPos, poiType, this.setDirty));
        this.add(newRecord);
    }

    public record Packed(boolean isValid, List<PoiRecord.Packed> records) {
        public static final Codec<Packed> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.BOOL.lenientOptionalFieldOf("Valid", (Object)false).forGetter(Packed::isValid), (App)PoiRecord.Packed.CODEC.listOf().fieldOf("Records").forGetter(Packed::records)).apply((Applicative)i, Packed::new));

        public PoiSection unpack(Runnable setDirty) {
            return new PoiSection(setDirty, this.isValid, this.records.stream().map(record -> record.unpack(setDirty)).toList());
        }
    }
}

