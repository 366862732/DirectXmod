package com.xgdt.dx11.perf.immediate.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;

/**
 * A shared texture that packs many 128x128 maps into one sheet.
 *
 * <p>Every vanilla map owns a texture, so drawing ten maps in an inventory
 * means ten texture switches and usually ten draws.  Packing them lets all
 * maps of one atlas share a render type and a texture binding.  Tiles are
 * allocated once per map id and stay in place until the level changes.</p>
 *
 * <p>The atlas image is a normal {@link DynamicTexture}; updating one map only
 * writes a 128x128 region of it, so the upload cost stays proportional to the
 * changed map rather than to the whole sheet.</p>
 */
public final class MapAtlas implements AutoCloseable {
    public static final int MAP_SIZE = 128;
    private static final int MAX_TILES = 256 * 256;

    private final int id;
    private final int size;
    private final int tilesPerRow;
    private final Identifier identifier;
    private final DynamicTexture texture;
    private int nextTile;
    private boolean closed;

    public MapAtlas(int id, int size) {
        if (size < MAP_SIZE || size % MAP_SIZE != 0) {
            throw new IllegalArgumentException("Atlas size must be a multiple of " + MAP_SIZE);
        }
        int tiles = size / MAP_SIZE;
        this.id = id;
        this.size = size;
        this.tilesPerRow = tiles;
        this.identifier = Identifier.fromNamespaceAndPath("gl4dx11", "map_atlas/" + id);
        this.texture = new DynamicTexture("gl4dx11 map atlas " + id, size, size, true);
        Minecraft.getInstance().getTextureManager().register(this.identifier, this.texture);
    }

    /**
     * Reserves the next free tile.
     *
     * @return a packed slot ({@code atlas << 16 | column << 8 | row}), or -1
     *         when this atlas is full
     */
    public int allocate() {
        if (this.nextTile >= this.tilesPerRow * this.tilesPerRow
            || this.nextTile >= MAX_TILES) {
            return -1;
        }
        int column = this.nextTile % this.tilesPerRow;
        int row = this.nextTile / this.tilesPerRow;
        this.nextTile++;
        return (this.id << 16) | (column << 8) | row;
    }

    public int id() {
        return this.id;
    }

    public int size() {
        return this.size;
    }

    public Identifier identifier() {
        return this.identifier;
    }

    public DynamicTexture texture() {
        return this.texture;
    }

    public static int column(int packedSlot) {
        return (packedSlot >>> 8) & 0xFF;
    }

    public static int row(int packedSlot) {
        return packedSlot & 0xFF;
    }

    public static int atlasId(int packedSlot) {
        return packedSlot >>> 16;
    }

    /**
     * Copies one map's colours into the atlas and uploads only that tile.
     */
    public void uploadTile(int tileX, int tileY, byte[] colors) {
        if (this.closed) {
            return;
        }
        NativeImage image = this.texture.getPixels();
        if (image == null) {
            return;
        }
        int length = Math.min(colors.length, MAP_SIZE * MAP_SIZE);
        for (int y = 0; y < MAP_SIZE; y++) {
            int sourceRow = y * MAP_SIZE;
            for (int x = 0; x < MAP_SIZE; x++) {
                int index = sourceRow + x;
                int color = index < length
                    ? MapColor.getColorFromPackedId(colors[index])
                    : 0;
                image.setPixel(tileX + x, tileY + y, color);
            }
        }
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(
            this.texture.getTexture(),
            image,
            0,
            0,
            tileX,
            tileY,
            MAP_SIZE,
            MAP_SIZE,
            tileX,
            tileY);
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        Minecraft.getInstance().getTextureManager().release(this.identifier);
    }
}
