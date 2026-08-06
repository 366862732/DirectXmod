/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.Multimap
 */
package net.minecraft.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DependencySorter<K, V extends Entry<K>> {
    private final Map<K, V> contents = new HashMap();

    public DependencySorter<K, V> addEntry(K id, V value) {
        this.contents.put(id, value);
        return this;
    }

    private void visitDependenciesAndElement(Multimap<K, K> dependencies, Set<K> alreadyVisited, K id, BiConsumer<K, V> output) {
        if (!alreadyVisited.add(id)) {
            return;
        }
        dependencies.get(id).forEach(dependency -> this.visitDependenciesAndElement(dependencies, alreadyVisited, dependency, output));
        Entry current = (Entry)this.contents.get(id);
        if (current != null) {
            output.accept(id, current);
        }
    }

    private static <K> boolean isCyclic(Multimap<K, K> directDependencies, K from, K to) {
        Collection dependencies = directDependencies.get(to);
        if (dependencies.contains(from)) {
            return true;
        }
        return dependencies.stream().anyMatch(dep -> DependencySorter.isCyclic(directDependencies, from, dep));
    }

    private static <K> void addDependencyIfNotCyclic(Multimap<K, K> directDependencies, K from, K to) {
        if (!DependencySorter.isCyclic(directDependencies, from, to)) {
            directDependencies.put(from, to);
        }
    }

    public void orderByDependencies(BiConsumer<K, V> output) {
        HashMultimap directDependencies = HashMultimap.create();
        this.contents.forEach((arg_0, arg_1) -> DependencySorter.lambda$orderByDependencies$0((Multimap)directDependencies, arg_0, arg_1));
        this.contents.forEach((arg_0, arg_1) -> DependencySorter.lambda$orderByDependencies$2((Multimap)directDependencies, arg_0, arg_1));
        HashSet alreadyVisited = new HashSet();
        this.contents.keySet().forEach(arg_0 -> this.lambda$orderByDependencies$4((Multimap)directDependencies, alreadyVisited, output, arg_0));
    }

    private /* synthetic */ void lambda$orderByDependencies$4(Multimap directDependencies, Set alreadyVisited, BiConsumer output, Object topId) {
        this.visitDependenciesAndElement(directDependencies, alreadyVisited, topId, output);
    }

    private static /* synthetic */ void lambda$orderByDependencies$2(Multimap directDependencies, Object id, Entry value) {
        value.visitOptionalDependencies(dep -> DependencySorter.addDependencyIfNotCyclic(directDependencies, id, dep));
    }

    private static /* synthetic */ void lambda$orderByDependencies$0(Multimap directDependencies, Object id, Entry value) {
        value.visitRequiredDependencies(dep -> DependencySorter.addDependencyIfNotCyclic(directDependencies, id, dep));
    }

    public static interface Entry<K> {
        public void visitRequiredDependencies(Consumer<K> var1);

        public void visitOptionalDependencies(Consumer<K> var1);
    }
}

