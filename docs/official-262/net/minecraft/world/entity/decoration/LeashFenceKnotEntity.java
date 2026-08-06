/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.decoration;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LeashFenceKnotEntity
extends BlockAttachedEntity {
    public static final double OFFSET_Y = 0.375;

    public LeashFenceKnotEntity(EntityType<? extends LeashFenceKnotEntity> type, Level level) {
        super((EntityType<? extends BlockAttachedEntity>)type, level);
    }

    public LeashFenceKnotEntity(Level level, BlockPos pos) {
        super(EntityTypes.LEASH_KNOT, level, pos);
        this.setPos(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    protected void recalculateBoundingBox() {
        this.setPosRaw((double)this.pos.getX() + 0.5, (double)this.pos.getY() + 0.375, (double)this.pos.getZ() + 0.5);
        double halfWidth = (double)this.getType().getWidth() / 2.0;
        double height = this.getType().getHeight();
        this.setBoundingBox(new AABB(this.getX() - halfWidth, this.getY(), this.getZ() - halfWidth, this.getX() + halfWidth, this.getY() + height, this.getZ() + halfWidth));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 1024.0;
    }

    @Override
    public void dropItem(ServerLevel level, @Nullable Entity causedBy) {
        this.playSound(SoundEvents.LEAD_UNTIED, 1.0f, 1.0f);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult.Success success;
        InteractionResult result;
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.getItemInHand(hand).is(Items.SHEARS) && (result = super.interact(player, hand, location)) instanceof InteractionResult.Success && (success = (InteractionResult.Success)result).wasItemInteraction()) {
            return result;
        }
        boolean attachedMob = false;
        List<Leashable> playerLeashable = Leashable.leashableLeashedTo(player);
        for (Leashable leashable : playerLeashable) {
            if (!leashable.canHaveALeashAttachedTo(this)) continue;
            leashable.setLeashedTo(this, true);
            attachedMob = true;
        }
        boolean anyDropped = false;
        if (!attachedMob && !player.isSecondaryUseActive()) {
            List<Leashable> knotLeashable = Leashable.leashableLeashedTo(this);
            for (Leashable mob : knotLeashable) {
                if (!mob.canHaveALeashAttachedTo(player)) continue;
                mob.setLeashedTo(player, true);
                anyDropped = true;
            }
        }
        if (attachedMob || anyDropped) {
            this.gameEvent(GameEvent.BLOCK_ATTACH, player);
            this.playSound(SoundEvents.LEAD_TIED);
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
    }

    @Override
    public void notifyLeasheeRemoved(Leashable entity) {
        if (Leashable.leashableLeashedTo(this).isEmpty()) {
            this.discard();
        }
    }

    @Override
    public boolean survives() {
        return this.level().getBlockState(this.pos).is(BlockTags.FENCES);
    }

    public static LeashFenceKnotEntity getOrCreateKnot(Level level, BlockPos pos) {
        return LeashFenceKnotEntity.getKnot(level, pos).orElseGet(() -> LeashFenceKnotEntity.createKnot(level, pos));
    }

    public static Optional<LeashFenceKnotEntity> getKnot(Level level, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        List<LeashFenceKnotEntity> knots = level.getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB((double)x - 1.0, (double)y - 1.0, (double)z - 1.0, (double)x + 1.0, (double)y + 1.0, (double)z + 1.0));
        for (LeashFenceKnotEntity knot : knots) {
            if (!knot.getPos().equals(pos)) continue;
            return Optional.of(knot);
        }
        return Optional.empty();
    }

    public static LeashFenceKnotEntity createKnot(Level level, BlockPos pos) {
        LeashFenceKnotEntity knot = new LeashFenceKnotEntity(level, pos);
        level.addFreshEntity(knot);
        return knot;
    }

    public void playPlacementSound() {
        this.playSound(SoundEvents.LEAD_TIED, 1.0f, 1.0f);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket((Entity)this, 0, this.getPos());
    }

    @Override
    public Vec3 getRopeHoldPosition(float partialTickTime) {
        return this.getPosition(partialTickTime).add(0.0, 0.2, 0.0);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.LEAD);
    }
}

