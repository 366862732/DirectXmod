package com.xgdt.dx12.lod;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** {@link LodChunkCodec} 的离线测试：往返、版本/长度校验、损坏数据。 */
class LodChunkCodecTest {

    private static final int MIN_Y = -64;
    private static final int HEIGHT = 384;

    private static LodRegionCache.VoxelChunk sample() {
        int[] palette = {-1, 0x7A5A3A, LodRegionCache.FLAG_WATER | 0x3F76E4};
        short[] blocks = new short[16 * 16 * HEIGHT];
        // 地面一层 + 一小块水
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                blocks[((64 - MIN_Y) * 16 + z) * 16 + x] = 1;
            }
        }
        for (int z = 0; z < 4; z++) {
            for (int x = 0; x < 4; x++) {
                blocks[((65 - MIN_Y) * 16 + z) * 16 + x] = 2;
            }
        }
        return new LodRegionCache.VoxelChunk(MIN_Y, HEIGHT, 65, palette, blocks);
    }

    @Test
    void roundTripPreservesEverything() {
        LodRegionCache.VoxelChunk original = sample();
        byte[] encoded = LodChunkCodec.encode(original);
        assertNotNull(encoded);

        LodRegionCache.VoxelChunk decoded = LodChunkCodec.decode(encoded);
        assertNotNull(decoded);
        assertEquals(original.minY(), decoded.minY());
        assertEquals(original.height(), decoded.height());
        assertEquals(original.topY(), decoded.topY());
        assertArrayEquals(original.palette(), decoded.palette());
        assertArrayEquals(original.blocks(), decoded.blocks());
    }

    @Test
    void airOnlyChunkRoundTrips() {
        LodRegionCache.VoxelChunk air = new LodRegionCache.VoxelChunk(
            MIN_Y, HEIGHT, MIN_Y, new int[]{-1}, new short[16 * 16 * HEIGHT]);
        LodRegionCache.VoxelChunk decoded = LodChunkCodec.decode(LodChunkCodec.encode(air));
        assertNotNull(decoded);
        assertEquals(MIN_Y, decoded.topY());
        assertArrayEquals(air.palette(), decoded.palette());
    }

    @Test
    void decodeRejectsGarbage() {
        assertNull(LodChunkCodec.decode(null));
        assertNull(LodChunkCodec.decode(new byte[0]));
        assertNull(LodChunkCodec.decode(new byte[]{1, 2, 3}));
        assertNull(LodChunkCodec.decode(new byte[64]));
    }

    @Test
    void decodeRejectsWrongVersion() {
        byte[] encoded = LodChunkCodec.encode(sample());
        encoded[0] = 0x7F;
        assertNull(LodChunkCodec.decode(encoded));
    }

    @Test
    void decodeRejectsTruncatedPayload() {
        byte[] encoded = LodChunkCodec.encode(sample());
        byte[] truncated = java.util.Arrays.copyOf(encoded, encoded.length - 2);
        assertNull(LodChunkCodec.decode(truncated));
    }

    @Test
    void encodeRejectsInconsistentBlockArray() {
        LodRegionCache.VoxelChunk broken = new LodRegionCache.VoxelChunk(
            MIN_Y, HEIGHT, 0, new int[]{-1}, new short[10]);
        assertNull(LodChunkCodec.encode(broken));
    }
}
