/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpawnEggItem
extends Item {
    public SpawnEggItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack itemStack = context.getItemInHand();
        EntityType<?> type = SpawnEggItem.getType(itemStack);
        if (type == null || !type.canSpawn(level)) {
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockState blockState = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Spawner) {
            Spawner spawnerHolder = (Spawner)((Object)blockEntity);
            if (!serverLevel.isSpawnerBlockEnabled()) {
                Player player = context.getPlayer();
                if (player instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer)player;
                    serverPlayer.sendSystemMessage(Component.translatable("advMode.notEnabled.spawner"));
                }
                return InteractionResult.FAIL;
            }
            spawnerHolder.setEntityId(type, level.getRandom());
            level.sendBlockUpdated(pos, blockState, blockState, 3);
            level.gameEvent((Entity)context.getPlayer(), GameEvent.BLOCK_CHANGE, pos);
            itemStack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        BlockPos spawnPos = blockState.getCollisionShape(level, pos).isEmpty() ? pos : pos.relative(clickedFace);
        return SpawnEggItem.spawnMob(type, context.getPlayer(), itemStack, serverLevel, spawnPos, true, !Objects.equals(pos, spawnPos) && clickedFace == Direction.UP);
    }

    private static InteractionResult spawnMob(EntityType<?> type, @Nullable LivingEntity user, ItemStack itemStack, ServerLevel level, BlockPos spawnPos, boolean tryMoveDown, boolean movedUp) {
        if (type.spawn(level, itemStack, user, spawnPos, EntitySpawnReason.SPAWN_ITEM_USE, tryMoveDown, movedUp) != null) {
            itemStack.consume(1, user);
            level.gameEvent((Entity)user, GameEvent.ENTITY_PLACE, spawnPos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        BlockHitResult hitResult = SpawnEggItem.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        EntityType<?> type = SpawnEggItem.getType(itemStack);
        if (type == null || !type.canSpawn(level)) {
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        BlockPos pos = hitResult.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof LiquidBlock)) {
            return InteractionResult.PASS;
        }
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hitResult.getDirection(), itemStack)) {
            return InteractionResult.FAIL;
        }
        InteractionResult result = SpawnEggItem.spawnMob(type, player, itemStack, serverLevel, pos, false, false);
        if (result == InteractionResult.SUCCESS) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return result;
    }

    public static boolean spawnsEntity(ItemStack itemStack, EntityType<?> type) {
        return Objects.equals(SpawnEggItem.getType(itemStack), type);
    }

    public static Optional<Holder<Item>> byId(EntityType<?> type) {
        return BuiltInRegistries.ITEM.componentLookup().findMatching(DataComponents.ENTITY_DATA, c -> c.type() == type).findAny();
    }

    public static @Nullable EntityType<?> getType(ItemStack itemStack) {
        TypedEntityData<EntityType<?>> entityData = itemStack.get(DataComponents.ENTITY_DATA);
        if (entityData != null) {
            return entityData.type();
        }
        return null;
    }

    public static Optional<Mob> spawnOffspringFromSpawnEgg(Player player, Mob parent, EntityType<? extends Mob> type, ServerLevel level, Vec3 pos, ItemStack spawnEggStack) {
        Mob offspring;
        if (!SpawnEggItem.spawnsEntity(spawnEggStack, type)) {
            return Optional.empty();
        }
        if (parent instanceof AgeableMob) {
            AgeableMob ageableMob = (AgeableMob)parent;
            offspring = ageableMob.getBreedOffspring(level, ageableMob);
        } else {
            offspring = type.create((Level)level, EntitySpawnReason.SPAWN_ITEM_USE);
        }
        if (offspring == null) {
            return Optional.empty();
        }
        offspring.setBaby(true);
        if (!offspring.isBaby()) {
            return Optional.empty();
        }
        offspring.snapTo(pos.x(), pos.y(), pos.z(), 0.0f, 0.0f);
        offspring.applyComponentsFromItemStack(spawnEggStack);
        level.addFreshEntityWithPassengers(offspring);
        spawnEggStack.consume(1, player);
        return Optional.of(offspring);
    }

    @Override
    public boolean shouldPrintOpWarning(ItemStack stack, @Nullable Player player) {
        TypedEntityData<EntityType<?>> entityData;
        if (player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) && (entityData = stack.get(DataComponents.ENTITY_DATA)) != null) {
            return entityData.type().onlyOpCanSetNbt();
        }
        return false;
    }
}

