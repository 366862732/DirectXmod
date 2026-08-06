/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.math.LongMath
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.Object2BooleanFunction
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.LongMath;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class PeriodicNotificationManager
extends SimplePreparableReloadListener<Map<String, List<Notification>>>
implements AutoCloseable {
    private static final Codec<Map<String, List<Notification>>> CODEC = Codec.unboundedMap((Codec)Codec.STRING, (Codec)RecordCodecBuilder.create(i -> i.group((App)Codec.LONG.optionalFieldOf("delay", (Object)0L).forGetter(Notification::delay), (App)Codec.LONG.fieldOf("period").forGetter(Notification::period), (App)Codec.STRING.fieldOf("title").forGetter(Notification::title), (App)Codec.STRING.fieldOf("message").forGetter(Notification::message)).apply((Applicative)i, Notification::new)).listOf());
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Identifier notifications;
    private final Object2BooleanFunction<String> selector;
    private @Nullable Timer timer;
    private @Nullable NotificationTask notificationTask;

    public PeriodicNotificationManager(Identifier notifications, Object2BooleanFunction<String> selector) {
        this.notifications = notifications;
        this.selector = selector;
    }

    @Override
    protected Map<String, List<Notification>> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map map;
        block8: {
            BufferedReader reader = manager.openAsReader(this.notifications);
            try {
                map = (Map)CODEC.parse((DynamicOps)JsonOps.INSTANCE, (Object)StrictJsonParser.parse(reader)).result().orElseThrow();
                if (reader == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (reader != null) {
                        try {
                            ((Reader)reader).close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (Exception e) {
                    LOGGER.warn("Failed to load {}", (Object)this.notifications, (Object)e);
                    return ImmutableMap.of();
                }
            }
            ((Reader)reader).close();
        }
        return map;
    }

    @Override
    protected void apply(Map<String, List<Notification>> preparations, ResourceManager manager, ProfilerFiller profiler) {
        List<Notification> notifications = preparations.entrySet().stream().filter(e -> (Boolean)this.selector.apply((Object)((String)e.getKey()))).map(Map.Entry::getValue).flatMap(Collection::stream).collect(Collectors.toList());
        if (notifications.isEmpty()) {
            this.stopTimer();
            return;
        }
        if (notifications.stream().anyMatch(n -> n.period == 0L)) {
            Util.logAndPauseIfInIde("A periodic notification in " + String.valueOf(this.notifications) + " has a period of zero minutes");
            this.stopTimer();
            return;
        }
        long delay = this.calculateInitialDelay(notifications);
        long period = this.calculateOptimalPeriod(notifications, delay);
        if (this.timer == null) {
            this.timer = new Timer();
        }
        this.notificationTask = this.notificationTask == null ? new NotificationTask(notifications, delay, period) : this.notificationTask.reset(notifications, period);
        this.timer.scheduleAtFixedRate((TimerTask)this.notificationTask, TimeUnit.MINUTES.toMillis(delay), TimeUnit.MINUTES.toMillis(period));
    }

    @Override
    public void close() {
        this.stopTimer();
    }

    private void stopTimer() {
        if (this.timer != null) {
            this.timer.cancel();
        }
    }

    private long calculateOptimalPeriod(List<Notification> notifications, long initialDelay) {
        return notifications.stream().mapToLong(c -> {
            long delayPeriods = c.delay - initialDelay;
            return LongMath.gcd((long)delayPeriods, (long)c.period);
        }).reduce(LongMath::gcd).orElseThrow(() -> new IllegalStateException("Empty notifications from: " + String.valueOf(this.notifications)));
    }

    private long calculateInitialDelay(List<Notification> notifications) {
        return notifications.stream().mapToLong(c -> c.delay).min().orElse(0L);
    }

    @Environment(value=EnvType.CLIENT)
    private static class NotificationTask
    extends TimerTask {
        private final Minecraft minecraft = Minecraft.getInstance();
        private final List<Notification> notifications;
        private final long period;
        private final AtomicLong elapsed;

        public NotificationTask(List<Notification> notifications, long elapsed, long period) {
            this.notifications = notifications;
            this.period = period;
            this.elapsed = new AtomicLong(elapsed);
        }

        public NotificationTask reset(List<Notification> notifications, long period) {
            this.cancel();
            return new NotificationTask(notifications, this.elapsed.get(), period);
        }

        @Override
        public void run() {
            long currentMinute = this.elapsed.getAndAdd(this.period);
            long nextMinute = this.elapsed.get();
            for (Notification notification : this.notifications) {
                long currentPeriods;
                long elapsedPeriods;
                if (currentMinute < notification.delay || (elapsedPeriods = currentMinute / notification.period) == (currentPeriods = nextMinute / notification.period)) continue;
                this.minecraft.execute(() -> SystemToast.add(Minecraft.getInstance().gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, Component.translatable(notification.title, elapsedPeriods), Component.translatable(notification.message, elapsedPeriods)));
                return;
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Notification(long delay, long period, String title, String message) {
        public Notification(long delay, long period, String title, String message) {
            this.delay = delay != 0L ? delay : period;
            this.period = period;
            this.title = title;
            this.message = message;
        }
    }
}

