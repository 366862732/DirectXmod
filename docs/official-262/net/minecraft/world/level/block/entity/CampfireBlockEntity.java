/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CampfireBlockEntity
extends BlockEntity
implements Clearable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int BURN_COOL_SPEED = 2;
    private static final int NUM_SLOTS = 4;
    private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private final int[] cookingProgress = new int[4];
    private final int[] cookingTime = new int[4];

    public CampfireBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.CAMPFIRE, worldPosition, blockState);
    }

    public static void cookTick(ServerLevel level, BlockPos pos, BlockState state, CampfireBlockEntity entity, RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipeCache) {
        boolean changed = false;
        for (int slot = 0; slot < entity.items.size(); ++slot) {
            SingleRecipeInput input;
            ItemStack result;
            ItemStack itemStack = entity.items.get(slot);
            if (itemStack.isEmpty()) continue;
            changed = true;
            int n = slot;
            entity.cookingProgress[n] = entity.cookingProgress[n] + 1;
            if (entity.cookingProgress[slot] < entity.cookingTime[slot] || !(result = recipeCache.getRecipeFor(input = new SingleRecipeInput(itemStack), level).map(r -> ((CampfireCookingRecipe)r.value()).assemble(input)).orElse(itemStack)).isItemEnabled(level.enabledFeatures())) continue;
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), result);
            entity.items.set(slot, ItemStack.EMPTY);
            level.sendBlockUpdated(pos, state, state, 3);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
        }
        if (changed) {
            CampfireBlockEntity.setChanged(level, pos, state);
        }
    }

    public static void cooldownTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity entity) {
        boolean changed = false;
        for (int slot = 0; slot < entity.items.size(); ++slot) {
            if (entity.cookingProgress[slot] <= 0) continue;
            changed = true;
            entity.cookingProgress[slot] = Mth.clamp(entity.cookingProgress[slot] - 2, 0, entity.cookingTime[slot]);
        }
        if (changed) {
            CampfireBlockEntity.setChanged(level, pos, state);
        }
    }

    public static void particleTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity entity) {
        RandomSource random = level.getRandom();
        if (random.nextFloat() < 0.11f) {
            for (int i = 0; i < random.nextInt(2) + 2; ++i) {
                CampfireBlock.makeParticles(level, pos, state.getValue(CampfireBlock.SIGNAL_FIRE), false);
            }
        }
        int rotation = state.getValue(CampfireBlock.FACING).get2DDataValue();
        for (int slot = 0; slot < entity.items.size(); ++slot) {
            if (entity.items.get(slot).isEmpty() || !(random.nextFloat() < 0.2f)) continue;
            Direction direction = Direction.from2DDataValue(Math.floorMod(slot + rotation, 4));
            float distanceFromCenter = 0.3125f;
            double x = (double)pos.getX() + 0.5 - (double)((float)direction.getStepX() * 0.3125f) + (double)((float)direction.getClockWise().getStepX() * 0.3125f);
            double y = (double)pos.getY() + 0.5;
            double z = (double)pos.getZ() + 0.5 - (double)((float)direction.getStepZ() * 0.3125f) + (double)((float)direction.getClockWise().getStepZ() * 0.3125f);
            for (int i = 0; i < 4; ++i) {
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 5.0E-4, 0.0);
            }
        }
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.clear();
        ContainerHelper.loadAllItems(input, this.items);
        input.getIntArray("CookingTimes").ifPresentOrElse(cookingTimes -> System.arraycopy(cookingTimes, 0, this.cookingProgress, 0, Math.min(this.cookingTime.length, ((int[])cookingTimes).length)), () -> Arrays.fill(this.cookingProgress, 0));
        input.getIntArray("CookingTotalTimes").ifPresentOrElse(cookingTimes -> System.arraycopy(cookingTimes, 0, this.cookingTime, 0, Math.min(this.cookingTime.length, ((int[])cookingTimes).length)), () -> Arrays.fill(this.cookingTime, 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items, true);
        output.putIntArray("CookingTimes", this.cookingProgress);
        output.putIntArray("CookingTotalTimes", this.cookingTime);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER);){
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            ContainerHelper.saveAllItems(output, this.items, true);
            CompoundTag compoundTag = output.buildResult();
            return compoundTag;
        }
    }

    public boolean placeFood(ServerLevel serverLevel, @Nullable LivingEntity sourceEntity, ItemStack placeItem) {
        for (int slot = 0; slot < this.items.size(); ++slot) {
            ItemStack item = this.items.get(slot);
            if (!item.isEmpty()) continue;
            Optional<RecipeHolder<CampfireCookingRecipe>> recipe = serverLevel.recipeAccess().getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(placeItem), serverLevel);
            if (recipe.isEmpty()) {
                return false;
            }
            this.cookingTime[slot] = recipe.get().value().cookingTime();
            this.cookingProgress[slot] = 0;
            this.items.set(slot, placeItem.consumeAndReturn(1, sourceEntity));
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(sourceEntity, this.getBlockState()));
            this.markUpdated();
            return true;
        }
        return false;
    }

    private void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.getItems());
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("Items");
    }
}

