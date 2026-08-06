/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.component;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.PlaySoundConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public record Consumable(float consumeSeconds, ItemUseAnimation animation, Holder<SoundEvent> sound, boolean hasConsumeParticles, List<ConsumeEffect> onConsumeEffects) {
    public static final float DEFAULT_CONSUME_SECONDS = 1.6f;
    private static final int CONSUME_EFFECTS_INTERVAL = 4;
    private static final float CONSUME_EFFECTS_START_FRACTION = 0.21875f;
    public static final Codec<Consumable> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("consume_seconds", (Object)Float.valueOf(1.6f)).forGetter(Consumable::consumeSeconds), (App)ItemUseAnimation.CODEC.optionalFieldOf("animation", (Object)ItemUseAnimation.EAT).forGetter(Consumable::animation), (App)SoundEvent.CODEC.optionalFieldOf("sound", SoundEvents.GENERIC_EAT).forGetter(Consumable::sound), (App)Codec.BOOL.optionalFieldOf("has_consume_particles", (Object)true).forGetter(Consumable::hasConsumeParticles), (App)ConsumeEffect.CODEC.listOf().optionalFieldOf("on_consume_effects", List.of()).forGetter(Consumable::onConsumeEffects)).apply((Applicative)i, Consumable::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Consumable> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, Consumable::consumeSeconds, ItemUseAnimation.STREAM_CODEC, Consumable::animation, SoundEvent.STREAM_CODEC, Consumable::sound, ByteBufCodecs.BOOL, Consumable::hasConsumeParticles, ConsumeEffect.STREAM_CODEC.apply(ByteBufCodecs.list()), Consumable::onConsumeEffects, Consumable::new);

    public InteractionResult startConsuming(LivingEntity user, ItemStack stack, InteractionHand hand) {
        boolean consumesOverTime;
        if (!this.canConsume(user, stack)) {
            return InteractionResult.FAIL;
        }
        boolean bl = consumesOverTime = this.consumeTicks() > 0;
        if (consumesOverTime) {
            user.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        ItemStack result = this.onConsume(user.level(), user, stack);
        return InteractionResult.CONSUME.heldItemTransformedTo(result);
    }

    public ItemStack onConsume(Level level, LivingEntity user, ItemStack stack) {
        RandomSource random = user.getRandom();
        this.emitParticlesAndSounds(random, user, stack, 16);
        if (user instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)user;
            serverPlayer.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }
        stack.getAllOfType(ConsumableListener.class).forEach(component -> component.onConsume(level, user, stack, this));
        if (!level.isClientSide()) {
            this.onConsumeEffects.forEach(action -> action.apply(level, stack, user));
        }
        user.gameEvent(this.animation == ItemUseAnimation.DRINK ? GameEvent.DRINK : GameEvent.EAT);
        stack.consume(1, user);
        return stack;
    }

    public boolean canConsume(LivingEntity user, ItemStack stack) {
        FoodProperties foodProperties = stack.get(DataComponents.FOOD);
        if (foodProperties != null && user instanceof Player) {
            Player player = (Player)user;
            return player.canEat(foodProperties.canAlwaysEat());
        }
        return true;
    }

    public int consumeTicks() {
        return (int)(this.consumeSeconds * 20.0f);
    }

    public void emitParticlesAndSounds(RandomSource random, LivingEntity user, ItemStack itemStack, int particleCount) {
        SoundEvent soundEvent;
        float consumablePitch;
        float eatVolume = random.nextBoolean() ? 0.5f : 1.0f;
        float eatPitch = random.triangle(1.0f, 0.2f);
        float drinkVolume = 0.5f;
        float drinkPitch = Mth.randomBetween(random, 0.9f, 1.0f);
        float consumableVolume = this.animation == ItemUseAnimation.DRINK ? 0.5f : eatVolume;
        float f = consumablePitch = this.animation == ItemUseAnimation.DRINK ? drinkPitch : eatPitch;
        if (this.hasConsumeParticles) {
            user.spawnItemParticles(itemStack, particleCount);
        }
        if (user instanceof OverrideConsumeSound) {
            OverrideConsumeSound override = (OverrideConsumeSound)((Object)user);
            soundEvent = override.getConsumeSound(itemStack);
        } else {
            soundEvent = this.sound.value();
        }
        SoundEvent consumeSound = soundEvent;
        user.playSound(consumeSound, consumableVolume, consumablePitch);
    }

    public boolean shouldEmitParticlesAndSounds(int useItemRemainingTicks) {
        int waitTicksBeforeUseEffects;
        int itemUsedForTicks = this.consumeTicks() - useItemRemainingTicks;
        boolean isValidTime = itemUsedForTicks > (waitTicksBeforeUseEffects = (int)((float)this.consumeTicks() * 0.21875f));
        return isValidTime && useItemRemainingTicks % 4 == 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static interface OverrideConsumeSound {
        public SoundEvent getConsumeSound(ItemStack var1);
    }

    public static class Builder {
        private float consumeSeconds = 1.6f;
        private ItemUseAnimation animation = ItemUseAnimation.EAT;
        private Holder<SoundEvent> sound = SoundEvents.GENERIC_EAT;
        private boolean hasConsumeParticles = true;
        private final List<ConsumeEffect> onConsumeEffects = new ArrayList<ConsumeEffect>();

        private Builder() {
        }

        public Builder consumeSeconds(float consumeSeconds) {
            this.consumeSeconds = consumeSeconds;
            return this;
        }

        public Builder animation(ItemUseAnimation animation) {
            this.animation = animation;
            return this;
        }

        public Builder sound(Holder<SoundEvent> sound) {
            this.sound = sound;
            return this;
        }

        public Builder soundAfterConsume(Holder<SoundEvent> soundAfterConsume) {
            return this.onConsume(new PlaySoundConsumeEffect(soundAfterConsume));
        }

        public Builder hasConsumeParticles(boolean hasConsumeParticles) {
            this.hasConsumeParticles = hasConsumeParticles;
            return this;
        }

        public Builder onConsume(ConsumeEffect effect) {
            this.onConsumeEffects.add(effect);
            return this;
        }

        public Consumable build() {
            return new Consumable(this.consumeSeconds, this.animation, this.sound, this.hasConsumeParticles, this.onConsumeEffects);
        }
    }
}

