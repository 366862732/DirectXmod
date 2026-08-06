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
package com.mojang.realmsclient.util;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component RIGHT_NOW = Component.translatable("mco.util.time.now");
    private static final int MINUTES = 60;
    private static final int HOURS = 3600;
    private static final int DAYS = 86400;

    public static Component convertToAgePresentation(long timeDiff) {
        if (timeDiff < 0L) {
            return RIGHT_NOW;
        }
        long timeDiffInSeconds = timeDiff / 1000L;
        if (timeDiffInSeconds < 60L) {
            return Component.translatable("mco.time.secondsAgo", timeDiffInSeconds);
        }
        if (timeDiffInSeconds < 3600L) {
            long minutes = timeDiffInSeconds / 60L;
            return Component.translatable("mco.time.minutesAgo", minutes);
        }
        if (timeDiffInSeconds < 86400L) {
            long hours = timeDiffInSeconds / 3600L;
            return Component.translatable("mco.time.hoursAgo", hours);
        }
        long days = timeDiffInSeconds / 86400L;
        return Component.translatable("mco.time.daysAgo", days);
    }

    public static Component convertToAgePresentationFromInstant(Instant date) {
        return RealmsUtil.convertToAgePresentation(System.currentTimeMillis() - date.toEpochMilli());
    }

    public static void extractPlayerFace(GuiGraphicsExtractor graphics, int x, int y, int size, UUID playerId) {
        PlayerFaceExtractor.extractRenderState(graphics, ResolvableProfile.createUnresolved(playerId), x, y, size);
    }

    public static <T> CompletableFuture<T> supplyAsync(RealmsIoFunction<T> function, @Nullable Consumer<RealmsServiceException> onFailure) {
        return CompletableFuture.supplyAsync(() -> {
            RealmsClient client = RealmsClient.getOrCreate();
            try {
                return function.apply(client);
            }
            catch (Throwable t) {
                if (t instanceof RealmsServiceException) {
                    RealmsServiceException e = (RealmsServiceException)t;
                    if (onFailure != null) {
                        onFailure.accept(e);
                    }
                } else {
                    LOGGER.error("Unhandled exception", t);
                }
                throw new RuntimeException(t);
            }
        }, Util.nonCriticalIoPool());
    }

    public static CompletableFuture<Void> runAsync(RealmsIoConsumer function, @Nullable Consumer<RealmsServiceException> onFailure) {
        return RealmsUtil.supplyAsync(function, onFailure);
    }

    public static Consumer<RealmsServiceException> openScreenOnFailure(Function<RealmsServiceException, Screen> errorScreen) {
        Minecraft minecraft = Minecraft.getInstance();
        return e -> minecraft.execute(() -> minecraft.gui.setScreen((Screen)errorScreen.apply((RealmsServiceException)e)));
    }

    public static Consumer<RealmsServiceException> openScreenAndLogOnFailure(Function<RealmsServiceException, Screen> errorScreen, String errorMessage) {
        return RealmsUtil.openScreenOnFailure(errorScreen).andThen(e -> LOGGER.error(errorMessage, (Throwable)e));
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface RealmsIoFunction<T> {
        public T apply(RealmsClient var1) throws RealmsServiceException;
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface RealmsIoConsumer
    extends RealmsIoFunction<Void> {
        public void accept(RealmsClient var1) throws RealmsServiceException;

        @Override
        default public Void apply(RealmsClient client) throws RealmsServiceException {
            this.accept(client);
            return null;
        }
    }
}

