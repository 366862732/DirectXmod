package com.xgdt.dx11.perf.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CompactTerrainFormatTest {
    private static final VertexFormat BLOCK = DefaultVertexFormat.BLOCK;

    @Test
    void describesTheExpected24ByteLayout() {
        assertEquals(24, CompactTerrainFormat.FORMAT.getVertexSize());
        assertEquals(0, CompactTerrainFormat.FORMAT.getOffset(
            CompactTerrainFormat.FORMAT.getElements().get(0)));
        assertEquals(12, CompactTerrainFormat.FORMAT.getOffset(
            CompactTerrainFormat.FORMAT.getElements().get(1)));
        assertEquals(16, CompactTerrainFormat.FORMAT.getOffset(
            CompactTerrainFormat.FORMAT.getElements().get(2)));
        assertEquals(20, CompactTerrainFormat.FORMAT.getOffset(
            CompactTerrainFormat.FORMAT.getElements().get(3)));

        VertexFormatElement packed = CompactTerrainFormat.packedUv0();
        assertEquals(VertexFormatElement.Type.USHORT, packed.type());
        assertEquals(VertexFormatElement.Usage.UV, packed.usage());
        assertEquals(2, packed.count());
        assertEquals(4, packed.byteSize());
    }

    @Test
    void acceptsOnlyTheVanillaBlockShape() {
        assertTrue(CompactTerrainFormat.supports(BLOCK));
        assertFalse(CompactTerrainFormat.supports(CompactTerrainFormat.FORMAT));
        assertFalse(CompactTerrainFormat.supports(DefaultVertexFormat.POSITION_COLOR));
    }

    @Test
    void convertsPositionsColoursUvsAndLightmap() {
        int count = 4;
        ByteBuffer source = allocate(BLOCK, count);
        for (int vertex = 0; vertex < count; vertex++) {
            writeVertex(
                source,
                BLOCK,
                vertex,
                10.25f + vertex,
                64.5f,
                -3.75f - vertex,
                0x04030201 + vertex * 0x01010101,
                vertex / 3.0f,
                1.0f - vertex / 3.0f,
                0x00F000F0 + vertex,
                (byte) (vertex + 1));
        }

        ByteBuffer target = ByteBuffer.allocateDirect(count * CompactTerrainFormat.STRIDE)
            .order(ByteOrder.nativeOrder());
        CompactTerrainFormat.convert(source, target, count, BLOCK, 16.0f, -8.0f, 32.0f);

        assertEquals(0, target.position());
        assertEquals(count * CompactTerrainFormat.STRIDE, target.limit());

        for (int vertex = 0; vertex < count; vertex++) {
            int base = vertex * CompactTerrainFormat.STRIDE;
            assertEquals(10.25f + vertex + 16.0f, target.getFloat(base), 0.0001f);
            assertEquals(64.5f - 8.0f, target.getFloat(base + 4), 0.0001f);
            assertEquals(-3.75f - vertex + 32.0f, target.getFloat(base + 8), 0.0001f);
            assertEquals(0x04030201 + vertex * 0x01010101, target.getInt(base + 12));
            assertEquals(
                unorm16(vertex / 3.0f),
                Short.toUnsignedInt(target.getShort(base + 16)));
            assertEquals(
                unorm16(1.0f - vertex / 3.0f),
                Short.toUnsignedInt(target.getShort(base + 18)));
            assertEquals(0x00F000F0 + vertex, target.getInt(base + 20));
        }
    }

    @Test
    void clampsAndRoundsPackedUvs() {
        ByteBuffer source = allocate(BLOCK, 1);
        writeVertex(source, BLOCK, 0, 0.0f, 0.0f, 0.0f, 0xFFFFFFFF, -0.25f, 1.25f, 0, (byte) 0);

        ByteBuffer target = ByteBuffer.allocateDirect(CompactTerrainFormat.STRIDE)
            .order(ByteOrder.nativeOrder());
        CompactTerrainFormat.convert(source, target, 1, BLOCK, 0.0f, 0.0f, 0.0f);

        assertEquals(0, Short.toUnsignedInt(target.getShort(CompactTerrainFormat.UV0_OFFSET)));

        ByteBuffer half = allocate(BLOCK, 1);
        writeVertex(half, BLOCK, 0, 0.0f, 0.0f, 0.0f, 0xFFFFFFFF, 0.5f, 1.25f, 0, (byte) 0);
        ByteBuffer packed = ByteBuffer.allocateDirect(CompactTerrainFormat.STRIDE)
            .order(ByteOrder.nativeOrder());
        CompactTerrainFormat.convert(half, packed, 1, BLOCK, 0.0f, 0.0f, 0.0f);

        assertEquals(32768, Short.toUnsignedInt(packed.getShort(CompactTerrainFormat.UV0_OFFSET)));
        assertEquals(65535, Short.toUnsignedInt(packed.getShort(CompactTerrainFormat.UV0_OFFSET + 2)));
    }

    @Test
    void honoursTheSourceBufferPosition() {
        ByteBuffer source = ByteBuffer.allocateDirect(3 + BLOCK.getVertexSize())
            .order(ByteOrder.nativeOrder());
        writeVertexAt(
            source, BLOCK, 3, 1.0f, 2.0f, 3.0f, 0x11223344, 0.25f, 0.75f, 7, (byte) 0);
        source.position(3);

        ByteBuffer target = ByteBuffer.allocateDirect(CompactTerrainFormat.STRIDE)
            .order(ByteOrder.nativeOrder());
        CompactTerrainFormat.convert(source, target, 1, BLOCK, 0.0f, 0.0f, 0.0f);

        assertEquals(1.0f, target.getFloat(0), 0.0f);
        assertEquals(2.0f, target.getFloat(4), 0.0f);
        assertEquals(3.0f, target.getFloat(8), 0.0f);
        assertEquals(0x11223344, target.getInt(12));
        assertEquals(7, target.getInt(20));
    }

    @Test
    void rejectsTooSmallTargetsAndBadVertexCounts() {
        ByteBuffer source = allocate(BLOCK, 4);
        ByteBuffer target = ByteBuffer.allocateDirect(CompactTerrainFormat.STRIDE)
            .order(ByteOrder.nativeOrder());

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> CompactTerrainFormat.convert(source, target, 2, BLOCK, 0, 0, 0));
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> CompactTerrainFormat.convert(source, target, -1, BLOCK, 0, 0, 0));
    }

    @Test
    void acceptsOnlyCompactShaderInputs() {
        assertTrue(CompactTerrainFormat.acceptsShaderInputs(
            Set.of("Position", "Color", "UV0", "UV2")));
        assertTrue(CompactTerrainFormat.acceptsShaderInputs(Set.of("Position", "UV0")));
        assertFalse(CompactTerrainFormat.acceptsShaderInputs(Set.of()));
        assertFalse(CompactTerrainFormat.acceptsShaderInputs(Set.of("Color")));
        assertFalse(CompactTerrainFormat.acceptsShaderInputs(Set.of("Position", "Normal")));
        assertFalse(CompactTerrainFormat.acceptsShaderInputs(null));
    }

    @Test
    void tracksShaderCompatibility() {
        CompactTerrainFormat.resetShaderInputs();
        assertFalse(CompactTerrainFormat.shaderObserved());

        CompactTerrainFormat.noteTerrainShaderInputs(
            Set.of("Position", "Color", "UV0", "UV2"));

        assertTrue(CompactTerrainFormat.shaderObserved());
        assertTrue(CompactTerrainFormat.shaderCompatible());

        // Inputs accumulate across define variants, so one incompatible
        // variant keeps compact vertices disabled from then on.
        CompactTerrainFormat.noteTerrainShaderInputs(
            Set.of("Position", "Color", "UV0", "UV2", "Normal"));

        assertFalse(CompactTerrainFormat.shaderCompatible());

        CompactTerrainFormat.resetShaderInputs();
        assertFalse(CompactTerrainFormat.shaderObserved());
    }

    @Test
    void tracksConversionStats() {
        CompactTerrainFormat.resetStats();
        ByteBuffer source = allocate(BLOCK, 4);
        ByteBuffer target = ByteBuffer.allocateDirect(4 * CompactTerrainFormat.STRIDE)
            .order(ByteOrder.nativeOrder());

        CompactTerrainFormat.convert(source, target, 4, BLOCK, 0.0f, 0.0f, 0.0f);

        String stats = CompactTerrainFormat.describe();
        assertTrue(stats.contains("uploads=1"), stats);
        assertTrue(stats.contains("vertices=4"), stats);
        assertTrue(stats.contains("saved=0.00 MiB"), stats);
    }

    private static ByteBuffer allocate(VertexFormat format, int vertexCount) {
        return ByteBuffer.allocateDirect(vertexCount * format.getVertexSize())
            .order(ByteOrder.nativeOrder());
    }

    private static void writeVertex(
        ByteBuffer buffer,
        VertexFormat format,
        int vertex,
        float x,
        float y,
        float z,
        int color,
        float u,
        float v,
        int light,
        byte normal) {
        writeVertexAt(
            buffer,
            format,
            vertex * format.getVertexSize(),
            x,
            y,
            z,
            color,
            u,
            v,
            light,
            normal);
    }

    private static void writeVertexAt(
        ByteBuffer buffer,
        VertexFormat format,
        int base,
        float x,
        float y,
        float z,
        int color,
        float u,
        float v,
        int light,
        byte normal) {
        int position = offset(format, "Position");
        buffer.putFloat(base + position, x);
        buffer.putFloat(base + position + 4, y);
        buffer.putFloat(base + position + 8, z);
        buffer.putInt(base + offset(format, "Color"), color);
        int uv0 = offset(format, "UV0");
        buffer.putFloat(base + uv0, u);
        buffer.putFloat(base + uv0 + 4, v);
        buffer.putInt(base + offset(format, "UV2"), light);
        int normalOffset = offset(format, "Normal");
        buffer.put(base + normalOffset, normal);
        buffer.put(base + normalOffset + 1, (byte) 0);
        buffer.put(base + normalOffset + 2, (byte) 127);
    }

    private static int offset(VertexFormat format, String name) {
        int index = format.getElementAttributeNames().indexOf(name);
        if (index < 0) {
            throw new IllegalArgumentException("Missing element " + name);
        }
        return format.getOffset(format.getElements().get(index));
    }

    private static int unorm16(float value) {
        float clamped = value < 0.0f ? 0.0f : (value > 1.0f ? 1.0f : value);
        return Math.round(clamped * 65535.0f);
    }
}
