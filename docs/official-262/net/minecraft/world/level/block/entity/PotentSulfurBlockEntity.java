/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.GeyserParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PotentSulfurBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PotentSulfurState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public class PotentSulfurBlockEntity
extends BlockEntity {
    private static final int EFFECT_APPLICATION_FREQUENCY_TICKS = 10;
    private static final float EFFECT_DURATION_IN_SECONDS = 4.0f;
    private static final int EFFECT_DURATION_IN_TICKS = 80;
    public static final float EFFECT_RANGE = 3.0f;
    private static final Predicate<Entity> EFFECT_PREDICATE = EntitySelector.NO_SPECTATORS.and(EntitySelector.ENTITY_STILL_ALIVE);
    public static final int PARTICLE_FREQUENCY_TICKS = 20;
    public static final int SOUND_FREQUENCY_TICKS = 40;
    private static final float GEYSER_BASE_LAUNCH_SPEED = 0.3f;
    private static final float GEYSER_LAUNCH_FORCE = 0.2f;
    public int waitingCountdown = -1;
    public long eruptionTick = -1L;
    public static BlockEntityTicker<PotentSulfurBlockEntity> SERVER_NAUSEA_EFFECT_TICKER = (level, pos, state, potentSulfur) -> {
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        BlockPos sourceBlock = PotentSulfurBlockEntity.findNoxiousGasSourceBlock(level, pos);
        if (sourceBlock == null) {
            return;
        }
        for (LivingEntity entity : PotentSulfurBlockEntity.getNearbyLivingEntities(level, sourceBlock)) {
            if (!PotentSulfurBlockEntity.canBeReachedByNoxiousGas(level, sourceBlock, entity.getEyePosition())) continue;
            PotentSulfurBlockEntity.applyNauseaEffect(entity);
        }
    };
    public static BlockEntityTicker<PotentSulfurBlockEntity> CLIENT_NOXIOUS_GAS_TICKER = (level, pos, state, entity) -> {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        BlockPos sourceBlock = PotentSulfurBlockEntity.findNoxiousGasSourceBlock(level, pos);
        if (sourceBlock != null) {
            PotentSulfurBlockEntity.spawnNoxiousGasCloudParticle(level, Vec3.atCenterOf(sourceBlock));
        }
    };
    public static Function<SoundEvent, BlockEntityTicker<PotentSulfurBlockEntity>> CLIENT_GEYSER_PLUME_TICKER = sound -> (level, pos, state, entity) -> {
        BlockPos sourceBlock = PotentSulfurBlockEntity.findNoxiousGasSourceBlock(level, pos);
        if (sourceBlock == null) {
            return;
        }
        long eruptionTime = level.getGameTime() - entity.eruptionTick;
        if (eruptionTime % 20L == 0L) {
            PotentSulfurBlockEntity.spawnGeyserParticle(level, pos, sourceBlock);
        }
        if (eruptionTime % 40L == 0L) {
            level.playLocalSound((double)sourceBlock.getX() + 0.5, (double)sourceBlock.getY() + 0.5, (double)sourceBlock.getZ() + 0.5, (SoundEvent)sound, SoundSource.BLOCKS, 1.0f, 1.0f, false);
        }
    };
    public static BlockEntityTicker<PotentSulfurBlockEntity> SERVER_WAITING_COUNTDOWN_TICKER = (level, pos, state, entity) -> {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        BlockPos sourceBlock = PotentSulfurBlockEntity.findNoxiousGasSourceBlock(level, pos);
        if (sourceBlock == null) {
            return;
        }
        if (entity.waitingCountdown <= 0) {
            int waterBlocks = sourceBlock.getY() - pos.getY() - 1;
            RandomSource geyserPositional = PotentSulfurBlockEntity.geyserPositional((ServerLevel)level, pos);
            if (state.getValue(PotentSulfurBlock.STATE) == PotentSulfurState.DORMANT) {
                entity.waitingCountdown = 10 * (waterBlocks - 1) + geyserPositional.nextIntBetweenInclusive(15, 30);
            } else {
                geyserPositional.nextInt();
                entity.waitingCountdown = waterBlocks - 1 + geyserPositional.nextIntBetweenInclusive(1, 2);
            }
        }
        if (entity.waitingCountdown > 0) {
            --entity.waitingCountdown;
        }
        if (entity.waitingCountdown == 0) {
            PotentSulfurState stateToSet = state.getValue(PotentSulfurBlock.STATE) == PotentSulfurState.DORMANT ? PotentSulfurState.ERUPTING : PotentSulfurState.DORMANT;
            level.setBlock(pos, (BlockState)state.setValue(PotentSulfurBlock.STATE, stateToSet), 3);
            if (stateToSet == PotentSulfurState.DORMANT) {
                level.gameEvent(GameEvent.BLOCK_DEACTIVATE, pos, GameEvent.Context.of(state));
            }
        }
    };
    public static final long GEYSER_SALT = -904011478L;
    public static BlockEntityTicker<PotentSulfurBlockEntity> LAUNCH_ENTITY_TICKER = (level, pos, state, entity) -> {
        BlockPos sourceBlock = PotentSulfurBlockEntity.findNoxiousGasSourceBlock(level, pos);
        if (sourceBlock == null) {
            return;
        }
        int waterBlocks = sourceBlock.getY() - pos.getY() - 1;
        int geyserForceHeight = PotentSulfurBlockEntity.getUnobstructedBlockCount(level, pos.above(), waterBlocks);
        AABB aabb = new AABB(pos.above()).expandTowards(0.0, geyserForceHeight - 1, 0.0);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, aabb, EFFECT_PREDICATE);
        for (Entity entityToBeLaunched : entities) {
            Vec3 entityVelocity = entityToBeLaunched.getDeltaMovement();
            entityToBeLaunched.checkFallDistanceAccumulation();
            if (!entityToBeLaunched.canSimulateMovement()) continue;
            if (entityToBeLaunched instanceof Player) {
                Player player = (Player)entityToBeLaunched;
                if (player.getAbilities().flying) continue;
            }
            if (entityToBeLaunched.isPassenger() || entityToBeLaunched.is(EntityTypeTags.NOT_AFFECTED_BY_GEYSERS) || !(entityVelocity.y < (double)0.3f + (double)waterBlocks * 0.1)) continue;
            entityToBeLaunched.addDeltaMovement(new Vec3(0.0, 0.2f, 0.0));
            entityToBeLaunched.needsSync = true;
        }
    };

    public PotentSulfurBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.POTENT_SULFUR, worldPosition, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("countdown", this.waitingCountdown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getInt("countdown").ifPresent(value -> {
            this.waitingCountdown = value;
        });
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (this.eruptionTick == -1L) {
            this.eruptionTick = level.getGameTime();
        }
    }

    public void resetCountdown() {
        this.waitingCountdown = -1;
    }

    private static void applyNauseaEffect(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, true, true));
    }

    private static List<LivingEntity> getNearbyLivingEntities(Level level, BlockPos pos) {
        AABB aabb = new AABB(pos).inflate(2.5, 0.0, 2.5);
        return level.getEntitiesOfClass(LivingEntity.class, aabb, EFFECT_PREDICATE);
    }

    public static RandomSource geyserPositional(ServerLevel level, BlockPos pos) {
        return new XoroshiroRandomSource(level.getSeed() ^ 0xFFFFFFFFCA1DE12AL).forkPositional().at(pos);
    }

    private static void spawnGeyserParticle(Level level, BlockPos sulfurPos, BlockPos sourcePos) {
        int waterBlocks = sourcePos.getY() - sulfurPos.getY() - 1;
        level.addParticle(new GeyserParticleOptions(ParticleTypes.GEYSER, waterBlocks), (double)sourcePos.getX() + 0.5, sourcePos.getY(), (double)sourcePos.getZ() + 0.5, 0.0, 0.0, 0.0);
    }

    private static void spawnNoxiousGasCloudParticle(Level level, Vec3 pos) {
        level.addParticle(ParticleTypes.NOXIOUS_GAS_CLOUD, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
    }

    private static int getUnobstructedBlockCount(Level level, BlockPos pos, int waterBlocks) {
        int geyserForceHeight = 6 * waterBlocks;
        CollisionContext geyserPositionContext = CollisionContext.positionContext(pos.below().getY());
        for (int i = 0; i < geyserForceHeight; ++i) {
            BlockPos currentPos = pos.above(i);
            BlockState state = level.getBlockState(currentPos);
            if (PotentSulfurBlockEntity.isGeyserPassableBlock(state, level, currentPos, geyserPositionContext)) continue;
            return i;
        }
        return geyserForceHeight;
    }

    private static boolean isGeyserPassableBlock(BlockState state, Level level, BlockPos pos, CollisionContext context) {
        if (state.isAir() || state.is(Blocks.WATER)) {
            return true;
        }
        return state.getCollisionShape(level, pos, context).isEmpty();
    }

    private static @Nullable BlockPos findNoxiousGasSourceBlock(Level level, BlockPos origin) {
        int maxY = origin.getY() + 4 + 1;
        CollisionContext geyserPositionContext = CollisionContext.positionContext(origin.getY());
        BlockPos.MutableBlockPos pos = origin.above(1).mutable();
        while (pos.getY() <= maxY) {
            BlockState state = level.getBlockState(pos);
            boolean isWaterLogged = level.getFluidState(pos).isSourceOfType(Fluids.WATER);
            if (!isWaterLogged || !state.is(Blocks.WATER) && !PotentSulfurBlockEntity.isGeyserPassableBlock(state, level, pos, geyserPositionContext)) {
                if (!state.isAir() && !PotentSulfurBlockEntity.isGeyserPassableBlock(state, level, pos, geyserPositionContext)) break;
                return pos.immutable();
            }
            pos.move(Direction.UP);
        }
        return null;
    }

    public static boolean canBeReachedByNoxiousGas(Level level, BlockPos sourceBlock, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        CollisionContext geyserPositionContext = CollisionContext.positionContext(blockPos.below().getY());
        if (!PotentSulfurBlockEntity.isGeyserPassableBlock(level.getBlockState(blockPos), level, blockPos, geyserPositionContext)) {
            return false;
        }
        if (pos.distanceToSqr(Vec3.atCenterOf(sourceBlock)) > 9.0) {
            return false;
        }
        Vec3 belowSource = Vec3.atCenterOf(sourceBlock.below());
        Vec3 belowPos = pos.with(Direction.Axis.Y, pos.y - 1.0);
        return PotentSulfurBlockEntity.isWater(level, belowPos) && PotentSulfurBlockEntity.haveLineOfSight(level, belowSource, belowPos);
    }

    private static boolean isWater(Level level, Vec3 pos) {
        return level.getFluidState(BlockPos.containing(pos)).isSourceOfType(Fluids.WATER);
    }

    private static boolean haveLineOfSight(Level level, Vec3 a, Vec3 b) {
        BlockHitResult hitResult = level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return ((HitResult)hitResult).getType() != HitResult.Type.BLOCK;
    }
}

