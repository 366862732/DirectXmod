/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Iterators
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 */
package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Util;

public class ClassInstanceMultiMap<T>
extends AbstractCollection<T> {
    private final Map<Class<?>, List<T>> byClass = Maps.newHashMap();
    private final Class<T> baseClass;
    private final List<T> allInstances = Lists.newArrayList();

    public ClassInstanceMultiMap(Class<T> baseClass) {
        this.baseClass = baseClass;
        this.byClass.put(baseClass, this.allInstances);
    }

    @Override
    public boolean add(T instance) {
        boolean success = false;
        for (Map.Entry<Class<?>, List<T>> entry : this.byClass.entrySet()) {
            if (!entry.getKey().isInstance(instance)) continue;
            success |= entry.getValue().add(instance);
        }
        return success;
    }

    @Override
    public boolean remove(Object object) {
        boolean success = false;
        for (Map.Entry<Class<?>, List<T>> entry : this.byClass.entrySet()) {
            if (!entry.getKey().isInstance(object)) continue;
            List<T> list = entry.getValue();
            success |= list.remove(object);
        }
        return success;
    }

    @Override
    public boolean contains(Object o) {
        return this.find(o.getClass()).contains(o);
    }

    public <S> Collection<S> find(Class<S> index) {
        if (!this.baseClass.isAssignableFrom(index)) {
            throw new IllegalArgumentException("Don't know how to search for " + String.valueOf(index));
        }
        List instances = this.byClass.computeIfAbsent(index, k -> this.allInstances.stream().filter(k::isInstance).collect(Util.toMutableList()));
        return Collections.unmodifiableCollection(instances);
    }

    @Override
    public Iterator<T> iterator() {
        if (this.allInstances.isEmpty()) {
            return Collections.emptyIterator();
        }
        return Iterators.unmodifiableIterator(this.allInstances.iterator());
    }

    public List<T> getAllInstances() {
        return ImmutableList.copyOf(this.allInstances);
    }

    @Override
    public int size() {
        return this.allInstances.size();
    }
}

