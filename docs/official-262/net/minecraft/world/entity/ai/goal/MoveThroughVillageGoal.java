/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.goal;

import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MoveThroughVillageGoal
extends Goal {
    protected final PathfinderMob mob;
    private final double speedModifier;
    private @Nullable Path path;
    private BlockPos poiPos;
    private final boolean onlyAtNight;
    private final List<BlockPos> visited = Lists.newArrayList();
    private final int distanceToPoi;
    private final BooleanSupplier canDealWithDoors;

    public MoveThroughVillageGoal(PathfinderMob mob, double speedModifier, boolean onlyAtNight, int distanceToPoi, BooleanSupplier canDealWithDoors) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.onlyAtNight = onlyAtNight;
        this.distanceToPoi = distanceToPoi;
        this.canDealWithDoors = canDealWithDoors;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        if (!GoalUtils.hasGroundPathNavigation(mob)) {
            throw new IllegalArgumentException("Unsupported mob for MoveThroughVillageGoal");
        }
    }

    @Override
    public boolean canUse() {
        BlockPos pos;
        if (!GoalUtils.hasGroundPathNavigation(this.mob)) {
            return false;
        }
        this.updateVisited();
        if (this.onlyAtNight && this.mob.level().isBrightOutside()) {
            return false;
        }
        ServerLevel level = (ServerLevel)this.mob.level();
        if (!level.isCloseToVillage(pos = this.mob.blockPosition(), 6)) {
            return false;
        }
        Vec3 landPos = LandRandomPos.getPos(this.mob, 15, 7, p -> {
            if (!level.isVillage((BlockPos)p)) {
                return Double.NEGATIVE_INFINITY;
            }
            Optional<BlockPos> newPoiPos = level.getPoiManager().find(e -> e.is(PoiTypeTags.VILLAGE), this::hasNotVisited, (BlockPos)p, 10, PoiManager.Occupancy.IS_OCCUPIED);
            return newPoiPos.map(poiPos -> -poiPos.distSqr(pos)).orElse(Double.NEGATIVE_INFINITY);
        });
        if (landPos == null) {
            return false;
        }
        Optional<BlockPos> target = level.getPoiManager().find(e -> e.is(PoiTypeTags.VILLAGE), this::hasNotVisited, BlockPos.containing(landPos), 10, PoiManager.Occupancy.IS_OCCUPIED);
        if (target.isEmpty()) {
            return false;
        }
        this.poiPos = target.get().immutable();
        PathNavigation navigation = this.mob.getNavigation();
        navigation.setCanOpenDoors(this.canDealWithDoors.getAsBoolean());
        this.path = navigation.createPath(this.poiPos, 0);
        navigation.setCanOpenDoors(true);
        if (this.path == null) {
            Vec3 partialStep = DefaultRandomPos.getPosTowards(this.mob, 10, 7, Vec3.atBottomCenterOf(this.poiPos), 1.5707963705062866);
            if (partialStep == null) {
                return false;
            }
            navigation.setCanOpenDoors(this.canDealWithDoors.getAsBoolean());
            this.path = this.mob.getNavigation().createPath(partialStep.x, partialStep.y, partialStep.z, 0);
            navigation.setCanOpenDoors(true);
            if (this.path == null) {
                return false;
            }
        }
        for (int i = 0; i < this.path.getNodeCount(); ++i) {
            Node node = this.path.getNode(i);
            BlockPos doorPos = new BlockPos(node.x, node.y + 1, node.z);
            if (!DoorBlock.isWoodenDoor(this.mob.level(), doorPos)) continue;
            this.path = this.mob.getNavigation().createPath(node.x, (double)node.y, node.z, 0);
            break;
        }
        return this.path != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mob.getNavigation().isDone()) {
            return false;
        }
        return !this.poiPos.closerToCenterThan(this.mob.position(), this.mob.getBbWidth() + (float)this.distanceToPoi);
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.path, this.speedModifier);
    }

    @Override
    public void stop() {
        if (this.mob.getNavigation().isDone() || this.poiPos.closerToCenterThan(this.mob.position(), this.distanceToPoi)) {
            this.visited.add(this.poiPos);
        }
    }

    private boolean hasNotVisited(BlockPos poi) {
        for (BlockPos visitedPoi : this.visited) {
            if (!Objects.equals(poi, visitedPoi)) continue;
            return false;
        }
        return true;
    }

    private void updateVisited() {
        if (this.visited.size() > 15) {
            this.visited.remove(0);
        }
    }
}

