/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.yggdrasil.response.PresenceResponse
 *  com.mojang.authlib.yggdrasil.response.PresenceStatusDto
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.friends;

import com.mojang.authlib.yggdrasil.response.PresenceResponse;
import com.mojang.authlib.yggdrasil.response.PresenceStatusDto;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.PrivacyConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.friends.AbstractFriendsTab;
import net.minecraft.client.gui.screens.friends.AddFriendWidget;
import net.minecraft.client.gui.screens.friends.FriendEntry;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonLinks;

@Environment(value=EnvType.CLIENT)
class FriendsTab
extends AbstractFriendsTab {
    private static final Component TAB_TITLE = Component.translatable("gui.friends.tab_friends");
    private static final Component MICROSOFT_ACCOUNT_LINK = Component.translatable("gui.friends.empty_state.link").withStyle(style -> style.withUnderlined(true).withColor(ChatFormatting.GRAY).withClickEvent(new ClickEvent.OpenUrl(CommonLinks.PRIVACY_AND_ONLINE_SETTINGS)));
    private static final Component EMPTY_STATE = Component.translatable("gui.friends.empty_state", MICROSOFT_ACCOUNT_LINK).withStyle(ChatFormatting.GRAY);
    private static final Component MANAGE_ACCOUNT_FOOTER = Component.translatable("gui.friends.manage_account_footer", MICROSOFT_ACCOUNT_LINK).withStyle(ChatFormatting.GRAY);
    private static final Identifier ILLUSTRATION = Identifier.withDefaultNamespace("friends/illustrations_00");
    private final FriendsOverlayScreen screen;
    private final LinearLayout layout;
    private final LinearLayout friendScrollableContent;
    private final LoadingDotsWidget loadingDotsWidget;
    private final AddFriendWidget addFriendWidget;
    private final ScrollableLayout scrollableLayout;

    FriendsTab(Minecraft minecraft, LoadingDotsWidget loadingDotsWidget, FriendsOverlayScreen screen, int width, int height) {
        super(width, height);
        this.screen = screen;
        this.layout = LinearLayout.vertical();
        this.layout.defaultCellSetting().alignHorizontallyCenter();
        this.loadingDotsWidget = loadingDotsWidget;
        this.addFriendWidget = new AddFriendWidget(width, this::onSendFriendRequestFinished);
        this.layout.addChild(this.addFriendWidget);
        this.friendScrollableContent = LinearLayout.vertical();
        this.friendScrollableContent.defaultCellSetting();
        this.scrollableLayout = new ScrollableLayout(minecraft, this.friendScrollableContent, height - this.addFriendWidget.contentHeight(), ScrollableLayout.ReserveStrategy.BOTH);
        this.scrollableLayout.setScrollbarSpacing(2);
        this.layout.addChild(this.scrollableLayout);
        this.rearrangeElements();
    }

    public void showLoading() {
        this.friendScrollableContent.removeChildren();
        this.friendScrollableContent.addChild(this.createCenteredFrame(this.loadingDotsWidget, this.getListContentWidth(), this.height - this.addFriendWidget.contentHeight()));
        this.addFriendWidget.applyState(AddFriendWidget.State.SENDING);
    }

    public void showError(Component message) {
        this.friendScrollableContent.removeChildren();
        int maxWidth = this.getListContentWidth();
        MultiLineTextWidget text = this.createCenteredText(message.copy().withStyle(ChatFormatting.GRAY), this.screen.getFont(), maxWidth);
        this.friendScrollableContent.addChild(this.createCenteredFrame(text, maxWidth, this.height - this.addFriendWidget.contentHeight()));
        this.addFriendWidget.applyState(AddFriendWidget.State.DISABLED);
    }

    public void showEmpty() {
        this.friendScrollableContent.removeChildren();
        LinearLayout content = new LinearLayout(0, 0, LinearLayout.Orientation.VERTICAL).spacing(8);
        content.defaultCellSetting().alignHorizontallyCenter().alignVerticallyMiddle();
        content.addChild(ImageWidget.sprite(128, 48, ILLUSTRATION));
        int maxWidth = this.getListContentWidth();
        MultiLineTextWidget textWidget = this.createCenteredText(EMPTY_STATE, this.screen.getFont(), maxWidth);
        textWidget.setComponentClickHandler(style -> {
            ClickEvent patt1$temp = style.getClickEvent();
            if (patt1$temp instanceof ClickEvent.OpenUrl) {
                URI uri;
                ClickEvent.OpenUrl $b$0 = (ClickEvent.OpenUrl)patt1$temp;
                try {
                    URI patt2$temp;
                    uri = patt2$temp = $b$0.uri();
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
                PrivacyConfirmLinkScreen.confirmLinkNow((Screen)this.screen, uri);
            }
        });
        content.addChild(textWidget);
        int frameHeight = this.scrollableLayout.getHeight();
        this.friendScrollableContent.addChild(this.createCenteredFrame(content, maxWidth, frameHeight));
        this.addFriendWidget.applyState(this.addFriendWidget.getValue().isEmpty() ? AddFriendWidget.State.EMPTY_INPUT : AddFriendWidget.State.READY);
    }

    @Override
    void rearrangeElements() {
        this.scrollableLayout.setMinHeight(this.height - this.addFriendWidget.contentHeight());
        this.scrollableLayout.setMaxHeight(this.height - this.addFriendWidget.contentHeight());
    }

    private void onSendFriendRequestFinished() {
        this.screen.refreshLists();
    }

    @Override
    public Component getTabTitle() {
        return TAB_TITLE;
    }

    @Override
    public Component getTabExtraNarration() {
        return Component.empty();
    }

    @Override
    public void visitChildren(Consumer<AbstractWidget> childrenConsumer) {
        this.layout.visitWidgets(childrenConsumer);
    }

    @Override
    public void doLayout(ScreenRectangle screenRectangle) {
        this.layout.arrangeElements();
        FrameLayout.alignInRectangle(this.layout, screenRectangle, 0.5f, 0.16666667f);
    }

    @Override
    public Layout getLayout() {
        return this.layout;
    }

    void updateEntries(List<FriendEntry> friendEntries) {
        this.friendScrollableContent.removeChildren();
        friendEntries.forEach(this.friendScrollableContent::addChild);
        this.friendScrollableContent.addChild(this.createManageAccountFooter());
    }

    void applyPresenceUpdate(PresenceResponse latestPresence) {
        this.friendScrollableContent.visitWidgets(widget -> {
            if (widget instanceof FriendEntry) {
                FriendEntry entry = (FriendEntry)widget;
                PresenceStatusDto newPresenceStatus = null;
                for (PresenceStatusDto presenceStatus : latestPresence.presence()) {
                    if (!presenceStatus.profileId().equals(entry.playerId())) continue;
                    newPresenceStatus = presenceStatus;
                    break;
                }
                entry.applyPresence(newPresenceStatus);
            }
        });
    }

    private FrameLayout createManageAccountFooter() {
        int maxWidth = this.getListContentWidth();
        MultiLineTextWidget textWidget = this.createCenteredText(MANAGE_ACCOUNT_FOOTER, this.screen.getFont(), maxWidth);
        textWidget.setComponentClickHandler(style -> {
            ClickEvent patt1$temp = style.getClickEvent();
            if (patt1$temp instanceof ClickEvent.OpenUrl) {
                URI uri;
                ClickEvent.OpenUrl $b$0 = (ClickEvent.OpenUrl)patt1$temp;
                try {
                    URI patt2$temp;
                    uri = patt2$temp = $b$0.uri();
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
                AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                PrivacyConfirmLinkScreen.confirmLinkNow((Screen)this.screen, uri);
            }
        });
        FrameLayout frame = new FrameLayout(maxWidth, textWidget.getHeight());
        frame.defaultChildLayoutSetting().alignHorizontallyCenter().alignVerticallyMiddle();
        frame.addChild(textWidget);
        return frame;
    }

    @Override
    protected Layout entriesContainer() {
        return this.friendScrollableContent;
    }
}

