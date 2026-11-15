package com.xgdt.dx11.mixin.immediate;

import com.xgdt.dx11.perf.immediate.map.MapAtlas;
import com.xgdt.dx11.perf.immediate.map.MapAtlasRenderStateAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.state.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Adds the atlas tile reference to the extracted map render state. */
@Environment(EnvType.CLIENT)
@Mixin(MapRenderState.class)
public abstract class MapRenderStateMixin implements MapAtlasRenderStateAccess {
    @Unique
    private MapAtlas dx11$atlas;

    @Unique
    private int dx11$atlasX;

    @Unique
    private int dx11$atlasY;

    @Override
    public void dx11$setMapAtlas(MapAtlas atlas, int atlasX, int atlasY) {
        this.dx11$atlas = atlas;
        this.dx11$atlasX = atlasX;
        this.dx11$atlasY = atlasY;
    }

    @Override
    public void dx11$clearMapAtlas() {
        this.dx11$atlas = null;
    }

    @Override
    public MapAtlas dx11$mapAtlas() {
        return this.dx11$atlas;
    }

    @Override
    public int dx11$mapAtlasX() {
        return this.dx11$atlasX;
    }

    @Override
    public int dx11$mapAtlasY() {
        return this.dx11$atlasY;
    }
}
