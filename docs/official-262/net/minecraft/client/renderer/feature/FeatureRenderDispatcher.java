/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import net.minecraft.client.renderer.feature.ShapeOutlineFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class FeatureRenderDispatcher
implements AutoCloseable {
    private final ModelManager modelManager;
    private final AtlasManager atlasManager;
    private final Font font;
    private final GameRenderState gameRenderState;
    private final StagedVertexBuffer stagedVertexBuffer;
    private final FeatureRendererMap featureRenderers = new FeatureRendererMap();
    private final PreparedFrame preparedFrame = new PreparedFrame(this);

    public FeatureRenderDispatcher(RenderBuffers renderBuffers, ModelManager modelManager, AtlasManager atlasManager, Font font, GameRenderState gameRenderState) {
        this.modelManager = modelManager;
        this.atlasManager = atlasManager;
        this.font = font;
        this.gameRenderState = gameRenderState;
        this.stagedVertexBuffer = renderBuffers.stagedVertexBuffer();
        this.featureRenderers.put(ShadowFeatureRenderer.TYPE, new ShadowFeatureRenderer());
        this.featureRenderers.put(FlameFeatureRenderer.TYPE, new FlameFeatureRenderer());
        this.featureRenderers.put(ModelFeatureRenderer.TYPE, new ModelFeatureRenderer());
        this.featureRenderers.put(NameTagFeatureRenderer.TYPE, new NameTagFeatureRenderer());
        this.featureRenderers.put(TextFeatureRenderer.TYPE, new TextFeatureRenderer());
        this.featureRenderers.put(LeashFeatureRenderer.TYPE, new LeashFeatureRenderer());
        this.featureRenderers.put(ItemFeatureRenderer.TYPE, new ItemFeatureRenderer());
        this.featureRenderers.put(CustomFeatureRenderer.TYPE, new CustomFeatureRenderer());
        this.featureRenderers.put(BlockModelFeatureRenderer.TYPE, new BlockModelFeatureRenderer());
        this.featureRenderers.put(MovingBlockFeatureRenderer.TYPE, new MovingBlockFeatureRenderer());
        this.featureRenderers.put(QuadParticleFeatureRenderer.TYPE, new QuadParticleFeatureRenderer());
        this.featureRenderers.put(ShapeOutlineFeatureRenderer.TYPE, new ShapeOutlineFeatureRenderer());
        this.featureRenderers.put(GizmoFeatureRenderer.TYPE, new GizmoFeatureRenderer());
    }

    public PreparedFrame prepareFrame(SubmitNodeStorage submitNodeStorage) {
        Minecraft minecraft = Minecraft.getInstance();
        return this.prepareFrameWithContext(new FeatureFrameContext(this.gameRenderState.optionsRenderState, this.font, this.modelManager.getBlockStateModelSet(), minecraft.getBlockColors(), minecraft.getTextureManager(), this.atlasManager, minecraft.gameRenderer.lightmap(), this.stagedVertexBuffer), submitNodeStorage);
    }

    private PreparedFrame prepareFrameWithContext(FeatureFrameContext context, SubmitNodeStorage submitNodeStorage) {
        PreparedFrame frame = this.preparedFrame.begin(context, submitNodeStorage);
        ProfilerFiller profiler = Profiler.get();
        profiler.push("sort");
        submitNodeStorage.drainPhases(phase -> phase.sortInto(new PhaseSubmitGrouper(frame, (FeatureRenderPhase<?>)phase)));
        profiler.popPush("beginPrepare");
        for (FeatureRenderer<?> featureRenderer : this.featureRenderers.values()) {
            featureRenderer.beginPrepare(context);
        }
        profiler.popPush("prepare");
        for (Map.Entry entry : frame.groupsByFeature.entrySet()) {
            profiler.push(((FeatureRendererType)entry.getKey()).toString());
            for (PreparedGroup group : (List)entry.getValue()) {
                group.prepare(context, this.featureRenderers, frame.allSubmits);
            }
            profiler.pop();
        }
        profiler.popPush("finishPrepare");
        for (FeatureRenderer featureRenderer : this.featureRenderers.values()) {
            featureRenderer.finishPrepare(context);
        }
        profiler.popPush("uploadSharedVertexBuffer");
        this.stagedVertexBuffer.upload();
        profiler.pop();
        return frame;
    }

    public void renderAllFeatures(SubmitNodeStorage submitNodeStorage) {
        try (PreparedFrame frame = this.prepareFrame(submitNodeStorage);){
            frame.executeSolid();
            frame.executeTranslucent();
            frame.executeTranslucentAfterTerrain();
            frame.executeAlwaysOnTop();
        }
    }

    @Override
    public void close() {
        this.featureRenderers.close();
    }

    @Environment(value=EnvType.CLIENT)
    public class PreparedFrame
    implements AutoCloseable {
        private @Nullable FeatureFrameContext context;
        private @Nullable SubmitNodeStorage submitNodeStorage;
        private final List<SubmitNode> allSubmits;
        private final Map<FeatureRenderPhase<?>, List<PreparedGroup<?>>> groupsByPhase;
        private final Map<FeatureRendererType<?>, List<PreparedGroup<?>>> groupsByFeature;
        final /* synthetic */ FeatureRenderDispatcher this$0;

        public PreparedFrame(FeatureRenderDispatcher this$0) {
            FeatureRenderDispatcher featureRenderDispatcher = this$0;
            Objects.requireNonNull(featureRenderDispatcher);
            this.this$0 = featureRenderDispatcher;
            this.allSubmits = new ArrayList<SubmitNode>();
            this.groupsByPhase = new IdentityHashMap();
            this.groupsByFeature = new IdentityHashMap();
        }

        private PreparedFrame begin(FeatureFrameContext context, SubmitNodeStorage submitNodeStorage) {
            if (this.context != null) {
                throw new IllegalStateException("PreparedFrame already in use");
            }
            this.context = context;
            this.submitNodeStorage = submitNodeStorage;
            return this;
        }

        public void executeSolid() {
            FeatureFrameContext context = Objects.requireNonNull(this.context);
            SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
            for (SubmitNodeCollection collection : submitNodeStorage.getSubmitsPerOrder().values()) {
                this.executePhase(collection.solid, context);
            }
        }

        public void executeTranslucent() {
            FeatureFrameContext context = Objects.requireNonNull(this.context);
            SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
            for (SubmitNodeCollection collection : submitNodeStorage.getSubmitsPerOrder().values()) {
                this.executePhase(collection.shadows, context);
                this.executePhase(collection.translucentModels, context);
                this.executePhase(collection.seeThroughNameTags, context);
                this.executePhase(collection.nameTags, context);
                this.executePhase(collection.texts, context);
                this.executePhase(collection.translucentCustomGeometry, context);
            }
            for (SubmitNodeCollection collection : submitNodeStorage.getSubmitsPerOrder().values()) {
                this.executePhase(collection.shapeOutlines, context);
                this.executePhase(collection.gizmos, context);
            }
            for (SubmitNodeCollection collection : submitNodeStorage.getSubmitsPerOrder().values()) {
                this.executePhase(collection.translucentBlocksAndItems, context);
                this.executePhase(collection.breakingOverlay, context);
                this.executePhase(collection.waterMask, context);
            }
        }

        public void executeOutline() {
            FeatureFrameContext context = Objects.requireNonNull(this.context);
            SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
            for (SubmitNodeCollection collection : submitNodeStorage.getSubmitsPerOrder().values()) {
                this.executePhase(collection.outline, context);
            }
        }

        public void executeTranslucentAfterTerrain() {
            FeatureFrameContext context = Objects.requireNonNull(this.context);
            SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
            for (SubmitNodeCollection collection : submitNodeStorage.getSubmitsPerOrder().values()) {
                this.executePhase(collection.afterTerrain, context);
            }
        }

        public void executeAlwaysOnTop() {
            FeatureFrameContext context = Objects.requireNonNull(this.context);
            SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
            for (SubmitNodeCollection collection : submitNodeStorage.getSubmitsPerOrder().values()) {
                this.executePhase(collection.alwaysOnTop, context);
            }
        }

        private void executePhase(FeatureRenderPhase<?> phase, FeatureFrameContext context) {
            ProfilerFiller profiler = Profiler.get();
            for (PreparedGroup group : this.groupsByPhase.getOrDefault(phase, List.of())) {
                profiler.push(group.featureType.toString());
                group.execute(context, this.this$0.featureRenderers, this.allSubmits);
                profiler.pop();
            }
        }

        public boolean hasAnyAlwaysOnTop() {
            SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
            for (SubmitNodeCollection collection : submitNodeStorage.getSubmitsPerOrder().values()) {
                if (this.groupsByPhase.getOrDefault(collection.alwaysOnTop, List.of()).isEmpty()) continue;
                return true;
            }
            return false;
        }

        public boolean hasAnyOutline() {
            SubmitNodeStorage submitNodeStorage = Objects.requireNonNull(this.submitNodeStorage);
            for (SubmitNodeCollection collection : submitNodeStorage.getSubmitsPerOrder().values()) {
                if (this.groupsByPhase.getOrDefault(collection.outline, List.of()).isEmpty()) continue;
                return true;
            }
            return false;
        }

        @Override
        public void close() {
            FeatureFrameContext context = Objects.requireNonNull(this.context, "Frame not in use");
            this.context = null;
            this.submitNodeStorage = null;
            for (FeatureRenderer<?> featureRenderer : this.this$0.featureRenderers.values()) {
                featureRenderer.finishExecute(context);
            }
            this.this$0.stagedVertexBuffer.endDraw();
            this.allSubmits.clear();
            PreparedFrame.clearGroups(this.groupsByPhase.values());
            PreparedFrame.clearGroups(this.groupsByFeature.values());
        }

        private static void clearGroups(Collection<List<PreparedGroup<?>>> groupsSet) {
            groupsSet.removeIf(groups -> {
                if (groups.isEmpty()) {
                    return true;
                }
                groups.clear();
                return false;
            });
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class PreparedGroup<Submit extends SubmitNode> {
        private final int featureGroupIndex;
        private final FeatureRendererType<Submit> featureType;
        private final boolean strictlyOrdered;
        private final int fromInclusive;
        private int toInclusive;

        public PreparedGroup(int featureGroupIndex, FeatureRendererType<Submit> featureType, boolean strictlyOrdered, int fromInclusive, int toInclusive) {
            this.featureGroupIndex = featureGroupIndex;
            this.featureType = featureType;
            this.strictlyOrdered = strictlyOrdered;
            this.fromInclusive = fromInclusive;
            this.toInclusive = toInclusive;
        }

        public void prepare(FeatureFrameContext context, FeatureRendererMap featureRenderers, List<SubmitNode> submits) {
            FeatureRenderer<Submit> featureRenderer = featureRenderers.getOrThrow(this.featureType);
            featureRenderer.prepareGroup(context, this.sliceUnchecked(submits), this.strictlyOrdered);
        }

        public void execute(FeatureFrameContext context, FeatureRendererMap featureRenderers, List<SubmitNode> submits) {
            FeatureRenderer<Submit> featureRenderer = featureRenderers.getOrThrow(this.featureType);
            featureRenderer.executeGroup(context, this.featureGroupIndex, this.sliceUnchecked(submits), this.strictlyOrdered);
        }

        private List<Submit> sliceUnchecked(List<SubmitNode> submits) {
            return submits.subList(this.fromInclusive, this.toInclusive + 1);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class PhaseSubmitGrouper
    implements FeatureRenderPhase.Output {
        private final PreparedFrame frame;
        private final List<SubmitNode> allSubmits;
        private final List<PreparedGroup<?>> phaseGroups;
        private @Nullable PreparedGroup<?> lastGroup;

        public PhaseSubmitGrouper(PreparedFrame frame, FeatureRenderPhase<?> phase) {
            this.frame = frame;
            this.allSubmits = frame.allSubmits;
            this.phaseGroups = frame.groupsByPhase.computeIfAbsent(phase, featureRenderPhase -> new ArrayList());
        }

        @Override
        public void accept(SubmitNode submit, boolean strictlyOrdered) {
            int index = this.allSubmits.size();
            this.allSubmits.add(submit);
            this.addOrExtendGroup(submit.featureType(), strictlyOrdered, index, index);
        }

        @Override
        public <Submit extends SubmitNode> void acceptFeatureGroup(FeatureRendererType<Submit> featureType, Collection<Submit> submits, boolean strictlyOrdered) {
            if (!submits.isEmpty()) {
                for (SubmitNode submit : submits) {
                    if (submit.featureType() == featureType) continue;
                    throw new IllegalArgumentException(String.valueOf(submit) + " was not of feature type " + String.valueOf(featureType));
                }
                int fromInclusive = this.allSubmits.size();
                this.allSubmits.addAll(submits);
                int toInclusive = this.allSubmits.size() - 1;
                this.addOrExtendGroup(featureType, strictlyOrdered, fromInclusive, toInclusive);
            }
        }

        private <Submit extends SubmitNode> void addOrExtendGroup(FeatureRendererType<Submit> featureType, boolean strictlyOrdered, int fromInclusive, int toInclusive) {
            if (this.lastGroup != null && this.lastGroup.featureType == featureType && this.lastGroup.strictlyOrdered == strictlyOrdered) {
                this.lastGroup.toInclusive = toInclusive;
                return;
            }
            List featureGroups = this.frame.groupsByFeature.computeIfAbsent(featureType, featureRendererType -> new ArrayList());
            PreparedGroup<Submit> group = new PreparedGroup<Submit>(featureGroups.size(), featureType, strictlyOrdered, fromInclusive, toInclusive);
            this.phaseGroups.add(group);
            featureGroups.add(group);
            this.lastGroup = group;
        }
    }
}

