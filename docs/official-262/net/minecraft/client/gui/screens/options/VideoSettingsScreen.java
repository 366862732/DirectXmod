/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.UnsupportedGraphicsWarningScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class VideoSettingsScreen
extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("options.videoTitle");
    private static final Component IMPROVED_TRANSPARENCY = Component.translatable("options.improvedTransparency").withStyle(ChatFormatting.ITALIC);
    private static final Component WARNING_MESSAGE = Component.translatable("options.graphics.warning.message", IMPROVED_TRANSPARENCY, IMPROVED_TRANSPARENCY);
    private static final Component WARNING_TITLE = Component.translatable("options.graphics.warning.title").withStyle(ChatFormatting.RED);
    private static final Component BUTTON_ACCEPT = Component.translatable("options.graphics.warning.accept");
    private static final Component BUTTON_CANCEL = Component.translatable("options.graphics.warning.cancel");
    private static final Component DISPLAY_HEADER = Component.translatable("options.video.display.header");
    private static final Component QUALITY_HEADER = Component.translatable("options.video.quality.header");
    private static final Component PREFERENCES_HEADER = Component.translatable("options.video.preferences.header");
    private static final Component RESTART_REQUIRED = Component.translatable("options.restartRequired").withColor(-2142128);
    private final GpuWarnlistManager gpuWarnlistManager;
    private final int oldMipmaps;
    private final int oldAnisotropyBit;
    private final TextureFilteringMethod oldTextureFiltering;
    private final LinearLayout header = LinearLayout.vertical().spacing(2);
    private @Nullable StringWidget restartWarning;

    private static OptionInstance<?>[] qualityOptions(Options options) {
        return new OptionInstance[]{options.biomeBlendRadius(), options.renderDistance(), options.prioritizeChunkUpdates(), options.simulationDistance(), options.ambientOcclusion(), options.cloudStatus(), options.particles(), options.mipmapLevels(), options.entityShadows(), options.entityDistanceScaling(), options.menuBackgroundBlurriness(), options.cloudRange(), options.cutoutLeaves(), options.improvedTransparency(), options.textureFiltering(), options.maxAnisotropyBit(), options.weatherRadius()};
    }

    private static OptionInstance<?>[] displayOptions(Options options) {
        return new OptionInstance[]{options.framerateLimit(), options.enableVsync(), options.inactivityFpsLimit(), options.guiScale(), options.fullscreen(), options.exclusiveFullscreen(), options.gamma(), options.preferredGraphicsBackend()};
    }

    private static OptionInstance<?>[] preferenceOptions(Options options) {
        return new OptionInstance[]{options.showAutosaveIndicator(), options.vignette(), options.attackIndicator(), options.chunkSectionFadeInTime()};
    }

    public VideoSettingsScreen(Screen lastScreen, Minecraft minecraft, Options options) {
        super(lastScreen, options, TITLE);
        this.gpuWarnlistManager = minecraft.getGpuWarnlistManager();
        this.gpuWarnlistManager.resetWarnings();
        if (options.improvedTransparency().get().booleanValue()) {
            this.gpuWarnlistManager.dismissWarning();
        }
        this.oldMipmaps = options.mipmapLevels().get();
        this.oldAnisotropyBit = options.maxAnisotropyBit().get();
        this.oldTextureFiltering = options.textureFiltering().get();
    }

    @Override
    protected void addOptions() {
        int initialValue;
        int CURRENT_MODE = -1;
        Window window = this.minecraft.getWindow();
        Monitor monitor = window.findBestMonitor();
        if (monitor == null) {
            initialValue = -1;
        } else {
            Optional<VideoMode> preferredFullscreenVideoMode = window.getPreferredFullscreenVideoMode();
            initialValue = preferredFullscreenVideoMode.map(monitor::indexOfMode).orElse(-1);
        }
        OptionInstance<Integer> fullscreenOption = new OptionInstance<Integer>("options.fullscreen.resolution", OptionInstance.noTooltip(), (caption, value) -> {
            if (monitor == null) {
                return Component.translatable("options.fullscreen.unavailable");
            }
            if (value == -1) {
                return Options.genericValueLabel(caption, Component.translatable("options.fullscreen.current"));
            }
            VideoMode mode = monitor.mode((int)value);
            return Options.genericValueLabel(caption, Component.translatable("options.fullscreen.entry", mode.getWidth(), mode.getHeight(), mode.getRefreshRate(), mode.getRedBits() + mode.getGreenBits() + mode.getBlueBits()));
        }, new OptionInstance.IntRange(-1, monitor != null ? monitor.modeCount() - 1 : -1), initialValue, value -> {
            if (monitor == null) {
                return;
            }
            window.setPreferredFullscreenVideoMode(value == -1 ? Optional.empty() : Optional.of(monitor.mode((int)value)));
        });
        this.list.addHeader(DISPLAY_HEADER);
        this.list.addBig(fullscreenOption);
        this.list.addSmall(VideoSettingsScreen.displayOptions(this.options));
        this.list.addHeader(QUALITY_HEADER);
        this.list.addBig(this.options.graphicsPreset());
        this.list.addSmall(VideoSettingsScreen.qualityOptions(this.options));
        this.list.addHeader(PREFERENCES_HEADER);
        this.list.addSmall(VideoSettingsScreen.preferenceOptions(this.options));
    }

    @Override
    protected void addTitle() {
        this.header.defaultCellSetting().alignHorizontallyCenter().alignVerticallyMiddle();
        this.header.addChild(new StringWidget(this.title, this.font));
        if (this.options.isRestartRequiredToApplyVideoSettings()) {
            this.restartWarning = new StringWidget(RESTART_REQUIRED, this.font);
            this.header.addChild(this.restartWarning);
        }
        this.layout.addToHeader(this.header);
    }

    @Override
    public void tick() {
        boolean restartRequired;
        AbstractWidget abstractWidget;
        if (this.list != null && (abstractWidget = this.list.findOption(this.options.maxAnisotropyBit())) instanceof AbstractSliderButton) {
            AbstractSliderButton maxAnisotropy = (AbstractSliderButton)abstractWidget;
            boolean bl = maxAnisotropy.active = this.options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC;
        }
        if ((restartRequired = this.options.isRestartRequiredToApplyVideoSettings()) && (this.restartWarning == null || !this.restartWarning.visible)) {
            if (this.restartWarning == null) {
                this.restartWarning = new StringWidget(RESTART_REQUIRED, this.font);
                this.header.addChild(this.restartWarning);
                this.addRenderableWidget(this.restartWarning);
            }
            this.restartWarning.visible = true;
            this.repositionElements();
        } else if (!restartRequired && this.restartWarning != null && this.restartWarning.visible) {
            this.restartWarning.visible = false;
            this.repositionElements();
        }
        super.tick();
    }

    @Override
    public void onClose() {
        this.minecraft.getWindow().changeFullscreenVideoMode();
        super.onClose();
    }

    @Override
    public void removed() {
        if (this.options.mipmapLevels().get() != this.oldMipmaps || this.options.maxAnisotropyBit().get() != this.oldAnisotropyBit || this.options.textureFiltering().get() != this.oldTextureFiltering) {
            this.minecraft.updateMaxMipLevel(this.options.mipmapLevels().get());
            this.minecraft.delayTextureReload();
        }
        super.removed();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            if (this.gpuWarnlistManager.isShowingWarning()) {
                String versionWarnings;
                String vendorWarnings;
                ArrayList warningMessage = Lists.newArrayList((Object[])new Component[]{WARNING_MESSAGE, CommonComponents.NEW_LINE});
                String rendererWarnings = this.gpuWarnlistManager.getRendererWarnings();
                if (rendererWarnings != null) {
                    warningMessage.add(CommonComponents.NEW_LINE);
                    warningMessage.add(Component.translatable("options.graphics.warning.renderer", rendererWarnings).withStyle(ChatFormatting.GRAY));
                }
                if ((vendorWarnings = this.gpuWarnlistManager.getVendorWarnings()) != null) {
                    warningMessage.add(CommonComponents.NEW_LINE);
                    warningMessage.add(Component.translatable("options.graphics.warning.vendor", vendorWarnings).withStyle(ChatFormatting.GRAY));
                }
                if ((versionWarnings = this.gpuWarnlistManager.getVersionWarnings()) != null) {
                    warningMessage.add(CommonComponents.NEW_LINE);
                    warningMessage.add(Component.translatable("options.graphics.warning.version", versionWarnings).withStyle(ChatFormatting.GRAY));
                }
                this.minecraft.gui.setScreen(new UnsupportedGraphicsWarningScreen(WARNING_TITLE, warningMessage, (ImmutableList<UnsupportedGraphicsWarningScreen.ButtonOption>)ImmutableList.of((Object)new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_ACCEPT, btn -> {
                    this.options.improvedTransparency().set(true);
                    Minecraft.getInstance().levelExtractor.allChanged();
                    this.gpuWarnlistManager.dismissWarning();
                    this.minecraft.gui.setScreen(this);
                }), (Object)new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_CANCEL, btn -> {
                    this.gpuWarnlistManager.dismissWarning();
                    this.options.improvedTransparency().set(false);
                    this.updateTransparencyButton();
                    this.minecraft.gui.setScreen(this);
                }))));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.minecraft.hasControlDown()) {
            OptionInstance<Integer> guiScale = this.options.guiScale();
            OptionInstance.ValueSet<Integer> valueSet = guiScale.values();
            if (valueSet instanceof OptionInstance.ClampingLazyMaxIntRange) {
                CycleButton cycleButton;
                OptionInstance.ClampingLazyMaxIntRange clampingLazyMaxIntRange = (OptionInstance.ClampingLazyMaxIntRange)valueSet;
                int oldValue = guiScale.get();
                int adjustedOldValue = oldValue == 0 ? clampingLazyMaxIntRange.maxInclusive() + 1 : oldValue;
                int newValue = adjustedOldValue + (int)Math.signum(scrollY);
                if (newValue != 0 && newValue <= clampingLazyMaxIntRange.maxInclusive() && newValue >= clampingLazyMaxIntRange.minInclusive() && (cycleButton = (CycleButton)this.list.findOption(guiScale)) != null) {
                    guiScale.set(newValue);
                    cycleButton.setValue(newValue);
                    this.list.setScrollAmount(0.0);
                    return true;
                }
            }
            return false;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    public void updateFullscreenButton(boolean fullscreen) {
        AbstractWidget fullscreenWidget;
        if (this.list != null && (fullscreenWidget = this.list.findOption(this.options.fullscreen())) != null) {
            CycleButton fullscreenButton = (CycleButton)fullscreenWidget;
            fullscreenButton.setValue(fullscreen);
        }
    }

    public void updateTransparencyButton() {
        OptionInstance<Boolean> option;
        AbstractWidget widget;
        if (this.list != null && (widget = this.list.findOption(option = this.options.improvedTransparency())) != null) {
            CycleButton button = (CycleButton)widget;
            button.setValue(option.get());
        }
    }
}

