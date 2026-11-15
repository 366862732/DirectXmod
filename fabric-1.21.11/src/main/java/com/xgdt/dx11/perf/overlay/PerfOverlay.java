package com.xgdt.dx11.perf.overlay;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.xgdt.dx11.perf.PerfState;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Clean, compact FPS/CPU/GPU overlay.
 *
 * <p>The visual language stays deliberately close to the vanilla/Legacy GUI:
 * flat fills, one-pixel borders, the vanilla font and no texture assets.  The
 * hierarchy and spacing are modern: a title row, aligned metric columns and a
 * single coloured bottleneck badge.</p>
 */
@Environment(EnvType.CLIENT)
public final class PerfOverlay {
    private static final int PANEL_WIDTH = 232;
    private static final int PANEL_HEIGHT = 108;
    private static final int PAD = 6;

    private static final int BG = 0xE612161A;
    private static final int BORDER = 0xFF2C343C;
    private static final int ACCENT = 0xFFE0A64A;
    private static final int TEXT = 0xFFF3F6F8;
    private static final int MUTED = 0xFF8D98A3;
    private static final int SEPARATOR = 0x33FFFFFF;
    private static final int BADGE_BG = 0x24FFFFFF;
    private static final int BADGE_BORDER = 0xFF3A444E;
    private static final int CPU_COLOR = 0xFFE0A64A;
    private static final int GPU_COLOR = 0xFF63C7D6;
    private static final int VSYNC_COLOR = 0xFF9AA4AE;
    private static final int WARN = 0xFFE06C75;

    private PerfOverlay() {
    }

