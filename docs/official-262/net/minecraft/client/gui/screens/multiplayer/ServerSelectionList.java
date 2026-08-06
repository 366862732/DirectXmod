/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.util.concurrent.ThreadFactoryBuilder
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ServerSelectionList
extends ObjectSelectionList<Entry> {
    private static final Identifier INCOMPATIBLE_SPRITE = Identifier.withDefaultNamespace("server_list/incompatible");
    private static final Identifier UNREACHABLE_SPRITE = Identifier.withDefaultNamespace("server_list/unreachable");
    private static final Identifier PING_1_SPRITE = Identifier.withDefaultNamespace("server_list/ping_1");
    private static final Identifier PING_2_SPRITE = Identifier.withDefaultNamespace("server_list/ping_2");
    private static final Identifier PING_3_SPRITE = Identifier.withDefaultNamespace("server_list/ping_3");
    private static final Identifier PING_4_SPRITE = Identifier.withDefaultNamespace("server_list/ping_4");
    private static final Identifier PING_5_SPRITE = Identifier.withDefaultNamespace("server_list/ping_5");
    private static final Identifier PINGING_1_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_1");
    private static final Identifier PINGING_2_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_2");
    private static final Identifier PINGING_3_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_3");
    private static final Identifier PINGING_4_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_4");
    private static final Identifier PINGING_5_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_5");
    private static final Identifier JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/join_highlighted");
    private static final Identifier JOIN_SPRITE = Identifier.withDefaultNamespace("server_list/join");
    private static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_up_highlighted");
    private static final Identifier MOVE_UP_SPRITE = Identifier.withDefaultNamespace("server_list/move_up");
    private static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_down_highlighted");
    private static final Identifier MOVE_DOWN_SPRITE = Identifier.withDefaultNamespace("server_list/move_down");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadPoolExecutor THREAD_POOL = new ScheduledThreadPoolExecutor(5, new ThreadFactoryBuilder().setNameFormat("Server Pinger #%d").setDaemon(true).setUncaughtExceptionHandler((Thread.UncaughtExceptionHandler)new DefaultUncaughtExceptionHandler(LOGGER)).build());
    private static final Component SCANNING_LABEL = Component.translatable("lanServer.scanning");
    private static final Component CANT_RESOLVE_TEXT = Component.translatable("multiplayer.status.cannot_resolve").withColor(-65536);
    private static final Component CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withColor(-65536);
    private static final Component INCOMPATIBLE_STATUS = Component.translatable("multiplayer.status.incompatible");
    private static final Component NO_CONNECTION_STATUS = Component.translatable("multiplayer.status.no_connection");
    private static final Component PINGING_STATUS = Component.translatable("multiplayer.status.pinging");
    private static final Component ONLINE_STATUS = Component.translatable("multiplayer.status.online");
    private final JoinMultiplayerScreen screen;
    private final List<OnlineServerEntry> onlineServers = Lists.newArrayList();
    private final Entry lanHeader = new LANHeader();
    private final List<NetworkServerEntry> networkServers = Lists.newArrayList();

    public ServerSelectionList(JoinMultiplayerScreen screen, Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
        this.screen = screen;
    }

    private void refreshEntries() {
        Entry previouslySelected = (Entry)this.getSelected();
        ArrayList<OnlineServerEntry> entriesToAdd = new ArrayList<OnlineServerEntry>(this.onlineServers);
        entriesToAdd.add((OnlineServerEntry)this.lanHeader);
        entriesToAdd.addAll(this.networkServers);
        this.replaceEntries(entriesToAdd);
        if (previouslySelected != null) {
            for (Entry entry : entriesToAdd) {
                if (!entry.matches(previouslySelected)) continue;
                this.setSelected(entry);
                break;
            }
        }
    }

    @Override
    public void setSelected(@Nullable Entry selected) {
        super.setSelected(selected);
        this.screen.onSelectedChange();
    }

    public void updateOnlineServers(ServerList servers) {
        this.onlineServers.clear();
        for (int i = 0; i < servers.size(); ++i) {
            this.onlineServers.add(new OnlineServerEntry(this, this.screen, servers.get(i)));
        }
        this.refreshEntries();
    }

    public void updateNetworkServers(List<LanServer> servers) {
        int newServerCount = servers.size() - this.networkServers.size();
        this.networkServers.clear();
        for (LanServer server : servers) {
            this.networkServers.add(new NetworkServerEntry(this.screen, server));
        }
        this.refreshEntries();
        for (int i = this.networkServers.size() - newServerCount; i < this.networkServers.size(); ++i) {
            NetworkServerEntry newServer = this.networkServers.get(i);
            int entryIndex = i - this.networkServers.size() + this.children().size();
            int rowTop = this.getRowTop(entryIndex);
            int rowBottom = this.getRowBottom(entryIndex);
            if (rowBottom < this.getY() || rowTop > this.getBottom()) continue;
            this.minecraft.getNarrator().saySystemQueued(Component.translatable("multiplayer.lan.server_found", newServer.getServerNarration()));
        }
    }

    @Override
    public int getRowWidth() {
        return 305;
    }

    public void removed() {
    }

    @Environment(value=EnvType.CLIENT)
    public static class LANHeader
    extends Entry {
        private final Minecraft minecraft = Minecraft.getInstance();
        private final LoadingDotsWidget loadingDotsWidget;

        public LANHeader() {
            this.loadingDotsWidget = new LoadingDotsWidget(this.minecraft.font, SCANNING_LABEL);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.loadingDotsWidget.setPosition(this.getContentXMiddle() - this.minecraft.font.width(SCANNING_LABEL) / 2, this.getContentY());
            this.loadingDotsWidget.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public Component getNarration() {
            return SCANNING_LABEL;
        }

        @Override
        protected boolean matches(Entry other) {
            return other instanceof LANHeader;
        }

        @Override
        public void join() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class Entry
    extends ObjectSelectionList.Entry<Entry>
    implements AutoCloseable {
        @Override
        public void close() {
        }

        protected abstract boolean matches(Entry var1);

        public abstract void join();
    }

    @Environment(value=EnvType.CLIENT)
    public class OnlineServerEntry
    extends Entry
    implements SelectableEntry {
        private static final int ICON_SIZE = 32;
        private static final int SPACING = 5;
        private static final int STATUS_ICON_WIDTH = 10;
        private static final int STATUS_ICON_HEIGHT = 8;
        private final JoinMultiplayerScreen screen;
        private final Minecraft minecraft;
        private final ServerData serverData;
        private final FaviconTexture icon;
        private byte @Nullable [] lastIconBytes;
        private @Nullable List<Component> onlinePlayersTooltip;
        private @Nullable Identifier statusIcon;
        private @Nullable Component statusIconTooltip;
        final /* synthetic */ ServerSelectionList this$0;

        protected OnlineServerEntry(ServerSelectionList this$0, JoinMultiplayerScreen screen, ServerData serverData) {
            ServerSelectionList serverSelectionList = this$0;
            Objects.requireNonNull(serverSelectionList);
            this.this$0 = serverSelectionList;
            this.screen = screen;
            this.serverData = serverData;
            this.minecraft = Minecraft.getInstance();
            this.icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), serverData.ip);
            this.refreshStatus();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            byte[] currentIconBytes;
            if (this.serverData.state() == ServerData.State.INITIAL) {
                this.serverData.setState(ServerData.State.PINGING);
                this.serverData.motd = CommonComponents.EMPTY;
                this.serverData.status = CommonComponents.EMPTY;
                THREAD_POOL.submit(() -> {
                    try {
                        this.screen.getPinger().pingServer(this.serverData, () -> this.minecraft.execute(this::updateServerList), () -> {
                            this.serverData.setState(this.serverData.protocol == SharedConstants.getCurrentVersion().protocolVersion() ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
                            this.minecraft.execute(this::refreshStatus);
                        }, EventLoopGroupHolder.remote(this.minecraft.options.useNativeTransport()));
                    }
                    catch (UnknownHostException ignored) {
                        this.serverData.setState(ServerData.State.UNREACHABLE);
                        this.serverData.motd = CANT_RESOLVE_TEXT;
                        this.minecraft.execute(this::refreshStatus);
                    }
                    catch (Exception ignored) {
                        this.serverData.setState(ServerData.State.UNREACHABLE);
                        this.serverData.motd = CANT_CONNECT_TEXT;
                        this.minecraft.execute(this::refreshStatus);
                    }
                });
            }
            graphics.text(this.minecraft.font, this.serverData.name, this.getContentX() + 32 + 3, this.getContentY() + 1, -1);
            List<FormattedCharSequence> lines = this.minecraft.font.split(this.serverData.motd, this.getContentWidth() - 32 - 2);
            for (int i = 0; i < Math.min(lines.size(), 2); ++i) {
                graphics.text(this.minecraft.font, lines.get(i), this.getContentX() + 32 + 3, this.getContentY() + 12 + this.minecraft.font.lineHeight * i, -8355712);
            }
            this.extractIcon(graphics, this.getContentX(), this.getContentY(), this.icon.textureLocation());
            int index = this.this$0.children().indexOf(this);
            if (this.serverData.state() == ServerData.State.PINGING) {
                int iconIndex = (int)(Util.getMillis() / 100L + (long)(index * 2) & 7L);
                if (iconIndex > 4) {
                    iconIndex = 8 - iconIndex;
                }
                this.statusIcon = switch (iconIndex) {
                    default -> PINGING_1_SPRITE;
                    case 1 -> PINGING_2_SPRITE;
                    case 2 -> PINGING_3_SPRITE;
                    case 3 -> PINGING_4_SPRITE;
                    case 4 -> PINGING_5_SPRITE;
                };
            }
            int statusIconX = this.getContentRight() - 10 - 5;
            if (this.statusIcon != null) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.statusIcon, statusIconX, this.getContentY(), 10, 8);
            }
            if (!Arrays.equals(currentIconBytes = this.serverData.getIconBytes(), this.lastIconBytes)) {
                if (this.uploadServerIcon(currentIconBytes)) {
                    this.lastIconBytes = currentIconBytes;
                } else {
                    this.serverData.setIconBytes(null);
                    this.updateServerList();
                }
            }
            Component status = this.serverData.state() == ServerData.State.INCOMPATIBLE ? this.serverData.version.copy().withStyle(ChatFormatting.RED) : this.serverData.status;
            int statusWidth = this.minecraft.font.width(status);
            int statusX = statusIconX - statusWidth - 5;
            graphics.text(this.minecraft.font, status, statusX, this.getContentY() + 1, -8355712);
            if (this.statusIconTooltip != null && mouseX >= statusIconX && mouseX <= statusIconX + 10 && mouseY >= this.getContentY() && mouseY <= this.getContentY() + 8) {
                graphics.setTooltipForNextFrame(this.statusIconTooltip, mouseX, mouseY);
            } else if (this.onlinePlayersTooltip != null && mouseX >= statusX && mouseX <= statusX + statusWidth && mouseY >= this.getContentY() && mouseY <= this.getContentY() - 1 + this.minecraft.font.lineHeight) {
                graphics.setTooltipForNextFrame(Lists.transform(this.onlinePlayersTooltip, Component::getVisualOrderText), mouseX, mouseY);
            }
            if (hovered) {
                graphics.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
                int relX = mouseX - this.getContentX();
                int relY = mouseY - this.getContentY();
                if (this.mouseOverRightHalf(relX, relY, 32)) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, JOIN_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                    this.this$0.handleCursor(graphics);
                } else {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, JOIN_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                }
                if (index > 0) {
                    if (this.mouseOverTopLeftQuarter(relX, relY, 32)) {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_UP_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                        this.this$0.handleCursor(graphics);
                    } else {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_UP_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                    }
                }
                if (index < this.screen.getServers().size() - 1) {
                    if (this.mouseOverBottomLeftQuarter(relX, relY, 32)) {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                        this.this$0.handleCursor(graphics);
                    } else {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOVE_DOWN_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                    }
                }
            }
        }

        private void refreshStatus() {
            this.onlinePlayersTooltip = null;
            switch (this.serverData.state()) {
                case INITIAL: 
                case PINGING: {
                    this.statusIcon = PING_1_SPRITE;
                    this.statusIconTooltip = PINGING_STATUS;
                    break;
                }
                case INCOMPATIBLE: {
                    this.statusIcon = INCOMPATIBLE_SPRITE;
                    this.statusIconTooltip = INCOMPATIBLE_STATUS;
                    this.onlinePlayersTooltip = this.serverData.playerList;
                    break;
                }
                case UNREACHABLE: {
                    this.statusIcon = UNREACHABLE_SPRITE;
                    this.statusIconTooltip = NO_CONNECTION_STATUS;
                    break;
                }
                case SUCCESSFUL: {
                    this.statusIcon = this.serverData.ping < 150L ? PING_5_SPRITE : (this.serverData.ping < 300L ? PING_4_SPRITE : (this.serverData.ping < 600L ? PING_3_SPRITE : (this.serverData.ping < 1000L ? PING_2_SPRITE : PING_1_SPRITE)));
                    this.statusIconTooltip = Component.translatable("multiplayer.status.ping", this.serverData.ping);
                    this.onlinePlayersTooltip = this.serverData.playerList;
                }
            }
        }

        public void updateServerList() {
            this.screen.getServers().save();
        }

        protected void extractIcon(GuiGraphicsExtractor graphics, int rowLeft, int rowTop, Identifier location) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, location, rowLeft, rowTop, 0.0f, 0.0f, 32, 32, 32, 32);
        }

        private boolean uploadServerIcon(byte @Nullable [] serverIconBytes) {
            if (serverIconBytes == null) {
                this.icon.clear();
            } else {
                try {
                    this.icon.upload(NativeImage.read(serverIconBytes));
                }
                catch (Throwable t) {
                    LOGGER.error("Invalid icon for server {} ({})", new Object[]{this.serverData.name, this.serverData.ip, t});
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isSelection()) {
                this.join();
                return true;
            }
            if (event.hasShiftDown()) {
                ServerSelectionList list = this.screen.serverSelectionList;
                int currentIndex = list.children().indexOf(this);
                if (currentIndex == -1) {
                    return true;
                }
                if (event.isDown() && currentIndex < this.screen.getServers().size() - 1 || event.isUp() && currentIndex > 0) {
                    this.swap(currentIndex, event.isDown() ? currentIndex + 1 : currentIndex - 1);
                    return true;
                }
            }
            return super.keyPressed(event);
        }

        @Override
        public void join() {
            this.screen.join(this.serverData);
        }

        private void swap(int currentIndex, int newIndex) {
            this.screen.getServers().swap(currentIndex, newIndex);
            this.screen.serverSelectionList.swap(currentIndex, newIndex);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            int relY;
            int relX = (int)event.x() - this.getContentX();
            if (this.mouseOverRightHalf(relX, relY = (int)event.y() - this.getContentY(), 32)) {
                this.join();
                return true;
            }
            int currentIndex = this.screen.serverSelectionList.children().indexOf(this);
            if (currentIndex > 0 && this.mouseOverTopLeftQuarter(relX, relY, 32)) {
                this.swap(currentIndex, currentIndex - 1);
                return true;
            }
            if (currentIndex < this.screen.getServers().size() - 1 && this.mouseOverBottomLeftQuarter(relX, relY, 32)) {
                this.swap(currentIndex, currentIndex + 1);
                return true;
            }
            if (doubleClick) {
                this.join();
            }
            return super.mouseClicked(event, doubleClick);
        }

        public ServerData getServerData() {
            return this.serverData;
        }

        @Override
        public Component getNarration() {
            MutableComponent narrationComponent = Component.empty();
            narrationComponent.append(Component.translatable("narrator.select", this.serverData.name));
            narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
            switch (this.serverData.state()) {
                case INCOMPATIBLE: {
                    narrationComponent.append(INCOMPATIBLE_STATUS);
                    narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    narrationComponent.append(Component.translatable("multiplayer.status.version.narration", this.serverData.version));
                    narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    narrationComponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
                    break;
                }
                case UNREACHABLE: {
                    narrationComponent.append(NO_CONNECTION_STATUS);
                    break;
                }
                case PINGING: {
                    narrationComponent.append(PINGING_STATUS);
                    break;
                }
                default: {
                    narrationComponent.append(ONLINE_STATUS);
                    narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    narrationComponent.append(Component.translatable("multiplayer.status.ping.narration", this.serverData.ping));
                    narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    narrationComponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
                    if (this.serverData.players == null) break;
                    narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    narrationComponent.append(Component.translatable("multiplayer.status.player_count.narration", this.serverData.players.online(), this.serverData.players.max()));
                    narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    narrationComponent.append(ComponentUtils.formatList(this.serverData.playerList, Component.literal(", ")));
                }
            }
            return narrationComponent;
        }

        @Override
        public void close() {
            this.icon.close();
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        @Override
        protected boolean matches(Entry other) {
            if (!(other instanceof OnlineServerEntry)) return false;
            OnlineServerEntry onlineServerEntry = (OnlineServerEntry)other;
            if (onlineServerEntry.serverData != this.serverData) return false;
            return true;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class NetworkServerEntry
    extends Entry {
        private static final int ICON_WIDTH = 32;
        private static final Component LAN_SERVER_HEADER = Component.translatable("lanServer.title");
        private static final Component HIDDEN_ADDRESS_TEXT = Component.translatable("selectServer.hiddenAddress");
        private final JoinMultiplayerScreen screen;
        protected final Minecraft minecraft;
        protected final LanServer serverData;

        protected NetworkServerEntry(JoinMultiplayerScreen screen, LanServer serverData) {
            this.screen = screen;
            this.serverData = serverData;
            this.minecraft = Minecraft.getInstance();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            graphics.text(this.minecraft.font, LAN_SERVER_HEADER, this.getContentX() + 32 + 3, this.getContentY() + 1, -1);
            graphics.text(this.minecraft.font, this.serverData.getMotd(), this.getContentX() + 32 + 3, this.getContentY() + 12, -8355712);
            if (this.minecraft.options.hideServerAddress) {
                graphics.text(this.minecraft.font, HIDDEN_ADDRESS_TEXT, this.getContentX() + 32 + 3, this.getContentY() + 12 + 11, -8355712);
            } else {
                graphics.text(this.minecraft.font, this.serverData.getAddress(), this.getContentX() + 32 + 3, this.getContentY() + 12 + 11, -8355712);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (doubleClick) {
                this.join();
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isSelection()) {
                this.join();
                return true;
            }
            return super.keyPressed(event);
        }

        @Override
        public void join() {
            this.screen.join(new ServerData(this.serverData.getMotd(), this.serverData.getAddress(), ServerData.Type.LAN));
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.getServerNarration());
        }

        public Component getServerNarration() {
            return Component.empty().append(LAN_SERVER_HEADER).append(CommonComponents.SPACE).append(this.serverData.getMotd());
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        @Override
        protected boolean matches(Entry other) {
            if (!(other instanceof NetworkServerEntry)) return false;
            NetworkServerEntry networkServerEntry = (NetworkServerEntry)other;
            if (networkServerEntry.serverData != this.serverData) return false;
            return true;
        }
    }
}

