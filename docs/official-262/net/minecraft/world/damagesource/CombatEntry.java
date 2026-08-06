/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.damagesource;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.FallLocation;
import org.jspecify.annotations.Nullable;

public record CombatEntry(DamageSource source, float damage, @Nullable FallLocation fallLocation, float fallDistance) {
}

