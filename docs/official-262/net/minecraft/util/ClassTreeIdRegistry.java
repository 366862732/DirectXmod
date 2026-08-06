/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 */
package net.minecraft.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.util.Util;

public class ClassTreeIdRegistry {
    public static final int NO_ID_VALUE = -1;
    private final Object2IntMap<Class<?>> classToLastIdCache = (Object2IntMap)Util.make(new Object2IntOpenHashMap(), map -> map.defaultReturnValue(-1));

    public int getLastIdFor(Class<?> clazz) {
        int id = this.classToLastIdCache.getInt(clazz);
        if (id != -1) {
            return id;
        }
        Class<?> superclass = clazz;
        while ((superclass = superclass.getSuperclass()) != Object.class) {
            int newId = this.classToLastIdCache.getInt(superclass);
            if (newId == -1) continue;
            return newId;
        }
        return -1;
    }

    public int getCount(Class<?> clazz) {
        return this.getLastIdFor(clazz) + 1;
    }

    public int define(Class<?> clazz) {
        int id = this.getLastIdFor(clazz);
        int nextId = id == -1 ? 0 : id + 1;
        this.classToLastIdCache.put(clazz, nextId);
        return nextId;
    }
}

