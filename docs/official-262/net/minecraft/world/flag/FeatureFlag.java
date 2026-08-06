/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.flag;

import net.minecraft.world.flag.FeatureFlagUniverse;

public class FeatureFlag {
    final FeatureFlagUniverse universe;
    final long mask;

    FeatureFlag(FeatureFlagUniverse universe, int bit) {
        this.universe = universe;
        this.mask = 1L << bit;
    }
}

