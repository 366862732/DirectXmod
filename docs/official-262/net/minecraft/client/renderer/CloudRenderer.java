/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector4fc
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class CloudRenderer
extends SimplePreparableReloadListener<Optional<TextureData>>
implements AutoCloseable {
    private static final int FLAG_INSIDE_FACE = 16;
    private static final int FLAG_USE_TOP_COLOR = 32;
    private static final float CELL_SIZE_IN_BLOCKS = 12.0f;
    private static final int TICKS_PER_CELL = 400;
    private static final float BLOCKS_PER_SECOND = 0.6f;
    private static final int UBO_SIZE = new Std140SizeCalculator().putVec4().putVec3().putVec3().get();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/environment/clouds.png");
    private static final long EMPTY_CELL = 0L;
    private static final int COLOR_OFFSET = 4;
    private static final int NORTH_OFFSET = 3;
    private static final int EAST_OFFSET = 2;
    private static final int SOUTH_OFFSET = 1;
    private static final int WEST_OFFSET = 0;
    private boolean needsRebuild = true;
    private int prevCellX = Integer.MIN_VALUE;
    private int prevCellZ = Integer.MIN_VALUE;
    private RelativeCameraPos prevRelativeCameraPos = RelativeCameraPos.INSIDE_CLOUDS;
    private @Nullable CloudStatus prevCloudStatus;
    private @Nullable TextureData texture;
    private int quadCount = 0;
    private final MappableRingBuffer ubo = new MappableRingBuffer(() -> "Cloud UBO", 130, UBO_SIZE);
    private @Nullable MappableRingBuffer utb;

    /*
     * Enabled aggressive exception aggregation
     */
    @Override
    protected Optional<TextureData> prepare(ResourceManager manager, ProfilerFiller profiler) {
        try (InputStream input = manager.open(TEXTURE_LOCATION);){
            NativeImage texture = NativeImage.read(input);
            try {
                int width = texture.getWidth();
                int height = texture.getHeight();
                long[] cells = new long[width * height];
                for (int y = 0; y < height; ++y) {
                    for (int x = 0; x < width; ++x) {
                        int color = texture.getPixel(x, y);
                        if (CloudRenderer.isCellEmpty(color)) {
                            cells[x + y * width] = 0L;
                            continue;
                        }
                        boolean north = CloudRenderer.isCellEmpty(texture.getPixel(x, Math.floorMod(y - 1, height)));
                        boolean east = CloudRenderer.isCellEmpty(texture.getPixel(Math.floorMod(x + 1, height), y));
                        boolean south = CloudRenderer.isCellEmpty(texture.getPixel(x, Math.floorMod(y + 1, height)));
                        boolean west = CloudRenderer.isCellEmpty(texture.getPixel(Math.floorMod(x - 1, height), y));
                        cells[x + y * width] = CloudRenderer.packCellData(color, north, east, south, west);
                    }
                }
                Optional<TextureData> optional = Optional.of(new TextureData(cells, width, height));
                if (texture != null) {
                    texture.close();
                }
                return optional;
            }
            catch (Throwable throwable) {
                if (texture != null) {
                    try {
                        texture.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
        }
        catch (IOException e) {
            LOGGER.error("Failed to load cloud texture", (Throwable)e);
            return Optional.empty();
        }
    }

    private static int getSizeForCloudDistance(int radiusCells) {
        int maxFacesPerCell = 4;
        int maxCells = (radiusCells + 1) * 2 * ((radiusCells + 1) * 2) / 2;
        int maxFaces = maxCells * 4 + 54;
        return maxFaces * 3;
    }

    @Override
    protected void apply(Optional<TextureData> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.texture = preparations.orElse(null);
        this.needsRebuild = true;
    }

    private static boolean isCellEmpty(int color) {
        return ARGB.alpha(color) < 10;
    }

    private static long packCellData(int color, boolean north, boolean east, boolean south, boolean west) {
        return (long)color << 4 | (long)((north ? 1 : 0) << 3) | (long)((east ? 1 : 0) << 2) | (long)((south ? 1 : 0) << 1) | (long)((west ? 1 : 0) << 0);
    }

    private static boolean isNorthEmpty(long cellData) {
        return (cellData >> 3 & 1L) != 0L;
    }

    private static boolean isEastEmpty(long cellData) {
        return (cellData >> 2 & 1L) != 0L;
    }

    private static boolean isSouthEmpty(long cellData) {
        return (cellData >> 1 & 1L) != 0L;
    }

    private static boolean isWestEmpty(long cellData) {
        return (cellData >> 0 & 1L) != 0L;
    }

    public void render(int color, CloudStatus cloudStatus, float bottomY, int range, Vec3 cameraPosition, long gameTime, float partialTicks) {
        GpuTextureView depthTexture;
        GpuTextureView colorTexture;
        GpuBufferSlice.MappedView view;
        RenderPipeline renderPipeline;
        float relativeBottomY;
        float relativeTopY;
        if (this.texture == null) {
            return;
        }
        int radiusBlocks = range * 16;
        int radiusCells = Mth.ceil((float)radiusBlocks / 12.0f);
        int utbSize = CloudRenderer.getSizeForCloudDistance(radiusCells);
        if (this.utb == null || this.utb.currentBuffer().size() != (long)utbSize) {
            if (this.utb != null) {
                this.utb.close();
            }
            this.utb = new MappableRingBuffer(() -> "Cloud UTB", 258, utbSize);
        }
        RelativeCameraPos relativeCameraPos = (relativeTopY = (relativeBottomY = (float)((double)bottomY - cameraPosition.y)) + 4.0f) < 0.0f ? RelativeCameraPos.ABOVE_CLOUDS : (relativeBottomY > 0.0f ? RelativeCameraPos.BELOW_CLOUDS : RelativeCameraPos.INSIDE_CLOUDS);
        float cloudOffset = (float)(gameTime % ((long)this.texture.width * 400L)) + partialTicks;
        double cloudX = cameraPosition.x + (double)(cloudOffset * 0.030000001f);
        double cloudZ = cameraPosition.z + (double)3.96f;
        double textureWidthBlocks = (double)this.texture.width * 12.0;
        double textureHeightBlocks = (double)this.texture.height * 12.0;
        cloudX -= (double)Mth.floor(cloudX / textureWidthBlocks) * textureWidthBlocks;
        cloudZ -= (double)Mth.floor(cloudZ / textureHeightBlocks) * textureHeightBlocks;
        int cellX = Mth.floor(cloudX / 12.0);
        int cellZ = Mth.floor(cloudZ / 12.0);
        float xInCell = (float)(cloudX - (double)((float)cellX * 12.0f));
        float zInCell = (float)(cloudZ - (double)((float)cellZ * 12.0f));
        boolean fancyClouds = cloudStatus == CloudStatus.FANCY;
        RenderPipeline renderPipeline2 = renderPipeline = fancyClouds ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;
        if (this.needsRebuild || cellX != this.prevCellX || cellZ != this.prevCellZ || relativeCameraPos != this.prevRelativeCameraPos || cloudStatus != this.prevCloudStatus) {
            this.needsRebuild = false;
            this.prevCellX = cellX;
            this.prevCellZ = cellZ;
            this.prevRelativeCameraPos = relativeCameraPos;
            this.prevCloudStatus = cloudStatus;
            this.utb.rotate();
            view = this.utb.currentBuffer().map(false, true);
            try {
                this.buildMesh(relativeCameraPos, view.data(), cellX, cellZ, fancyClouds, radiusCells);
                this.quadCount = view.data().position() / 3;
            }
            finally {
                if (view != null) {
                    view.close();
                }
            }
        }
        if (this.quadCount == 0) {
            return;
        }
        view = this.ubo.currentBuffer().map(false, true);
        try {
            Std140Builder.intoBuffer(view.data()).putVec4((Vector4fc)ARGB.vector4fFromARGB32(color)).putVec3(-xInCell, relativeBottomY, -zInCell).putVec3(12.0f, 4.0f, 12.0f);
        }
        finally {
            if (view != null) {
                view.close();
            }
        }
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy());
        RenderTarget mainRenderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        RenderTarget cloudTarget = Minecraft.getInstance().levelRenderer.cloudsTarget();
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = indices.getBuffer(6 * this.quadCount);
        if (cloudTarget != null) {
            colorTexture = cloudTarget.getColorTextureView();
            depthTexture = cloudTarget.getDepthTextureView();
        } else {
            colorTexture = mainRenderTarget.getColorTextureView();
            depthTexture = mainRenderTarget.getDepthTextureView();
        }
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Clouds", colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty());){
            renderPass.setPipeline(renderPipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setIndexBuffer(indexBuffer, indices.type());
            renderPass.setUniform("CloudInfo", this.ubo.currentBuffer());
            renderPass.setUniform("CloudFaces", this.utb.currentBuffer());
            renderPass.drawIndexed(6 * this.quadCount, 1, 0, 0, 0);
        }
    }

    private void buildMesh(RelativeCameraPos relativePos, ByteBuffer faceBuffer, int centerCellX, int centerCellZ, boolean extrude, int radiusCells) {
        if (this.texture == null) {
            return;
        }
        long[] cells = this.texture.cells;
        int textureWidth = this.texture.width;
        int textureHeight = this.texture.height;
        for (int ring = 0; ring <= 2 * radiusCells; ++ring) {
            for (int relativeCellX = -ring; relativeCellX <= ring; ++relativeCellX) {
                int relativeCellZ = ring - Math.abs(relativeCellX);
                if (relativeCellZ < 0 || relativeCellZ > radiusCells || relativeCellX * relativeCellX + relativeCellZ * relativeCellZ > radiusCells * radiusCells) continue;
                if (relativeCellZ != 0) {
                    this.tryBuildCell(relativePos, faceBuffer, centerCellX, centerCellZ, extrude, relativeCellX, textureWidth, -relativeCellZ, textureHeight, cells);
                }
                this.tryBuildCell(relativePos, faceBuffer, centerCellX, centerCellZ, extrude, relativeCellX, textureWidth, relativeCellZ, textureHeight, cells);
            }
        }
    }

    private void tryBuildCell(RelativeCameraPos relativePos, ByteBuffer faceBuffer, int cellX, int cellZ, boolean extrude, int relativeCellX, int textureWidth, int relativeCellZ, int textureHeight, long[] cells) {
        int indexY;
        int indexX = Math.floorMod(cellX + relativeCellX, textureWidth);
        long cellData = cells[indexX + (indexY = Math.floorMod(cellZ + relativeCellZ, textureHeight)) * textureWidth];
        if (cellData == 0L) {
            return;
        }
        if (extrude) {
            this.buildExtrudedCell(relativePos, faceBuffer, relativeCellX, relativeCellZ, cellData);
        } else {
            this.buildFlatCell(faceBuffer, relativeCellX, relativeCellZ);
        }
    }

    private void buildFlatCell(ByteBuffer faceBuffer, int x, int z) {
        this.encodeFace(faceBuffer, x, z, Direction.DOWN, 32);
    }

    private void encodeFace(ByteBuffer faceBuffer, int x, int z, Direction direction, int flags) {
        int dirAndFlags = direction.get3DDataValue() | flags;
        dirAndFlags |= (x & 1) << 7;
        faceBuffer.put((byte)(x >> 1)).put((byte)(z >> 1)).put((byte)(dirAndFlags |= (z & 1) << 6));
    }

    private void buildExtrudedCell(RelativeCameraPos relativePos, ByteBuffer faceBuffer, int x, int z, long cellData) {
        boolean addInteriorFaces;
        if (relativePos != RelativeCameraPos.BELOW_CLOUDS) {
            this.encodeFace(faceBuffer, x, z, Direction.UP, 0);
        }
        if (relativePos != RelativeCameraPos.ABOVE_CLOUDS) {
            this.encodeFace(faceBuffer, x, z, Direction.DOWN, 0);
        }
        if (CloudRenderer.isNorthEmpty(cellData) && z > 0) {
            this.encodeFace(faceBuffer, x, z, Direction.NORTH, 0);
        }
        if (CloudRenderer.isSouthEmpty(cellData) && z < 0) {
            this.encodeFace(faceBuffer, x, z, Direction.SOUTH, 0);
        }
        if (CloudRenderer.isWestEmpty(cellData) && x > 0) {
            this.encodeFace(faceBuffer, x, z, Direction.WEST, 0);
        }
        if (CloudRenderer.isEastEmpty(cellData) && x < 0) {
            this.encodeFace(faceBuffer, x, z, Direction.EAST, 0);
        }
        boolean bl = addInteriorFaces = Math.abs(x) <= 1 && Math.abs(z) <= 1;
        if (addInteriorFaces) {
            for (Direction direction : Direction.values()) {
                this.encodeFace(faceBuffer, x, z, direction, 16);
            }
        }
    }

    public void markForRebuild() {
        this.needsRebuild = true;
    }

    public void endFrame() {
        this.ubo.rotate();
    }

    @Override
    public void close() {
        this.ubo.close();
        if (this.utb != null) {
            this.utb.close();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static enum RelativeCameraPos {
        ABOVE_CLOUDS,
        INSIDE_CLOUDS,
        BELOW_CLOUDS;

    }

    @Environment(value=EnvType.CLIENT)
    public record TextureData(long[] cells, int width, int height) {
    }
}

