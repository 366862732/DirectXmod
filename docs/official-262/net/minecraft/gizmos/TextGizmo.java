/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gizmos;

import java.util.OptionalDouble;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public record TextGizmo(Vec3 pos, String text, Style style) implements Gizmo
{
    @Override
    public void emit(GizmoPrimitives primitives, float alphaMultiplier) {
        Style newStyle = alphaMultiplier < 1.0f ? new Style(ARGB.multiplyAlpha(this.style.color, alphaMultiplier), this.style.scale, this.style.adjustLeft) : this.style;
        primitives.addText(this.pos, this.text, newStyle);
    }

    public record Style(int color, float scale, OptionalDouble adjustLeft) {
        public static final float DEFAULT_SCALE = 0.32f;

        public static Style whiteAndCentered() {
            return new Style(-1, 0.32f, OptionalDouble.empty());
        }

        public static Style forColorAndCentered(int argb) {
            return new Style(argb, 0.32f, OptionalDouble.empty());
        }

        public static Style forColor(int argb) {
            return new Style(argb, 0.32f, OptionalDouble.of(0.0));
        }

        public Style withScale(float scale) {
            return new Style(this.color, scale, this.adjustLeft);
        }

        public Style withLeftAlignment(float adjustLeft) {
            return new Style(this.color, this.scale, OptionalDouble.of(adjustLeft));
        }
    }
}

