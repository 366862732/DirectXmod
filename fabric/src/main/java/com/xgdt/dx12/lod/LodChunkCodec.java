package com.xgdt.dx12.lod;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 把 {@link LodRegionCache.VoxelChunk} 编成 Voxy 风格的紧凑字节流：调色板 + 16 位下标。
 *
 * <p>调色板同时存 RGB 和方块注册名（{@code v2}），用于 LOD 纹理查找：渲染端按方块名查
 * {@link net.minecraft.client.resources.model.sprite.TextureAtlas#getSprite}，不再靠猜
 * {@link net.minecraft.world.level.material.MapColor} 选纹理。
 *
 * <p>负载布局（小端）：
 * <pre>
 * u16 version        (v2)
 * u16 reserved
 * u16 minY
 * u16 height
 * u16 topY
 * u16 paletteSize
 * i32 paletteSize × RGB
 * paletteSize × { u16 len + bytes }    // 方块注册名（null 编码 len=0）
 * u16 16×16×height × 调色板下标
 * </pre>
 */
public final class LodChunkCodec {

    /** 负载版本；格式变化时递增，旧缓存会被当作 miss 重新生成。 */
    public static final int VERSION = 2;

    /** 固定头字节数：version + reserved + minY + height + topY + paletteSize。 */
    private static final int HEADER = 12;

    private LodChunkCodec() {
    }

    /** 编码；数据不自洽时返回 {@code null}（调用方跳过写入）。 */
    public static byte[] encode(LodRegionCache.VoxelChunk chunk) {
        int[] palette = chunk.palette();
        String[] names = chunk.paletteNames();
        short[] blocks = chunk.blocks();
        if (palette.length == 0 || palette.length > 0xFFFF) {
            return null;
        }
        if (names == null || names.length != palette.length) {
            return null;
        }
        if (blocks.length != 16 * 16 * chunk.height()) {
            return null;
        }
        // 计算方块名字节总大小
        int nameBytesTotal = 0;
        for (String name : names) {
            if (name == null) {
                nameBytesTotal += 2;  // u16 len = 0
            } else {
                nameBytesTotal += 2 + name.getBytes(StandardCharsets.UTF_8).length;
            }
        }
        long size = HEADER + (long) palette.length * 4
            + nameBytesTotal
            + (long) blocks.length * 2;
        if (size > Integer.MAX_VALUE) {
            return null;
        }
        ByteBuffer out = ByteBuffer.allocate((int) size).order(ByteOrder.LITTLE_ENDIAN);
        out.putShort((short) VERSION);
        out.putShort((short) 0);
        out.putShort((short) chunk.minY());
        out.putShort((short) chunk.height());
        out.putShort((short) chunk.topY());
        out.putShort((short) palette.length);
        for (int rgb : palette) {
            out.putInt(rgb);
        }
        for (String name : names) {
            if (name == null) {
                out.putShort((short) 0);
            } else {
                byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
                out.putShort((short) bytes.length);
                out.put(bytes);
            }
        }
        for (short block : blocks) {
            out.putShort(block);
        }
        return out.array();
    }

    /** 解码；版本不符、长度不符或数据损坏时返回 {@code null}。 */
    public static LodRegionCache.VoxelChunk decode(byte[] payload) {
        if (payload == null || payload.length < HEADER) {
            return null;
        }
        ByteBuffer in = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        int version = in.getShort() & 0xFFFF;
        if (version != VERSION) {
            return null;
        }
        in.getShort(); // reserved
        int minY = in.getShort();
        int height = in.getShort() & 0xFFFF;
        int topY = in.getShort();
        int paletteSize = in.getShort() & 0xFFFF;
        if (height <= 0 || height > 4096 || paletteSize == 0) {
            return null;
        }
        int[] palette = new int[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            palette[i] = in.getInt();
        }
        String[] names = new String[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            int len = in.getShort() & 0xFFFF;
            if (len == 0) {
                names[i] = null;
            } else {
                byte[] buf = new byte[len];
                in.get(buf);
                names[i] = new String(buf, StandardCharsets.UTF_8);
                // 越界保护
                if (in.position() > in.limit()) {
                    return null;
                }
            }
        }
        int blocksLen = 16 * 16 * height;
        short[] blocks = new short[blocksLen];
        for (int i = 0; i < blocksLen; i++) {
            if (in.remaining() < 2) {
                return null;
            }
            blocks[i] = in.getShort();
        }
        return new LodRegionCache.VoxelChunk(minY, height, topY, palette, names, blocks);
    }

    /*
     * 以下是「地表列」负载：每个区块 256 列的 (高度, 地表颜色)。
     *
     * 高度场重建一次要采样十几万个角点、覆盖上万区块。挨个解 NBT 的话，即使三条后台
     * 线程也要十几秒才能把第一屏远景填满，期间几何会一直停在「已采样=0」。把解析结果
     * 按区块压进 {@link LodSectionStore}（约 1.5KB 原始 / 几百字节压缩），下次进世界
     * 或下次重建只需读盘 + 解压，和 Voxy 一样秒开。
     *
     * 负载布局（小端）：
     * <pre>
     * u16 version
     * u16 reserved
     * u16 columns       (固定 256)
     * u16 reserved
     * i16 columns × 高度（绝对 y，Short.MIN_VALUE = 无数据）
     * i32 columns × 地表颜色（-1 = 未知，bit24 = 水体标记）
     * </pre>
     *
     * <p>版本 2 起颜色取的是压过树干后的地面方块（和高度同列）。版本 1 存的是
     * {@code WORLD_SURFACE} 树冠色，读出来会在山坡上留下一道道深绿涂抹，必须作废重解。
     */
    public static final int COLUMNS_VERSION = 2;
    private static final int COLUMNS = 256;
    private static final int COLUMNS_HEADER = 8;
    private static final int COLUMNS_BYTES = COLUMNS_HEADER + COLUMNS * 2 + COLUMNS * 4;

    /** 编码地表列；长度不自洽时返回 {@code null}。 */
    public static byte[] encodeColumns(LodRegionCache.ChunkColumns columns) {
        short[] heights = columns.heights();
        int[] colors = columns.colors();
        if (heights.length != COLUMNS || colors.length != COLUMNS) {
            return null;
        }
        ByteBuffer out = ByteBuffer.allocate(COLUMNS_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        out.putShort((short) COLUMNS_VERSION);
        out.putShort((short) 0);
        out.putShort((short) COLUMNS);
        out.putShort((short) 0);
        for (short height : heights) {
            out.putShort(height);
        }
        for (int color : colors) {
            out.putInt(color);
        }
        return out.array();
    }

    /** 解码地表列；版本/长度不符时返回 {@code null}。 */
    public static LodRegionCache.ChunkColumns decodeColumns(byte[] payload) {
        if (payload == null || payload.length != COLUMNS_BYTES) {
            return null;
        }
        ByteBuffer in = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        int version = in.getShort() & 0xFFFF;
        in.getShort();
        int count = in.getShort() & 0xFFFF;
        in.getShort();
        if (version != COLUMNS_VERSION || count != COLUMNS) {
            return null;
        }
        short[] heights = new short[COLUMNS];
        for (int i = 0; i < COLUMNS; i++) {
            heights[i] = in.getShort();
        }
        int[] colors = new int[COLUMNS];
        for (int i = 0; i < COLUMNS; i++) {
            colors[i] = in.getInt();
        }
        return new LodRegionCache.ChunkColumns(heights, colors);
    }
}
