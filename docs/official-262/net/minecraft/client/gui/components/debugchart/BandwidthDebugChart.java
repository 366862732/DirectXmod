/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.debugchart.AbstractDebugChart;
import net.minecraft.util.Mth;
import net.minecraft.util.debugchart.SampleStorage;

@Environment(value=EnvType.CLIENT)
public class BandwidthDebugChart
extends AbstractDebugChart {
    private static final int MIN_COLOR = -16711681;
    private static final int MID_COLOR = -6250241;
    private static final int MAX_COLOR = -65536;
    private static final int KILOBYTE = 1024;
    private static final int MEGABYTE = 0x100000;
    private static final int CHART_TOP_VALUE = 0x100000;

    public BandwidthDebugChart(Font font, SampleStorage sampleStorage) {
        super(font, sampleStorage);
    }

    @Override
    protected void extractAdditionalLinesAndLabels(GuiGraphicsExtractor graphics, int left, int width, int bottom) {
        this.extractLabeledLineAtValue(graphics, left, width, bottom, 64);
        this.extractLabeledLineAtValue(graphics, left, width, bottom, 1024);
        this.extractLabeledLineAtValue(graphics, left, width, bottom, 16384);
        this.extractStringWithShade(graphics, BandwidthDebugChart.toDisplayStringInternal(1048576.0), left + 1, bottom - BandwidthDebugChart.getSampleHeightInternal(1048576.0) + 1);
    }

    private void extractLabeledLineAtValue(GuiGraphicsExtractor graphics, int left, int width, int bottom, int bytesPerSecond) {
        this.extractLineWithLabel(graphics, left, width, bottom - BandwidthDebugChart.getSampleHeightInternal(bytesPerSecond), BandwidthDebugChart.toDisplayStringInternal(bytesPerSecond));
    }

    private void extractLineWithLabel(GuiGraphicsExtractor graphics, int x, int width, int y, String label) {
        this.extractStringWithShade(graphics, label, x + 1, y + 1);
        graphics.horizontalLine(x, x + width - 1, y, -1);
    }

    @Override
    protected String toDisplayString(double bytesPerTick) {
        return BandwidthDebugChart.toDisplayStringInternal(BandwidthDebugChart.toBytesPerSecond(bytesPerTick));
    }

    private static String toDisplayStringInternal(double bytesPerSecond) {
        if (bytesPerSecond >= 1048576.0) {
            return String.format(Locale.ROOT, "%.1f MiB/s", bytesPerSecond / 1048576.0);
        }
        if (bytesPerSecond >= 1024.0) {
            return String.format(Locale.ROOT, "%.1f KiB/s", bytesPerSecond / 1024.0);
        }
        return String.format(Locale.ROOT, "%d B/s", Mth.floor(bytesPerSecond));
    }

    @Override
    protected int getSampleHeight(double bytesPerTick) {
        return BandwidthDebugChart.getSampleHeightInternal(BandwidthDebugChart.toBytesPerSecond(bytesPerTick));
    }

    private static int getSampleHeightInternal(double bytesPerSecond) {
        return (int)Math.round(Math.log(bytesPerSecond + 1.0) * 60.0 / Math.log(1048576.0));
    }

    @Override
    protected int getSampleColor(long bytesPerTick) {
        return this.getSampleColor(BandwidthDebugChart.toBytesPerSecond(bytesPerTick), 0.0, -16711681, 8192.0, -6250241, 1.048576E7, -65536);
    }

    private static double toBytesPerSecond(double bytesPerTick) {
        return bytesPerTick * 20.0;
    }
}

