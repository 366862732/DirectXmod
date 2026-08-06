/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.data.models;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;

@Environment(value=EnvType.CLIENT)
public class WaypointStyleProvider
implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public WaypointStyleProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "waypoint_style");
    }

    private static void bootstrap(BiConsumer<ResourceKey<WaypointStyleAsset>, WaypointStyle> consumer) {
        consumer.accept(WaypointStyleAssets.DEFAULT, new WaypointStyle(128, 332, List.of(Identifier.withDefaultNamespace("default_0"), Identifier.withDefaultNamespace("default_1"), Identifier.withDefaultNamespace("default_2"), Identifier.withDefaultNamespace("default_3"))));
        consumer.accept(WaypointStyleAssets.BOWTIE, new WaypointStyle(64, 332, List.of(Identifier.withDefaultNamespace("bowtie"), Identifier.withDefaultNamespace("default_0"), Identifier.withDefaultNamespace("default_1"), Identifier.withDefaultNamespace("default_2"), Identifier.withDefaultNamespace("default_3"))));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        HashMap waypointStyles = new HashMap();
        WaypointStyleProvider.bootstrap((id, asset) -> {
            if (waypointStyles.putIfAbsent(id, asset) != null) {
                throw new IllegalStateException("Tried to register waypoint style twice for id: " + String.valueOf(id));
            }
        });
        return DataProvider.saveAll(cache, WaypointStyle.CODEC, this.pathProvider::json, waypointStyles);
    }

    @Override
    public String getName() {
        return "Waypoint Style Definitions";
    }
}

