/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.gui.screens.RealmsDownloadLatestWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsBrokenWorldScreen
extends RealmsScreen {
    private static final Identifier SLOT_FRAME_SPRITE = Identifier.withDefaultNamespace("widget/slot_frame");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_BUTTON_WIDTH = 80;
    private final Screen lastScreen;
    private @Nullable RealmsServer serverData;
    private final long serverId;
    private final Component[] message = new Component[]{Component.translatable("mco.brokenworld.message.line1"), Component.translatable("mco.brokenworld.message.line2")};
    private int leftX;
    private final List<Integer> slotsThatHasBeenDownloaded = Lists.newArrayList();
    private int animTick;

    public RealmsBrokenWorldScreen(Screen lastScreen, long serverId, boolean isMinigame) {
        super(isMinigame ? Component.translatable("mco.brokenworld.minigame.title") : Component.translatable("mco.brokenworld.title"));
        this.lastScreen = lastScreen;
        this.serverId = serverId;
    }

    @Override
    public void init() {
        this.leftX = this.width / 2 - 150;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).bounds((this.width - 150) / 2, RealmsBrokenWorldScreen.row(13) - 5, 150, 20).build());
        if (this.serverData == null) {
            this.fetchServerData(this.serverId);
        } else {
            this.addButtons();
        }
    }

    @Override
    public Component getNarrationMessage() {
        return ComponentUtils.formatList(Stream.concat(Stream.of(this.title), Stream.of(this.message)).collect(Collectors.toList()), CommonComponents.SPACE);
    }

    private void addButtons() {
        for (Map.Entry<Integer, RealmsSlot> entry : this.serverData.slots.entrySet()) {
            Button playOrDownloadButton;
            boolean canPlay;
            int slot = entry.getKey();
            boolean bl = canPlay = slot != this.serverData.activeSlot || this.serverData.isMinigameActive();
            if (canPlay) {
                playOrDownloadButton = Button.builder(Component.translatable("mco.brokenworld.play"), button -> this.minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchSlotTask(this.serverData.id, slot, this::doSwitchOrReset)))).bounds(this.getFramePositionX(slot), RealmsBrokenWorldScreen.row(8), 80, 20).build();
                playOrDownloadButton.active = !this.serverData.slots.get((Object)Integer.valueOf((int)slot)).options.empty;
            } else {
                playOrDownloadButton = Button.builder(Component.translatable("mco.brokenworld.download"), button -> this.minecraft.gui.setScreen(RealmsPopups.infoPopupScreen(this, Component.translatable("mco.configure.world.restore.download.question.line1"), popupScreen -> this.downloadWorld(slot)))).bounds(this.getFramePositionX(slot), RealmsBrokenWorldScreen.row(8), 80, 20).build();
            }
            if (this.slotsThatHasBeenDownloaded.contains(slot)) {
                playOrDownloadButton.active = false;
                playOrDownloadButton.setMessage(Component.translatable("mco.brokenworld.downloaded"));
            }
            this.addRenderableWidget(playOrDownloadButton);
        }
    }

    @Override
    public void tick() {
        ++this.animTick;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int xm, int ym, float a) {
        super.extractRenderState(graphics, xm, ym, a);
        graphics.centeredText(this.font, this.title, this.width / 2, 17, -1);
        for (int i = 0; i < this.message.length; ++i) {
            graphics.centeredText(this.font, this.message[i], this.width / 2, RealmsBrokenWorldScreen.row(-1) + 3 + i * 12, -6250336);
        }
        if (this.serverData == null) {
            return;
        }
        for (Map.Entry<Integer, RealmsSlot> entry : this.serverData.slots.entrySet()) {
            if (entry.getValue().options.templateImage != null && entry.getValue().options.templateId != -1L) {
                this.extractSlotFrame(graphics, this.getFramePositionX(entry.getKey()), RealmsBrokenWorldScreen.row(1) + 5, xm, ym, this.serverData.activeSlot == entry.getKey() && !this.isMinigame(), entry.getValue().options.getSlotName(entry.getKey()), entry.getKey(), entry.getValue().options.templateId, entry.getValue().options.templateImage, entry.getValue().options.empty);
                continue;
            }
            this.extractSlotFrame(graphics, this.getFramePositionX(entry.getKey()), RealmsBrokenWorldScreen.row(1) + 5, xm, ym, this.serverData.activeSlot == entry.getKey() && !this.isMinigame(), entry.getValue().options.getSlotName(entry.getKey()), entry.getKey(), -1L, null, entry.getValue().options.empty);
        }
    }

    private int getFramePositionX(int i) {
        return this.leftX + (i - 1) * 110;
    }

    public Screen createErrorScreen(RealmsServiceException exception) {
        return new RealmsGenericErrorScreen(exception, this.lastScreen);
    }

    private void fetchServerData(long realmId) {
        RealmsUtil.supplyAsync(client -> client.getOwnRealm(realmId), RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get own world")).thenAcceptAsync(serverData -> {
            this.serverData = serverData;
            this.addButtons();
        }, (Executor)this.minecraft);
    }

    public void doSwitchOrReset() {
        new Thread(() -> {
            RealmsClient client = RealmsClient.getOrCreate();
            if (this.serverData.state == RealmsServer.State.CLOSED) {
                this.minecraft.execute(() -> this.minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(this, new OpenServerTask(this.serverData, this, true, this.minecraft))));
            } else {
                try {
                    RealmsServer ownRealm = client.getOwnRealm(this.serverId);
                    this.minecraft.execute(() -> RealmsMainScreen.play(ownRealm, this));
                }
                catch (RealmsServiceException e) {
                    LOGGER.error("Couldn't get own world", (Throwable)e);
                    this.minecraft.execute(() -> this.minecraft.gui.setScreen(this.createErrorScreen(e)));
                }
            }
        }, "Realms open server").start();
    }

    private void downloadWorld(int slotId) {
        RealmsClient client = RealmsClient.getOrCreate();
        try {
            WorldDownload worldDownload = client.requestDownloadInfo(this.serverData.id, slotId);
            RealmsDownloadLatestWorldScreen downloadScreen = new RealmsDownloadLatestWorldScreen(this, worldDownload, this.serverData.getWorldName(slotId), result -> {
                if (result) {
                    this.slotsThatHasBeenDownloaded.add(slotId);
                    this.clearWidgets();
                    this.addButtons();
                } else {
                    this.minecraft.gui.setScreen(this);
                }
            });
            this.minecraft.gui.setScreen(downloadScreen);
        }
        catch (RealmsServiceException e) {
            LOGGER.error("Couldn't download world data", (Throwable)e);
            this.minecraft.gui.setScreen(new RealmsGenericErrorScreen(e, (Screen)this));
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    private boolean isMinigame() {
        return this.serverData != null && this.serverData.isMinigameActive();
    }

    private void extractSlotFrame(GuiGraphicsExtractor graphics, int x, int y, int xm, int ym, boolean active, String text, int i, long imageId, @Nullable String image, boolean empty) {
        Identifier texture = empty ? RealmsWorldSlotButton.EMPTY_SLOT_LOCATION : (image != null && imageId != -1L ? RealmsTextureManager.worldTemplate(String.valueOf(imageId), image) : (i == 1 ? RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_1 : (i == 2 ? RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_2 : (i == 3 ? RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_3 : RealmsTextureManager.worldTemplate(String.valueOf(this.serverData.minigameId), this.serverData.minigameImage)))));
        if (active) {
            float c = 0.9f + 0.1f * Mth.cos((float)this.animTick * 0.2f);
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + 3, y + 3, 0.0f, 0.0f, 74, 74, 74, 74, 74, 74, ARGB.colorFromFloat(1.0f, c, c, c));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, x, y, 80, 80);
        } else {
            int color = ARGB.colorFromFloat(1.0f, 0.56f, 0.56f, 0.56f);
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + 3, y + 3, 0.0f, 0.0f, 74, 74, 74, 74, 74, 74, color);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, x, y, 80, 80, color);
        }
        graphics.centeredText(this.font, text, x + 40, y + 66, -1);
    }
}

