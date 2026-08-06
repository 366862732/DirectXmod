/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoulFireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseFireBlock
extends Block {
    private static final int SECONDS_ON_FIRE = 8;
    private static final int MIN_FIRE_TICKS_TO_ADD = 1;
    private static final int MAX_FIRE_TICKS_TO_ADD = 3;
    private final float fireDamage;
    protected static final VoxelShape SHAPE = Block.column(16.0, 0.0, 1.0);

    public BaseFireBlock(BlockBehaviour.Properties properties, float fireDamage) {
        super(properties);
        this.fireDamage = fireDamage;
    }

    protected abstract MapCodec<? extends BaseFireBlock> codec();

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return BaseFireBlock.getState(context.getLevel(), context.getClickedPos());
    }

    public static BlockState getState(BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (SoulFireBlock.canSurviveOnBlock(belowState)) {
            return Blocks.SOUL_FIRE.defaultBlockState();
        }
        return ((FireBlock)Blocks.FIRE).getStateForPlacement(level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        block12: {
            double zz;
            double yy;
            double xx;
            int i;
            block11: {
                BlockPos below;
                BlockState belowState;
                if (random.nextInt(24) == 0) {
                    level.playLocalSound((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0f + random.nextFloat(), random.nextFloat() * 0.7f + 0.3f, false);
                }
                if (!this.canBurn(belowState = level.getBlockState(below = pos.below())) && !belowState.isFaceSturdy(level, below, Direction.UP)) break block11;
                for (int i2 = 0; i2 < 3; ++i2) {
                    double xx2 = (double)pos.getX() + random.nextDouble();
                    double yy2 = (double)pos.getY() + random.nextDouble() * 0.5 + 0.5;
                    double zz2 = (double)pos.getZ() + random.nextDouble();
                    level.addParticle(ParticleTypes.LARGE_SMOKE, xx2, yy2, zz2, 0.0, 0.0, 0.0);
                }
                break block12;
            }
            if (this.canBurn(level.getBlockState(pos.west()))) {
                for (i = 0; i < 2; ++i) {
                    xx = (double)pos.getX() + random.nextDouble() * (double)0.1f;
                    yy = (double)pos.getY() + random.nextDouble();
                    zz = (double)pos.getZ() + random.nextDouble();
                    level.addParticle(ParticleTypes.LARGE_SMOKE, xx, yy, zz, 0.0, 0.0, 0.0);
                }
            }
            if (this.canBurn(level.getBlockState(pos.east()))) {
                for (i = 0; i < 2; ++i) {
                    xx = (double)(pos.getX() + 1) - random.nextDouble() * (double)0.1f;
                    yy = (double)pos.getY() + random.nextDouble();
                    zz = (double)pos.getZ() + random.nextDouble();
                    level.addParticle(ParticleTypes.LARGE_SMOKE, xx, yy, zz, 0.0, 0.0, 0.0);
                }
            }
            if (this.canBurn(level.getBlockState(pos.north()))) {
                for (i = 0; i < 2; ++i) {
                    xx = (double)pos.getX() + random.nextDouble();
                    yy = (double)pos.getY() + random.nextDouble();
                    zz = (double)pos.getZ() + random.nextDouble() * (double)0.1f;
                    level.addParticle(ParticleTypes.LARGE_SMOKE, xx, yy, zz, 0.0, 0.0, 0.0);
                }
            }
            if (this.canBurn(level.getBlockState(pos.south()))) {
                for (i = 0; i < 2; ++i) {
                    xx = (double)pos.getX() + random.nextDouble();
                    yy = (double)pos.getY() + random.nextDouble();
                    zz = (double)(pos.getZ() + 1) - random.nextDouble() * (double)0.1f;
                    level.addParticle(ParticleTypes.LARGE_SMOKE, xx, yy, zz, 0.0, 0.0, 0.0);
                }
            }
            if (!this.canBurn(level.getBlockState(pos.above()))) break block12;
            for (i = 0; i < 2; ++i) {
                xx = (double)pos.getX() + random.nextDouble();
                yy = (double)(pos.getY() + 1) - random.nextDouble() * (double)0.1f;
                zz = (double)pos.getZ() + random.nextDouble();
                level.addParticle(ParticleTypes.LARGE_SMOKE, xx, yy, zz, 0.0, 0.0, 0.0);
            }
        }
    }

    protected abstract boolean canBurn(BlockState var1);

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        effectApplier.apply(InsideBlockEffectType.CLEAR_FREEZE);
        effectApplier.apply(InsideBlockEffectType.FIRE_IGNITE);
        effectApplier.runAfter(InsideBlockEffectType.FIRE_IGNITE, e -> e.hurt(e.level().damageSources().inFire(), this.fireDamage));
    }

    public static void fireIgnite(Entity entity) {
        if (!entity.fireImmune()) {
            if (entity.getRemainingFireTicks() < 0) {
                entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 1);
            } else if (entity instanceof ServerPlayer) {
                int addedFireTicks = entity.level().getRandom().nextInt(1, 3);
                entity.setRemainingFireTicks(entity.getRemainingFireTicks() + addedFireTicks);
            }
            if (entity.getRemainingFireTicks() >= 0) {
                entity.igniteForSeconds(8.0f);
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        Optional<PortalShape> optionalShape;
        if (oldState.is(state.getBlock())) {
            return;
        }
        if (BaseFireBlock.inPortalDimension(level) && (optionalShape = PortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X)).isPresent()) {
            optionalShape.get().createPortalBlocks(level);
            return;
        }
        if (!state.canSurvive(level, pos)) {
            level.removeBlock(pos, false);
        }
    }

    private static boolean inPortalDimension(Level level) {
        return level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER;
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            level.levelEvent(null, 1009, pos, 0);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    public static boolean canBePlacedAt(Level level, BlockPos pos, Direction forwardDirection) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            return false;
        }
        return BaseFireBlock.getState(level, pos).canSurvive(level, pos) || BaseFireBlock.isPortal(level, pos, forwardDirection);
    }

    private static boolean isPortal(Level level, BlockPos pos, Direction forwardDirection) {
        if (!BaseFireBlock.inPortalDimension(level)) {
            return false;
        }
        BlockPos.MutableBlockPos testPos = pos.mutable();
        boolean hasObsidian = false;
        for (Direction face : Direction.values()) {
            if (!level.getBlockState(testPos.set(pos).move(face)).is(Blocks.OBSIDIAN)) continue;
            hasObsidian = true;
            break;
        }
        if (!hasObsidian) {
            return false;
        }
        Direction.Axis preferredAxis = forwardDirection.getAxis().isHorizontal() ? forwardDirection.getCounterClockWise().getAxis() : Direction.Plane.HORIZONTAL.getRandomAxis(level.getRandom());
        return PortalShape.findEmptyPortalShape(level, pos, preferredAxis).isPresent();
    }
}

