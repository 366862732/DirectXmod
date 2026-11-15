package com.xgdt.dx11.perf.region;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 24-byte compact terrain vertex format used by region draws.
 *
 * <p>Vanilla {@code DefaultVertexFormat.BLOCK} is 32 bytes: position float3,
 * colour RGBA8, UV0 float2, UV2 short2, normal byte3 and one padding byte.
 * Terrain shaders never read the normal, so the compact layout drops it and
 * stores UV0 as normalized unsigned shorts:</p>
 *
 * <pre>
 *   offset  size  element
 *   0       12    Position  float3
 *   12       4    Color     RGBA8 (unnormalized byte copy)
 *   16       4    UV0       UNORM16 x2
 *   20       4    UV2       short2 (raw light coordinates)
 * </pre>
 *
 * <p>The format is only used for the D3D11 input layout; vertex data is packed
 * directly by {@link #convert}.  The vanilla block format stays untouched, so
 * other pipelines and mods keep their 32-byte path.</p>
 */
public final class CompactTerrainFormat {
    public static final int STRIDE = 24;
    public static final int POSITION_OFFSET = 0;
    public static final int COLOR_OFFSET = 12;
    public static final int UV0_OFFSET = 16;
    public static final int UV2_OFFSET = 20;

    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    private static final String POSITION = "Position";
    private static final String COLOR = "Color";
    private static final String UV0 = "UV0";
    private static final String UV2 = "UV2";
    private static final Set<String> COMPACT_INPUTS = Set.of(POSITION, COLOR, UV0, UV2);

    /**
     * Registered packed UV0 element.  Vanilla registers ids 0..6, so the first
     * free id is normally 7; the loop is defensive in case another mod claimed
     * it before this class was loaded.
     */
    private static final VertexFormatElement PACKED_UV0 = registerPackedUv0();

    /**
     * The compact format used to build the D3D11 input layout.  Attribute names
     * match the terrain shader's inputs, so the same shaders compile unchanged.
     */
    public static final VertexFormat FORMAT = VertexFormat.builder()
        .add(POSITION, VertexFormatElement.POSITION)
        .add(COLOR, VertexFormatElement.COLOR)
        .add(UV0, PACKED_UV0)
        .add(UV2, VertexFormatElement.UV2)
        .build();

    private static final AtomicLong UPLOADS = new AtomicLong();
    private static final AtomicLong VERTICES = new AtomicLong();
    private static final AtomicLong SAVED_BYTES = new AtomicLong();
    private static volatile Set<String> terrainInputs = Set.of();
    private static volatile boolean warnedUnsupported;

    private CompactTerrainFormat() {
    }

    /** The element whose D3D11 format must be R16G16_UNORM. */
    public static VertexFormatElement packedUv0() {
        return PACKED_UV0;
    }

    /**
     * True when {@code format} has the element names and source types this
     * converter understands (the vanilla block format or an equivalent).
     */
    public static boolean supports(VertexFormat format) {
        VertexFormatElement position = element(format, POSITION);
        VertexFormatElement color = element(format, COLOR);
        VertexFormatElement uv0 = element(format, UV0);
        VertexFormatElement uv2 = element(format, UV2);
        return position != null
            && position.type() == VertexFormatElement.Type.FLOAT
            && position.count() == 3
            && color != null
            && color.type() == VertexFormatElement.Type.UBYTE
            && color.count() == 4
            && uv0 != null
            && uv0.type() == VertexFormatElement.Type.FLOAT
            && uv0.count() == 2
            && uv2 != null
            && uv2.type() == VertexFormatElement.Type.SHORT
            && uv2.count() == 2;
    }

    /**
     * Records which vertex inputs the terrain shader actually uses.  A shader
     * pack that reads normal/other data must keep the vanilla 32-byte path;
     * unused declarations (vanilla's {@code Normal}) are ignored because they
     * are stripped from the linked interface.
     */
    public static void noteTerrainShaderInputs(Collection<String> usedInputs) {
        Set<String> copy = Set.copyOf(usedInputs);
        if (copy.isEmpty() || terrainInputs.containsAll(copy)) {
            return;
        }
        // Accumulate across define variants and pipeline types.  A single
        // incompatible variant (for example a shader pack that only reads the
        // normal under one define) must disable compact vertices everywhere,
        // not just until another variant is compiled.
        Set<String> merged = new HashSet<>(terrainInputs);
        merged.addAll(copy);
        terrainInputs = Set.copyOf(merged);
        if (!acceptsShaderInputs(merged)) {
            LOGGER.info(
                "[dx11-perf] Terrain shader inputs {} are not compact-compatible; keeping 32-byte vertices",
                merged);
        }
    }

    /**
     * True when a shader that reads exactly these vertex inputs can use the
     * compact layout.  Empty input sets are rejected: a terrain shader must at
     * least read the position.
     */
    public static boolean acceptsShaderInputs(Collection<String> usedInputs) {
        return usedInputs != null
            && usedInputs.contains(POSITION)
            && COMPACT_INPUTS.containsAll(usedInputs);
    }

    /** Forgets observed shader inputs after a shader/pipeline reload. */
    public static void resetShaderInputs() {
        terrainInputs = Set.of();
        warnedUnsupported = false;
    }

    /** True when the active terrain shader only reads compact-provided inputs. */
    public static boolean shaderCompatible() {
        return acceptsShaderInputs(terrainInputs);
    }

    /** True once the terrain shader has been observed at least once. */
    public static boolean shaderObserved() {
        return !terrainInputs.isEmpty();
    }

    /**
     * Converts vanilla-layout terrain vertices into the compact layout and
     * applies the region-origin delta to every position.
     *
     * @param source        source vertices; its current position is honoured
     * @param target        native-order destination, at least {@code vertexCount * 24} bytes
     * @param vertexCount   number of vertices to convert
     * @param sourceFormat  format describing {@code source}
     */
    public static void convert(
        ByteBuffer source,
        ByteBuffer target,
        int vertexCount,
        VertexFormat sourceFormat,
        float dx,
        float dy,
        float dz) {
        int sourceStride = sourceFormat.getVertexSize();
        int positionOffset = requiredOffset(sourceFormat, POSITION);
        int colorOffset = requiredOffset(sourceFormat, COLOR);
        int uv0Offset = requiredOffset(sourceFormat, UV0);
        int uv2Offset = requiredOffset(sourceFormat, UV2);
        if (vertexCount < 0 || sourceStride <= 0
            || positionOffset + 12 > sourceStride
            || colorOffset + 4 > sourceStride
            || uv0Offset + 8 > sourceStride
            || uv2Offset + 4 > sourceStride
            || target.capacity() < vertexCount * STRIDE) {
            throw new IllegalArgumentException("Invalid compact terrain conversion");
        }

        ByteBuffer src = source.duplicate();
        src.order(ByteOrder.nativeOrder());
        int start = src.position();
        if (src.remaining() < vertexCount * sourceStride) {
            throw new IllegalArgumentException("Source has fewer than the requested vertices");
        }

        target.clear();
        target.order(ByteOrder.nativeOrder());
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            int srcBase = start + vertex * sourceStride;
            int dstBase = vertex * STRIDE;
            target.putFloat(dstBase + POSITION_OFFSET, src.getFloat(srcBase + positionOffset) + dx);
            target.putFloat(dstBase + POSITION_OFFSET + 4, src.getFloat(srcBase + positionOffset + 4) + dy);
            target.putFloat(dstBase + POSITION_OFFSET + 8, src.getFloat(srcBase + positionOffset + 8) + dz);
            // Colour and lightmap coordinates are already in the exact byte
            // layout D3D11 expects; copying the 32-bit word avoids any
            // per-component work.
            target.putInt(dstBase + COLOR_OFFSET, src.getInt(srcBase + colorOffset));
            target.putShort(dstBase + UV0_OFFSET, unorm16(src.getFloat(srcBase + uv0Offset)));
            target.putShort(dstBase + UV0_OFFSET + 2, unorm16(src.getFloat(srcBase + uv0Offset + 4)));
            target.putInt(dstBase + UV2_OFFSET, src.getInt(srcBase + uv2Offset));
        }
        target.position(0);
        target.limit(vertexCount * STRIDE);

        onConverted(vertexCount, sourceStride);
    }

    /** Test/statistics hook for a successful conversion. */
    private static void onConverted(int vertexCount, int sourceStride) {
        UPLOADS.incrementAndGet();
        VERTICES.addAndGet(vertexCount);
        long saved = (long) vertexCount * (sourceStride - STRIDE);
        if (saved > 0) {
            SAVED_BYTES.addAndGet(saved);
        }
    }

    public static String describe() {
        long saved = SAVED_BYTES.get();
        return "compact supported=" + shaderCompatible()
            + " uploads=" + UPLOADS.get()
            + " vertices=" + VERTICES.get()
            + " saved=" + String.format(Locale.ROOT, "%.2f", saved / (1024.0 * 1024.0)) + " MiB";
    }

    public static void resetStats() {
        UPLOADS.set(0);
        VERTICES.set(0);
        SAVED_BYTES.set(0);
    }

    /** Emits at most one warning when compact mode is requested but unusable. */
    static void warnShaderIncompatibleOnce() {
        if (warnedUnsupported || !shaderObserved() || shaderCompatible()) {
            return;
        }
        warnedUnsupported = true;
        LOGGER.warn(
            "[dx11-perf] compactVertices requested, but the terrain shader reads {}; "
                + "keeping 32-byte vertices",
            terrainInputs);
    }

    private static VertexFormatElement element(VertexFormat format, String name) {
        int index = format.getElementAttributeNames().indexOf(name);
        return index < 0 ? null : format.getElements().get(index);
    }

    private static int requiredOffset(VertexFormat format, String name) {
        VertexFormatElement element = element(format, name);
        if (element == null) {
            throw new IllegalArgumentException("Missing terrain element " + name);
        }
        return format.getOffset(element);
    }

    private static short unorm16(float value) {
        float clamped = value < 0.0f ? 0.0f : (value > 1.0f ? 1.0f : value);
        return (short) Math.round(clamped * 65535.0f);
    }

    private static VertexFormatElement registerPackedUv0() {
        for (int id = 7; id < VertexFormatElement.MAX_COUNT; id++) {
            try {
                return VertexFormatElement.register(
                    id,
                    0,
                    VertexFormatElement.Type.USHORT,
                    VertexFormatElement.Usage.UV,
                    2);
            } catch (IllegalArgumentException alreadyRegistered) {
                // Another mod owns this id; try the next free slot.
            }
        }
        throw new IllegalStateException("No free vertex element id for compact terrain UV0");
    }
}
