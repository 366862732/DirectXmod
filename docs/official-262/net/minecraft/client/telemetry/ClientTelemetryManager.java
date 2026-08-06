/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.mojang.authlib.minecraft.TelemetrySession
 *  com.mojang.authlib.minecraft.UserApiService
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.telemetry;

import com.google.common.base.Suppliers;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.telemetry.TelemetryEventInstance;
import net.minecraft.client.telemetry.TelemetryEventLogger;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryLogManager;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.TelemetryPropertyMap;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ClientTelemetryManager
implements AutoCloseable {
    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(1);
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread result = new Thread(r, "Telemetry-Sender-#" + THREAD_COUNT.getAndIncrement());
        result.setDaemon(true);
        return result;
    });
    private final Minecraft minecraft;
    private final UserApiService userApiService;
    private final TelemetryPropertyMap deviceSessionProperties;
    private final Path logDirectory;
    private final CompletableFuture<Optional<TelemetryLogManager>> logManager;
    private final Supplier<TelemetryEventSender> outsideSessionSender = Suppliers.memoize(this::createEventSender);

    public ClientTelemetryManager(Minecraft minecraft, UserApiService userApiService, User user) {
        this.minecraft = minecraft;
        this.userApiService = userApiService;
        TelemetryPropertyMap.Builder properties = TelemetryPropertyMap.builder();
        user.getXuid().ifPresent(id -> properties.put(TelemetryProperty.USER_ID, id));
        user.getClientId().ifPresent(id -> properties.put(TelemetryProperty.CLIENT_ID, id));
        properties.put(TelemetryProperty.MINECRAFT_SESSION_ID, UUID.randomUUID());
        properties.put(TelemetryProperty.GAME_VERSION, SharedConstants.getCurrentVersion().id());
        properties.put(TelemetryProperty.OPERATING_SYSTEM, Util.getPlatform().telemetryName());
        properties.put(TelemetryProperty.PLATFORM, System.getProperty("os.name"));
        properties.put(TelemetryProperty.CLIENT_MODDED, Minecraft.checkModStatus().shouldReportAsModified());
        properties.putIfNotNull(TelemetryProperty.LAUNCHER_NAME, Minecraft.getLauncherBrand());
        this.deviceSessionProperties = properties.build();
        this.logDirectory = minecraft.gameDirectory.toPath().resolve("logs/telemetry");
        this.logManager = TelemetryLogManager.open(this.logDirectory);
    }

    public WorldSessionTelemetryManager createWorldSessionManager(boolean newWorld, @Nullable Duration worldLoadDuration, @Nullable String minigameName, UUID sessionId) {
        return new WorldSessionTelemetryManager(this.createEventSender(), newWorld, worldLoadDuration, minigameName, sessionId);
    }

    public TelemetryEventSender getOutsideSessionSender() {
        return this.outsideSessionSender.get();
    }

    private TelemetryEventSender createEventSender() {
        if (!this.minecraft.allowsTelemetry()) {
            return TelemetryEventSender.DISABLED;
        }
        TelemetrySession telemetrySession = this.userApiService.newTelemetrySession(EXECUTOR);
        if (!telemetrySession.isEnabled()) {
            return TelemetryEventSender.DISABLED;
        }
        CompletionStage loggerFuture = this.logManager.thenCompose(manager -> manager.map(TelemetryLogManager::openLogger).orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())));
        return (arg_0, arg_1) -> this.lambda$createEventSender$2((CompletableFuture)loggerFuture, telemetrySession, arg_0, arg_1);
    }

    public Path getLogDirectory() {
        return this.logDirectory;
    }

    @Override
    public void close() {
        this.logManager.thenAccept(manager -> manager.ifPresent(TelemetryLogManager::close));
    }

    private /* synthetic */ void lambda$createEventSender$2(CompletableFuture loggerFuture, TelemetrySession telemetrySession, TelemetryEventType type, Consumer buildFunction) {
        if (type.isOptIn() && !Minecraft.getInstance().telemetryOptInExtra()) {
            return;
        }
        TelemetryPropertyMap.Builder properties = TelemetryPropertyMap.builder();
        properties.putAll(this.deviceSessionProperties);
        properties.put(TelemetryProperty.EVENT_TIMESTAMP_UTC, Instant.now());
        properties.put(TelemetryProperty.OPT_IN, type.isOptIn());
        buildFunction.accept(properties);
        TelemetryEventInstance event = new TelemetryEventInstance(type, properties.build());
        loggerFuture.thenAccept(logger -> {
            if (logger.isEmpty()) {
                return;
            }
            ((TelemetryEventLogger)logger.get()).log(event);
            if (!SharedConstants.IS_RUNNING_IN_IDE || !SharedConstants.DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND) {
                event.export(telemetrySession).send();
            }
        });
    }
}

