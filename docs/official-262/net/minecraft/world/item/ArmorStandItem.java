/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ArmorStandItem
extends Item {
    public ArmorStandItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.DOWN) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos blockPos = placeContext.getClickedPos();
        ItemStack itemStack = context.getItemInHand();
        Vec3 pos = Vec3.atBottomCenterOf(blockPos);
        AABB box = EntityTypes.ARMOR_STAND.getDimensions().makeBoundingBox(pos.x(), pos.y(), pos.z());
        if (!level.noCollision(null, box) || !level.getEntities(null, box).isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            PostSpawnProcessor entityConfig = EntityType.createDefaultStackConfig(serverLevel, itemStack, context.getPlayer());
            ArmorStand entity = EntityTypes.ARMOR_STAND.create(serverLevel, entityConfig, blockPos, EntitySpawnReason.SPAWN_ITEM_USE, true, true);
            if (entity == null) {
                return InteractionResult.FAIL;
            }
            float yRot = (float)Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0f) + 22.5f) / 45.0f) * 45.0f;
            entity.snapTo(entity.getX(), entity.getY(), entity.getZ(), yRot, 0.0f);
            serverLevel.addFreshEntityWithPassengers(entity);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75f, 0.8f);
            entity.gameEvent(GameEvent.ENTITY_PLACE, context.getPlayer());
        }
        itemStack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}

