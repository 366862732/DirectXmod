/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.debugchart.AbstractDebugChart;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.debugchart.SampleStorage;
import net.minecraft.util.debugchart.TpsDebugDimensions;

@Environment(value=EnvType.CLIENT)
public class TpsDebugChart
extends AbstractDebugChart {
    private static final int TICK_METHOD_COLOR = -6745839;
    private static final int TASK_COLOR = -4548257;
    private static final int OTHER_COLOR = -10547572;
    private final Supplier<Float> msptSupplier;

    public TpsDebugChart(Font font, SampleStorage sampleStorage, Supplier<Float> msptSupplier) {
        super(font, sampleStorage);
        this.msptSupplier = msptSupplier;
    }

    @Override
    protected void extractAdditionalLinesAndLabels(GuiGraphicsExtractor graphics, int left, int width, int bottom) {
        float tps = (float)TimeUtil.MILLISECONDS_PER_SECOND / this.msptSupplier.get().floatValue();
        this.extractStringWithShade(graphics, String.format(Locale.ROOT, "%.1f TPS", Float.valueOf(tps)), left + 1, bottom - 60 + 1);
    }

    @Override
    protected void extractAdditionalSampleBars(GuiGraphicsExtractor graphics, int bottom, int currentX, int sampleIndex) {
        long tickMethodTime = this.sampleStorage.get(sampleIndex, TpsDebugDimensions.TICK_SERVER_METHOD.ordinal());
        int tickMethodHeight = this.getSampleHeight(tickMethodTime);
        graphics.fill(currentX, bottom - tickMethodHeight, currentX + 1, bottom, -6745839);
        long tasksTime = this.sampleStorage.get(sampleIndex, TpsDebugDimensions.SCHEDULED_TASKS.ordinal());
        int tasksHeight = this.getSampleHeight(tasksTime);
        graphics.fill(currentX, bottom - tickMethodHeight - tasksHeight, currentX + 1, bottom - tickMethodHeight, -4548257);
        long otherTime = this.sampleStorage.get(sampleIndex) - this.sampleStorage.get(sampleIndex, TpsDebugDimensions.IDLE.ordinal()) - tickMethodTime - tasksTime;
        int otherHeight = this.getSampleHeight(otherTime);
        graphics.fill(currentX, bottom - otherHeight - tasksHeight - tickMethodHeight, currentX + 1, bottom - tasksHeight - tickMethodHeight, -10547572);
    }

    @Override
    protected long getValueForAggregation(int sampleIndex) {
        return this.sampleStorage.get(sampleIndex) - this.sampleStorage.get(sampleIndex, TpsDebugDimensions.IDLE.ordinal());
    }

    @Override
    protected String toDisplayString(double nanos) {
        return String.format(Locale.ROOT, "%d ms", (int)Math.round(TpsDebugChart.toMilliseconds(nanos)));
    }

    @Override
    protected int getSampleHeight(double nanos) {
        return (int)Math.round(TpsDebugChart.toMilliseconds(nanos) * 60.0 / (double)this.msptSupplier.get().floatValue());
    }

    @Override
    protected int getSampleColor(long nanos) {
        float mspt = this.msptSupplier.get().floatValue();
        return this.getSampleColor(TpsDebugChart.toMilliseconds(nanos), mspt, -16711936, (double)mspt * 1.125, -256, (double)mspt * 1.25, -65536);
    }

    private static double toMilliseconds(double nanos) {
        return nanos / 1000000.0;
    }
}

