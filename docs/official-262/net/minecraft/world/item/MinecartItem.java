/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class MinecartItem
extends Item {
    private final EntityType<? extends AbstractMinecart> type;

    public MinecartItem(EntityType<? extends AbstractMinecart> type, Item.Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos;
        Level level = context.getLevel();
        BlockState blockState = level.getBlockState(pos = context.getClickedPos());
        if (!blockState.is(BlockTags.RAILS)) {
            return InteractionResult.FAIL;
        }
        ItemStack itemStack = context.getItemInHand();
        RailShape shape = blockState.getBlock() instanceof BaseRailBlock ? blockState.getValue(((BaseRailBlock)blockState.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
        double offset = 0.0;
        if (shape.isSlope()) {
            offset = 0.5;
        }
        Vec3 spawnPos = new Vec3((double)pos.getX() + 0.5, (double)pos.getY() + 0.0625 + offset, (double)pos.getZ() + 0.5);
        AbstractMinecart cart = AbstractMinecart.createMinecart(level, spawnPos.x, spawnPos.y, spawnPos.z, this.type, EntitySpawnReason.DISPENSER, itemStack, context.getPlayer());
        if (cart == null) {
            return InteractionResult.FAIL;
        }
        if (AbstractMinecart.useExperimentalMovement(level)) {
            List<Entity> entities = level.getEntities(null, cart.getBoundingBox());
            for (Entity entity : entities) {
                if (!(entity instanceof AbstractMinecart)) continue;
                return InteractionResult.FAIL;
            }
        }
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            serverLevel.addFreshEntity(cart);
            serverLevel.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(context.getPlayer(), serverLevel.getBlockState(pos.below())));
        }
        itemStack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}

