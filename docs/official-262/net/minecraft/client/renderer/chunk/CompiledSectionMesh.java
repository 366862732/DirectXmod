/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.vertex.MeshData;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class CompiledSectionMesh
implements SectionMesh {
    public static final SectionMesh UNCOMPILED = new SectionMesh(){

        @Override
        public boolean facesCanSeeEachother(Direction direction1, Direction direction2) {
            return false;
        }
    };
    public static final SectionMesh EMPTY = new SectionMesh(){

        @Override
        public boolean facesCanSeeEachother(Direction direction1, Direction direction2) {
            return true;
        }
    };
    private final List<BlockEntity> renderableBlockEntities;
    private final VisibilitySet visibilitySet;
    private final @Nullable MeshData.SortState transparencyState;
    private @Nullable TranslucencyPointOfView translucencyPointOfView;
    private final Map<ChunkSectionLayer, SectionMesh.SectionDraw> draws = new EnumMap<ChunkSectionLayer, SectionMesh.SectionDraw>(ChunkSectionLayer.class);
    private final Map<ChunkSectionLayer, AtomicBoolean> vertexBufferUploaded = Util.makeEnumMap(ChunkSectionLayer.class, layer -> new AtomicBoolean());
    private final Map<ChunkSectionLayer, AtomicBoolean> indexBufferUploaded = Util.makeEnumMap(ChunkSectionLayer.class, layer -> new AtomicBoolean());

    public CompiledSectionMesh(TranslucencyPointOfView translucencyPointOfView, SectionCompiler.Results results) {
        this.translucencyPointOfView = translucencyPointOfView;
        this.visibilitySet = results.visibilitySet;
        this.renderableBlockEntities = results.blockEntities;
        this.transparencyState = results.transparencyState;
        results.renderedLayers.forEach((layer, mesh) -> this.draws.put((ChunkSectionLayer)((Object)layer), new SectionMesh.SectionDraw(mesh.drawState().indexCount(), mesh.drawState().indexType(), mesh.indexBuffer() != null)));
    }

    public void setTranslucencyPointOfView(TranslucencyPointOfView translucencyPointOfView) {
        this.translucencyPointOfView = translucencyPointOfView;
    }

    @Override
    public boolean isDifferentPointOfView(TranslucencyPointOfView pointOfView) {
        return !pointOfView.equals(this.translucencyPointOfView);
    }

    @Override
    public boolean hasRenderableLayers() {
        return !this.draws.isEmpty();
    }

    @Override
    public boolean isEmpty(ChunkSectionLayer layer) {
        return !this.draws.containsKey((Object)layer);
    }

    @Override
    public List<BlockEntity> getRenderableBlockEntities() {
        return this.renderableBlockEntities;
    }

    @Override
    public boolean facesCanSeeEachother(Direction direction1, Direction direction2) {
        return this.visibilitySet.visibilityBetween(direction1, direction2);
    }

    @Override
    public @Nullable SectionMesh.SectionDraw getSectionDraw(ChunkSectionLayer layer) {
        return this.draws.get((Object)layer);
    }

    public boolean isVertexBufferUploaded(ChunkSectionLayer layer) {
        return this.vertexBufferUploaded.get((Object)layer).get();
    }

    public boolean isIndexBufferUploaded(ChunkSectionLayer layer) {
        return this.indexBufferUploaded.get((Object)layer).get();
    }

    public void setVertexBufferUploaded(ChunkSectionLayer layer) {
        this.vertexBufferUploaded.get((Object)layer).set(true);
    }

    public void setIndexBufferUploaded(ChunkSectionLayer layer) {
        this.indexBufferUploaded.get((Object)layer).set(true);
    }

    @Override
    public boolean hasTranslucentGeometry() {
        return this.draws.containsKey((Object)ChunkSectionLayer.TRANSLUCENT);
    }

    public @Nullable MeshData.SortState getTransparencyState() {
        return this.transparencyState;
    }

    @Override
    public void close() {
        this.draws.clear();
        this.vertexBufferUploaded.clear();
        this.indexBufferUploaded.clear();
    }
}

