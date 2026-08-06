/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.RealmsAvailability;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.task.DataFetcher;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class RealmsNotificationsScreen
extends RealmsScreen {
    private static final Identifier UNSEEN_NOTIFICATION_SPRITE = Identifier.withDefaultNamespace("icon/unseen_notification");
    private static final Identifier NEWS_SPRITE = Identifier.withDefaultNamespace("icon/news");
    private static final Identifier INVITE_SPRITE = Identifier.withDefaultNamespace("icon/invite");
    private static final Identifier TRIAL_AVAILABLE_SPRITE = Identifier.withDefaultNamespace("icon/trial_available");
    private final CompletableFuture<Boolean> validClient = RealmsAvailability.get().thenApply(result -> result.type() == RealmsAvailability.Type.SUCCESS);
    private @Nullable DataFetcher.Subscription realmsDataSubscription;
    private @Nullable DataFetcherConfiguration currentConfiguration;
    private volatile int numberOfPendingInvites;
    private static boolean trialAvailable;
    private static boolean hasUnreadNews;
    private static boolean hasUnseenNotifications;
    private final DataFetcherConfiguration showAll = new DataFetcherConfiguration(this){
        final /* synthetic */ RealmsNotificationsScreen this$0;
        {
            RealmsNotificationsScreen realmsNotificationsScreen = this$0;
            Objects.requireNonNull(realmsNotificationsScreen);
            this.this$0 = realmsNotificationsScreen;
        }

        @Override
        public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher dataSource) {
            DataFetcher.Subscription result = dataSource.dataFetcher.createSubscription();
            this.this$0.addNewsAndInvitesSubscriptions(dataSource, result);
            this.this$0.addNotificationsSubscriptions(dataSource, result);
            return result;
        }

        @Override
        public boolean showOldNotifications() {
            return true;
        }
    };
    private final DataFetcherConfiguration onlyNotifications = new DataFetcherConfiguration(this){
        final /* synthetic */ RealmsNotificationsScreen this$0;
        {
            RealmsNotificationsScreen realmsNotificationsScreen = this$0;
            Objects.requireNonNull(realmsNotificationsScreen);
            this.this$0 = realmsNotificationsScreen;
        }

        @Override
        public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher dataSource) {
            DataFetcher.Subscription result = dataSource.dataFetcher.createSubscription();
            this.this$0.addNotificationsSubscriptions(dataSource, result);
            return result;
        }

        @Override
        public boolean showOldNotifications() {
            return false;
        }
    };

    public RealmsNotificationsScreen() {
        super(GameNarrator.NO_TITLE);
    }

    @Override
    public void init() {
        if (this.realmsDataSubscription != null) {
            this.realmsDataSubscription.forceUpdate();
        }
    }

    @Override
    public void added() {
        super.added();
        this.minecraft.realmsDataFetcher().notificationsTask.reset();
    }

    private @Nullable DataFetcherConfiguration getConfiguration() {
        boolean realmsEnabled;
        boolean bl = realmsEnabled = this.inTitleScreen() && this.validClient.getNow(false) != false;
        if (!realmsEnabled) {
            return null;
        }
        return this.getRealmsNotificationsEnabled() ? this.showAll : this.onlyNotifications;
    }

    @Override
    public void tick() {
        DataFetcherConfiguration dataFetcherConfiguration = this.getConfiguration();
        if (!Objects.equals(this.currentConfiguration, dataFetcherConfiguration)) {
            this.currentConfiguration = dataFetcherConfiguration;
            this.realmsDataSubscription = this.currentConfiguration != null ? this.currentConfiguration.initDataFetcher(this.minecraft.realmsDataFetcher()) : null;
        }
        if (this.realmsDataSubscription != null) {
            this.realmsDataSubscription.tick();
        }
    }

    private boolean getRealmsNotificationsEnabled() {
        return this.minecraft.options.realmsNotifications().get();
    }

    private boolean inTitleScreen() {
        return this.minecraft.gui.screen() instanceof TitleScreen;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int xm, int ym, float a) {
        super.extractRenderState(graphics, xm, ym, a);
        if (this.validClient.getNow(false).booleanValue()) {
            this.extractIcons(graphics);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

    private void extractIcons(GuiGraphicsExtractor graphics) {
        int pendingInvitesCount = this.numberOfPendingInvites;
        int spacing = 24;
        int topPos = this.height / 4 + 48;
        int buttonRight = this.width / 2 + 100;
        int baseY = topPos + 48 + 2;
        int iconRight = buttonRight - 3;
        if (hasUnseenNotifications) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, UNSEEN_NOTIFICATION_SPRITE, iconRight - 12, baseY + 3, 10, 10);
            iconRight -= 16;
        }
        if (this.currentConfiguration != null && this.currentConfiguration.showOldNotifications()) {
            if (hasUnreadNews) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NEWS_SPRITE, iconRight - 14, baseY + 1, 14, 14);
                iconRight -= 16;
            }
            if (pendingInvitesCount != 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INVITE_SPRITE, iconRight - 14, baseY + 1, 14, 14);
                iconRight -= 16;
            }
            if (trialAvailable) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRIAL_AVAILABLE_SPRITE, iconRight - 10, baseY + 4, 8, 8);
            }
        }
    }

    private void addNewsAndInvitesSubscriptions(RealmsDataFetcher dataSource, DataFetcher.Subscription result) {
        result.subscribe(dataSource.pendingInvitesTask, value -> {
            this.numberOfPendingInvites = value;
        });
        result.subscribe(dataSource.trialAvailabilityTask, value -> {
            trialAvailable = value;
        });
        result.subscribe(dataSource.newsTask, value -> {
            dataSource.newsManager.updateUnreadNews((RealmsNews)value);
            hasUnreadNews = dataSource.newsManager.hasUnreadNews();
        });
    }

    private void addNotificationsSubscriptions(RealmsDataFetcher dataSource, DataFetcher.Subscription result) {
        result.subscribe(dataSource.notificationsTask, notifications -> {
            hasUnseenNotifications = false;
            for (RealmsNotification notification : notifications) {
                if (notification.seen()) continue;
                hasUnseenNotifications = true;
                break;
            }
        });
    }

    @Environment(value=EnvType.CLIENT)
    private static interface DataFetcherConfiguration {
        public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher var1);

        public boolean showOldNotifications();
    }
}

