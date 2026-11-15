package com.xgdt.dx11.perf.immediate.map;

/**
 * Carries the atlas location from {@code MapRenderer.extractRenderState} to
 * the two draw paths (GUI blit and world/held-map custom geometry).
 */
public interface MapAtlasRenderStateAccess {
    void dx11$setMapAtlas(MapAtlas atlas, int atlasX, int atlasY);

    void dx11$clearMapAtlas();

    MapAtlas dx11$mapAtlas();

    int dx11$mapAtlasX();

    int dx11$mapAtlasY();
}
