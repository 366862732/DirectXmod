/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.google.common.collect.BiMap
 *  com.google.common.collect.ImmutableBiMap
 *  com.google.common.collect.ImmutableBiMap$Builder
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.item;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;

public class HoneycombItem
extends Item
implements SignApplicator {
    public static final Supplier<BiMap<Block, Block>> WAXABLES = Suppliers.memoize(() -> {
        ImmutableBiMap.Builder builder = ImmutableBiMap.builder();
        Stream.of(Blocks.COPPER_BLOCK, Blocks.CUT_COPPER, Blocks.CUT_COPPER_SLAB, Blocks.CUT_COPPER_STAIRS, Blocks.CHISELED_COPPER, Blocks.COPPER_DOOR, Blocks.COPPER_TRAPDOOR, Blocks.COPPER_BARS, Blocks.COPPER_GRATE, Blocks.COPPER_BULB, Blocks.COPPER_CHEST, Blocks.COPPER_GOLEM_STATUE, Blocks.LIGHTNING_ROD, Blocks.COPPER_LANTERN, Blocks.COPPER_CHAIN).forEach(collection -> collection.zipUnwaxedWaxed((arg_0, arg_1) -> ((ImmutableBiMap.Builder)builder).put(arg_0, arg_1)));
        return builder.build();
    });
    public static final Supplier<BiMap<Block, Block>> WAX_OFF_BY_BLOCK = Suppliers.memoize(() -> WAXABLES.get().inverse());
    public static final ImmutableMap<Block, Pair<RecipeCategory, String>> WAXED_RECIPES;

    public HoneycombItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState oldState = level.getBlockState(pos);
        return HoneycombItem.getWaxed(oldState).map(waxedState -> {
            Player player = context.getPlayer();
            ItemStack itemInHand = context.getItemInHand();
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, itemInHand);
            }
            itemInHand.shrink(1);
            level.setBlock(pos, (BlockState)waxedState, 11);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, waxedState));
            level.levelEvent(player, 3003, pos, 0);
            if (oldState.getBlock() instanceof ChestBlock && oldState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos neighborPos = ChestBlock.getConnectedBlockPos(pos, oldState);
                level.gameEvent(GameEvent.BLOCK_CHANGE, neighborPos, GameEvent.Context.of(player, level.getBlockState(neighborPos)));
                level.levelEvent(player, 3003, neighborPos, 0);
            }
            return InteractionResult.SUCCESS;
        }).orElse(InteractionResult.PASS);
    }

    public static Optional<BlockState> getWaxed(BlockState oldState) {
        return Optional.ofNullable((Block)WAXABLES.get().get((Object)oldState.getBlock())).map(b -> b.withPropertiesOf(oldState));
    }

    @Override
    public boolean tryApplyToSign(Level level, SignBlockEntity sign, boolean isFrontText, ItemStack item, Player player) {
        if (sign.setWaxed(true)) {
            level.levelEvent(null, 3003, sign.getBlockPos(), 0);
            return true;
        }
        return false;
    }

    @Override
    public boolean canApplyToSign(SignText text, ItemStack item, Player player) {
        return true;
    }

    static {
        ImmutableMap.Builder builder = ImmutableMap.builder();
        for (WaxedRecipeGroup data : List.of(new WaxedRecipeGroup(Blocks.COPPER_BULB, block -> Pair.of((Object)((Object)RecipeCategory.REDSTONE), (Object)block.builtInRegistryHolder().key().identifier().getPath())), new WaxedRecipeGroup(Blocks.COPPER_DOOR, block -> Pair.of((Object)((Object)RecipeCategory.REDSTONE), (Object)"waxed_copper_door")), new WaxedRecipeGroup(Blocks.COPPER_TRAPDOOR, block -> Pair.of((Object)((Object)RecipeCategory.REDSTONE), (Object)"waxed_copper_trapdoor")), new WaxedRecipeGroup(Blocks.COPPER_GOLEM_STATUE, block -> Pair.of((Object)((Object)RecipeCategory.BUILDING_BLOCKS), (Object)"waxed_copper_golem_statue")), new WaxedRecipeGroup(Blocks.COPPER_CHEST, block -> Pair.of((Object)((Object)RecipeCategory.BUILDING_BLOCKS), (Object)"waxed_copper_chest")), new WaxedRecipeGroup(Blocks.LIGHTNING_ROD, block -> Pair.of((Object)((Object)RecipeCategory.BUILDING_BLOCKS), (Object)"waxed_lightning_rod")), new WaxedRecipeGroup(Blocks.COPPER_BARS, block -> Pair.of((Object)((Object)RecipeCategory.BUILDING_BLOCKS), (Object)"waxed_copper_bar")), new WaxedRecipeGroup(Blocks.COPPER_CHAIN, block -> Pair.of((Object)((Object)RecipeCategory.BUILDING_BLOCKS), (Object)"waxed_copper_chain")), new WaxedRecipeGroup(Blocks.COPPER_LANTERN, block -> Pair.of((Object)((Object)RecipeCategory.BUILDING_BLOCKS), (Object)"waxed_copper_lantern")), new WaxedRecipeGroup(Blocks.COPPER_BLOCK, block -> Pair.of((Object)((Object)RecipeCategory.BUILDING_BLOCKS), (Object)"waxed_copper_block")))) {
            data.block.waxed().forEach(block -> builder.put(block, data.recipeIdProvider.apply((Block)block)));
        }
        WAXED_RECIPES = builder.build();
    }

    private record WaxedRecipeGroup(WeatheringCopperCollection<Block> block, Function<Block, Pair<RecipeCategory, String>> recipeIdProvider) {
    }
}

