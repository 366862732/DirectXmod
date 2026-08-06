/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;

public class CrossbowAttack<E extends Mob, T extends LivingEntity>
extends Behavior<E> {
    private static final int TIMEOUT = 1200;
    private int attackDelay;
    private CrossbowState crossbowState = CrossbowState.UNCHARGED;

    public CrossbowAttack() {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.ATTACK_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT)), 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E body) {
        LivingEntity attackTarget = CrossbowAttack.getAttackTarget(body);
        return ((LivingEntity)body).isHolding(Items.CROSSBOW) && BehaviorUtils.canSee(body, attackTarget) && BehaviorUtils.isWithinAttackRange(body, attackTarget, 0);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, E body, long timestamp) {
        return ((LivingEntity)body).getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && this.checkExtraStartConditions(level, body);
    }

    @Override
    protected void tick(ServerLevel level, E body, long timestamp) {
        LivingEntity target = CrossbowAttack.getAttackTarget(body);
        this.lookAtTarget((Mob)body, target);
        this.crossbowAttack(body, target);
    }

    @Override
    protected void stop(ServerLevel level, E body, long timestamp) {
        if (((LivingEntity)body).isUsingItem()) {
            ((LivingEntity)body).stopUsingItem();
        }
        if (((LivingEntity)body).isHolding(Items.CROSSBOW)) {
            ((CrossbowAttackMob)body).setChargingCrossbow(false);
            ((LivingEntity)body).getUseItem().set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        }
    }

    private void crossbowAttack(E body, LivingEntity target) {
        if (this.crossbowState == CrossbowState.UNCHARGED) {
            ((LivingEntity)body).startUsingItem(ProjectileUtil.getWeaponHoldingHand(body, Items.CROSSBOW));
            this.crossbowState = CrossbowState.CHARGING;
            ((CrossbowAttackMob)body).setChargingCrossbow(true);
        } else if (this.crossbowState == CrossbowState.CHARGING) {
            ItemStack useItem;
            int pullTime;
            if (!((LivingEntity)body).isUsingItem()) {
                this.crossbowState = CrossbowState.UNCHARGED;
            }
            if ((pullTime = ((LivingEntity)body).getTicksUsingItem()) >= CrossbowItem.getChargeDuration(useItem = ((LivingEntity)body).getUseItem(), body)) {
                ((LivingEntity)body).releaseUsingItem();
                this.crossbowState = CrossbowState.CHARGED;
                this.attackDelay = 20 + ((Entity)body).getRandom().nextInt(20);
                ((CrossbowAttackMob)body).setChargingCrossbow(false);
            }
        } else if (this.crossbowState == CrossbowState.CHARGED) {
            --this.attackDelay;
            if (this.attackDelay == 0) {
                this.crossbowState = CrossbowState.READY_TO_ATTACK;
            }
        } else if (this.crossbowState == CrossbowState.READY_TO_ATTACK) {
            ((RangedAttackMob)body).performRangedAttack(target, 1.0f);
            this.crossbowState = CrossbowState.UNCHARGED;
        }
    }

    private void lookAtTarget(Mob body, LivingEntity target) {
        body.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
    }

    private static LivingEntity getAttackTarget(LivingEntity body) {
        return body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }

    private static enum CrossbowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK;

    }
}

