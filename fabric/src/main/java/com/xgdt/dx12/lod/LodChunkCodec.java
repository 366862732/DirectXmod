package com.xgdt.dx12.lod;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 把 {@link LodRegionCache.VoxelChunk} 编成 Voxy 风格的紧凑字节流：调色板 + 16 位下标。
 *
 * <p>这里只存颜色（RGB），不存方块注册名或数字 id。好处是缓存跨版本、跨模组稳定，
 * 不需要 Voxy 那样的 {@code id_mapping} 数据库；代价是丢失「水/原木」以外的方块语义，
 * 而 LOD 渲染本来也只需要颜色和 alpha。
 *
 * <p>负载布局（小端）：
 * <pre>
 * u16 version
 * u16 reserved
 * u16 minY
 * u16 height
 * u16 topY
 * u16 paletteSize
 * i32 paletteSize × RGB
 * u16 16×16×height × 调色板下标
 * </pre>
 */
public final class LodChunkCodec {

    /** 负载版本；格式变化时递增，旧缓存会被当作 miss 重新生成。 */
    public static final int VERSION = 1;

    /** 固定头字节数：version + reserved + minY + height + topY + paletteSize。 */
    private static final int HEADER = 12;

    private LodChunkCodec() {
    }

    /** 编码；数据不自洽时返回 {@code null}（调用方跳过写入）。 */
    public static byte[] encode(LodRegionCache.VoxelChunk chunk) {
        int[] palette = chunk.palette();
        short[] blocks = chunk.blocks();
        if (palette.length == 0 || palette.length > 0xFFFF) {
            return null;
        }
        if (blocks.length != 16 * 16 * chunk.height()) {
            return null;
        }
        long size = HEADER + (long) palette.length * 4 + (long) blocks.length * 2;
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
        long expected = HEADER + (long) paletteSize * 4 + (long) 16 * 16 * height * 2;
        if (expected != payload.length) {
            return null;
        }
        int[] palette = new int[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            palette[i] = in.getInt();
        }
        short[] blocks = new short[16 * 16 * height];
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = in.getShort();
        }
        return new LodRegionCache.VoxelChunk(minY, height, topY, palette, blocks);
    }
}
