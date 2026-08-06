/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Predicate
 *  com.google.common.base.Predicates
 *  com.google.common.collect.Iterators
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.Iterator;
import net.minecraft.core.IdMap;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class CrudeIncrementalIntIdentityHashBiMap<K>
implements IdMap<K> {
    private static final int NOT_FOUND = -1;
    private static final Object EMPTY_SLOT = null;
    private static final float LOADFACTOR = 0.8f;
    private @Nullable K[] keys;
    private int[] values;
    private @Nullable K[] byId;
    private int nextId;
    private int size;

    private CrudeIncrementalIntIdentityHashBiMap(int capacity) {
        this.keys = new Object[capacity];
        this.values = new int[capacity];
        this.byId = new Object[capacity];
    }

    private CrudeIncrementalIntIdentityHashBiMap(K[] keys, int[] values, K[] byId, int nextId, int size) {
        this.keys = keys;
        this.values = values;
        this.byId = byId;
        this.nextId = nextId;
        this.size = size;
    }

    public static <A> CrudeIncrementalIntIdentityHashBiMap<A> create(int initialCapacity) {
        return new CrudeIncrementalIntIdentityHashBiMap((int)((float)initialCapacity / 0.8f));
    }

    @Override
    public int getId(@Nullable K thing) {
        return this.getValue(this.indexOf(thing, this.hash(thing)));
    }

    @Override
    public @Nullable K byId(int id) {
        if (id < 0 || id >= this.byId.length) {
            return null;
        }
        return this.byId[id];
    }

    private int getValue(int index) {
        if (index == -1) {
            return -1;
        }
        return this.values[index];
    }

    public boolean contains(K key) {
        return this.getId(key) != -1;
    }

    public boolean contains(int id) {
        return this.byId(id) != null;
    }

    public int add(K key) {
        int value = this.nextId();
        this.addMapping(key, value);
        return value;
    }

    private int nextId() {
        while (this.nextId < this.byId.length && this.byId[this.nextId] != null) {
            ++this.nextId;
        }
        return this.nextId;
    }

    private void grow(int newSize) {
        K[] oldKeys = this.keys;
        int[] oldValues = this.values;
        CrudeIncrementalIntIdentityHashBiMap<K> resized = new CrudeIncrementalIntIdentityHashBiMap<K>(newSize);
        for (int i = 0; i < oldKeys.length; ++i) {
            if (oldKeys[i] == null) continue;
            resized.addMapping(oldKeys[i], oldValues[i]);
        }
        this.keys = resized.keys;
        this.values = resized.values;
        this.byId = resized.byId;
        this.nextId = resized.nextId;
        this.size = resized.size;
    }

    public void addMapping(K key, int id) {
        int minSize = Math.max(id, this.size + 1);
        if ((float)minSize >= (float)this.keys.length * 0.8f) {
            int newSize;
            for (newSize = this.keys.length << 1; newSize < id; newSize <<= 1) {
            }
            this.grow(newSize);
        }
        int index = this.findEmpty(this.hash(key));
        this.keys[index] = key;
        this.values[index] = id;
        this.byId[id] = key;
        ++this.size;
        if (id == this.nextId) {
            ++this.nextId;
        }
    }

    private int hash(@Nullable K key) {
        return (Mth.murmurHash3Mixer(System.identityHashCode(key)) & Integer.MAX_VALUE) % this.keys.length;
    }

    private int indexOf(@Nullable K key, int startFrom) {
        int i;
        for (i = startFrom; i < this.keys.length; ++i) {
            if (this.keys[i] == key) {
                return i;
            }
            if (this.keys[i] != EMPTY_SLOT) continue;
            return -1;
        }
        for (i = 0; i < startFrom; ++i) {
            if (this.keys[i] == key) {
                return i;
            }
            if (this.keys[i] != EMPTY_SLOT) continue;
            return -1;
        }
        return -1;
    }

    private int findEmpty(int startFrom) {
        int i;
        for (i = startFrom; i < this.keys.length; ++i) {
            if (this.keys[i] != EMPTY_SLOT) continue;
            return i;
        }
        for (i = 0; i < startFrom; ++i) {
            if (this.keys[i] != EMPTY_SLOT) continue;
            return i;
        }
        throw new RuntimeException("Overflowed :(");
    }

    @Override
    public Iterator<K> iterator() {
        return Iterators.filter((Iterator)Iterators.forArray((Object[])this.byId), (Predicate)Predicates.notNull());
    }

    public void clear() {
        Arrays.fill(this.keys, null);
        Arrays.fill(this.byId, null);
        this.nextId = 0;
        this.size = 0;
    }

    @Override
    public int size() {
        return this.size;
    }

    public CrudeIncrementalIntIdentityHashBiMap<K> copy() {
        return new CrudeIncrementalIntIdentityHashBiMap<Object>((Object[])this.keys.clone(), (int[])this.values.clone(), (Object[])this.byId.clone(), this.nextId, this.size);
    }
}

