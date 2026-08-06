/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TargetBlock
extends Block {
    public static final MapCodec<TargetBlock> CODEC = TargetBlock.simpleCodec(TargetBlock::new);
    private static final IntegerProperty OUTPUT_POWER = BlockStateProperties.POWER;
    private static final int ACTIVATION_TICKS_ARROWS = 20;
    private static final int ACTIVATION_TICKS_OTHER = 8;

    public MapCodec<TargetBlock> codec() {
        return CODEC;
    }

    public TargetBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(OUTPUT_POWER, 0));
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hitResult, Projectile projectile) {
        int outputStrength = TargetBlock.updateRedstoneOutput(level, state, hitResult, projectile);
        Entity owner = projectile.getOwner();
        if (owner instanceof ServerPlayer) {
            ServerPlayer playerOwner = (ServerPlayer)owner;
            playerOwner.awardStat(Stats.TARGET_HIT);
            CriteriaTriggers.TARGET_BLOCK_HIT.trigger(playerOwner, projectile, hitResult.getLocation(), outputStrength);
        }
    }

    private static int updateRedstoneOutput(LevelAccessor level, BlockState state, BlockHitResult hitResult, Entity entity) {
        int duration;
        int redstoneStrength = TargetBlock.getRedstoneStrength(hitResult, hitResult.getLocation());
        int n = duration = entity instanceof AbstractArrow ? 20 : 8;
        if (!level.getBlockTicks().hasScheduledTick(hitResult.getBlockPos(), state.getBlock())) {
            TargetBlock.setOutputPower(level, state, redstoneStrength, hitResult.getBlockPos(), duration);
        }
        return redstoneStrength;
    }

    private static int getRedstoneStrength(BlockHitResult hitResult, Vec3 hitLocation) {
        Direction hitDirection = hitResult.getDirection();
        double distX = Math.abs(Mth.frac(hitLocation.x) - 0.5);
        double distY = Math.abs(Mth.frac(hitLocation.y) - 0.5);
        double distZ = Math.abs(Mth.frac(hitLocation.z) - 0.5);
        Direction.Axis axis = hitDirection.getAxis();
        double distance = axis == Direction.Axis.Y ? Math.max(distX, distZ) : (axis == Direction.Axis.Z ? Math.max(distX, distY) : Math.max(distY, distZ));
        return Math.max(1, Mth.ceil(15.0 * Mth.clamp((0.5 - distance) / 0.5, 0.0, 1.0)));
    }

    private static void setOutputPower(LevelAccessor level, BlockState state, int outputStrength, BlockPos pos, int duration) {
        level.setBlock(pos, (BlockState)state.setValue(OUTPUT_POWER, outputStrength), 3);
        level.scheduleTick(pos, state.getBlock(), duration);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(OUTPUT_POWER) != 0) {
            level.setBlock(pos, (BlockState)state.setValue(OUTPUT_POWER, 0), 3);
        }
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(OUTPUT_POWER);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OUTPUT_POWER);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide() || state.is(oldState.getBlock())) {
            return;
        }
        if (state.getValue(OUTPUT_POWER) > 0 && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.setBlock(pos, (BlockState)state.setValue(OUTPUT_POWER, 0), 18);
        }
    }
}

