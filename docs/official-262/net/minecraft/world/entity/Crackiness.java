/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import net.minecraft.world.item.ItemStack;

public class Crackiness {
    public static final Crackiness GOLEM = new Crackiness(0.75f, 0.5f, 0.25f);
    public static final Crackiness WOLF_ARMOR = new Crackiness(0.95f, 0.69f, 0.32f);
    private final float fractionLow;
    private final float fractionMedium;
    private final float fractionHigh;

    private Crackiness(float fractionLow, float fractionMedium, float fractionHigh) {
        this.fractionLow = fractionLow;
        this.fractionMedium = fractionMedium;
        this.fractionHigh = fractionHigh;
    }

    public Level byFraction(float fraction) {
        if (fraction < this.fractionHigh) {
            return Level.HIGH;
        }
        if (fraction < this.fractionMedium) {
            return Level.MEDIUM;
        }
        if (fraction < this.fractionLow) {
            return Level.LOW;
        }
        return Level.NONE;
    }

    public Level byDamage(ItemStack item) {
        if (!item.isDamageableItem()) {
            return Level.NONE;
        }
        return this.byDamage(item.getDamageValue(), item.getMaxDamage());
    }

    public Level byDamage(int damage, int maxDamage) {
        return this.byFraction((float)(maxDamage - damage) / (float)maxDamage);
    }

    public static enum Level {
        NONE,
        LOW,
        MEDIUM,
        HIGH;

    }
}

