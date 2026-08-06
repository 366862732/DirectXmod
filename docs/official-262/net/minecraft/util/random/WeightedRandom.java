/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.random;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

public class WeightedRandom {
    private WeightedRandom() {
    }

    public static <T> int getTotalWeight(List<T> items, ToIntFunction<T> weightGetter) {
        long totalWeight = 0L;
        for (T item : items) {
            totalWeight += (long)weightGetter.applyAsInt(item);
        }
        if (totalWeight > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
        }
        return (int)totalWeight;
    }

    public static <T> Optional<T> getRandomItem(RandomSource random, List<T> items, int totalWeight, ToIntFunction<T> weightGetter) {
        if (totalWeight < 0) {
            throw Util.pauseInIde(new IllegalArgumentException("Negative total weight in getRandomItem"));
        }
        if (totalWeight == 0) {
            return Optional.empty();
        }
        int selection = random.nextInt(totalWeight);
        return WeightedRandom.getWeightedItem(items, selection, weightGetter);
    }

    public static <T> Optional<T> getWeightedItem(List<T> items, int index, ToIntFunction<T> weightGetter) {
        for (T item : items) {
            if ((index -= weightGetter.applyAsInt(item)) >= 0) continue;
            return Optional.of(item);
        }
        return Optional.empty();
    }

    public static <T> Optional<T> getRandomItem(RandomSource random, List<T> items, ToIntFunction<T> weightGetter) {
        return WeightedRandom.getRandomItem(random, items, WeightedRandom.getTotalWeight(items, weightGetter), weightGetter);
    }
}

