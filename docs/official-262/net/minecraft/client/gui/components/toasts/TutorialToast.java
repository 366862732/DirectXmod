/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.toasts;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TutorialToast
implements Toast {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/tutorial");
    public static final int PROGRESS_BAR_WIDTH = 154;
    public static final int PROGRESS_BAR_HEIGHT = 1;
    public static final int PROGRESS_BAR_X = 3;
    public static final int PROGRESS_BAR_MARGIN_BOTTOM = 4;
    private static final int PADDING_TOP = 7;
    private static final int PADDING_BOTTOM = 3;
    private static final int LINE_SPACING = 11;
    private static final int TEXT_LEFT = 30;
    private static final int TEXT_WIDTH = 126;
    private final Icons icon;
    private final List<FormattedCharSequence> lines;
    private Toast.Visibility visibility = Toast.Visibility.SHOW;
    private long lastSmoothingTime;
    private float smoothedProgress;
    private float progress;
    private final boolean progressable;
    private final int timeToDisplayMs;

    public TutorialToast(Font font, Icons icon, Component title, @Nullable Component message, boolean progressable, int timeToDisplayMs) {
        this.icon = icon;
        this.lines = new ArrayList<FormattedCharSequence>(2);
        this.lines.addAll(font.split(title.copy().withColor(-11534256), 126));
        if (message != null) {
            this.lines.addAll(font.split(message, 126));
        }
        this.progressable = progressable;
        this.timeToDisplayMs = timeToDisplayMs;
    }

    public TutorialToast(Font font, Icons icon, Component title, @Nullable Component message, boolean progressable) {
        this(font, icon, title, message, progressable, 0);
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.visibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        if (this.timeToDisplayMs > 0) {
            this.smoothedProgress = this.progress = Math.min((float)fullyVisibleForMs / (float)this.timeToDisplayMs, 1.0f);
            this.lastSmoothingTime = fullyVisibleForMs;
            if (fullyVisibleForMs > (long)this.timeToDisplayMs) {
                this.hide();
            }
        } else if (this.progressable) {
            this.smoothedProgress = Mth.clampedLerp((float)(fullyVisibleForMs - this.lastSmoothingTime) / 100.0f, this.smoothedProgress, this.progress);
            this.lastSmoothingTime = fullyVisibleForMs;
        }
    }

    @Override
    public int height() {
        return 7 + this.contentHeight() + 3;
    }

    private int contentHeight() {
        return Math.max(this.lines.size(), 2) * 11;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        int height = this.height();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), height);
        this.icon.extractRenderState(graphics, 6, 6);
        int textHeight = this.lines.size() * 11;
        int textTop = 7 + (this.contentHeight() - textHeight) / 2;
        for (int i = 0; i < this.lines.size(); ++i) {
            graphics.text(font, this.lines.get(i), 30, textTop + i * 11, -16777216, false);
        }
        if (this.progressable) {
            int progressBarY = height - 4;
            graphics.fill(3, progressBarY, 157, progressBarY + 1, -1);
            int col = this.progress >= this.smoothedProgress ? -16755456 : -11206656;
            graphics.fill(3, progressBarY, (int)(3.0f + 154.0f * this.smoothedProgress), progressBarY + 1, col);
        }
    }

    public void hide() {
        this.visibility = Toast.Visibility.HIDE;
    }

    public void updateProgress(float progress) {
        this.progress = progress;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Icons {
        MOVEMENT_KEYS(Identifier.withDefaultNamespace("toast/movement_keys")),
        MOUSE(Identifier.withDefaultNamespace("toast/mouse")),
        TREE(Identifier.withDefaultNamespace("toast/tree")),
        RECIPE_BOOK(Identifier.withDefaultNamespace("toast/recipe_book")),
        WOODEN_PLANKS(Identifier.withDefaultNamespace("toast/wooden_planks")),
        SOCIAL_INTERACTIONS(Identifier.withDefaultNamespace("toast/social_interactions")),
        RIGHT_CLICK(Identifier.withDefaultNamespace("toast/right_click"));

        private final Identifier sprite;

        private Icons(Identifier sprite) {
            this.sprite = sprite;
        }

        public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, x, y, 20, 20);
        }
    }
}

