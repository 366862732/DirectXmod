/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.util;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.Ease;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

public interface EasingType {
    public static final ExtraCodecs.LateBoundIdMapper<String, EasingType> SIMPLE_REGISTRY = new ExtraCodecs.LateBoundIdMapper();
    public static final Codec<EasingType> CODEC = Codec.either(SIMPLE_REGISTRY.codec((Codec<String>)Codec.STRING), CubicBezier.CODEC).xmap(Either::unwrap, easing -> {
        Either either;
        if (easing instanceof CubicBezier) {
            CubicBezier bezier = (CubicBezier)easing;
            either = Either.right((Object)bezier);
        } else {
            either = Either.left((Object)easing);
        }
        return either;
    });
    public static final EasingType CONSTANT = EasingType.registerSimple("constant", x -> 0.0f);
    public static final EasingType LINEAR = EasingType.registerSimple("linear", x -> x);
    public static final EasingType IN_BACK = EasingType.registerSimple("in_back", Ease::inBack);
    public static final EasingType IN_BOUNCE = EasingType.registerSimple("in_bounce", Ease::inBounce);
    public static final EasingType IN_CIRC = EasingType.registerSimple("in_circ", Ease::inCirc);
    public static final EasingType IN_CUBIC = EasingType.registerSimple("in_cubic", Ease::inCubic);
    public static final EasingType IN_ELASTIC = EasingType.registerSimple("in_elastic", Ease::inElastic);
    public static final EasingType IN_EXPO = EasingType.registerSimple("in_expo", Ease::inExpo);
    public static final EasingType IN_QUAD = EasingType.registerSimple("in_quad", Ease::inQuad);
    public static final EasingType IN_QUART = EasingType.registerSimple("in_quart", Ease::inQuart);
    public static final EasingType IN_QUINT = EasingType.registerSimple("in_quint", Ease::inQuint);
    public static final EasingType IN_SINE = EasingType.registerSimple("in_sine", Ease::inSine);
    public static final EasingType IN_OUT_BACK = EasingType.registerSimple("in_out_back", Ease::inOutBack);
    public static final EasingType IN_OUT_BOUNCE = EasingType.registerSimple("in_out_bounce", Ease::inOutBounce);
    public static final EasingType IN_OUT_CIRC = EasingType.registerSimple("in_out_circ", Ease::inOutCirc);
    public static final EasingType IN_OUT_CUBIC = EasingType.registerSimple("in_out_cubic", Ease::inOutCubic);
    public static final EasingType IN_OUT_ELASTIC = EasingType.registerSimple("in_out_elastic", Ease::inOutElastic);
    public static final EasingType IN_OUT_EXPO = EasingType.registerSimple("in_out_expo", Ease::inOutExpo);
    public static final EasingType IN_OUT_QUAD = EasingType.registerSimple("in_out_quad", Ease::inOutQuad);
    public static final EasingType IN_OUT_QUART = EasingType.registerSimple("in_out_quart", Ease::inOutQuart);
    public static final EasingType IN_OUT_QUINT = EasingType.registerSimple("in_out_quint", Ease::inOutQuint);
    public static final EasingType IN_OUT_SINE = EasingType.registerSimple("in_out_sine", Ease::inOutSine);
    public static final EasingType OUT_BACK = EasingType.registerSimple("out_back", Ease::outBack);
    public static final EasingType OUT_BOUNCE = EasingType.registerSimple("out_bounce", Ease::outBounce);
    public static final EasingType OUT_CIRC = EasingType.registerSimple("out_circ", Ease::outCirc);
    public static final EasingType OUT_CUBIC = EasingType.registerSimple("out_cubic", Ease::outCubic);
    public static final EasingType OUT_ELASTIC = EasingType.registerSimple("out_elastic", Ease::outElastic);
    public static final EasingType OUT_EXPO = EasingType.registerSimple("out_expo", Ease::outExpo);
    public static final EasingType OUT_QUAD = EasingType.registerSimple("out_quad", Ease::outQuad);
    public static final EasingType OUT_QUART = EasingType.registerSimple("out_quart", Ease::outQuart);
    public static final EasingType OUT_QUINT = EasingType.registerSimple("out_quint", Ease::outQuint);
    public static final EasingType OUT_SINE = EasingType.registerSimple("out_sine", Ease::outSine);

