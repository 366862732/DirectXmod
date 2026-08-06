/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.minecraft.TelemetryEvent
 *  com.mojang.authlib.minecraft.TelemetryPropertyContainer
 *  com.mojang.authlib.minecraft.TelemetrySession
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.telemetry;

import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetryPropertyContainer;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.telemetry.TelemetryEventInstance;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.TelemetryPropertyMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class TelemetryEventType {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, TelemetryEventType> REGISTRY = new Object2ObjectLinkedOpenHashMap();
    public static final Codec<TelemetryEventType> CODEC = Codec.STRING.comapFlatMap(key -> {
        TelemetryEventType type = REGISTRY.get(key);
        if (type != null) {
            return DataResult.success((Object)type);
        }
        return DataResult.error(() -> "No TelemetryEventType with key: '" + key + "'");
    }, TelemetryEventType::id);
    private static final List<TelemetryProperty<?>> GLOBAL_PROPERTIES = List.of(TelemetryProperty.USER_ID, TelemetryProperty.CLIENT_ID, TelemetryProperty.MINECRAFT_SESSION_ID, TelemetryProperty.GAME_VERSION, TelemetryProperty.OPERATING_SYSTEM, TelemetryProperty.PLATFORM, TelemetryProperty.CLIENT_MODDED, TelemetryProperty.LAUNCHER_NAME, TelemetryProperty.EVENT_TIMESTAMP_UTC, TelemetryProperty.OPT_IN);
    private static final List<TelemetryProperty<?>> WORLD_SESSION_PROPERTIES = Stream.concat(GLOBAL_PROPERTIES.stream(), Stream.of(TelemetryProperty.WORLD_SESSION_ID, TelemetryProperty.SERVER_MODDED, TelemetryProperty.SERVER_TYPE, TelemetryProperty.SERVER_SESSION_ID)).toList();
    public static final TelemetryEventType GRAPHICS_CAPABILITIES = TelemetryEventType.builder("graphics_capabilities", "GraphicsCapabilities").defineAll(GLOBAL_PROPERTIES).define(TelemetryProperty.BACKEND_NAME).define(TelemetryProperty.BACKEND_FAILURE_MESSAGE).define(TelemetryProperty.BACKEND_FAILURE_REASON).define(TelemetryProperty.BACKEND_FAILURE_MISSING_CAPABILITIES).register();
    public static final TelemetryEventType WORLD_LOADED = TelemetryEventType.builder("world_loaded", "WorldLoaded").defineAll(WORLD_SESSION_PROPERTIES).define(TelemetryProperty.GAME_MODE).define(TelemetryProperty.REALMS_MAP_CONTENT).register();
    public static final TelemetryEventType PERFORMANCE_METRICS = TelemetryEventType.builder("performance_metrics", "PerformanceMetrics").defineAll(WORLD_SESSION_PROPERTIES).define(TelemetryProperty.FRAME_RATE_SAMPLES).define(TelemetryProperty.RENDER_TIME_SAMPLES).define(TelemetryProperty.USED_MEMORY_SAMPLES).define(TelemetryProperty.NUMBER_OF_SAMPLES).define(TelemetryProperty.RENDER_DISTANCE).define(TelemetryProperty.DEDICATED_MEMORY_KB).optIn().register();
    public static final TelemetryEventType WORLD_LOAD_TIMES = TelemetryEventType.builder("world_load_times", "WorldLoadTimes").defineAll(WORLD_SESSION_PROPERTIES).define(TelemetryProperty.WORLD_LOAD_TIME_MS).define(TelemetryProperty.NEW_WORLD).optIn().register();
    public static final TelemetryEventType WORLD_UNLOADED = TelemetryEventType.builder("world_unloaded", "WorldUnloaded").defineAll(WORLD_SESSION_PROPERTIES).define(TelemetryProperty.SECONDS_SINCE_LOAD).define(TelemetryProperty.TICKS_SINCE_LOAD).register();
    public static final TelemetryEventType ADVANCEMENT_MADE = TelemetryEventType.builder("advancement_made", "AdvancementMade").defineAll(WORLD_SESSION_PROPERTIES).define(TelemetryProperty.ADVANCEMENT_ID).define(TelemetryProperty.ADVANCEMENT_GAME_TIME).optIn().register();
    public static final TelemetryEventType GAME_LOAD_TIMES = TelemetryEventType.builder("game_load_times", "GameLoadTimes").defineAll(GLOBAL_PROPERTIES).define(TelemetryProperty.LOAD_TIME_TOTAL_TIME_MS).define(TelemetryProperty.LOAD_TIME_PRE_WINDOW_MS).define(TelemetryProperty.LOAD_TIME_BOOTSTRAP_MS).define(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS).optIn().register();
    private final String id;
    private final String exportKey;
    private final List<TelemetryProperty<?>> properties;
    private final boolean isOptIn;
    private final MapCodec<TelemetryEventInstance> codec;

    private TelemetryEventType(String id, String exportKey, List<TelemetryProperty<?>> properties, boolean isOptIn) {
        this.id = id;
        this.exportKey = exportKey;
        this.properties = properties;
        this.isOptIn = isOptIn;
        this.codec = TelemetryPropertyMap.createCodec(properties).xmap(map -> new TelemetryEventInstance(this, (TelemetryPropertyMap)map), TelemetryEventInstance::properties);
    }

    public static Builder builder(String id, String exportKey) {
        return new Builder(id, exportKey);
    }

    public String id() {
        return this.id;
    }

    public List<TelemetryProperty<?>> properties() {
        return this.properties;
    }

    public MapCodec<TelemetryEventInstance> codec() {
        return this.codec;
    }

    public boolean isOptIn() {
        return this.isOptIn;
    }

    public TelemetryEvent export(TelemetrySession session, TelemetryPropertyMap input) {
        TelemetryEvent output = session.createNewEvent(this.exportKey);
        for (TelemetryProperty<?> property : this.properties) {
            property.export(input, (TelemetryPropertyContainer)output);
        }
        return output;
    }

    public <T> boolean contains(TelemetryProperty<T> property) {
        return this.properties.contains(property);
    }

    public String toString() {
        return "TelemetryEventType[" + this.id + "]";
    }

    public MutableComponent title() {
        return this.makeTranslation("title");
    }

    public MutableComponent description() {
        return this.makeTranslation("description");
    }

    private MutableComponent makeTranslation(String suffix) {
        return Component.translatable("telemetry.event." + this.id + "." + suffix);
    }

    public static List<TelemetryEventType> values() {
        return List.copyOf(REGISTRY.values());
    }

    public static boolean selfTest() {
        boolean hasErrors = false;
        HashSet allProperties = new HashSet();
        for (Map.Entry<String, TelemetryEventType> entry : REGISTRY.entrySet()) {
            TelemetryEventType type = entry.getValue();
            if (!ComponentUtils.isTranslationResolvable(type.description()) || !ComponentUtils.isTranslationResolvable(type.title())) {
                LOGGER.warn("Missing translations for telemetry event {}", (Object)entry.getKey());
                hasErrors = true;
            }
            allProperties.addAll(type.properties);
        }
        for (TelemetryProperty telemetryProperty : allProperties) {
            if (ComponentUtils.isTranslationResolvable(telemetryProperty.title())) continue;
            LOGGER.warn("Missing translation for telemetry property {}", (Object)telemetryProperty.id());
            hasErrors = true;
        }
        return hasErrors;
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final String id;
        private final String exportKey;
        private final List<TelemetryProperty<?>> properties = new ArrayList();
        private boolean isOptIn;

        private Builder(String id, String exportKey) {
            this.id = id;
            this.exportKey = exportKey;
        }

        public Builder defineAll(List<TelemetryProperty<?>> properties) {
            this.properties.addAll(properties);
            return this;
        }

        public <T> Builder define(TelemetryProperty<T> property) {
            this.properties.add(property);
            return this;
        }

        public Builder optIn() {
            this.isOptIn = true;
            return this;
        }

        public TelemetryEventType register() {
            TelemetryEventType type = new TelemetryEventType(this.id, this.exportKey, List.copyOf(this.properties), this.isOptIn);
            if (REGISTRY.putIfAbsent(this.id, type) != null) {
                throw new IllegalStateException("Duplicate TelemetryEventType with key: '" + this.id + "'");
            }
            return type;
        }
    }
}

