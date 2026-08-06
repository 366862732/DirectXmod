/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class DyeItem
extends Item
implements SignApplicator {
    public DyeItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack itemStack, Player player, LivingEntity target, InteractionHand type) {
        DyeColor dyeColor;
        Sheep sheep;
        if (target instanceof Sheep && (sheep = (Sheep)target).isAlive() && !sheep.isSheared() && (dyeColor = itemStack.get(DataComponents.DYE)) != null && sheep.getColor() != dyeColor) {
            sheep.level().playSound((Entity)player, sheep, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (!player.level().isClientSide()) {
                sheep.setColor(dyeColor);
                itemStack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean tryApplyToSign(Level level, SignBlockEntity sign, boolean isFrontText, ItemStack item, Player player) {
        DyeColor dye = item.get(DataComponents.DYE);
        if (dye != null && sign.updateText(text -> text.setColor(dye), isFrontText)) {
            level.playSound(null, sign.getBlockPos(), SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            return true;
        }
        return false;
    }
}