    /** True when the overlay should draw in the current frame. */
    public static boolean isVisible() {
        try {
            if (!PerfState.layerActive()) {
                return false;
            }
            if (!PerfState.config().debugOverlay) {
                return false;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.options == null || minecraft.options.hideGui) {
                return false;
            }
            return minecraft.getDebugOverlay() == null
                || !minecraft.getDebugOverlay().showDebugScreen();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Draws the panel at the top-left of the GUI. */
    public static void render(GuiGraphics graphics) {
        if (!isVisible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int x = 6;
        int y = 6;
        int right = x + PANEL_WIDTH;

        // A soft one-pixel shadow keeps the panel attached to the world without
        // a heavy box or a gradient.
        graphics.fill(x + 2, y + 2, right + 2, y + PANEL_HEIGHT + 2, 0x40000000);
        graphics.fill(x, y, right, y + PANEL_HEIGHT, BORDER);
        graphics.fill(x + 1, y + 1, right - 1, y + PANEL_HEIGHT - 1, BG);
        graphics.fill(x + 1, y + 1, right - 1, y + 3, ACCENT);

        int left = x + PAD + 2;
        int valueColumn = left + 48;
        int detailColumn = left + 118;
        int top = y + 7;

        text(graphics, font, "DX11 // 1.21.11", left, top, ACCENT);
        String fps = PerfOverlayStats.fps() + " FPS";
        text(graphics, font, fps, right - PAD - 2 - font.width(fps), top, fpsColor());

        int row = top + 13;
        label(graphics, font, "FRAME", left, row);
        text(graphics, font, decimal(PerfOverlayStats.frameMs()) + " ms", valueColumn, row, TEXT);
        String max = "max " + decimal(PerfOverlayStats.frameMaxMs()) + " ms";
        text(graphics, font, max, right - PAD - 2 - font.width(max), row, MUTED);

        row += 11;
        label(graphics, font, "CPU", left, row);
        text(graphics, font, percent(PerfOverlayStats.cpuLoad()), valueColumn, row, TEXT);
        text(
            graphics, font,
            "render " + decimal(PerfOverlayStats.cpuMs()) + " ms",
            detailColumn, row, MUTED);

        row += 11;
        label(graphics, font, "GPU", left, row);
        if (PerfOverlayStats.gpuAvailable()) {
            text(graphics, font, percent(PerfOverlayStats.gpuShare()), valueColumn, row, TEXT);
            text(
                graphics, font,
                "frame " + decimal(PerfOverlayStats.gpuMs()) + " ms",
                detailColumn, row, MUTED);
        } else {
            text(graphics, font, "n/a", valueColumn, row, MUTED);
            text(
                graphics, font,
                "wait " + decimal(PerfOverlayStats.gpuWaitMs()) + " ms",
                detailColumn, row, MUTED);
        }

        row += 11;
        label(graphics, font, "DRAWS", left, row);
        text(
            graphics, font,
            String.format(Locale.ROOT, "%,.0f/f", PerfOverlayStats.drawsPerFrame()),
            valueColumn, row, TEXT);
        String upload = decimal(PerfOverlayStats.uploadMiBPerSecond()) + " MiB/s";
        text(graphics, font, "up " + upload, right - PAD - 2 - font.width("up " + upload), row, MUTED);

        row += 11;
        text(graphics, font, adapterLine(), left, row, MUTED, right - PAD - 2);
        row += 10;
        text(graphics, font, backendLine(), left, row, MUTED, right - PAD - 2);

        int separator = y + 86;
        graphics.fill(x + PAD + 2, separator, right - PAD - 2, separator + 1, SEPARATOR);

        int badgeTop = separator + 5;
        int badgeBottom = badgeTop + 14;
        graphics.fill(x + PAD, badgeTop, right - PAD, badgeBottom, BADGE_BORDER);
        graphics.fill(x + PAD + 1, badgeTop + 1, right - PAD - 1, badgeBottom - 1, BADGE_BG);

        PerfOverlayStats.Bottleneck bottleneck = PerfOverlayStats.bottleneck();
        int color = bottleneckColor(bottleneck);
        graphics.fill(x + PAD + 5, badgeTop + 5, x + PAD + 9, badgeTop + 9, color);
        text(graphics, font, "BOTTLENECK", x + PAD + 14, badgeTop + 3, MUTED);
        text(graphics, font, bottleneck.label(), x + PAD + 82, badgeTop + 3, color);
        String reason = bottleneckReason(bottleneck);
        text(
            graphics, font, reason,
            right - PAD - 5 - font.width(reason), badgeTop + 3, MUTED);
    }

    private static void label(GuiGraphics graphics, Font font, String value, int x, int y) {
        text(graphics, font, value, x, y, MUTED);
    }

    private static void text(GuiGraphics graphics, Font font, String value, int x, int y, int color) {
        graphics.drawString(font, value, x, y, color, false);
    }

    private static void text(
        GuiGraphics graphics, Font font, String value, int x, int y, int color, int maxWidth) {
        graphics.drawString(font, trim(font, value, maxWidth), x, y, color, false);
    }

    private static String trim(Font font, String value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String ellipsis = "...";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return value.substring(0, end) + ellipsis;
    }

    private static String percent(double fraction) {
        return PerfOverlayStats.formatPercent(fraction);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static int fpsColor() {
        int fps = PerfOverlayStats.fps();
        if (fps < 25) {
            return WARN;
        }
        return fps < 45 ? ACCENT : TEXT;
    }

    private static int bottleneckColor(PerfOverlayStats.Bottleneck bottleneck) {
        return switch (bottleneck) {
            case CPU -> CPU_COLOR;
            case GPU -> GPU_COLOR;
            case VSYNC -> VSYNC_COLOR;
        };
    }

    private static String bottleneckReason(PerfOverlayStats.Bottleneck bottleneck) {
        return switch (bottleneck) {
            case CPU -> String.format(
                Locale.ROOT, "%.1f / %.1f ms",
                PerfOverlayStats.cpuMs(), PerfOverlayStats.frameMs());
            case GPU -> PerfOverlayStats.gpuAvailable()
                ? String.format(
                    Locale.ROOT, "%.1f / %.1f ms",
                    PerfOverlayStats.gpuMs(), PerfOverlayStats.frameMs())
                : String.format(
                    Locale.ROOT, "wait %.1f ms", PerfOverlayStats.gpuWaitMs());
            case VSYNC -> "frame cap";
        };
    }

    private static String adapterLine() {
        try {
            GpuDevice device = RenderSystem.getDevice();
            if (device == null) {
                return "No graphics device";
            }
            String renderer = device.getRenderer();
            int separator = renderer.indexOf(" - ");
            return separator >= 0 ? renderer.substring(separator + 3) : renderer;
        } catch (Throwable ignored) {
            return "No graphics device";
        }
    }

    private static String backendLine() {
        String backend = "D3D11";
        String version = "";
        int width = 0;
        int height = 0;
        try {
            GpuDevice device = RenderSystem.getDevice();
            if (device != null) {
                backend = device.getBackendName();
                version = shortVersion(device.getVersion());
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getWindow() != null) {
                width = minecraft.getWindow().getWidth();
                height = minecraft.getWindow().getHeight();
            }
        } catch (Throwable ignored) {
        }
        String suffix = version.isEmpty() ? "" : " " + version;
        return backend + suffix + " · " + width + "x" + height;
    }

    private static String shortVersion(String version) {
        if (version == null) {
            return "";
        }
        return version.startsWith("D3D_FEATURE_LEVEL_")
            ? "FL" + version.substring("D3D_FEATURE_LEVEL_".length())
            : version;
    }
}
