/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterators
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.RandomSource;

public class ShufflingList<U>
implements Iterable<U> {
    protected final List<WeightedEntry<U>> entries;
    private final RandomSource random = RandomSource.create();

    public ShufflingList() {
        this.entries = Lists.newArrayList();
    }

    private ShufflingList(List<WeightedEntry<U>> entries) {
        this.entries = Lists.newArrayList(entries);
    }

    public static <U> Codec<ShufflingList<U>> codec(Codec<U> elementCodec) {
        return WeightedEntry.codec(elementCodec).listOf().xmap(ShufflingList::new, l -> l.entries);
    }

    public ShufflingList<U> add(U data, int weight) {
        this.entries.add(new WeightedEntry<U>(data, weight));
        return this;
    }

    public ShufflingList<U> shuffle() {
        this.entries.forEach(k -> k.setRandom(this.random.nextFloat()));
        this.entries.sort(Comparator.comparingDouble(WeightedEntry::getRandWeight));
        return this;
    }

    public Stream<U> stream() {
        return this.entries.stream().map(WeightedEntry::getData);
    }

    @Override
    public Iterator<U> iterator() {
        return Iterators.transform(this.entries.iterator(), WeightedEntry::getData);
    }

    public String toString() {
        return "ShufflingList[" + String.valueOf(this.entries) + "]";
    }

    public static class WeightedEntry<T> {
        private final T data;
        private final int weight;
        private double randWeight;

        private WeightedEntry(T data, int weight) {
            this.weight = weight;
            this.data = data;
        }

        private double getRandWeight() {
            return this.randWeight;
        }

        private void setRandom(float random) {
            this.randWeight = -Math.pow(random, 1.0f / (float)this.weight);
        }

        public T getData() {
            return this.data;
        }

        public int getWeight() {
            return this.weight;
        }

        public String toString() {
            return this.weight + ":" + String.valueOf(this.data);
        }

        public static <E> Codec<WeightedEntry<E>> codec(final Codec<E> elementCodec) {
            return new Codec<WeightedEntry<E>>(){

                public <T> DataResult<Pair<WeightedEntry<E>, T>> decode(DynamicOps<T> ops, T input) {
                    Dynamic map = new Dynamic(ops, input);
                    return map.get("data").flatMap(arg_0 -> ((Codec)elementCodec).parse(arg_0)).map(data -> new WeightedEntry<Object>(data, map.get("weight").asInt(1))).map(r -> Pair.of((Object)r, (Object)ops.empty()));
                }

                public <T> DataResult<T> encode(WeightedEntry<E> input, DynamicOps<T> ops, T prefix) {
                    return ops.mapBuilder().add("weight", ops.createInt(input.weight)).add("data", elementCodec.encodeStart(ops, input.data)).build(prefix);
                }
            };
        }
    }
}

