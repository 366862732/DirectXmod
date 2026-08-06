/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class RenderTypeFeatureRenderer<Submit extends SubmitNode>
implements FeatureRenderer<Submit> {
    private @Nullable Group currentGroup;
    private final List<Group> groups = new ArrayList<Group>();

    protected abstract void buildGroup(FeatureFrameContext var1, List<Submit> var2);

    protected final VertexConsumer getVertexBuilder(RenderType renderType) {
        return this.currentGroup().getVertexBuilder(renderType);
    }

    private Group currentGroup() {
        return Objects.requireNonNull(this.currentGroup, "Not preparing group");
    }

    @Override
    public final void prepareGroup(FeatureFrameContext context, List<Submit> submits, boolean strictlyOrdered) {
        this.currentGroup = new Group(context.stagedVertexBuffer(), !strictlyOrdered);
        this.buildGroup(context, submits);
        this.groups.add(this.currentGroup);
        this.currentGroup = null;
    }

    @Override
    public void executeGroup(FeatureFrameContext context, int groupIndex, List<Submit> submits, boolean strictlyOrdered) {
        Group group = this.groups.get(groupIndex);
        for (int i = 0; i < group.draws.size(); ++i) {
            PreparedRenderType renderType = group.drawRenderTypes.get(i);
            StagedVertexBuffer.ExecuteInfo info = context.stagedVertexBuffer().getExecuteInfo(group.draws.get(i));
            if (info == null) continue;
            renderType.drawFromBuffer(info);
        }
    }

    @Override
    public void finishExecute(FeatureFrameContext context) {
        this.groups.clear();
    }

    @Environment(value=EnvType.CLIENT)
    private static class Group {
        private final StagedVertexBuffer stagedBuffer;
        private final boolean canReorder;
        private final List<StagedVertexBuffer.Draw> draws = new ArrayList<StagedVertexBuffer.Draw>();
        private final List<PreparedRenderType> drawRenderTypes = new ArrayList<PreparedRenderType>();
        private @Nullable RenderType lastRenderType;
        private @Nullable StagedVertexBuffer.Draw lastDraw;

        private Group(StagedVertexBuffer stagedBuffer, boolean canReorder) {
            this.stagedBuffer = stagedBuffer;
            this.canReorder = canReorder;
        }

        public VertexConsumer getVertexBuilder(RenderType renderType) {
            if (this.lastDraw == null || this.lastRenderType != renderType || !renderType.canConsolidateConsecutiveGeometry()) {
                this.lastDraw = this.getOrAddDraw(renderType);
                this.lastRenderType = renderType;
            }
            return this.stagedBuffer.getVertexBuilder(this.lastDraw);
        }

        private StagedVertexBuffer.Draw getOrAddDraw(RenderType renderType) {
            int existingIndex;
            PreparedRenderType preparedRenderType = renderType.prepare();
            int n = existingIndex = this.canReorder && renderType.canConsolidateConsecutiveGeometry() ? this.drawRenderTypes.indexOf(preparedRenderType) : -1;
            if (existingIndex != -1) {
                return this.draws.get(existingIndex);
            }
            VertexSorting quadSorting = renderType.sortOnUpload() ? RenderSystem.getProjectionType().vertexSorting() : null;
            StagedVertexBuffer.Draw draw = this.stagedBuffer.appendDraw(renderType.format(), renderType.primitiveTopology(), quadSorting);
            this.draws.add(draw);
            this.drawRenderTypes.add(preparedRenderType);
            return draw;
        }
    }
}

