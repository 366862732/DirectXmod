/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMaps
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 */
package net.minecraft.stats;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.world.entity.player.Player;

public class StatsCounter {
    protected final Object2IntMap<Stat<?>> stats = Object2IntMaps.synchronize((Object2IntMap)new Object2IntOpenHashMap());

    public StatsCounter() {
        this.stats.defaultReturnValue(0);
    }

    public void increment(Player player, Stat<?> stat, int count) {
        int result = (int)Math.min((long)this.getValue(stat) + (long)count, Integer.MAX_VALUE);
        this.setValue(player, stat, result);
    }

    public void setValue(Player player, Stat<?> stat, int count) {
        this.stats.put(stat, count);
    }

    public <T> int getValue(StatType<T> type, T key) {
        return type.contains(key) ? this.getValue(type.get(key)) : 0;
    }

    public int getValue(Stat<?> stat) {
        return this.stats.getInt(stat);
    }
}

