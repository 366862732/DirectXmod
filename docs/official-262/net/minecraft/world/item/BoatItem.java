/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BoatItem
extends Item {
    private final EntityType<? extends AbstractBoat> entityType;

    public BoatItem(EntityType<? extends AbstractBoat> entityType, Item.Properties properties) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        BlockHitResult hitResult = BoatItem.getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (((HitResult)hitResult).getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        }
        Vec3 viewVector = player.getViewVector(1.0f);
        double range = 5.0;
        List<Entity> entities = level.getEntities(player, player.getBoundingBox().expandTowards(viewVector.scale(5.0)).inflate(1.0), EntitySelector.CAN_BE_PICKED);
        if (!entities.isEmpty()) {
            Vec3 from = player.getEyePosition();
            for (Entity entity : entities) {
                AABB bb = entity.getBoundingBox().inflate(entity.getPickRadius());
                if (!bb.contains(from)) continue;
                return InteractionResult.PASS;
            }
        }
        if (((HitResult)hitResult).getType() == HitResult.Type.BLOCK) {
            AbstractBoat boat = this.getBoat(level, hitResult, itemStack, player);
            if (boat == null) {
                return InteractionResult.FAIL;
            }
            boat.setYRot(player.getYRot());
            if (!level.noCollision(boat, boat.getBoundingBox())) {
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide()) {
                level.addFreshEntity(boat);
                level.gameEvent((Entity)player, GameEvent.ENTITY_PLACE, hitResult.getLocation());
                itemStack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private @Nullable AbstractBoat getBoat(Level level, HitResult hitResult, ItemStack itemStack, Player player) {
        AbstractBoat boat = this.entityType.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (boat != null) {
            Vec3 location = hitResult.getLocation();
            boat.setInitialPos(location.x, location.y, location.z);
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                EntityType.createDefaultStackConfig(serverLevel, itemStack, player).apply(boat);
            }
        }
        return boat;
    }
}

