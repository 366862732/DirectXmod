/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonHoldingPatternPhase
extends AbstractDragonPhaseInstance {
    private static final TargetingConditions NEW_TARGET_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight();
    private @Nullable Path currentPath;
    private @Nullable Vec3 targetLocation;
    private boolean clockwise;

    public DragonHoldingPatternPhase(EnderDragon dragon) {
        super(dragon);
    }

    public EnderDragonPhase<DragonHoldingPatternPhase> getPhase() {
        return EnderDragonPhase.HOLDING_PATTERN;
    }

    @Override
    public void doServerTick(ServerLevel level) {
        double distToTarget;
        double d = distToTarget = this.targetLocation == null ? 0.0 : this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
        if (distToTarget < 100.0 || distToTarget > 22500.0 || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
            this.findNewTarget(level);
        }
    }

    @Override
    public void begin() {
        this.currentPath = null;
        this.targetLocation = null;
    }

    @Override
    public @Nullable Vec3 getFlyTargetLocation() {
        return this.targetLocation;
    }

    private void findNewTarget(ServerLevel level) {
        if (this.currentPath != null && this.currentPath.isDone()) {
            int crystals;
            BlockPos egg = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(this.dragon.getFightOrigin()));
            int n = crystals = this.dragon.getDragonFight() == null ? 0 : this.dragon.getDragonFight().aliveCrystals();
            if (this.dragon.getRandom().nextInt(crystals + 3) == 0) {
                this.dragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING_APPROACH);
                return;
            }
            Player playerNearestToEgg = level.getNearestPlayer(NEW_TARGET_TARGETING, this.dragon, (double)egg.getX(), (double)egg.getY(), egg.getZ());
            double distSqr = playerNearestToEgg != null ? egg.distToCenterSqr(playerNearestToEgg.position()) / 512.0 : 64.0;
            if (playerNearestToEgg != null && (this.dragon.getRandom().nextInt((int)(distSqr + 2.0)) == 0 || this.dragon.getRandom().nextInt(crystals + 2) == 0)) {
                this.strafePlayer(playerNearestToEgg);
                return;
            }
        }
        if (this.currentPath == null || this.currentPath.isDone()) {
            int currentNodeIndex;
            int targetNodeIndex = currentNodeIndex = this.dragon.findClosestNode();
            if (this.dragon.getRandom().nextInt(8) == 0) {
                this.clockwise = !this.clockwise;
                targetNodeIndex += 6;
            }
            targetNodeIndex = this.clockwise ? ++targetNodeIndex : --targetNodeIndex;
            if (this.dragon.getDragonFight() == null || this.dragon.getDragonFight().aliveCrystals() < 0) {
                targetNodeIndex -= 12;
                targetNodeIndex &= 7;
                targetNodeIndex += 12;
            } else if ((targetNodeIndex %= 12) < 0) {
                targetNodeIndex += 12;
            }
            this.currentPath = this.dragon.findPath(currentNodeIndex, targetNodeIndex, null);
            if (this.currentPath != null) {
                this.currentPath.advance();
            }
        }
        this.navigateToNextPathNode();
    }

    private void strafePlayer(Player playerNearestToEgg) {
        this.dragon.getPhaseManager().setPhase(EnderDragonPhase.STRAFE_PLAYER);
        this.dragon.getPhaseManager().getPhase(EnderDragonPhase.STRAFE_PLAYER).setTarget(playerNearestToEgg);
    }

    private void navigateToNextPathNode() {
        if (this.currentPath != null && !this.currentPath.isDone()) {
            double yTarget;
            BlockPos current = this.currentPath.getNextNodePos();
            this.currentPath.advance();
            double xTarget = current.getX();
            double zTarget = current.getZ();
            while ((yTarget = (double)((float)current.getY() + this.dragon.getRandom().nextFloat() * 20.0f)) < (double)current.getY()) {
            }
            this.targetLocation = new Vec3(xTarget, yTarget, zTarget);
        }
    }

    @Override
    public void onCrystalDestroyed(EndCrystal crystal, BlockPos pos, DamageSource source, @Nullable Player player) {
        if (player != null && this.dragon.canAttack(player)) {
            this.strafePlayer(player);
        }
    }
}

