/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.ArrayUtils
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.feature.phase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.BatchableSubmit;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SimpleFeatureRenderPhase
implements FeatureRenderPhase<SubmitNode> {
    private @Nullable FeatureSubmits<?>[] submitsByFeature = new FeatureSubmits[0];

    @Override
    public void submit(SubmitNode submit) {
        FeatureSubmits<Object> submits;
        FeatureRendererType<? extends SubmitNode> type = submit.featureType();
        if (this.submitsByFeature.length <= type.id()) {
            this.submitsByFeature = Arrays.copyOf(this.submitsByFeature, Mth.roundToward(type.id() + 1, 16));
        }
        if ((submits = this.submitsByFeature[type.id()]) == null) {
            submits = new FeatureSubmits<SubmitNode>(type);
            this.submitsByFeature[type.id()] = submits;
        }
        submits.addUnchecked(submit);
    }

    @Override
    public void sortInto(FeatureRenderPhase.Output output) {
        for (FeatureSubmits<?> submits : SimpleFeatureRenderPhase.maybeShuffle(this.submitsByFeature)) {
            if (submits == null) continue;
            SimpleFeatureRenderPhase.sortFeatureInto(output, submits);
        }
        this.clear();
    }

    private static <Submit extends SubmitNode> void sortFeatureInto(FeatureRenderPhase.Output output, FeatureSubmits<Submit> submits) {
        output.acceptFeatureGroup(submits.featureType, SimpleFeatureRenderPhase.maybeShuffle(submits.unbatched), false);
        for (List batch : SimpleFeatureRenderPhase.maybeShuffle(submits.batches.values())) {
            output.acceptFeatureGroup(submits.featureType, SimpleFeatureRenderPhase.maybeShuffle(batch), false);
        }
    }

    private static <V> Collection<V> maybeShuffle(Collection<V> collection) {
        if (SharedConstants.DEBUG_SHUFFLE_MODELS) {
            ArrayList<V> shuffled = new ArrayList<V>(collection);
            Collections.shuffle(shuffled);
            return shuffled;
        }
        return collection;
    }

    private static <V> V[] maybeShuffle(V[] array) {
        if (SharedConstants.DEBUG_SHUFFLE_MODELS) {
            Object[] shuffled = Arrays.copyOf(array, array.length);
            ArrayUtils.shuffle((Object[])shuffled);
            return shuffled;
        }
        return array;
    }

    public void clear() {
        for (int i = 0; i < this.submitsByFeature.length; ++i) {
            FeatureSubmits<?> submits = this.submitsByFeature[i];
            if (submits == null) continue;
            if (submits.isEmpty()) {
                this.submitsByFeature[i] = null;
                continue;
            }
            submits.clear();
        }
    }

    @Override
    public boolean isEmpty() {
        for (FeatureSubmits<?> submits : this.submitsByFeature) {
            if (submits == null || submits.isEmpty()) continue;
            return false;
        }
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    private static class FeatureSubmits<Submit extends SubmitNode> {
        private final FeatureRendererType<Submit> featureType;
        private final List<Submit> unbatched = new ArrayList<Submit>();
        private final Map<Object, List<Submit>> batches = new HashMap<Object, List<Submit>>();

        private FeatureSubmits(FeatureRendererType<Submit> featureType) {
            this.featureType = featureType;
        }

        public void addUnchecked(SubmitNode submit) {
            this.add(submit);
        }

        public void add(Submit submit) {
            Object key = FeatureSubmits.batchKey(submit);
            if (key == null) {
                this.unbatched.add(submit);
            } else {
                this.batches.computeIfAbsent(key, object -> new ArrayList()).add(submit);
            }
        }

        private static @Nullable Object batchKey(SubmitNode submit) {
            if (submit instanceof BatchableSubmit) {
                BatchableSubmit batchable = (BatchableSubmit)submit;
                return batchable.batchKey();
            }
            return null;
        }

        public boolean isEmpty() {
            if (!this.unbatched.isEmpty()) {
                return false;
            }
            for (List<Submit> submits : this.batches.values()) {
                if (submits.isEmpty()) continue;
                return false;
            }
            return true;
        }

        public void clear() {
            this.unbatched.clear();
            this.batches.values().removeIf(submits -> {
                if (submits.isEmpty()) {
                    return true;
                }
                submits.clear();
                return false;
            });
        }
    }
}

