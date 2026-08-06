/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InsideBrownianWalk {
    public static BehaviorControl<PathfinderMob> create(float speedModifier) {
        return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.WALK_TARGET)).apply((Applicative)i, walkTarget -> (level, body, timestamp) -> {
            if (level.canSeeSky(body.blockPosition())) {
                return false;
            }
            BlockPos bodyPos = body.blockPosition();
            List poses = BlockPos.betweenClosedStream(bodyPos.offset(-1, -1, -1), bodyPos.offset(1, 1, 1)).map(BlockPos::immutable).collect(Util.toMutableList());
            Collections.shuffle(poses);
            poses.stream().filter(pos -> !level.canSeeSky((BlockPos)pos)).filter(pos -> level.loadedAndEntityCanStandOn((BlockPos)pos, body)).filter(pos -> level.noCollision(body)).findFirst().ifPresent(target -> walkTarget.set(new WalkTarget((BlockPos)target, speedModifier, 0)));
            return true;
        }));
    }
}

