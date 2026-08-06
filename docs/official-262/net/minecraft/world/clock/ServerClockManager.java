/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
 *  org.apache.commons.lang3.mutable.MutableBoolean
 */
package net.minecraft.world.clock;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.PackedClockStates;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.timeline.Timeline;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class ServerClockManager
extends SavedData
implements ClockManager {
    public static final SavedDataType<ServerClockManager> TYPE = new SavedDataType<ServerClockManager>(Identifier.withDefaultNamespace("world_clocks"), () -> new ServerClockManager(PackedClockStates.EMPTY), PackedClockStates.CODEC.xmap(ServerClockManager::new, ServerClockManager::packState), DataFixTypes.SAVED_DATA_WORLD_CLOCKS);
    private final PackedClockStates packedClockStates;
    private MinecraftServer server;
    private final Map<Holder<WorldClock>, ClockInstance> clocks = new HashMap<Holder<WorldClock>, ClockInstance>();

    private ServerClockManager(PackedClockStates packedClockStates) {
        this.packedClockStates = packedClockStates;
    }

    public void init(MinecraftServer server) {
        this.server = server;
        server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).listElements().forEach(definition -> this.clocks.put((Holder<WorldClock>)definition, new ClockInstance()));
        server.registryAccess().lookupOrThrow(Registries.TIMELINE).listElements().forEach(timeline -> ((Timeline)timeline.value()).registerTimeMarkers(this::registerTimeMarker));
        this.packedClockStates.clocks().forEach((definition, state) -> {
            ClockInstance instance = this.getInstance((Holder<WorldClock>)definition);
            instance.loadFrom((ClockState)state);
        });
    }

    private void registerTimeMarker(ResourceKey<ClockTimeMarker> timeMarkerId, ClockTimeMarker timeMarker) {
        this.getInstance(timeMarker.clock()).timeMarkers.put(timeMarkerId, timeMarker);
    }

    public PackedClockStates packState() {
        return new PackedClockStates(Util.mapValues(this.clocks, ClockInstance::packState));
    }

    public void tick() {
        boolean advanceTime = this.server.getGlobalGameRules().get(GameRules.ADVANCE_TIME);
        if (advanceTime) {
            this.clocks.values().forEach(ClockInstance::tick);
            this.setDirty();
        }
    }

    private ClockInstance getInstance(Holder<WorldClock> definition) {
        ClockInstance instance = this.clocks.get(definition);
        if (instance == null) {
            throw new IllegalStateException("No clock initialized for definition: " + String.valueOf(definition));
        }
        return instance;
    }

    public void setTotalTicks(Holder<WorldClock> clock, long totalTicks) {
        this.modifyClock(clock, instance -> {
            instance.totalTicks = totalTicks;
            instance.partialTick = 0.0f;
        });
    }

    public boolean moveToTimeMarker(Holder<WorldClock> clock, ResourceKey<ClockTimeMarker> timeMarkerId) {
        MutableBoolean set = new MutableBoolean();
        this.modifyClock(clock, instance -> {
            ClockTimeMarker timeMarker = instance.timeMarkers.get(timeMarkerId);
            if (timeMarker == null) {
                return;
            }
            instance.totalTicks = timeMarker.resolveTimeToMoveTo(instance.totalTicks);
            instance.partialTick = 0.0f;
            set.setTrue();
        });
        return set.booleanValue();
    }

    public void addTicks(Holder<WorldClock> clock, int ticks) {
        this.modifyClock(clock, instance -> {
            instance.totalTicks = Math.max(instance.totalTicks + (long)ticks, 0L);
        });
    }

    public void setPaused(Holder<WorldClock> clock, boolean paused) {
        this.modifyClock(clock, instance -> {
            instance.paused = paused;
        });
    }

    public void setRate(Holder<WorldClock> clock, float rate) {
        this.modifyClock(clock, instance -> {
            instance.rate = rate;
        });
    }

    private void modifyClock(Holder<WorldClock> clock, Consumer<? super ClockInstance> action) {
        ClockInstance instance = this.getInstance(clock);
        action.accept(instance);
        Map<Holder<WorldClock>, ClockNetworkState> updates = Map.of(clock, instance.packNetworkState(this.server));
        this.server.getPlayerList().broadcastAll(new ClientboundSetTimePacket(this.getGameTime(), updates));
        this.setDirty();
        for (ServerLevel level : this.server.getAllLevels()) {
            level.environmentAttributes().invalidateTickCache();
        }
    }

    @Override
    public long getTotalTicks(Holder<WorldClock> definition) {
        return this.getInstance(definition).totalTicks;
    }

    public ClientboundSetTimePacket createFullSyncPacket() {
        return new ClientboundSetTimePacket(this.getGameTime(), Util.mapValues(this.clocks, clock -> clock.packNetworkState(this.server)));
    }

    private long getGameTime() {
        return this.server.overworld().getGameTime();
    }

    public boolean isAtTimeMarker(Holder<WorldClock> clock, ResourceKey<ClockTimeMarker> timeMarkerId) {
        ClockInstance clockInstance = this.getInstance(clock);
        ClockTimeMarker timeMarker = clockInstance.timeMarkers.get(timeMarkerId);
        return timeMarker != null && timeMarker.occursAt(clockInstance.totalTicks);
    }

    public Stream<ResourceKey<ClockTimeMarker>> commandTimeMarkersForClock(Holder<WorldClock> clock) {
        return this.getInstance(clock).timeMarkers.entrySet().stream().filter(entry -> ((ClockTimeMarker)entry.getValue()).showInCommands()).map(Map.Entry::getKey);
    }

    private static class ClockInstance {
        private final Map<ResourceKey<ClockTimeMarker>, ClockTimeMarker> timeMarkers = new Reference2ObjectOpenHashMap();
        private long totalTicks;
        private float partialTick;
        private float rate = 1.0f;
        private boolean paused;

        private ClockInstance() {
        }

        public void loadFrom(ClockState state) {
            this.totalTicks = state.totalTicks();
            this.partialTick = state.partialTick();
            this.rate = state.rate();
            this.paused = state.paused();
        }

        public void tick() {
            if (!this.paused) {
                this.partialTick += this.rate;
                int fullTicks = Mth.floor(this.partialTick);
                this.partialTick -= (float)fullTicks;
                this.totalTicks += (long)fullTicks;
            }
        }

        public ClockState packState() {
            return new ClockState(this.totalTicks, this.partialTick, this.rate, this.paused);
        }

        public ClockNetworkState packNetworkState(MinecraftServer server) {
            boolean advanceTime = server.getGlobalGameRules().get(GameRules.ADVANCE_TIME);
            boolean paused = this.paused || !advanceTime;
            return new ClockNetworkState(this.totalTicks, this.partialTick, paused ? 0.0f : this.rate);
        }
    }
}

