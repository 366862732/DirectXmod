/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.projectile.throwableitemprojectile;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ThrownEnderpearl
extends ThrowableItemProjectile {
    private long ticketTimer = 0L;

    public ThrownEnderpearl(EntityType<? extends ThrownEnderpearl> type, Level level) {
        super((EntityType<? extends ThrowableItemProjectile>)type, level);
    }

    public ThrownEnderpearl(Level level, LivingEntity mob, ItemStack itemStack) {
        super(EntityTypes.ENDER_PEARL, mob, level, itemStack);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.ENDER_PEARL;
    }

    @Override
    protected void setOwner(@Nullable EntityReference<Entity> owner) {
        this.deregisterFromCurrentOwner();
        super.setOwner(owner);
        this.registerToCurrentOwner();
    }

    private void deregisterFromCurrentOwner() {
        Entity entity = this.getOwner();
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            serverPlayer.deregisterEnderPearl(this);
        }
    }

    private void registerToCurrentOwner() {
        Entity entity = this.getOwner();
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            serverPlayer.registerEnderPearl(this);
        }
    }

    @Override
    public @Nullable Entity getOwner() {
        Level level;
        if (this.owner == null || !((level = this.level()) instanceof ServerLevel)) {
            return super.getOwner();
        }
        ServerLevel serverLevel = (ServerLevel)level;
        return this.owner.getEntity(serverLevel, Entity.class);
    }

    private static @Nullable Entity findOwnerIncludingDeadPlayer(ServerLevel serverLevel, UUID uuid) {
        Entity owner = serverLevel.getEntityInAnyDimension(uuid);
        if (owner != null) {
            return owner;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(uuid);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        hitResult.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0f);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        ServerLevel level;
        block15: {
            block14: {
                super.onHit(hitResult);
                for (int i = 0; i < 32; ++i) {
                    this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY() + this.random.nextDouble() * 2.0, this.getZ(), this.random.nextGaussian(), 0.0, this.random.nextGaussian());
                }
                Level level2 = this.level();
                if (!(level2 instanceof ServerLevel)) break block14;
                level = (ServerLevel)level2;
                if (!this.isRemoved()) break block15;
            }
            return;
        }
        Entity owner = this.getOwner();
        if (owner == null || !ThrownEnderpearl.isAllowedToTeleportOwner(owner, level)) {
            this.discard();
            return;
        }
        Vec3 teleportPos = this.oldPosition();
        if (owner instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)owner;
            if (player.connection.isAcceptingMessages()) {
                ServerPlayer newOwner;
                Endermite endermite;
                if (this.random.nextFloat() < 0.05f && level.isSpawningMonsters() && level.getLevelData().getDifficulty() != Difficulty.PEACEFUL && (endermite = EntityTypes.ENDERMITE.create((Level)level, EntitySpawnReason.TRIGGERED)) != null) {
                    endermite.snapTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
                    level.addFreshEntity(endermite);
                }
                if (this.isOnPortalCooldown()) {
                    owner.setPortalCooldown();
                }
                if ((newOwner = player.teleport(new TeleportTransition(level, teleportPos, Vec3.ZERO, 0.0f, 0.0f, Relative.union(Relative.ROTATION, Relative.DELTA), TeleportTransition.DO_NOTHING))) != null) {
                    newOwner.resetFallDistance();
                    newOwner.resetCurrentImpulseContext();
                    newOwner.hurtServer(player.level(), this.damageSources().enderPearl(), 5.0f);
                }
                this.playSound(level, teleportPos);
            }
        } else {
            Entity newOwner = owner.teleport(new TeleportTransition(level, teleportPos, owner.getDeltaMovement(), owner.getYRot(), owner.getXRot(), TeleportTransition.DO_NOTHING));
            if (newOwner != null) {
                newOwner.resetFallDistance();
            }
            if (newOwner instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)newOwner;
                livingEntity.resetCurrentImpulseContext();
            }
            this.playSound(level, teleportPos);
        }
        this.discard();
    }

    private static boolean isAllowedToTeleportOwner(Entity owner, Level newLevel) {
        if (owner.level().dimension() == newLevel.dimension()) {
            if (owner instanceof LivingEntity) {
                LivingEntity livingOwner = (LivingEntity)owner;
                return livingOwner.isAlive() && !livingOwner.isSleeping();
            }
            return owner.isAlive();
        }
        return owner.canUsePortal(true);
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public void tick() {
        var2_1 = this.level();
        if (!(var2_1 instanceof ServerLevel)) {
            super.tick();
            return;
        }
        serverLevel = (ServerLevel)var2_1;
        previousChunkX = SectionPos.blockToSectionCoord(this.position().x());
        previousChunkZ = SectionPos.blockToSectionCoord(this.position().z());
        v0 = owner = this.owner != null ? ThrownEnderpearl.findOwnerIncludingDeadPlayer(serverLevel, this.owner.getUUID()) : null;
        if (!(owner instanceof ServerPlayer)) ** GOTO lbl-1000
        serverPlayer = (ServerPlayer)owner;
        if (!owner.isAlive() && !serverPlayer.wonGame && serverPlayer.level().getGameRules().get(GameRules.ENDER_PEARLS_VANISH_ON_DEATH).booleanValue()) {
            this.discard();
        } else lbl-1000:
        // 2 sources

        {
            super.tick();
        }
        if (!this.isAlive()) {
            return;
        }
        currentPos = BlockPos.containing(this.position());
        if ((--this.ticketTimer <= 0L || previousChunkX != SectionPos.blockToSectionCoord(currentPos.getX()) || previousChunkZ != SectionPos.blockToSectionCoord(currentPos.getZ())) && owner instanceof ServerPlayer) {
            serverPlayer = (ServerPlayer)owner;
            this.ticketTimer = serverPlayer.registerAndUpdateEnderPearlTicket(this);
        }
    }

    private void playSound(Level level, Vec3 position) {
        level.playSound(null, position.x, position.y, position.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS);
    }

    @Override
    public @Nullable Entity teleport(TeleportTransition transition) {
        Entity newEntity = super.teleport(transition);
        if (newEntity != null) {
            newEntity.placePortalTicket(BlockPos.containing(newEntity.position()));
        }
        return newEntity;
    }

    @Override
    public boolean canTeleport(Level from, Level to) {
        Entity entity;
        if (from.dimension() == Level.END && to.dimension() == Level.OVERWORLD && (entity = this.getOwner()) instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            return super.canTeleport(from, to) && player.seenCredits;
        }
        return super.canTeleport(from, to);
    }

    @Override
    protected void onInsideBlock(BlockState state) {
        Entity entity;
        super.onInsideBlock(state);
        if (state.is(Blocks.END_GATEWAY) && (entity = this.getOwner()) instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            player.onInsideBlock(state);
        }
    }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        if (reason != Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            this.deregisterFromCurrentOwner();
        }
        super.onRemoval(reason);
    }

    @Override
    public void onAboveBubbleColumn(boolean dragDown, BlockPos pos) {
        Entity.handleOnAboveBubbleColumn(this, dragDown, pos);
    }

    @Override
    public void onInsideBubbleColumn(boolean dragDown) {
        Entity.handleOnInsideBubbleColumn(this, dragDown);
    }
}

