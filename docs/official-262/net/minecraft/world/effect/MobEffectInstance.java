/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ComparisonChain
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 *  it.unimi.dsi.fastutil.ints.Int2IntFunction
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.effect;

import com.google.common.collect.ComparisonChain;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MobEffectInstance
implements Comparable<MobEffectInstance> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int INFINITE_DURATION = -1;
    public static final int MIN_AMPLIFIER = 0;
    public static final int MAX_AMPLIFIER = 255;
    public static final Codec<MobEffectInstance> CODEC = RecordCodecBuilder.create(i -> i.group((App)MobEffect.CODEC.fieldOf("id").forGetter(MobEffectInstance::getEffect), (App)Details.MAP_CODEC.forGetter(MobEffectInstance::asDetails)).apply((Applicative)i, MobEffectInstance::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MobEffectInstance> STREAM_CODEC = StreamCodec.composite(MobEffect.STREAM_CODEC, MobEffectInstance::getEffect, Details.STREAM_CODEC, MobEffectInstance::asDetails, MobEffectInstance::new);
    private final Holder<MobEffect> effect;
    private int duration;
    private int amplifier;
    private boolean ambient;
    private boolean visible;
    private boolean showIcon;
    private @Nullable MobEffectInstance hiddenEffect;
    private final BlendState blendState = new BlendState();

    public MobEffectInstance(Holder<MobEffect> effect) {
        this(effect, 0, 0);
    }

    public MobEffectInstance(Holder<MobEffect> effect, int duration) {
        this(effect, duration, 0);
    }

    public MobEffectInstance(Holder<MobEffect> effect, int duration, int amplifier) {
        this(effect, duration, amplifier, false, true);
    }

    public MobEffectInstance(Holder<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible) {
        this(effect, duration, amplifier, ambient, visible, visible);
    }

    public MobEffectInstance(Holder<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        this(effect, duration, amplifier, ambient, visible, showIcon, null);
    }

    public MobEffectInstance(Holder<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon, @Nullable MobEffectInstance hiddenEffect) {
        this.effect = effect;
        this.duration = duration;
        this.amplifier = Mth.clamp(amplifier, 0, 255);
        this.ambient = ambient;
        this.visible = visible;
        this.showIcon = showIcon;
        this.hiddenEffect = hiddenEffect;
    }

    public MobEffectInstance(MobEffectInstance copy) {
        this.effect = copy.effect;
        this.setDetailsFrom(copy);
    }

    private MobEffectInstance(Holder<MobEffect> effect, Details details) {
        this(effect, details.duration(), details.amplifier(), details.ambient(), details.showParticles(), details.showIcon(), details.hiddenEffect().map(hidden -> new MobEffectInstance(effect, (Details)hidden)).orElse(null));
    }

    private Details asDetails() {
        return new Details(this.getAmplifier(), this.getDuration(), this.isAmbient(), this.isVisible(), this.showIcon(), Optional.ofNullable(this.hiddenEffect).map(MobEffectInstance::asDetails));
    }

    public float getBlendFactor(LivingEntity livingEntity, float partialTickTime) {
        return this.blendState.getFactor(livingEntity, partialTickTime);
    }

    public ParticleOptions getParticleOptions() {
        return this.effect.value().createParticleOptions(this);
    }

    private void setDetailsFrom(MobEffectInstance copy) {
        this.duration = copy.duration;
        this.amplifier = copy.amplifier;
        this.ambient = copy.ambient;
        this.visible = copy.visible;
        this.showIcon = copy.showIcon;
    }

    public boolean update(MobEffectInstance takeOver) {
        if (!this.effect.equals(takeOver.effect)) {
            LOGGER.warn("This method should only be called for matching effects!");
        }
        boolean changed = false;
        if (takeOver.amplifier > this.amplifier) {
            if (takeOver.isShorterDurationThan(this)) {
                MobEffectInstance prevHiddenEffect = this.hiddenEffect;
                this.hiddenEffect = new MobEffectInstance(this);
                this.hiddenEffect.hiddenEffect = prevHiddenEffect;
            }
            this.amplifier = takeOver.amplifier;
            this.duration = takeOver.duration;
            changed = true;
        } else if (this.isShorterDurationThan(takeOver)) {
            if (takeOver.amplifier == this.amplifier) {
                this.duration = takeOver.duration;
                changed = true;
            } else if (this.hiddenEffect == null) {
                this.hiddenEffect = new MobEffectInstance(takeOver);
            } else {
                this.hiddenEffect.update(takeOver);
            }
        }
        if (!takeOver.ambient && this.ambient || changed) {
            this.ambient = takeOver.ambient;
            changed = true;
        }
        if (takeOver.visible != this.visible) {
            this.visible = takeOver.visible;
            changed = true;
        }
        if (takeOver.showIcon != this.showIcon) {
            this.showIcon = takeOver.showIcon;
            changed = true;
        }
        return changed;
    }

    private boolean isShorterDurationThan(MobEffectInstance other) {
        return !this.isInfiniteDuration() && (this.duration < other.duration || other.isInfiniteDuration());
    }

    public boolean isInfiniteDuration() {
        return this.duration == -1;
    }

    public boolean endsWithin(int ticks) {
        return !this.isInfiniteDuration() && this.duration <= ticks;
    }

    public MobEffectInstance withScaledDuration(float scale) {
        MobEffectInstance copy = new MobEffectInstance(this);
        copy.duration = copy.mapDuration(duration -> Math.max(Mth.floor((float)duration * scale), 1));
        return copy;
    }

    public int mapDuration(Int2IntFunction mapper) {
        if (this.isInfiniteDuration() || this.duration == 0) {
            return this.duration;
        }
        return mapper.applyAsInt(this.duration);
    }

    public Holder<MobEffect> getEffect() {
        return this.effect;
    }

    public int getDuration() {
        return this.duration;
    }

    public int getAmplifier() {
        return this.amplifier;
    }

    public boolean isAmbient() {
        return this.ambient;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean showIcon() {
        return this.showIcon;
    }

    public boolean tickServer(ServerLevel serverLevel, LivingEntity target, Runnable onEffectUpdate) {
        int tickCount;
        if (!this.hasRemainingDuration()) {
            return false;
        }
        int n = tickCount = this.isInfiniteDuration() ? target.tickCount : this.duration;
        if (this.effect.value().shouldApplyEffectTickThisTick(tickCount, this.amplifier) && !this.effect.value().applyEffectTick(serverLevel, target, this.amplifier)) {
            return false;
        }
        this.tickDownDuration();
        if (this.downgradeToHiddenEffect()) {
            onEffectUpdate.run();
        }
        return this.hasRemainingDuration();
    }

    public void tickClient() {
        if (this.hasRemainingDuration()) {
            this.tickDownDuration();
            this.downgradeToHiddenEffect();
        }
        this.blendState.tick(this);
    }

    private boolean hasRemainingDuration() {
        return this.isInfiniteDuration() || this.duration > 0;
    }

    private void tickDownDuration() {
        if (this.hiddenEffect != null) {
            this.hiddenEffect.tickDownDuration();
        }
        this.duration = this.mapDuration(d -> d - 1);
    }

    private boolean downgradeToHiddenEffect() {
        if (this.duration == 0 && this.hiddenEffect != null) {
            this.setDetailsFrom(this.hiddenEffect);
            this.hiddenEffect = this.hiddenEffect.hiddenEffect;
            return true;
        }
        return false;
    }

    public void onEffectStarted(LivingEntity mob) {
        this.effect.value().onEffectStarted(mob, this.amplifier);
    }

    public void onMobRemoved(ServerLevel level, LivingEntity mob, Entity.RemovalReason reason) {
        this.effect.value().onMobRemoved(level, mob, this.amplifier, reason);
    }

    public void onMobHurt(ServerLevel level, LivingEntity mob, DamageSource source, float damage) {
        this.effect.value().onMobHurt(level, mob, this.amplifier, source, damage);
    }

    public String getDescriptionId() {
        return this.effect.value().getDescriptionId();
    }

    public String toString() {
        String result = this.amplifier > 0 ? this.getDescriptionId() + " x " + (this.amplifier + 1) + ", Duration: " + this.describeDuration() : this.getDescriptionId() + ", Duration: " + this.describeDuration();
        if (!this.visible) {
            result = result + ", Particles: false";
        }
        if (!this.showIcon) {
            result = result + ", Show Icon: false";
        }
        return result;
    }

    private String describeDuration() {
        if (this.isInfiniteDuration()) {
            return "infinite";
        }
        return Integer.toString(this.duration);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MobEffectInstance) {
            MobEffectInstance that = (MobEffectInstance)o;
            return this.duration == that.duration && this.amplifier == that.amplifier && this.ambient == that.ambient && this.visible == that.visible && this.showIcon == that.showIcon && this.effect.equals(that.effect);
        }
        return false;
    }

    public int hashCode() {
        int result = this.effect.hashCode();
        result = 31 * result + this.duration;
        result = 31 * result + this.amplifier;
        result = 31 * result + (this.ambient ? 1 : 0);
        result = 31 * result + (this.visible ? 1 : 0);
        result = 31 * result + (this.showIcon ? 1 : 0);
        return result;
    }

    @Override
    public int compareTo(MobEffectInstance o) {
        int updateCutOff = 32147;
        if (this.getDuration() > 32147 && o.getDuration() > 32147 || this.isAmbient() && o.isAmbient()) {
            return ComparisonChain.start().compare(Boolean.valueOf(this.isAmbient()), Boolean.valueOf(o.isAmbient())).compare(this.getEffect().value().getColor(), o.getEffect().value().getColor()).result();
        }
        return ComparisonChain.start().compareFalseFirst(this.isAmbient(), o.isAmbient()).compareFalseFirst(this.isInfiniteDuration(), o.isInfiniteDuration()).compare(this.getDuration(), o.getDuration()).compare(this.getEffect().value().getColor(), o.getEffect().value().getColor()).result();
    }

    public void onEffectAdded(LivingEntity livingEntity) {
        this.effect.value().onEffectAdded(livingEntity, this.amplifier);
    }

    public boolean is(Holder<MobEffect> effect) {
        return this.effect.equals(effect);
    }

    public void copyBlendState(MobEffectInstance instance) {
        this.blendState.copyFrom(instance.blendState);
    }

    public void skipBlending() {
        this.blendState.setImmediate(this);
    }

    private static class BlendState {
        private float factor;
        private float factorPreviousFrame;

        private BlendState() {
        }

        public void setImmediate(MobEffectInstance instance) {
            this.factorPreviousFrame = this.factor = BlendState.hasEffect(instance) ? 1.0f : 0.0f;
        }

        public void copyFrom(BlendState other) {
            this.factor = other.factor;
            this.factorPreviousFrame = other.factorPreviousFrame;
        }

        public void tick(MobEffectInstance instance) {
            int blendDuration;
            float target;
            this.factorPreviousFrame = this.factor;
            boolean hasEffect = BlendState.hasEffect(instance);
            float f = target = hasEffect ? 1.0f : 0.0f;
            if (this.factor == target) {
                return;
            }
            MobEffect effect = instance.getEffect().value();
            int n = blendDuration = hasEffect ? effect.getBlendInDurationTicks() : effect.getBlendOutDurationTicks();
            if (blendDuration == 0) {
                this.factor = target;
            } else {
                float maxDeltaPerTick = 1.0f / (float)blendDuration;
                this.factor += Mth.clamp(target - this.factor, -maxDeltaPerTick, maxDeltaPerTick);
            }
        }

        private static boolean hasEffect(MobEffectInstance instance) {
            return !instance.endsWithin(instance.getEffect().value().getBlendOutAdvanceTicks());
        }

        public float getFactor(LivingEntity livingEntity, float partialTickTime) {
            if (livingEntity.isRemoved()) {
                this.factorPreviousFrame = this.factor;
            }
            return Mth.lerp(partialTickTime, this.factorPreviousFrame, this.factor);
        }
    }

    private record Details(int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon, Optional<Details> hiddenEffect) {
        public static final MapCodec<Details> MAP_CODEC = MapCodec.recursive((String)"MobEffectInstance.Details", codec -> RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("amplifier", (Object)0).forGetter(Details::amplifier), (App)Codec.INT.optionalFieldOf("duration", (Object)0).forGetter(Details::duration), (App)Codec.BOOL.optionalFieldOf("ambient", (Object)false).forGetter(Details::ambient), (App)Codec.BOOL.optionalFieldOf("show_particles", (Object)true).forGetter(Details::showParticles), (App)Codec.BOOL.optionalFieldOf("show_icon").forGetter(d -> Optional.of(d.showIcon())), (App)codec.optionalFieldOf("hidden_effect").forGetter(Details::hiddenEffect)).apply((Applicative)i, Details::create)));
        public static final StreamCodec<ByteBuf, Details> STREAM_CODEC = StreamCodec.recursive(subCodec -> StreamCodec.composite(ByteBufCodecs.VAR_INT, Details::amplifier, ByteBufCodecs.VAR_INT, Details::duration, ByteBufCodecs.BOOL, Details::ambient, ByteBufCodecs.BOOL, Details::showParticles, ByteBufCodecs.BOOL, Details::showIcon, subCodec.apply(ByteBufCodecs::optional), Details::hiddenEffect, Details::new));

        private static Details create(int amplifier, int duration, boolean ambient, boolean showParticles, Optional<Boolean> showIcon, Optional<Details> hiddenEffect) {
            return new Details(amplifier, duration, ambient, showParticles, showIcon.orElse(showParticles), hiddenEffect);
        }
    }
}

