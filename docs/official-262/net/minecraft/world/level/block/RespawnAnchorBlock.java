/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RespawnAnchorBlock
extends Block {
    public static final MapCodec<RespawnAnchorBlock> CODEC = RespawnAnchorBlock.simpleCodec(RespawnAnchorBlock::new);
    public static final int MIN_CHARGES = 0;
    public static final int MAX_CHARGES = 4;
    public static final IntegerProperty CHARGE = BlockStateProperties.RESPAWN_ANCHOR_CHARGES;
    private static final ImmutableList<Vec3i> RESPAWN_HORIZONTAL_OFFSETS = ImmutableList.of((Object)new Vec3i(0, 0, -1), (Object)new Vec3i(-1, 0, 0), (Object)new Vec3i(0, 0, 1), (Object)new Vec3i(1, 0, 0), (Object)new Vec3i(-1, 0, -1), (Object)new Vec3i(1, 0, -1), (Object)new Vec3i(-1, 0, 1), (Object)new Vec3i(1, 0, 1));
    private static final ImmutableList<Vec3i> RESPAWN_OFFSETS = new ImmutableList.Builder().addAll(RESPAWN_HORIZONTAL_OFFSETS).addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::below).iterator()).addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::above).iterator()).add((Object)new Vec3i(0, 1, 0)).build();

    public MapCodec<RespawnAnchorBlock> codec() {
        return CODEC;
    }

    public RespawnAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(CHARGE, 0));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (RespawnAnchorBlock.isRespawnFuel(itemStack) && RespawnAnchorBlock.canBeCharged(state)) {
            RespawnAnchorBlock.charge(player, level, pos, state);
            itemStack.consume(1, player);
            return InteractionResult.SUCCESS;
        }
        if (hand == InteractionHand.MAIN_HAND && RespawnAnchorBlock.isRespawnFuel(player.getItemInHand(InteractionHand.OFF_HAND)) && RespawnAnchorBlock.canBeCharged(state)) {
            return InteractionResult.PASS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(CHARGE) == 0) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.CONSUME;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (RespawnAnchorBlock.canSetSpawn(serverLevel, pos)) {
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                ServerPlayer.RespawnConfig respawnConfig = serverPlayer.getRespawnConfig();
                ServerPlayer.RespawnConfig newRespawnConfig = new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(serverLevel.dimension(), pos, 0.0f, 0.0f), false);
                if (respawnConfig == null || !respawnConfig.isSamePosition(newRespawnConfig)) {
                    serverPlayer.setRespawnPosition(newRespawnConfig, true);
                    serverLevel.playSound(null, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0f, 1.0f);
                    return InteractionResult.SUCCESS_SERVER;
                }
            }
            return InteractionResult.CONSUME;
        }
        this.explode(state, serverLevel, pos);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean isRespawnFuel(ItemStack itemInHand) {
        return itemInHand.is(Items.GLOWSTONE);
    }

    private static boolean canBeCharged(BlockState state) {
        return state.getValue(CHARGE) < 4;
    }

    private static boolean isWaterThatWouldFlow(BlockPos pos, Level level) {
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.is(FluidTags.WATER)) {
            return false;
        }
        if (fluid.isSource()) {
            return true;
        }
        float amount = fluid.getAmount();
        if (amount < 2.0f) {
            return false;
        }
        FluidState fluidBelow = level.getFluidState(pos.below());
        return !fluidBelow.is(FluidTags.WATER);
    }

    private void explode(BlockState state, ServerLevel level, final BlockPos pos) {
        level.removeBlock(pos, false);
        boolean anyWaterNeighbors = Direction.Plane.HORIZONTAL.stream().map(pos::relative).anyMatch(neighborPos -> RespawnAnchorBlock.isWaterThatWouldFlow(neighborPos, level));
        final boolean inWater = anyWaterNeighbors || level.getFluidState(pos.above()).is(FluidTags.WATER);
        ExplosionDamageCalculator damageCalculator = new ExplosionDamageCalculator(this){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos testPos, BlockState block, FluidState fluid) {
                if (testPos.equals(pos) && inWater) {
                    return Optional.of(Float.valueOf(Blocks.WATER.getExplosionResistance()));
                }
                return super.getBlockExplosionResistance(explosion, level, testPos, block, fluid);
            }
        };
        Vec3 boomPos = Vec3.atCenterOf(pos);
        level.explode(null, level.damageSources().badRespawnPointExplosion(boomPos), damageCalculator, boomPos, 5.0f, true, Level.ExplosionInteraction.BLOCK);
    }

    public static boolean canSetSpawn(ServerLevel level, BlockPos pos) {
        return level.environmentAttributes().getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, pos);
    }

    public static void charge(@Nullable Entity sourceEntity, Level level, BlockPos pos, BlockState state) {
        BlockState newState = (BlockState)state.setValue(CHARGE, state.getValue(CHARGE) + 1);
        level.setBlock(pos, newState, 3);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
        level.playSound(null, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CHARGE) == 0) {
            return;
        }
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 1.0f, 1.0f, false);
        }
        double x = (double)pos.getX() + 0.5 + (0.5 - random.nextDouble());
        double y = (double)pos.getY() + 1.0;
        double z = (double)pos.getZ() + 0.5 + (0.5 - random.nextDouble());
        double ya = (double)random.nextFloat() * 0.04;
        level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0.0, ya, 0.0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    public static int getScaledChargeLevel(BlockState state, int maximum) {
        return Mth.floor((float)(state.getValue(CHARGE) - 0) / 4.0f * (float)maximum);
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return RespawnAnchorBlock.getScaledChargeLevel(state, 15);
    }

    public static Optional<Vec3> findStandUpPosition(EntityType<?> type, CollisionGetter level, BlockPos pos) {
        Optional<Vec3> safePosition = RespawnAnchorBlock.findStandUpPosition(type, level, pos, true);
        if (safePosition.isPresent()) {
            return safePosition;
        }
        return RespawnAnchorBlock.findStandUpPosition(type, level, pos, false);
    }

    private static Optional<Vec3> findStandUpPosition(EntityType<?> type, CollisionGetter level, BlockPos pos, boolean checkDangerous) {
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (Vec3i offset : RESPAWN_OFFSETS) {
            blockPos.set(pos).move(offset);
            Vec3 position = DismountHelper.findSafeDismountLocation(type, level, blockPos, checkDangerous);
            if (position == null) continue;
            return Optional.of(position);
        }
        return Optional.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}

