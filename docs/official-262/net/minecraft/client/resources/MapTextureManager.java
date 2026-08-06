/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Environment(value=EnvType.CLIENT)
public class MapTextureManager
implements AutoCloseable {
    private final Int2ObjectMap<MapInstance> maps = new Int2ObjectOpenHashMap();
    private final TextureManager textureManager;

    public MapTextureManager(TextureManager textureManager) {
        this.textureManager = textureManager;
    }

    public void update(MapId id, MapItemSavedData data) {
        this.getOrCreateMapInstance(id, data).forceUpload();
    }

    public Identifier prepareMapTexture(MapId id, MapItemSavedData data) {
        MapInstance mapInstance = this.getOrCreateMapInstance(id, data);
        mapInstance.updateTextureIfNeeded();
        return mapInstance.location;
    }

    public void resetData() {
        for (MapInstance mapInstance : this.maps.values()) {
            mapInstance.close();
        }
        this.maps.clear();
    }

    private MapInstance getOrCreateMapInstance(MapId id, MapItemSavedData data) {
        return (MapInstance)this.maps.compute(id.id(), (k, instance) -> {
            if (instance == null) {
                return new MapInstance(this, (int)k, data);
            }
            instance.replaceMapData(data);
            return instance;
        });
    }

    @Override
    public void close() {
        this.resetData();
    }

    @Environment(value=EnvType.CLIENT)
    private class MapInstance
    implements AutoCloseable {
        private MapItemSavedData data;
        private final DynamicTexture texture;
        private boolean requiresUpload;
        private final Identifier location;

        private MapInstance(MapTextureManager mapTextureManager, int id, MapItemSavedData data) {
            Objects.requireNonNull(mapTextureManager);
            this.requiresUpload = true;
            this.data = data;
            this.texture = new DynamicTexture(() -> "Map " + id, 128, 128, true);
            this.location = Identifier.withDefaultNamespace("map/" + id);
            mapTextureManager.textureManager.register(this.location, this.texture);
        }

        private void replaceMapData(MapItemSavedData data) {
            boolean dataChanged = this.data != data;
            this.data = data;
            this.requiresUpload |= dataChanged;
        }

        public void forceUpload() {
            this.requiresUpload = true;
        }

        private void updateTextureIfNeeded() {
            if (this.requiresUpload) {
                NativeImage pixels = this.texture.getPixels();
                for (int y = 0; y < 128; ++y) {
                    for (int x = 0; x < 128; ++x) {
                        int i = x + y * 128;
                        pixels.setPixel(x, y, MapColor.getColorFromPackedId(this.data.colors[i]));
                    }
                }
                this.texture.upload();
                this.requiresUpload = false;
            }
        }

        @Override
        public void close() {
            this.texture.close();
        }
    }
}

