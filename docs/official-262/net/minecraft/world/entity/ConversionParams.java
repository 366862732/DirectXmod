/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import net.minecraft.world.entity.ConversionType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

public record ConversionParams(ConversionType type, boolean keepEquipment, boolean preserveCanPickUpLoot, @Nullable PlayerTeam team) {
    public static ConversionParams single(Mob mob, boolean keepEquipment, boolean preserveCanPickUpLoot) {
        return new ConversionParams(ConversionType.SINGLE, keepEquipment, preserveCanPickUpLoot, mob.getTeam());
    }

    @FunctionalInterface
    public static interface AfterConversion<T extends Mob> {
        public void finalizeConversion(T var1);
    }
}

