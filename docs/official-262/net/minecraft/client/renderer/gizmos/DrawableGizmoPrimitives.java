/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.gizmos;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class DrawableGizmoPrimitives
implements GizmoPrimitives {
    private final Group opaque = new Group(true);
    private final Group translucent = new Group(false);
    private boolean isEmpty = true;

    private Group getGroup(int color) {
        if (ARGB.alpha(color) < 255) {
            return this.translucent;
        }
        return this.opaque;
    }

    @Override
    public void addPoint(Vec3 pos, int color, float size) {
        this.getGroup((int)color).points.add(new Point(pos, color, size));
        this.isEmpty = false;
    }

    @Override
    public void addLine(Vec3 start, Vec3 end, int color, float width) {
        this.getGroup((int)color).lines.add(new Line(start, end, color, width));
        this.isEmpty = false;
    }

    @Override
    public void addTriangleFan(Vec3[] points, int color) {
        this.getGroup((int)color).triangleFans.add(new TriangleFan(points, color));
        this.isEmpty = false;
    }

    @Override
    public void addQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        this.getGroup((int)color).quads.add(new Quad(a, b, c, d, color));
        this.isEmpty = false;
    }

    @Override
    public void addText(Vec3 pos, String text, TextGizmo.Style style) {
        this.getGroup((int)style.color()).texts.add(new Text(pos, text, style));
        this.isEmpty = false;
    }

    public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, boolean onTop) {
        if (this.isEmpty) {
            return;
        }
        submitNodeCollector.submitGizmoPrimitives(this.opaque, cameraRenderState, onTop);
        submitNodeCollector.submitGizmoPrimitives(this.translucent, cameraRenderState, onTop);
    }

    @Environment(value=EnvType.CLIENT)
    public record Group(boolean opaque, List<Line> lines, List<Quad> quads, List<TriangleFan> triangleFans, List<Text> texts, List<Point> points) {
        private Group(boolean opaque) {
            this(opaque, new ArrayList<Line>(), new ArrayList<Quad>(), new ArrayList<TriangleFan>(), new ArrayList<Text>(), new ArrayList<Point>());
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Point(Vec3 pos, int color, float size) {
    }

    @Environment(value=EnvType.CLIENT)
    public record Line(Vec3 start, Vec3 end, int color, float width) {
    }

    @Environment(value=EnvType.CLIENT)
    public record TriangleFan(Vec3[] points, int color) {
    }

    @Environment(value=EnvType.CLIENT)
    public record Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
    }

    @Environment(value=EnvType.CLIENT)
    public record Text(Vec3 pos, String text, TextGizmo.Style style) {
    }
}

