/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class ItemBasedSteering {
    private static final int MIN_BOOST_TIME = 140;
    private static final int MAX_BOOST_TIME = 700;
    private final SynchedEntityData entityData;
    private final EntityDataAccessor<Integer> boostTimeAccessor;
    private boolean boosting;
    private int boostTime;

    public ItemBasedSteering(SynchedEntityData entityData, EntityDataAccessor<Integer> boostTimeAccessor) {
        this.entityData = entityData;
        this.boostTimeAccessor = boostTimeAccessor;
    }

    public void onSynced() {
        this.boosting = true;
        this.boostTime = 0;
    }

    public boolean boost(RandomSource random) {
        if (this.boosting) {
            return false;
        }
        this.boosting = true;
        this.boostTime = 0;
        this.entityData.set(this.boostTimeAccessor, random.nextInt(841) + 140);
        return true;
    }

    public void tickBoost() {
        if (this.boosting && this.boostTime++ > this.boostTimeTotal()) {
            this.boosting = false;
        }
    }

    public float boostFactor() {
        if (this.boosting) {
            return 1.0f + 1.15f * Mth.sin((float)this.boostTime / (float)this.boostTimeTotal() * (float)Math.PI);
        }
        return 1.0f;
    }

    private int boostTimeTotal() {
        return this.entityData.get(this.boostTimeAccessor);
    }
}

