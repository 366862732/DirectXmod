/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.gui.screens.configuration;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.gui.screens.RealmsConfirmScreen;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigurationTab;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.configuration.RealmsInviteScreen;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsPlayersTab
extends GridLayoutTab
implements RealmsConfigurationTab {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Component TITLE = Component.translatable("mco.configure.world.players.title");
    private static final Component QUESTION_TITLE = Component.translatable("mco.question");
    private static final int PADDING = 8;
    private final RealmsConfigureWorldScreen configurationScreen;
    private final Minecraft minecraft;
    private final Font font;
    private RealmsServer serverData;
    private final InvitedObjectSelectionList invitedList;

    public RealmsPlayersTab(RealmsConfigureWorldScreen configurationScreen, Minecraft minecraft, RealmsServer serverData) {
        super(TITLE);
        this.configurationScreen = configurationScreen;
        this.minecraft = minecraft;
        this.font = configurationScreen.getFont();
        this.serverData = serverData;
        GridLayout.RowHelper helper = this.layout.spacing(8).createRowHelper(1);
        this.invitedList = helper.addChild(new InvitedObjectSelectionList(this, configurationScreen.width, this.calculateListHeight()), LayoutSettings.defaults().alignVerticallyTop().alignHorizontallyCenter());
        helper.addChild(Button.builder(Component.translatable("mco.configure.world.buttons.invite"), button -> minecraft.gui.setScreen(new RealmsInviteScreen(configurationScreen, serverData))).build(), LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyCenter());
        this.updateData(serverData);
    }

    public int calculateListHeight() {
        return this.configurationScreen.getContentHeight() - 20 - 16;
    }

    @Override
    public void doLayout(ScreenRectangle screenRectangle) {
        super.doLayout(screenRectangle);
        this.invitedList.updateSizeAndPosition(this.configurationScreen.width, this.calculateListHeight(), this.invitedList.getX(), this.invitedList.getY());
    }

    @Override
    public void updateData(RealmsServer serverData) {
        this.serverData = serverData;
        this.invitedList.updateList(serverData);
    }

    @Environment(value=EnvType.CLIENT)
    private class InvitedObjectSelectionList
    extends ContainerObjectSelectionList<Entry> {
        private static final int PLAYER_ENTRY_HEIGHT = 36;
        final /* synthetic */ RealmsPlayersTab this$0;

        public InvitedObjectSelectionList(RealmsPlayersTab realmsPlayersTab, int width, int height) {
            RealmsPlayersTab realmsPlayersTab2 = realmsPlayersTab;
            Objects.requireNonNull(realmsPlayersTab2);
            this.this$0 = realmsPlayersTab2;
            super(Minecraft.getInstance(), width, height, realmsPlayersTab.configurationScreen.getHeaderHeight(), 36);
        }

        private void updateList(RealmsServer serverData) {
            this.clearEntries();
            this.populateList(serverData);
        }

        private void populateList(RealmsServer serverData) {
            HeaderEntry entry = new HeaderEntry(this.this$0);
            this.addEntry(entry, entry.height(this.this$0.font.lineHeight));
            for (PlayerEntry newChild : serverData.players.stream().map(x$0 -> new PlayerEntry(this.this$0, (PlayerInfo)x$0)).toList()) {
                this.addEntry(newChild);
            }
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class HeaderEntry
    extends Entry {
        private String cachedNumberOfInvites;
        private final FocusableTextWidget invitedWidget;
        final /* synthetic */ RealmsPlayersTab this$0;

        public HeaderEntry(RealmsPlayersTab realmsPlayersTab) {
            RealmsPlayersTab realmsPlayersTab2 = realmsPlayersTab;
            Objects.requireNonNull(realmsPlayersTab2);
            this.this$0 = realmsPlayersTab2;
            this.cachedNumberOfInvites = "";
            MutableComponent invitedText = Component.translatable("mco.configure.world.invited.number", "").withStyle(ChatFormatting.UNDERLINE);
            this.invitedWidget = FocusableTextWidget.builder(invitedText, realmsPlayersTab.font).alwaysShowBorder(false).backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS).build();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            String numberOfInvites;
            String string = numberOfInvites = this.this$0.serverData.players != null ? Integer.toString(this.this$0.serverData.players.size()) : "0";
            if (!numberOfInvites.equals(this.cachedNumberOfInvites)) {
                this.cachedNumberOfInvites = numberOfInvites;
                MutableComponent invitedComponent = Component.translatable("mco.configure.world.invited.number", numberOfInvites).withStyle(ChatFormatting.UNDERLINE);
                this.invitedWidget.setMessage(invitedComponent);
            }
            this.invitedWidget.setPosition(this.this$0.invitedList.getRowLeft() + this.this$0.invitedList.getRowWidth() / 2 - this.invitedWidget.getWidth() / 2, this.getY() + this.getHeight() / 2 - this.invitedWidget.getHeight() / 2);
            this.invitedWidget.extractRenderState(graphics, mouseX, mouseY, a);
        }

        private int height(int lineHeight) {
            return lineHeight + this.invitedWidget.getPadding() * 2;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.invitedWidget);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.invitedWidget);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class PlayerEntry
    extends Entry {
        protected static final int SKIN_FACE_SIZE = 32;
        private static final Component NORMAL_USER_TEXT = Component.translatable("mco.configure.world.invites.normal.tooltip");
        private static final Component OP_TEXT = Component.translatable("mco.configure.world.invites.ops.tooltip");
        private static final Component REMOVE_TEXT = Component.translatable("mco.configure.world.invites.remove.tooltip");
        private static final Identifier MAKE_OP_SPRITE = Identifier.withDefaultNamespace("player_list/make_operator");
        private static final Identifier REMOVE_OP_SPRITE = Identifier.withDefaultNamespace("player_list/remove_operator");
        private static final Identifier REMOVE_PLAYER_SPRITE = Identifier.withDefaultNamespace("player_list/remove_player");
        private static final int ICON_WIDTH = 8;
        private static final int ICON_HEIGHT = 7;
        private final PlayerInfo playerInfo;
        private final Button removeButton;
        private final Button makeOpButton;
        private final Button removeOpButton;
        final /* synthetic */ RealmsPlayersTab this$0;

        public PlayerEntry(RealmsPlayersTab realmsPlayersTab, PlayerInfo playerInfo) {
            RealmsPlayersTab realmsPlayersTab2 = realmsPlayersTab;
            Objects.requireNonNull(realmsPlayersTab2);
            this.this$0 = realmsPlayersTab2;
            this.playerInfo = playerInfo;
            int index = realmsPlayersTab.serverData.players.indexOf(this.playerInfo);
            this.makeOpButton = SpriteIconButton.builder(NORMAL_USER_TEXT, button -> this.op(index), false).sprite(MAKE_OP_SPRITE, 8, 7).width(16 + realmsPlayersTab.configurationScreen.getFont().width(NORMAL_USER_TEXT)).narration(defaultNarrationSupplier -> CommonComponents.joinForNarration(Component.translatable("mco.invited.player.narration", playerInfo.name), (Component)defaultNarrationSupplier.get(), Component.translatable("narration.cycle_button.usage.focused", OP_TEXT))).build();
            this.removeOpButton = SpriteIconButton.builder(OP_TEXT, button -> this.deop(index), false).sprite(REMOVE_OP_SPRITE, 8, 7).width(16 + realmsPlayersTab.configurationScreen.getFont().width(OP_TEXT)).narration(defaultNarrationSupplier -> CommonComponents.joinForNarration(Component.translatable("mco.invited.player.narration", playerInfo.name), (Component)defaultNarrationSupplier.get(), Component.translatable("narration.cycle_button.usage.focused", NORMAL_USER_TEXT))).build();
            this.removeButton = SpriteIconButton.builder(REMOVE_TEXT, button -> this.uninvite(index), false).sprite(REMOVE_PLAYER_SPRITE, 8, 7).width(16 + realmsPlayersTab.configurationScreen.getFont().width(REMOVE_TEXT)).narration(defaultNarrationSupplier -> CommonComponents.joinForNarration(Component.translatable("mco.invited.player.narration", playerInfo.name), (Component)defaultNarrationSupplier.get())).build();
            this.updateOpButtons();
        }

        private void op(int index) {
            UUID selectedInvite = this.this$0.serverData.players.get((int)index).uuid;
            RealmsUtil.supplyAsync(client -> client.op(this.this$0.serverData.id, selectedInvite), e -> LOGGER.error("Couldn't op the user", (Throwable)e)).thenAcceptAsync(ops -> {
                this.updateOps((Ops)ops);
                this.updateOpButtons();
                this.setFocused(this.removeOpButton);
            }, (Executor)this.this$0.minecraft);
        }

        private void deop(int index) {
            UUID selectedInvite = this.this$0.serverData.players.get((int)index).uuid;
            RealmsUtil.supplyAsync(client -> client.deop(this.this$0.serverData.id, selectedInvite), e -> LOGGER.error("Couldn't deop the user", (Throwable)e)).thenAcceptAsync(ops -> {
                this.updateOps((Ops)ops);
                this.updateOpButtons();
                this.setFocused(this.makeOpButton);
            }, (Executor)this.this$0.minecraft);
        }

        private void uninvite(int index) {
            if (index >= 0 && index < this.this$0.serverData.players.size()) {
                PlayerInfo playerInfo = this.this$0.serverData.players.get(index);
                RealmsConfirmScreen confirmScreen = new RealmsConfirmScreen(result -> {
                    if (result) {
                        RealmsUtil.runAsync(client -> client.uninvite(this.this$0.serverData.id, playerInfo.uuid), e -> LOGGER.error("Couldn't uninvite user", (Throwable)e));
                        this.this$0.serverData.players.remove(index);
                        this.this$0.updateData(this.this$0.serverData);
                    }
                    this.this$0.minecraft.gui.setScreen(this.this$0.configurationScreen);
                }, QUESTION_TITLE, (Component)Component.translatable("mco.configure.world.uninvite.player", playerInfo.name));
                this.this$0.minecraft.gui.setScreen(confirmScreen);
            }
        }

        private void updateOps(Ops ops) {
            for (PlayerInfo playerInfo : this.this$0.serverData.players) {
                playerInfo.operator = ops.ops().contains(playerInfo.name);
            }
        }

        private void updateOpButtons() {
            this.makeOpButton.visible = !this.playerInfo.operator;
            this.removeOpButton.visible = !this.makeOpButton.visible;
        }

        private Button activeOpButton() {
            if (this.makeOpButton.visible) {
                return this.makeOpButton;
            }
            return this.removeOpButton;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of((Object)this.activeOpButton(), (Object)this.removeButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of((Object)this.activeOpButton(), (Object)this.removeButton);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int inviteColor = !this.playerInfo.accepted ? -6250336 : (this.playerInfo.online ? -16711936 : -1);
            int skinYPos = this.getContentYMiddle() - 16;
            RealmsUtil.extractPlayerFace(graphics, this.getContentX(), skinYPos, 32, this.playerInfo.uuid);
            int textYPos = this.getContentYMiddle() - this.this$0.font.lineHeight / 2;
            graphics.text(this.this$0.font, this.playerInfo.name, this.getContentX() + 8 + 32, textYPos, inviteColor);
            int iconYPos = this.getContentYMiddle() - 10;
            int removeButtonXPos = this.getContentRight() - this.removeButton.getWidth();
            this.removeButton.setPosition(removeButtonXPos, iconYPos);
            this.removeButton.extractRenderState(graphics, mouseX, mouseY, a);
            int opButtonXPos = removeButtonXPos - this.activeOpButton().getWidth() - 8;
            this.makeOpButton.setPosition(opButtonXPos, iconYPos);
            this.makeOpButton.extractRenderState(graphics, mouseX, mouseY, a);
            this.removeOpButton.setPosition(opButtonXPos, iconYPos);
            this.removeOpButton.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static abstract class Entry
    extends ContainerObjectSelectionList.Entry<Entry> {
        private Entry() {
        }
    }
}

