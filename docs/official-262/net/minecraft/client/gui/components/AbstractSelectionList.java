/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractSelectionList<E extends Entry<E>>
extends AbstractContainerWidget {
    private static final Identifier MENU_LIST_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");
    private static final Identifier INWORLD_MENU_LIST_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
    private static final int SEPARATOR_HEIGHT = 2;
    protected final Minecraft minecraft;
    protected final int defaultEntryHeight;
    private final List<E> children = new TrackedList(this);
    protected boolean centerListVertically = true;
    private @Nullable E selected;
    private @Nullable E hovered;

    public AbstractSelectionList(Minecraft minecraft, int width, int height, int y, int defaultEntryHeight) {
        super(0, y, width, height, CommonComponents.EMPTY, AbstractScrollArea.defaultSettings(defaultEntryHeight / 2));
        this.minecraft = minecraft;
        this.defaultEntryHeight = defaultEntryHeight;
    }

    public @Nullable E getSelected() {
        return this.selected;
    }

    public void setSelected(@Nullable E selected) {
        this.selected = selected;
        if (selected != null) {
            boolean bottomClipped;
            boolean topClipped = ((Entry)selected).getContentY() < this.getY();
            boolean bl = bottomClipped = ((Entry)selected).getContentBottom() > this.getBottom();
            if (this.minecraft.getLastInputType().isKeyboard() || topClipped || bottomClipped) {
                this.scrollToEntry(selected);
            }
        }
    }

    public @Nullable E getFocused() {
        return (E)((Entry)super.getFocused());
    }

    public final List<E> children() {
        return Collections.unmodifiableList(this.children);
    }

    protected void sort(Comparator<E> comparator) {
        this.children.sort(comparator);
        this.repositionEntries();
    }

    protected void swap(int firstIndex, int secondIndex) {
        Collections.swap(this.children, firstIndex, secondIndex);
        this.repositionEntries();
        this.scrollToEntry((Entry)this.children.get(secondIndex));
    }

    protected void clearEntries() {
        this.children.clear();
        this.selected = null;
    }

    protected void clearEntriesExcept(E exception) {
        this.children.removeIf(entry -> entry != exception);
        if (this.selected != exception) {
            this.setSelected(null);
        }
    }

    public void replaceEntries(Collection<E> newChildren) {
        this.clearEntries();
        for (Entry newChild : newChildren) {
            this.addEntry(newChild);
        }
    }

    private int getFirstEntryY() {
        return this.getY() + 2;
    }

    public int getNextY() {
        int y = this.getFirstEntryY() - (int)this.scrollAmount();
        for (Entry child : this.children) {
            y += child.getHeight();
        }
        return y;
    }

    protected int addEntry(E entry) {
        return this.addEntry(entry, this.defaultEntryHeight);
    }

    protected int addEntry(E entry, int height) {
        ((Entry)entry).setX(this.getRowLeft());
        ((Entry)entry).setWidth(this.getRowWidth());
        ((Entry)entry).setY(this.getNextY());
        ((Entry)entry).setHeight(height);
        this.children.add(entry);
        return this.children.size() - 1;
    }

    protected void addEntryToTop(E entry) {
        this.addEntryToTop(entry, this.defaultEntryHeight);
    }

    protected void addEntryToTop(E entry, int height) {
        double scrollFromBottom = (double)this.maxScrollAmount() - this.scrollAmount();
        ((Entry)entry).setHeight(height);
        this.children.addFirst(entry);
        this.repositionEntries();
        this.setScrollAmount((double)this.maxScrollAmount() - scrollFromBottom);
    }

    private void repositionEntries() {
        int y = this.getFirstEntryY() - (int)this.scrollAmount();
        for (Entry child : this.children) {
            child.setY(y);
            y += child.getHeight();
            child.setX(this.getRowLeft());
            child.setWidth(this.getRowWidth());
        }
    }

    protected void removeEntryFromTop(E entry) {
        double scrollFromBottom = (double)this.maxScrollAmount() - this.scrollAmount();
        this.removeEntry(entry);
        this.setScrollAmount((double)this.maxScrollAmount() - scrollFromBottom);
    }

    protected int getItemCount() {
        return this.children().size();
    }

    protected boolean entriesCanBeSelected() {
        return true;
    }

    protected final @Nullable E getEntryAtPosition(double posX, double posY) {
        for (Entry child : this.children) {
            if (!child.isMouseOver(posX, posY)) continue;
            return (E)child;
        }
        return null;
    }

    public void updateSize(int width, HeaderAndFooterLayout layout) {
        this.updateSizeAndPosition(width, layout.getContentHeight(), layout.getHeaderHeight());
    }

    public void updateSizeAndPosition(int width, int height, int y) {
        this.updateSizeAndPosition(width, height, 0, y);
    }

    public void updateSizeAndPosition(int width, int height, int x, int y) {
        this.setSize(width, height);
        this.setPosition(x, y);
        this.repositionEntries();
        if (this.getSelected() != null) {
            this.scrollToEntry(this.getSelected());
        }
        this.refreshScrollAmount();
    }

    @Override
    protected int contentHeight() {
        int totalHeight = 0;
        for (Entry child : this.children) {
            totalHeight += child.getHeight();
        }
        return totalHeight + 4;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.hovered = this.isMouseOver(mouseX, mouseY) ? this.getEntryAtPosition(mouseX, mouseY) : null;
        this.extractListBackground(graphics);
        this.enableScissor(graphics);
        this.extractListItems(graphics, mouseX, mouseY, a);
        graphics.disableScissor();
        this.extractListSeparators(graphics);
        this.extractScrollbar(graphics, mouseX, mouseY);
    }

    protected void extractListSeparators(GuiGraphicsExtractor graphics) {
        Identifier headerSeparator = this.minecraft.level == null ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
        Identifier footerSeparator = this.minecraft.level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
        graphics.blit(RenderPipelines.GUI_TEXTURED, headerSeparator, this.getX(), this.getY() - 2, 0.0f, 0.0f, this.getWidth(), 2, 32, 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, footerSeparator, this.getX(), this.getBottom(), 0.0f, 0.0f, this.getWidth(), 2, 32, 2);
    }

    protected void extractListBackground(GuiGraphicsExtractor graphics) {
        Identifier menuListBackground = this.minecraft.level == null ? MENU_LIST_BACKGROUND : INWORLD_MENU_LIST_BACKGROUND;
        graphics.blit(RenderPipelines.GUI_TEXTURED, menuListBackground, this.getX(), this.getY(), (float)this.getRight(), (float)(this.getBottom() + (int)this.scrollAmount()), this.getWidth(), this.getHeight(), 32, 32);
    }

    protected void enableScissor(GuiGraphicsExtractor graphics) {
        graphics.enableScissor(Mth.clamp(this.getX(), 0, graphics.guiWidth()), Mth.clamp(this.getY(), 0, graphics.guiHeight()), Mth.clamp(this.getRight(), 0, graphics.guiWidth()), Mth.clamp(this.getBottom(), 0, graphics.guiHeight()));
    }

    protected void scrollToEntry(E entry) {
        int bottomDelta;
        int topDelta = ((Entry)entry).getY() - this.getY() - 2;
        if (topDelta < 0) {
            this.scroll(topDelta);
        }
        if ((bottomDelta = this.getBottom() - ((Entry)entry).getY() - ((Entry)entry).getHeight() - 2) < 0) {
            this.scroll(-bottomDelta);
        }
    }

    protected void centerScrollOn(E entry) {
        int y = 0;
        for (Entry child : this.children) {
            if (child == entry) {
                y += child.getHeight() / 2;
                break;
            }
            y += child.getHeight();
        }
        this.setScrollAmount((double)y - (double)this.height / 2.0);
    }

    private void scroll(int amount) {
        this.setScrollAmount(this.scrollAmount() + (double)amount);
    }

    @Override
    public void setScrollAmount(double scrollAmount) {
        super.setScrollAmount(scrollAmount);
        this.repositionEntries();
    }

    @Override
    protected int scrollBarX() {
        return this.getRowRight() + this.scrollbarWidth() + 2;
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double x, double y) {
        return Optional.ofNullable(this.getEntryAtPosition(x, y));
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        GuiEventListener oldFocus = this.getFocused();
        if (oldFocus != focused && oldFocus instanceof ContainerEventHandler) {
            ContainerEventHandler oldFocusContainer = (ContainerEventHandler)oldFocus;
            oldFocusContainer.setFocused(null);
        }
        super.setFocused(focused);
        int index = this.children.indexOf(focused);
        if (index >= 0) {
            Entry magicallyCastEntry = (Entry)this.children.get(index);
            this.setSelected(magicallyCastEntry);
        }
    }

    protected @Nullable E nextEntry(ScreenDirection dir) {
        return (E)this.nextEntry(dir, entry -> true);
    }

    protected @Nullable E nextEntry(ScreenDirection dir, Predicate<E> canSelect) {
        return this.nextEntry(dir, canSelect, this.getSelected());
    }

    protected @Nullable E nextEntry(ScreenDirection dir, Predicate<E> canSelect, @Nullable E startEntry) {
        int delta;
        switch (dir) {
            default: {
                throw new MatchException(null, null);
            }
            case RIGHT: 
            case LEFT: {
                int n = 0;
                break;
            }
            case UP: {
                int n = -1;
                break;
            }
            case DOWN: {
                int n = delta = 1;
            }
        }
        if (!this.children().isEmpty() && delta != 0) {
            int index = startEntry == null ? (delta > 0 ? 0 : this.children().size() - 1) : this.children().indexOf(startEntry) + delta;
            for (int i = index; i >= 0 && i < this.children.size(); i += delta) {
                Entry selected = (Entry)this.children().get(i);
                if (!canSelect.test(selected)) continue;
                return (E)selected;
            }
        }
        return null;
    }

    protected void extractListItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        for (Entry child : this.children) {
            if (child.getY() + child.getHeight() < this.getY() || child.getY() > this.getBottom()) continue;
            this.extractItem(graphics, mouseX, mouseY, a, child);
        }
    }

    protected void extractItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, E entry) {
        if (this.entriesCanBeSelected() && this.getSelected() == entry) {
            int outlineColor = this.isFocused() ? -1 : -8355712;
            this.extractSelection(graphics, entry, outlineColor);
        }
        ((Entry)entry).extractContent(graphics, mouseX, mouseY, Objects.equals(this.hovered, entry), a);
    }

    protected void extractSelection(GuiGraphicsExtractor graphics, E entry, int outlineColor) {
        int outlineX0 = ((Entry)entry).getX();
        int outlineY0 = ((Entry)entry).getY();
        int outlineX1 = outlineX0 + ((Entry)entry).getWidth();
        int outlineY1 = outlineY0 + ((Entry)entry).getHeight();
        graphics.fill(outlineX0, outlineY0, outlineX1, outlineY1, outlineColor);
        graphics.fill(outlineX0 + 1, outlineY0 + 1, outlineX1 - 1, outlineY1 - 1, -16777216);
    }

    public int getRowLeft() {
        return this.getX() + this.width / 2 - this.getRowWidth() / 2;
    }

    public int getRowRight() {
        return this.getRowLeft() + this.getRowWidth();
    }

    public int getRowTop(int row) {
        return ((Entry)this.children.get(row)).getY();
    }

    public int getRowBottom(int row) {
        Entry child = (Entry)this.children.get(row);
        return child.getY() + child.getHeight();
    }

    public int getRowWidth() {
        return 220;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        }
        if (this.hovered != null) {
            return NarratableEntry.NarrationPriority.HOVERED;
        }
        return NarratableEntry.NarrationPriority.NONE;
    }

    protected void removeEntries(List<E> entries) {
        entries.forEach(this::removeEntry);
    }

    protected void removeEntry(E entry) {
        boolean removed = this.children.remove(entry);
        if (removed) {
            this.repositionEntries();
            if (entry == this.getSelected()) {
                this.setSelected(null);
            }
        }
    }

    protected @Nullable E getHovered() {
        return this.hovered;
    }

    private void bindEntryToSelf(Entry<E> entry) {
        entry.list = this;
    }

    protected void narrateListElementPosition(NarrationElementOutput output, E element) {
        int index;
        List<E> children = this.children();
        if (children.size() > 1 && (index = children.indexOf(element)) != -1) {
            output.add(NarratedElementType.POSITION, (Component)Component.translatable("narrator.position.list", index + 1, children.size()));
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class TrackedList
    extends AbstractList<E> {
        private final List<E> delegate;
        final /* synthetic */ AbstractSelectionList this$0;

        private TrackedList(AbstractSelectionList abstractSelectionList) {
            AbstractSelectionList abstractSelectionList2 = abstractSelectionList;
            Objects.requireNonNull(abstractSelectionList2);
            this.this$0 = abstractSelectionList2;
            this.delegate = Lists.newArrayList();
        }

        @Override
        public E get(int index) {
            return (Entry)this.delegate.get(index);
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public E set(int index, E element) {
            Entry entry = (Entry)this.delegate.set(index, element);
            this.this$0.bindEntryToSelf(element);
            return entry;
        }

        @Override
        public void add(int index, E element) {
            this.delegate.add(index, element);
            this.this$0.bindEntryToSelf(element);
        }

        @Override
        public E remove(int index) {
            return (Entry)this.delegate.remove(index);
        }
    }

    @Environment(value=EnvType.CLIENT)
    protected static abstract class Entry<E extends Entry<E>>
    implements LayoutElement,
    GuiEventListener {
        public static final int CONTENT_PADDING = 2;
        private int x = 0;
        private int y = 0;
        private int width = 0;
        private int height;
        @Deprecated
        private AbstractSelectionList<E> list;

        protected Entry() {
        }

        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return this.list.getFocused() == this;
        }

        public abstract void extractContent(GuiGraphicsExtractor var1, int var2, int var3, boolean var4, float var5);

        @Override
        public boolean isMouseOver(double mx, double my) {
            return this.getRectangle().containsPoint((int)mx, (int)my);
        }

        @Override
        public void setX(int x) {
            this.x = x;
        }

        @Override
        public void setY(int y) {
            this.y = y;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getContentX() {
            return this.getX() + 2;
        }

        public int getContentY() {
            return this.getY() + 2;
        }

        public int getContentHeight() {
            return this.getHeight() - 4;
        }

        public int getContentYMiddle() {
            return this.getContentY() + this.getContentHeight() / 2;
        }

        public int getContentBottom() {
            return this.getContentY() + this.getContentHeight();
        }

        public int getContentWidth() {
            return this.getWidth() - 4;
        }

        public int getContentXMiddle() {
            return this.getContentX() + this.getContentWidth() / 2;
        }

        public int getContentRight() {
            return this.getContentX() + this.getContentWidth();
        }

        @Override
        public int getX() {
            return this.x;
        }

        @Override
        public int getY() {
            return this.y;
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> widgetVisitor) {
        }

        @Override
        public ScreenRectangle getRectangle() {
            return LayoutElement.super.getRectangle();
        }
    }
}

