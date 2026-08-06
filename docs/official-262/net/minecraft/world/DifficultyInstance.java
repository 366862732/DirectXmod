/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.concurrent.Immutable
 */
package net.minecraft.world;

import javax.annotation.concurrent.Immutable;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;

@Immutable
public class DifficultyInstance {
    private static final float DIFFICULTY_TIME_GLOBAL_OFFSET = -72000.0f;
    private static final float MAX_DIFFICULTY_TIME_GLOBAL = 1440000.0f;
    private static final float MAX_DIFFICULTY_TIME_LOCAL = 3600000.0f;
    private final Difficulty base;
    private final float effectiveDifficulty;

    public DifficultyInstance(Difficulty base, long totalGameTime, long localGameTime, float moonBrightness) {
        this.base = base;
        this.effectiveDifficulty = this.calculateDifficulty(base, totalGameTime, localGameTime, moonBrightness);
    }

    public Difficulty getDifficulty() {
        return this.base;
    }

    public float getEffectiveDifficulty() {
        return this.effectiveDifficulty;
    }

    public boolean isHard() {
        return this.effectiveDifficulty >= (float)Difficulty.HARD.ordinal();
    }

    public boolean isHarderThan(float requiredDifficulty) {
        return this.effectiveDifficulty > requiredDifficulty;
    }

    public float getSpecialMultiplier() {
        if (this.effectiveDifficulty < 2.0f) {
            return 0.0f;
        }
        if (this.effectiveDifficulty > 4.0f) {
            return 1.0f;
        }
        return (this.effectiveDifficulty - 2.0f) / 2.0f;
    }

    private float calculateDifficulty(Difficulty base, long totalGameTime, long localGameTime, float moonBrightness) {
        if (base == Difficulty.PEACEFUL) {
            return 0.0f;
        }
        boolean isHard = base == Difficulty.HARD;
        float scale = 0.75f;
        float globalScale = Mth.clamp(((float)totalGameTime + -72000.0f) / 1440000.0f, 0.0f, 1.0f) * 0.25f;
        scale += globalScale;
        float localScale = 0.0f;
        localScale += Mth.clamp((float)localGameTime / 3600000.0f, 0.0f, 1.0f) * (isHard ? 1.0f : 0.75f);
        localScale += Mth.clamp(moonBrightness * 0.25f, 0.0f, globalScale);
        if (base == Difficulty.EASY) {
            localScale *= 0.5f;
        }
        return (float)base.getId() * (scale += localScale);
    }
}

