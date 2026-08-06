/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.booleans.BooleanConsumer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.worldselection;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Collection;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.flag.FeatureFlags;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ConfirmExperimentalFeaturesScreen
extends Screen {
    private static final Component TITLE = Component.translatable("selectWorld.experimental.title");
    private static final Component MESSAGE = Component.translatable("selectWorld.experimental.message");
    private static final Component DETAILS_BUTTON = Component.translatable("selectWorld.experimental.details");
    private static final int COLUMN_SPACING = 10;
    private static final int DETAILS_BUTTON_WIDTH = 100;
    private final BooleanConsumer callback;
    private final Collection<Pack> enabledPacks;
    private final GridLayout layout = new GridLayout().columnSpacing(10).rowSpacing(20);

    public ConfirmExperimentalFeaturesScreen(Collection<Pack> enabledPacks, BooleanConsumer callback) {
        super(TITLE);
        this.enabledPacks = enabledPacks;
        this.callback = callback;
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), MESSAGE);
    }

    @Override
    protected void init() {
        super.init();
        GridLayout.RowHelper helper = this.layout.createRowHelper(2);
        LayoutSettings centered = helper.newCellSettings().alignHorizontallyCenter();
        helper.addChild(new StringWidget(this.title, this.font), 2, centered);
        MultiLineTextWidget messageLabel = helper.addChild(new MultiLineTextWidget(MESSAGE, this.font).setCentered(true), 2, centered);
        messageLabel.setMaxWidth(310);
        helper.addChild(Button.builder(DETAILS_BUTTON, button -> this.minecraft.gui.setScreen(new DetailsScreen(this))).width(100).build(), 2, centered);
        helper.addChild(Button.builder(CommonComponents.GUI_PROCEED, button -> this.callback.accept(true)).build());
        helper.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.callback.accept(false)).build());
        ConfirmExperimentalFeaturesScreen confirmExperimentalFeaturesScreen = this;
        this.layout.visitWidgets(x$0 -> confirmExperimentalFeaturesScreen.addRenderableWidget(x$0));
        this.layout.arrangeElements();
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        FrameLayout.alignInRectangle(this.layout, 0, 0, this.width, this.height, 0.5f, 0.5f);
    }

    @Override
    public void onClose() {
        this.callback.accept(false);
    }

    @Environment(value=EnvType.CLIENT)
    private class DetailsScreen
    extends Screen {
        private static final Component TITLE = Component.translatable("selectWorld.experimental.details.title");
        private final HeaderAndFooterLayout layout;
        private @Nullable PackList list;
        final /* synthetic */ ConfirmExperimentalFeaturesScreen this$0;

        private DetailsScreen(ConfirmExperimentalFeaturesScreen confirmExperimentalFeaturesScreen) {
            ConfirmExperimentalFeaturesScreen confirmExperimentalFeaturesScreen2 = confirmExperimentalFeaturesScreen;
            Objects.requireNonNull(confirmExperimentalFeaturesScreen2);
            this.this$0 = confirmExperimentalFeaturesScreen2;
            super(TITLE);
            this.layout = new HeaderAndFooterLayout(this);
        }

        @Override
        protected void init() {
            this.layout.addTitleHeader(TITLE, this.font);
            this.list = this.layout.addToContents(new PackList(this, this.minecraft, this.this$0.enabledPacks));
            this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
            DetailsScreen detailsScreen = this;
            this.layout.visitWidgets(x$0 -> detailsScreen.addRenderableWidget(x$0));
            this.repositionElements();
        }

        @Override
        protected void repositionElements() {
            if (this.list != null) {
                this.list.updateSize(this.width, this.layout);
            }
            this.layout.arrangeElements();
        }

        @Override
        public void onClose() {
            this.minecraft.gui.setScreen(this.this$0);
        }

        @Environment(value=EnvType.CLIENT)
        private class PackList
        extends ObjectSelectionList<PackListEntry> {
            public PackList(DetailsScreen detailsScreen, Minecraft minecraft, Collection<Pack> selectedPacks) {
                Objects.requireNonNull(detailsScreen);
                super(minecraft, detailsScreen.width, detailsScreen.layout.getContentHeight(), detailsScreen.layout.getHeaderHeight(), (minecraft.font.lineHeight + 2) * 3);
                for (Pack pack : selectedPacks) {
                    String nonVanillaFeatures = FeatureFlags.printMissingFlags(FeatureFlags.VANILLA_SET, pack.getRequestedFeatures());
                    if (nonVanillaFeatures.isEmpty()) continue;
                    Component title = ComponentUtils.mergeStyles(pack.getTitle(), Style.EMPTY.withBold(true));
                    MutableComponent message = Component.translatable("selectWorld.experimental.details.entry", nonVanillaFeatures);
                    this.addEntry(new PackListEntry(detailsScreen, title, message, MultiLineLabel.create(detailsScreen.font, (Component)message, this.getRowWidth())));
                }
            }

            @Override
            public int getRowWidth() {
                return this.width * 3 / 4;
            }
        }

        @Environment(value=EnvType.CLIENT)
        private class PackListEntry
        extends ObjectSelectionList.Entry<PackListEntry> {
            private final Component packId;
            private final Component message;
            private final MultiLineLabel splitMessage;
            final /* synthetic */ DetailsScreen this$1;

            private PackListEntry(DetailsScreen detailsScreen, Component packId, Component message, MultiLineLabel splitMessage) {
                DetailsScreen detailsScreen2 = detailsScreen;
                Objects.requireNonNull(detailsScreen2);
                this.this$1 = detailsScreen2;
                this.packId = packId;
                this.message = message;
                this.splitMessage = splitMessage;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                ActiveTextCollector textRenderer = graphics.textRenderer();
                graphics.text(((DetailsScreen)this.this$1).minecraft.font, this.packId, this.getContentX(), this.getContentY(), -1);
                this.splitMessage.visitLines(TextAlignment.LEFT, this.getContentX(), this.getContentY() + 12, ((DetailsScreen)this.this$1).font.lineHeight, textRenderer);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.packId, this.message));
            }
        }
    }
}

