/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 */
package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ItemCommands {
    static final Dynamic3CommandExceptionType ERROR_TARGET_NOT_A_CONTAINER = new Dynamic3CommandExceptionType((x, y, z) -> Component.translatableEscape("commands.item.target.not_a_container", x, y, z));
    static final Dynamic3CommandExceptionType ERROR_SOURCE_NOT_A_CONTAINER = new Dynamic3CommandExceptionType((x, y, z) -> Component.translatableEscape("commands.item.source.not_a_container", x, y, z));
    static final DynamicCommandExceptionType ERROR_TARGET_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(slot -> Component.translatableEscape("commands.item.target.no_such_slot", slot));
    private static final DynamicCommandExceptionType ERROR_SOURCE_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(slot -> Component.translatableEscape("commands.item.source.no_such_slot", slot));
    private static final DynamicCommandExceptionType ERROR_TARGET_NO_CHANGES = new DynamicCommandExceptionType(slot -> Component.translatableEscape("commands.item.target.no_changes", slot));
    private static final Dynamic2CommandExceptionType ERROR_TARGET_NO_CHANGES_KNOWN_ITEM = new Dynamic2CommandExceptionType((item, slot) -> Component.translatableEscape("commands.item.target.no_changed.known_item", item, slot));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("item").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(((LiteralArgumentBuilder)Commands.literal("replace").then(Commands.literal("block").then(Commands.argument("pos", BlockPosArgument.blockPos()).then(((RequiredArgumentBuilder)Commands.argument("slot", SlotArgument.slot()).then(Commands.literal("with").then(((RequiredArgumentBuilder)Commands.argument("item", ItemArgument.item(context)).executes(c -> ItemCommands.setBlockItem((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), ItemArgument.getItem(c, "item").createItemStack(1)))).then(Commands.argument("count", IntegerArgumentType.integer((int)1, (int)99)).executes(c -> ItemCommands.setBlockItem((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), ItemArgument.getItem(c, "item").createItemStack(IntegerArgumentType.getInteger((CommandContext)c, (String)"count")))))))).then(((LiteralArgumentBuilder)Commands.literal("from").then(Commands.literal("block").then(Commands.argument("source", BlockPosArgument.blockPos()).then(((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot()).executes(c -> ItemCommands.blockToBlock((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "source"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "sourceSlot"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot")))).then(Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context)).executes(c -> ItemCommands.blockToBlock((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "source"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "sourceSlot"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), ResourceOrIdArgument.getLootModifier((CommandContext<CommandSourceStack>)c, "modifier")))))))).then(Commands.literal("entity").then(Commands.argument("source", EntityArgument.entity()).then(((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot()).executes(c -> ItemCommands.entityToBlock((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "source"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "sourceSlot"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot")))).then(Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context)).executes(c -> ItemCommands.entityToBlock((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "source"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "sourceSlot"), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), ResourceOrIdArgument.getLootModifier((CommandContext<CommandSourceStack>)c, "modifier")))))))))))).then(Commands.literal("entity").then(Commands.argument("targets", EntityArgument.entities()).then(((RequiredArgumentBuilder)Commands.argument("slot", SlotArgument.slot()).then(Commands.literal("with").then(((RequiredArgumentBuilder)Commands.argument("item", ItemArgument.item(context)).executes(c -> ItemCommands.setEntityItem((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), ItemArgument.getItem(c, "item").createItemStack(1)))).then(Commands.argument("count", IntegerArgumentType.integer((int)1, (int)99)).executes(c -> ItemCommands.setEntityItem((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), ItemArgument.getItem(c, "item").createItemStack(IntegerArgumentType.getInteger((CommandContext)c, (String)"count")))))))).then(((LiteralArgumentBuilder)Commands.literal("from").then(Commands.literal("block").then(Commands.argument("source", BlockPosArgument.blockPos()).then(((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot()).executes(c -> ItemCommands.blockToEntities((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "source"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "sourceSlot"), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot")))).then(Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context)).executes(c -> ItemCommands.blockToEntities((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "source"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "sourceSlot"), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), ResourceOrIdArgument.getLootModifier((CommandContext<CommandSourceStack>)c, "modifier")))))))).then(Commands.literal("entity").then(Commands.argument("source", EntityArgument.entity()).then(((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot()).executes(c -> ItemCommands.entityToEntities((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "source"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "sourceSlot"), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot")))).then(Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context)).executes(c -> ItemCommands.entityToEntities((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "source"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "sourceSlot"), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), ResourceOrIdArgument.getLootModifier((CommandContext<CommandSourceStack>)c, "modifier"))))))))))))).then(((LiteralArgumentBuilder)Commands.literal("modify").then(Commands.literal("block").then(Commands.argument("pos", BlockPosArgument.blockPos()).then(Commands.argument("slot", SlotArgument.slot()).then(Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context)).executes(c -> ItemCommands.modifyBlockItem((CommandSourceStack)c.getSource(), BlockPosArgument.getLoadedBlockPos((CommandContext<CommandSourceStack>)c, "pos"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), ResourceOrIdArgument.getLootModifier((CommandContext<CommandSourceStack>)c, "modifier")))))))).then(Commands.literal("entity").then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("slot", SlotArgument.slot()).then(Commands.argument("modifier", ResourceOrIdArgument.lootModifier(context)).executes(c -> ItemCommands.modifyEntityItem((CommandSourceStack)c.getSource(), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets"), SlotArgument.getSlot((CommandContext<CommandSourceStack>)c, "slot"), ResourceOrIdArgument.getLootModifier((CommandContext<CommandSourceStack>)c, "modifier")))))))));
    }

    private static int modifyBlockItem(CommandSourceStack source, BlockPos pos, int slot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
        Container container = ItemCommands.getContainer(source, pos, ERROR_TARGET_NOT_A_CONTAINER);
        if (slot < 0 || slot >= container.getContainerSize()) {
            throw ERROR_TARGET_INAPPLICABLE_SLOT.create((Object)slot);
        }
        ItemStack itemStack = ItemCommands.applyModifier(source, modifier, container.getItem(slot));
        container.setItem(slot, itemStack);
        source.sendSuccess(() -> Component.translatable("commands.item.block.set.success", pos.getX(), pos.getY(), pos.getZ(), itemStack.getDisplayName()), true);
        return 1;
    }

    private static int modifyEntityItem(CommandSourceStack source, Collection<? extends Entity> entities, int slot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
        HashMap changedEntities = Maps.newHashMapWithExpectedSize((int)entities.size());
        for (Entity entity : entities) {
            ItemStack itemStack;
            SlotAccess slotAccess = entity.getSlot(slot);
            if (slotAccess == null || !slotAccess.set(itemStack = ItemCommands.applyModifier(source, modifier, slotAccess.get().copy()))) continue;
            changedEntities.put(entity, itemStack);
            if (!(entity instanceof ServerPlayer)) continue;
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            serverPlayer.containerMenu.broadcastChanges();
        }
        if (changedEntities.isEmpty()) {
            throw ERROR_TARGET_NO_CHANGES.create((Object)slot);
        }
        if (changedEntities.size() == 1) {
            Map.Entry e = changedEntities.entrySet().iterator().next();
            source.sendSuccess(() -> Component.translatable("commands.item.entity.set.success.single", ((Entity)e.getKey()).getDisplayName(), ((ItemStack)e.getValue()).getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.item.entity.set.success.multiple", changedEntities.size()), true);
        }
        return changedEntities.size();
    }

    private static int setBlockItem(CommandSourceStack source, BlockPos pos, int slot, ItemStack itemStack) throws CommandSyntaxException {
        Container container = ItemCommands.getContainer(source, pos, ERROR_TARGET_NOT_A_CONTAINER);
        if (slot < 0 || slot >= container.getContainerSize()) {
            throw ERROR_TARGET_INAPPLICABLE_SLOT.create((Object)slot);
        }
        container.setItem(slot, itemStack);
        source.sendSuccess(() -> Component.translatable("commands.item.block.set.success", pos.getX(), pos.getY(), pos.getZ(), itemStack.getDisplayName()), true);
        return 1;
    }

    static Container getContainer(CommandSourceStack source, BlockPos pos, Dynamic3CommandExceptionType exceptionType) throws CommandSyntaxException {
        BlockEntity entity = source.getLevel().getBlockEntity(pos);
        if (entity instanceof Container) {
            Container container = (Container)((Object)entity);
            return container;
        }
        throw exceptionType.create((Object)pos.getX(), (Object)pos.getY(), (Object)pos.getZ());
    }

    private static int setEntityItem(CommandSourceStack source, Collection<? extends Entity> entities, int slot, ItemStack itemStack) throws CommandSyntaxException {
        ArrayList changedEntities = Lists.newArrayListWithCapacity((int)entities.size());
        for (Entity entity : entities) {
            SlotAccess slotAccess = entity.getSlot(slot);
            if (slotAccess == null || !slotAccess.set(itemStack.copy())) continue;
            changedEntities.add(entity);
            if (!(entity instanceof ServerPlayer)) continue;
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            serverPlayer.containerMenu.broadcastChanges();
        }
        if (changedEntities.isEmpty()) {
            throw ERROR_TARGET_NO_CHANGES_KNOWN_ITEM.create((Object)itemStack.getDisplayName(), (Object)slot);
        }
        if (changedEntities.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.item.entity.set.success.single", ((Entity)changedEntities.getFirst()).getDisplayName(), itemStack.getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.item.entity.set.success.multiple", changedEntities.size(), itemStack.getDisplayName()), true);
        }
        return changedEntities.size();
    }

    private static int blockToEntities(CommandSourceStack source, BlockPos sourcePos, int sourceSlot, Collection<? extends Entity> targetEntities, int targetSlot) throws CommandSyntaxException {
        return ItemCommands.setEntityItem(source, targetEntities, targetSlot, ItemCommands.getBlockItem(source, sourcePos, sourceSlot));
    }

    private static int blockToEntities(CommandSourceStack source, BlockPos sourcePos, int sourceSlot, Collection<? extends Entity> targetEntities, int targetSlot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
        return ItemCommands.setEntityItem(source, targetEntities, targetSlot, ItemCommands.applyModifier(source, modifier, ItemCommands.getBlockItem(source, sourcePos, sourceSlot)));
    }

    private static int blockToBlock(CommandSourceStack source, BlockPos sourcePos, int sourceSlot, BlockPos targetPos, int targetSlot) throws CommandSyntaxException {
        return ItemCommands.setBlockItem(source, targetPos, targetSlot, ItemCommands.getBlockItem(source, sourcePos, sourceSlot));
    }

    private static int blockToBlock(CommandSourceStack source, BlockPos sourcePos, int sourceSlot, BlockPos targetPos, int targetSlot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
        return ItemCommands.setBlockItem(source, targetPos, targetSlot, ItemCommands.applyModifier(source, modifier, ItemCommands.getBlockItem(source, sourcePos, sourceSlot)));
    }

    private static int entityToBlock(CommandSourceStack source, Entity sourceEntity, int sourceSlot, BlockPos targetPos, int targetSlot) throws CommandSyntaxException {
        return ItemCommands.setBlockItem(source, targetPos, targetSlot, ItemCommands.getItemInSlot(sourceEntity, sourceSlot));
    }

    private static int entityToBlock(CommandSourceStack source, Entity sourceEntity, int sourceSlot, BlockPos targetPos, int targetSlot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
        return ItemCommands.setBlockItem(source, targetPos, targetSlot, ItemCommands.applyModifier(source, modifier, ItemCommands.getItemInSlot(sourceEntity, sourceSlot)));
    }

    private static int entityToEntities(CommandSourceStack source, Entity sourceEntity, int sourceSlot, Collection<? extends Entity> targetEntities, int targetSlot) throws CommandSyntaxException {
        return ItemCommands.setEntityItem(source, targetEntities, targetSlot, ItemCommands.getItemInSlot(sourceEntity, sourceSlot));
    }

    private static int entityToEntities(CommandSourceStack source, Entity sourceEntity, int sourceSlot, Collection<? extends Entity> targetEntities, int targetSlot, Holder<LootItemFunction> modifier) throws CommandSyntaxException {
        return ItemCommands.setEntityItem(source, targetEntities, targetSlot, ItemCommands.applyModifier(source, modifier, ItemCommands.getItemInSlot(sourceEntity, sourceSlot)));
    }

    private static ItemStack applyModifier(CommandSourceStack source, Holder<LootItemFunction> modifier, ItemStack item) {
        ServerLevel level = source.getLevel();
        LootParams lootParams = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, source.getPosition()).withOptionalParameter(LootContextParams.THIS_ENTITY, source.getEntity()).create(LootContextParamSets.COMMAND);
        LootContext context = new LootContext.Builder(lootParams).create(Optional.empty());
        context.pushVisitedElement(LootContext.createVisitedEntry(modifier.value()));
        ItemStack newItem = (ItemStack)modifier.value().apply(item, context);
        newItem.limitSize(newItem.getMaxStackSize());
        return newItem;
    }

    private static ItemStack getItemInSlot(SlotProvider slotProvider, int slot) throws CommandSyntaxException {
        SlotAccess slotAccess = slotProvider.getSlot(slot);
        if (slotAccess == null) {
            throw ERROR_SOURCE_INAPPLICABLE_SLOT.create((Object)slot);
        }
        return slotAccess.get().copy();
    }

    private static ItemStack getBlockItem(CommandSourceStack source, BlockPos pos, int slot) throws CommandSyntaxException {
        Container container = ItemCommands.getContainer(source, pos, ERROR_SOURCE_NOT_A_CONTAINER);
        return ItemCommands.getItemInSlot(container, slot);
    }
}

