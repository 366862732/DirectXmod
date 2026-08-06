/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public class FlintAndSteelItem
extends Item {
    public FlintAndSteelItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos;
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockState state = level.getBlockState(pos = context.getClickedPos());
        if (CampfireBlock.canLight(state) || CandleBlock.canLight(state) || CandleCakeBlock.canLight(state)) {
            level.playSound((Entity)player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.4f + 0.8f);
            level.setBlock(pos, (BlockState)state.setValue(BlockStateProperties.LIT, true), 11);
            level.gameEvent((Entity)player, GameEvent.BLOCK_CHANGE, pos);
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, (LivingEntity)player, context.getHand().asEquipmentSlot());
            }
            return InteractionResult.SUCCESS;
        }
        BlockPos relativePos = pos.relative(context.getClickedFace());
        if (BaseFireBlock.canBePlacedAt(level, relativePos, context.getHorizontalDirection())) {
            level.playSound((Entity)player, relativePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.4f + 0.8f);
            BlockState fireState = BaseFireBlock.getState(level, relativePos);
            level.setBlock(relativePos, fireState, 11);
            level.gameEvent((Entity)player, GameEvent.BLOCK_PLACE, pos);
            ItemStack itemStack = context.getItemInHand();
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, relativePos, itemStack);
                itemStack.hurtAndBreak(1, (LivingEntity)player, context.getHand().asEquipmentSlot());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}

