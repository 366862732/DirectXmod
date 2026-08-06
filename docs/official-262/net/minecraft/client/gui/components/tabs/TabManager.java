/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.tabs;

import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TabManager {
    private final Consumer<AbstractWidget> addWidget;
    private final Consumer<AbstractWidget> removeWidget;
    private final Consumer<Tab> onSelected;
    private final Consumer<Tab> onDeselected;
    private @Nullable Tab currentTab;
    private @Nullable ScreenRectangle tabArea;

    public TabManager(Consumer<AbstractWidget> addWidget, Consumer<AbstractWidget> removeWidget) {
        this(addWidget, removeWidget, t -> {}, t -> {});
    }

    public TabManager(Consumer<AbstractWidget> addWidget, Consumer<AbstractWidget> removeWidget, Consumer<Tab> onSelected, Consumer<Tab> onDeselected) {
        this.addWidget = addWidget;
        this.removeWidget = removeWidget;
        this.onSelected = onSelected;
        this.onDeselected = onDeselected;
    }

    public void setTabArea(ScreenRectangle tabArea) {
        this.tabArea = tabArea;
        Tab tab = this.getCurrentTab();
        if (tab != null) {
            tab.doLayout(tabArea);
        }
    }

    public void setCurrentTab(Tab tab, boolean playSound) {
        this.setCurrentTab(tab, playSound, true);
    }

    public void setCurrentTab(Tab tab, boolean playSound, boolean addWidget) {
        if (!Objects.equals(this.currentTab, tab)) {
            if (this.currentTab != null) {
                this.currentTab.visitChildren(this.removeWidget);
            }
            Tab oldTab = this.currentTab;
            this.currentTab = tab;
            if (addWidget) {
                tab.visitChildren(this.addWidget);
            }
            if (this.tabArea != null) {
                tab.doLayout(this.tabArea);
            }
            if (playSound) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
            this.onDeselected.accept(oldTab);
            this.onSelected.accept(this.currentTab);
        }
    }

    public @Nullable Tab getCurrentTab() {
        return this.currentTab;
    }
}

