/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.yggdrasil.response.PresenceResponse
 *  com.mojang.authlib.yggdrasil.response.PresenceStatusDto
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens.friends;

import com.mojang.authlib.yggdrasil.response.PresenceResponse;
import com.mojang.authlib.yggdrasil.response.PresenceStatusDto;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.friends.AddFriendWidget;
import net.minecraft.client.gui.screens.friends.FriendEntry;
import net.minecraft.client.gui.screens.friends.FriendsOverlayTabButton;
import net.minecraft.client.gui.screens.friends.FriendsTab;
import net.minecraft.client.gui.screens.friends.IncomingEntry;
import net.minecraft.client.gui.screens.friends.OutgoingEntry;
import net.minecraft.client.gui.screens.friends.PendingTab;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class FriendsOverlayScreen
extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("gui.friends.open");
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("friends/background");
    private static final Component LOADING_FRIENDS = Component.translatable("gui.friends.loading_friends");
    private static final Component LOADING_REQUESTS = Component.translatable("gui.friends.loading_requests");
    private static final Component ERROR_UPGRADE_NEEDED = Component.translatable("gui.friends.error.upgrade_needed");
    private static final Component ERROR_CONNECTION_ISSUE = Component.translatable("gui.friends.error.connection_issue");
    private static final Component ERROR_TEMPORARY_UNAVAILABLE = Component.translatable("gui.friends.error.temporary_unavailable");
    private static final Component ERROR_USER_MAY_LACK_ACTIVE_PROFILE = Component.translatable("gui.friends.error.user_may_lack_active_profile");
    private static final Component ERROR_UNAUTHORIZED = Component.translatable("gui.friends.error.unauthorized");
    private static final Component ERROR_GENERIC = Component.translatable("gui.friends.error.generic");
    private static final Component ERROR_TOAST_GENERIC = Component.translatable("gui.friends.toast.generic_error");
    private static final int BG_BORDER_WIDTH = 8;
    private static final int OVERLAY_WIDTH = 220;
    public static final int TAB_BUTTON_WIDTH = 110;
    private static final int TAB_BUTTON_HEIGHT = 20;
    private final @Nullable Screen backgroundScreen;
    private @Nullable FriendsTab friendsTab;
    private @Nullable PendingTab pendingTab;
    private @Nullable TabNavigationBar tabNavigationBar;
    private @Nullable LinearLayout layout;
    private @Nullable LinearLayout contentLayout;
    private @Nullable FriendsOverlayTabButton pendingTabButton;
    private final TabManager tabManager;
    private final Runnable friendListUpdateListener = this::onFriendListUpdate;
    private final Set<UUID> pendingFriendRemovals = new HashSet<UUID>();

    public FriendsOverlayScreen(@Nullable Screen backgroundScreen) {
        super(TITLE);
        this.backgroundScreen = backgroundScreen;
        FriendsOverlayScreen friendsOverlayScreen = this;
        Consumer<AbstractWidget> consumer = x$0 -> friendsOverlayScreen.addRenderableWidget(x$0);
        friendsOverlayScreen = this;
        this.tabManager = new TabManager(consumer, x$0 -> friendsOverlayScreen.removeWidget((GuiEventListener)x$0), this::selectTab, this::deselectTab);
    }

    @Override
    public void added() {
        super.added();
        if (this.backgroundScreen != null) {
            this.backgroundScreen.clearFocus();
        }
        this.minecraft.getPlayerSocialManager().addFriendListUpdateListener(this.friendListUpdateListener);
    }

    @Override
    public void removed() {
        this.minecraft.getPlayerSocialManager().removeFriendListUpdateListener(this.friendListUpdateListener);
        super.removed();
    }

    private void onFriendListUpdate() {
        if (this.minecraft.gui.screen() != this || !this.minecraft.getPlayerSocialManager().isFriendListEnabled()) {
            return;
        }
        this.refreshLists();
    }

    @Override
    protected void init() {
        if (this.backgroundScreen != null) {
            this.backgroundScreen.init(this.width, this.height);
        }
        this.layout = LinearLayout.vertical();
        int scrollableMaxHeight = this.height - 80;
        this.friendsTab = new FriendsTab(this.minecraft, new LoadingDotsWidget(this.font, LOADING_FRIENDS), this, 220, scrollableMaxHeight);
        this.pendingTab = new PendingTab(this.minecraft, new LoadingDotsWidget(this.font, LOADING_REQUESTS), this, 220, scrollableMaxHeight);
        this.pendingTabButton = new FriendsOverlayTabButton(this.tabManager, this.pendingTab, 110, 20);
        this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, 0, 0, 220, 20).addTab(new FriendsOverlayTabButton(this.tabManager, this.friendsTab, 110, 20), this.friendsTab).addTab(this.pendingTabButton, this.pendingTab).build();
        this.addRenderableWidget(this.tabNavigationBar);
        this.contentLayout = this.layout.addChild(LinearLayout.vertical());
        this.tabManager.setCurrentTab(this.friendsTab, false, false);
        FriendsOverlayScreen friendsOverlayScreen = this;
        this.layout.visitWidgets(x$0 -> friendsOverlayScreen.addRenderableWidget(x$0));
        this.repositionElements();
        this.refreshLists();
    }

    public int getOverlayWidth() {
        return 220;
    }

    @Override
    protected void repositionElements() {
        if (this.backgroundScreen != null) {
            this.backgroundScreen.resize(this.width, this.height);
        }
        this.friendsTab.setHeight(this.height - 80);
        this.pendingTab.setHeight(this.height - 80);
        this.layout.arrangeElements();
        FrameLayout.alignInRectangle(this.layout, this.getRectangle(), 0.5f, 0.5f);
        this.tabNavigationBar.setPosition(this.layout.getX(), this.layout.getY() - 20 - 7);
        this.tabNavigationBar.arrangeElements(this.width);
    }

    @Override
    public void tick() {
        super.tick();
        this.minecraft.getPlayerSocialManager().getPresenceHandler().tryUpdatePresence();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.backgroundScreen != null) {
            this.backgroundScreen.extractBackground(graphics, mouseX, mouseY, a);
            graphics.nextStratum();
            this.backgroundScreen.extractRenderState(graphics, -1, -1, a);
            graphics.nextStratum();
            this.extractBlurredBackground(graphics);
        } else {
            super.extractBackground(graphics, mouseX, mouseY, a);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, this.layout.getX() - 8, this.layout.getY() - 8, this.layout.getWidth() + 16, this.layout.getHeight() + 16 + 1);
    }

    private void selectTab(Tab tab) {
        if (this.contentLayout != null) {
            this.contentLayout.addChild(tab.getLayout());
            this.repositionElements();
        }
    }

    private void deselectTab(Tab tab) {
        if (this.contentLayout != null) {
            this.contentLayout.removeChildren();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int panelLeft = this.layout.getX() - 8;
        int panelRight = this.layout.getX() + this.layout.getWidth() + 8;
        int panelTop = this.tabNavigationBar.getY();
        int panelBottom = this.layout.getY() + this.layout.getHeight() + 16;
        if (event.x() < (double)panelLeft || event.x() > (double)panelRight || event.y() < (double)panelTop || event.y() > (double)panelBottom) {
            this.minecraft.gui.setScreen(this.backgroundScreen);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        AddFriendWidget addFriendWidget;
        GuiEventListener guiEventListener;
        if (!(!this.minecraft.options.keyFriends.matches(event) || (guiEventListener = this.getFocused()) instanceof AddFriendWidget && (addFriendWidget = (AddFriendWidget)guiEventListener).getEditBox().isFocused())) {
            this.onClose();
            return true;
        }
        if (this.tabNavigationBar != null && this.tabNavigationBar.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.backgroundScreen);
    }

    public void refreshLists() {
        PlayerSocialManager playerSocialManager = this.minecraft.getPlayerSocialManager();
        RemoteFriendListUpdateHandler.State state = playerSocialManager.getFriendListState();
        switch (state) {
            case LOADING: {
                this.friendsTab.showLoading();
                this.pendingTab.showLoading();
                break;
            }
            case UPGRADE_NEEDED: {
                this.showError(ERROR_UPGRADE_NEEDED);
                break;
            }
            case CONNECTION_ISSUE: {
                this.showError(ERROR_CONNECTION_ISSUE);
                break;
            }
            case TEMPORARY_UNAVAILABLE: {
                this.showError(ERROR_TEMPORARY_UNAVAILABLE);
                break;
            }
            case USER_MAY_LACK_ACTIVE_PROFILE: {
                this.showError(ERROR_USER_MAY_LACK_ACTIVE_PROFILE);
                break;
            }
            case UNAUTHORIZED: {
                this.showError(ERROR_UNAUTHORIZED);
                break;
            }
            case GENERIC_ERROR: {
                this.showError(ERROR_GENERIC);
                break;
            }
            case SUCCESS: {
                this.populateLists(playerSocialManager);
            }
        }
        if (this.layout != null) {
            this.layout.arrangeElements();
            FrameLayout.alignInRectangle(this.layout, this.getRectangle(), 0.5f, 0.5f);
        }
    }

    public void applyPresenceUpdate() {
        if (this.friendsTab != null) {
            this.friendsTab.applyPresenceUpdate(this.minecraft.getPlayerSocialManager().getPresenceHandler().getLatestPresence());
        }
    }

    private void showError(Component message) {
        this.friendsTab.showError(message);
        this.pendingTab.showError(message);
    }

    private void populateLists(PlayerSocialManager playerSocialManager) {
        int totalRequestsCount;
        List<PlayerSocialManager.PlayerData> friends = playerSocialManager.getFriends();
        List<PlayerSocialManager.PlayerData> incomingRequests = playerSocialManager.getIncomingRequests();
        List<PlayerSocialManager.PlayerData> outgoingRequests = playerSocialManager.getOutgoingRequests();
        PresenceResponse latestPresence = this.minecraft.getPlayerSocialManager().getPresenceHandler().getLatestPresence();
        if (friends.isEmpty()) {
            this.friendsTab.showEmpty();
        } else {
            ArrayList<FriendEntry> entries = new ArrayList<FriendEntry>(friends.size());
            for (PlayerSocialManager.PlayerData friend : friends) {
                PresenceStatusDto presence = null;
                for (PresenceStatusDto presenceStatusDto : latestPresence.presence()) {
                    if (!presenceStatusDto.profileId().equals(friend.id())) continue;
                    presence = presenceStatusDto;
                    break;
                }
                UUID friendId = friend.id();
                boolean removalPending = this.pendingFriendRemovals.contains(friendId);
                entries.add(new FriendEntry(this.minecraft, this, friend, presence, removalPending, () -> {
                    this.pendingFriendRemovals.add(friendId);
                    ((CompletableFuture)((CompletableFuture)this.minecraft.getPlayerSocialManager().removeFriend(friendId).whenCompleteAsync((resultCode, throwable) -> this.pendingFriendRemovals.remove(friendId), (Executor)this.minecraft)).thenAcceptAsync(resultCode -> this.refreshLists(), (Executor)this.minecraft)).exceptionally(this::onActionFailed);
                }));
            }
            entries.sort(Comparator.comparingInt(FriendEntry::presenceStatusSortOrder));
            this.friendsTab.updateEntries(entries);
        }
        int incomingRequestsCount = incomingRequests.size();
        if (this.pendingTabButton != null) {
            this.pendingTabButton.setMessage(Component.translatable("gui.friends.requests_count", incomingRequestsCount));
        }
        if ((totalRequestsCount = incomingRequestsCount + outgoingRequests.size()) == 0) {
            this.pendingTab.showEmpty();
            return;
        }
        ArrayList<IncomingEntry> incomingEntries = new ArrayList<IncomingEntry>(incomingRequests.size());
        if (!incomingRequests.isEmpty()) {
            for (PlayerSocialManager.PlayerData incomingRequest : incomingRequests) {
                incomingEntries.add(new IncomingEntry(this.minecraft, this, incomingRequest, () -> ((CompletableFuture)this.minecraft.getPlayerSocialManager().acceptIncomingFriendRequest(incomingRequest.id()).thenAcceptAsync(resultCode -> this.refreshLists(), (Executor)this.minecraft)).exceptionally(this::onActionFailed), () -> ((CompletableFuture)this.minecraft.getPlayerSocialManager().declineIncomingFriendRequest(incomingRequest.id()).thenRunAsync(this::refreshLists, this.minecraft)).exceptionally(this::onActionFailed)));
            }
        }
        ArrayList<OutgoingEntry> outgoingEntries = new ArrayList<OutgoingEntry>(outgoingRequests.size());
        if (!outgoingRequests.isEmpty()) {
            for (PlayerSocialManager.PlayerData outgoingRequest : outgoingRequests) {
                outgoingEntries.add(new OutgoingEntry(this.minecraft, this, outgoingRequest, () -> ((CompletableFuture)this.minecraft.getPlayerSocialManager().revokeOutgoingFriendRequest(outgoingRequest.id()).thenRunAsync(this::refreshLists, this.minecraft)).exceptionally(this::onActionFailed)));
            }
        }
        this.pendingTab.updateEntries(incomingEntries, outgoingEntries);
        if (this.layout != null) {
            this.layout.arrangeElements();
            FrameLayout.alignInRectangle(this.layout, this.getRectangle(), 0.5f, 0.5f);
        }
    }

    void startFriendAction() {
        this.friendsTab.disable();
        this.pendingTab.disable();
    }

    private @Nullable Void onActionFailed(Throwable ex) {
        LOGGER.error("Friend action failed", ex);
        this.minecraft.execute(() -> {
            SystemToast.addOrUpdate(this.minecraft.gui.toastManager(), SystemToast.SystemToastId.FRIEND_SYSTEM_NOTIFICATION, ERROR_TOAST_GENERIC, null);
            this.refreshLists();
        });
        return null;
    }
}

