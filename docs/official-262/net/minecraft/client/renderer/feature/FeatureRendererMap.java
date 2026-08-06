/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.feature;

import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class FeatureRendererMap
implements AutoCloseable {
    private @Nullable FeatureRenderer<?>[] renderers = new FeatureRenderer[0];

    public <Submit extends SubmitNode> void put(FeatureRendererType<Submit> type, FeatureRenderer<Submit> renderer) {
        if (this.renderers.length <= type.id()) {
            this.renderers = Arrays.copyOf(this.renderers, Mth.roundToward(type.id() + 1, 16));
        }
        this.renderers[type.id()] = renderer;
    }

    public <Submit extends SubmitNode> @Nullable FeatureRenderer<Submit> get(FeatureRendererType<Submit> type) {
        if (type.id() >= this.renderers.length) {
            return null;
        }
        return this.renderers[type.id()];
    }

    public <Submit extends SubmitNode> FeatureRenderer<Submit> getOrThrow(FeatureRendererType<Submit> type) {
        FeatureRenderer<Submit> renderer = this.get(type);
        if (renderer == null) {
            throw new IllegalArgumentException("No FeatureRenderer for type " + String.valueOf(type));
        }
        return renderer;
    }

    public Iterable<FeatureRenderer<?>> values() {
        return Iterables.filter(Arrays.asList(this.renderers), Objects::nonNull);
    }

    @Override
    public void close() {
        for (FeatureRenderer<?> renderer : this.renderers) {
            if (renderer == null) continue;
            renderer.close();
        }
    }
}

