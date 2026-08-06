/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 */
package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.ItemCommands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class LootCommand {
    private static final DynamicCommandExceptionType ERROR_NO_HELD_ITEMS = new DynamicCommandExceptionType(entity -> Component.translatableEscape("commands.drop.no_held_items", entity));
    private static final DynamicCommandExceptionType ERROR_NO_ENTITY_LOOT_TABLE = new DynamicCommandExceptionType(entity -> Component.translatableEscape("commands.drop.no_loot_table.entity", entity));
    private static final DynamicCommandExceptionType ERROR_NO_BLOCK_LOOT_TABLE = new DynamicCommandExceptionType(block -> Component.translatableEscape("commands.drop.no_loot_table.block", block));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(LootCommand.addTargets((LiteralArgumentBuilder)Commands.literal("loot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)), (target, output) -> target.then(Commands.literal("fish").then(Commands.argument("loot_table", ResourceOrIdArgument.lootTable(context)).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> LootCommand.dropFishingLoot((CommandContext<CommandSourceStack>)c, ResourceOrIdArgument.getLootTable((CommandContext<CommandSourceStack>)c, "loot_table"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), ItemStack.EMPTY, output))).then(Commands.argument("tool", ItemArgument.item(context)).executes(c -> LootCommand.dropFishingLoot((CommandContext<CommandSourceStack>)c, ResourceOrIdArgument.getLootTable((CommandContext<CommandSourceStack>)c, "loot_table"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), ItemArgument.getItem(c, "tool").createItemStack(1), output)))).then(Commands.literal("mainhand").executes(c -> LootCommand.dropFishingLoot((CommandContext<CommandSourceStack>)c, ResourceOrIdArgument.getLootTable((CommandContext<CommandSourceStack>)c, "loot_table"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), LootCommand.getSourceHandItem((CommandSourceStack)c.getSource(), EquipmentSlot.MAINHAND), output)))).then(Commands.literal("offhand").executes(c -> LootCommand.dropFishingLoot((CommandContext<CommandSourceStack>)c, ResourceOrIdArgument.getLootTable((CommandContext<CommandSourceStack>)c, "loot_table"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), LootCommand.getSourceHandItem((CommandSourceStack)c.getSource(), EquipmentSlot.OFFHAND), output)))))).then(Commands.literal("loot").then(Commands.argument("loot_table", ResourceOrIdArgument.lootTable(context)).executes(c -> LootCommand.dropChestLoot((CommandContext<CommandSourceStack>)c, ResourceOrIdArgument.getLootTable((CommandContext<CommandSourceStack>)c, "loot_table"), output)))).then(Commands.literal("kill").then(Commands.argument("target", EntityArgument.entity()).executes(c -> LootCommand.dropKillLoot((CommandContext<CommandSourceStack>)c, EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "target"), output)))).then(Commands.literal("mine").then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> LootCommand.dropBlockLoot((CommandContext<CommandSourceStack>)c, BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), ItemStack.EMPTY, output))).then(Commands.argument("tool", ItemArgument.item(context)).executes(c -> LootCommand.dropBlockLoot((CommandContext<CommandSourceStack>)c, BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), ItemArgument.getItem(c, "tool").createItemStack(1), output)))).then(Commands.literal("mainhand").executes(c -> LootCommand.dropBlockLoot((CommandContext<CommandSourceStack>)c, BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), LootCommand.getSourceHandItem((CommandSourceStack)c.getSource(), EquipmentSlot.MAINHAND), output)))).then(Commands.literal("offhand").executes(c -> LootCommand.dropBlockLoot((CommandContext<CommandSourceStack>)c, BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), LootCommand.getSourceHandItem((CommandSourceStack)c.getSource(), EquipmentSlot.OFFHAND), output)))))));
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addTargets(T root, TailProvider tail) {
        return (T)root.then(((LiteralArgumentBuilder)Commands.literal("replace").then(Commands.literal("entity").then(Commands.argument("entities", EntityArgument.entities()).then(tail.construct((ArgumentBuilder<CommandSourceStack, ?>)Commands.argument("slot", SlotArgument.slot()), (c, drops, callback) -> LootCommand.entityReplace(EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "entities"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), drops.size(), drops, callback)).then(tail.construct((ArgumentBuilder<CommandSourceStack, ?>)Commands.argument("count", IntegerArgumentType.integer((int)0)), (c, drops, callback) -> LootCommand.entityReplace(EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "entities"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), IntegerArgumentType.getInteger((CommandContext)c, (String)"count"), drops, callback))))))).then(Commands.literal("block").then(Commands.argument("targetPos", BlockPosArgument.blockPos()).then(tail.construct((ArgumentBuilder<CommandSourceStack, ?>)Commands.argument("slot", SlotArgument.slot()), (c, drops, callback) -> LootCommand.blockReplace((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "targetPos"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), drops.size(), drops, callback)).then(tail.construct((ArgumentBuilder<CommandSourceStack, ?>)Commands.argument("count", IntegerArgumentType.integer((int)0)), (c, drops, callback) -> LootCommand.blockReplace((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "targetPos"), IntegerArgumentType.getInteger((CommandContext)c, (String)"slot"), IntegerArgumentType.getInteger((CommandContext)c, (String)"count"), drops, callback))))))).then(Commands.literal("insert").then(tail.construct((ArgumentBuilder<CommandSourceStack, ?>)Commands.argument("targetPos", BlockPosArgument.blockPos()), (c, drops, callback) -> LootCommand.blockDistribute((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "targetPos"), drops, callback)))).then(Commands.literal("give").then(tail.construct((ArgumentBuilder<CommandSourceStack, ?>)Commands.argument("players", EntityArgument.players()), (c, drops, callback) -> LootCommand.playerGive(EntityArgument.getPlayers((CommandContext<CommandSourceStack>)c, "players"), drops, callback)))).then(Commands.literal("spawn").then(tail.construct((ArgumentBuilder<CommandSourceStack, ?>)Commands.argument("targetPos", Vec3Argument.vec3()), (c, drops, callback) -> LootCommand.dropInWorld((CommandSourceStack)c.getSource(), Vec3Argument.getVec3((CommandContext<CommandSourceStack>)c, "targetPos"), drops, callback))));
    }

    private static Container getContainer(CommandSourceStack source, BlockPos pos) throws CommandSyntaxException {
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof Container)) {
            throw ItemCommands.ERROR_TARGET_NOT_A_CONTAINER.create((Object)pos.getX(), (Object)pos.getY(), (Object)pos.getZ());
        }
        Container container = (Container)((Object)blockEntity);
        return container;
    }

    private static int blockDistribute(CommandSourceStack source, BlockPos pos, List<ItemStack> drops, Callback callback) throws CommandSyntaxException {
        Container container = LootCommand.getContainer(source, pos);
        ArrayList usedItems = Lists.newArrayListWithCapacity((int)drops.size());
        for (ItemStack drop : drops) {
            if (!LootCommand.distributeToContainer(container, drop.copy())) continue;
            container.setChanged();
            usedItems.add(drop);
        }
        callback.accept(usedItems);
        return usedItems.size();
    }

    private static boolean distributeToContainer(Container container, ItemStack itemStack) {
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize() && !itemStack.isEmpty(); ++slot) {
            ItemStack current = container.getItem(slot);
            if (!container.canPlaceItem(slot, itemStack)) continue;
            if (current.isEmpty()) {
                container.setItem(slot, itemStack);
                changed = true;
                break;
            }
            if (!LootCommand.canMergeItems(current, itemStack)) continue;
            int space = itemStack.getMaxStackSize() - current.getCount();
            int count = Math.min(itemStack.getCount(), space);
            itemStack.shrink(count);
            current.grow(count);
            changed = true;
        }
        return changed;
    }

    private static int blockReplace(CommandSourceStack source, BlockPos pos, int startSlot, int slotCount, List<ItemStack> drops, Callback callback) throws CommandSyntaxException {
        Container container = LootCommand.getContainer(source, pos);
        int maxSlot = container.getContainerSize();
        if (startSlot < 0 || startSlot >= maxSlot) {
            throw ItemCommands.ERROR_TARGET_INAPPLICABLE_SLOT.create((Object)startSlot);
        }
        ArrayList usedItems = Lists.newArrayListWithCapacity((int)drops.size());
        for (int i = 0; i < slotCount; ++i) {
            ItemStack toAdd;
            int slot = startSlot + i;
            ItemStack itemStack = toAdd = i < drops.size() ? drops.get(i) : ItemStack.EMPTY;
            if (!container.canPlaceItem(slot, toAdd)) continue;
            container.setItem(slot, toAdd);
            usedItems.add(toAdd);
        }
        callback.accept(usedItems);
        return usedItems.size();
    }

    private static boolean canMergeItems(ItemStack a, ItemStack b) {
        return a.getCount() <= a.getMaxStackSize() && ItemStack.isSameItemSameComponents(a, b);
    }

    private static int playerGive(Collection<ServerPlayer> players, List<ItemStack> drops, Callback callback) throws CommandSyntaxException {
        ArrayList usedItems = Lists.newArrayListWithCapacity((int)drops.size());
        for (ItemStack drop : drops) {
            for (ServerPlayer player : players) {
                if (!player.getInventory().add(drop.copy())) continue;
                usedItems.add(drop);
            }
        }
        callback.accept(usedItems);
        return usedItems.size();
    }

    private static void setSlots(Entity entity, List<ItemStack> itemsToSet, int startSlot, int count, List<ItemStack> usedItems) {
        for (int i = 0; i < count; ++i) {
            ItemStack item = i < itemsToSet.size() ? itemsToSet.get(i) : ItemStack.EMPTY;
            SlotAccess slotAccess = entity.getSlot(startSlot + i);
            if (slotAccess == null || !slotAccess.set(item.copy())) continue;
            usedItems.add(item);
        }
    }

    private static int entityReplace(Collection<? extends Entity> entities, int startSlot, int count, List<ItemStack> drops, Callback callback) throws CommandSyntaxException {
        ArrayList usedItems = Lists.newArrayListWithCapacity((int)drops.size());
        for (Entity entity : entities) {
            if (entity instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)entity;
                LootCommand.setSlots(entity, drops, startSlot, count, usedItems);
                player.containerMenu.broadcastChanges();
                continue;
            }
            LootCommand.setSlots(entity, drops, startSlot, count, usedItems);
        }
        callback.accept(usedItems);
        return usedItems.size();
    }

    private static int dropInWorld(CommandSourceStack source, Vec3 pos, List<ItemStack> drops, Callback callback) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        drops.forEach(drop -> {
            ItemEntity entity = new ItemEntity(level, pos.x, pos.y, pos.z, drop.copy());
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        });
        callback.accept(drops);
        return drops.size();
    }

    private static void callback(CommandSourceStack source, List<ItemStack> drops) {
        if (drops.size() == 1) {
            ItemStack drop = drops.get(0);
            source.sendSuccess(() -> Component.translatable("commands.drop.success.single", drop.getCount(), drop.getDisplayName()), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.drop.success.multiple", drops.size()), false);
        }
    }

    private static void callback(CommandSourceStack source, List<ItemStack> drops, ResourceKey<LootTable> location) {
        if (drops.size() == 1) {
            ItemStack drop = drops.get(0);
            source.sendSuccess(() -> Component.translatable("commands.drop.success.single_with_table", drop.getCount(), drop.getDisplayName(), Component.translationArg(location.identifier())), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.drop.success.multiple_with_table", drops.size(), Component.translationArg(location.identifier())), false);
        }
    }

    private static ItemStack getSourceHandItem(CommandSourceStack source, EquipmentSlot slot) throws CommandSyntaxException {
        Entity entity = source.getEntityOrException();
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            return livingEntity.getItemBySlot(slot);
        }
        throw ERROR_NO_HELD_ITEMS.create((Object)entity.getDisplayName());
    }

    private static int dropBlockLoot(CommandContext<CommandSourceStack> context, BlockPos pos, ItemInstance tool, DropConsumer output) throws CommandSyntaxException {
        CommandSourceStack source = (CommandSourceStack)context.getSource();
        ServerLevel level = source.getLevel();
        BlockState blockState = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        Optional<ResourceKey<LootTable>> lootTable = blockState.getBlock().getLootTable();
        if (lootTable.isEmpty()) {
            throw ERROR_NO_BLOCK_LOOT_TABLE.create((Object)blockState.getBlock().getName());
        }
        LootParams.Builder lootParams = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.BLOCK_STATE, blockState).withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity).withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity()).withParameter(LootContextParams.TOOL, tool);
        List<ItemStack> drops = blockState.getDrops(lootParams);
        return output.accept(context, drops, usedItems -> LootCommand.callback(source, usedItems, (ResourceKey)lootTable.get()));
    }

    private static int dropKillLoot(CommandContext<CommandSourceStack> context, Entity target, DropConsumer output) throws CommandSyntaxException {
        Optional<ResourceKey<LootTable>> lootTableId = target.getLootTable();
        if (lootTableId.isEmpty()) {
            throw ERROR_NO_ENTITY_LOOT_TABLE.create((Object)target.getDisplayName());
        }
        CommandSourceStack source = (CommandSourceStack)context.getSource();
        LootParams.Builder builder = new LootParams.Builder(source.getLevel());
        Entity killer = source.getEntity();
        if (killer instanceof Player) {
            Player player = (Player)killer;
            builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
        }
        builder.withParameter(LootContextParams.DAMAGE_SOURCE, target.damageSources().magic());
        builder.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer);
        builder.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer);
        builder.withParameter(LootContextParams.THIS_ENTITY, target);
        builder.withParameter(LootContextParams.ORIGIN, source.getPosition());
        LootParams lootParams = builder.create(LootContextParamSets.ENTITY);
        LootTable lootTable = source.getServer().reloadableRegistries().getLootTable(lootTableId.get());
        ObjectArrayList<ItemStack> drops = lootTable.getRandomItems(lootParams);
        return output.accept(context, (List<ItemStack>)drops, usedItems -> LootCommand.callback(source, usedItems, (ResourceKey)lootTableId.get()));
    }

    private static int dropChestLoot(CommandContext<CommandSourceStack> context, Holder<LootTable> lootTable, DropConsumer output) throws CommandSyntaxException {
        CommandSourceStack source = (CommandSourceStack)context.getSource();
        LootParams lootParams = new LootParams.Builder(source.getLevel()).withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity()).withParameter(LootContextParams.ORIGIN, source.getPosition()).create(LootContextParamSets.CHEST);
        return LootCommand.drop(context, lootTable, lootParams, output);
    }

    private static int dropFishingLoot(CommandContext<CommandSourceStack> context, Holder<LootTable> lootTable, BlockPos pos, ItemInstance tool, DropConsumer output) throws CommandSyntaxException {
        CommandSourceStack source = (CommandSourceStack)context.getSource();
        LootParams lootParams = new LootParams.Builder(source.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL, tool).withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity()).create(LootContextParamSets.FISHING);
        return LootCommand.drop(context, lootTable, lootParams, output);
    }

    private static int drop(CommandContext<CommandSourceStack> context, Holder<LootTable> lootTable, LootParams lootParams, DropConsumer output) throws CommandSyntaxException {
        CommandSourceStack source = (CommandSourceStack)context.getSource();
        ObjectArrayList<ItemStack> drops = lootTable.value().getRandomItems(lootParams);
        return output.accept(context, (List<ItemStack>)drops, usedItems -> LootCommand.callback(source, usedItems));
    }

    @FunctionalInterface
    private static interface TailProvider {
        public ArgumentBuilder<CommandSourceStack, ?> construct(ArgumentBuilder<CommandSourceStack, ?> var1, DropConsumer var2);
    }

    @FunctionalInterface
    private static interface DropConsumer {
        public int accept(CommandContext<CommandSourceStack> var1, List<ItemStack> var2, Callback var3) throws CommandSyntaxException;
    }

    @FunctionalInterface
    private static interface Callback {
        public void accept(List<ItemStack> var1) throws CommandSyntaxException;
    }
}

