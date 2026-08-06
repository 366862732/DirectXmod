/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  org.apache.commons.lang3.mutable.MutableInt
 */
package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.Graph;
import net.minecraft.util.Util;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;

public class FeatureSorter {
    public static <T> List<StepFeatureData> buildFeaturesPerStep(List<T> featureSources, Function<T, List<HolderSet<PlacedFeature>>> featureGetter, boolean tryReducingError) {
        Object2IntOpenHashMap featureIndex = new Object2IntOpenHashMap();
        MutableInt nextFeatureIndex = new MutableInt(0);
        record FeatureData(int featureIndex, int step, PlacedFeature feature) {
        }
        Comparator<FeatureData> featureDataComparator = Comparator.comparingInt(FeatureData::step).thenComparingInt(FeatureData::featureIndex);
        TreeMap<FeatureData, Set> edges = new TreeMap<FeatureData, Set>(featureDataComparator);
        int maxStep = 0;
        for (T featureSource : featureSources) {
            int i;
            ArrayList featureList = Lists.newArrayList();
            List<HolderSet<PlacedFeature>> featuresForStep = featureGetter.apply(featureSource);
            maxStep = Math.max(maxStep, featuresForStep.size());
            for (i = 0; i < featuresForStep.size(); ++i) {
                for (Holder featureSupplier : (HolderSet)featuresForStep.get(i)) {
                    PlacedFeature feature = (PlacedFeature)featureSupplier.value();
                    featureList.add(new FeatureData(featureIndex.computeIfAbsent((Object)feature, f -> nextFeatureIndex.getAndIncrement()), i, feature));
                }
            }
            for (i = 0; i < featureList.size(); ++i) {
                Set data = edges.computeIfAbsent((FeatureData)featureList.get(i), k -> new TreeSet(featureDataComparator));
                if (i >= featureList.size() - 1) continue;
                data.add((FeatureData)featureList.get(i + 1));
            }
        }
        TreeSet<FeatureData> discovered = new TreeSet<FeatureData>(featureDataComparator);
        TreeSet<FeatureData> currentlyVisiting = new TreeSet<FeatureData>(featureDataComparator);
        ArrayList sortedFeatures = Lists.newArrayList();
        for (FeatureData feature : edges.keySet()) {
            if (!currentlyVisiting.isEmpty()) {
                throw new IllegalStateException("You somehow broke the universe; DFS bork (iteration finished with non-empty in-progress vertex set");
            }
            if (discovered.contains(feature) || !Graph.depthFirstSearch(edges, discovered, currentlyVisiting, sortedFeatures::add, feature)) continue;
            if (tryReducingError) {
                int lastSize;
                ArrayList<T> reducedSources = new ArrayList<T>(featureSources);
                do {
                    lastSize = reducedSources.size();
                    ListIterator iterator = reducedSources.listIterator();
                    while (iterator.hasNext()) {
                        Object source = iterator.next();
                        iterator.remove();
                        try {
                            FeatureSorter.buildFeaturesPerStep(reducedSources, featureGetter, false);
                        }
                        catch (IllegalStateException e) {
                            continue;
                        }
                        iterator.add(source);
                    }
                } while (lastSize != reducedSources.size());
                throw new IllegalStateException("Feature order cycle found, involved sources: " + String.valueOf(reducedSources));
            }
            throw new IllegalStateException("Feature order cycle found");
        }
        Collections.reverse(sortedFeatures);
        ImmutableList.Builder features = ImmutableList.builder();
        int step = 0;
        while (step < maxStep) {
            int finalStep = step++;
            List<PlacedFeature> featuresInStep = sortedFeatures.stream().filter(p -> p.step() == finalStep).map(FeatureData::feature).collect(Collectors.toList());
            features.add((Object)new StepFeatureData(featuresInStep));
        }
        return features.build();
    }

    public record StepFeatureData(List<PlacedFeature> features, ToIntFunction<PlacedFeature> indexMapping) {
        private StepFeatureData(List<PlacedFeature> features) {
            this(features, Util.createIndexIdentityLookup(features));
        }
    }
}

