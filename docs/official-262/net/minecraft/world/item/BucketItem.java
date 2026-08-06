/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public class BucketItem
extends Item
implements DispensibleContainerItem {
    protected final Fluid content;

    public BucketItem(Fluid content, Item.Properties properties) {
        super(properties);
        this.content = content;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        BlockHitResult hitResult = BucketItem.getPlayerPOVHitResult(level, player, this.getFluidContext());
        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        }
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BucketPickup bucketPickupBlock;
            ItemStack taken;
            BlockState blockState;
            Block block;
            BlockPos placePos;
            BlockPos pos = hitResult.getBlockPos();
            Direction direction = hitResult.getDirection();
            BlockPos directionOffsetPos = pos.relative(direction);
            if (!level.mayInteract(player, pos) || !player.mayUseItemAt(directionOffsetPos, direction, itemStack)) {
                return InteractionResult.FAIL;
            }
            BlockState clicked = level.getBlockState(pos);
            BlockPos blockPos = placePos = clicked.getBlock() instanceof LiquidBlockContainer && this.content == Fluids.WATER ? pos : directionOffsetPos;
            if (this.emptyContents(player, level, placePos, hitResult)) {
                this.checkExtraContent(player, level, itemStack, placePos);
                if (player instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer)player;
                    if (this.content != Fluids.EMPTY) {
                        CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, placePos, itemStack);
                    }
                }
                player.awardStat(Stats.ITEM_USED.get(this));
                ItemStack emptyResult = ItemUtils.createFilledResult(itemStack, player, BucketItem.getEmptySuccessItem(itemStack, player));
                return InteractionResult.SUCCESS.heldItemTransformedTo(emptyResult);
            }
            if (this.content == Fluids.EMPTY && (block = (blockState = level.getBlockState(pos)).getBlock()) instanceof BucketPickup && !(taken = (bucketPickupBlock = (BucketPickup)((Object)block)).pickupBlock(player, level, pos, blockState)).isEmpty()) {
                player.awardStat(Stats.ITEM_USED.get(this));
                bucketPickupBlock.getPickupSound().ifPresent(soundEvent -> player.playSound((SoundEvent)soundEvent, 1.0f, 1.0f));
                level.gameEvent((Entity)player, GameEvent.FLUID_PICKUP, pos);
                ItemStack result = ItemUtils.createFilledResult(itemStack, player, taken);
                if (!level.isClientSide()) {
                    CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)player, taken);
                }
                return InteractionResult.SUCCESS.heldItemTransformedTo(result);
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    public ClipContext.Fluid getFluidContext() {
        return this.content == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE;
    }

    public static ItemStack getEmptySuccessItem(ItemStack itemStack, Player player) {
        if (!player.hasInfiniteMaterials()) {
            return new ItemStack(Items.BUCKET);
        }
        return itemStack;
    }

    @Override
    public void checkExtraContent(@Nullable LivingEntity user, Level level, ItemStack itemStack, BlockPos pos) {
    }

    @Override
    public boolean emptyContents(@Nullable LivingEntity user, Level level, BlockPos pos, @Nullable BlockHitResult hitResult) {
        boolean canPlaceFluidInsideBlock;
        LiquidBlockContainer container;
        Fluid fluid = this.content;
        if (!(fluid instanceof FlowingFluid)) {
            return false;
        }
        FlowingFluid flowingFluid = (FlowingFluid)fluid;
        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();
        boolean mayReplace = blockState.canBeReplaced(this.content);
        boolean shiftKeyDown = user != null && user.isShiftKeyDown();
        boolean placeLiquid = mayReplace || block instanceof LiquidBlockContainer && (container = (LiquidBlockContainer)((Object)block)).canPlaceLiquid(user, level, pos, blockState, this.content);
        boolean bl = canPlaceFluidInsideBlock = blockState.isAir() || placeLiquid && (!shiftKeyDown || hitResult == null);
        if (!canPlaceFluidInsideBlock) {
            return hitResult != null && this.emptyContents(user, level, hitResult.getBlockPos().relative(hitResult.getDirection()), null);
        }
        if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos).booleanValue() && this.content.is(FluidTags.WATER)) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            RandomSource random = level.getRandom();
            level.playSound((Entity)user, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f + (random.nextFloat() - random.nextFloat()) * 0.8f);
            for (int i = 0; i < 8; ++i) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, (float)x + random.nextFloat(), (float)y + random.nextFloat(), (float)z + random.nextFloat(), 0.0, 0.0, 0.0);
            }
            return true;
        }
        if (block instanceof LiquidBlockContainer) {
            LiquidBlockContainer container2 = (LiquidBlockContainer)((Object)block);
            if (this.content == Fluids.WATER) {
                container2.placeLiquid(level, pos, blockState, flowingFluid.getSource(false));
                this.playEmptySound(user, level, pos);
                return true;
            }
        }
        if (!level.isClientSide() && mayReplace && !blockState.liquid()) {
            level.destroyBlock(pos, true);
        }
        if (level.setBlock(pos, this.content.defaultFluidState().createLegacyBlock(), 11) || blockState.getFluidState().isSource()) {
            this.playEmptySound(user, level, pos);
            return true;
        }
        return false;
    }

    protected void playEmptySound(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos) {
        SoundEvent soundEvent = this.content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        level.playSound(user, pos, soundEvent, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.gameEvent((Entity)user, GameEvent.FLUID_PLACE, pos);
    }

    public Fluid getContent() {
        return this.content;
    }
}