    public static EasingType registerSimple(String id, EasingType easing) {
        SIMPLE_REGISTRY.put(id, easing);
        return easing;
    }

    public static EasingType cubicBezier(float x1, float y1, float x2, float y2) {
        return new CubicBezier(new CubicBezierControls(x1, y1, x2, y2));
    }

    public static EasingType symmetricCubicBezier(float x1, float y1) {
        return EasingType.cubicBezier(x1, y1, 1.0f - x1, 1.0f - y1);
    }

    public float apply(float var1);

    public static final class CubicBezier
    implements EasingType {
        public static final Codec<CubicBezier> CODEC = RecordCodecBuilder.create(i -> i.group((App)CubicBezierControls.CODEC.fieldOf("cubic_bezier").forGetter(b -> b.controls)).apply((Applicative)i, CubicBezier::new));
        private static final int NEWTON_RAPHSON_ITERATIONS = 4;
        private static final float MAX_STEP = 0.25f;
        private final CubicBezierControls controls;
        private final CubicCurve xCurve;
        private final CubicCurve yCurve;

        public CubicBezier(CubicBezierControls controls) {
            this.controls = controls;
            this.xCurve = CubicBezier.curveFromControls(controls.x1, controls.x2);
            this.yCurve = CubicBezier.curveFromControls(controls.y1, controls.y2);
        }

        private static CubicCurve curveFromControls(float v1, float v2) {
            return new CubicCurve(3.0f * v1 - 3.0f * v2 + 1.0f, -6.0f * v1 + 3.0f * v2, 3.0f * v1);
        }

        @Override
        public float apply(float x) {
            return this.yCurve.sample(this.solveT(x));
        }

        private float solveT(float x) {
            float t = x;
            for (int i = 0; i < 4; ++i) {
                float error = this.xCurve.sample(t) - x;
                if (Math.abs(error) < 1.0E-5f) {
                    return t;
                }
                float gradient = this.xCurve.sampleGradient(t);
                if (gradient < 1.0E-5f) break;
                t -= Mth.clamp(error / gradient, -0.25f, 0.25f);
            }
            return this.solveTBisect(x, t);
        }

        private float solveTBisect(float x, float initialT) {
            float t0 = 0.0f;
            float t1 = 1.0f;
            float t = initialT;
            while (t0 < t1) {
                float error = this.xCurve.sample(t) - x;
                if (Math.abs(error) < 1.0E-5f) {
                    return t;
                }
                if (error < 0.0f) {
                    t0 = t;
                } else {
                    t1 = t;
                }
                t = (t1 + t0) / 2.0f;
            }
            return t;
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof CubicBezier)) return false;
            CubicBezier bezier = (CubicBezier)obj;
            if (!this.controls.equals(bezier.controls)) return false;
            return true;
        }

        public int hashCode() {
            return this.controls.hashCode();
        }

        public String toString() {
            return "CubicBezier(" + this.controls.x1 + ", " + this.controls.y1 + ", " + this.controls.x2 + ", " + this.controls.y2 + ")";
        }

        private record CubicCurve(float a, float b, float c) {
            public float sample(float t) {
                return ((this.a * t + this.b) * t + this.c) * t;
            }

            public float sampleGradient(float t) {
                return (3.0f * this.a * t + 2.0f * this.b) * t + this.c;
            }
        }
    }

    public record CubicBezierControls(float x1, float y1, float x2, float y2) {
        public static final Codec<CubicBezierControls> CODEC = Codec.FLOAT.listOf(4, 4).xmap(floats -> new CubicBezierControls(((Float)floats.get(0)).floatValue(), ((Float)floats.get(1)).floatValue(), ((Float)floats.get(2)).floatValue(), ((Float)floats.get(3)).floatValue()), controls -> List.of(Float.valueOf(controls.x1), Float.valueOf(controls.y1), Float.valueOf(controls.x2), Float.valueOf(controls.y2))).validate(CubicBezierControls::validate);

        private DataResult<CubicBezierControls> validate() {
            if (this.x1 < 0.0f || this.x1 > 1.0f) {
                return DataResult.error(() -> "x1 must be in range [0; 1]");
            }
            if (this.x2 < 0.0f || this.x2 > 1.0f) {
                return DataResult.error(() -> "x2 must be in range [0; 1]");
            }
            return DataResult.success((Object)this);
        }
    }
}

