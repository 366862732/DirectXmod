/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.util.concurrent.RateLimiter
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsAvailability;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientActivePlayersTooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsMainScreen
extends RealmsScreen {
    private static final Identifier INFO_SPRITE = Identifier.withDefaultNamespace("icon/info");
    private static final Identifier NEW_REALM_SPRITE = Identifier.withDefaultNamespace("icon/new_realm");
    private static final Identifier EXPIRED_SPRITE = Identifier.withDefaultNamespace("realm_status/expired");
    private static final Identifier EXPIRES_SOON_SPRITE = Identifier.withDefaultNamespace("realm_status/expires_soon");
    private static final Identifier OPEN_SPRITE = Identifier.withDefaultNamespace("realm_status/open");
    private static final Identifier CLOSED_SPRITE = Identifier.withDefaultNamespace("realm_status/closed");
    private static final Identifier INVITE_SPRITE = Identifier.withDefaultNamespace("icon/invite");
    private static final Identifier NEWS_SPRITE = Identifier.withDefaultNamespace("icon/news");
    public static final Identifier HARDCORE_MODE_SPRITE = Identifier.withDefaultNamespace("hud/heart/hardcore_full");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier NO_REALMS_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/no_realms.png");
    private static final Component TITLE = Component.translatable("menu.online");
    private static final Component LOADING_TEXT = Component.translatable("mco.selectServer.loading");
    private static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized");
    private static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
    private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
    private static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
    private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
    private static final Component LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
    private static final Component CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
    private static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
    private static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
    private static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
    private static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
    private static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
    private static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
    private static final Component NO_REALMS_TEXT = Component.translatable("mco.selectServer.noRealms");
    private static final Component NO_PENDING_INVITES = Component.translatable("mco.invites.nopending");
    private static final Component PENDING_INVITES = Component.translatable("mco.invites.pending");
    private static final Component INCOMPATIBLE_POPUP_TITLE = Component.translatable("mco.compatibility.incompatible.popup.title");
    private static final Component INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE = Component.translatable("mco.compatibility.incompatible.releaseType.popup.message");
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_COLUMNS = 3;
    private static final int BUTTON_SPACING = 4;
    private static final int CONTENT_WIDTH = 308;
    private static final int LOGO_PADDING = 5;
    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_PADDING = 11;
    private static final int NEW_REALM_SPRITE_WIDTH = 40;
    private static final int NEW_REALM_SPRITE_HEIGHT = 20;
    private static final boolean SNAPSHOT;
    private static boolean snapshotToggle;
    private final CompletableFuture<RealmsAvailability.Result> availability = RealmsAvailability.get();
    private @Nullable DataFetcher.Subscription dataSubscription;
    private final Set<UUID> handledSeenNotifications = new HashSet<UUID>();
    private static boolean regionsPinged;
    private final RateLimiter inviteNarrationLimiter;
    private final Screen lastScreen;
    private Button playButton;
    private Button backButton;
    private Button renewButton;
    private Button configureButton;
    private Button leaveButton;
    private RealmSelectionList realmSelectionList;
    private RealmsServerList serverList;
    private List<RealmsServer> availableSnapshotServers = List.of();
    private RealmsServerPlayerLists onlinePlayersPerRealm = new RealmsServerPlayerLists(Map.of());
    private volatile boolean trialsAvailable;
    private volatile @Nullable String newsLink;
    private final List<RealmsNotification> notifications = new ArrayList<RealmsNotification>();
    private Button addRealmButton;
    private NotificationButton pendingInvitesButton;
    private NotificationButton newsButton;
    private LayoutState activeLayoutState;
    private @Nullable HeaderAndFooterLayout layout;

    public RealmsMainScreen(Screen lastScreen) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.inviteNarrationLimiter = RateLimiter.create((double)0.01666666753590107);
    }

    @Override
    public void init() {
        this.serverList = new RealmsServerList(this.minecraft);
        this.realmSelectionList = new RealmSelectionList(this);
        MutableComponent invitesTitle = Component.translatable("mco.invites.title");
        this.pendingInvitesButton = new NotificationButton(invitesTitle, INVITE_SPRITE, button -> this.minecraft.gui.setScreen(new RealmsPendingInvitesScreen(this, invitesTitle)), null);
        MutableComponent newsTitle = Component.translatable("mco.news");
        this.newsButton = new NotificationButton(newsTitle, NEWS_SPRITE, b -> {
            String newsLink = this.newsLink;
            if (newsLink == null) {
                return;
            }
            ConfirmLinkScreen.confirmLinkNow((Screen)this, newsLink);
            if (this.newsButton.notificationCount() != 0) {
                RealmsPersistence.RealmsPersistenceData data = RealmsPersistence.readFile();
                data.hasUnreadNews = false;
                RealmsPersistence.writeFile(data);
                this.newsButton.setNotificationCount(0);
            }
        }, newsTitle);
        this.playButton = Button.builder(PLAY_TEXT, button -> RealmsMainScreen.play(this.getSelectedServer(), this)).width(100).build();
        this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, button -> this.configureClicked(this.getSelectedServer())).width(100).build();
        this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, button -> this.onRenew(this.getSelectedServer())).width(100).build();
        this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, button -> this.leaveClicked(this.getSelectedServer())).width(100).build();
        this.addRealmButton = Button.builder(Component.translatable("mco.selectServer.purchase"), button -> this.openTrialAvailablePopup()).size(100, 20).build();
        this.backButton = Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).width(100).build();
        if (RealmsClient.ENVIRONMENT == RealmsClient.Environment.STAGE) {
            this.addRenderableWidget(CycleButton.booleanBuilder(Component.literal("Snapshot"), Component.literal("Release"), snapshotToggle).create(5, 5, 100, 20, Component.literal("Realm"), (button, value) -> {
                snapshotToggle = value;
                this.availableSnapshotServers = List.of();
                this.debugRefreshDataFetchers();
            }));
        }
        this.updateLayout(LayoutState.LOADING);
        this.updateButtonStates();
        this.availability.thenAcceptAsync(result -> {
            Screen errorScreen = result.createErrorScreen(this.lastScreen);
            if (errorScreen == null) {
                this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
            } else {
                this.minecraft.gui.setScreen(errorScreen);
            }
        }, this.screenExecutor);
    }

    public static boolean isSnapshot() {
        return SNAPSHOT && snapshotToggle;
    }

    @Override
    protected void repositionElements() {
        if (this.layout != null) {
            this.realmSelectionList.updateSize(this.width, this.layout);
            this.layout.arrangeElements();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    private void updateLayout() {
        if (this.serverList.isEmpty() && this.availableSnapshotServers.isEmpty() && this.notifications.isEmpty()) {
            this.updateLayout(LayoutState.NO_REALMS);
        } else {
            this.updateLayout(LayoutState.LIST);
        }
    }

    private void updateLayout(LayoutState state) {
        RealmsMainScreen realmsMainScreen;
        if (this.activeLayoutState == state) {
            return;
        }
        if (this.layout != null) {
            realmsMainScreen = this;
            this.layout.visitWidgets(x$0 -> realmsMainScreen.removeWidget((GuiEventListener)x$0));
        }
        this.layout = this.createLayout(state);
        this.activeLayoutState = state;
        realmsMainScreen = this;
        this.layout.visitWidgets(x$0 -> realmsMainScreen.addRenderableWidget(x$0));
        this.repositionElements();
    }

    private HeaderAndFooterLayout createLayout(LayoutState state) {
        HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        layout.setHeaderHeight(44);
        layout.addToHeader(this.createHeader());
        Layout footer = this.createFooter(state);
        footer.arrangeElements();
        layout.setFooterHeight(footer.getHeight() + 22);
        layout.addToFooter(footer);
        switch (state.ordinal()) {
            case 0: {
                layout.addToContents(new LoadingDotsWidget(this.font, LOADING_TEXT));
                break;
            }
            case 1: {
                layout.addToContents(this.createNoRealmsContent());
                break;
            }
            case 2: {
                layout.addToContents(this.realmSelectionList);
            }
        }
        return layout;
    }

    private Layout createHeader() {
        int sideCellWidth = 90;
        LinearLayout buttons = LinearLayout.horizontal().spacing(4);
        buttons.defaultCellSetting().alignVerticallyMiddle();
        buttons.addChild(this.pendingInvitesButton);
        buttons.addChild(this.newsButton);
        LinearLayout header = LinearLayout.horizontal();
        header.defaultCellSetting().alignVerticallyMiddle();
        header.addChild(SpacerElement.width(90));
        header.addChild(RealmsMainScreen.realmsLogo(), LayoutSettings::alignHorizontallyCenter);
        header.addChild(new FrameLayout(90, 44)).addChild(buttons, LayoutSettings::alignHorizontallyRight);
        return header;
    }

    private Layout createFooter(LayoutState state) {
        GridLayout footer = new GridLayout().spacing(4);
        GridLayout.RowHelper helper = footer.createRowHelper(3);
        if (state == LayoutState.LIST) {
            helper.addChild(this.playButton);
            helper.addChild(this.configureButton);
            helper.addChild(this.renewButton);
            helper.addChild(this.leaveButton);
        }
        helper.addChild(this.addRealmButton);
        helper.addChild(this.backButton);
        return footer;
    }

    private LinearLayout createNoRealmsContent() {
        LinearLayout content = LinearLayout.vertical().spacing(8);
        content.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(ImageWidget.texture(130, 64, NO_REALMS_LOCATION, 130, 64));
        content.addChild(FocusableTextWidget.builder(NO_REALMS_TEXT, this.font).maxWidth(308).alwaysShowBorder(false).backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS).build());
        return content;
    }

    private void updateButtonStates() {
        RealmsServer server = this.getSelectedServer();
        boolean serverSelected = server != null;
        this.addRealmButton.active = this.activeLayoutState != LayoutState.LOADING;
        boolean bl = this.playButton.active = serverSelected && server.shouldPlayButtonBeActive();
        if (!this.playButton.active && serverSelected && server.state == RealmsServer.State.CLOSED) {
            this.playButton.setTooltip(Tooltip.create(RealmsServer.WORLD_CLOSED_COMPONENT));
        }
        this.renewButton.active = serverSelected && this.shouldRenewButtonBeActive(server);
        this.leaveButton.active = serverSelected && this.shouldLeaveButtonBeActive(server);
        this.configureButton.active = serverSelected && this.shouldConfigureButtonBeActive(server);
    }

    private boolean shouldRenewButtonBeActive(RealmsServer server) {
        return server.expired && RealmsMainScreen.isSelfOwnedServer(server);
    }

    private boolean shouldConfigureButtonBeActive(RealmsServer server) {
        return RealmsMainScreen.isSelfOwnedServer(server) && server.state != RealmsServer.State.UNINITIALIZED;
    }

    private boolean shouldLeaveButtonBeActive(RealmsServer server) {
        return !RealmsMainScreen.isSelfOwnedServer(server);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.dataSubscription != null) {
            this.dataSubscription.tick();
        }
    }

    public static void refreshPendingInvites() {
        Minecraft.getInstance().realmsDataFetcher().pendingInvitesTask.reset();
    }

    public static void refreshServerList() {
        Minecraft.getInstance().realmsDataFetcher().serverListUpdateTask.reset();
    }

    private void debugRefreshDataFetchers() {
        for (DataFetcher.Task<?> task : this.minecraft.realmsDataFetcher().getTasks()) {
            task.reset();
        }
    }

    private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher dataSource) {
        DataFetcher.Subscription result = dataSource.dataFetcher.createSubscription();
        result.subscribe(dataSource.serverListUpdateTask, updatedServers -> {
            this.serverList.updateServersList(updatedServers.serverList());
            this.availableSnapshotServers = updatedServers.availableSnapshotServers();
            this.refreshListAndLayout();
            boolean ownsNonExpiredRealmServer = false;
            for (RealmsServer retrievedServer : this.serverList) {
                if (!this.isSelfOwnedNonExpiredServer(retrievedServer)) continue;
                ownsNonExpiredRealmServer = true;
            }
            if (!regionsPinged && ownsNonExpiredRealmServer) {
                regionsPinged = true;
                this.pingRegions();
            }
        });
        RealmsMainScreen.callRealmsClient(RealmsClient::getNotifications, retrievedNotifications -> {
            this.notifications.clear();
            this.notifications.addAll((Collection<RealmsNotification>)retrievedNotifications);
            for (RealmsNotification notification : retrievedNotifications) {
                RealmsNotification.InfoPopup popup;
                PopupScreen popupScreen;
                if (!(notification instanceof RealmsNotification.InfoPopup) || (popupScreen = (popup = (RealmsNotification.InfoPopup)notification).buildScreen(this, this::dismissNotification)) == null) continue;
                this.minecraft.gui.setScreen(popupScreen);
                this.markNotificationsAsSeen(List.of(notification));
                break;
            }
            if (!this.notifications.isEmpty() && this.activeLayoutState != LayoutState.LOADING) {
                this.refreshListAndLayout();
            }
        });
        result.subscribe(dataSource.pendingInvitesTask, numberOfPendingInvites -> {
            this.pendingInvitesButton.setNotificationCount((int)numberOfPendingInvites);
            this.pendingInvitesButton.setTooltip(numberOfPendingInvites == 0 ? Tooltip.create(NO_PENDING_INVITES) : Tooltip.create(PENDING_INVITES));
            if (numberOfPendingInvites > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
                this.minecraft.getNarrator().saySystemNow(Component.translatable("mco.configure.world.invite.narration", numberOfPendingInvites));
            }
        });
        result.subscribe(dataSource.trialAvailabilityTask, newStatus -> {
            this.trialsAvailable = newStatus;
        });
        result.subscribe(dataSource.onlinePlayersTask, playerList -> {
            this.onlinePlayersPerRealm = playerList;
        });
        result.subscribe(dataSource.newsTask, news -> {
            dataSource.newsManager.updateUnreadNews((RealmsNews)news);
            this.newsLink = dataSource.newsManager.newsLink();
            this.newsButton.setNotificationCount(dataSource.newsManager.hasUnreadNews() ? Integer.MAX_VALUE : 0);
        });
        return result;
    }

    private void markNotificationsAsSeen(Collection<RealmsNotification> notifications) {
        ArrayList<UUID> seenNotifications = new ArrayList<UUID>(notifications.size());
        for (RealmsNotification notification : notifications) {
            if (notification.seen() || this.handledSeenNotifications.contains(notification.uuid())) continue;
            seenNotifications.add(notification.uuid());
        }
        if (!seenNotifications.isEmpty()) {
            RealmsMainScreen.callRealmsClient(realmsClient -> {
                realmsClient.notificationsSeen(seenNotifications);
                return null;
            }, ignored -> this.handledSeenNotifications.addAll(seenNotifications));
        }
    }

    private static <T> void callRealmsClient(RealmsCall<T> supplier, Consumer<T> callback) {
        Minecraft minecraft = Minecraft.getInstance();
        ((CompletableFuture)CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.request(RealmsClient.getOrCreate(minecraft));
            }
            catch (RealmsServiceException e) {
                throw new RuntimeException(e);
            }
        }, Util.nonCriticalIoPool()).thenAcceptAsync(callback, (Executor)minecraft)).exceptionally(e -> {
            LOGGER.error("Failed to execute call to Realms Service", e);
            return null;
        });
    }

    private void refreshListAndLayout() {
        this.realmSelectionList.refreshEntries(this);
        this.updateLayout();
        this.updateButtonStates();
    }

    private void pingRegions() {
        Thread pingThread = new Thread(() -> {
            List<RegionPingResult> regionPingResultList = Ping.pingAllRegions();
            RealmsClient client = RealmsClient.getOrCreate();
            PingResult pingResult = new PingResult(regionPingResultList, this.getOwnedNonExpiredRealmIds());
            try {
                client.sendPingResults(pingResult);
            }
            catch (Throwable t) {
                LOGGER.warn("Could not send ping result to Realms: ", t);
            }
        }, "Realms ping");
        pingThread.setDaemon(true);
        pingThread.start();
    }

    private List<Long> getOwnedNonExpiredRealmIds() {
        ArrayList ids = Lists.newArrayList();
        for (RealmsServer server : this.serverList) {
            if (!this.isSelfOwnedNonExpiredServer(server)) continue;
            ids.add(server.id);
        }
        return ids;
    }

    private void onRenew(@Nullable RealmsServer server) {
        if (server != null) {
            String extensionUrl = CommonLinks.extendRealms(server.remoteSubscriptionId, this.minecraft.getUser().getProfileId(), server.expiredTrial);
            this.minecraft.gui.setScreen(new ConfirmLinkScreen(result -> {
                if (result) {
                    Util.getPlatform().openUri(extensionUrl);
                } else {
                    this.minecraft.gui.setScreen(this);
                }
            }, extensionUrl, true));
        }
    }

    private void configureClicked(@Nullable RealmsServer selectedServer) {
        if (selectedServer != null && this.minecraft.isLocalPlayer(selectedServer.ownerUUID)) {
            this.minecraft.gui.setScreen(new RealmsConfigureWorldScreen(this, selectedServer.id));
        }
    }

    private void leaveClicked(@Nullable RealmsServer selectedServer) {
        if (selectedServer != null && !this.minecraft.isLocalPlayer(selectedServer.ownerUUID)) {
            MutableComponent popupMessage = Component.translatable("mco.configure.world.leave.question.line1");
            this.minecraft.gui.setScreen(RealmsPopups.infoPopupScreen(this, popupMessage, popupScreen -> this.leaveServer(selectedServer)));
        }
    }

    private @Nullable RealmsServer getSelectedServer() {
        Object e = this.realmSelectionList.getSelected();
        if (e instanceof ServerEntry) {
            ServerEntry entry = (ServerEntry)e;
            return entry.getServer();
        }
        return null;
    }

    private void leaveServer(final RealmsServer server) {
        new Thread(this, "Realms-leave-server"){
            final /* synthetic */ RealmsMainScreen this$0;
            {
                RealmsMainScreen realmsMainScreen = this$0;
                Objects.requireNonNull(realmsMainScreen);
                this.this$0 = realmsMainScreen;
                super(name);
            }

            @Override
            public void run() {
                try {
                    RealmsClient client = RealmsClient.getOrCreate();
                    client.uninviteMyselfFrom(server.id);
                    this.this$0.minecraft.execute(RealmsMainScreen::refreshServerList);
                }
                catch (RealmsServiceException e) {
                    LOGGER.error("Couldn't configure world", (Throwable)e);
                    this.this$0.minecraft.execute(() -> ((RealmsMainScreen)this.this$0).minecraft.gui.setScreen(new RealmsGenericErrorScreen(e, (Screen)this.this$0)));
                }
            }
        }.start();
        this.minecraft.gui.setScreen(this);
    }

    private void dismissNotification(UUID uuid) {
        RealmsMainScreen.callRealmsClient(realmsClient -> {
            realmsClient.notificationsDismiss(List.of(uuid));
            return null;
        }, ignored -> {
            this.notifications.removeIf(notification -> notification.dismissable() && uuid.equals(notification.uuid()));
            this.refreshListAndLayout();
        });
    }

    public void resetScreen() {
        this.realmSelectionList.setSelected((Entry)null);
        RealmsMainScreen.refreshServerList();
    }

    @Override
    public Component getNarrationMessage() {
        return switch (this.activeLayoutState.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> CommonComponents.joinForNarration(super.getNarrationMessage(), LOADING_TEXT);
            case 1 -> CommonComponents.joinForNarration(super.getNarrationMessage(), NO_REALMS_TEXT);
            case 2 -> super.getNarrationMessage();
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int xm, int ym, float a) {
        super.extractRenderState(graphics, xm, ym, a);
        if (RealmsMainScreen.isSnapshot()) {
            graphics.text(this.font, "Minecraft " + SharedConstants.getCurrentVersion().name(), 2, this.height - 10, -1);
        }
        if (this.trialsAvailable && this.addRealmButton.active) {
            AddRealmPopupScreen.extractDiamond(graphics, this.addRealmButton);
        }
        switch (RealmsClient.ENVIRONMENT) {
            case STAGE: {
                this.extractEnvironment(graphics, "STAGE!", -256);
                break;
            }
            case LOCAL: {
                this.extractEnvironment(graphics, "LOCAL!", -8388737);
            }
        }
    }

    private void openTrialAvailablePopup() {
        this.minecraft.gui.setScreen(new AddRealmPopupScreen(this, this.trialsAvailable));
    }

    public static void play(@Nullable RealmsServer server, Screen cancelScreen) {
        RealmsMainScreen.play(server, cancelScreen, false);
    }

    public static void play(@Nullable RealmsServer server, Screen cancelScreen, boolean skipCompatibility) {
        if (server != null) {
            if (!RealmsMainScreen.isSnapshot() || skipCompatibility || server.isMinigameActive()) {
                Minecraft.getInstance().gui.setScreen(new RealmsLongRunningMcoTaskScreen(cancelScreen, new GetServerDetailsTask(cancelScreen, server)));
                return;
            }
            switch (server.compatibility) {
                case COMPATIBLE: {
                    Minecraft.getInstance().gui.setScreen(new RealmsLongRunningMcoTaskScreen(cancelScreen, new GetServerDetailsTask(cancelScreen, server)));
                    break;
                }
                case UNVERIFIABLE: {
                    RealmsMainScreen.confirmToPlay(server, cancelScreen, Component.translatable("mco.compatibility.unverifiable.title").withColor(-171), Component.translatable("mco.compatibility.unverifiable.message"), CommonComponents.GUI_CONTINUE);
                    break;
                }
                case NEEDS_DOWNGRADE: {
                    RealmsMainScreen.confirmToPlay(server, cancelScreen, Component.translatable("selectWorld.backupQuestion.downgrade").withColor(-2142128), Component.translatable("mco.compatibility.downgrade.description", Component.literal(server.activeVersion).withColor(-171), Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171)), Component.translatable("mco.compatibility.downgrade"));
                    break;
                }
                case NEEDS_UPGRADE: {
                    RealmsMainScreen.upgradeRealmAndPlay(server, cancelScreen);
                    break;
                }
                case INCOMPATIBLE: {
                    Minecraft.getInstance().gui.setScreen(new PopupScreen.Builder(cancelScreen, INCOMPATIBLE_POPUP_TITLE).addMessage(Component.translatable("mco.compatibility.incompatible.series.popup.message", Component.literal(server.activeVersion).withColor(-171), Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171))).addButton(CommonComponents.GUI_BACK, PopupScreen::onClose).build());
                    break;
                }
                case RELEASE_TYPE_INCOMPATIBLE: {
                    Minecraft.getInstance().gui.setScreen(new PopupScreen.Builder(cancelScreen, INCOMPATIBLE_POPUP_TITLE).addMessage(INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE).addButton(CommonComponents.GUI_BACK, PopupScreen::onClose).build());
                }
            }
        }
    }

    private static void confirmToPlay(RealmsServer server, Screen lastScreen, Component title, Component message, Component confirmButton) {
        Minecraft.getInstance().gui.setScreen(new PopupScreen.Builder(lastScreen, title).addMessage(message).addButton(confirmButton, popupScreen -> {
            Minecraft.getInstance().gui.setScreen(new RealmsLongRunningMcoTaskScreen(lastScreen, new GetServerDetailsTask(lastScreen, server)));
            RealmsMainScreen.refreshServerList();
        }).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build());
    }

    private static void upgradeRealmAndPlay(RealmsServer server, Screen cancelScreen) {
        MutableComponent title = Component.translatable("mco.compatibility.upgrade.title").withColor(-171);
        MutableComponent confirmButton = Component.translatable("mco.compatibility.upgrade");
        MutableComponent serverVersion = Component.literal(server.activeVersion).withColor(-171);
        MutableComponent clientVersion = Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171);
        MutableComponent message = RealmsMainScreen.isSelfOwnedServer(server) ? Component.translatable("mco.compatibility.upgrade.description", serverVersion, clientVersion) : Component.translatable("mco.compatibility.upgrade.friend.description", serverVersion, clientVersion);
        RealmsMainScreen.confirmToPlay(server, cancelScreen, title, message, confirmButton);
    }

    public static Component getVersionComponent(String version, boolean isCompatible) {
        return RealmsMainScreen.getVersionComponent(version, isCompatible ? -8355712 : -2142128);
    }

    public static Component getVersionComponent(String version, int color) {
        if (StringUtils.isBlank((CharSequence)version)) {
            return CommonComponents.EMPTY;
        }
        return Component.literal(version).withColor(color);
    }

    public static Component getGameModeComponent(int gameMode, boolean hardcore) {
        if (hardcore) {
            return Component.translatable("gameMode.hardcore").withColor(-65536);
        }
        return GameType.byId(gameMode).getLongDisplayName();
    }

    private static boolean isSelfOwnedServer(RealmsServer serverData) {
        return Minecraft.getInstance().isLocalPlayer(serverData.ownerUUID);
    }

    private boolean isSelfOwnedNonExpiredServer(RealmsServer serverData) {
        return RealmsMainScreen.isSelfOwnedServer(serverData) && !serverData.expired;
    }

    private void extractEnvironment(GuiGraphicsExtractor graphics, String text, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float)(this.width / 2 - 25), 20.0f);
        graphics.pose().rotate(-0.34906584f);
        graphics.pose().scale(1.5f, 1.5f);
        graphics.text(this.font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    static {
        snapshotToggle = SNAPSHOT = !SharedConstants.getCurrentVersion().stable();
    }

    @Environment(value=EnvType.CLIENT)
    private class RealmSelectionList
    extends ObjectSelectionList<Entry> {
        final /* synthetic */ RealmsMainScreen this$0;

        public RealmSelectionList(RealmsMainScreen realmsMainScreen) {
            RealmsMainScreen realmsMainScreen2 = realmsMainScreen;
            Objects.requireNonNull(realmsMainScreen2);
            this.this$0 = realmsMainScreen2;
            super(Minecraft.getInstance(), realmsMainScreen.width, realmsMainScreen.height, 0, 36);
        }

        @Override
        public void setSelected(@Nullable Entry selected) {
            super.setSelected(selected);
            this.this$0.updateButtonStates();
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        private void refreshEntries(RealmsMainScreen realmsMainScreen) {
            Entry previouslySelected = (Entry)this.getSelected();
            this.clearEntries();
            for (RealmsNotification notification : this.this$0.notifications) {
                if (!(notification instanceof RealmsNotification.VisitUrl)) continue;
                RealmsNotification.VisitUrl visitUrl = (RealmsNotification.VisitUrl)notification;
                this.addEntriesForNotification(visitUrl, realmsMainScreen, previouslySelected);
                this.this$0.markNotificationsAsSeen(List.of(notification));
                break;
            }
            this.refreshServerEntries(previouslySelected);
        }

        private void addEntriesForNotification(RealmsNotification.VisitUrl visitUrl, RealmsMainScreen realmsMainScreen, @Nullable Entry previouslySelected) {
            NotificationMessageEntry notificationMessageEntry;
            Component message = visitUrl.getMessage();
            int messageHeight = this.this$0.font.wordWrapHeight(message, NotificationMessageEntry.textWidth(this.getRowWidth()));
            NotificationMessageEntry entry = new NotificationMessageEntry(this.this$0, realmsMainScreen, messageHeight, message, visitUrl);
            this.addEntry(entry, 38 + messageHeight);
            if (previouslySelected instanceof NotificationMessageEntry && (notificationMessageEntry = (NotificationMessageEntry)previouslySelected).getText().equals(message)) {
                this.setSelected(entry);
            }
        }

        private void refreshServerEntries(@Nullable Entry previouslySelected) {
            for (RealmsServer eligibleForSnapshotServer : this.this$0.availableSnapshotServers) {
                this.addEntry(new AvailableSnapshotEntry(this.this$0, eligibleForSnapshotServer));
            }
            for (RealmsServer server : this.this$0.serverList) {
                Entry entry;
                if (RealmsMainScreen.isSnapshot() && !server.isSnapshotRealm()) {
                    if (server.state == RealmsServer.State.UNINITIALIZED) continue;
                    entry = new ParentEntry(this.this$0, server);
                } else {
                    entry = new ServerEntry(this.this$0, server);
                }
                this.addEntry(entry);
                if (!(previouslySelected instanceof ServerEntry)) continue;
                ServerEntry serverEntry = (ServerEntry)previouslySelected;
                if (serverEntry.serverData.id != server.id) continue;
                this.setSelected(entry);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class NotificationButton
    extends SpriteIconButton.CenteredIcon {
        private static final Identifier[] NOTIFICATION_ICONS = new Identifier[]{Identifier.withDefaultNamespace("notification/1"), Identifier.withDefaultNamespace("notification/2"), Identifier.withDefaultNamespace("notification/3"), Identifier.withDefaultNamespace("notification/4"), Identifier.withDefaultNamespace("notification/5"), Identifier.withDefaultNamespace("notification/more")};
        private static final int UNKNOWN_COUNT = Integer.MAX_VALUE;
        private static final int SIZE = 20;
        private static final int SPRITE_SIZE = 14;
        private int notificationCount;

        public NotificationButton(Component title, Identifier texture, Button.OnPress onPress, @Nullable Component tooltip) {
            super(20, 20, title, 14, 14, 0, 0, new WidgetSprites(texture), onPress, tooltip, null, false);
        }

        private int notificationCount() {
            return this.notificationCount;
        }

        public void setNotificationCount(int notificationCount) {
            this.notificationCount = notificationCount;
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            super.extractContents(graphics, mouseX, mouseY, a);
            if (this.isActive() && this.notificationCount != 0) {
                this.extractNotificationCounter(graphics);
            }
        }

        private void extractNotificationCounter(GuiGraphicsExtractor graphics) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NOTIFICATION_ICONS[Math.min(this.notificationCount, 6) - 1], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static enum LayoutState {
        LOADING,
        NO_REALMS,
        LIST;

    }

    @Environment(value=EnvType.CLIENT)
    private static interface RealmsCall<T> {
        public T request(RealmsClient var1) throws RealmsServiceException;
    }

    @Environment(value=EnvType.CLIENT)
    private class ServerEntry
    extends Entry {
        private static final Component ONLINE_PLAYERS_TOOLTIP_HEADER = Component.translatable("mco.onlinePlayers");
        private static final int PLAYERS_ONLINE_SPRITE_SIZE = 9;
        private static final int PLAYERS_ONLINE_SPRITE_SEPARATION = 3;
        private static final int SKIN_HEAD_LARGE_WIDTH = 36;
        private final RealmsServer serverData;
        private final WidgetTooltipHolder tooltip;
        final /* synthetic */ RealmsMainScreen this$0;

        public ServerEntry(RealmsMainScreen realmsMainScreen, RealmsServer serverData) {
            RealmsMainScreen realmsMainScreen2 = realmsMainScreen;
            Objects.requireNonNull(realmsMainScreen2);
            this.this$0 = realmsMainScreen2;
            super(realmsMainScreen);
            this.tooltip = new WidgetTooltipHolder();
            this.serverData = serverData;
            boolean selfOwnedServer = RealmsMainScreen.isSelfOwnedServer(serverData);
            if (RealmsMainScreen.isSnapshot() && selfOwnedServer && serverData.isSnapshotRealm()) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.paired", serverData.parentWorldName)));
            } else if (!selfOwnedServer && serverData.needsDowngrade()) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.friendsRealm.downgrade", serverData.activeVersion)));
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NEW_REALM_SPRITE, this.getContentX() - 5, this.getContentYMiddle() - 10, 40, 20);
                int textYPos = this.getContentYMiddle() - ((RealmsMainScreen)this.this$0).font.lineHeight / 2;
                graphics.text(this.this$0.font, SERVER_UNITIALIZED_TEXT, this.getContentX() + 40 - 2, textYPos, -8388737);
                return;
            }
            RealmsUtil.extractPlayerFace(graphics, this.getContentX(), this.getContentY(), 32, this.serverData.ownerUUID);
            this.extractFirstLine(graphics, this.getContentY(), this.getContentX(), this.getContentWidth(), -1, this.serverData);
            this.extractSecondLine(graphics, this.getContentY(), this.getContentX(), this.getContentWidth(), this.serverData);
            this.extractThirdLine(graphics, this.getContentY(), this.getContentX(), this.serverData);
            this.extractStatusLights(this.serverData, graphics, this.getContentRight(), this.getContentY(), mouseX, mouseY);
            boolean hasTooltip = this.extractOnlinePlayers(graphics, this.getContentY(), this.getContentX(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, a);
            if (!hasTooltip) {
                this.tooltip.refreshTooltipForNextRenderPass(graphics, mouseX, mouseY, hovered, this.isFocused(), new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight()));
            }
        }

        private boolean extractOnlinePlayers(GuiGraphicsExtractor graphics, int rowTop, int rowLeft, int rowWidth, int rowHeight, int mouseX, int mouseY, float a) {
            List<ResolvableProfile> profileResults = this.this$0.onlinePlayersPerRealm.getProfileResultsFor(this.serverData.id);
            int playerCount = profileResults.size();
            if (playerCount > 0) {
                int playersOnlineXEnd = rowLeft + rowWidth - 21;
                int playersOnlineY = rowTop + rowHeight - 9 - 2;
                int playerOnlineWidth = 9 * playerCount + 3 * (playerCount - 1);
                int playersOnlineXStart = playersOnlineXEnd - playerOnlineWidth;
                ArrayList<PlayerSkinRenderCache.RenderInfo> tooltipEntries = mouseX >= playersOnlineXStart && mouseX <= playersOnlineXEnd && mouseY >= playersOnlineY && mouseY <= playersOnlineY + 9 ? new ArrayList<PlayerSkinRenderCache.RenderInfo>(playerCount) : null;
                PlayerSkinRenderCache skinCache = this.this$0.minecraft.playerSkinRenderCache();
                for (int i = 0; i < profileResults.size(); ++i) {
                    ResolvableProfile profile = profileResults.get(i);
                    PlayerSkinRenderCache.RenderInfo profileRenderInfo = skinCache.getOrDefault(profile);
                    int xPos = playersOnlineXStart + 12 * i;
                    PlayerFaceExtractor.extractRenderState(graphics, profileRenderInfo.playerSkin(), xPos, playersOnlineY, 9);
                    if (tooltipEntries == null) continue;
                    tooltipEntries.add(profileRenderInfo);
                }
                if (tooltipEntries != null) {
                    graphics.setTooltipForNextFrame(this.this$0.font, List.of(ONLINE_PLAYERS_TOOLTIP_HEADER), Optional.of(new ClientActivePlayersTooltip.ActivePlayersTooltip(tooltipEntries)), mouseX, mouseY);
                    return true;
                }
            }
            return false;
        }

        private void playRealm() {
            this.this$0.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            RealmsMainScreen.play(this.serverData, this.this$0);
        }

        private void createUnitializedRealm() {
            this.this$0.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            RealmsCreateRealmScreen createScreen = new RealmsCreateRealmScreen(this.this$0, this.serverData, this.serverData.isSnapshotRealm());
            ((RealmsMainScreen)this.this$0).minecraft.gui.setScreen(createScreen);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                this.createUnitializedRealm();
            } else if (this.serverData.shouldPlayButtonBeActive() && doubleClick && this.isFocused()) {
                this.playRealm();
            }
            return true;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isSelection()) {
                if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                    this.createUnitializedRealm();
                    return true;
                }
                if (this.serverData.shouldPlayButtonBeActive()) {
                    this.playRealm();
                    return true;
                }
            }
            return super.keyPressed(event);
        }

        @Override
        public Component getNarration() {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                return UNITIALIZED_WORLD_NARRATION;
            }
            return Component.translatable("narrator.select", Objects.requireNonNullElse(this.serverData.name, "unknown server"));
        }

        public RealmsServer getServer() {
            return this.serverData;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private abstract class Entry
    extends ObjectSelectionList.Entry<Entry> {
        protected static final int STATUS_LIGHT_WIDTH = 10;
        private static final int STATUS_LIGHT_HEIGHT = 28;
        protected static final int PADDING_X = 7;
        protected static final int PADDING_Y = 2;
        final /* synthetic */ RealmsMainScreen this$0;

        private Entry(RealmsMainScreen realmsMainScreen) {
            RealmsMainScreen realmsMainScreen2 = realmsMainScreen;
            Objects.requireNonNull(realmsMainScreen2);
            this.this$0 = realmsMainScreen2;
        }

        protected void extractStatusLights(RealmsServer serverData, GuiGraphicsExtractor graphics, int rowRight, int rowTop, int mouseX, int mouseY) {
            int x = rowRight - 10 - 7;
            int y = rowTop + 2;
            if (serverData.expired) {
                this.extractRealmStatus(graphics, x, y, mouseX, mouseY, EXPIRED_SPRITE, () -> SERVER_EXPIRED_TOOLTIP);
            } else if (serverData.state == RealmsServer.State.CLOSED) {
                this.extractRealmStatus(graphics, x, y, mouseX, mouseY, CLOSED_SPRITE, () -> SERVER_CLOSED_TOOLTIP);
            } else if (RealmsMainScreen.isSelfOwnedServer(serverData) && serverData.daysLeft < 7) {
                this.extractRealmStatus(graphics, x, y, mouseX, mouseY, EXPIRES_SOON_SPRITE, () -> {
                    if (serverData.daysLeft <= 0) {
                        return SERVER_EXPIRES_SOON_TOOLTIP;
                    }
                    if (serverData.daysLeft == 1) {
                        return SERVER_EXPIRES_IN_DAY_TOOLTIP;
                    }
                    return Component.translatable("mco.selectServer.expires.days", serverData.daysLeft);
                });
            } else if (serverData.state == RealmsServer.State.OPEN) {
                this.extractRealmStatus(graphics, x, y, mouseX, mouseY, OPEN_SPRITE, () -> SERVER_OPEN_TOOLTIP);
            }
        }

        private void extractRealmStatus(GuiGraphicsExtractor graphics, int x, int y, int xm, int ym, Identifier sprite, Supplier<Component> tooltip) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 10, 28);
            if (this.this$0.realmSelectionList.isMouseOver(xm, ym) && xm >= x && xm <= x + 10 && ym >= y && ym <= y + 28) {
                graphics.setTooltipForNextFrame(tooltip.get(), xm, ym);
            }
        }

        protected void extractFirstLine(GuiGraphicsExtractor graphics, int rowTop, int rowLeft, int rowWidth, int serverNameColor, RealmsServer serverData) {
            int textX = this.textX(rowLeft);
            int firstLineY = this.firstLineY(rowTop);
            Component versionComponent = RealmsMainScreen.getVersionComponent(serverData.activeVersion, serverData.isCompatible());
            int versionTextX = this.versionTextX(rowLeft, rowWidth, versionComponent);
            this.extractClampedString(graphics, serverData.getName(), textX, firstLineY, versionTextX, serverNameColor);
            if (versionComponent != CommonComponents.EMPTY && !serverData.isMinigameActive()) {
                graphics.text(this.this$0.font, versionComponent, versionTextX, firstLineY, -8355712);
            }
        }

        protected void extractSecondLine(GuiGraphicsExtractor graphics, int rowTop, int rowLeft, int rowWidth, RealmsServer serverData) {
            int textX = this.textX(rowLeft);
            int firstLineY = this.firstLineY(rowTop);
            int secondLineY = this.secondLineY(firstLineY);
            String minigameName = serverData.getMinigameName();
            boolean minigameActive = serverData.isMinigameActive();
            if (minigameActive && minigameName != null) {
                MutableComponent minigameNameComponent = Component.literal(minigameName).withStyle(ChatFormatting.GRAY);
                graphics.text(this.this$0.font, Component.translatable("mco.selectServer.minigameName", minigameNameComponent).withColor(-171), textX, secondLineY, -1);
            } else {
                int maxX = this.extractGameMode(serverData, graphics, rowLeft, rowWidth, firstLineY);
                this.extractClampedString(graphics, serverData.getDescription(), textX, this.secondLineY(firstLineY), maxX, -8355712);
            }
        }

        protected void extractThirdLine(GuiGraphicsExtractor graphics, int rowTop, int rowLeft, RealmsServer server) {
            int textX = this.textX(rowLeft);
            int firstLineY = this.firstLineY(rowTop);
            int thirdLineY = this.thirdLineY(firstLineY);
            if (!RealmsMainScreen.isSelfOwnedServer(server)) {
                graphics.text(this.this$0.font, server.owner, textX, this.thirdLineY(firstLineY), -8355712);
            } else if (server.expired) {
                Component expirationText = server.expiredTrial ? TRIAL_EXPIRED_TEXT : SUBSCRIPTION_EXPIRED_TEXT;
                graphics.text(this.this$0.font, expirationText, textX, thirdLineY, -2142128);
            }
        }

        protected void extractClampedString(GuiGraphicsExtractor graphics, @Nullable String string, int x, int y, int maxX, int color) {
            if (string == null) {
                return;
            }
            int availableSpace = maxX - x;
            if (this.this$0.font.width(string) > availableSpace) {
                String clampedName = this.this$0.font.plainSubstrByWidth(string, availableSpace - this.this$0.font.width("... "));
                graphics.text(this.this$0.font, clampedName + "...", x, y, color);
            } else {
                graphics.text(this.this$0.font, string, x, y, color);
            }
        }

        protected int versionTextX(int rowLeft, int rowWidth, Component versionComponent) {
            return rowLeft + rowWidth - this.this$0.font.width(versionComponent) - 20;
        }

        protected int gameModeTextX(int rowLeft, int rowWidth, Component versionComponent) {
            return rowLeft + rowWidth - this.this$0.font.width(versionComponent) - 20;
        }

        protected int extractGameMode(RealmsServer server, GuiGraphicsExtractor graphics, int rowLeft, int rowWidth, int firstLineY) {
            boolean hardcore = server.isHardcore;
            int gameMode = server.gameMode;
            int x = rowLeft;
            if (GameType.isValidId(gameMode)) {
                Component gameModeComponent = RealmsMainScreen.getGameModeComponent(gameMode, hardcore);
                x = this.gameModeTextX(rowLeft, rowWidth, gameModeComponent);
                graphics.text(this.this$0.font, gameModeComponent, x, this.secondLineY(firstLineY), -8355712);
            }
            if (hardcore) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HARDCORE_MODE_SPRITE, x -= 10, this.secondLineY(firstLineY), 8, 8);
            }
            return x;
        }

        protected int firstLineY(int rowTop) {
            return rowTop + 1;
        }

        protected int lineHeight() {
            return 2 + ((RealmsMainScreen)this.this$0).font.lineHeight;
        }

        protected int textX(int rowLeft) {
            return rowLeft + 36 + 2;
        }

        protected int secondLineY(int firstLineY) {
            return firstLineY + this.lineHeight();
        }

        protected int thirdLineY(int firstLineY) {
            return firstLineY + this.lineHeight() * 2;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class CrossButton
    extends ImageButton {
        private static final WidgetSprites SPRITES = new WidgetSprites(Identifier.withDefaultNamespace("widget/cross_button"), Identifier.withDefaultNamespace("widget/cross_button_highlighted"));

        protected CrossButton(Button.OnPress onPress, Component tooltip) {
            super(0, 0, 14, 14, SPRITES, onPress);
            this.setTooltip(Tooltip.create(tooltip));
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class ParentEntry
    extends Entry {
        private final RealmsServer server;
        private final WidgetTooltipHolder tooltip;

        public ParentEntry(RealmsMainScreen realmsMainScreen, RealmsServer server) {
            Objects.requireNonNull(realmsMainScreen);
            super(realmsMainScreen);
            this.tooltip = new WidgetTooltipHolder();
            this.server = server;
            if (!server.expired) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.parent.tooltip")));
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.extractStatusLights(this.server, graphics, this.getContentRight(), this.getContentY(), mouseX, mouseY);
            RealmsUtil.extractPlayerFace(graphics, this.getContentX(), this.getContentY(), 32, this.server.ownerUUID);
            this.extractFirstLine(graphics, this.getContentY(), this.getContentX(), this.getContentWidth(), -8355712, this.server);
            this.extractSecondLine(graphics, this.getContentY(), this.getContentX(), this.getContentWidth(), this.server);
            this.extractThirdLine(graphics, this.getContentY(), this.getContentX(), this.server);
            this.tooltip.refreshTooltipForNextRenderPass(graphics, mouseX, mouseY, hovered, this.isFocused(), new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight()));
        }

        @Override
        public Component getNarration() {
            return Component.literal(Objects.requireNonNullElse(this.server.name, "unknown server"));
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class AvailableSnapshotEntry
    extends Entry {
        private static final Component START_SNAPSHOT_REALM = Component.translatable("mco.snapshot.start");
        private static final int TEXT_PADDING = 5;
        private final WidgetTooltipHolder tooltip;
        private final RealmsServer parent;
        final /* synthetic */ RealmsMainScreen this$0;

        public AvailableSnapshotEntry(RealmsMainScreen realmsMainScreen, RealmsServer parent) {
            RealmsMainScreen realmsMainScreen2 = realmsMainScreen;
            Objects.requireNonNull(realmsMainScreen2);
            this.this$0 = realmsMainScreen2;
            super(realmsMainScreen);
            this.tooltip = new WidgetTooltipHolder();
            this.parent = parent;
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.tooltip")));
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NEW_REALM_SPRITE, this.getContentX() - 5, this.getContentYMiddle() - 10, 40, 20);
            int textYPos = this.getContentYMiddle() - ((RealmsMainScreen)this.this$0).font.lineHeight / 2;
            graphics.text(this.this$0.font, START_SNAPSHOT_REALM, this.getContentX() + 40 - 2, textYPos - 5, -8388737);
            graphics.text(this.this$0.font, Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server")), this.getContentX() + 40 - 2, textYPos + 5, -8355712);
            this.tooltip.refreshTooltipForNextRenderPass(graphics, mouseX, mouseY, hovered, this.isFocused() && ((RealmsMainScreen)this.this$0).minecraft.gui.screen() == this.this$0, new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight()));
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            this.addSnapshotRealm();
            return true;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isSelection()) {
                this.addSnapshotRealm();
                return false;
            }
            return super.keyPressed(event);
        }

        private void addSnapshotRealm() {
            this.this$0.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            ((RealmsMainScreen)this.this$0).minecraft.gui.setScreen(new PopupScreen.Builder(this.this$0, Component.translatable("mco.snapshot.createSnapshotPopup.title")).addMessage(Component.translatable("mco.snapshot.createSnapshotPopup.text")).addButton(Component.translatable("mco.selectServer.create"), popupScreen -> ((RealmsMainScreen)this.this$0).minecraft.gui.setScreen(new RealmsCreateRealmScreen(this.this$0, this.parent, true))).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build());
        }

        @Override
        public Component getNarration() {
            return Component.translatable("gui.narrate.button", CommonComponents.joinForNarration(START_SNAPSHOT_REALM, Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server"))));
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class NotificationMessageEntry
    extends Entry {
        private static final int SIDE_MARGINS = 40;
        public static final int PADDING = 7;
        public static final int HEIGHT_WITHOUT_TEXT = 38;
        private final Component text;
        private final List<AbstractWidget> children;
        private final @Nullable CrossButton dismissButton;
        private final MultiLineTextWidget textWidget;
        private final GridLayout gridLayout;
        private final FrameLayout textFrame;
        private final Button button;
        private int lastEntryWidth;

        public NotificationMessageEntry(RealmsMainScreen realmsMainScreen, RealmsMainScreen realmsMainScreen2, int messageHeight, Component text, RealmsNotification.VisitUrl notification) {
            Objects.requireNonNull(realmsMainScreen);
            super(realmsMainScreen);
            this.children = new ArrayList<AbstractWidget>();
            this.lastEntryWidth = -1;
            this.text = text;
            this.gridLayout = new GridLayout();
            this.gridLayout.addChild(ImageWidget.sprite(20, 20, INFO_SPRITE), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
            this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
            this.textFrame = this.gridLayout.addChild(new FrameLayout(0, messageHeight), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
            this.textWidget = this.textFrame.addChild(new MultiLineTextWidget(text, realmsMainScreen.font).setCentered(true), this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop());
            this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
            this.dismissButton = notification.dismissable() ? this.gridLayout.addChild(new CrossButton(b -> realmsMainScreen.dismissNotification(notification.uuid()), Component.translatable("mco.notification.dismiss")), 0, 2, this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0)) : null;
            this.button = this.gridLayout.addChild(notification.buildOpenLinkButton(realmsMainScreen2), 1, 1, this.gridLayout.newCellSettings().alignHorizontallyCenter().padding(4));
            NotificationMessageEntry notificationMessageEntry = this;
            this.button.setOverrideRenderHighlightedSprite(() -> notificationMessageEntry.isFocused());
            this.gridLayout.visitWidgets(this.children::add);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (this.button.keyPressed(event)) {
                return true;
            }
            if (this.dismissButton != null && this.dismissButton.keyPressed(event)) {
                return true;
            }
            return super.keyPressed(event);
        }

        private void updateEntryWidth() {
            int entryWidth = this.getWidth();
            if (this.lastEntryWidth != entryWidth) {
                this.refreshLayout(entryWidth);
                this.lastEntryWidth = entryWidth;
            }
        }

        private void refreshLayout(int entryWidth) {
            int width = NotificationMessageEntry.textWidth(entryWidth);
            this.textFrame.setMinWidth(width);
            this.textWidget.setMaxWidth(width);
            this.gridLayout.arrangeElements();
        }

        public static int textWidth(int rowWidth) {
            return rowWidth - 80;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.gridLayout.setPosition(this.getContentX(), this.getContentY());
            this.updateEntryWidth();
            this.children.forEach(child -> child.extractRenderState(graphics, mouseX, mouseY, a));
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.dismissButton != null && this.dismissButton.mouseClicked(event, doubleClick)) {
                return true;
            }
            if (this.button.mouseClicked(event, doubleClick)) {
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        public Component getText() {
            return this.text;
        }

        @Override
        public Component getNarration() {
            return this.getText();
        }
    }
}

