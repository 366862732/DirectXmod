package com.xgdt.dx11.perf.immediate.map;

/**
 * Implemented by {@code MapTextureManager} to own the map atlases and the
 * map-id to tile mapping.
 *
 * <p>Slots are requested when the vanilla map instance is constructed, so the
 * mixin on that instance can capture its tile once instead of looking it up on
 * every frame.  The render path asks for the existing slot only; this keeps
 * maps that were created before the feature was toggled off working.</p>
 */
public interface MapAtlasManagerAccess {
    /** Returns an existing slot, or allocates one when the feature is active. */
    int dx11$allocateAtlasSlot(int mapId);

    /** Returns the slot for an already mapped map, or -1. */
    int dx11$atlasSlot(int mapId);

    /** Returns the atlas for a packed slot's atlas id, or {@code null}. */
    MapAtlas dx11$atlas(int atlasId);

    /** Releases every atlas (called when the map data is reset). */
    void dx11$resetMapAtlases();
}
