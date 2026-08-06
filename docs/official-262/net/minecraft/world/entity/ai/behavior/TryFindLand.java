/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 *  org.apache.commons.lang3.mutable.MutableLong
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.apache.commons.lang3.mutable.MutableLong;

public class TryFindLand {
    private static final int COOLDOWN_TICKS = 60;

    public static BehaviorControl<PathfinderMob> create(int range, float speedModifier) {
        MutableLong nextOkStartTime = new MutableLong(0L);
        return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.ATTACK_TARGET), i.absent(MemoryModuleType.WALK_TARGET), i.registered(MemoryModuleType.LOOK_TARGET)).apply((Applicative)i, (attackTarget, walkTarget, lookTarget) -> (level, body, timestamp) -> {
            if (!level.getFluidState(body.blockPosition()).is(FluidTags.WATER)) {
                return false;
            }
            if (timestamp < nextOkStartTime.longValue()) {
                nextOkStartTime.setValue(timestamp + 60L);
                return true;
            }
            BlockPos bodyBlockPos = body.blockPosition();
            BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
            CollisionContext context = CollisionContext.of(body);
            for (BlockPos pos : BlockPos.withinManhattan(bodyBlockPos, range, range, range)) {
                if (pos.getX() == bodyBlockPos.getX() && pos.getZ() == bodyBlockPos.getZ()) continue;
                BlockState state = level.getBlockState(pos);
                BlockState belowState = level.getBlockState(belowPos.setWithOffset((Vec3i)pos, Direction.DOWN));
                if (state.is(Blocks.WATER) || !level.getFluidState(pos).isEmpty() || !state.getCollisionShape(level, pos, context).isEmpty() || !belowState.isFaceSturdy(level, belowPos, Direction.UP)) continue;
                BlockPos targetPos = pos.immutable();
                lookTarget.set(new BlockPosTracker(targetPos));
                walkTarget.set(new WalkTarget(new BlockPosTracker(targetPos), speedModifier, 1));
                break;
            }
            nextOkStartTime.setValue(timestamp + 60L);
            return true;
        }));
    }
}

