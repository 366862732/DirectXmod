/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens;

import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.PresetFlatWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class CreateFlatWorldScreen
extends Screen {
    private static final Component TITLE = Component.translatable("createWorld.customize.flat.title");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int SLOT_BG_SIZE = 18;
    private static final int SLOT_STAT_HEIGHT = 20;
    private static final int SLOT_BG_X = 1;
    private static final int SLOT_BG_Y = 1;
    private static final int SLOT_FG_X = 2;
    private static final int SLOT_FG_Y = 2;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 64);
    protected final CreateWorldScreen parent;
    private final Consumer<FlatLevelGeneratorSettings> applySettings;
    private FlatLevelGeneratorSettings generator;
    private @Nullable DetailsList list;
    private @Nullable Button deleteLayerButton;

    public CreateFlatWorldScreen(CreateWorldScreen parent, Consumer<FlatLevelGeneratorSettings> applySettings, FlatLevelGeneratorSettings generator) {
        super(TITLE);
        this.parent = parent;
        this.applySettings = applySettings;
        this.generator = generator;
    }

    public FlatLevelGeneratorSettings settings() {
        return this.generator;
    }

    public void setConfig(FlatLevelGeneratorSettings generator) {
        this.generator = generator;
        if (this.list != null) {
            this.list.resetRows();
            this.updateButtonValidity();
        }
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        this.list = this.layout.addToContents(new DetailsList(this));
        LinearLayout footer = this.layout.addToFooter(LinearLayout.vertical().spacing(4));
        footer.defaultCellSetting().alignVerticallyMiddle();
        LinearLayout topFooterButtons = footer.addChild(LinearLayout.horizontal().spacing(8));
        LinearLayout bottomFooterButtons = footer.addChild(LinearLayout.horizontal().spacing(8));
        this.deleteLayerButton = topFooterButtons.addChild(Button.builder(Component.translatable("createWorld.customize.flat.removeLayer"), button -> {
            Object patt0$temp;
            if (this.list != null && (patt0$temp = this.list.getSelected()) instanceof DetailsList.LayerEntry) {
                DetailsList.LayerEntry selectedLayerEntry = (DetailsList.LayerEntry)patt0$temp;
                this.list.deleteLayer(selectedLayerEntry);
            }
        }).build());
        topFooterButtons.addChild(Button.builder(Component.translatable("createWorld.customize.presets"), button -> {
            this.minecraft.gui.setScreen(new PresetFlatWorldScreen(this));
            this.generator.updateLayers();
            this.updateButtonValidity();
        }).build());
        bottomFooterButtons.addChild(Button.builder(CommonComponents.GUI_DONE, button -> {
            this.applySettings.accept(this.generator);
            this.onClose();
            this.generator.updateLayers();
        }).build());
        bottomFooterButtons.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.onClose();
            this.generator.updateLayers();
        }).build());
        this.generator.updateLayers();
        this.updateButtonValidity();
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
        this.layout.arrangeElements();
    }

    private void updateButtonValidity() {
        if (this.deleteLayerButton != null) {
            this.deleteLayerButton.active = this.hasValidSelection();
        }
    }

    private boolean hasValidSelection() {
        return this.list != null && this.list.getSelected() instanceof DetailsList.LayerEntry;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Environment(value=EnvType.CLIENT)
    private class DetailsList
    extends ObjectSelectionList<Entry> {
        private static final Component LAYER_MATERIAL_TITLE = Component.translatable("createWorld.customize.flat.tile").withStyle(ChatFormatting.UNDERLINE);
        private static final Component HEIGHT_TITLE = Component.translatable("createWorld.customize.flat.height").withStyle(ChatFormatting.UNDERLINE);
        final /* synthetic */ CreateFlatWorldScreen this$0;

        public DetailsList(CreateFlatWorldScreen createFlatWorldScreen) {
            CreateFlatWorldScreen createFlatWorldScreen2 = createFlatWorldScreen;
            Objects.requireNonNull(createFlatWorldScreen2);
            this.this$0 = createFlatWorldScreen2;
            super(createFlatWorldScreen.minecraft, createFlatWorldScreen.width, createFlatWorldScreen.height - 103, 43, 24);
            this.populateList();
        }

        private void populateList() {
            this.addEntry(new HeaderEntry(this.this$0.font), (int)((double)this.this$0.font.lineHeight * 1.5));
            SequencedCollection layersInfo = this.this$0.generator.getLayersInfo().reversed();
            for (int i = 0; i < layersInfo.size(); ++i) {
                this.addEntry(new LayerEntry(this, (FlatLayerInfo)layersInfo.get(i), i));
            }
        }

        @Override
        public void setSelected(@Nullable Entry selected) {
            super.setSelected(selected);
            this.this$0.updateButtonValidity();
        }

        public void resetRows() {
            int index = this.children().indexOf(this.getSelected());
            this.clearEntries();
            this.populateList();
            List children = this.children();
            if (index >= 0 && index < children.size()) {
                this.setSelected((Entry)children.get(index));
            }
        }

        private void deleteLayer(LayerEntry selectedLayerEntry) {
            List<FlatLayerInfo> layersInfo = this.this$0.generator.getLayersInfo();
            int deletedLayerIndex = this.children().indexOf(selectedLayerEntry);
            this.removeEntry(selectedLayerEntry);
            layersInfo.remove(selectedLayerEntry.layerInfo);
            this.setSelected(layersInfo.isEmpty() ? null : (Entry)this.children().get(Math.min(deletedLayerIndex, layersInfo.size())));
            this.this$0.generator.updateLayers();
            this.resetRows();
            this.this$0.updateButtonValidity();
        }

        @Environment(value=EnvType.CLIENT)
        private static class HeaderEntry
        extends Entry {
            private final Font font;

            public HeaderEntry(Font font) {
                this.font = font;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.text(this.font, LAYER_MATERIAL_TITLE, this.getContentX(), this.getContentY(), -1);
                graphics.text(this.font, HEIGHT_TITLE, this.getContentRight() - this.font.width(HEIGHT_TITLE), this.getContentY(), -1);
            }

            @Override
            public Component getNarration() {
                return CommonComponents.joinForNarration(LAYER_MATERIAL_TITLE, HEIGHT_TITLE);
            }
        }

        @Environment(value=EnvType.CLIENT)
        private class LayerEntry
        extends Entry {
            private final FlatLayerInfo layerInfo;
            private final int index;
            final /* synthetic */ DetailsList this$1;

            public LayerEntry(DetailsList detailsList, FlatLayerInfo layerInfo, int index) {
                DetailsList detailsList2 = detailsList;
                Objects.requireNonNull(detailsList2);
                this.this$1 = detailsList2;
                this.layerInfo = layerInfo;
                this.index = index;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                BlockState blockState = this.layerInfo.getBlockState();
                ItemStack itemStack = this.getDisplayItem(blockState);
                this.blitSlot(graphics, this.getContentX(), this.getContentY(), itemStack);
                int y = this.getContentYMiddle() - this.this$1.this$0.font.lineHeight / 2;
                graphics.text(this.this$1.this$0.font, itemStack.getHoverName(), this.getContentX() + 18 + 5, y, -1);
                MutableComponent height = this.index == 0 ? Component.translatable("createWorld.customize.flat.layer.top", this.layerInfo.getHeight()) : (this.index == this.this$1.this$0.generator.getLayersInfo().size() - 1 ? Component.translatable("createWorld.customize.flat.layer.bottom", this.layerInfo.getHeight()) : Component.translatable("createWorld.customize.flat.layer", this.layerInfo.getHeight()));
                graphics.text(this.this$1.this$0.font, height, this.getContentRight() - this.this$1.this$0.font.width(height), y, -1);
            }

            private ItemStack getDisplayItem(BlockState blockState) {
                Item item = blockState.getBlock().asItem();
                if (item == Items.AIR) {
                    if (blockState.is(Blocks.WATER)) {
                        item = Items.WATER_BUCKET;
                    } else if (blockState.is(Blocks.LAVA)) {
                        item = Items.LAVA_BUCKET;
                    }
                }
                return new ItemStack(item);
            }

            @Override
            public Component getNarration() {
                ItemStack itemStack = this.getDisplayItem(this.layerInfo.getBlockState());
                if (!itemStack.isEmpty()) {
                    return CommonComponents.joinForNarration(Component.translatable("narrator.select", itemStack.getHoverName()), HEIGHT_TITLE, Component.literal(String.valueOf(this.layerInfo.getHeight())));
                }
                return CommonComponents.EMPTY;
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                this.this$1.setSelected(this);
                return super.mouseClicked(event, doubleClick);
            }

            private void blitSlot(GuiGraphicsExtractor graphics, int x, int y, ItemStack itemStack) {
                this.blitSlotBg(graphics, x + 1, y + 1);
                if (!itemStack.isEmpty()) {
                    graphics.fakeItem(itemStack, x + 2, y + 2);
                }
            }

            private void blitSlotBg(GuiGraphicsExtractor graphics, int x, int y) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);
            }
        }

        @Environment(value=EnvType.CLIENT)
        private static abstract class Entry
        extends ObjectSelectionList.Entry<Entry> {
            private Entry() {
            }
        }
    }
}

