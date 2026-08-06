/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.text2speech.Narrator
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens;

import com.mojang.text2speech.Narrator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class AccessibilityOnboardingScreen
extends Screen {
    private static final Component TITLE = Component.translatable("accessibility.onboarding.screen.title");
    private static final Component ONBOARDING_NARRATOR_MESSAGE = Component.translatable("accessibility.onboarding.screen.narrator");
    private static final int PADDING = 4;
    private static final int TITLE_PADDING = 16;
    private static final float FADE_OUT_TIME = 1000.0f;
    private static final int TEXT_WIDGET_WIDTH = 374;
    private final LogoRenderer logoRenderer;
    private final Options options;
    private final boolean narratorAvailable;
    private boolean hasNarrated;
    private float timer;
    private final Runnable onClose;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, this.initTitleYPos(), 33);
    private @Nullable FocusableTextWidget focusableTextWidget;
    private float fadeInStart;
    private boolean fadingIn = true;
    private float fadeOutStart;

    public AccessibilityOnboardingScreen(Options options, Runnable onClose) {
        super(TITLE);
        this.options = options;
        this.onClose = onClose;
        this.logoRenderer = new LogoRenderer(true);
        this.narratorAvailable = Minecraft.getInstance().getNarrator().isActive();
        this.minecraft.gameRenderer.panorama().holdSpin();
    }

    @Override
    public void init() {
        LinearLayout content = this.layout.addToContents(LinearLayout.vertical());
        content.defaultCellSetting().alignHorizontallyCenter().padding(4);
        this.focusableTextWidget = content.addChild(FocusableTextWidget.builder(this.title, this.font).maxWidth(374).build(), w -> w.padding(8));
        GridLayout grid = content.addChild(new GridLayout());
        grid.defaultCellSetting().padding(4);
        GridLayout.RowHelper rowHelper = grid.createRowHelper(2);
        AbstractWidget abstractWidget = this.options.narrator().createButton(this.options);
        if (abstractWidget instanceof CycleButton) {
            CycleButton cycleButton;
            this.narratorButton = cycleButton = (CycleButton)abstractWidget;
            this.narratorButton.active = this.narratorAvailable;
            rowHelper.addChild(this.narratorButton);
        }
        rowHelper.addChild(SpriteIconButton.builder(Component.translatable("options.sounds"), button -> this.closeAndSetScreen(new SoundOptionsScreen(this, this.options)), false).width(150).sprite(Identifier.withDefaultNamespace("icon/music_notes"), 16, 16).build());
        rowHelper.addChild(CommonButtons.accessibility(150, button -> this.closeAndSetScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), false));
        rowHelper.addChild(CommonButtons.language(150, button -> this.closeAndSetScreen(new LanguageSelectScreen((Screen)this, this.minecraft.options, this.minecraft.getLanguageManager())), false));
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_CONTINUE, button -> this.onClose()).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (this.focusableTextWidget != null) {
            this.focusableTextWidget.updateHeight();
        }
        this.layout.arrangeElements();
    }

    @Override
    protected void setInitialFocus() {
        if (this.narratorAvailable && this.narratorButton != null) {
            this.setInitialFocus(this.narratorButton);
        } else {
            super.setInitialFocus();
        }
    }

    private int initTitleYPos() {
        return 90;
    }

    @Override
    public void onClose() {
        if (this.fadeOutStart == 0.0f) {
            this.fadeOutStart = Util.getMillis();
        }
    }

    private void closeAndSetScreen(Screen screen) {
        this.close(false, () -> this.minecraft.gui.setScreen(screen));
    }

    private void close(boolean onboardingFinished, Runnable runnable) {
        if (onboardingFinished) {
            this.options.onboardingAccessibilityFinished();
        }
        Narrator.getNarrator().clear();
        runnable.run();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        float widgetAlpha;
        float fade;
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.handleInitialNarrationDelay();
        if (this.fadeInStart == 0.0f && this.fadingIn) {
            this.fadeInStart = Util.getMillis();
        }
        if (this.fadeInStart > 0.0f) {
            fade = ((float)Util.getMillis() - this.fadeInStart) / 2000.0f;
            widgetAlpha = 1.0f;
            if (fade >= 1.0f) {
                this.fadingIn = false;
                this.fadeInStart = 0.0f;
            } else {
                fade = Mth.clamp(fade, 0.0f, 1.0f);
                widgetAlpha = Mth.clampedMap(fade, 0.5f, 1.0f, 0.0f, 1.0f);
            }
            this.fadeWidgets(widgetAlpha);
        }
        if (this.fadeOutStart > 0.0f) {
            fade = 1.0f - ((float)Util.getMillis() - this.fadeOutStart) / 1000.0f;
            widgetAlpha = 0.0f;
            if (fade <= 0.0f) {
                this.fadeOutStart = 0.0f;
                this.close(true, this.onClose);
            } else {
                fade = Mth.clamp(fade, 0.0f, 1.0f);
                widgetAlpha = Mth.clampedMap(fade, 0.5f, 1.0f, 0.0f, 1.0f);
            }
            this.fadeWidgets(widgetAlpha);
        }
        this.logoRenderer.extractRenderState(graphics, this.width, 1.0f);
    }

    private void handleInitialNarrationDelay() {
        if (!this.hasNarrated && this.narratorAvailable) {
            if (this.timer < 40.0f) {
                this.timer += 1.0f;
            } else if (this.minecraft.isWindowActive()) {
                Narrator.getNarrator().say(ONBOARDING_NARRATOR_MESSAGE.getString(), true, this.minecraft.options.getFinalSoundSourceVolume(SoundSource.VOICE));
                this.hasNarrated = true;
            }
        }
    }
}

