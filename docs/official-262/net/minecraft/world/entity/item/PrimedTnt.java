/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.item;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class PrimedTnt
extends Entity
implements TraceableEntity {
    private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(PrimedTnt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE_ID = SynchedEntityData.defineId(PrimedTnt.class, EntityDataSerializers.BLOCK_STATE);
    public static final short DEFAULT_FUSE_TIME = 80;
    public static final int NO_FUSE = -1;
    private static final float DEFAULT_EXPLOSION_POWER = 4.0f;
    private static final BlockState DEFAULT_BLOCK_STATE = Blocks.TNT.defaultBlockState();
    private static final String TAG_BLOCK_STATE = "block_state";
    public static final String TAG_FUSE = "fuse";
    private static final String TAG_EXPLOSION_POWER = "explosion_power";
    public static final ExplosionDamageCalculator USED_PORTAL_DAMAGE_CALCULATOR = new ExplosionDamageCalculator(){

        @Override
        public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
            if (state.is(Blocks.NETHER_PORTAL)) {
                return false;
            }
            return super.shouldBlockExplode(explosion, level, pos, state, power);
        }

        @Override
        public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState block, FluidState fluid) {
            if (block.is(Blocks.NETHER_PORTAL)) {
                return Optional.empty();
            }
            return super.getBlockExplosionResistance(explosion, level, pos, block, fluid);
        }
    };
    private @Nullable EntityReference<LivingEntity> owner;
    private boolean usedPortal;
    private float explosionPower = 4.0f;

    public PrimedTnt(EntityType<? extends PrimedTnt> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public PrimedTnt(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this((EntityType<? extends PrimedTnt>)EntityTypes.TNT, level);
        this.setPos(x, y, z);
        double rot = level.getRandom().nextDouble() * 6.2831854820251465;
        this.setDeltaMovement(-Math.sin(rot) * 0.02, 0.2f, -Math.cos(rot) * 0.02);
        this.setFuse(80);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.owner = EntityReference.of(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_FUSE_ID, 80);
        entityData.define(DATA_BLOCK_STATE_ID, DEFAULT_BLOCK_STATE);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
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
        this.handlePortal();
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.applyEffectsFromBlocks();
        this.setDeltaMovement(this.getDeltaMovement().scale(this.getAirDrag()));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
        }
        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);
        if (fuse <= 0) {
            this.discard();
            if (!this.level().isClientSide()) {
                this.explode();
            }
        } else {
            this.updateFluidInteraction();
            if (this.level().isClientSide()) {
                this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    private void explode() {
        ServerLevel level;
        Level level2 = this.level();
        if (level2 instanceof ServerLevel && (level = (ServerLevel)level2).getGameRules().get(GameRules.TNT_EXPLODES).booleanValue()) {
            this.level().explode(this, Explosion.getDefaultDamageSource(this.level(), this), this.usedPortal ? USED_PORTAL_DAMAGE_CALCULATOR : null, this.getX(), this.getY(0.0625), this.getZ(), this.explosionPower, false, Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putShort(TAG_FUSE, (short)this.getFuse());
        output.store(TAG_BLOCK_STATE, BlockState.CODEC, this.getBlockState());
        if (this.explosionPower != 4.0f) {
            output.putFloat(TAG_EXPLOSION_POWER, this.explosionPower);
        }
        EntityReference.store(this.owner, output, "owner");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setFuse(input.getShortOr(TAG_FUSE, (short)80));
        this.setBlockState(input.read(TAG_BLOCK_STATE, BlockState.CODEC).orElse(DEFAULT_BLOCK_STATE));
        this.explosionPower = Mth.clamp(input.getFloatOr(TAG_EXPLOSION_POWER, 4.0f), 0.0f, 128.0f);
        this.owner = EntityReference.read(input, "owner");
    }

    @Override
    public @Nullable LivingEntity getOwner() {
        return EntityReference.getLivingEntity(this.owner, this.level());
    }

    @Override
    public void restoreFrom(Entity oldEntity) {
        super.restoreFrom(oldEntity);
        if (oldEntity instanceof PrimedTnt) {
            PrimedTnt primedTnt = (PrimedTnt)oldEntity;
            this.owner = primedTnt.owner;
        }
    }

    public void setFuse(int time) {
        this.entityData.set(DATA_FUSE_ID, time);
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE_ID);
    }

    public static int getRandomShortFuse(int fuse, RandomSource random) {
        return random.nextInt(Math.max(1, fuse / 4)) + fuse / 8;
    }

    public void setBlockState(BlockState blockState) {
        this.entityData.set(DATA_BLOCK_STATE_ID, blockState);
    }

    public BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK_STATE_ID);
    }

    private void setUsedPortal(boolean usedPortal) {
        this.usedPortal = usedPortal;
    }

    @Override
    public @Nullable Entity teleport(TeleportTransition transition) {
        Entity newEntity = super.teleport(transition);
        if (newEntity instanceof PrimedTnt) {
            PrimedTnt tnt = (PrimedTnt)newEntity;
            tnt.setUsedPortal(true);
        }
        return newEntity;
    }

    @Override
    public final boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }
}

