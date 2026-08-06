/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 */
package net.minecraft.world.entity.ai.gossip;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.ai.gossip.GossipType;

public class GossipContainer {
    public static final Codec<GossipContainer> CODEC = GossipEntry.CODEC.listOf().xmap(GossipContainer::new, container -> container.unpack().toList());
    public static final int DISCARD_THRESHOLD = 2;
    private final Map<UUID, EntityGossips> gossips = new HashMap<UUID, EntityGossips>();

    public GossipContainer() {
    }

    private GossipContainer(List<GossipEntry> entries) {
        entries.forEach(e -> this.getOrCreate((UUID)e.target).entries.put((Object)e.type, e.value));
    }

    @VisibleForDebug
    public Map<UUID, Object2IntMap<GossipType>> getGossipEntries() {
        HashMap result = Maps.newHashMap();
        this.gossips.keySet().forEach(uuid -> {
            EntityGossips entityGossips = this.gossips.get(uuid);
            result.put(uuid, entityGossips.entries);
        });
        return result;
    }

    public void decay() {
        Iterator<EntityGossips> iterator = this.gossips.values().iterator();
        while (iterator.hasNext()) {
            EntityGossips entityGossips = iterator.next();
            entityGossips.decay();
            if (!entityGossips.isEmpty()) continue;
            iterator.remove();
        }
    }

    private Stream<GossipEntry> unpack() {
        return this.gossips.entrySet().stream().flatMap(e -> ((EntityGossips)e.getValue()).unpack((UUID)e.getKey()));
    }

    private Collection<GossipEntry> selectGossipsForTransfer(RandomSource random, int maxCount) {
        List<GossipEntry> entries = this.unpack().toList();
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        int[] ranges = new int[entries.size()];
        int rangesEnd = 0;
        for (int i = 0; i < entries.size(); ++i) {
            GossipEntry gossip = entries.get(i);
            ranges[i] = (rangesEnd += Math.abs(gossip.weightedValue())) - 1;
        }
        Set results = Sets.newIdentityHashSet();
        for (int i = 0; i < maxCount; ++i) {
            int choice = random.nextInt(rangesEnd);
            int selectedIndex = Arrays.binarySearch(ranges, choice);
            results.add(entries.get(selectedIndex < 0 ? -selectedIndex - 1 : selectedIndex));
        }
        return results;
    }

    private EntityGossips getOrCreate(UUID target) {
        return this.gossips.computeIfAbsent(target, uuid -> new EntityGossips());
    }

    public void transferFrom(GossipContainer source, RandomSource random, int maxCount) {
        Collection<GossipEntry> newGossips = source.selectGossipsForTransfer(random, maxCount);
        newGossips.forEach(newGossip -> {
            int decayedValue = newGossip.value - newGossip.type.decayPerTransfer;
            if (decayedValue >= 2) {
                this.getOrCreate((UUID)newGossip.target).entries.mergeInt((Object)newGossip.type, decayedValue, GossipContainer::mergeValuesForTransfer);
            }
        });
    }

    public int getReputation(UUID entity, Predicate<GossipType> types) {
        EntityGossips entry = this.gossips.get(entity);
        return entry != null ? entry.weightedValue(types) : 0;
    }

    public long getCountForType(GossipType type, DoublePredicate valueTest) {
        return this.gossips.values().stream().filter(e -> valueTest.test(e.entries.getOrDefault((Object)type, 0) * type.weight)).count();
    }

    public void add(UUID target, GossipType type, int amountToAdd) {
        EntityGossips entityGossips = this.getOrCreate(target);
        entityGossips.entries.mergeInt((Object)type, amountToAdd, (o, n) -> this.mergeValuesForAddition(type, o, n));
        entityGossips.makeSureValueIsntTooLowOrTooHigh(type);
        if (entityGossips.isEmpty()) {
            this.gossips.remove(target);
        }
    }

    public void remove(UUID target, GossipType type, int amountToRemove) {
        this.add(target, type, -amountToRemove);
    }

    public void remove(UUID target, GossipType type) {
        EntityGossips entityGossips = this.gossips.get(target);
        if (entityGossips != null) {
            entityGossips.remove(type);
            if (entityGossips.isEmpty()) {
                this.gossips.remove(target);
            }
        }
    }

    public void remove(GossipType type) {
        Iterator<EntityGossips> iterator = this.gossips.values().iterator();
        while (iterator.hasNext()) {
            EntityGossips entityGossips = iterator.next();
            entityGossips.remove(type);
            if (!entityGossips.isEmpty()) continue;
            iterator.remove();
        }
    }

    public void clear() {
        this.gossips.clear();
    }

    public void putAll(GossipContainer container) {
        container.gossips.forEach((target, gossips) -> this.getOrCreate((UUID)target).entries.putAll(gossips.entries));
    }

    private static int mergeValuesForTransfer(int oldValue, int newValue) {
        return Math.max(oldValue, newValue);
    }

    private int mergeValuesForAddition(GossipType type, int oldValue, int newValue) {
        int sum = oldValue + newValue;
        return sum > type.max ? Math.max(type.max, oldValue) : sum;
    }

    public GossipContainer copy() {
        GossipContainer container = new GossipContainer();
        container.putAll(this);
        return container;
    }

    private static class EntityGossips {
        private final Object2IntMap<GossipType> entries = new Object2IntOpenHashMap();

        private EntityGossips() {
        }

        public int weightedValue(Predicate<GossipType> types) {
            return this.entries.object2IntEntrySet().stream().filter(e -> types.test((GossipType)e.getKey())).mapToInt(e -> e.getIntValue() * ((GossipType)e.getKey()).weight).sum();
        }

        public Stream<GossipEntry> unpack(UUID target) {
            return this.entries.object2IntEntrySet().stream().map(e -> new GossipEntry(target, (GossipType)e.getKey(), e.getIntValue()));
        }

        public void decay() {
            ObjectIterator it = this.entries.object2IntEntrySet().iterator();
            while (it.hasNext()) {
                Object2IntMap.Entry gossip = (Object2IntMap.Entry)it.next();
                int newValue = gossip.getIntValue() - ((GossipType)gossip.getKey()).decayPerDay;
                if (newValue < 2) {
                    it.remove();
                    continue;
                }
                gossip.setValue(newValue);
            }
        }

        public boolean isEmpty() {
            return this.entries.isEmpty();
        }

        public void makeSureValueIsntTooLowOrTooHigh(GossipType type) {
            int value = this.entries.getInt((Object)type);
            if (value > type.max) {
                this.entries.put((Object)type, type.max);
            }
            if (value < 2) {
                this.remove(type);
            }
        }

        public void remove(GossipType type) {
            this.entries.removeInt((Object)type);
        }
    }

    private record GossipEntry(UUID target, GossipType type, int value) {
        public static final Codec<GossipEntry> CODEC = RecordCodecBuilder.create(i -> i.group((App)UUIDUtil.CODEC.fieldOf("Target").forGetter(GossipEntry::target), (App)GossipType.CODEC.fieldOf("Type").forGetter(GossipEntry::type), (App)ExtraCodecs.POSITIVE_INT.fieldOf("Value").forGetter(GossipEntry::value)).apply((Applicative)i, GossipEntry::new));

        public int weightedValue() {
            return this.value * this.type.weight;
        }
    }
}

