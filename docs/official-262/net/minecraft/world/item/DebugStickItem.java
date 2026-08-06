/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class DebugStickItem
extends Item {
    public DebugStickItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean canDestroyBlock(ItemStack itemStack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        if (user instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)user;
            this.handleInteraction(player, state, level, pos, false, itemStack);
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos;
        ServerPlayer serverPlayer;
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player instanceof ServerPlayer && !this.handleInteraction(serverPlayer = (ServerPlayer)player, level.getBlockState(pos = context.getClickedPos()), level, pos, true, context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    private boolean handleInteraction(ServerPlayer player, BlockState state, LevelAccessor level, BlockPos pos, boolean cycle, ItemStack itemStackInHand) {
        if (!player.canUseGameMasterBlocks()) {
            return false;
        }
        Holder<Block> block = state.typeHolder();
        StateDefinition<Block, BlockState> definition = block.value().getStateDefinition();
        Collection<Property<?>> properties = definition.getProperties();
        if (properties.isEmpty()) {
            DebugStickItem.message(player, Component.translatable(this.descriptionId + ".empty", block.getRegisteredName()));
            return false;
        }
        DebugStickState debugStickState = itemStackInHand.get(DataComponents.DEBUG_STICK_STATE);
        if (debugStickState == null) {
            return false;
        }
        Property<?> property = debugStickState.properties().get(block);
        if (cycle) {
            if (property == null) {
                property = properties.iterator().next();
            }
            BlockState newState = DebugStickItem.cycleState(state, property, player.isSecondaryUseActive());
            level.setBlock(pos, newState, 18);
            DebugStickItem.message(player, Component.translatable(this.descriptionId + ".update", property.getName(), DebugStickItem.getNameHelper(newState, property)));
        } else {
            property = DebugStickItem.getRelative(properties, property, player.isSecondaryUseActive());
            itemStackInHand.set(DataComponents.DEBUG_STICK_STATE, debugStickState.withProperty(block, property));
            DebugStickItem.message(player, Component.translatable(this.descriptionId + ".select", property.getName(), DebugStickItem.getNameHelper(state, property)));
        }
        return true;
    }

    private static <T extends Comparable<T>> BlockState cycleState(BlockState state, Property<T> property, boolean backward) {
        return (BlockState)state.setValue(property, (Comparable)DebugStickItem.getRelative(property.getPossibleValues(), state.getValue(property), backward));
    }

    private static <T> T getRelative(Iterable<T> collection, @Nullable T current, boolean backward) {
        return backward ? Util.findPreviousInIterable(collection, current) : Util.findNextInIterable(collection, current);
    }

    private static void message(ServerPlayer player, Component message) {
        player.sendOverlayMessage(message);
    }

    private static <T extends Comparable<T>> String getNameHelper(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}

