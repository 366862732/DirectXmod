package com.xgdt.dx11.mixin.immediate;

import com.xgdt.dx11.perf.immediate.ImmediateFlags;
import com.xgdt.dx11.perf.immediate.map.MapAtlas;
import com.xgdt.dx11.perf.immediate.map.MapAtlasManagerAccess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.MapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Owns the map atlases.
 *
 * <p>The mapping is allocated when a map instance is created (see the
 * {@code MapInstance} mixin) and cleared together with vanilla's map data.
 * Maps are never moved between tiles, so a render state can hold a plain tile
 * offset for its whole submission.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(MapTextureManager.class)
public abstract class MapTextureManagerMixin implements MapAtlasManagerAccess {
    private static final int MAX_ATLASES = 4;

    @Unique
    private final List<MapAtlas> dx11$atlases = new ArrayList<>();

    @Unique
    private final Map<Integer, Integer> dx11$slots = new HashMap<>();

    @Override
    public int dx11$allocateAtlasSlot(int mapId) {
        Integer existing = this.dx11$slots.get(mapId);
        if (existing != null) {
            return existing;
        }
        if (!ImmediateFlags.mapAtlas() || this.dx11$atlases.size() >= MAX_ATLASES) {
            return -1;
        }
        for (int i = 0; i < this.dx11$atlases.size(); i++) {
            int slot = this.dx11$atlases.get(i).allocate();
            if (slot != -1) {
                this.dx11$slots.put(mapId, slot);
                return slot;
            }
        }
        MapAtlas atlas = new MapAtlas(this.dx11$atlases.size(), ImmediateFlags.mapAtlasSize());
        this.dx11$atlases.add(atlas);
        int slot = atlas.allocate();
        if (slot == -1) {
            this.dx11$atlases.remove(atlas);
            atlas.close();
            return -1;
        }
        this.dx11$slots.put(mapId, slot);
        return slot;
    }

    @Override
    public int dx11$atlasSlot(int mapId) {
        Integer slot = this.dx11$slots.get(mapId);
        return slot == null ? -1 : slot;
    }

    @Override
    public MapAtlas dx11$atlas(int atlasId) {
        return atlasId >= 0 && atlasId < this.dx11$atlases.size()
            ? this.dx11$atlases.get(atlasId)
            : null;
    }

    @Override
    public void dx11$resetMapAtlases() {
        for (int i = 0; i < this.dx11$atlases.size(); i++) {
            this.dx11$atlases.get(i).close();
        }
        this.dx11$atlases.clear();
        this.dx11$slots.clear();
    }

    @Inject(method = "resetData", at = @At("RETURN"))
    private void dx11$releaseAtlases(CallbackInfo ci) {
        this.dx11$resetMapAtlases();
    }
}
