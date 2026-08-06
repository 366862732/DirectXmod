/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  org.slf4j.Logger
 */
package net.minecraft.world.flag;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlagUniverse;
import org.slf4j.Logger;

public class FeatureFlagRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final FeatureFlagUniverse universe;
    private final Map<Identifier, FeatureFlag> names;
    private final FeatureFlagSet allFlags;

    private FeatureFlagRegistry(FeatureFlagUniverse universe, FeatureFlagSet allFlags, Map<Identifier, FeatureFlag> names) {
        this.universe = universe;
        this.names = names;
        this.allFlags = allFlags;
    }

    public boolean isSubset(FeatureFlagSet set) {
        return set.isSubsetOf(this.allFlags);
    }

    public FeatureFlagSet allFlags() {
        return this.allFlags;
    }

    public FeatureFlagSet fromNames(Iterable<Identifier> flagIds) {
        return this.fromNames(flagIds, flagId -> LOGGER.warn("Unknown feature flag: {}", flagId));
    }

    public FeatureFlagSet subset(FeatureFlag ... flags) {
        return FeatureFlagSet.create(this.universe, Arrays.asList(flags));
    }

    public FeatureFlagSet fromNames(Iterable<Identifier> flagIds, Consumer<Identifier> unknownFlags) {
        Set flags = Sets.newIdentityHashSet();
        for (Identifier flagId : flagIds) {
            FeatureFlag flag = this.names.get(flagId);
            if (flag == null) {
                unknownFlags.accept(flagId);
                continue;
            }
            flags.add(flag);
        }
        return FeatureFlagSet.create(this.universe, flags);
    }

    public Set<Identifier> toNames(FeatureFlagSet set) {
        HashSet<Identifier> result = new HashSet<Identifier>();
        this.names.forEach((id, flag) -> {
            if (set.contains((FeatureFlag)flag)) {
                result.add((Identifier)id);
            }
        });
        return result;
    }

    public Codec<FeatureFlagSet> codec() {
        return Identifier.CODEC.listOf().comapFlatMap(ids -> {
            HashSet unknownIds = new HashSet();
            FeatureFlagSet result = this.fromNames((Iterable<Identifier>)ids, unknownIds::add);
            if (!unknownIds.isEmpty()) {
                return DataResult.error(() -> "Unknown feature ids: " + String.valueOf(unknownIds), (Object)result);
            }
            return DataResult.success((Object)result);
        }, set -> List.copyOf(this.toNames((FeatureFlagSet)set)));
    }

    public static class Builder {
        private final FeatureFlagUniverse universe;
        private int id;
        private final Map<Identifier, FeatureFlag> flags = new LinkedHashMap<Identifier, FeatureFlag>();

        public Builder(String universeId) {
            this.universe = new FeatureFlagUniverse(universeId);
        }

        public FeatureFlag createVanilla(String name) {
            return this.create(Identifier.withDefaultNamespace(name));
        }

        public FeatureFlag create(Identifier name) {
            FeatureFlag result;
            FeatureFlag previous;
            if (this.id >= 64) {
                throw new IllegalStateException("Too many feature flags");
            }
            if ((previous = this.flags.put(name, result = new FeatureFlag(this.universe, this.id++))) != null) {
                throw new IllegalStateException("Duplicate feature flag " + String.valueOf(name));
            }
            return result;
        }

        public FeatureFlagRegistry build() {
            FeatureFlagSet allValues = FeatureFlagSet.create(this.universe, this.flags.values());
            return new FeatureFlagRegistry(this.universe, allValues, Map.copyOf(this.flags));
        }
    }
}

