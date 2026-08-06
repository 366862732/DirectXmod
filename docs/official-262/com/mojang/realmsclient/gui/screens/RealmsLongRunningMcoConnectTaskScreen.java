/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.dto.RealmsJoinInformation;
import com.mojang.realmsclient.dto.ServiceQuality;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.task.LongRunningTask;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public class RealmsLongRunningMcoConnectTaskScreen
extends RealmsLongRunningMcoTaskScreen {
    private final LongRunningTask task;
    private final RealmsJoinInformation serverAddress;
    private final LinearLayout footer = LinearLayout.vertical();

    public RealmsLongRunningMcoConnectTaskScreen(Screen lastScreen, RealmsJoinInformation serverAddress, LongRunningTask task) {
        super(lastScreen, task);
        this.task = task;
        this.serverAddress = serverAddress;
    }

    @Override
    public void init() {
        super.init();
        if (this.serverAddress.regionData() == null || this.serverAddress.regionData().region() == null) {
            return;
        }
        LinearLayout regionInfo = LinearLayout.horizontal().spacing(10);
        StringWidget region = new StringWidget(Component.translatable("mco.connect.region", Component.translatable(this.serverAddress.regionData().region().translationKey)), this.font);
        regionInfo.addChild(region);
        Identifier icon = this.serverAddress.regionData().serviceQuality() != null ? this.serverAddress.regionData().serviceQuality().getIcon() : ServiceQuality.UNKNOWN.getIcon();
        regionInfo.addChild(ImageWidget.sprite(10, 8, icon), LayoutSettings::alignVerticallyTop);
        this.footer.addChild(regionInfo, layoutSettings -> layoutSettings.paddingTop(40));
        RealmsLongRunningMcoConnectTaskScreen realmsLongRunningMcoConnectTaskScreen = this;
        this.footer.visitWidgets(x$0 -> realmsLongRunningMcoConnectTaskScreen.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        int contentBottom = this.layout.getY() + this.layout.getHeight();
        ScreenRectangle footerRectangle = new ScreenRectangle(0, contentBottom, this.width, this.height - contentBottom);
        this.footer.arrangeElements();
        FrameLayout.alignInRectangle(this.footer, footerRectangle, 0.5f, 0.0f);
    }

    @Override
    public void tick() {
        super.tick();
        this.task.tick();
    }

    @Override
    protected void cancel() {
        this.task.abortTask();
        super.cancel();
    }
}

