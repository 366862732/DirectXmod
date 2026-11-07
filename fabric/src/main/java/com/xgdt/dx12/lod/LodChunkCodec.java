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
}
