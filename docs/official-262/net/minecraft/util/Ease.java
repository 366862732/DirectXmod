/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import net.minecraft.util.Mth;

public class Ease {
    public static float inBack(float x) {
        float c1 = 1.70158f;
        float c3 = 2.70158f;
        return Mth.square(x) * (2.70158f * x - 1.70158f);
    }

    public static float inBounce(float x) {
        return 1.0f - Ease.outBounce(1.0f - x);
    }

    public static float inCubic(float x) {
        return Mth.cube(x);
    }

    public static float inElastic(float x) {
        if (x == 0.0f) {
            return 0.0f;
        }
        if (x == 1.0f) {
            return 1.0f;
        }
        float c4 = 2.0943952f;
        return (float)(-Math.pow(2.0, 10.0 * (double)x - 10.0) * Math.sin(((double)x * 10.0 - 10.75) * 2.094395160675049));
    }

    public static float inExpo(float x) {
        return x == 0.0f ? 0.0f : (float)Math.pow(2.0, 10.0 * (double)x - 10.0);
    }

    public static float inQuart(float x) {
        return Mth.square(Mth.square(x));
    }

    public static float inQuint(float x) {
        return Mth.square(Mth.square(x)) * x;
    }

    public static float inSine(float x) {
        return 1.0f - Mth.cos(x * 1.5707964f);
    }

    public static float inOutBounce(float x) {
        if (x < 0.5f) {
            return (1.0f - Ease.outBounce(1.0f - 2.0f * x)) / 2.0f;
        }
        return (1.0f + Ease.outBounce(2.0f * x - 1.0f)) / 2.0f;
    }

    public static float inOutCirc(float x) {
        if (x < 0.5f) {
            return (float)((1.0 - Math.sqrt(1.0 - Math.pow(2.0 * (double)x, 2.0))) / 2.0);
        }
        return (float)((Math.sqrt(1.0 - Math.pow(-2.0 * (double)x + 2.0, 2.0)) + 1.0) / 2.0);
    }

    public static float inOutCubic(float x) {
        if (x < 0.5f) {
            return 4.0f * Mth.cube(x);
        }
        return (float)(1.0 - Math.pow(-2.0 * (double)x + 2.0, 3.0) / 2.0);
    }

    public static float inOutQuad(float x) {
        if (x < 0.5f) {
            return 2.0f * Mth.square(x);
        }
        return (float)(1.0 - Math.pow(-2.0 * (double)x + 2.0, 2.0) / 2.0);
    }

    public static float inOutQuart(float x) {
        if (x < 0.5f) {
            return 8.0f * Mth.square(Mth.square(x));
        }
        return (float)(1.0 - Math.pow(-2.0 * (double)x + 2.0, 4.0) / 2.0);
    }

    public static float inOutQuint(float x) {
        if ((double)x < 0.5) {
            return 16.0f * x * x * x * x * x;
        }
        return (float)(1.0 - Math.pow(-2.0 * (double)x + 2.0, 5.0) / 2.0);
    }

    public static float outBounce(float x) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if (x < 0.36363637f) {
            return 7.5625f * Mth.square(x);
        }
        if (x < 0.72727275f) {
            return 7.5625f * Mth.square(x - 0.54545456f) + 0.75f;
        }
        if ((double)x < 0.9090909090909091) {
            return 7.5625f * Mth.square(x - 0.8181818f) + 0.9375f;
        }
        return 7.5625f * Mth.square(x - 0.95454544f) + 0.984375f;
    }

    public static float outElastic(float x) {
        float c4 = 2.0943952f;
        if (x == 0.0f) {
            return 0.0f;
        }
        if (x == 1.0f) {
            return 1.0f;
        }
        return (float)(Math.pow(2.0, -10.0 * (double)x) * Math.sin(((double)x * 10.0 - 0.75) * 2.094395160675049) + 1.0);
    }

    public static float outExpo(float x) {
        if (x == 1.0f) {
            return 1.0f;
        }
        return 1.0f - (float)Math.pow(2.0, -10.0 * (double)x);
    }

    public static float outQuad(float x) {
        return 1.0f - Mth.square(1.0f - x);
    }

    public static float outQuint(float x) {
        return 1.0f - (float)Math.pow(1.0 - (double)x, 5.0);
    }

    public static float outSine(float x) {
        return Mth.sin(x * 1.5707964f);
    }

    public static float inOutSine(float x) {
        return -(Mth.cos((float)Math.PI * x) - 1.0f) / 2.0f;
    }

    public static float outBack(float x) {
        float c1 = 1.70158f;
        float c3 = 2.70158f;
        return 1.0f + 2.70158f * Mth.cube(x - 1.0f) + 1.70158f * Mth.square(x - 1.0f);
    }

    public static float outQuart(float x) {
        return 1.0f - Mth.square(Mth.square(1.0f - x));
    }

    public static float outCubic(float x) {
        return 1.0f - Mth.cube(1.0f - x);
    }

    public static float inOutExpo(float x) {
        if (x < 0.5f) {
            return x == 0.0f ? 0.0f : (float)(Math.pow(2.0, 20.0 * (double)x - 10.0) / 2.0);
        }
        return x == 1.0f ? 1.0f : (float)((2.0 - Math.pow(2.0, -20.0 * (double)x + 10.0)) / 2.0);
    }

    public static float inQuad(float x) {
        return x * x;
    }

    public static float outCirc(float x) {
        return (float)Math.sqrt(1.0f - Mth.square(x - 1.0f));
    }

    public static float inOutElastic(float x) {
        float c5 = 1.3962635f;
        if (x == 0.0f) {
            return 0.0f;
        }
        if (x == 1.0f) {
            return 1.0f;
        }
        double sin = Math.sin((20.0 * (double)x - 11.125) * 1.3962634801864624);
        if (x < 0.5f) {
            return (float)(-(Math.pow(2.0, 20.0 * (double)x - 10.0) * sin) / 2.0);
        }
        return (float)(Math.pow(2.0, -20.0 * (double)x + 10.0) * sin / 2.0 + 1.0);
    }

    public static float inCirc(float x) {
        return (float)(-Math.sqrt(1.0f - x * x)) + 1.0f;
    }

    public static float inOutBack(float x) {
        float c1 = 1.70158f;
        float c2 = 2.5949094f;
        if (x < 0.5f) {
            return 4.0f * x * x * (7.189819f * x - 2.5949094f) / 2.0f;
        }
        float dt = 2.0f * x - 2.0f;
        return (dt * dt * (3.5949094f * dt + 2.5949094f) + 2.0f) / 2.0f;
    }
}

