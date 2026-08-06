/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.stats;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;

public class StatType<T>
implements Iterable<Stat<T>> {
    private final Registry<T> registry;
    private final Map<T, Stat<T>> map = new IdentityHashMap<T, Stat<T>>();
    private final Component displayName;
    private final StreamCodec<RegistryFriendlyByteBuf, Stat<T>> streamCodec;

    public StatType(Registry<T> registry, Component displayName) {
        this.registry = registry;
        this.displayName = displayName;
        this.streamCodec = ByteBufCodecs.registry(registry.key()).map(this::get, Stat::getValue);
    }

    public StreamCodec<RegistryFriendlyByteBuf, Stat<T>> streamCodec() {
        return this.streamCodec;
    }

    public boolean contains(T key) {
        return this.map.containsKey(key);
    }

    public Stat<T> get(T argument, StatFormatter formatter) {
        return this.map.computeIfAbsent(argument, t -> new Stat<Object>(this, t, formatter));
    }

    public Registry<T> getRegistry() {
        return this.registry;
    }

    @Override
    public Iterator<Stat<T>> iterator() {
        return this.map.values().iterator();
    }

    public Stat<T> get(T argument) {
        return this.get(argument, StatFormatter.DEFAULT);
    }

    public Component getDisplayName() {
        return this.displayName;
    }
}

