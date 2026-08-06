/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 */
package net.minecraft.util;

import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class Graph {
    private Graph() {
    }

    public static <T> boolean depthFirstSearch(Map<T, Set<T>> edges, Set<T> discovered, Set<T> currentlyVisiting, Consumer<T> reverseTopologicalOrder, T current) {
        if (discovered.contains(current)) {
            return false;
        }
        if (currentlyVisiting.contains(current)) {
            return true;
        }
        currentlyVisiting.add(current);
        for (Object next : (Set)edges.getOrDefault(current, (Set<T>)ImmutableSet.of())) {
            if (!Graph.depthFirstSearch(edges, discovered, currentlyVisiting, reverseTopologicalOrder, next)) continue;
            return true;
        }
        currentlyVisiting.remove(current);
        discovered.add(current);
        reverseTopologicalOrder.accept(current);
        return false;
    }
}

