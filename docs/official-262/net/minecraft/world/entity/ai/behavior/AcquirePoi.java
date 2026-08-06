/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  org.apache.commons.lang3.mutable.MutableLong
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jspecify.annotations.Nullable;

public class AcquirePoi {
    public static final int SCAN_RANGE = 48;

    public static BehaviorControl<PathfinderMob> create(Predicate<Holder<PoiType>> poiType, MemoryModuleType<GlobalPos> memoryToAcquire, boolean onlyIfAdult, Optional<Byte> onPoiAcquisitionEvent, BiPredicate<ServerLevel, BlockPos> validPoi) {
        return AcquirePoi.create(poiType, memoryToAcquire, memoryToAcquire, onlyIfAdult, onPoiAcquisitionEvent, validPoi);
    }

    public static BehaviorControl<PathfinderMob> create(Predicate<Holder<PoiType>> poiType, MemoryModuleType<GlobalPos> memoryToAcquire, boolean onlyIfAdult, Optional<Byte> onPoiAcquisitionEvent) {
        return AcquirePoi.create(poiType, memoryToAcquire, memoryToAcquire, onlyIfAdult, onPoiAcquisitionEvent, (l, p) -> true);
    }

    public static BehaviorControl<PathfinderMob> create(Predicate<Holder<PoiType>> poiType, MemoryModuleType<GlobalPos> memoryToValidate, MemoryModuleType<GlobalPos> memoryToAcquire, boolean onlyIfAdult, Optional<Byte> onPoiAcquisitionEvent, BiPredicate<ServerLevel, BlockPos> validPoi) {
        int batchSize = 5;
        int rate = 20;
        MutableLong nextScheduledStart = new MutableLong(0L);
        Long2ObjectOpenHashMap batchCache = new Long2ObjectOpenHashMap();
        OneShot<PathfinderMob> acquirePoi = BehaviorBuilder.create(arg_0 -> AcquirePoi.lambda$create$1(memoryToAcquire, onlyIfAdult, nextScheduledStart, (Long2ObjectMap)batchCache, poiType, validPoi, onPoiAcquisitionEvent, arg_0));
        if (memoryToAcquire == memoryToValidate) {
            return acquirePoi;
        }
        return BehaviorBuilder.create(i -> i.group(i.absent(memoryToValidate)).apply((Applicative)i, toValidate -> acquirePoi));
    }

    public static @Nullable Path findPathToPois(Mob body, Set<Pair<Holder<PoiType>, BlockPos>> pois) {
        if (pois.isEmpty()) {
            return null;
        }
        HashSet<BlockPos> targets = new HashSet<BlockPos>();
        int maxRange = 1;
        for (Pair<Holder<PoiType>, BlockPos> p : pois) {
            maxRange = Math.max(maxRange, ((PoiType)((Holder)p.getFirst()).value()).validRange());
            targets.add((BlockPos)p.getSecond());
        }
        return body.getNavigation().createPath(targets, maxRange);
    }

    private static /* synthetic */ App lambda$create$1(MemoryModuleType memoryToAcquire, boolean onlyIfAdult, MutableLong nextScheduledStart, Long2ObjectMap batchCache, Predicate poiType, BiPredicate validPoi, Optional onPoiAcquisitionEvent, BehaviorBuilder.Instance i) {
        return i.group(i.absent(memoryToAcquire)).apply((Applicative)i, toAcquire -> (level, body, timestamp) -> {
            if (onlyIfAdult && body.isBaby()) {
                return false;
            }
            RandomSource random = level.getRandom();
            if (nextScheduledStart.longValue() == 0L) {
                nextScheduledStart.setValue(level.getGameTime() + (long)random.nextInt(20));
                return false;
            }
            if (level.getGameTime() < nextScheduledStart.longValue()) {
                return false;
            }
            nextScheduledStart.setValue(timestamp + 20L + (long)random.nextInt(20));
            PoiManager poiManager = level.getPoiManager();
            batchCache.long2ObjectEntrySet().removeIf(entry -> !((JitteredLinearRetry)entry.getValue()).isStillValid(timestamp));
            Predicate<BlockPos> cacheTest = pos -> {
                JitteredLinearRetry retryMarker = (JitteredLinearRetry)batchCache.get(pos.asLong());
                if (retryMarker == null) {
                    return true;
                }
                if (!retryMarker.shouldRetry(timestamp)) {
                    return false;
                }
                retryMarker.markAttempt(timestamp);
                return true;
            };
            Set<Pair<Holder<PoiType>, BlockPos>> poiPositions = poiManager.findAllClosestFirstWithType(poiType, cacheTest, body.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE).limit(5L).filter(p -> validPoi.test(level, (BlockPos)p.getSecond())).collect(Collectors.toSet());
            Path path = AcquirePoi.findPathToPois(body, poiPositions);
            if (path != null && path.canReach()) {
                BlockPos targetPos = path.getTarget();
                poiManager.getType(targetPos).ifPresent(type -> {
                    poiManager.take(poiType, (t, poiPos) -> poiPos.equals(targetPos), targetPos, 1);
                    toAcquire.set(GlobalPos.of(level.dimension(), targetPos));
                    onPoiAcquisitionEvent.ifPresent(event -> level.broadcastEntityEvent(body, (byte)event));
                    batchCache.clear();
                    level.debugSynchronizers().updatePoi(targetPos);
                });
            } else {
                for (Pair<Holder<PoiType>, BlockPos> p2 : poiPositions) {
                    batchCache.computeIfAbsent(((BlockPos)p2.getSecond()).asLong(), key -> new JitteredLinearRetry(random, timestamp));
                }
            }
            return true;
        });
    }

    private static class JitteredLinearRetry {
        private static final int MIN_INTERVAL_INCREASE = 40;
        private static final int MAX_INTERVAL_INCREASE = 80;
        private static final int MAX_RETRY_PATHFINDING_INTERVAL = 400;
        private final RandomSource random;
        private long previousAttemptTimestamp;
        private long nextScheduledAttemptTimestamp;
        private int currentDelay;

        public JitteredLinearRetry(RandomSource random, long firstAttemptTimestamp) {
            this.random = random;
            this.markAttempt(firstAttemptTimestamp);
        }

        public void markAttempt(long timestamp) {
            this.previousAttemptTimestamp = timestamp;
            int suggestedDelay = this.currentDelay + this.random.nextInt(40) + 40;
            this.currentDelay = Math.min(suggestedDelay, 400);
            this.nextScheduledAttemptTimestamp = timestamp + (long)this.currentDelay;
        }

        public boolean isStillValid(long timestamp) {
            return timestamp - this.previousAttemptTimestamp < 400L;
        }

        public boolean shouldRetry(long timestamp) {
            return timestamp >= this.nextScheduledAttemptTimestamp;
        }

        public String toString() {
            return "RetryMarker{, previousAttemptAt=" + this.previousAttemptTimestamp + ", nextScheduledAttemptAt=" + this.nextScheduledAttemptTimestamp + ", currentDelay=" + this.currentDelay + "}";
        }
    }
}

