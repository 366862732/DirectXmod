/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Lists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public class HarvestFarmland
extends Behavior<Villager> {
    private static final int HARVEST_DURATION = 200;
    public static final float SPEED_MODIFIER = 0.5f;
    private @Nullable BlockPos aboveFarmlandPos;
    private long nextOkStartTime;
    private int timeWorkedSoFar;
    private final List<BlockPos> validFarmlandAroundVillager = Lists.newArrayList();

    public HarvestFarmland() {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.SECONDARY_JOB_SITE, (Object)((Object)MemoryStatus.VALUE_PRESENT)));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager body) {
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING).booleanValue()) {
            return false;
        }
        if (!body.getVillagerData().profession().is(VillagerProfession.FARMER)) {
            return false;
        }
        BlockPos.MutableBlockPos mutPos = body.blockPosition().mutable();
        this.validFarmlandAroundVillager.clear();
        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                for (int z = -1; z <= 1; ++z) {
                    mutPos.set(body.getX() + (double)x, body.getY() + (double)y, body.getZ() + (double)z);
                    if (!this.validPos(mutPos, level)) continue;
                    this.validFarmlandAroundVillager.add(new BlockPos(mutPos));
                }
            }
        }
        this.aboveFarmlandPos = this.getValidFarmland(level);
        return this.aboveFarmlandPos != null;
    }

    private @Nullable BlockPos getValidFarmland(ServerLevel level) {
        return this.validFarmlandAroundVillager.isEmpty() ? null : this.validFarmlandAroundVillager.get(level.getRandom().nextInt(this.validFarmlandAroundVillager.size()));
    }

    private boolean validPos(BlockPos blockPos, ServerLevel level) {
        CropBlock cropBlock;
        BlockState state = level.getBlockState(blockPos);
        Block block = state.getBlock();
        Block blockBelow = level.getBlockState(blockPos.below()).getBlock();
        return block instanceof CropBlock && (cropBlock = (CropBlock)block).isMaxAge(state) || state.isAir() && blockBelow instanceof FarmlandBlock;
    }

    @Override
    protected void start(ServerLevel level, Villager body, long timestamp) {
        if (timestamp > this.nextOkStartTime && this.aboveFarmlandPos != null) {
            body.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.aboveFarmlandPos));
            body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosTracker(this.aboveFarmlandPos), 0.5f, 1));
        }
    }

    @Override
    protected void stop(ServerLevel level, Villager body, long timestamp) {
        body.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.timeWorkedSoFar = 0;
        this.nextOkStartTime = timestamp + 40L;
    }

    @Override
    protected void tick(ServerLevel level, Villager body, long timestamp) {
        if (this.aboveFarmlandPos != null && !this.aboveFarmlandPos.closerToCenterThan(body.position(), 1.0)) {
            return;
        }
        if (this.aboveFarmlandPos != null && timestamp > this.nextOkStartTime) {
            CropBlock cropBlock;
            BlockState blockState = level.getBlockState(this.aboveFarmlandPos);
            Block block = blockState.getBlock();
            Block blockBelow = level.getBlockState(this.aboveFarmlandPos.below()).getBlock();
            if (block instanceof CropBlock && (cropBlock = (CropBlock)block).isMaxAge(blockState)) {
                level.destroyBlock(this.aboveFarmlandPos, true, body);
            }
            if (blockState.isAir() && blockBelow instanceof FarmlandBlock && body.hasFarmSeeds()) {
                SimpleContainer inventory = body.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); ++i) {
                    Item item;
                    ItemStack itemStack = inventory.getItem(i);
                    boolean ok = false;
                    if (!itemStack.isEmpty() && itemStack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS) && (item = itemStack.getItem()) instanceof BlockItem) {
                        BlockItem blockItem = (BlockItem)item;
                        BlockState place = blockItem.getBlock().defaultBlockState();
                        level.setBlockAndUpdate(this.aboveFarmlandPos, place);
                        level.gameEvent(GameEvent.BLOCK_PLACE, this.aboveFarmlandPos, GameEvent.Context.of(body, place));
                        ok = true;
                    }
                    if (!ok) continue;
                    level.playSound(null, (double)this.aboveFarmlandPos.getX(), (double)this.aboveFarmlandPos.getY(), (double)this.aboveFarmlandPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
                    itemStack.shrink(1);
                    if (!itemStack.isEmpty()) break;
                    inventory.setItem(i, ItemStack.EMPTY);
                    break;
                }
            }
            if (block instanceof CropBlock && !(cropBlock = (CropBlock)block).isMaxAge(blockState)) {
                this.validFarmlandAroundVillager.remove(this.aboveFarmlandPos);
                this.aboveFarmlandPos = this.getValidFarmland(level);
                if (this.aboveFarmlandPos != null) {
                    this.nextOkStartTime = timestamp + 20L;
                    body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosTracker(this.aboveFarmlandPos), 0.5f, 1));
                    body.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.aboveFarmlandPos));
                }
            }
        }
        ++this.timeWorkedSoFar;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager body, long timestamp) {
        return this.timeWorkedSoFar < 200;
    }
}

