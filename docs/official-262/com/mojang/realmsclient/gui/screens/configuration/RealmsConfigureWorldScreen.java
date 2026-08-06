/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.PreferredRegionsDto;
import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RegionDataDto;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.RegionSelectionPreferenceDto;
import com.mojang.realmsclient.dto.ServiceQuality;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigurationTab;
import com.mojang.realmsclient.gui.screens.configuration.RealmsPlayersTab;
import com.mojang.realmsclient.gui.screens.configuration.RealmsSettingsTab;
import com.mojang.realmsclient.gui.screens.configuration.RealmsSubscriptionTab;
import com.mojang.realmsclient.gui.screens.configuration.RealmsWorldsTab;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.CloseServerTask;
import com.mojang.realmsclient.util.task.OpenServerTask;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.LoadingTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.StringUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsConfigureWorldScreen
extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
    private final RealmsMainScreen lastScreen;
    private @Nullable RealmsServer serverData;
    private @Nullable PreferredRegionsDto regions;
    private @Nullable Subscription subscription;
    private final Map<RealmsRegion, ServiceQuality> regionServiceQuality = new LinkedHashMap<RealmsRegion, ServiceQuality>();
    private final long serverId;
    private boolean stateChanged;
    private final TabManager tabManager;
    private @Nullable Button playButton;
    private @Nullable MenuTabBar tabNavigationBar;
    final HeaderAndFooterLayout layout;

    public RealmsConfigureWorldScreen(RealmsMainScreen lastScreen, long serverId, @Nullable RealmsServer serverData, @Nullable PreferredRegionsDto regions) {
        super(Component.empty());
        RealmsConfigureWorldScreen realmsConfigureWorldScreen = this;
        Consumer<AbstractWidget> consumer = x$0 -> realmsConfigureWorldScreen.addRenderableWidget(x$0);
        realmsConfigureWorldScreen = this;
        this.tabManager = new TabManager(consumer, x$0 -> realmsConfigureWorldScreen.removeWidget((GuiEventListener)x$0), this::onTabSelected, this::onTabDeselected);
        this.layout = new HeaderAndFooterLayout(this);
        this.lastScreen = lastScreen;
        this.serverId = serverId;
        this.serverData = serverData;
        this.regions = regions;
    }

    public RealmsConfigureWorldScreen(RealmsMainScreen lastScreen, long serverId) {
        this(lastScreen, serverId, null, null);
    }

    @Override
    public void init() {
        if (this.serverData == null) {
            this.fetchServerData(this.serverId);
        }
        if (this.regions == null) {
            this.fetchRegionData();
        }
        if (this.subscription == null) {
            this.fetchSubscription(this.serverId);
        }
        MutableComponent loadingTitle = Component.translatable("mco.configure.world.loading");
        this.tabNavigationBar = MenuTabBar.builder(this.tabManager, this.width).addTabs(new LoadingTab(this.getFont(), RealmsWorldsTab.TITLE, loadingTitle), new LoadingTab(this.getFont(), RealmsPlayersTab.TITLE, loadingTitle), new LoadingTab(this.getFont(), RealmsSubscriptionTab.TITLE, loadingTitle), new LoadingTab(this.getFont(), RealmsSettingsTab.TITLE, loadingTitle)).build();
        this.tabNavigationBar.setTabActiveState(3, false);
        this.addRenderableWidget(this.tabNavigationBar);
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.playButton = footer.addChild(Button.builder(PLAY_TEXT, button -> {
            this.onClose();
            RealmsMainScreen.play(this.serverData, this);
        }).width(150).build());
        this.playButton.active = false;
        footer.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
        this.layout.visitWidgets(button -> {
            button.setTabOrderGroup(1);
            this.addRenderableWidget(button);
        });
        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();
        if (this.serverData != null && this.regions != null && this.subscription != null) {
            this.onRealmsDataFetched();
        }
    }

    private void onTabSelected(Tab tab) {
        if (this.serverData != null && tab instanceof RealmsConfigurationTab) {
            RealmsConfigurationTab configurationTab = (RealmsConfigurationTab)((Object)tab);
            configurationTab.onSelected(this.serverData);
        }
    }

    private void onTabDeselected(Tab tab) {
        if (this.serverData != null && tab instanceof RealmsConfigurationTab) {
            RealmsConfigurationTab configurationTab = (RealmsConfigurationTab)((Object)tab);
            configurationTab.onDeselected(this.serverData);
        }
    }

    public int getContentHeight() {
        return this.layout.getContentHeight();
    }

    public int getHeaderHeight() {
        return this.layout.getHeaderHeight();
    }

    public Screen getLastScreen() {
        return this.lastScreen;
    }

    public Screen createErrorScreen(RealmsServiceException exception) {
        return new RealmsGenericErrorScreen(exception, (Screen)this.lastScreen);
    }

    @Override
    public void repositionElements() {
        if (this.tabNavigationBar == null) {
            return;
        }
        this.tabNavigationBar.arrangeElements(this.width);
        int tabAreaTop = this.tabNavigationBar.getRectangle().bottom();
        ScreenRectangle tabArea = new ScreenRectangle(0, tabAreaTop, this.width, this.height - this.layout.getFooterHeight() - tabAreaTop);
        this.tabManager.setTabArea(tabArea);
        this.layout.setHeaderHeight(tabAreaTop);
        this.layout.arrangeElements();
    }

    private void updateButtonStates() {
        if (this.serverData != null && this.playButton != null) {
            this.playButton.active = this.serverData.shouldPlayButtonBeActive();
            if (!this.playButton.active && this.serverData.state == RealmsServer.State.CLOSED) {
                this.playButton.setTooltip(Tooltip.create(RealmsServer.WORLD_CLOSED_COMPONENT));
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int xm, int ym, float a) {
        super.extractRenderState(graphics, xm, ym, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0f, 0.0f, this.width, 2, 32, 2);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.tabNavigationBar.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0f, 0.0f, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.extractMenuBackground(graphics, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    @Override
    public void onClose() {
        Tab tab;
        if (this.serverData != null && (tab = this.tabManager.getCurrentTab()) instanceof RealmsConfigurationTab) {
            RealmsConfigurationTab tab2 = (RealmsConfigurationTab)((Object)tab);
            tab2.onDeselected(this.serverData);
        }
        this.minecraft.gui.setScreen(this.lastScreen);
        if (this.stateChanged) {
            this.lastScreen.resetScreen();
        }
    }

    public void fetchRegionData() {
        RealmsUtil.supplyAsync(RealmsClient::getPreferredRegionSelections, RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get realms region data")).thenAcceptAsync(regions -> {
            this.regions = regions;
            this.onRealmsDataFetched();
        }, (Executor)this.minecraft);
    }

    public void fetchServerData(long realmId) {
        RealmsUtil.supplyAsync(client -> client.getOwnRealm(realmId), RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get own world")).thenAcceptAsync(serverData -> {
            this.serverData = serverData;
            this.onRealmsDataFetched();
        }, (Executor)this.minecraft);
    }

    public void fetchSubscription(long realmId) {
        RealmsUtil.supplyAsync(client -> client.subscriptionFor(realmId), e -> {
            LOGGER.error("Couldn't get subscription", (Throwable)e);
            this.minecraft.execute(() -> {
                if (this.minecraft.gui.screen() instanceof RealmsConfigureWorldScreen) {
                    this.minecraft.gui.setScreen(this.createErrorScreen((RealmsServiceException)e));
                }
            });
        }).thenAcceptAsync(subscription -> {
            this.subscription = subscription;
            this.onRealmsDataFetched();
        }, (Executor)this.minecraft);
    }

    private void onRealmsDataFetched() {
        if (this.serverData == null || this.regions == null || this.subscription == null) {
            return;
        }
        this.regionServiceQuality.clear();
        for (RegionDataDto region : this.regions.regionData()) {
            if (region.region() == RealmsRegion.INVALID_REGION) continue;
            this.regionServiceQuality.put(region.region(), region.serviceQuality());
        }
        int focusedTabIndex = -1;
        if (this.tabNavigationBar != null) {
            focusedTabIndex = this.tabNavigationBar.getTabs().indexOf(this.tabManager.getCurrentTab());
        }
        if (this.tabNavigationBar != null) {
            this.removeWidget(this.tabNavigationBar);
        }
        this.tabNavigationBar = this.addRenderableWidget(MenuTabBar.builder(this.tabManager, this.width).addTabs(new RealmsWorldsTab(this, Objects.requireNonNull(this.minecraft), this.serverData), new RealmsPlayersTab(this, this.minecraft, this.serverData), new RealmsSubscriptionTab(this, this.minecraft, this.serverData, this.subscription), new RealmsSettingsTab(this, this.minecraft, this.serverData, this.regionServiceQuality)).build());
        this.setFocused(this.tabNavigationBar);
        if (focusedTabIndex != -1) {
            this.tabNavigationBar.selectTab(focusedTabIndex, false);
        }
        this.tabNavigationBar.setTabActiveState(3, !this.serverData.expired);
        if (this.serverData.expired) {
            this.tabNavigationBar.setTabTooltip(3, Tooltip.create(Component.translatable("mco.configure.world.settings.expired")));
        } else {
            this.tabNavigationBar.setTabTooltip(3, null);
        }
        this.updateButtonStates();
        this.repositionElements();
    }

    public void saveSlotSettings(RealmsSlot slot) {
        RealmsSlot oldSlot = this.serverData.slots.get(this.serverData.activeSlot);
        slot.options.templateId = oldSlot.options.templateId;
        slot.options.templateImage = oldSlot.options.templateImage;
        RealmsClient client = RealmsClient.getOrCreate();
        try {
            if (this.serverData.activeSlot != slot.slotId) {
                throw new RealmsServiceException(RealmsError.CustomError.configurationError());
            }
            client.updateSlot(this.serverData.id, slot.slotId, slot.options, slot.settings);
            this.serverData.slots.put(this.serverData.activeSlot, slot);
            if (slot.options.gameMode != oldSlot.options.gameMode || slot.isHardcore() != oldSlot.isHardcore()) {
                RealmsMainScreen.refreshServerList();
            }
            this.stateChanged();
        }
        catch (RealmsServiceException e) {
            LOGGER.error("Couldn't save slot settings", (Throwable)e);
            this.minecraft.gui.setScreen(new RealmsGenericErrorScreen(e, (Screen)this));
            return;
        }
        this.minecraft.gui.setScreen(this);
    }

    public void saveSettings(String name, String desc, RegionSelectionPreference preference, @Nullable RealmsRegion region) {
        String description = StringUtil.isBlank(desc) ? "" : desc;
        String finalName = StringUtil.isBlank(name) ? "" : name;
        RealmsClient client = RealmsClient.getOrCreate();
        try {
            RealmsSlot realmsSlot = this.serverData.slots.get(this.serverData.activeSlot);
            RealmsRegion regionSelection = preference == RegionSelectionPreference.MANUAL ? region : null;
            RegionSelectionPreferenceDto regionSelectionPreference = new RegionSelectionPreferenceDto(preference, regionSelection);
            client.updateConfiguration(this.serverData.id, finalName, description, regionSelectionPreference, realmsSlot.slotId, realmsSlot.options, realmsSlot.settings);
            this.serverData.regionSelectionPreference = regionSelectionPreference;
            this.serverData.name = name;
            this.serverData.motd = description;
            this.stateChanged();
        }
        catch (RealmsServiceException e) {
            LOGGER.error("Couldn't save settings", (Throwable)e);
            this.minecraft.gui.setScreen(new RealmsGenericErrorScreen(e, (Screen)this));
            return;
        }
        this.minecraft.gui.setScreen(this);
    }

    public void openTheWorld(boolean join) {
        RealmsConfigureWorldScreen screenWithKnownData = this.getNewScreenWithKnownData(this.serverData);
        this.minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(this.getNewScreen(), new OpenServerTask(this.serverData, screenWithKnownData, join, this.minecraft)));
    }

    public void closeTheWorld() {
        RealmsConfigureWorldScreen screenWithKnownData = this.getNewScreenWithKnownData(this.serverData);
        this.minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(this.getNewScreen(), new CloseServerTask(this.serverData, screenWithKnownData)));
    }

    public void stateChanged() {
        this.stateChanged = true;
        if (this.tabNavigationBar != null) {
            for (Tab child : this.tabNavigationBar.getTabs()) {
                if (!(child instanceof RealmsConfigurationTab)) continue;
                RealmsConfigurationTab tab = (RealmsConfigurationTab)((Object)child);
                tab.updateData(this.serverData);
            }
        }
    }

    public boolean invitePlayer(long serverId, String name) {
        RealmsClient client = RealmsClient.getOrCreate();
        try {
            List<PlayerInfo> players = client.invite(serverId, name);
            if (this.serverData != null) {
                this.serverData.players = players;
            } else {
                this.serverData = client.getOwnRealm(serverId);
            }
            this.stateChanged();
        }
        catch (RealmsServiceException e) {
            LOGGER.error("Couldn't invite user", (Throwable)e);
            return false;
        }
        return true;
    }

    public RealmsConfigureWorldScreen getNewScreen() {
        RealmsConfigureWorldScreen realmsConfigureWorldScreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId);
        realmsConfigureWorldScreen.stateChanged = this.stateChanged;
        return realmsConfigureWorldScreen;
    }

    public RealmsConfigureWorldScreen getNewScreenWithKnownData(RealmsServer serverData) {
        RealmsConfigureWorldScreen realmsConfigureWorldScreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId, serverData, this.regions);
        realmsConfigureWorldScreen.stateChanged = this.stateChanged;
        return realmsConfigureWorldScreen;
    }
}

