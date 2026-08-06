/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.ServiceQuality;
import com.mojang.realmsclient.gui.screens.configuration.RealmsSettingsTab;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class RealmsPreferredRegionSelectionScreen
extends Screen {
    private static final Component REGION_SELECTION_LABEL = Component.translatable("mco.configure.world.region_preference.title");
    private static final int SPACING = 8;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final Screen parent;
    private final BiConsumer<RegionSelectionPreference, RealmsRegion> applySettings;
    private final Map<RealmsRegion, ServiceQuality> regionServiceQuality;
    private @Nullable RegionSelectionList list;
    private RealmsSettingsTab.RegionSelection selection;
    private @Nullable Button doneButton;

    public RealmsPreferredRegionSelectionScreen(Screen parent, BiConsumer<RegionSelectionPreference, RealmsRegion> applySettings, Map<RealmsRegion, ServiceQuality> regionServiceQuality, RealmsSettingsTab.RegionSelection currentSelection) {
        super(REGION_SELECTION_LABEL);
        this.parent = parent;
        this.applySettings = applySettings;
        this.regionServiceQuality = regionServiceQuality;
        this.selection = currentSelection;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    protected void init() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(8));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.getTitle(), this.font));
        this.list = this.layout.addToContents(new RegionSelectionList(this));
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());
        this.list.setSelected((RegionSelectionList.Entry)this.list.children().stream().filter(e -> Objects.equals(e.regionSelection, this.selection)).findFirst().orElse(null));
        RealmsPreferredRegionSelectionScreen realmsPreferredRegionSelectionScreen = this;
        this.layout.visitWidgets(x$0 -> realmsPreferredRegionSelectionScreen.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    private void onDone() {
        if (this.selection.region() != null) {
            this.applySettings.accept(this.selection.preference(), this.selection.region());
        }
        this.onClose();
    }

    private void updateButtonValidity() {
        if (this.doneButton != null && this.list != null) {
            this.doneButton.active = this.list.getSelected() != null;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class RegionSelectionList
    extends ObjectSelectionList<Entry> {
        final /* synthetic */ RealmsPreferredRegionSelectionScreen this$0;

        private RegionSelectionList(RealmsPreferredRegionSelectionScreen realmsPreferredRegionSelectionScreen) {
            RealmsPreferredRegionSelectionScreen realmsPreferredRegionSelectionScreen2 = realmsPreferredRegionSelectionScreen;
            Objects.requireNonNull(realmsPreferredRegionSelectionScreen2);
            this.this$0 = realmsPreferredRegionSelectionScreen2;
            super(realmsPreferredRegionSelectionScreen.minecraft, realmsPreferredRegionSelectionScreen.width, realmsPreferredRegionSelectionScreen.height - 77, 40, 16);
            this.addEntry(new Entry(this, RegionSelectionPreference.AUTOMATIC_PLAYER, null));
            this.addEntry(new Entry(this, RegionSelectionPreference.AUTOMATIC_OWNER, null));
            RegionSelectionList regionSelectionList = this;
            realmsPreferredRegionSelectionScreen.regionServiceQuality.keySet().stream().map(region -> new Entry(this, RegionSelectionPreference.MANUAL, (RealmsRegion)((Object)region))).forEach(x$0 -> regionSelectionList.addEntry(x$0));
        }

        @Override
        public void setSelected(@Nullable Entry selected) {
            super.setSelected(selected);
            if (selected != null) {
                this.this$0.selection = selected.regionSelection;
            }
            this.this$0.updateButtonValidity();
        }

        @Environment(value=EnvType.CLIENT)
        private class Entry
        extends ObjectSelectionList.Entry<Entry> {
            private final RealmsSettingsTab.RegionSelection regionSelection;
            private final Component name;
            final /* synthetic */ RegionSelectionList this$1;

            public Entry(RegionSelectionList regionSelectionList, @Nullable RegionSelectionPreference preference, RealmsRegion region) {
                this(regionSelectionList, new RealmsSettingsTab.RegionSelection(preference, region));
            }

            public Entry(RegionSelectionList regionSelectionList, RealmsSettingsTab.RegionSelection regionSelection) {
                RegionSelectionList regionSelectionList2 = regionSelectionList;
                Objects.requireNonNull(regionSelectionList2);
                this.this$1 = regionSelectionList2;
                this.regionSelection = regionSelection;
                this.name = regionSelection.preference() == RegionSelectionPreference.MANUAL ? (regionSelection.region() != null ? Component.translatable(regionSelection.region().translationKey) : Component.empty()) : Component.translatable(regionSelection.preference().translationKey);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.name);
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.text(this.this$1.this$0.font, this.name, this.getContentX() + 5, this.getContentY() + 2, -1);
                if (this.regionSelection.region() != null && this.this$1.this$0.regionServiceQuality.containsKey((Object)this.regionSelection.region())) {
                    ServiceQuality serviceQuality = this.this$1.this$0.regionServiceQuality.getOrDefault((Object)this.regionSelection.region(), ServiceQuality.UNKNOWN);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, serviceQuality.getIcon(), this.getContentRight() - 18, this.getContentY() + 2, 10, 8);
                }
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                this.this$1.setSelected(this);
                if (doubleClick) {
                    this.this$1.playDownSound(this.this$1.minecraft.getSoundManager());
                    this.this$1.this$0.onDone();
                    return true;
                }
                return super.mouseClicked(event, doubleClick);
            }

            @Override
            public boolean keyPressed(KeyEvent event) {
                if (event.isSelection()) {
                    this.this$1.playDownSound(this.this$1.minecraft.getSoundManager());
                    this.this$1.this$0.onDone();
                    return true;
                }
                return super.keyPressed(event);
            }
        }
    }
}

