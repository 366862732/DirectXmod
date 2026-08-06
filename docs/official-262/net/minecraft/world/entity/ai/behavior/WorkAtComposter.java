/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WorkAtComposter
extends WorkAtPoi {
    private static final List<Item> COMPOSTABLE_ITEMS = ImmutableList.of((Object)Items.WHEAT_SEEDS, (Object)Items.BEETROOT_SEEDS);

    @Override
    protected void useWorkstation(ServerLevel level, Villager body) {
        Optional<GlobalPos> jobSiteMemory = body.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (jobSiteMemory.isEmpty()) {
            return;
        }
        GlobalPos jobSitePos = jobSiteMemory.get();
        BlockState blockState = level.getBlockState(jobSitePos.pos());
        if (blockState.is(Blocks.COMPOSTER)) {
            this.makeBread(level, body);
            this.compostItems(level, body, jobSitePos, blockState);
        }
    }

    private void compostItems(ServerLevel level, Villager body, GlobalPos jobSitePos, BlockState blockState) {
        BlockPos pos = jobSitePos.pos();
        if (blockState.getValue(ComposterBlock.LEVEL) == 8) {
            blockState = ComposterBlock.extractProduce(body, blockState, level, pos);
        }
        int totalItemsToUse = 20;
        int minStackSize = 10;
        int[] itemsSeenSoFar = new int[COMPOSTABLE_ITEMS.size()];
        SimpleContainer inventory = body.getInventory();
        int containerSize = inventory.getContainerSize();
        BlockState tempState = blockState;
        for (int i = containerSize - 1; i >= 0 && totalItemsToUse > 0; --i) {
            int totalItemCount;
            ItemStack itemStack = inventory.getItem(i);
            int itemIndex = COMPOSTABLE_ITEMS.indexOf(itemStack.getItem());
            if (itemIndex == -1) continue;
            int stackSize = itemStack.getCount();
            itemsSeenSoFar[itemIndex] = totalItemCount = itemsSeenSoFar[itemIndex] + stackSize;
            int itemsToUse = Math.min(Math.min(totalItemCount - 10, totalItemsToUse), stackSize);
            if (itemsToUse <= 0) continue;
            totalItemsToUse -= itemsToUse;
            for (int j = 0; j < itemsToUse; ++j) {
                if ((tempState = ComposterBlock.insertItem(body, tempState, level, itemStack, pos)).getValue(ComposterBlock.LEVEL) != 7) continue;
                this.spawnComposterFillEffects(level, blockState, pos, tempState);
                return;
            }
        }
        this.spawnComposterFillEffects(level, blockState, pos, tempState);
    }

    private void spawnComposterFillEffects(ServerLevel level, BlockState blockState, BlockPos pos, BlockState newState) {
        level.levelEvent(1500, pos, newState != blockState ? 1 : 0);
    }

    private void makeBread(ServerLevel level, Villager body) {
        SimpleContainer inventory = body.getInventory();
        if (inventory.countItem(Items.BREAD) > 36) {
            return;
        }
        int howMuchWheatIHave = inventory.countItem(Items.WHEAT);
        int maxAmountOfBreadToMake = 3;
        int amountOfWheatNeededToCraftOneBread = 3;
        int howMuchBreadToMake = Math.min(3, howMuchWheatIHave / 3);
        if (howMuchBreadToMake == 0) {
            return;
        }
        int howMuchWheatToUse = howMuchBreadToMake * 3;
        inventory.removeItemType(Items.WHEAT, howMuchWheatToUse);
        ItemStack breadICantCarry = inventory.addItem(new ItemStack(Items.BREAD, howMuchBreadToMake));
        if (!breadICantCarry.isEmpty()) {
            body.spawnAtLocation(level, breadICantCarry, 0.5f);
        }
    }
}

