/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DataResult$Error
 *  com.mojang.serialization.DataResult$Success
 *  org.apache.commons.lang3.math.Fraction
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import com.mojang.serialization.DataResult;
import java.lang.runtime.SwitchBootstraps;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.math.Fraction;
import org.jspecify.annotations.Nullable;

public class BundleItem
extends Item {
    public static final int MAX_SHOWN_GRID_ITEMS_X = 4;
    public static final int MAX_SHOWN_GRID_ITEMS_Y = 3;
    public static final int MAX_SHOWN_GRID_ITEMS = 12;
    public static final int OVERFLOWING_MAX_SHOWN_GRID_ITEMS = 11;
    private static final int FULL_BAR_COLOR = ARGB.colorFromFloat(1.0f, 1.0f, 0.33f, 0.33f);
    private static final int BAR_COLOR = ARGB.colorFromFloat(1.0f, 0.44f, 0.53f, 1.0f);
    private static final int TICKS_AFTER_FIRST_THROW = 10;
    private static final int TICKS_BETWEEN_THROWS = 2;
    private static final int TICKS_MAX_THROW_DURATION = 200;

    public BundleItem(Item.Properties properties) {
        super(properties);
    }

    private static Fraction getWeightSafe(BundleContents contents) {
        DataResult<Fraction> dataResult = contents.weight();
        Objects.requireNonNull(dataResult);
        DataResult<Fraction> dataResult2 = dataResult;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{DataResult.Success.class, DataResult.Error.class}, dataResult2, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                DataResult.Success success = (DataResult.Success)dataResult2;
                yield (Fraction)success.value();
            }
            case 1 -> {
                DataResult.Error error = (DataResult.Error)dataResult2;
                yield Fraction.ONE;
            }
        };
    }

    public static float getFullnessDisplay(ItemStack itemStack) {
        BundleContents contents = itemStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return BundleItem.getWeightSafe(contents).floatValue();
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction clickAction, Player player) {
        BundleContents initialContents = self.get(DataComponents.BUNDLE_CONTENTS);
        if (initialContents == null) {
            return false;
        }
        ItemStack other = slot.getItem();
        BundleContents.Mutable contents = new BundleContents.Mutable(initialContents);
        if (clickAction == ClickAction.PRIMARY && !other.isEmpty()) {
            if (contents.tryTransfer(slot, player) > 0) {
                BundleItem.playInsertSound(player);
            } else {
                BundleItem.playInsertFailSound(player);
            }
            self.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
            this.broadcastChangesOnContainerMenu(player);
            return true;
        }
        if (clickAction == ClickAction.SECONDARY && other.isEmpty()) {
            ItemStack itemStack = contents.removeOne();
            if (itemStack != null) {
                ItemStack remainder = slot.safeInsert(itemStack);
                if (remainder.getCount() > 0) {
                    contents.tryInsert(remainder);
                } else {
                    BundleItem.playRemoveOneSound(player);
                }
            }
            self.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
            this.broadcastChangesOnContainerMenu(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem) {
        if (clickAction == ClickAction.PRIMARY && other.isEmpty()) {
            BundleItem.toggleSelectedItem(self, -1);
            return false;
        }
        BundleContents initialContents = self.get(DataComponents.BUNDLE_CONTENTS);
        if (initialContents == null) {
            return false;
        }
        BundleContents.Mutable contents = new BundleContents.Mutable(initialContents);
        if (clickAction == ClickAction.PRIMARY && !other.isEmpty()) {
            if (slot.allowModification(player) && contents.tryInsert(other) > 0) {
                BundleItem.playInsertSound(player);
            } else {
                BundleItem.playInsertFailSound(player);
            }
            self.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
            this.broadcastChangesOnContainerMenu(player);
            return true;
        }
        if (clickAction == ClickAction.SECONDARY && other.isEmpty()) {
            ItemStack removed;
            if (slot.allowModification(player) && (removed = contents.removeOne()) != null) {
                BundleItem.playRemoveOneSound(player);
                carriedItem.set(removed);
            }
            self.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
            this.broadcastChangesOnContainerMenu(player);
            return true;
        }
        BundleItem.toggleSelectedItem(self, -1);
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.SUCCESS;
    }

    private void dropContent(Level level, Player player, ItemStack itemStack) {
        if (this.dropContent(itemStack, player)) {
            BundleItem.playDropContentsSound(level, player);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return BundleItem.getWeightSafe(contents).compareTo(Fraction.ZERO) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return Math.min(1 + Mth.mulAndTruncate(BundleItem.getWeightSafe(contents), 12), 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return BundleItem.getWeightSafe(contents).compareTo(Fraction.ONE) >= 0 ? FULL_BAR_COLOR : BAR_COLOR;
    }

    public static void toggleSelectedItem(ItemStack stack, int selectedItem) {
        BundleContents initialContents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (initialContents == null) {
            return;
        }
        BundleContents.Mutable contents = new BundleContents.Mutable(initialContents);
        contents.toggleSelectedItem(selectedItem);
        stack.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
    }

    public static int getSelectedItemIndex(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).getSelectedItemIndex();
    }

    public static @Nullable ItemStackTemplate getSelectedItem(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).getSelectedItem();
    }

    public static int getNumberOfItemsToShow(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return contents.getNumberOfItemsToShow();
    }

    private boolean dropContent(ItemStack bundle, Player player) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) {
            return false;
        }
        Optional<ItemStack> itemStack = BundleItem.removeOneItemFromBundle(bundle, player, contents);
        if (itemStack.isPresent()) {
            player.drop(itemStack.get(), true);
            return true;
        }
        return false;
    }

    private static Optional<ItemStack> removeOneItemFromBundle(ItemStack self, Player player, BundleContents initialContents) {
        BundleContents.Mutable contents = new BundleContents.Mutable(initialContents);
        ItemStack removed = contents.removeOne();
        if (removed != null) {
            BundleItem.playRemoveOneSound(player);
            self.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
            return Optional.of(removed);
        }
        return Optional.empty();
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack itemStack, int ticksRemaining) {
        if (livingEntity instanceof Player) {
            boolean isFirstTick;
            Player player = (Player)livingEntity;
            int useDuration = this.getUseDuration(itemStack, livingEntity);
            boolean bl = isFirstTick = ticksRemaining == useDuration;
            if (isFirstTick || ticksRemaining < useDuration - 10 && ticksRemaining % 2 == 0) {
                this.dropContent(level, player, itemStack);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity entity) {
        return 200;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.BUNDLE;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack bundle) {
        TooltipDisplay display = bundle.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        if (!display.shows(DataComponents.BUNDLE_CONTENTS)) {
            return Optional.empty();
        }
        return Optional.ofNullable(bundle.get(DataComponents.BUNDLE_CONTENTS)).map(BundleTooltip::new);
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        BundleContents contents = entity.getItem().get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return;
        }
        entity.getItem().set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        ItemUtils.onContainerDestroyed(entity, contents.itemCopyStream());
    }

    private static void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + entity.level().getRandom().nextFloat() * 0.4f);
    }

    private static void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + entity.level().getRandom().nextFloat() * 0.4f);
    }

    private static void playInsertFailSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0f, 1.0f);
    }

    private static void playDropContentsSound(Level level, Entity entity) {
        level.playSound(null, entity.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8f, 0.8f + entity.level().getRandom().nextFloat() * 0.4f);
    }

    private void broadcastChangesOnContainerMenu(Player player) {
        AbstractContainerMenu containerMenu = player.containerMenu;
        if (containerMenu != null) {
            containerMenu.slotsChanged(player.getInventory());
        }
    }
}

