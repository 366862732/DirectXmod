/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public class UseBonemeal
extends Behavior<Villager> {
    private static final int BONEMEALING_DURATION = 80;
    private long nextWorkCycleTime;
    private long lastBonemealingSession;
    private int timeWorkedSoFar;
    private Optional<BlockPos> cropPos = Optional.empty();

    public UseBonemeal() {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT)));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager body) {
        if (body.tickCount % 10 != 0 || this.lastBonemealingSession != 0L && this.lastBonemealingSession + 160L > (long)body.tickCount) {
            return false;
        }
        if (body.getInventory().countItem(Items.BONE_MEAL) <= 0) {
            return false;
        }
        this.cropPos = this.pickNextTarget(level, body);
        return this.cropPos.isPresent();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager body, long timestamp) {
        return this.timeWorkedSoFar < 80 && this.cropPos.isPresent();
    }

    private Optional<BlockPos> pickNextTarget(ServerLevel level, Villager body) {
        BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();
        RandomSource random = level.getRandom();
        Optional<BlockPos> result = Optional.empty();
        int count = 0;
        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                for (int z = -1; z <= 1; ++z) {
                    mutPos.setWithOffset(body.blockPosition(), x, y, z);
                    if (!this.validPos(mutPos, level) || random.nextInt(++count) != 0) continue;
                    result = Optional.of(mutPos.immutable());
                }
            }
        }
        return result;
    }

    private boolean validPos(BlockPos blockPos, ServerLevel level) {
        CropBlock cropBlock;
        BlockState state = level.getBlockState(blockPos);
        Block block = state.getBlock();
        return block instanceof CropBlock && !(cropBlock = (CropBlock)block).isMaxAge(state);
    }

    @Override
    protected void start(ServerLevel level, Villager body, long timestamp) {
        this.setCurrentCropAsTarget(body);
        body.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BONE_MEAL));
        this.nextWorkCycleTime = timestamp;
        this.timeWorkedSoFar = 0;
    }

    private void setCurrentCropAsTarget(Villager body) {
        this.cropPos.ifPresent(pos -> {
            BlockPosTracker cropPosWrapper = new BlockPosTracker((BlockPos)pos);
            body.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, cropPosWrapper);
            body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(cropPosWrapper, 0.5f, 1));
        });
    }

    @Override
    protected void stop(ServerLevel level, Villager body, long timestamp) {
        body.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.lastBonemealingSession = body.tickCount;
    }

    @Override
    protected void tick(ServerLevel level, Villager body, long timestamp) {
        BlockPos targetPos = this.cropPos.get();
        if (timestamp < this.nextWorkCycleTime || !targetPos.closerToCenterThan(body.position(), 1.0)) {
            return;
        }
        ItemStack bonemealStack = ItemStack.EMPTY;
        SimpleContainer inventory = body.getInventory();
        int containerSize = inventory.getContainerSize();
        for (int i = 0; i < containerSize; ++i) {
            ItemStack item = inventory.getItem(i);
            if (!item.is(Items.BONE_MEAL)) continue;
            bonemealStack = item;
            break;
        }
        if (!bonemealStack.isEmpty() && BoneMealItem.growCrop(bonemealStack, level, targetPos)) {
            level.levelEvent(1505, targetPos, 15);
            this.cropPos = this.pickNextTarget(level, body);
            this.setCurrentCropAsTarget(body);
            this.nextWorkCycleTime = timestamp + 40L;
        }
        ++this.timeWorkedSoFar;
    }
}

