/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ShulkerBoxBlockEntity
extends RandomizableContainerBlockEntity
implements WorldlyContainer {
    public static final int COLUMNS = 9;
    public static final int ROWS = 3;
    public static final int CONTAINER_SIZE = 27;
    public static final int EVENT_SET_OPEN_COUNT = 1;
    public static final int OPENING_TICK_LENGTH = 10;
    public static final float MAX_LID_HEIGHT = 0.5f;
    public static final float MAX_LID_ROTATION = 270.0f;
    private static final int[] SLOTS = IntStream.range(0, 27).toArray();
    private static final Component DEFAULT_NAME = Component.translatable("container.shulkerBox");
    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(27, ItemStack.EMPTY);
    private int openCount;
    private AnimationStatus animationStatus = AnimationStatus.CLOSED;
    private float progress;
    private float progressOld;
    private final @Nullable DyeColor color;

    public ShulkerBoxBlockEntity(@Nullable DyeColor color, BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.SHULKER_BOX, worldPosition, blockState);
        this.color = color;
    }

    public ShulkerBoxBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.SHULKER_BOX, worldPosition, blockState);
        DyeColor dyeColor;
        Block block = blockState.getBlock();
        if (block instanceof ShulkerBoxBlock) {
            ShulkerBoxBlock shulkerBoxBlock = (ShulkerBoxBlock)block;
            dyeColor = shulkerBoxBlock.getColor();
        } else {
            dyeColor = null;
        }
        this.color = dyeColor;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShulkerBoxBlockEntity entity) {
        entity.updateAnimation(level, pos, state);
    }

    private void updateAnimation(Level level, BlockPos pos, BlockState blockState) {
        this.progressOld = this.progress;
        switch (this.animationStatus.ordinal()) {
            case 0: {
                this.progress = 0.0f;
                break;
            }
            case 1: {
                this.progress += 0.1f;
                if (this.progressOld == 0.0f) {
                    ShulkerBoxBlockEntity.doNeighborUpdates(level, pos, blockState);
                }
                if (this.progress >= 1.0f) {
                    this.animationStatus = AnimationStatus.OPENED;
                    this.progress = 1.0f;
                    ShulkerBoxBlockEntity.doNeighborUpdates(level, pos, blockState);
                }
                this.moveCollidedEntities(level, pos, blockState);
                break;
            }
            case 3: {
                this.progress -= 0.1f;
                if (this.progressOld == 1.0f) {
                    ShulkerBoxBlockEntity.doNeighborUpdates(level, pos, blockState);
                }
                if (!(this.progress <= 0.0f)) break;
                this.animationStatus = AnimationStatus.CLOSED;
                this.progress = 0.0f;
                ShulkerBoxBlockEntity.doNeighborUpdates(level, pos, blockState);
                break;
            }
            case 2: {
                this.progress = 1.0f;
            }
        }
    }

    public AnimationStatus getAnimationStatus() {
        return this.animationStatus;
    }

    public AABB getBoundingBox(BlockState state) {
        Vec3 bottomCenter = new Vec3(0.5, 0.0, 0.5);
        return Shulker.getProgressAabb(1.0f, state.getValue(ShulkerBoxBlock.FACING), 0.5f * this.getProgress(1.0f), bottomCenter);
    }

    private void moveCollidedEntities(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
            return;
        }
        Direction direction = state.getValue(ShulkerBoxBlock.FACING);
        AABB aabb = Shulker.getProgressDeltaAabb(1.0f, direction, this.progressOld, this.progress, Vec3.atBottomCenterOf(pos));
        List<Entity> entities = level.getEntities(null, aabb);
        if (entities.isEmpty()) {
            return;
        }
        for (Entity entity : entities) {
            if (entity.getPistonPushReaction() == PushReaction.IGNORE) continue;
            entity.move(MoverType.SHULKER_BOX, new Vec3((aabb.getXsize() + 0.01) * (double)direction.getStepX(), (aabb.getYsize() + 0.01) * (double)direction.getStepY(), (aabb.getZsize() + 0.01) * (double)direction.getStepZ()));
        }
    }

    @Override
    public int getContainerSize() {
        return this.itemStacks.size();
    }

    @Override
    public boolean triggerEvent(int b0, int b1) {
        if (b0 == 1) {
            this.openCount = b1;
            if (b1 == 0) {
                this.animationStatus = AnimationStatus.CLOSING;
            }
            if (b1 == 1) {
                this.animationStatus = AnimationStatus.OPENING;
            }
            return true;
        }
        return super.triggerEvent(b0, b1);
    }

    private static void doNeighborUpdates(Level level, BlockPos pos, BlockState blockState) {
        blockState.updateNeighbourShapes(level, pos, 3);
        level.updateNeighborsAt(pos, blockState.getBlock());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    public void startOpen(ContainerUser containerUser) {
        if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
            if (this.openCount < 0) {
                this.openCount = 0;
            }
            ++this.openCount;
            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
            if (this.openCount == 1) {
                this.level.gameEvent((Entity)containerUser.getLivingEntity(), GameEvent.CONTAINER_OPEN, this.worldPosition);
                this.level.playSound(null, this.worldPosition, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.5f, this.level.getRandom().nextFloat() * 0.1f + 0.9f);
            }
        }
    }

    @Override
    public void stopOpen(ContainerUser containerUser) {
        if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
            --this.openCount;
            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
            if (this.openCount <= 0) {
                this.level.gameEvent((Entity)containerUser.getLivingEntity(), GameEvent.CONTAINER_CLOSE, this.worldPosition);
                this.level.playSound(null, this.worldPosition, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.5f, this.level.getRandom().nextFloat() * 0.1f + 0.9f);
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.loadFromTag(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, this.itemStacks, false);
        }
    }

    public void loadFromTag(ValueInput input) {
        this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, this.itemStacks);
        }
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.itemStacks;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.itemStacks = items;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        return !(Block.byItem(itemStack.getItem()) instanceof ShulkerBoxBlock);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return true;
    }

    public float getProgress(float a) {
        return Mth.lerp(a, this.progressOld, this.progress);
    }

    public @Nullable DyeColor getColor() {
        return this.color;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ShulkerBoxMenu(containerId, inventory, this);
    }

    public boolean isClosed() {
        return this.animationStatus == AnimationStatus.CLOSED;
    }

    public static enum AnimationStatus {
        CLOSED,
        OPENING,
        OPENED,
        CLOSING;

    }
}

