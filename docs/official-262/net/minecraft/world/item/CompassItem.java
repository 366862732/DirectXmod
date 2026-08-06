/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public class CompassItem
extends Item {
    private static final Component LODESTONE_COMPASS_NAME = Component.translatable("item.minecraft.lodestone_compass");

    public CompassItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack itemStack) {
        return itemStack.has(DataComponents.LODESTONE_TRACKER) || super.isFoil(itemStack);
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        LodestoneTracker newTracker;
        LodestoneTracker tracker = itemStack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker != null && (newTracker = tracker.tick(level)) != tracker) {
            itemStack.set(DataComponents.LODESTONE_TRACKER, newTracker);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos blockPos = context.getClickedPos();
        Level level = context.getLevel();
        if (level.getBlockState(blockPos).is(Blocks.LODESTONE)) {
            level.playSound(null, blockPos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
            Player player = context.getPlayer();
            ItemStack itemStack = context.getItemInHand();
            boolean replaceExistingStack = !player.hasInfiniteMaterials() && itemStack.getCount() == 1;
            LodestoneTracker target = new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), blockPos)), true);
            if (replaceExistingStack) {
                itemStack.set(DataComponents.LODESTONE_TRACKER, target);
            } else {
                ItemStack lodestoneCompass = itemStack.transmuteCopy(Items.COMPASS, 1);
                itemStack.consume(1, player);
                lodestoneCompass.set(DataComponents.LODESTONE_TRACKER, target);
                if (!player.getInventory().add(lodestoneCompass)) {
                    player.drop(lodestoneCompass, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    public Component getName(ItemStack itemStack) {
        return itemStack.has(DataComponents.LODESTONE_TRACKER) ? LODESTONE_COMPASS_NAME : super.getName(itemStack);
    }
}

