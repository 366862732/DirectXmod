/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractContainerScreen<T extends AbstractContainerMenu>
extends Screen
implements MenuAccess<T> {
    public static final Identifier INVENTORY_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_front");
    protected static final int BACKGROUND_TEXTURE_WIDTH = 256;
    protected static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    protected static final int DEFAULT_IMAGE_WIDTH = 176;
    protected static final int DEFAULT_IMAGE_HEIGHT = 166;
    protected final int imageWidth;
    protected final int imageHeight;
    protected int titleLabelX;
    protected int titleLabelY;
    protected int inventoryLabelX;
    protected int inventoryLabelY;
    private final List<ItemSlotMouseAction> itemSlotMouseActions;
    protected final T menu;
    protected final Component playerInventoryTitle;
    protected @Nullable Slot hoveredSlot;
    private @Nullable Slot lastClickSlot;
    protected int leftPos;
    protected int topPos;
    protected final Set<Slot> quickCraftSlots = Sets.newHashSet();
    protected boolean isQuickCrafting;
    private int quickCraftingType;
    private @MouseButtonInfo.MouseButton int quickCraftingButton;
    private boolean skipNextRelease;
    private int quickCraftingRemainder;
    private boolean doubleclick;
    private ItemStack lastQuickMoved = ItemStack.EMPTY;

    public AbstractContainerScreen(T menu, Inventory inventory, Component title) {
        this(menu, inventory, title, 176, 166);
    }

    public AbstractContainerScreen(T menu, Inventory inventory, Component title, int imageWidth, int imageHeight) {
        super(title);
        this.menu = menu;
        this.playerInventoryTitle = inventory.getDisplayName();
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.skipNextRelease = true;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = imageHeight - 94;
        this.itemSlotMouseActions = new ArrayList<ItemSlotMouseAction>();
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.itemSlotMouseActions.clear();
        this.addItemSlotMouseAction(new BundleMouseActions(this.minecraft));
    }

    protected void addItemSlotMouseAction(ItemSlotMouseAction itemSlotMouseAction) {
        this.itemSlotMouseActions.add(itemSlotMouseAction);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractContents(graphics, mouseX, mouseY, a);
        this.extractCarriedItem(graphics, mouseX, mouseY);
        this.extractTooltip(graphics, mouseX, mouseY);
    }

    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int xo = this.leftPos;
        int yo = this.topPos;
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float)xo, (float)yo);
        this.extractLabels(graphics, mouseX, mouseY);
        Slot previouslyHoveredSlot = this.hoveredSlot;
        this.hoveredSlot = this.getHoveredSlot(mouseX, mouseY);
        this.extractSlotHighlightBack(graphics);
        this.extractSlots(graphics, mouseX, mouseY);
        this.extractSlotHighlightFront(graphics);
        if (previouslyHoveredSlot != null && previouslyHoveredSlot != this.hoveredSlot) {
            this.onStopHovering(previouslyHoveredSlot);
        }
        graphics.pose().popMatrix();
    }

    public void extractCarriedItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack carried = ((AbstractContainerMenu)this.menu).getCarried();
        if (!carried.isEmpty()) {
            int xOffset = 8;
            int yOffset = 8;
            String itemCount = null;
            if (this.isQuickCrafting && this.quickCraftSlots.size() > 1 && (carried = carried.copyWithCount(this.quickCraftingRemainder)).isEmpty()) {
                itemCount = String.valueOf((Object)ChatFormatting.YELLOW) + "0";
            }
            graphics.nextStratum();
            this.extractFloatingItem(graphics, carried, mouseX - 8, mouseY - 8, itemCount);
        }
    }

    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (Slot slot : ((AbstractContainerMenu)this.menu).slots) {
            if (!slot.isActive()) continue;
            this.extractSlot(graphics, slot, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (!itemMouseAction.matches(this.hoveredSlot) || !itemMouseAction.onMouseScrolled(scrollX, scrollY, this.hoveredSlot.index, this.hoveredSlot.getItem())) continue;
                return true;
            }
        }
        return false;
    }

    private void extractSlotHighlightBack(GuiGraphicsExtractor graphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    private void extractSlotHighlightFront(GuiGraphicsExtractor graphics) {
        if (this.hoveredSlot != null && this.hoveredSlot.isHighlightable()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, this.hoveredSlot.x - 4, this.hoveredSlot.y - 4, 24, 24);
        }
    }

    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot == null || !this.hoveredSlot.hasItem()) {
            return;
        }
        ItemStack item = this.hoveredSlot.getItem();
        if (((AbstractContainerMenu)this.menu).getCarried().isEmpty() || this.showTooltipWithItemInHand(item)) {
            graphics.setTooltipForNextFrame(this.font, this.getTooltipFromContainerItem(item), item.getTooltipImage(), mouseX, mouseY, item.get(DataComponents.TOOLTIP_STYLE));
        }
    }

    private boolean showTooltipWithItemInHand(ItemStack item) {
        return item.getTooltipImage().map(ClientTooltipComponent::create).map(ClientTooltipComponent::showTooltipWithItemInHand).orElse(false);
    }

    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        return AbstractContainerScreen.getTooltipFromItem(this.minecraft, itemStack);
    }

    private void extractFloatingItem(GuiGraphicsExtractor graphics, ItemStack carried, int x, int y, @Nullable String itemCount) {
        graphics.item(carried, x, y);
        graphics.itemDecorations(this.font, carried, x, y, itemCount);
    }

    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        Identifier icon;
        int x = slot.x;
        int y = slot.y;
        ItemStack itemStack = slot.getItem();
        boolean quickCraftStack = false;
        boolean done = false;
        ItemStack carried = ((AbstractContainerMenu)this.menu).getCarried();
        String itemCount = null;
        if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !carried.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                return;
            }
            if (AbstractContainerMenu.canItemQuickReplace(slot, carried, true) && ((AbstractContainerMenu)this.menu).canDragTo(slot)) {
                quickCraftStack = true;
                int maxSize = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                int carry = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                int newCount = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots.size(), this.quickCraftingType, carried) + carry;
                if (newCount > maxSize) {
                    newCount = maxSize;
                    itemCount = ChatFormatting.YELLOW.toString() + maxSize;
                }
                itemStack = carried.copyWithCount(newCount);
            } else {
                this.quickCraftSlots.remove(slot);
                this.recalculateQuickCraftRemaining();
            }
        }
        if (itemStack.isEmpty() && slot.isActive() && (icon = slot.getNoItemIcon()) != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x, y, 16, 16);
            done = true;
        }
        if (!done) {
            if (quickCraftStack) {
                graphics.fill(x, y, x + 16, y + 16, -2130706433);
            }
            int seed = slot.x + slot.y * this.imageWidth;
            if (slot.isFake()) {
                graphics.fakeItem(itemStack, x, y, seed);
            } else {
                graphics.item(itemStack, x, y, seed);
            }
            graphics.itemDecorations(this.font, itemStack, x, y, itemCount);
        }
    }

    private void recalculateQuickCraftRemaining() {
        ItemStack carried = ((AbstractContainerMenu)this.menu).getCarried();
        if (carried.isEmpty() || !this.isQuickCrafting) {
            return;
        }
        if (this.quickCraftingType == 2) {
            this.quickCraftingRemainder = carried.getMaxStackSize();
            return;
        }
        this.quickCraftingRemainder = carried.getCount();
        for (Slot slot : this.quickCraftSlots) {
            ItemStack slotItemStack = slot.getItem();
            int carry = slotItemStack.isEmpty() ? 0 : slotItemStack.getCount();
            int maxSize = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
            int newCount = Math.min(AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots.size(), this.quickCraftingType, carried) + carry, maxSize);
            this.quickCraftingRemainder -= newCount - carry;
        }
    }

    private @Nullable Slot getHoveredSlot(double x, double y) {
        for (Slot slot : ((AbstractContainerMenu)this.menu).slots) {
            if (!slot.isActive() || !this.isHovering(slot, x, y)) continue;
            return slot;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        boolean cloning = this.minecraft.options.keyPickItem.matchesMouse(event) && this.minecraft.player.hasInfiniteMaterials();
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        this.doubleclick = this.lastClickSlot == slot && doubleClick;
        this.skipNextRelease = false;
        if (event.button() == 0 || event.button() == 1 || cloning) {
            int xo = this.leftPos;
            int yo = this.topPos;
            boolean clickedOutside = this.hasClickedOutside(event.x(), event.y(), xo, yo);
            int slotId = -1;
            if (slot != null) {
                slotId = slot.index;
            }
            if (clickedOutside) {
                slotId = -999;
            }
            if (slotId != -1 && !this.isQuickCrafting) {
                if (((AbstractContainerMenu)this.menu).getCarried().isEmpty()) {
                    if (cloning) {
                        this.slotClicked(slot, slotId, event.button(), ContainerInput.CLONE);
                    } else {
                        boolean quickKey = slotId != -999 && event.hasShiftDown();
                        ContainerInput containerInput = ContainerInput.PICKUP;
                        if (quickKey) {
                            this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                            containerInput = ContainerInput.QUICK_MOVE;
                        } else if (slotId == -999) {
                            containerInput = ContainerInput.THROW;
                        }
                        this.slotClicked(slot, slotId, event.button(), containerInput);
                    }
                    this.skipNextRelease = true;
                } else {
                    this.isQuickCrafting = true;
                    this.quickCraftingButton = event.button();
                    this.quickCraftSlots.clear();
                    if (event.button() == 0) {
                        this.quickCraftingType = 0;
                    } else if (event.button() == 1) {
                        this.quickCraftingType = 1;
                    } else if (cloning) {
                        this.quickCraftingType = 2;
                    }
                }
            }
        } else {
            this.checkHotbarMouseClicked(event);
        }
        this.lastClickSlot = slot;
        return true;
    }

    private void checkHotbarMouseClicked(MouseButtonEvent event) {
        if (this.hoveredSlot != null && ((AbstractContainerMenu)this.menu).getCarried().isEmpty()) {
            if (this.minecraft.options.keySwapOffhand.matchesMouse(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ContainerInput.SWAP);
                return;
            }
            for (int i = 0; i < 9; ++i) {
                if (!this.minecraft.options.keyHotbarSlots[i].matchesMouse(event)) continue;
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ContainerInput.SWAP);
            }
        }
    }

    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        return mx < (double)xo || my < (double)yo || mx >= (double)(xo + this.imageWidth) || my >= (double)(yo + this.imageHeight);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        ItemStack carried = ((AbstractContainerMenu)this.menu).getCarried();
        if (slot != null && this.shouldAddSlotToQuickCraft(slot, carried) && this.quickCraftSlots.add(slot)) {
            this.recalculateQuickCraftRemaining();
            return true;
        }
        if (slot == null && ((AbstractContainerMenu)this.menu).getCarried().isEmpty()) {
            return super.mouseDragged(event, dx, dy);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        int xo = this.leftPos;
        int yo = this.topPos;
        boolean clickedOutside = this.hasClickedOutside(event.x(), event.y(), xo, yo);
        int slotId = -1;
        if (slot != null) {
            slotId = slot.index;
        }
        if (clickedOutside) {
            slotId = -999;
        }
        if (this.doubleclick && slot != null && event.button() == 0 && ((AbstractContainerMenu)this.menu).canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
            if (event.hasShiftDown()) {
                if (!this.lastQuickMoved.isEmpty()) {
                    for (Slot target : ((AbstractContainerMenu)this.menu).slots) {
                        if (target == null || !target.mayPickup(this.minecraft.player) || !target.hasItem() || target.container != slot.container || !AbstractContainerMenu.canItemQuickReplace(target, this.lastQuickMoved, true)) continue;
                        this.slotClicked(target, target.index, event.button(), ContainerInput.QUICK_MOVE);
                    }
                }
            } else {
                this.slotClicked(slot, slotId, event.button(), ContainerInput.PICKUP_ALL);
            }
            this.doubleclick = false;
        } else {
            if (this.isQuickCrafting && this.quickCraftingButton != event.button()) {
                this.isQuickCrafting = false;
                this.quickCraftSlots.clear();
                this.skipNextRelease = true;
                return true;
            }
            if (this.skipNextRelease) {
                this.skipNextRelease = false;
                return true;
            }
            if (this.isQuickCrafting && !this.quickCraftSlots.isEmpty()) {
                this.quickCraftToSlots();
            } else if (!((AbstractContainerMenu)this.menu).getCarried().isEmpty()) {
                if (this.minecraft.options.keyPickItem.matchesMouse(event)) {
                    this.slotClicked(slot, slotId, event.button(), ContainerInput.CLONE);
                } else {
                    boolean quickKey;
                    boolean bl = quickKey = slotId != -999 && event.hasShiftDown();
                    if (quickKey) {
                        this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                    }
                    this.slotClicked(slot, slotId, event.button(), quickKey ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
                }
            }
        }
        this.isQuickCrafting = false;
        return super.mouseReleased(event);
    }

    private boolean isHovering(Slot slot, double xm, double ym) {
        return this.isHovering(slot.x, slot.y, 16, 16, xm, ym);
    }

    protected boolean isHovering(int left, int top, int w, int h, double xm, double ym) {
        int xo = this.leftPos;
        int yo = this.topPos;
        return (xm -= (double)xo) >= (double)(left - 1) && xm < (double)(left + w + 1) && (ym -= (double)yo) >= (double)(top - 1) && ym < (double)(top + h + 1);
    }

    private void onStopHovering(Slot slot) {
        if (slot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (!itemMouseAction.matches(slot)) continue;
                itemMouseAction.onStopHovering(slot);
            }
        }
    }

    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        if (slot != null) {
            slotId = slot.index;
        }
        this.onMouseClickAction(slot, containerInput);
        this.minecraft.gameMode.handleContainerInput(((AbstractContainerMenu)this.menu).containerId, slotId, buttonNum, containerInput, this.minecraft.player);
    }

    protected void onMouseClickAction(@Nullable Slot slot, ContainerInput containerInput) {
        if (slot != null && slot.hasItem()) {
            for (ItemSlotMouseAction itemMouseAction : this.itemSlotMouseActions) {
                if (!itemMouseAction.matches(slot)) continue;
                itemMouseAction.onSlotClicked(slot, containerInput);
            }
        }
    }

    protected void handleSlotStateChanged(int slotId, int containerId, boolean newState) {
        this.minecraft.gameMode.handleSlotStateChanged(slotId, containerId, newState);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        if (this.minecraft.options.keyInventory.matches(event)) {
            this.onClose();
            return true;
        }
        this.checkHotbarKeyPressed(event);
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            if (this.minecraft.options.keyPickItem.matches(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ContainerInput.CLONE);
            } else if (this.minecraft.options.keyDrop.matches(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, event.hasControlDown() ? 1 : 0, ContainerInput.THROW);
            }
        }
        return false;
    }

    protected boolean checkHotbarKeyPressed(KeyEvent event) {
        if (((AbstractContainerMenu)this.menu).getCarried().isEmpty() && this.hoveredSlot != null) {
            if (this.minecraft.options.keySwapOffhand.matches(event)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ContainerInput.SWAP);
                return true;
            }
            for (int i = 0; i < 9; ++i) {
                if (!this.minecraft.options.keyHotbarSlots[i].matches(event)) continue;
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ContainerInput.SWAP);
                return true;
            }
        }
        return false;
    }

    @Override
    public void removed() {
        if (this.minecraft.player == null) {
            return;
        }
        ((AbstractContainerMenu)this.menu).removed(this.minecraft.player);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public final void tick() {
        super.tick();
        if (!this.minecraft.player.isAlive() || this.minecraft.player.isRemoved()) {
            this.minecraft.player.closeContainer();
        } else {
            this.containerTick();
        }
    }

    protected void containerTick() {
    }

    private boolean shouldAddSlotToQuickCraft(Slot slot, ItemStack carried) {
        return this.isQuickCrafting && !carried.isEmpty() && (carried.getCount() > this.quickCraftSlots.size() || this.quickCraftingType == 2) && AbstractContainerMenu.canItemQuickReplace(slot, carried, true) && slot.mayPlace(carried) && ((AbstractContainerMenu)this.menu).canDragTo(slot);
    }

    private void quickCraftToSlots() {
        this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(0, this.quickCraftingType), ContainerInput.QUICK_CRAFT);
        for (Slot quickSlot : this.quickCraftSlots) {
            this.slotClicked(quickSlot, quickSlot.index, AbstractContainerMenu.getQuickcraftMask(1, this.quickCraftingType), ContainerInput.QUICK_CRAFT);
        }
        this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(2, this.quickCraftingType), ContainerInput.QUICK_CRAFT);
    }

    @Override
    public T getMenu() {
        return this.menu;
    }

    @Override
    public void onClose() {
        this.minecraft.player.closeContainer();
        if (this.hoveredSlot != null) {
            this.onStopHovering(this.hoveredSlot);
        }
        super.onClose();
    }
}

