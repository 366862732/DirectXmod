/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class KeyframeAnimation {
    private final AnimationDefinition definition;
    private final List<Entry> entries;

    private KeyframeAnimation(AnimationDefinition definition, List<Entry> entries) {
        this.definition = definition;
        this.entries = entries;
    }

    static KeyframeAnimation bake(ModelPart root, AnimationDefinition definition) {
        ArrayList<Entry> entries = new ArrayList<Entry>();
        Function<String, @Nullable ModelPart> partLookup = root.createPartLookup();
        for (Map.Entry<String, List<AnimationChannel>> entry : definition.boneAnimations().entrySet()) {
            String partName = entry.getKey();
            List<AnimationChannel> channels = entry.getValue();
            ModelPart part = partLookup.apply(partName);
            if (part == null) {
                throw new IllegalArgumentException("Cannot animate " + partName + ", which does not exist in model");
            }
            for (AnimationChannel channel : channels) {
                entries.add(new Entry(part, channel.target(), channel.keyframes()));
            }
        }
        return new KeyframeAnimation(definition, List.copyOf(entries));
    }

    public void applyStatic() {
        this.apply(0L, 1.0f);
    }

    public void applyWalk(float animationPos, float animationSpeed, float speedFactor, float scaleFactor) {
        long time = (long)(animationPos * 50.0f * speedFactor);
        float scale = Math.min(animationSpeed * scaleFactor, 1.0f);
        this.apply(time, scale);
    }

    public void apply(AnimationState animationState, float currentTime) {
        this.apply(animationState, currentTime, 1.0f);
    }

    public void apply(AnimationState animationState, float currentTime, float speedFactor) {
        animationState.ifStarted(state -> this.apply((long)((float)state.getTimeInMillis(currentTime) * speedFactor), 1.0f));
    }

    public void apply(long millisSinceStart, float targetScale) {
        float secondsSinceStart = this.getElapsedSeconds(millisSinceStart);
        Vector3f scratchVector = new Vector3f();
        for (Entry entry : this.entries) {
            entry.apply(secondsSinceStart, targetScale, scratchVector);
        }
    }

    private float getElapsedSeconds(long millisSinceStart) {
        float secondsSinceStart = (float)millisSinceStart / 1000.0f;
        return this.definition.looping() ? secondsSinceStart % this.definition.lengthInSeconds() : secondsSinceStart;
    }

    @Environment(value=EnvType.CLIENT)
    private record Entry(ModelPart part, AnimationChannel.Target target, Keyframe[] keyframes) {
        public void apply(float secondsSinceStart, float targetScale, Vector3f scratchVector) {
            int prev = Math.max(0, Mth.binarySearch(0, this.keyframes.length, i -> secondsSinceStart <= this.keyframes[i].timestamp()) - 1);
            int next = Math.min(this.keyframes.length - 1, prev + 1);
            Keyframe previousFrame = this.keyframes[prev];
            Keyframe nextFrame = this.keyframes[next];
            float keyframeTimeDelta = secondsSinceStart - previousFrame.timestamp();
            float lerpAlpha = next != prev ? Mth.clamp(keyframeTimeDelta / (nextFrame.timestamp() - previousFrame.timestamp()), 0.0f, 1.0f) : 0.0f;
            nextFrame.interpolation().apply(scratchVector, lerpAlpha, this.keyframes, prev, next, targetScale);
            this.target.apply(this.part, scratchVector);
        }
    }
}

