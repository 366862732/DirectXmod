/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.telemetry;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryPropertyMap;

@FunctionalInterface
@Environment(value=EnvType.CLIENT)
public interface TelemetryEventSender {
    public static final TelemetryEventSender DISABLED = (type, buildFunction) -> {};

    default public TelemetryEventSender decorate(Consumer<TelemetryPropertyMap.Builder> decorator) {
        return (type, buildFunction) -> this.send(type, properties -> {
            buildFunction.accept(properties);
            decorator.accept((TelemetryPropertyMap.Builder)properties);
        });
    }

    public void send(TelemetryEventType var1, Consumer<TelemetryPropertyMap.Builder> var2);
}

