/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.HashCommon
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.flag;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagUniverse;
import org.jspecify.annotations.Nullable;

public final class FeatureFlagSet {
    private static final FeatureFlagSet EMPTY = new FeatureFlagSet(null, 0L);
    public static final int MAX_CONTAINER_SIZE = 64;
    private final @Nullable FeatureFlagUniverse universe;
    private final long mask;

    private FeatureFlagSet(@Nullable FeatureFlagUniverse universe, long mask) {
        this.universe = universe;
        this.mask = mask;
    }

    static FeatureFlagSet create(FeatureFlagUniverse universe, Collection<FeatureFlag> flags) {
        if (flags.isEmpty()) {
            return EMPTY;
        }
        long mask = FeatureFlagSet.computeMask(universe, 0L, flags);
        return new FeatureFlagSet(universe, mask);
    }

    public static FeatureFlagSet of() {
        return EMPTY;
    }

    public static FeatureFlagSet of(FeatureFlag flag) {
        return new FeatureFlagSet(flag.universe, flag.mask);
    }

    public static FeatureFlagSet of(FeatureFlag flag, FeatureFlag ... flags) {
        long mask = flags.length == 0 ? flag.mask : FeatureFlagSet.computeMask(flag.universe, flag.mask, Arrays.asList(flags));
        return new FeatureFlagSet(flag.universe, mask);
    }

    private static long computeMask(FeatureFlagUniverse universe, long mask, Iterable<FeatureFlag> flags) {
        for (FeatureFlag f : flags) {
            if (universe != f.universe) {
                throw new IllegalStateException("Mismatched feature universe, expected '" + String.valueOf(universe) + "', but got '" + String.valueOf(f.universe) + "'");
            }
            mask |= f.mask;
        }
        return mask;
    }

    public boolean contains(FeatureFlag flag) {
        if (this.universe != flag.universe) {
            return false;
        }
        return (this.mask & flag.mask) != 0L;
    }

    public boolean isEmpty() {
        return this.equals(EMPTY);
    }

    public boolean isSubsetOf(FeatureFlagSet set) {
        if (this.universe == null) {
            return true;
        }
        if (this.universe != set.universe) {
            return false;
        }
        return (this.mask & (set.mask ^ 0xFFFFFFFFFFFFFFFFL)) == 0L;
    }

    public boolean intersects(FeatureFlagSet set) {
        if (this.universe == null || set.universe == null || this.universe != set.universe) {
            return false;
        }
        return (this.mask & set.mask) != 0L;
    }

    public FeatureFlagSet join(FeatureFlagSet other) {
        if (this.universe == null) {
            return other;
        }
        if (other.universe == null) {
            return this;
        }
        if (this.universe != other.universe) {
            throw new IllegalArgumentException("Mismatched set elements: '" + String.valueOf(this.universe) + "' != '" + String.valueOf(other.universe) + "'");
        }
        return new FeatureFlagSet(this.universe, this.mask | other.mask);
    }

    public FeatureFlagSet subtract(FeatureFlagSet other) {
        if (this.universe == null || other.universe == null) {
            return this;
        }
        if (this.universe != other.universe) {
            throw new IllegalArgumentException("Mismatched set elements: '" + String.valueOf(this.universe) + "' != '" + String.valueOf(other.universe) + "'");
        }
        long newMask = this.mask & (other.mask ^ 0xFFFFFFFFFFFFFFFFL);
        if (newMask == 0L) {
            return EMPTY;
        }
        return new FeatureFlagSet(this.universe, newMask);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FeatureFlagSet)) return false;
        FeatureFlagSet that = (FeatureFlagSet)o;
        if (this.universe != that.universe) return false;
        if (this.mask != that.mask) return false;
        return true;
    }

    public int hashCode() {
        return (int)HashCommon.mix((long)this.mask);
    }
}

