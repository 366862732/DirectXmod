/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonSittingPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class DragonSittingScanningPhase
extends AbstractDragonSittingPhase {
    private static final int SITTING_SCANNING_IDLE_TICKS = 100;
    private static final int SITTING_ATTACK_Y_VIEW_RANGE = 10;
    private static final int SITTING_ATTACK_VIEW_RANGE = 20;
    private static final int SITTING_CHARGE_VIEW_RANGE = 150;
    private static final TargetingConditions CHARGE_TARGETING = TargetingConditions.forCombat().range(150.0);
    private final TargetingConditions scanTargeting = TargetingConditions.forCombat().range(20.0).selector((target, level) -> Math.abs(target.getY() - dragon.getY()) <= 10.0);
    private int scanningTime;

    public DragonSittingScanningPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    public void doServerTick(ServerLevel level) {
        ++this.scanningTime;
        Player attackTarget = level.getNearestPlayer(this.scanTargeting, this.dragon, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
        if (attackTarget != null) {
            if (this.scanningTime > 25) {
                this.dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_ATTACKING);
            } else {
                Vec3 aim = new Vec3(attackTarget.getX() - this.dragon.getX(), 0.0, attackTarget.getZ() - this.dragon.getZ()).normalize();
                Vec3 dir = new Vec3(Mth.sin(this.dragon.getYRot() * ((float)Math.PI / 180)), 0.0, -Mth.cos(this.dragon.getYRot() * ((float)Math.PI / 180))).normalize();
                float dot = (float)dir.dot(aim);
                float angle = (float)(Math.acos(dot) * 57.2957763671875) + 0.5f;
                if (angle < 0.0f || angle > 10.0f) {
                    float dist;
                    double xAttackDist = attackTarget.getX() - this.dragon.head.getX();
                    double zAttackDist = attackTarget.getZ() - this.dragon.head.getZ();
                    double yRotDelta = Mth.clamp(Mth.wrapDegrees(180.0 - Mth.atan2(xAttackDist, zAttackDist) * 57.2957763671875 - (double)this.dragon.getYRot()), -100.0, 100.0);
                    this.dragon.yRotA *= 0.8f;
                    float rotSpeed = dist = (float)Math.sqrt(xAttackDist * xAttackDist + zAttackDist * zAttackDist) + 1.0f;
                    if (dist > 40.0f) {
                        dist = 40.0f;
                    }
                    this.dragon.yRotA += (float)yRotDelta * (0.7f / dist / rotSpeed);
                    this.dragon.setYRot(this.dragon.getYRot() + this.dragon.yRotA);
                }
            }
        } else if (this.scanningTime >= 100) {
            attackTarget = level.getNearestPlayer(CHARGE_TARGETING, this.dragon, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.TAKEOFF);
            if (attackTarget != null) {
                this.dragon.getPhaseManager().setPhase(EnderDragonPhase.CHARGING_PLAYER);
                this.dragon.getPhaseManager().getPhase(EnderDragonPhase.CHARGING_PLAYER).setTarget(new Vec3(attackTarget.getX(), attackTarget.getY(), attackTarget.getZ()));
            }
        }
    }

    @Override
    public void begin() {
        this.scanningTime = 0;
    }

    public EnderDragonPhase<DragonSittingScanningPhase> getPhase() {
        return EnderDragonPhase.SITTING_SCANNING;
    }
}

