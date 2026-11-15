package com.xgdt.dx11.perf.immediate.sign;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Marker pose stack used for the nested pass that rasterises text into the
 * sign atlas.  The sign renderer mixin skips its own buffering hook when it
 * sees this type, so the nested {@code submitSignText} call takes the vanilla
 * glyph path instead of recursing.
 */
public final class NoTextTransformPoseStack extends PoseStack {
}
