/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RemoveBlockGoal
extends MoveToBlockGoal {
    private final Block blockToRemove;
    private final Mob removerMob;
    private int ticksSinceReachedGoal;
    private static final int WAIT_AFTER_BLOCK_FOUND = 20;

    public RemoveBlockGoal(Block blockToRemove, PathfinderMob mob, double speedModifier, int verticalSearchRange) {
        super(mob, speedModifier, 24, verticalSearchRange);
        this.blockToRemove = blockToRemove;
        this.removerMob = mob;
    }

    @Override
    public boolean canUse() {
        if (!RemoveBlockGoal.getServerLevel(this.removerMob).getGameRules().get(GameRules.MOB_GRIEFING).booleanValue()) {
            return false;
        }
        if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        }
        if (this.findNearestBlock()) {
            this.nextStartTick = RemoveBlockGoal.reducedTickDelay(20);
            return true;
        }
        this.nextStartTick = this.nextStartTick(this.mob);
        return false;
    }

    @Override
    public void stop() {
        super.stop();
        this.removerMob.fallDistance = 1.0;
    }

    @Override
    public void start() {
        super.start();
        this.ticksSinceReachedGoal = 0;
    }

    public void playDestroyProgressSound(LevelAccessor level, BlockPos pos) {
    }

    public void playBreakSound(Level level, BlockPos pos) {
    }

    @Override
    public void tick() {
        super.tick();
        Level level = this.removerMob.level();
        BlockPos mobPos = this.removerMob.blockPosition();
        BlockPos eatPos = this.getPosWithBlock(mobPos, level);
        RandomSource random = this.removerMob.getRandom();
        if (this.isReachedTarget() && eatPos != null) {
            Vec3 movement;
            if (this.ticksSinceReachedGoal > 0) {
                movement = this.removerMob.getDeltaMovement();
                this.removerMob.setDeltaMovement(movement.x, 0.3, movement.z);
                if (level instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel)level;
                    serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.EGG), (double)eatPos.getX() + 0.5, (double)eatPos.getY() + 0.7, (double)eatPos.getZ() + 0.5, 3, ((double)random.nextFloat() - 0.5) * 0.08, ((double)random.nextFloat() - 0.5) * 0.08, ((double)random.nextFloat() - 0.5) * 0.08, 0.15f);
                }
            }
            if (this.ticksSinceReachedGoal % 2 == 0) {
                movement = this.removerMob.getDeltaMovement();
                this.removerMob.setDeltaMovement(movement.x, -0.3, movement.z);
                if (this.ticksSinceReachedGoal % 6 == 0) {
                    this.playDestroyProgressSound(level, this.blockPos);
                }
            }
            if (this.ticksSinceReachedGoal > 60) {
                level.removeBlock(eatPos, false);
                if (!level.isClientSide()) {
                    for (int i = 0; i < 20; ++i) {
                        double xa = random.nextGaussian() * 0.02;
                        double ya = random.nextGaussian() * 0.02;
                        double za = random.nextGaussian() * 0.02;
                        ((ServerLevel)level).sendParticles(ParticleTypes.POOF, (double)eatPos.getX() + 0.5, eatPos.getY(), (double)eatPos.getZ() + 0.5, 1, xa, ya, za, 0.15f);
                    }
                    this.playBreakSound(level, eatPos);
                }
            }
            ++this.ticksSinceReachedGoal;
        }
    }

    private @Nullable BlockPos getPosWithBlock(BlockPos pos, BlockGetter level) {
        BlockPos[] neighbours;
        if (level.getBlockState(pos).is(this.blockToRemove)) {
            return pos;
        }
        for (BlockPos neighborPos : neighbours = new BlockPos[]{pos.below(), pos.west(), pos.east(), pos.north(), pos.south(), pos.below().below()}) {
            if (!level.getBlockState(neighborPos).is(this.blockToRemove)) continue;
            return neighborPos;
        }
        return null;
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
        if (chunk != null) {
            return chunk.getBlockState(pos).is(this.blockToRemove) && chunk.getBlockState(pos.above()).isAir() && chunk.getBlockState(pos.above(2)).isAir();
        }
        return false;
    }
}

