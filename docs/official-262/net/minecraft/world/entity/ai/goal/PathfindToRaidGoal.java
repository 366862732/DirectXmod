/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 */
package net.minecraft.world.entity.ai.goal;

import com.google.common.collect.Sets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.phys.Vec3;

public class PathfindToRaidGoal<T extends Raider>
extends Goal {
    private static final int RECRUITMENT_SEARCH_TICK_DELAY = 20;
    private static final float SPEED_MODIFIER = 1.0f;
    private final T mob;
    private int recruitmentTick;

    public PathfindToRaidGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return ((Mob)this.mob).getTarget() == null && !((Entity)this.mob).hasControllingPassenger() && ((Raider)this.mob).hasActiveRaid() && !((Raider)this.mob).getCurrentRaid().isOver() && !PathfindToRaidGoal.getServerLevel(((Entity)this.mob).level()).isVillage(((Entity)this.mob).blockPosition());
    }

    @Override
    public boolean canContinueToUse() {
        return ((Raider)this.mob).hasActiveRaid() && !((Raider)this.mob).getCurrentRaid().isOver() && !PathfindToRaidGoal.getServerLevel(((Entity)this.mob).level()).isVillage(((Entity)this.mob).blockPosition());
    }

    @Override
    public void tick() {
        if (((Raider)this.mob).hasActiveRaid()) {
            Vec3 posTowards;
            Raid raid = ((Raider)this.mob).getCurrentRaid();
            if (((Raider)this.mob).tickCount > this.recruitmentTick) {
                this.recruitmentTick = ((Raider)this.mob).tickCount + 20;
                this.recruitNearby(raid);
            }
            if (!((PathfinderMob)this.mob).isPathFinding() && (posTowards = DefaultRandomPos.getPosTowards(this.mob, 15, 4, Vec3.atBottomCenterOf(raid.getCenter()), 1.5707963705062866)) != null) {
                ((Mob)this.mob).getNavigation().moveTo(posTowards.x, posTowards.y, posTowards.z, 1.0);
            }
        }
    }

    private void recruitNearby(Raid raid) {
        if (raid.isActive()) {
            ServerLevel level = PathfindToRaidGoal.getServerLevel(((Entity)this.mob).level());
            HashSet raidersToAdd = Sets.newHashSet();
            List<Raider> raidersNearby = level.getEntitiesOfClass(Raider.class, ((Entity)this.mob).getBoundingBox().inflate(16.0), mob -> !mob.hasActiveRaid() && Raids.canJoinRaid(mob));
            raidersToAdd.addAll(raidersNearby);
            for (Raider raider : raidersToAdd) {
                raid.joinRaid(level, raid.getGroupsSpawned(), raider, null, true);
            }
        }
    }
}

