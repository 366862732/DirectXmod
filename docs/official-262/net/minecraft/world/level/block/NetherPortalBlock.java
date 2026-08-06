/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class NetherPortalBlock
extends Block
implements Portal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<NetherPortalBlock> CODEC = NetherPortalBlock.simpleCodec(NetherPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.column(4.0, 16.0, 0.0, 16.0));

    public MapCodec<NetherPortalBlock> codec() {
        return CODEC;
    }

    public NetherPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(AXIS));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isSpawningMonsters() && level.getLevelData().getDifficulty() != Difficulty.PEACEFUL && level.environmentAttributes().getValue(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, pos).booleanValue() && random.nextInt(2000) < level.getDifficulty().getId() && level.anyPlayerCloseEnoughForSpawning(pos)) {
            ZombifiedPiglin entity;
            while (level.getBlockState(pos).is(this)) {
                pos = pos.below();
            }
            if (level.getBlockState(pos).isValidSpawn(level, pos, EntityTypes.ZOMBIFIED_PIGLIN) && (entity = EntityTypes.ZOMBIFIED_PIGLIN.spawn(level, pos.above(), EntitySpawnReason.STRUCTURE)) != null) {
                entity.setPortalCooldown();
                Entity vehicle = entity.getVehicle();
                if (vehicle != null) {
                    vehicle.setPortalCooldown();
                }
            }
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        boolean wrongAxis;
        Direction.Axis updateAxis = directionToNeighbour.getAxis();
        Direction.Axis axis = state.getValue(AXIS);
        boolean bl = wrongAxis = axis != updateAxis && updateAxis.isHorizontal();
        if (wrongAxis || neighbourState.is(this) || PortalShape.findAnyShape(level, pos, axis).isComplete()) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player)entity;
            return Math.max(0, level.getGameRules().get(player.getAbilities().invulnerable ? GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY));
        }
        return 0;
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
        ResourceKey<Level> newDimension = currentLevel.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
        ServerLevel newLevel = currentLevel.getServer().getLevel(newDimension);
        if (newLevel == null) {
            return null;
        }
        boolean toNether = newLevel.dimension() == Level.NETHER;
        WorldBorder newWorldBorder = newLevel.getWorldBorder();
        double teleportationScale = DimensionType.getTeleportationScale(currentLevel.dimensionType(), newLevel.dimensionType());
        BlockPos approximateExitPos = newWorldBorder.clampToBounds(entity.getX() * teleportationScale, entity.getY(), entity.getZ() * teleportationScale);
        return this.getExitPortal(newLevel, entity, portalEntryPos, approximateExitPos, toNether, newWorldBorder);
    }

    private @Nullable TeleportTransition getExitPortal(ServerLevel newLevel, Entity entity, BlockPos portalEntryPos, BlockPos approximateExitPos, boolean toNether, WorldBorder worldBorder) {
        TeleportTransition.PostTeleportTransition post;
        BlockUtil.FoundRectangle exitPortal;
        Optional<BlockPos> exitPortalPos = newLevel.getPortalForcer().findClosestPortalPosition(approximateExitPos, toNether, worldBorder);
        if (exitPortalPos.isPresent()) {
            BlockPos pos = exitPortalPos.get();
            BlockState portalState = newLevel.getBlockState(pos);
            exitPortal = BlockUtil.getLargestRectangleAround(pos, portalState.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, blockPos -> newLevel.getBlockState((BlockPos)blockPos) == portalState);
            post = TeleportTransition.PLAY_PORTAL_SOUND.then(e -> e.placePortalTicket(pos));
        } else {
            Direction.Axis sourcePortalAxis = entity.level().getBlockState(portalEntryPos).getOptionalValue(AXIS).orElse(Direction.Axis.X);
            Optional<BlockUtil.FoundRectangle> createdExit = newLevel.getPortalForcer().createPortal(approximateExitPos, sourcePortalAxis);
            if (createdExit.isEmpty()) {
                LOGGER.error("Unable to create a portal, likely target out of worldborder");
                return null;
            }
            exitPortal = createdExit.get();
            post = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
        }
        return NetherPortalBlock.getDimensionTransitionFromExit(entity, portalEntryPos, exitPortal, newLevel, post);
    }

    private static TeleportTransition getDimensionTransitionFromExit(Entity entity, BlockPos portalEntryPos, BlockUtil.FoundRectangle exitPortal, ServerLevel newLevel, TeleportTransition.PostTeleportTransition postTeleportTransition) {
        Vec3 offset;
        Direction.Axis axis;
        BlockState blockState = entity.level().getBlockState(portalEntryPos);
        if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            axis = blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
            BlockUtil.FoundRectangle portalArea = BlockUtil.getLargestRectangleAround(portalEntryPos, axis, 21, Direction.Axis.Y, 21, pos -> entity.level().getBlockState((BlockPos)pos) == blockState);
            offset = entity.getRelativePortalPosition(axis, portalArea);
        } else {
            axis = Direction.Axis.X;
            offset = new Vec3(0.5, 0.0, 0.0);
        }
        return NetherPortalBlock.createDimensionTransition(newLevel, exitPortal, axis, offset, entity, postTeleportTransition);
    }

    private static TeleportTransition createDimensionTransition(ServerLevel newLevel, BlockUtil.FoundRectangle foundRectangle, Direction.Axis portalAxis, Vec3 offset, Entity entity, TeleportTransition.PostTeleportTransition postTeleportTransition) {
        BlockPos bottomLeft = foundRectangle.minCorner;
        BlockState blockState = newLevel.getBlockState(bottomLeft);
        Direction.Axis axis = blockState.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
        double width = foundRectangle.axis1Size;
        double height = foundRectangle.axis2Size;
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        int outputRotation = portalAxis == axis ? 0 : 90;
        double offsetRight = (double)dimensions.width() / 2.0 + (width - (double)dimensions.width()) * offset.x();
        double offsetUp = (height - (double)dimensions.height()) * offset.y();
        double offsetForward = 0.5 + offset.z();
        boolean xAligned = axis == Direction.Axis.X;
        Vec3 targetPos = new Vec3((double)bottomLeft.getX() + (xAligned ? offsetRight : offsetForward), (double)bottomLeft.getY() + offsetUp, (double)bottomLeft.getZ() + (xAligned ? offsetForward : offsetRight));
        Vec3 collisionFreePos = PortalShape.findCollisionFreePosition(targetPos, newLevel, entity, dimensions);
        return new TeleportTransition(newLevel, collisionFreePos, Vec3.ZERO, outputRotation, 0.0f, Relative.union(Relative.DELTA, Relative.ROTATION), postTeleportTransition);
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5f, random.nextFloat() * 0.4f + 0.8f, false);
        }
        for (int i = 0; i < 4; ++i) {
            double x = (double)pos.getX() + random.nextDouble();
            double y = (double)pos.getY() + random.nextDouble();
            double z = (double)pos.getZ() + random.nextDouble();
            double xa = ((double)random.nextFloat() - 0.5) * 0.5;
            double ya = ((double)random.nextFloat() - 0.5) * 0.5;
            double za = ((double)random.nextFloat() - 0.5) * 0.5;
            int flip = random.nextInt(2) * 2 - 1;
            if (level.getBlockState(pos.west()).is(this) || level.getBlockState(pos.east()).is(this)) {
                z = (double)pos.getZ() + 0.5 + 0.25 * (double)flip;
                za = random.nextFloat() * 2.0f * (float)flip;
            } else {
                x = (double)pos.getX() + 0.5 + 0.25 * (double)flip;
                xa = random.nextFloat() * 2.0f * (float)flip;
            }
            level.addParticle(ParticleTypes.PORTAL, x, y, z, xa, ya, za);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        switch (rotation) {
            case COUNTERCLOCKWISE_90: 
            case CLOCKWISE_90: {
                switch (state.getValue(AXIS)) {
                    case X: {
                        return (BlockState)state.setValue(AXIS, Direction.Axis.Z);
                    }
                    case Z: {
                        return (BlockState)state.setValue(AXIS, Direction.Axis.X);
                    }
                }
                return state;
            }
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}

