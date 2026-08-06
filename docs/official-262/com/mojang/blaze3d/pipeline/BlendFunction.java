/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.pipeline.BlendEquation;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.BlendOp;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record BlendFunction(BlendEquation color, BlendEquation alpha) {
    public static final BlendFunction LIGHTNING = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE);
    public static final BlendFunction GLINT = new BlendFunction(BlendFactor.SRC_COLOR, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE);
    public static final BlendFunction OVERLAY = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO);
    public static final BlendFunction TRANSLUCENT = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA);
    public static final BlendFunction TRANSLUCENT_PREMULTIPLIED_ALPHA = new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA);
    public static final BlendFunction ADDITIVE = new BlendFunction(BlendFactor.ONE, BlendFactor.ONE);
    public static final BlendFunction ENTITY_OUTLINE_BLIT = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ZERO, BlendFactor.ONE);
    public static final BlendFunction INVERT = new BlendFunction(BlendFactor.ONE_MINUS_DST_COLOR, BlendFactor.ONE_MINUS_SRC_COLOR, BlendFactor.ONE, BlendFactor.ZERO);

    public BlendFunction(BlendFactor srcColorFactor, BlendFactor dstColorFactor, BlendOp colorOp, BlendFactor srcAlphaFactor, BlendFactor dstAlphaFactor, BlendOp alphaOp) {
        this(new BlendEquation(srcColorFactor, dstColorFactor, colorOp), new BlendEquation(srcAlphaFactor, dstAlphaFactor, alphaOp));
    }

    public BlendFunction(BlendFactor srcColorFactor, BlendFactor dstColorFactor, BlendFactor srcAlphaFactor, BlendFactor dstAlphaFactor) {
        this(srcColorFactor, dstColorFactor, BlendOp.ADD, srcAlphaFactor, dstAlphaFactor, BlendOp.ADD);
    }

    public BlendFunction(BlendEquation equation) {
        this(equation, equation);
    }

    public BlendFunction(BlendFactor srcFactor, BlendFactor dstFactor, BlendOp op) {
        this(new BlendEquation(srcFactor, dstFactor, op));
    }

    public BlendFunction(BlendFactor srcFactor, BlendFactor dstFactor) {
        this(new BlendEquation(srcFactor, dstFactor, BlendOp.ADD));
    }
}

