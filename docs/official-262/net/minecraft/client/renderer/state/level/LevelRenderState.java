/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.state.level;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.state.level.SectionUpdateRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.client.renderer.state.level.WorldBorderRenderState;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class LevelRenderState {
    public CameraRenderState cameraRenderState = new CameraRenderState();
    public final List<SectionUpdateRenderState> sectionUpdateRenderStates = new ArrayList<SectionUpdateRenderState>();
    public final List<EntityRenderState> entityRenderStates = new ArrayList<EntityRenderState>();
    public final List<BlockEntityRenderState> blockEntityRenderStates = new ArrayList<BlockEntityRenderState>();
    public @Nullable BlockOutlineRenderState blockOutlineRenderState;
    public final List<BlockBreakingRenderState> blockBreakingRenderStates = new ArrayList<BlockBreakingRenderState>();
    public final WeatherRenderState weatherRenderState = new WeatherRenderState();
    public final WorldBorderRenderState worldBorderRenderState = new WorldBorderRenderState();
    public final SkyRenderState skyRenderState = new SkyRenderState();
    public final ParticlesRenderState particlesRenderState = new ParticlesRenderState();
    public long gameTime;
    public int lastEntityRenderStateCount;
    public int cloudColor;
    public float cloudHeight;
    public boolean render3dCrosshair;
    public @Nullable Runnable playerCompiledSectionCallback;
    public ChunkLoadingRenderState chunkLoadingRenderState = new ChunkLoadingRenderState();
    public boolean shouldResetChunkLayerSampler;
    public boolean shouldShowEntityOutlines;
    public boolean shouldResetSkyRenderer;

    public void reset() {
        this.sectionUpdateRenderStates.clear();
        this.entityRenderStates.clear();
        this.blockEntityRenderStates.clear();
        this.blockBreakingRenderStates.clear();
        this.blockOutlineRenderState = null;
        this.weatherRenderState.reset();
        this.worldBorderRenderState.reset();
        this.skyRenderState.reset();
        this.particlesRenderState.reset();
        this.chunkLoadingRenderState.reset();
        this.gameTime = 0L;
    }
}

