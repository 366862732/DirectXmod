/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;

@Environment(value=EnvType.CLIENT)
public class WaypointStyleManager
extends SimpleJsonResourceReloadListener<WaypointStyle> {
    private static final FileToIdConverter ASSET_LISTER = FileToIdConverter.json("waypoint_style");
    private static final WaypointStyle MISSING = new WaypointStyle(0, 1, List.of(MissingTextureAtlasSprite.getLocation()));
    private Map<ResourceKey<WaypointStyleAsset>, WaypointStyle> waypointStyles = Map.of();

    public WaypointStyleManager() {
        super(WaypointStyle.CODEC, ASSET_LISTER);
    }

    @Override
    protected void apply(Map<Identifier, WaypointStyle> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.waypointStyles = preparations.entrySet().stream().collect(Collectors.toUnmodifiableMap(e -> ResourceKey.create(WaypointStyleAssets.ROOT_ID, (Identifier)e.getKey()), Map.Entry::getValue));
    }

    public WaypointStyle get(ResourceKey<WaypointStyleAsset> id) {
        return this.waypointStyles.getOrDefault(id, MISSING);
    }
}

