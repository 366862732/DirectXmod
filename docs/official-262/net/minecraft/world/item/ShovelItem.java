/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Maps
 */
package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class ShovelItem
extends Item {
    protected static final Map<Block, BlockState> FLATTENABLES = Maps.newHashMap((Map)new ImmutableMap.Builder().put((Object)Blocks.GRASS_BLOCK, (Object)Blocks.DIRT_PATH.defaultBlockState()).put((Object)Blocks.DIRT, (Object)Blocks.DIRT_PATH.defaultBlockState()).put((Object)Blocks.PODZOL, (Object)Blocks.DIRT_PATH.defaultBlockState()).put((Object)Blocks.COARSE_DIRT, (Object)Blocks.DIRT_PATH.defaultBlockState()).put((Object)Blocks.MYCELIUM, (Object)Blocks.DIRT_PATH.defaultBlockState()).put((Object)Blocks.ROOTED_DIRT, (Object)Blocks.DIRT_PATH.defaultBlockState()).build());

    public ShovelItem(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline, Item.Properties properties) {
        super(properties.shovel(material, attackDamageBaseline, attackSpeedBaseline));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState blockState = level.getBlockState(pos);
        if (context.getClickedFace() != Direction.DOWN) {
            Player player = context.getPlayer();
            BlockState newState = FLATTENABLES.get(blockState.getBlock());
            BlockState updatedState = null;
            if (newState != null && level.getBlockState(pos.above()).isAir()) {
                level.playSound((Entity)player, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                updatedState = newState;
            } else if (blockState.getBlock() instanceof CampfireBlock && blockState.getValue(CampfireBlock.LIT).booleanValue()) {
                if (!level.isClientSide()) {
                    level.levelEvent(null, 1009, pos, 0);
                }
                CampfireBlock.dowse(context.getPlayer(), level, pos, blockState);
                updatedState = (BlockState)blockState.setValue(CampfireBlock.LIT, false);
            }
            if (updatedState != null) {
                if (!level.isClientSide()) {
                    level.setBlock(pos, updatedState, 11);
                    level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, updatedState));
                    if (player != null) {
                        context.getItemInHand().hurtAndBreak(1, (LivingEntity)player, context.getHand().asEquipmentSlot());
                    }
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }
}

