/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.LongJumpToRandomPos;
import net.minecraft.world.level.block.Block;

public class LongJumpToPreferredBlock<E extends Mob>
extends LongJumpToRandomPos<E> {
    private final TagKey<Block> preferredBlockTag;
    private final float preferredBlocksChance;
    private final List<LongJumpToRandomPos.PossibleJump> notPrefferedJumpCandidates = new ArrayList<LongJumpToRandomPos.PossibleJump>();
    private boolean currentlyWantingPreferredOnes;

    public LongJumpToPreferredBlock(UniformInt timeBetweenLongJumps, int maxLongJumpHeight, int maxLongJumpWidth, float maxJumpVelocity, Function<E, SoundEvent> getJumpSound, TagKey<Block> preferredBlockTag, float preferredBlocksChance, BiPredicate<E, BlockPos> acceptableLandingSpot) {
        super(timeBetweenLongJumps, maxLongJumpHeight, maxLongJumpWidth, maxJumpVelocity, getJumpSound, acceptableLandingSpot);
        this.preferredBlockTag = preferredBlockTag;
        this.preferredBlocksChance = preferredBlocksChance;
    }

    @Override
    protected void start(ServerLevel level, E body, long timestamp) {
        super.start(level, body, timestamp);
        this.notPrefferedJumpCandidates.clear();
        this.currentlyWantingPreferredOnes = ((Entity)body).getRandom().nextFloat() < this.preferredBlocksChance;
    }

    @Override
    protected Optional<LongJumpToRandomPos.PossibleJump> getJumpCandidate(ServerLevel level) {
        if (!this.currentlyWantingPreferredOnes) {
            return super.getJumpCandidate(level);
        }
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
        while (!this.jumpCandidates.isEmpty()) {
            Optional<LongJumpToRandomPos.PossibleJump> jumpCandidate = super.getJumpCandidate(level);
            if (!jumpCandidate.isPresent()) continue;
            LongJumpToRandomPos.PossibleJump possibleJump = jumpCandidate.get();
            if (level.getBlockState(testPos.setWithOffset((Vec3i)possibleJump.targetPos(), Direction.DOWN)).is(this.preferredBlockTag)) {
                return jumpCandidate;
            }
            this.notPrefferedJumpCandidates.add(possibleJump);
        }
        if (!this.notPrefferedJumpCandidates.isEmpty()) {
            return Optional.of(this.notPrefferedJumpCandidates.remove(0));
        }
        return Optional.empty();
    }
}

