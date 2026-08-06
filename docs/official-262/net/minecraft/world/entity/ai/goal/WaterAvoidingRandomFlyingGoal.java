/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.goal;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class WaterAvoidingRandomFlyingGoal
extends WaterAvoidingRandomStrollGoal {
    public WaterAvoidingRandomFlyingGoal(PathfinderMob mob, double speedModifier) {
        super(mob, speedModifier);
    }

    @Override
    protected @Nullable Vec3 getPosition() {
        Vec3 wanderDirection = this.mob.getViewVector(0.0f);
        int xzDist = 8;
        Vec3 groundBasedPosition = HoverRandomPos.getPos(this.mob, 8, 7, wanderDirection.x, wanderDirection.z, 1.5707964f, 3, 1);
        if (groundBasedPosition != null) {
            return groundBasedPosition;
        }
        return AirAndWaterRandomPos.getPos(this.mob, 8, 4, -2, wanderDirection.x, wanderDirection.z, 1.5707963705062866);
    }
}

