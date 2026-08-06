/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Sets
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.achievement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ItemDisplayWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.LoadingTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class StatsScreen
extends Screen {
    private static final Component TITLE = Component.translatable("gui.stats");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier HEADER_SPRITE = Identifier.withDefaultNamespace("statistics/header");
    private static final Identifier SORT_UP_SPRITE = Identifier.withDefaultNamespace("statistics/sort_up");
    private static final Identifier SORT_DOWN_SPRITE = Identifier.withDefaultNamespace("statistics/sort_down");
    private static final Component PENDING_TEXT = Component.translatable("multiplayer.downloadingStats");
    private static final Component NO_VALUE_DISPLAY = Component.translatable("stats.none");
    private static final Component GENERAL_BUTTON = Component.translatable("stat.generalButton");
    private static final Component ITEMS_BUTTON = Component.translatable("stat.itemsButton");
    private static final Component MOBS_BUTTON = Component.translatable("stat.mobsButton");
    protected final Screen lastScreen;
    private static final int LIST_WIDTH = 280;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager;
    private @Nullable MenuTabBar tabNavigationBar;
    private final StatsCounter stats;
    private boolean isLoading;

    public StatsScreen(Screen lastScreen, StatsCounter stats) {
        super(TITLE);
        StatsScreen statsScreen = this;
        Consumer<AbstractWidget> consumer = x$0 -> statsScreen.addRenderableWidget(x$0);
        statsScreen = this;
        this.tabManager = new TabManager(consumer, x$0 -> statsScreen.removeWidget((GuiEventListener)x$0));
        this.isLoading = true;
        this.lastScreen = lastScreen;
        this.stats = stats;
    }

    @Override
    protected void init() {
        Component loadingTitle = PENDING_TEXT;
        this.tabNavigationBar = MenuTabBar.builder(this.tabManager, this.width).addTabs(new LoadingTab(this.getFont(), GENERAL_BUTTON, loadingTitle), new LoadingTab(this.getFont(), ITEMS_BUTTON, loadingTitle), new LoadingTab(this.getFont(), MOBS_BUTTON, loadingTitle)).build();
        this.addRenderableWidget(this.tabNavigationBar);
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());
        this.tabNavigationBar.setTabActiveState(0, true);
        this.tabNavigationBar.setTabActiveState(1, false);
        this.tabNavigationBar.setTabActiveState(2, false);
        this.layout.visitWidgets(button -> {
            button.setTabOrderGroup(1);
            this.addRenderableWidget(button);
        });
        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();
        this.minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
    }

    public void onStatsUpdated() {
        if (this.isLoading) {
            if (this.tabNavigationBar != null) {
                this.removeWidget(this.tabNavigationBar);
            }
            this.tabNavigationBar = MenuTabBar.builder(this.tabManager, this.width).addTabs(new StatisticsTab(this, GENERAL_BUTTON, new GeneralStatisticsList(this, this.minecraft)), new StatisticsTab(this, ITEMS_BUTTON, new ItemStatisticsList(this, this.minecraft)), new StatisticsTab(this, MOBS_BUTTON, new MobsStatisticsList(this, this.minecraft))).build();
            this.setFocused(this.tabNavigationBar);
            this.addRenderableWidget(this.tabNavigationBar);
            this.setTabActiveStateAndTooltip(1);
            this.setTabActiveStateAndTooltip(2);
            this.tabNavigationBar.selectTab(0, false);
            this.repositionElements();
            this.isLoading = false;
        }
    }

    /*
     * Unable to fully structure code
     */
    private void setTabActiveStateAndTooltip(int index) {
        if (this.tabNavigationBar == null) {
            return;
        }
        var4_2 = this.tabNavigationBar.getTabs().get(index);
        if (!(var4_2 instanceof StatisticsTab)) ** GOTO lbl-1000
        statsTab = (StatisticsTab)var4_2;
        if (!statsTab.list.children().isEmpty()) {
            v0 = true;
        } else lbl-1000:
        // 2 sources

        {
            v0 = false;
        }
        active = v0;
        this.tabNavigationBar.setTabActiveState(index, active);
        if (active) {
            this.tabNavigationBar.setTabTooltip(index, null);
        } else {
            this.tabNavigationBar.setTabTooltip(index, Tooltip.create(Component.translatable("gui.stats.none_found")));
        }
    }

    @Override
    protected void repositionElements() {
        if (this.tabNavigationBar == null) {
            return;
        }
        this.tabNavigationBar.arrangeElements(this.width);
        int tabAreaTop = this.tabNavigationBar.getRectangle().bottom();
        ScreenRectangle tabArea = new ScreenRectangle(0, tabAreaTop, this.width, this.height - this.layout.getFooterHeight() - tabAreaTop);
        this.tabNavigationBar.getTabs().forEach(tab -> tab.visitChildren(child -> child.setHeight(tabArea.height())));
        this.tabManager.setTabArea(tabArea);
        this.layout.setHeaderHeight(tabAreaTop);
        this.layout.arrangeElements();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.tabNavigationBar != null && this.tabNavigationBar.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int xm, int ym, float a) {
        super.extractRenderState(graphics, xm, ym, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight(), 0.0f, 0.0f, this.width, 2, 32, 2);
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0f, 0.0f, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.extractMenuBackground(graphics, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    private static String getTranslationKey(Stat<Identifier> stat) {
        return "stat." + stat.getValue().toString().replace(':', '.');
    }

    @Environment(value=EnvType.CLIENT)
    private class StatisticsTab
    extends GridLayoutTab {
        protected final AbstractSelectionList<?> list;
        final /* synthetic */ StatsScreen this$0;

        public StatisticsTab(StatsScreen statsScreen, Component title, AbstractSelectionList<?> list) {
            StatsScreen statsScreen2 = statsScreen;
            Objects.requireNonNull(statsScreen2);
            this.this$0 = statsScreen2;
            super(title);
            this.layout.addChild(list, 1, 1);
            this.list = list;
        }

        @Override
        public void doLayout(ScreenRectangle screenRectangle) {
            this.list.updateSizeAndPosition(this.this$0.width, this.this$0.layout.getContentHeight(), this.this$0.layout.getHeaderHeight());
            super.doLayout(screenRectangle);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class GeneralStatisticsList
    extends ObjectSelectionList<Entry> {
        final /* synthetic */ StatsScreen this$0;

        public GeneralStatisticsList(StatsScreen statsScreen, Minecraft minecraft) {
            StatsScreen statsScreen2 = statsScreen;
            Objects.requireNonNull(statsScreen2);
            this.this$0 = statsScreen2;
            super(minecraft, statsScreen.width, statsScreen.layout.getContentHeight(), 33, 14);
            ObjectArrayList stats = new ObjectArrayList(Stats.CUSTOM.iterator());
            stats.sort(Comparator.comparing(k -> I18n.get(StatsScreen.getTranslationKey(k), new Object[0])));
            for (Stat stat : stats) {
                this.addEntry(new Entry(this, stat));
            }
        }

        @Override
        public int getRowWidth() {
            return 280;
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {
        }

        @Environment(value=EnvType.CLIENT)
        private class Entry
        extends ObjectSelectionList.Entry<Entry> {
            private final Stat<Identifier> stat;
            private final Component statDisplay;
            final /* synthetic */ GeneralStatisticsList this$1;

            private Entry(GeneralStatisticsList generalStatisticsList, Stat<Identifier> stat) {
                GeneralStatisticsList generalStatisticsList2 = generalStatisticsList;
                Objects.requireNonNull(generalStatisticsList2);
                this.this$1 = generalStatisticsList2;
                this.stat = stat;
                this.statDisplay = Component.translatable(StatsScreen.getTranslationKey(stat));
            }

            private String getValueText() {
                return this.stat.format(this.this$1.this$0.stats.getValue(this.stat));
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int y = this.getContentYMiddle() - ((StatsScreen)this.this$1.this$0).font.lineHeight / 2;
                int index = this.this$1.children().indexOf(this);
                int color = index % 2 == 0 ? -1 : -4539718;
                graphics.text(this.this$1.this$0.font, this.statDisplay, this.getContentX() + 2, y, color);
                String msg = this.getValueText();
                graphics.text(this.this$1.this$0.font, msg, this.getContentRight() - this.this$1.this$0.font.width(msg) - 4, y, color);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", Component.empty().append(this.statDisplay).append(CommonComponents.SPACE).append(this.getValueText()));
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class ItemStatisticsList
    extends ContainerObjectSelectionList<Entry> {
        private static final int SLOT_BG_SIZE = 18;
        private static final int SLOT_STAT_HEIGHT = 22;
        private static final int SLOT_BG_Y = 1;
        private static final int SORT_NONE = 0;
        private static final int SORT_DOWN = -1;
        private static final int SORT_UP = 1;
        protected final List<StatType<Block>> blockColumns;
        protected final List<StatType<Item>> itemColumns;
        protected final Comparator<ItemRow> itemStatSorter;
        protected @Nullable StatType<?> sortColumn;
        protected int sortOrder;
        final /* synthetic */ StatsScreen this$0;

        public ItemStatisticsList(StatsScreen statsScreen, Minecraft minecraft) {
            boolean addToList;
            StatsScreen statsScreen2 = statsScreen;
            Objects.requireNonNull(statsScreen2);
            this.this$0 = statsScreen2;
            super(minecraft, statsScreen.width, statsScreen.layout.getContentHeight(), 33, 22);
            this.itemStatSorter = new ItemRowComparator(this);
            this.blockColumns = Lists.newArrayList();
            this.blockColumns.add(Stats.BLOCK_MINED);
            this.itemColumns = Lists.newArrayList((Object[])new StatType[]{Stats.ITEM_BROKEN, Stats.ITEM_CRAFTED, Stats.ITEM_USED, Stats.ITEM_PICKED_UP, Stats.ITEM_DROPPED});
            Set items = Sets.newIdentityHashSet();
            for (Item item : BuiltInRegistries.ITEM) {
                addToList = false;
                for (StatType<Item> statType : this.itemColumns) {
                    if (!statType.contains(item) || statsScreen.stats.getValue(statType.get(item)) <= 0) continue;
                    addToList = true;
                }
                if (!addToList) continue;
                items.add(item);
            }
            for (Block block : BuiltInRegistries.BLOCK) {
                addToList = false;
                for (StatType<FeatureElement> statType : this.blockColumns) {
                    if (!statType.contains(block) || statsScreen.stats.getValue(statType.get(block)) <= 0) continue;
                    addToList = true;
                }
                if (!addToList) continue;
                items.add(block.asItem());
            }
            items.remove(Items.AIR);
            if (!items.isEmpty()) {
                this.addEntry(new HeaderEntry(this));
                for (Item item : items) {
                    this.addEntry(new ItemRow(this, item));
                }
            }
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {
        }

        private int getColumnX(int col) {
            return 75 + 40 * col;
        }

        @Override
        public int getRowWidth() {
            return 280;
        }

        private StatType<?> getColumn(int i) {
            return i < this.blockColumns.size() ? this.blockColumns.get(i) : this.itemColumns.get(i - this.blockColumns.size());
        }

        private int getColumnIndex(StatType<?> column) {
            int i = this.blockColumns.indexOf(column);
            if (i >= 0) {
                return i;
            }
            int j = this.itemColumns.indexOf(column);
            if (j >= 0) {
                return j + this.blockColumns.size();
            }
            return -1;
        }

        protected void sortByColumn(StatType<?> column) {
            if (column != this.sortColumn) {
                this.sortColumn = column;
                this.sortOrder = -1;
            } else if (this.sortOrder == -1) {
                this.sortOrder = 1;
            } else {
                this.sortColumn = null;
                this.sortOrder = 0;
            }
            this.sortItems(this.itemStatSorter);
        }

        protected void sortItems(Comparator<ItemRow> comparator) {
            List<ItemRow> itemRows = this.getItemRows();
            itemRows.sort(comparator);
            this.clearEntriesExcept((Entry)this.children().getFirst());
            for (ItemRow newChild : itemRows) {
                this.addEntry(newChild);
            }
        }

        private List<ItemRow> getItemRows() {
            ArrayList<ItemRow> itemRows = new ArrayList<ItemRow>();
            this.children().forEach(entry -> {
                if (entry instanceof ItemRow) {
                    ItemRow itemRow = (ItemRow)entry;
                    itemRows.add(itemRow);
                }
            });
            return itemRows;
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {
        }

        @Environment(value=EnvType.CLIENT)
        private class ItemRowComparator
        implements Comparator<ItemRow> {
            final /* synthetic */ ItemStatisticsList this$1;

            private ItemRowComparator(ItemStatisticsList itemStatisticsList) {
                ItemStatisticsList itemStatisticsList2 = itemStatisticsList;
                Objects.requireNonNull(itemStatisticsList2);
                this.this$1 = itemStatisticsList2;
            }

            @Override
            public int compare(ItemRow one, ItemRow two) {
                int key2;
                int key1;
                Item item1 = one.getItem();
                Item item2 = two.getItem();
                if (this.this$1.sortColumn == null) {
                    key1 = 0;
                    key2 = 0;
                } else if (this.this$1.blockColumns.contains(this.this$1.sortColumn)) {
                    int n;
                    BlockItem blockItem;
                    StatType<?> type = this.this$1.sortColumn;
                    if (item1 instanceof BlockItem) {
                        blockItem = (BlockItem)item1;
                        v0 = this.this$1.this$0.stats.getValue(type, blockItem.getBlock());
                    } else {
                        v0 = key1 = -1;
                    }
                    if (item2 instanceof BlockItem) {
                        blockItem = (BlockItem)item2;
                        n = this.this$1.this$0.stats.getValue(type, blockItem.getBlock());
                    } else {
                        n = -1;
                    }
                    key2 = n;
                } else {
                    StatType<?> type = this.this$1.sortColumn;
                    key1 = this.this$1.this$0.stats.getValue(type, item1);
                    key2 = this.this$1.this$0.stats.getValue(type, item2);
                }
                if (key1 == key2) {
                    return this.this$1.sortOrder * Integer.compare(Item.getId(item1), Item.getId(item2));
                }
                return this.this$1.sortOrder * Integer.compare(key1, key2);
            }
        }

        @Environment(value=EnvType.CLIENT)
        private class HeaderEntry
        extends Entry {
            private static final Identifier BLOCK_MINED_SPRITE = Identifier.withDefaultNamespace("statistics/block_mined");
            private static final Identifier ITEM_BROKEN_SPRITE = Identifier.withDefaultNamespace("statistics/item_broken");
            private static final Identifier ITEM_CRAFTED_SPRITE = Identifier.withDefaultNamespace("statistics/item_crafted");
            private static final Identifier ITEM_USED_SPRITE = Identifier.withDefaultNamespace("statistics/item_used");
            private static final Identifier ITEM_PICKED_UP_SPRITE = Identifier.withDefaultNamespace("statistics/item_picked_up");
            private static final Identifier ITEM_DROPPED_SPRITE = Identifier.withDefaultNamespace("statistics/item_dropped");
            private final StatSortButton blockMined;
            private final StatSortButton itemBroken;
            private final StatSortButton itemCrafted;
            private final StatSortButton itemUsed;
            private final StatSortButton itemPickedUp;
            private final StatSortButton itemDropped;
            private final List<AbstractWidget> children;
            final /* synthetic */ ItemStatisticsList this$1;

            private HeaderEntry(ItemStatisticsList itemStatisticsList) {
                ItemStatisticsList itemStatisticsList2 = itemStatisticsList;
                Objects.requireNonNull(itemStatisticsList2);
                this.this$1 = itemStatisticsList2;
                this.children = new ArrayList<AbstractWidget>();
                this.blockMined = new StatSortButton(this, 0, BLOCK_MINED_SPRITE);
                this.itemBroken = new StatSortButton(this, 1, ITEM_BROKEN_SPRITE);
                this.itemCrafted = new StatSortButton(this, 2, ITEM_CRAFTED_SPRITE);
                this.itemUsed = new StatSortButton(this, 3, ITEM_USED_SPRITE);
                this.itemPickedUp = new StatSortButton(this, 4, ITEM_PICKED_UP_SPRITE);
                this.itemDropped = new StatSortButton(this, 5, ITEM_DROPPED_SPRITE);
                this.children.addAll(List.of(this.blockMined, this.itemBroken, this.itemCrafted, this.itemUsed, this.itemPickedUp, this.itemDropped));
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                this.blockMined.setPosition(this.getContentX() + this.this$1.getColumnX(0) - 18, this.getContentY() + 1);
                this.blockMined.extractRenderState(graphics, mouseX, mouseY, a);
                this.itemBroken.setPosition(this.getContentX() + this.this$1.getColumnX(1) - 18, this.getContentY() + 1);
                this.itemBroken.extractRenderState(graphics, mouseX, mouseY, a);
                this.itemCrafted.setPosition(this.getContentX() + this.this$1.getColumnX(2) - 18, this.getContentY() + 1);
                this.itemCrafted.extractRenderState(graphics, mouseX, mouseY, a);
                this.itemUsed.setPosition(this.getContentX() + this.this$1.getColumnX(3) - 18, this.getContentY() + 1);
                this.itemUsed.extractRenderState(graphics, mouseX, mouseY, a);
                this.itemPickedUp.setPosition(this.getContentX() + this.this$1.getColumnX(4) - 18, this.getContentY() + 1);
                this.itemPickedUp.extractRenderState(graphics, mouseX, mouseY, a);
                this.itemDropped.setPosition(this.getContentX() + this.this$1.getColumnX(5) - 18, this.getContentY() + 1);
                this.itemDropped.extractRenderState(graphics, mouseX, mouseY, a);
                if (this.this$1.sortColumn != null) {
                    int offset = this.this$1.getColumnX(this.this$1.getColumnIndex(this.this$1.sortColumn)) - 36;
                    Identifier sprite = this.this$1.sortOrder == 1 ? SORT_UP_SPRITE : SORT_DOWN_SPRITE;
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getContentX() + offset, this.getContentY() + 1, 18, 18);
                }
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return this.children;
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return this.children;
            }

            @Environment(value=EnvType.CLIENT)
            private class StatSortButton
            extends ImageButton {
                private final Identifier sprite;

                private StatSortButton(HeaderEntry headerEntry, int column, Identifier sprite) {
                    Objects.requireNonNull(headerEntry);
                    super(18, 18, new WidgetSprites(HEADER_SPRITE, SLOT_SPRITE), button -> this$2.this$1.sortByColumn(this$2.this$1.getColumn(column)), headerEntry.this$1.getColumn(column).getDisplayName());
                    this.sprite = sprite;
                    this.setTooltip(Tooltip.create(this.getMessage()));
                }

                @Override
                public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                    Identifier background = this.sprites.get(this.isActive(), this.isHoveredOrFocused());
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, background, this.getX(), this.getY(), this.width, this.height);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), this.width, this.height);
                }
            }
        }

        @Environment(value=EnvType.CLIENT)
        private class ItemRow
        extends Entry {
            private final Item item;
            private final ItemRowWidget itemRowWidget;
            final /* synthetic */ ItemStatisticsList this$1;

            private ItemRow(ItemStatisticsList itemStatisticsList, Item item) {
                ItemStatisticsList itemStatisticsList2 = itemStatisticsList;
                Objects.requireNonNull(itemStatisticsList2);
                this.this$1 = itemStatisticsList2;
                this.item = item;
                this.itemRowWidget = new ItemRowWidget(this, item.getDefaultInstance());
            }

            protected Item getItem() {
                return this.item;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int col;
                this.itemRowWidget.setPosition(this.getContentX(), this.getContentY());
                this.itemRowWidget.extractRenderState(graphics, mouseX, mouseY, a);
                ItemStatisticsList itemStatsList = this.this$1;
                int index = itemStatsList.children().indexOf(this);
                for (col = 0; col < itemStatsList.blockColumns.size(); ++col) {
                    Stat<Block> stat;
                    Item item = this.item;
                    if (item instanceof BlockItem) {
                        BlockItem blockItem = (BlockItem)item;
                        stat = itemStatsList.blockColumns.get(col).get(blockItem.getBlock());
                    } else {
                        stat = null;
                    }
                    this.extractStat(graphics, stat, this.getContentX() + this.this$1.getColumnX(col), this.getContentYMiddle() - ((StatsScreen)this.this$1.this$0).font.lineHeight / 2, index % 2 == 0);
                }
                for (col = 0; col < itemStatsList.itemColumns.size(); ++col) {
                    this.extractStat(graphics, itemStatsList.itemColumns.get(col).get(this.item), this.getContentX() + this.this$1.getColumnX(col + itemStatsList.blockColumns.size()), this.getContentYMiddle() - ((StatsScreen)this.this$1.this$0).font.lineHeight / 2, index % 2 == 0);
                }
            }

            protected void extractStat(GuiGraphicsExtractor graphics, @Nullable Stat<?> stat, int x, int y, boolean shaded) {
                Component msg = stat == null ? NO_VALUE_DISPLAY : Component.literal(stat.format(this.this$1.this$0.stats.getValue(stat)));
                graphics.text(this.this$1.this$0.font, msg, x - this.this$1.this$0.font.width(msg), y, shaded ? -1 : -4539718);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(this.itemRowWidget);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(this.itemRowWidget);
            }

            @Environment(value=EnvType.CLIENT)
            private class ItemRowWidget
            extends ItemDisplayWidget {
                final /* synthetic */ ItemRow this$2;

                private ItemRowWidget(ItemRow itemRow, ItemStack itemStack) {
                    ItemRow itemRow2 = itemRow;
                    Objects.requireNonNull(itemRow2);
                    this.this$2 = itemRow2;
                    super(itemRow.this$1.minecraft, 1, 1, 18, 18, itemStack.getHoverName(), itemStack, false, true);
                }

                @Override
                protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, this.this$2.getContentX(), this.this$2.getContentY(), 18, 18);
                    super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
                }

                @Override
                protected void extractTooltip(GuiGraphicsExtractor graphics, int x, int y) {
                    super.extractTooltip(graphics, this.this$2.getContentX() + 18, this.this$2.getContentY() + 18);
                }
            }
        }

        @Environment(value=EnvType.CLIENT)
        private static abstract class Entry
        extends ContainerObjectSelectionList.Entry<Entry> {
            private Entry() {
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class MobsStatisticsList
    extends ObjectSelectionList<MobRow> {
        final /* synthetic */ StatsScreen this$0;

        public MobsStatisticsList(StatsScreen statsScreen, Minecraft minecraft) {
            StatsScreen statsScreen2 = statsScreen;
            Objects.requireNonNull(statsScreen2);
            this.this$0 = statsScreen2;
            super(minecraft, statsScreen.width, statsScreen.layout.getContentHeight(), 33, ((StatsScreen)statsScreen).font.lineHeight * 4);
            for (EntityType entityType : BuiltInRegistries.ENTITY_TYPE) {
                if (statsScreen.stats.getValue(Stats.ENTITY_KILLED.get(entityType)) <= 0 && statsScreen.stats.getValue(Stats.ENTITY_KILLED_BY.get(entityType)) <= 0) continue;
                this.addEntry(new MobRow(this, entityType));
            }
        }

        @Override
        public int getRowWidth() {
            return 280;
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {
        }

        @Environment(value=EnvType.CLIENT)
        private class MobRow
        extends ObjectSelectionList.Entry<MobRow> {
            private final Component mobName;
            private final Component kills;
            private final Component killedBy;
            private final boolean hasKills;
            private final boolean wasKilledBy;
            final /* synthetic */ MobsStatisticsList this$1;

            public MobRow(MobsStatisticsList mobsStatisticsList, EntityType<?> type) {
                MobsStatisticsList mobsStatisticsList2 = mobsStatisticsList;
                Objects.requireNonNull(mobsStatisticsList2);
                this.this$1 = mobsStatisticsList2;
                this.mobName = type.getDescription();
                int kills = mobsStatisticsList.this$0.stats.getValue(Stats.ENTITY_KILLED.get(type));
                if (kills == 0) {
                    this.kills = Component.translatable("stat_type.minecraft.killed.none", this.mobName);
                    this.hasKills = false;
                } else {
                    this.kills = Component.translatable("stat_type.minecraft.killed", kills, this.mobName);
                    this.hasKills = true;
                }
                int killedBy = mobsStatisticsList.this$0.stats.getValue(Stats.ENTITY_KILLED_BY.get(type));
                if (killedBy == 0) {
                    this.killedBy = Component.translatable("stat_type.minecraft.killed_by.none", this.mobName);
                    this.wasKilledBy = false;
                } else {
                    this.killedBy = Component.translatable("stat_type.minecraft.killed_by", this.mobName, killedBy);
                    this.wasKilledBy = true;
                }
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.text(this.this$1.this$0.font, this.mobName, this.getContentX() + 2, this.getContentY() + 1, -1);
                graphics.text(this.this$1.this$0.font, this.kills, this.getContentX() + 2 + 10, this.getContentY() + 1 + ((StatsScreen)this.this$1.this$0).font.lineHeight, this.hasKills ? -4539718 : -8355712);
                graphics.text(this.this$1.this$0.font, this.killedBy, this.getContentX() + 2 + 10, this.getContentY() + 1 + ((StatsScreen)this.this$1.this$0).font.lineHeight * 2, this.wasKilledBy ? -4539718 : -8355712);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.kills, this.killedBy));
            }
        }
    }
}

