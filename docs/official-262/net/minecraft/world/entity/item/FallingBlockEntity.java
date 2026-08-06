/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.entity.item;

import com.mojang.logging.LogUtils;
import java.util.function.Predicate;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FallingBlockEntity
extends Entity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockState DEFAULT_BLOCK_STATE = Blocks.SAND.defaultBlockState();
    private static final int DEFAULT_TIME = 0;
    private static final float DEFAULT_FALL_DAMAGE_PER_DISTANCE = 0.0f;
    private static final int DEFAULT_MAX_FALL_DAMAGE = 40;
    private static final boolean DEFAULT_DROP_ITEM = true;
    private static final boolean DEFAULT_CANCEL_DROP = false;
    private BlockState blockState = DEFAULT_BLOCK_STATE;
    public int time = 0;
    public boolean dropItem = true;
    private boolean cancelDrop = false;
    private boolean hurtEntities;
    private int fallDamageMax = 40;
    private float fallDamagePerDistance = 0.0f;
    public @Nullable CompoundTag blockData;
    public boolean forceTickAfterTeleportToDuplicate;
    protected static final EntityDataAccessor<BlockPos> DATA_START_POS = SynchedEntityData.defineId(FallingBlockEntity.class, EntityDataSerializers.BLOCK_POS);

    public FallingBlockEntity(EntityType<? extends FallingBlockEntity> type, Level level) {
        super(type, level);
    }

    private FallingBlockEntity(Level level, double x, double y, double z, BlockState blockState) {
        this((EntityType<? extends FallingBlockEntity>)EntityTypes.FALLING_BLOCK, level);
        this.blockState = blockState;
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
    }

    public static FallingBlockEntity fall(Level level, BlockPos pos, BlockState state) {
        FallingBlockEntity entity = new FallingBlockEntity(level, (double)pos.getX() + 0.5, pos.getY(), (double)pos.getZ() + 0.5, state.hasProperty(BlockStateProperties.WATERLOGGED) ? (BlockState)state.setValue(BlockStateProperties.WATERLOGGED, false) : state);
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public final boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!this.isInvulnerableToBase(source)) {
            this.markHurt();
        }
        return false;
    }

    public void setStartPos(BlockPos pos) {
        this.entityData.set(DATA_START_POS, pos);
    }

    public BlockPos getStartPos() {
        return this.entityData.get(DATA_START_POS);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_START_POS, BlockPos.ZERO);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        if (this.blockState.isAir()) {
            this.discard();
            return;
        }
        Block block = this.blockState.getBlock();
        ++this.time;
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.applyEffectsFromBlocks();
        this.handlePortal();
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (this.isAlive() || this.forceTickAfterTeleportToDuplicate) {
                BlockHitResult clip;
                BlockPos pos = this.blockPosition();
                boolean isConcrete = this.blockState.getBlock() instanceof ConcretePowderBlock;
                boolean isStuckInWater = isConcrete && this.level().getFluidState(pos).is(FluidTags.WATER);
                double moveVec = this.getDeltaMovement().lengthSqr();
                if (isConcrete && moveVec > 1.0 && (clip = this.level().clip(new ClipContext(new Vec3(this.xo, this.yo, this.zo), this.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, this))).getType() != HitResult.Type.MISS && this.level().getFluidState(clip.getBlockPos()).is(FluidTags.WATER)) {
                    pos = clip.getBlockPos();
                    isStuckInWater = true;
                }
                if (this.onGround() || isStuckInWater) {
                    BlockState currentState = this.level().getBlockState(pos);
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
                    if (!currentState.is(Blocks.MOVING_PISTON)) {
                        if (!this.cancelDrop) {
                            boolean wouldSurvive;
                            boolean mayReplace = currentState.canBeReplaced(new DirectionalPlaceContext(this.level(), pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
                            boolean wouldContinueFalling = FallingBlock.isFree(this.level().getBlockState(pos.below())) && (!isConcrete || !isStuckInWater);
                            boolean bl = wouldSurvive = this.blockState.canSurvive(this.level(), pos) && !wouldContinueFalling;
                            if (mayReplace && wouldSurvive) {
                                if (this.blockState.hasProperty(BlockStateProperties.WATERLOGGED) && this.level().getFluidState(pos).is(Fluids.WATER)) {
                                    this.blockState = (BlockState)this.blockState.setValue(BlockStateProperties.WATERLOGGED, true);
                                }
                                if (this.level().setBlock(pos, this.blockState, 3)) {
                                    BlockEntity blockEntity;
                                    serverLevel.getChunkSource().chunkMap.sendToTrackingPlayers(this, new ClientboundBlockUpdatePacket(pos, this.level().getBlockState(pos)));
                                    this.discard();
                                    if (block instanceof Fallable) {
                                        Fallable fallable = (Fallable)((Object)block);
                                        fallable.onLand(this.level(), pos, this.blockState, currentState, this);
                                    }
                                    if (this.blockData != null && this.blockState.hasBlockEntity() && (blockEntity = this.level().getBlockEntity(pos)) != null) {
                                        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), LOGGER);){
                                            RegistryAccess registryAccess = this.level().registryAccess();
                                            TagValueOutput output = TagValueOutput.createWithContext(reporter, registryAccess);
                                            blockEntity.saveWithoutMetadata(output);
                                            CompoundTag merged = output.buildResult();
                                            this.blockData.forEach((name, tag) -> merged.put((String)name, tag.copy()));
                                            blockEntity.loadWithComponents(TagValueInput.create((ProblemReporter)reporter, (HolderLookup.Provider)registryAccess, merged));
                                        }
                                        catch (Exception e) {
                                            LOGGER.error("Failed to load block entity from falling block", (Throwable)e);
                                        }
                                        blockEntity.setChanged();
                                    }
                                } else if (this.dropItem && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS).booleanValue()) {
                                    this.discard();
                                    this.callOnBrokenAfterFall(block, pos);
                                    this.spawnAtLocation(serverLevel, block);
                                }
                            } else {
                                this.discard();
                                if (this.dropItem && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS).booleanValue()) {
                                    this.callOnBrokenAfterFall(block, pos);
                                    this.spawnAtLocation(serverLevel, block);
                                }
                            }
                        } else {
                            this.discard();
                            this.callOnBrokenAfterFall(block, pos);
                        }
                    }
                } else if (this.time > 100 && (pos.getY() <= this.level().getMinY() || pos.getY() > this.level().getMaxY()) || this.time > 600) {
                    if (this.dropItem && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS).booleanValue()) {
                        this.spawnAtLocation(serverLevel, block);
                    }
                    this.discard();
                }
            }
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(this.getAirDrag()));
    }

    public void callOnBrokenAfterFall(Block block, BlockPos pos) {
        if (block instanceof Fallable) {
            Fallable fallable = (Fallable)((Object)block);
            fallable.onBrokenAfterFall(this.level(), pos, this);
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        DamageSource damageSource2;
        if (!this.hurtEntities) {
            return false;
        }
        int fallDistanceInt = Mth.ceil(fallDistance - 1.0);
        if (fallDistanceInt < 0) {
            return false;
        }
        Predicate<Entity> entitySelector = EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
        Block block = this.blockState.getBlock();
        if (block instanceof Fallable) {
            Fallable fallable = (Fallable)((Object)block);
            damageSource2 = fallable.getFallDamageSource(this);
        } else {
            damageSource2 = this.damageSources().fallingBlock(this);
        }
        DamageSource actualDamageSource = damageSource2;
        float damage = Math.min(Mth.floor((float)fallDistanceInt * this.fallDamagePerDistance), this.fallDamageMax);
        this.level().getEntities(this, this.getBoundingBox(), entitySelector).forEach(entity -> entity.hurt(actualDamageSource, damage));
        boolean isAnvil = this.blockState.is(BlockTags.ANVIL);
        if (isAnvil && damage > 0.0f && this.random.nextFloat() < 0.05f + (float)fallDistanceInt * 0.05f) {
            BlockState newBlockState = AnvilBlock.damage(this.blockState);
            if (newBlockState == null) {
                this.cancelDrop = true;
            } else {
                this.blockState = newBlockState;
            }
        }
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("BlockState", BlockState.CODEC, this.blockState);
        output.putInt("Time", this.time);
        output.putBoolean("DropItem", this.dropItem);
        output.putBoolean("HurtEntities", this.hurtEntities);
        output.putFloat("FallHurtAmount", this.fallDamagePerDistance);
        output.putInt("FallHurtMax", this.fallDamageMax);
        if (this.blockData != null) {
            output.store("TileEntityData", CompoundTag.CODEC, this.blockData);
        }
        output.putBoolean("CancelDrop", this.cancelDrop);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.blockState = input.read("BlockState", BlockState.CODEC).orElse(DEFAULT_BLOCK_STATE);
        this.time = input.getIntOr("Time", 0);
        boolean defaultHurtEntities = this.blockState.is(BlockTags.ANVIL);
        this.hurtEntities = input.getBooleanOr("HurtEntities", defaultHurtEntities);
        this.fallDamagePerDistance = input.getFloatOr("FallHurtAmount", 0.0f);
        this.fallDamageMax = input.getIntOr("FallHurtMax", 40);
        this.dropItem = input.getBooleanOr("DropItem", true);
        this.blockData = input.read("TileEntityData", CompoundTag.CODEC).orElse(null);
        this.cancelDrop = input.getBooleanOr("CancelDrop", false);
    }

    public void setHurtsEntities(float damagePerDistance, int damageMax) {
        this.hurtEntities = true;
        this.fallDamagePerDistance = damagePerDistance;
        this.fallDamageMax = damageMax;
    }

    public void disableDrop() {
        this.cancelDrop = true;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public void fillCrashReportCategory(CrashReportCategory category) {
        super.fillCrashReportCategory(category);
        category.setDetail("Immitating BlockState", this.blockState.toString());
    }

    public BlockState getBlockState() {
        return this.blockState;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.minecraft.falling_block_type", this.blockState.getBlock().getName());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket((Entity)this, serverEntity, Block.getId(this.getBlockState()));
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.blockState = Block.stateById(packet.getData());
        this.blocksBuilding = true;
        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        this.setPos(x, y, z);
        this.setStartPos(this.blockPosition());
    }

    @Override
    public @Nullable Entity teleport(TeleportTransition transition) {
        ResourceKey<Level> newDimension = transition.newLevel().dimension();
        ResourceKey<Level> oldDimension = this.level().dimension();
        boolean fromOrToEnd = (oldDimension == Level.END || newDimension == Level.END) && oldDimension != newDimension;
        Entity newEntity = super.teleport(transition);
        this.forceTickAfterTeleportToDuplicate = newEntity != null && fromOrToEnd;
        return newEntity;
    }
}

