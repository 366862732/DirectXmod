/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Stopwatch
 *  com.google.common.base.Ticker
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.telemetry.events;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class GameLoadTimesEvent {
    public static final GameLoadTimesEvent INSTANCE = new GameLoadTimesEvent(Ticker.systemTicker());
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Ticker timeSource;
    private final Map<TelemetryProperty<Measurement>, Stopwatch> measurements = new HashMap<TelemetryProperty<Measurement>, Stopwatch>();
    private OptionalLong bootstrapTime = OptionalLong.empty();

    protected GameLoadTimesEvent(Ticker timeSource) {
        this.timeSource = timeSource;
    }

    public synchronized void beginStep(TelemetryProperty<Measurement> property) {
        this.beginStep(property, (TelemetryProperty<Measurement> p) -> Stopwatch.createStarted((Ticker)this.timeSource));
    }

    public synchronized void beginStep(TelemetryProperty<Measurement> property, Stopwatch measurement) {
        this.beginStep(property, (TelemetryProperty<Measurement> p) -> measurement);
    }

    private synchronized void beginStep(TelemetryProperty<Measurement> property, Function<TelemetryProperty<Measurement>, Stopwatch> measurement) {
        this.measurements.computeIfAbsent(property, measurement);
    }

    public synchronized void endStep(TelemetryProperty<Measurement> property) {
        Stopwatch stepMeasurement = this.measurements.get(property);
        if (stepMeasurement == null) {
            LOGGER.warn("Attempted to end step for {} before starting it", (Object)property.id());
            return;
        }
        if (stepMeasurement.isRunning()) {
            stepMeasurement.stop();
        }
    }

    public void send(TelemetryEventSender eventSender) {
        eventSender.send(TelemetryEventType.GAME_LOAD_TIMES, properties -> {
            GameLoadTimesEvent gameLoadTimesEvent = this;
            synchronized (gameLoadTimesEvent) {
                this.measurements.forEach((key, stepMeasurement) -> {
                    if (!stepMeasurement.isRunning()) {
                        long elapsed = stepMeasurement.elapsed(TimeUnit.MILLISECONDS);
                        properties.put(key, new Measurement((int)elapsed));
                    } else {
                        LOGGER.warn("Measurement {} was discarded since it was still ongoing when the event {} was sent.", (Object)key.id(), (Object)TelemetryEventType.GAME_LOAD_TIMES.id());
                    }
                });
                this.bootstrapTime.ifPresent(duration -> properties.put(TelemetryProperty.LOAD_TIME_BOOTSTRAP_MS, new Measurement((int)duration)));
                this.measurements.clear();
            }
        });
    }

    public synchronized void setBootstrapTime(long duration) {
        this.bootstrapTime = OptionalLong.of(duration);
    }

    @Environment(value=EnvType.CLIENT)
    public record Measurement(int millis) {
        public static final Codec<Measurement> CODEC = Codec.INT.xmap(Measurement::new, o -> o.millis);
    }
}

