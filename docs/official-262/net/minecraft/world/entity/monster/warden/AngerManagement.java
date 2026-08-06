/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Streams
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.warden;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public class AngerManagement {
    @VisibleForTesting
    protected static final int CONVERSION_DELAY = 2;
    @VisibleForTesting
    protected static final int MAX_ANGER = 150;
    private static final int DEFAULT_ANGER_DECREASE = 1;
    private int conversionDelay = Mth.randomBetweenInclusive(RandomSource.createThreadLocalInstance(), 0, 2);
    private int highestAnger;
    private static final Codec<Pair<UUID, Integer>> SUSPECT_ANGER_PAIR = RecordCodecBuilder.create(i -> i.group((App)UUIDUtil.CODEC.fieldOf("uuid").forGetter(Pair::getFirst), (App)ExtraCodecs.NON_NEGATIVE_INT.fieldOf("anger").forGetter(Pair::getSecond)).apply((Applicative)i, Pair::of));
    private final Predicate<Entity> filter;
    @VisibleForTesting
    protected final ArrayList<Entity> suspects;
    private final Sorter suspectSorter;
    @VisibleForTesting
    protected final Object2IntMap<Entity> angerBySuspect;
    @VisibleForTesting
    protected final Object2IntMap<UUID> angerByUuid;

    public static Codec<AngerManagement> codec(Predicate<Entity> filter) {
        return RecordCodecBuilder.create(i -> i.group((App)SUSPECT_ANGER_PAIR.listOf().lenientOptionalFieldOf("suspects", List.of()).forGetter(AngerManagement::createUuidAngerPairs)).apply((Applicative)i, list -> new AngerManagement(filter, (List<Pair<UUID, Integer>>)list)));
    }

    public AngerManagement(Predicate<Entity> filter, List<Pair<UUID, Integer>> angerByUuid) {
        this.filter = filter;
        this.suspects = new ArrayList();
        this.suspectSorter = new Sorter(this);
        this.angerBySuspect = new Object2IntOpenHashMap();
        this.angerByUuid = new Object2IntOpenHashMap(angerByUuid.size());
        angerByUuid.forEach(pair -> this.angerByUuid.put((Object)((UUID)pair.getFirst()), (Integer)pair.getSecond()));
    }

    private List<Pair<UUID, Integer>> createUuidAngerPairs() {
        return Streams.concat((Stream[])new Stream[]{this.suspects.stream().map(e -> Pair.of((Object)e.getUUID(), (Object)this.angerBySuspect.getInt(e))), this.angerByUuid.object2IntEntrySet().stream().map(e -> Pair.of((Object)((UUID)e.getKey()), (Object)e.getIntValue()))}).collect(Collectors.toList());
    }

    public void tick(ServerLevel level, Predicate<Entity> validEntity) {
        --this.conversionDelay;
        if (this.conversionDelay <= 0) {
            this.convertFromUuids(level);
            this.conversionDelay = 2;
        }
        ObjectIterator serializedIterator = this.angerByUuid.object2IntEntrySet().iterator();
        while (serializedIterator.hasNext()) {
            Object2IntMap.Entry entry = (Object2IntMap.Entry)serializedIterator.next();
            int anger = entry.getIntValue();
            if (anger <= 1) {
                serializedIterator.remove();
                continue;
            }
            entry.setValue(anger - 1);
        }
        ObjectIterator iterator = this.angerBySuspect.object2IntEntrySet().iterator();
        while (iterator.hasNext()) {
            Object2IntMap.Entry entry = (Object2IntMap.Entry)iterator.next();
            int anger = entry.getIntValue();
            Entity entity = (Entity)entry.getKey();
            Entity.RemovalReason removalReason = entity.getRemovalReason();
            if (anger <= 1 || !validEntity.test(entity) || removalReason != null) {
                this.suspects.remove(entity);
                iterator.remove();
                if (anger <= 1 || removalReason == null) continue;
                switch (removalReason) {
                    case CHANGED_DIMENSION: 
                    case UNLOADED_TO_CHUNK: 
                    case UNLOADED_WITH_PLAYER: {
                        this.angerByUuid.put((Object)entity.getUUID(), anger - 1);
                    }
                }
                continue;
            }
            entry.setValue(anger - 1);
        }
        this.sortAndUpdateHighestAnger();
    }

    private void sortAndUpdateHighestAnger() {
        this.highestAnger = 0;
        this.suspects.sort(this.suspectSorter);
        if (this.suspects.size() == 1) {
            this.highestAnger = this.angerBySuspect.getInt((Object)this.suspects.get(0));
        }
    }

    private void convertFromUuids(ServerLevel level) {
        ObjectIterator iterator = this.angerByUuid.object2IntEntrySet().iterator();
        while (iterator.hasNext()) {
            Object2IntMap.Entry entry = (Object2IntMap.Entry)iterator.next();
            int anger = entry.getIntValue();
            Entity entity = level.getEntity((UUID)entry.getKey());
            if (entity == null) continue;
            this.angerBySuspect.put((Object)entity, anger);
            this.suspects.add(entity);
            iterator.remove();
        }
    }

    public int increaseAnger(Entity entity, int increment) {
        boolean newSuspect = !this.angerBySuspect.containsKey((Object)entity);
        int currentAnger = this.angerBySuspect.computeInt((Object)entity, (k, anger) -> Math.min(150, (anger == null ? 0 : anger) + increment));
        if (newSuspect) {
            int serializedAnger = this.angerByUuid.removeInt((Object)entity.getUUID());
            this.angerBySuspect.put((Object)entity, currentAnger += serializedAnger);
            this.suspects.add(entity);
        }
        this.sortAndUpdateHighestAnger();
        return currentAnger;
    }

    public void clearAnger(Entity entity) {
        this.angerBySuspect.removeInt((Object)entity);
        this.suspects.remove(entity);
        this.sortAndUpdateHighestAnger();
    }

    private @Nullable Entity getTopSuspect() {
        return this.suspects.stream().filter(this.filter).findFirst().orElse(null);
    }

    public int getActiveAnger(@Nullable Entity currentTarget) {
        return currentTarget == null ? this.highestAnger : this.angerBySuspect.getInt((Object)currentTarget);
    }

    public Optional<LivingEntity> getActiveEntity() {
        return Optional.ofNullable(this.getTopSuspect()).filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity)e);
    }

    @VisibleForTesting
    protected record Sorter(AngerManagement angerManagement) implements Comparator<Entity>
    {
        @Override
        public int compare(Entity entity1, Entity entity2) {
            boolean angryAt2;
            if (entity1.equals(entity2)) {
                return 0;
            }
            int anger1 = this.angerManagement.angerBySuspect.getOrDefault((Object)entity1, 0);
            int anger2 = this.angerManagement.angerBySuspect.getOrDefault((Object)entity2, 0);
            this.angerManagement.highestAnger = Math.max(this.angerManagement.highestAnger, Math.max(anger1, anger2));
            boolean angryAt1 = AngerLevel.byAnger(anger1).isAngry();
            if (angryAt1 != (angryAt2 = AngerLevel.byAnger(anger2).isAngry())) {
                return angryAt1 ? -1 : 1;
            }
            boolean isPlayer1 = entity1 instanceof Player;
            boolean isPlayer2 = entity2 instanceof Player;
            if (isPlayer1 != isPlayer2) {
                return isPlayer1 ? -1 : 1;
            }
            return Integer.compare(anger2, anger1);
        }
    }
}

