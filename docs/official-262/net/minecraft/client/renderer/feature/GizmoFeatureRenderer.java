/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionfc
 *  org.joml.Vector4f
 *  org.joml.Vector4fc
 */
package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

@Environment(value=EnvType.CLIENT)
public class GizmoFeatureRenderer
extends RenderTypeFeatureRenderer<Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("Gizmo");
    private final PoseStack poseStack = new PoseStack();

    @Override
    protected void buildGroup(FeatureFrameContext context, List<Submit> submits) {
        Matrix4f modelViewMatrix = RenderSystem.getModelViewMatrixCopy();
        for (Submit submit : submits) {
            CameraRenderState camera = submit.camera;
            this.buildQuads(submit.group.quads(), camera);
            this.buildTriangleFans(submit.group.triangleFans(), camera);
            this.buildLines(submit.group.lines(), camera, (Matrix4fc)modelViewMatrix, submit.group.opaque());
            this.buildTexts(submit.group.texts(), camera, context.font());
            this.buildPoints(submit.group.points(), camera);
        }
    }

    private void buildTexts(List<DrawableGizmoPrimitives.Text> texts, CameraRenderState camera, Font font) {
        if (texts.isEmpty() || !camera.initialized) {
            return;
        }
        double camX = camera.pos.x();
        double camY = camera.pos.y();
        double camZ = camera.pos.z();
        for (DrawableGizmoPrimitives.Text text : texts) {
            TextGizmo.Style style = text.style();
            this.poseStack.pushPose();
            this.poseStack.translate((float)(text.pos().x() - camX), (float)(text.pos().y() - camY), (float)(text.pos().z() - camZ));
            this.poseStack.mulPose((Quaternionfc)camera.orientation);
            this.poseStack.scale(style.scale() / 16.0f, -style.scale() / 16.0f, style.scale() / 16.0f);
            float fontX = style.adjustLeft().isEmpty() ? (float)(-font.width(text.text())) / 2.0f : (float)(-style.adjustLeft().getAsDouble()) / style.scale();
            final Matrix4f pose = this.poseStack.last().pose();
            Font.PreparedText preparedText = font.prepareText(text.text(), fontX, 0.0f, style.color(), false, 0);
            preparedText.visit(new Font.GlyphVisitor(){
                final /* synthetic */ GizmoFeatureRenderer this$0;
                {
                    GizmoFeatureRenderer gizmoFeatureRenderer = this$0;
                    Objects.requireNonNull(gizmoFeatureRenderer);
                    this.this$0 = gizmoFeatureRenderer;
                }

                @Override
                public void acceptRenderable(TextRenderable renderable) {
                    VertexConsumer buffer = this.this$0.getVertexBuilder(renderable.renderType(Font.DisplayMode.NORMAL));
                    renderable.render((Matrix4fc)pose, buffer, 0xF000F0, false);
                }
            });
            this.poseStack.popPose();
        }
    }

    private void buildLines(List<DrawableGizmoPrimitives.Line> lines, CameraRenderState camera, Matrix4fc modelViewMatrix, boolean opaque) {
        if (lines.isEmpty()) {
            return;
        }
        VertexConsumer builder = this.getVertexBuilder(opaque ? RenderTypes.lines() : RenderTypes.linesTranslucent());
        PoseStack.Pose pose = this.poseStack.last();
        Vector4f start = new Vector4f();
        Vector4f end = new Vector4f();
        Vector4f startViewSpace = new Vector4f();
        Vector4f endViewSpace = new Vector4f();
        Vector4f intersectionInWorld = new Vector4f();
        double camX = camera.pos.x();
        double camY = camera.pos.y();
        double camZ = camera.pos.z();
        for (DrawableGizmoPrimitives.Line line : lines) {
            boolean endIsBehindCamera;
            start.set(line.start().x() - camX, line.start().y() - camY, line.start().z() - camZ, 1.0);
            end.set(line.end().x() - camX, line.end().y() - camY, line.end().z() - camZ, 1.0);
            start.mul(modelViewMatrix, startViewSpace);
            end.mul(modelViewMatrix, endViewSpace);
            boolean startIsBehindCamera = startViewSpace.z > -0.05f;
            boolean bl = endIsBehindCamera = endViewSpace.z > -0.05f;
            if (startIsBehindCamera && endIsBehindCamera) continue;
            if (startIsBehindCamera || endIsBehindCamera) {
                float denom = endViewSpace.z - startViewSpace.z;
                if (Math.abs(denom) < 1.0E-9f) continue;
                float intersection = Mth.clamp((-0.05f - startViewSpace.z) / denom, 0.0f, 1.0f);
                start.lerp((Vector4fc)end, intersection, intersectionInWorld);
                if (startIsBehindCamera) {
                    start.set((Vector4fc)intersectionInWorld);
                } else {
                    end.set((Vector4fc)intersectionInWorld);
                }
            }
            builder.addVertex(pose, start.x, start.y, start.z).setNormal(pose, end.x - start.x, end.y - start.y, end.z - start.z).setColor(line.color()).setLineWidth(line.width());
            builder.addVertex(pose, end.x, end.y, end.z).setNormal(pose, end.x - start.x, end.y - start.y, end.z - start.z).setColor(line.color()).setLineWidth(line.width());
        }
    }

    private void buildTriangleFans(List<DrawableGizmoPrimitives.TriangleFan> triangleFans, CameraRenderState camera) {
        if (triangleFans.isEmpty()) {
            return;
        }
        PoseStack.Pose pose = this.poseStack.last();
        double camX = camera.pos.x();
        double camY = camera.pos.y();
        double camZ = camera.pos.z();
        for (DrawableGizmoPrimitives.TriangleFan triangleFan : triangleFans) {
            VertexConsumer builder = this.getVertexBuilder(RenderTypes.debugTriangleFan());
            for (Vec3 point : triangleFan.points()) {
                builder.addVertex(pose, (float)(point.x() - camX), (float)(point.y() - camY), (float)(point.z() - camZ)).setColor(triangleFan.color());
            }
        }
    }

    private void buildQuads(List<DrawableGizmoPrimitives.Quad> quads, CameraRenderState camera) {
        if (quads.isEmpty()) {
            return;
        }
        VertexConsumer builder = this.getVertexBuilder(RenderTypes.debugFilledBox());
        PoseStack.Pose pose = this.poseStack.last();
        double camX = camera.pos.x();
        double camY = camera.pos.y();
        double camZ = camera.pos.z();
        for (DrawableGizmoPrimitives.Quad quad : quads) {
            builder.addVertex(pose, (float)(quad.a().x() - camX), (float)(quad.a().y() - camY), (float)(quad.a().z() - camZ)).setColor(quad.color());
            builder.addVertex(pose, (float)(quad.b().x() - camX), (float)(quad.b().y() - camY), (float)(quad.b().z() - camZ)).setColor(quad.color());
            builder.addVertex(pose, (float)(quad.c().x() - camX), (float)(quad.c().y() - camY), (float)(quad.c().z() - camZ)).setColor(quad.color());
            builder.addVertex(pose, (float)(quad.d().x() - camX), (float)(quad.d().y() - camY), (float)(quad.d().z() - camZ)).setColor(quad.color());
        }
    }

    private void buildPoints(List<DrawableGizmoPrimitives.Point> points, CameraRenderState camera) {
        if (points.isEmpty()) {
            return;
        }
        VertexConsumer builder = this.getVertexBuilder(RenderTypes.debugPoint());
        PoseStack.Pose pose = this.poseStack.last();
        double camX = camera.pos.x();
        double camY = camera.pos.y();
        double camZ = camera.pos.z();
        for (DrawableGizmoPrimitives.Point point : points) {
            Vec3 pos = point.pos();
            builder.addVertex(pose, (float)(pos.x() - camX), (float)(pos.y() - camY), (float)(pos.z() - camZ)).setColor(point.color()).setLineWidth(point.size());
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Submit(DrawableGizmoPrimitives.Group group, CameraRenderState camera) implements SubmitNode
    {
        public FeatureRendererType<Submit> featureType() {
            return TYPE;
        }
    }
}

