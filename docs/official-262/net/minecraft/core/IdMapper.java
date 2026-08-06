/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterators
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.objects.Reference2IntMap
 *  it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.IdMap;
import org.jspecify.annotations.Nullable;

public class IdMapper<T>
implements IdMap<T> {
    private int nextId;
    private final Reference2IntMap<T> tToId;
    private final List<T> idToT;

    public IdMapper() {
        this(512);
    }

    public IdMapper(int expectedSize) {
        this.idToT = Lists.newArrayListWithExpectedSize((int)expectedSize);
        this.tToId = new Reference2IntOpenHashMap(expectedSize);
        this.tToId.defaultReturnValue(-1);
    }

    public void addMapping(T thing, int id) {
        this.tToId.put(thing, id);
        while (this.idToT.size() <= id) {
            this.idToT.add(null);
        }
        this.idToT.set(id, thing);
        if (this.nextId <= id) {
            this.nextId = id + 1;
        }
    }

    public void add(T thing) {
        this.addMapping(thing, this.nextId);
    }

    @Override
    public int getId(T thing) {
        return this.tToId.getInt(thing);
    }

    @Override
    public final @Nullable T byId(int id) {
        if (id >= 0 && id < this.idToT.size()) {
            return this.idToT.get(id);
        }
        return null;
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.filter(this.idToT.iterator(), Objects::nonNull);
    }

    public boolean contains(int id) {
        return this.byId(id) != null;
    }

    @Override
    public int size() {
        return this.tToId.size();
    }
}

