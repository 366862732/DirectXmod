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
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.configuration.RealmsBackupInfoScreen;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.DownloadTask;
import com.mojang.realmsclient.util.task.RestoreTask;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsBackupScreen
extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("mco.configure.world.backup");
    private static final Component RESTORE_TOOLTIP = Component.translatable("mco.backup.button.restore");
    private static final Component HAS_CHANGES_TOOLTIP = Component.translatable("mco.backup.changes.tooltip");
    private static final Component NO_BACKUPS_LABEL = Component.translatable("mco.backup.nobackups");
    private static final Component DOWNLOAD_LATEST = Component.translatable("mco.backup.button.download");
    private static final String UPLOADED_KEY = "uploaded";
    private static final int PADDING = 8;
    public static final DateTimeFormatter SHORT_DATE_FORMAT = Util.localizedDateFormatter(FormatStyle.SHORT);
    private final RealmsConfigureWorldScreen lastScreen;
    private List<Backup> backups = Collections.emptyList();
    private @Nullable BackupObjectSelectionList backupList;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final int slotId;
    private @Nullable Button downloadButton;
    private final RealmsServer serverData;
    private boolean noBackups = false;

    public RealmsBackupScreen(RealmsConfigureWorldScreen lastScreen, RealmsServer serverData, int slotId) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.serverData = serverData;
        this.slotId = slotId;
    }

    @Override
    public void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        this.backupList = this.layout.addToContents(new BackupObjectSelectionList(this));
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.downloadButton = footer.addChild(Button.builder(DOWNLOAD_LATEST, button -> this.downloadClicked()).build());
        this.downloadButton.active = false;
        footer.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
        RealmsBackupScreen realmsBackupScreen = this;
        this.layout.visitWidgets(x$0 -> realmsBackupScreen.addRenderableWidget(x$0));
        this.repositionElements();
        this.fetchRealmsBackups();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (this.noBackups && this.backupList != null) {
            graphics.text(this.font, NO_BACKUPS_LABEL, this.width / 2 - this.font.width(NO_BACKUPS_LABEL) / 2, this.backupList.getY() + this.backupList.getHeight() / 2 - this.font.lineHeight / 2, -1);
        }
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.backupList != null) {
            this.backupList.updateSize(this.width, this.layout);
        }
    }

    private void fetchRealmsBackups() {
        new Thread(this, "Realms-fetch-backups"){
            final /* synthetic */ RealmsBackupScreen this$0;
            {
                RealmsBackupScreen realmsBackupScreen = this$0;
                Objects.requireNonNull(realmsBackupScreen);
                this.this$0 = realmsBackupScreen;
                super(name);
            }

            @Override
            public void run() {
                RealmsClient client = RealmsClient.getOrCreate();
                try {
                    List<Backup> backups = client.backupsFor(this.this$0.serverData.id).backups();
                    this.this$0.minecraft.execute(() -> {
                        this.this$0.backups = backups;
                        this.this$0.noBackups = this.this$0.backups.isEmpty();
                        if (!this.this$0.noBackups && this.this$0.downloadButton != null) {
                            this.this$0.downloadButton.active = true;
                        }
                        if (this.this$0.backupList != null) {
                            this.this$0.backupList.replaceEntries(this.this$0.backups.stream().map(x$0 -> new Entry(this.this$0, (Backup)x$0)).toList());
                        }
                    });
                }
                catch (RealmsServiceException e) {
                    LOGGER.error("Couldn't request backups", (Throwable)e);
                }
            }
        }.start();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    private void downloadClicked() {
        this.minecraft.gui.setScreen(RealmsPopups.infoPopupScreen(this, Component.translatable("mco.configure.world.restore.download.question.line1"), popupScreen -> this.minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen.getNewScreen(), new DownloadTask(this.serverData.id, this.slotId, Objects.requireNonNullElse(this.serverData.name, "") + " (" + this.serverData.slots.get((Object)Integer.valueOf((int)this.serverData.activeSlot)).options.getSlotName(this.serverData.activeSlot) + ")", this)))));
    }

    @Environment(value=EnvType.CLIENT)
    private class BackupObjectSelectionList
    extends ContainerObjectSelectionList<Entry> {
        private static final int ITEM_HEIGHT = 36;

        public BackupObjectSelectionList(RealmsBackupScreen realmsBackupScreen) {
            Objects.requireNonNull(realmsBackupScreen);
            super(Minecraft.getInstance(), realmsBackupScreen.width, realmsBackupScreen.layout.getContentHeight(), realmsBackupScreen.layout.getHeaderHeight(), 36);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class Entry
    extends ContainerObjectSelectionList.Entry<Entry> {
        private static final int Y_PADDING = 2;
        private final Backup backup;
        private @Nullable Button restoreButton;
        private @Nullable Button changesButton;
        private final List<AbstractWidget> children;
        final /* synthetic */ RealmsBackupScreen this$0;

        public Entry(RealmsBackupScreen realmsBackupScreen, Backup backup) {
            RealmsBackupScreen realmsBackupScreen2 = realmsBackupScreen;
            Objects.requireNonNull(realmsBackupScreen2);
            this.this$0 = realmsBackupScreen2;
            this.children = new ArrayList<AbstractWidget>();
            this.backup = backup;
            this.populateChangeList(backup);
            if (!backup.changeList.isEmpty()) {
                this.changesButton = Button.builder(HAS_CHANGES_TOOLTIP, button -> ((RealmsBackupScreen)this$0).minecraft.gui.setScreen(new RealmsBackupInfoScreen(realmsBackupScreen, this.backup))).width(8 + realmsBackupScreen.font.width(HAS_CHANGES_TOOLTIP)).createNarration(this::narrationForBackupEntry).build();
                this.children.add(this.changesButton);
            }
            if (!realmsBackupScreen.serverData.expired) {
                this.restoreButton = Button.builder(RESTORE_TOOLTIP, button -> this.restoreClicked()).width(8 + realmsBackupScreen.font.width(HAS_CHANGES_TOOLTIP)).createNarration(this::narrationForBackupEntry).build();
                this.children.add(this.restoreButton);
            }
        }

        private MutableComponent narrationForBackupEntry(Supplier<MutableComponent> defaultNarrationSupplier) {
            return CommonComponents.joinForNarration(Component.translatable("mco.backup.narration", SHORT_DATE_FORMAT.format(this.backup.lastModifiedDate())), defaultNarrationSupplier.get());
        }

        private void populateChangeList(Backup backup) {
            int index = this.this$0.backups.indexOf(backup);
            if (index == this.this$0.backups.size() - 1) {
                return;
            }
            Backup olderBackup = this.this$0.backups.get(index + 1);
            for (String key : backup.metadata.keySet()) {
                if (!key.contains(RealmsBackupScreen.UPLOADED_KEY) && olderBackup.metadata.containsKey(key)) {
                    if (backup.metadata.get(key).equals(olderBackup.metadata.get(key))) continue;
                    this.addToChangeList(key);
                    continue;
                }
                this.addToChangeList(key);
            }
        }

        private void addToChangeList(String key) {
            if (key.contains(RealmsBackupScreen.UPLOADED_KEY)) {
                String uploadedTime = SHORT_DATE_FORMAT.format(this.backup.lastModifiedDate());
                this.backup.changeList.put(key, uploadedTime);
                this.backup.uploadedVersion = true;
            } else {
                this.backup.changeList.put(key, this.backup.metadata.get(key));
            }
        }

        private void restoreClicked() {
            Component age = RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModified);
            String lastModifiedDate = SHORT_DATE_FORMAT.format(this.backup.lastModifiedDate());
            MutableComponent popupMessage = Component.translatable("mco.configure.world.restore.question.line1", lastModifiedDate, age);
            ((RealmsBackupScreen)this.this$0).minecraft.gui.setScreen(RealmsPopups.warningPopupScreen(this.this$0, popupMessage, popupScreen -> {
                RealmsConfigureWorldScreen newScreen = this.this$0.lastScreen.getNewScreen();
                ((RealmsBackupScreen)this.this$0).minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(newScreen, new RestoreTask(this.backup, this.this$0.serverData.id, newScreen)));
            }));
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int middle = this.getContentYMiddle();
            int firstLineYPos = middle - ((RealmsBackupScreen)this.this$0).font.lineHeight - 2;
            int secondLineYPos = middle + 2;
            int color = this.backup.uploadedVersion ? -8388737 : -1;
            graphics.text(this.this$0.font, Component.translatable("mco.backup.entry", RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModified)), this.getContentX(), firstLineYPos, color);
            graphics.text(this.this$0.font, SHORT_DATE_FORMAT.format(this.backup.lastModifiedDate()), this.getContentX(), secondLineYPos, -11776948);
            int iconXOffet = 0;
            int iconYPos = this.getContentYMiddle() - 10;
            if (this.restoreButton != null) {
                this.restoreButton.setX(this.getContentRight() - (iconXOffet += this.restoreButton.getWidth() + 8));
                this.restoreButton.setY(iconYPos);
                this.restoreButton.extractRenderState(graphics, mouseX, mouseY, a);
            }
            if (this.changesButton != null) {
                this.changesButton.setX(this.getContentRight() - (iconXOffet += this.changesButton.getWidth() + 8));
                this.changesButton.setY(iconYPos);
                this.changesButton.extractRenderState(graphics, mouseX, mouseY, a);
            }
        }
    }
}

