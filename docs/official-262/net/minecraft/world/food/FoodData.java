/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.food;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FoodData {
    private static final int DEFAULT_TICK_TIMER = 0;
    private static final float DEFAULT_EXHAUSTION_LEVEL = 0.0f;
    private int foodLevel = 20;
    private float saturationLevel = 5.0f;
    private float exhaustionLevel;
    private int tickTimer;

    private void add(int food, float saturation) {
        this.foodLevel = Mth.clamp(food + this.foodLevel, 0, 20);
        this.saturationLevel = Mth.clamp(saturation + this.saturationLevel, 0.0f, (float)this.foodLevel);
    }

    public void eat(int food, float saturationModifier) {
        this.add(food, FoodConstants.saturationByModifier(food, saturationModifier));
    }

    public void eat(FoodProperties foodProperties) {
        this.add(foodProperties.nutrition(), foodProperties.saturation());
    }

    public void tick(ServerPlayer player) {
        boolean naturalRegen;
        ServerLevel level = player.level();
        Difficulty difficulty = level.getDifficulty();
        if (this.exhaustionLevel > 4.0f) {
            this.exhaustionLevel -= 4.0f;
            if (this.saturationLevel > 0.0f) {
                this.saturationLevel = Math.max(this.saturationLevel - 1.0f, 0.0f);
            } else if (difficulty != Difficulty.PEACEFUL) {
                this.foodLevel = Math.max(this.foodLevel - 1, 0);
            }
        }
        if ((naturalRegen = level.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION).booleanValue()) && this.saturationLevel > 0.0f && player.isHurt() && this.foodLevel >= 20) {
            ++this.tickTimer;
            if (this.tickTimer >= 10) {
                float saturationSpent = Math.min(this.saturationLevel, 6.0f);
                player.heal(saturationSpent / 6.0f);
                this.addExhaustion(saturationSpent);
                this.tickTimer = 0;
            }
        } else if (naturalRegen && this.foodLevel >= 18 && player.isHurt()) {
            ++this.tickTimer;
            if (this.tickTimer >= 80) {
                player.heal(1.0f);
                this.addExhaustion(6.0f);
                this.tickTimer = 0;
            }
        } else if (this.foodLevel <= 0) {
            ++this.tickTimer;
            if (this.tickTimer >= 80) {
                if (player.getHealth() > 10.0f || difficulty == Difficulty.HARD || player.getHealth() > 1.0f && difficulty == Difficulty.NORMAL) {
                    player.hurtServer(level, player.damageSources().starve(), 1.0f);
                }
                this.tickTimer = 0;
            }
        } else {
            this.tickTimer = 0;
        }
    }

    public void readAdditionalSaveData(ValueInput input) {
        this.foodLevel = input.getIntOr("foodLevel", 20);
        this.tickTimer = input.getIntOr("foodTickTimer", 0);
        this.saturationLevel = input.getFloatOr("foodSaturationLevel", 5.0f);
        this.exhaustionLevel = input.getFloatOr("foodExhaustionLevel", 0.0f);
    }

    public void addAdditionalSaveData(ValueOutput output) {
        output.putInt("foodLevel", this.foodLevel);
        output.putInt("foodTickTimer", this.tickTimer);
        output.putFloat("foodSaturationLevel", this.saturationLevel);
        output.putFloat("foodExhaustionLevel", this.exhaustionLevel);
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public boolean hasEnoughFood() {
        return (float)this.getFoodLevel() > 6.0f;
    }

    public boolean needsFood() {
        return this.foodLevel < 20;
    }

    public void addExhaustion(float amount) {
        this.exhaustionLevel = Math.min(this.exhaustionLevel + amount, 40.0f);
    }

    public float getSaturationLevel() {
        return this.saturationLevel;
    }

    public void setFoodLevel(int food) {
        this.foodLevel = food;
    }

    public void setSaturation(float saturation) {
        this.saturationLevel = saturation;
    }
}

