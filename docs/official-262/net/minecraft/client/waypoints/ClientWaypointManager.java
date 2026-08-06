/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.waypoints;

import com.mojang.datafixers.util.Either;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.TrackedWaypointManager;

@Environment(value=EnvType.CLIENT)
public class ClientWaypointManager
implements TrackedWaypointManager {
    private final Map<Either<UUID, String>, TrackedWaypoint> waypoints = new ConcurrentHashMap<Either<UUID, String>, TrackedWaypoint>();

    @Override
    public void trackWaypoint(TrackedWaypoint waypoint) {
        this.waypoints.put(waypoint.id(), waypoint);
    }

    @Override
    public void updateWaypoint(TrackedWaypoint waypoint) {
        this.waypoints.get(waypoint.id()).update(waypoint);
    }

    @Override
    public void untrackWaypoint(TrackedWaypoint waypoint) {
        this.waypoints.remove(waypoint.id());
    }

    public boolean hasWaypoints() {
        return !this.waypoints.isEmpty();
    }

    public void forEachWaypoint(Entity fromEntity, Consumer<TrackedWaypoint> consumer) {
        this.waypoints.values().stream().sorted(Comparator.comparingDouble(waypoint -> waypoint.distanceSquared(fromEntity)).reversed()).forEachOrdered(consumer);
    }
}

