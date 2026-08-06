/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.decoration.painting;

import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Painting
extends HangingEntity {
    private static final EntityDataAccessor<Holder<PaintingVariant>> DATA_PAINTING_VARIANT_ID = SynchedEntityData.defineId(Painting.class, EntityDataSerializers.PAINTING_VARIANT);
    public static final float DEPTH = 0.0625f;

    public Painting(EntityType<? extends Painting> type, Level level) {
        super((EntityType<? extends HangingEntity>)type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_PAINTING_VARIANT_ID, VariantUtils.getAny(this.registryAccess(), Registries.PAINTING_VARIANT));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_PAINTING_VARIANT_ID.equals(accessor)) {
            this.recalculateBoundingBox();
        }
    }

    private void setVariant(Holder<PaintingVariant> variant) {
        this.entityData.set(DATA_PAINTING_VARIANT_ID, variant);
    }

    public Holder<PaintingVariant> getVariant() {
        return this.entityData.get(DATA_PAINTING_VARIANT_ID);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.PAINTING_VARIANT) {
            return Painting.castComponentValue(type, this.getVariant());
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.PAINTING_VARIANT);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.PAINTING_VARIANT) {
            this.setVariant(Painting.castComponentValue(DataComponents.PAINTING_VARIANT, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    public static Optional<Painting> create(Level level, BlockPos pos, Direction direction) {
        Painting candidate = new Painting(level, pos);
        ArrayList<Holder> potentialVariants = new ArrayList<Holder>();
        level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).getTagOrEmpty(PaintingVariantTags.PLACEABLE).forEach(potentialVariants::add);
        if (potentialVariants.isEmpty()) {
            return Optional.empty();
        }
        candidate.setDirection(direction);
        potentialVariants.removeIf(variant -> {
            candidate.setVariant((Holder<PaintingVariant>)variant);
            return !candidate.survives();
        });
        if (potentialVariants.isEmpty()) {
            return Optional.empty();
        }
        int largestPaintingAreaSize = potentialVariants.stream().mapToInt(Painting::variantArea).max().orElse(0);
        potentialVariants.removeIf(variant -> Painting.variantArea(variant) < largestPaintingAreaSize);
        Optional selectedVariant = Util.getRandomSafe(potentialVariants, candidate.random);
        if (selectedVariant.isEmpty()) {
            return Optional.empty();
        }
        candidate.setVariant((Holder)selectedVariant.get());
        candidate.setDirection(direction);
        return Optional.of(candidate);
    }

    private static int variantArea(Holder<PaintingVariant> variant) {
        return variant.value().area();
    }

    private Painting(Level level, BlockPos blockPos) {
        super((EntityType<? extends HangingEntity>)EntityTypes.PAINTING, level, blockPos);
    }

    public Painting(Level level, BlockPos blockPos, Direction direction, Holder<PaintingVariant> variant) {
        this(level, blockPos);
        this.setVariant(variant);
        this.setDirection(direction);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("facing", Direction.LEGACY_ID_CODEC_2D, this.getDirection());
        super.addAdditionalSaveData(output);
        VariantUtils.writeVariant(output, this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        Direction direction = input.read("facing", Direction.LEGACY_ID_CODEC_2D).orElse(Direction.SOUTH);
        super.readAdditionalSaveData(input);
        this.setDirection(direction);
        VariantUtils.readVariant(input, Registries.PAINTING_VARIANT).ifPresent(this::setVariant);
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        float shiftToBlockWall = 0.46875f;
        Vec3 attachedToWall = Vec3.atCenterOf(pos).relative(direction, -0.46875);
        PaintingVariant variant = this.getVariant().value();
        double horizontalOffset = this.offsetForPaintingSize(variant.width());
        double verticalOffset = this.offsetForPaintingSize(variant.height());
        Direction left = direction.getCounterClockWise();
        Vec3 position = attachedToWall.relative(left, horizontalOffset).relative(Direction.UP, verticalOffset);
        Direction.Axis axis = direction.getAxis();
        double xSize = axis == Direction.Axis.X ? 0.0625 : (double)variant.width();
        double ySize = variant.height();
        double zSize = axis == Direction.Axis.Z ? 0.0625 : (double)variant.width();
        return AABB.ofSize(position, xSize, ySize, zSize);
    }

    private double offsetForPaintingSize(int size) {
        return size % 2 == 0 ? 0.5 : 0.0;
    }

    @Override
    public void dropItem(ServerLevel level, @Nullable Entity causedBy) {
        Player player;
        if (!level.getGameRules().get(GameRules.ENTITY_DROPS).booleanValue()) {
            return;
        }
        this.playSound(SoundEvents.PAINTING_BREAK, 1.0f, 1.0f);
        if (causedBy instanceof Player && (player = (Player)causedBy).hasInfiniteMaterials()) {
            return;
        }
        this.spawnAtLocation(level, Items.PAINTING);
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.PAINTING_PLACE, 1.0f, 1.0f);
    }

    @Override
    public void snapTo(double x, double y, double z, float yRot, float xRot) {
        this.setPos(x, y, z);
    }

    @Override
    public Vec3 trackingPosition() {
        return Vec3.atLowerCornerOf(this.pos);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket((Entity)this, this.getDirection().get3DDataValue(), this.getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.PAINTING);
    }
}

