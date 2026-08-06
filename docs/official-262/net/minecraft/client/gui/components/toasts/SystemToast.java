/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SystemToast
implements Toast {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/system");
    private static final int MAX_LINE_SIZE = 200;
    private static final int LINE_SPACING = 12;
    private static final int MARGIN = 10;
    private static final int TEXT_X_START = 18;
    private final SystemToastId id;
    private List<FormattedCharSequence> titleLines;
    private List<FormattedCharSequence> messageLines;
    private long lastChanged;
    private boolean changed;
    private int width;
    private boolean forceHide;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    public SystemToast(SystemToastId id, Component title, @Nullable Component message) {
        this.id = id;
        this.update(title, message);
    }

    private static List<FormattedCharSequence> nullToEmpty(@Nullable Component text) {
        return text == null ? ImmutableList.of() : SystemToast.splitToLength(text);
    }

    private static List<FormattedCharSequence> splitToLength(Component text) {
        return Minecraft.getInstance().font.split(text, 200);
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        int titleHeight = (this.titleLines.size() - 1) * 12;
        int messageHeight = Math.max(this.messageLines.size(), 1) * 12;
        return 20 + titleHeight + messageHeight;
    }

    public void forceHide() {
        this.forceHide = true;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        if (this.changed) {
            this.lastChanged = fullyVisibleForMs;
            this.changed = false;
        }
        double timeToDisplayUpdate = (double)this.id.displayTime * manager.getNotificationDisplayTimeMultiplier();
        long timeSinceUpdate = fullyVisibleForMs - this.lastChanged;
        this.wantedVisibility = !this.forceHide && (double)timeSinceUpdate < timeToDisplayUpdate ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
        if (this.messageLines.isEmpty()) {
            this.extractTextLines(graphics, font, this.titleLines, 12, -256);
        } else {
            this.extractTextLines(graphics, font, this.titleLines, 7, -256);
            this.extractTextLines(graphics, font, this.messageLines, 7 + this.titleLines.size() * 12, -1);
        }
    }

    private void extractTextLines(GuiGraphicsExtractor graphics, Font font, List<FormattedCharSequence> textLines, int yStart, int textColor) {
        for (int i = 0; i < textLines.size(); ++i) {
            graphics.text(font, textLines.get(i), 18, yStart + i * 12, textColor, false);
        }
    }

    public void reset(Component title, @Nullable Component message) {
        this.update(title, message);
        this.changed = true;
    }

    private void update(Component title, @Nullable Component message) {
        this.titleLines = SystemToast.splitToLength(title);
        this.messageLines = SystemToast.nullToEmpty(message);
        this.recalculateWidth();
    }

    public void recalculateWidth() {
        int width = Math.max(160, Stream.concat(this.titleLines.stream(), this.messageLines.stream()).mapToInt(Minecraft.getInstance().font::width).max().orElse(200));
        this.width = width + 30;
    }

    @Override
    public SystemToastId getToken() {
        return this.id;
    }

    public static void add(ToastManager toastManager, SystemToastId id, Component title, @Nullable Component message) {
        toastManager.addToast(new SystemToast(id, title, message));
    }

    public static void addOrUpdate(ToastManager toastManager, SystemToastId id, Component title, @Nullable Component message) {
        SystemToast toast = toastManager.getToast(SystemToast.class, id);
        if (toast == null) {
            SystemToast.add(toastManager, id, title, message);
        } else {
            toast.reset(title, message);
        }
    }

    public static void forceHide(ToastManager toastManager, SystemToastId id) {
        SystemToast toast = toastManager.getToast(SystemToast.class, id);
        if (toast != null) {
            toast.forceHide();
        }
    }

    public static void onWorldAccessFailure(Minecraft minecraft, String levelId) {
        SystemToast.add(minecraft.gui.toastManager(), SystemToastId.WORLD_ACCESS_FAILURE, Component.translatable("selectWorld.access_failure"), Component.literal(levelId));
    }

    public static void onWorldDeleteFailure(Minecraft minecraft, String levelId) {
        SystemToast.add(minecraft.gui.toastManager(), SystemToastId.WORLD_ACCESS_FAILURE, Component.translatable("selectWorld.delete_failure"), Component.literal(levelId));
    }

    public static void onPackCopyFailure(Minecraft minecraft, String extraInfo) {
        SystemToast.add(minecraft.gui.toastManager(), SystemToastId.PACK_COPY_FAILURE, Component.translatable("pack.copyFailure"), Component.literal(extraInfo));
    }

    public static void onFileDropFailure(Minecraft minecraft, int count) {
        SystemToast.add(minecraft.gui.toastManager(), SystemToastId.FILE_DROP_FAILURE, Component.translatable("gui.fileDropFailure.title"), Component.translatable("gui.fileDropFailure.detail", count));
    }

    public static void onLowDiskSpace(Minecraft minecraft) {
        SystemToast.addOrUpdate(minecraft.gui.toastManager(), SystemToastId.LOW_DISK_SPACE, Component.translatable("chunk.toast.lowDiskSpace"), Component.translatable("chunk.toast.lowDiskSpace.description"));
    }

    public static void onChunkLoadFailure(Minecraft minecraft, ChunkPos pos) {
        SystemToast.addOrUpdate(minecraft.gui.toastManager(), SystemToastId.CHUNK_LOAD_FAILURE, Component.translatable("chunk.toast.loadFailure", Component.translationArg(pos)).withStyle(ChatFormatting.RED), Component.translatable("chunk.toast.checkLog"));
    }

    public static void onChunkSaveFailure(Minecraft minecraft, ChunkPos pos) {
        SystemToast.addOrUpdate(minecraft.gui.toastManager(), SystemToastId.CHUNK_SAVE_FAILURE, Component.translatable("chunk.toast.saveFailure", Component.translationArg(pos)).withStyle(ChatFormatting.RED), Component.translatable("chunk.toast.checkLog"));
    }

    @Environment(value=EnvType.CLIENT)
    public static class SystemToastId {
        public static final SystemToastId NARRATOR_TOGGLE = new SystemToastId();
        public static final SystemToastId WORLD_BACKUP = new SystemToastId();
        public static final SystemToastId PACK_LOAD_FAILURE = new SystemToastId();
        public static final SystemToastId WORLD_ACCESS_FAILURE = new SystemToastId();
        public static final SystemToastId PACK_COPY_FAILURE = new SystemToastId();
        public static final SystemToastId FILE_DROP_FAILURE = new SystemToastId();
        public static final SystemToastId PERIODIC_NOTIFICATION = new SystemToastId();
        public static final SystemToastId LOW_DISK_SPACE = new SystemToastId(10000L);
        public static final SystemToastId CHUNK_LOAD_FAILURE = new SystemToastId();
        public static final SystemToastId CHUNK_SAVE_FAILURE = new SystemToastId();
        public static final SystemToastId UNSECURE_SERVER_WARNING = new SystemToastId(10000L);
        public static final SystemToastId FRIEND_SYSTEM_NOTIFICATION = new SystemToastId();
        private final long displayTime;

        public SystemToastId(long displayTime) {
            this.displayTime = displayTime;
        }

        public SystemToastId() {
            this(5000L);
        }
    }
}

