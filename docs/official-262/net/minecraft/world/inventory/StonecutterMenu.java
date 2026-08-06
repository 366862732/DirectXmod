/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class StonecutterMenu
extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    private static final int INV_SLOT_START = 2;
    private static final int INV_SLOT_END = 29;
    private static final int USE_ROW_SLOT_START = 29;
    private static final int USE_ROW_SLOT_END = 38;
    private final ContainerLevelAccess access;
    private final DataSlot selectedRecipeIndex = DataSlot.standalone();
    private final Level level;
    private SelectableRecipe.SingleInputSet<StonecutterRecipe> recipesForInput = SelectableRecipe.SingleInputSet.empty();
    private ItemStack input = ItemStack.EMPTY;
    private long lastSoundTime;
    private final Slot inputSlot;
    private final Slot resultSlot;
    private Runnable slotUpdateListener = () -> {};
    public final Container container = new SimpleContainer(this, 1){
        final /* synthetic */ StonecutterMenu this$0;
        {
            StonecutterMenu stonecutterMenu = this$0;
            Objects.requireNonNull(stonecutterMenu);
            this.this$0 = stonecutterMenu;
            super(size);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            this.this$0.slotsChanged(this);
            this.this$0.slotUpdateListener.run();
        }
    };
    private final ResultContainer resultContainer = new ResultContainer();

    public StonecutterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public StonecutterMenu(int containerId, Inventory inventory, final ContainerLevelAccess access) {
        super(MenuType.STONECUTTER, containerId);
        this.access = access;
        this.level = inventory.player.level();
        this.inputSlot = this.addSlot(new Slot(this.container, 0, 20, 33));
        this.resultSlot = this.addSlot(new Slot(this, this.resultContainer, 1, 143, 33){
            final /* synthetic */ StonecutterMenu this$0;
            {
                StonecutterMenu stonecutterMenu = this$0;
                Objects.requireNonNull(stonecutterMenu);
                this.this$0 = stonecutterMenu;
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                carried.onCraftedBy(player, carried.getCount());
                this.this$0.resultContainer.awardUsedRecipes(player, this.getRelevantItems());
                ItemStack remaining = this.this$0.inputSlot.remove(1);
                if (!remaining.isEmpty()) {
                    this.this$0.setupResultSlot(this.this$0.selectedRecipeIndex.get());
                }
                access.execute((level, pos) -> {
                    long gameTime = level.getGameTime();
                    if (this.this$0.lastSoundTime != gameTime) {
                        level.playSound(null, (BlockPos)pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);
                        this.this$0.lastSoundTime = gameTime;
                    }
                });
                super.onTake(player, carried);
            }

            private List<ItemStack> getRelevantItems() {
                return List.of(this.this$0.inputSlot.getItem());
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlot(this.selectedRecipeIndex);
    }

    public int getSelectedRecipeIndex() {
        return this.selectedRecipeIndex.get();
    }

    public SelectableRecipe.SingleInputSet<StonecutterRecipe> getVisibleRecipes() {
        return this.recipesForInput;
    }

    public int getNumberOfVisibleRecipes() {
        return this.recipesForInput.size();
    }

    public boolean hasInputItem() {
        return this.inputSlot.hasItem() && !this.recipesForInput.isEmpty();
    }

    @Override
    public boolean stillValid(Player player) {
        return StonecutterMenu.stillValid(this.access, player, Blocks.STONECUTTER);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (this.selectedRecipeIndex.get() == buttonId) {
            return false;
        }
        if (this.isValidRecipeIndex(buttonId)) {
            this.selectedRecipeIndex.set(buttonId);
            this.setupResultSlot(buttonId);
        }
        return true;
    }

    private boolean isValidRecipeIndex(int buttonId) {
        return buttonId >= 0 && buttonId < this.recipesForInput.size();
    }

    @Override
    public void slotsChanged(Container container) {
        ItemStack input = this.inputSlot.getItem();
        if (!input.is(this.input.getItem())) {
            this.input = input.copy();
            this.setupRecipeList(input);
        }
    }

    private void setupRecipeList(ItemStack item) {
        this.selectedRecipeIndex.set(-1);
        this.resultSlot.set(ItemStack.EMPTY);
        this.recipesForInput = !item.isEmpty() ? this.level.recipeAccess().stonecutterRecipes().selectByInput(item) : SelectableRecipe.SingleInputSet.empty();
    }

    private void setupResultSlot(int index) {
        Optional<RecipeHolder<Object>> usedRecipe;
        if (!this.recipesForInput.isEmpty() && this.isValidRecipeIndex(index)) {
            SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry = this.recipesForInput.entries().get(index);
            usedRecipe = entry.recipe().recipe();
        } else {
            usedRecipe = Optional.empty();
        }
        usedRecipe.ifPresentOrElse(recipe -> {
            this.resultContainer.setRecipeUsed((RecipeHolder<?>)recipe);
            this.resultSlot.set(((StonecutterRecipe)recipe.value()).assemble(new SingleRecipeInput(this.container.getItem(0))));
        }, () -> {
            this.resultSlot.set(ItemStack.EMPTY);
            this.resultContainer.setRecipeUsed(null);
        });
        this.broadcastChanges();
    }

    @Override
    public MenuType<?> getType() {
        return MenuType.STONECUTTER;
    }

    public void registerUpdateListener(Runnable slotUpdateListener) {
        this.slotUpdateListener = slotUpdateListener;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultContainer && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            Item item = stack.getItem();
            clicked = stack.copy();
            if (slotIndex == 1) {
                item.onCraftedBy(stack, player);
                if (!this.moveItemStackTo(stack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex == 0 ? !this.moveItemStackTo(stack, 2, 38, false) : (this.level.recipeAccess().stonecutterRecipes().acceptsInput(stack) ? !this.moveItemStackTo(stack, 0, 1, false) : (slotIndex >= 2 && slotIndex < 29 ? !this.moveItemStackTo(stack, 29, 38, false) : slotIndex >= 29 && slotIndex < 38 && !this.moveItemStackTo(stack, 2, 29, false)))) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }
            slot.setChanged();
            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            if (slotIndex == 1) {
                player.drop(stack, false);
            }
            this.broadcastChanges();
        }
        return clicked;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultContainer.removeItemNoUpdate(1);
        this.access.execute((level, pos) -> this.clearContainer(player, this.container));
    }
}

