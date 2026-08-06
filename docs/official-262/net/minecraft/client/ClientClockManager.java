/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;

@Environment(value=EnvType.CLIENT)
public class ClientClockManager
implements ClockManager {
    private final Map<Holder<WorldClock>, ClockInstance> clocks = new HashMap<Holder<WorldClock>, ClockInstance>();
    private long lastTickGameTime;

    private ClockInstance getInstance(Holder<WorldClock> definition) {
        return this.clocks.computeIfAbsent(definition, holder -> new ClockInstance());
    }

    public void tick(long gameTime) {
        long gameTimeDelta = gameTime - this.lastTickGameTime;
        this.lastTickGameTime = gameTime;
        for (ClockInstance instance : this.clocks.values()) {
            double newPartialTicks = (double)instance.partialTick + (double)gameTimeDelta * (double)instance.rate;
            long fullTicks = Mth.floor(newPartialTicks);
            instance.partialTick = (float)(newPartialTicks - (double)fullTicks);
            instance.totalTicks += fullTicks;
        }
    }

    public void handleUpdates(long gameTime, Map<Holder<WorldClock>, ClockNetworkState> updates) {
        this.tick(gameTime);
        updates.forEach((definition, state) -> {
            ClockInstance clock = this.getInstance((Holder<WorldClock>)definition);
            clock.totalTicks = state.totalTicks();
            clock.partialTick = state.partialTick();
            clock.rate = state.rate();
        });
    }

    @Override
    public long getTotalTicks(Holder<WorldClock> definition) {
        return this.getInstance(definition).totalTicks;
    }

    @Environment(value=EnvType.CLIENT)
    private static class ClockInstance {
        private long totalTicks;
        private float partialTick;
        private float rate = 1.0f;

        private ClockInstance() {
        }
    }
}

