/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.clock;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.clock.ClockTimeMarker;

public interface ClockTimeMarkers {
    public static final ResourceKey<? extends Registry<ClockTimeMarker>> ROOT_ID = ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("clock_time_marker"));
    public static final ResourceKey<ClockTimeMarker> DAY = ClockTimeMarkers.createKey("day");
    public static final ResourceKey<ClockTimeMarker> NOON = ClockTimeMarkers.createKey("noon");
    public static final ResourceKey<ClockTimeMarker> NIGHT = ClockTimeMarkers.createKey("night");
    public static final ResourceKey<ClockTimeMarker> MIDNIGHT = ClockTimeMarkers.createKey("midnight");
    public static final ResourceKey<ClockTimeMarker> WAKE_UP_FROM_SLEEP = ClockTimeMarkers.createKey("wake_up_from_sleep");
    public static final ResourceKey<ClockTimeMarker> ROLL_VILLAGE_SIEGE = ClockTimeMarkers.createKey("roll_village_siege");

    public static ResourceKey<ClockTimeMarker> createKey(String name) {
        return ResourceKey.create(ROOT_ID, Identifier.withDefaultNamespace(name));
    }
}

