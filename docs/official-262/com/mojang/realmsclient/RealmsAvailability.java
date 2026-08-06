/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsClientOutdatedScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsParentalConsentScreen;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsAvailability {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static @Nullable CompletableFuture<Result> future;

    public static CompletableFuture<Result> get() {
        if (future == null || RealmsAvailability.shouldRefresh(future)) {
            future = RealmsAvailability.check();
        }
        return future;
    }

    private static boolean shouldRefresh(CompletableFuture<Result> future) {
        Result result = future.getNow(null);
        return result != null && result.exception() != null;
    }

    private static CompletableFuture<Result> check() {
        if (Minecraft.getInstance().isOfflineDeveloperMode()) {
            return CompletableFuture.completedFuture(new Result(Type.AUTHENTICATION_ERROR));
        }
        if (SharedConstants.DEBUG_BYPASS_REALMS_VERSION_CHECK) {
            return CompletableFuture.completedFuture(new Result(Type.SUCCESS));
        }
        return CompletableFuture.supplyAsync(() -> {
            RealmsClient client = RealmsClient.getOrCreate();
            try {
                if (client.clientCompatible() != RealmsClient.CompatibleVersionResponse.COMPATIBLE) {
                    return new Result(Type.INCOMPATIBLE_CLIENT);
                }
                if (!client.hasParentalConsent()) {
                    return new Result(Type.NEEDS_PARENTAL_CONSENT);
                }
                return new Result(Type.SUCCESS);
            }
            catch (RealmsServiceException e) {
                LOGGER.error("Couldn't connect to realms", (Throwable)e);
                if (e.realmsError.errorCode() == 401) {
                    return new Result(Type.AUTHENTICATION_ERROR);
                }
                return new Result(e);
            }
        }, Util.nonCriticalIoPool());
    }

    @Environment(value=EnvType.CLIENT)
    public record Result(Type type, @Nullable RealmsServiceException exception) {
        public Result(Type type) {
            this(type, null);
        }

        public Result(RealmsServiceException exception) {
            this(Type.UNEXPECTED_ERROR, exception);
        }

        public @Nullable Screen createErrorScreen(Screen lastScreen) {
            return switch (this.type.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> null;
                case 1 -> new RealmsClientOutdatedScreen(lastScreen);
                case 2 -> new RealmsParentalConsentScreen(lastScreen);
                case 3 -> new RealmsGenericErrorScreen(Component.translatable("mco.error.invalid.session.title"), Component.translatable("mco.error.invalid.session.message"), lastScreen);
                case 4 -> new RealmsGenericErrorScreen(Objects.requireNonNull(this.exception), lastScreen);
            };
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Type {
        SUCCESS,
        INCOMPATIBLE_CLIENT,
        NEEDS_PARENTAL_CONSENT,
        AUTHENTICATION_ERROR,
        UNEXPECTED_ERROR;

    }
}

