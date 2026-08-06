/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import net.minecraft.world.entity.EntitySpawnReason;

public record EntitySpawnRequest(EntitySpawnReason reason, boolean ignoreChecks) {
}

