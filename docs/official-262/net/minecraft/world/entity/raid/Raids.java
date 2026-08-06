/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.raid;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Raids
extends SavedData {
    private static final Identifier RAID_FILE_ID = Identifier.withDefaultNamespace("raids");
    public static final Codec<Raids> CODEC = RecordCodecBuilder.create(i -> i.group((App)RaidWithId.CODEC.listOf().optionalFieldOf("raids", List.of()).forGetter(r -> r.raidMap.int2ObjectEntrySet().stream().map(RaidWithId::from).toList()), (App)Codec.INT.fieldOf("next_id").forGetter(r -> r.nextId), (App)Codec.INT.fieldOf("tick").forGetter(r -> r.tick)).apply((Applicative)i, Raids::new));
    public static final SavedDataType<Raids> TYPE = new SavedDataType<Raids>(RAID_FILE_ID, Raids::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS);
    private final Int2ObjectMap<Raid> raidMap = new Int2ObjectOpenHashMap();
    private int nextId = 1;
    private int tick;

    public Raids() {
        this.setDirty();
    }

    private Raids(List<RaidWithId> raids, int nextId, int tick) {
        for (RaidWithId raid : raids) {
            this.raidMap.put(raid.id, (Object)raid.raid);
        }
        this.nextId = nextId;
        this.tick = tick;
    }

    public @Nullable Raid get(int raidId) {
        return (Raid)this.raidMap.get(raidId);
    }

    public OptionalInt getId(Raid raid) {
        for (Int2ObjectMap.Entry entry : this.raidMap.int2ObjectEntrySet()) {
            if (entry.getValue() != raid) continue;
            return OptionalInt.of(entry.getIntKey());
        }
        return OptionalInt.empty();
    }

    public void tick(ServerLevel level) {
        ++this.tick;
        ObjectIterator raidIterator = this.raidMap.values().iterator();
        while (raidIterator.hasNext()) {
            Raid raid = (Raid)raidIterator.next();
            if (!level.getGameRules().get(GameRules.RAIDS).booleanValue()) {
                raid.stop();
            }
            if (raid.isStopped()) {
                raidIterator.remove();
                this.setDirty();
                continue;
            }
            raid.tick(level);
        }
        if (this.tick % 200 == 0) {
            this.setDirty();
        }
    }

    public static boolean canJoinRaid(Raider raider) {
        return raider.isAlive() && raider.canJoinRaid() && raider.getNoActionTime() <= 2400;
    }

    public @Nullable Raid createOrExtendRaid(ServerPlayer player, BlockPos raidPosition) {
        BlockPos raidCenterPos;
        if (player.isSpectator()) {
            return null;
        }
        ServerLevel level = player.level();
        if (!level.getGameRules().get(GameRules.RAIDS).booleanValue()) {
            return null;
        }
        if (!level.environmentAttributes().getValue(EnvironmentAttributes.CAN_START_RAID, raidPosition).booleanValue()) {
            return null;
        }
        List<PoiRecord> posses = level.getPoiManager().getInRange(e -> e.is(PoiTypeTags.VILLAGE), raidPosition, 64, PoiManager.Occupancy.IS_OCCUPIED).toList();
        int count = 0;
        Vec3 posTotals = Vec3.ZERO;
        for (PoiRecord p : posses) {
            BlockPos pos = p.getPos();
            posTotals = posTotals.add(pos.getX(), pos.getY(), pos.getZ());
            ++count;
        }
        if (count > 0) {
            posTotals = posTotals.scale(1.0 / (double)count);
            raidCenterPos = BlockPos.containing(posTotals);
        } else {
            raidCenterPos = raidPosition;
        }
        Raid raid = this.getOrCreateRaid(level, raidCenterPos);
        if (!raid.isStarted() && !this.raidMap.containsValue((Object)raid)) {
            this.raidMap.put(this.getUniqueId(), (Object)raid);
        }
        if (!raid.isStarted() || raid.getRaidOmenLevel() < raid.getMaxRaidOmenLevel()) {
            raid.absorbRaidOmen(player);
        }
        this.setDirty();
        return raid;
    }

    private Raid getOrCreateRaid(ServerLevel level, BlockPos pos) {
        Raid raid = level.getRaidAt(pos);
        return raid != null ? raid : new Raid(pos, level.getDifficulty());
    }

    public static Raids load(CompoundTag tag) {
        return CODEC.parse((DynamicOps)NbtOps.INSTANCE, (Object)tag).resultOrPartial().orElseGet(Raids::new);
    }

    private int getUniqueId() {
        return ++this.nextId;
    }

    public @Nullable Raid getNearbyRaid(BlockPos pos, int maxDistSqr) {
        Raid closest = null;
        double closestDistanceSqr = maxDistSqr;
        for (Raid raid : this.raidMap.values()) {
            double distance = raid.getCenter().distSqr(pos);
            if (!raid.isActive() || !(distance < closestDistanceSqr)) continue;
            closest = raid;
            closestDistanceSqr = distance;
        }
        return closest;
    }

    @VisibleForDebug
    public List<BlockPos> getRaidCentersInChunk(ChunkPos chunkPos) {
        return this.raidMap.values().stream().map(Raid::getCenter).filter(chunkPos::contains).toList();
    }

    private record RaidWithId(int id, Raid raid) {
        public static final Codec<RaidWithId> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.INT.fieldOf("id").forGetter(RaidWithId::id), (App)Raid.MAP_CODEC.forGetter(RaidWithId::raid)).apply((Applicative)i, RaidWithId::new));

        public static RaidWithId from(Int2ObjectMap.Entry<Raid> entry) {
            return new RaidWithId(entry.getIntKey(), (Raid)entry.getValue());
        }
    }
}

