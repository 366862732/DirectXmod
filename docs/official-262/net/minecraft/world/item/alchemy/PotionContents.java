/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.alchemy;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;

public record PotionContents(Optional<Holder<Potion>> potion, Optional<Integer> customColor, List<MobEffectInstance> customEffects, Optional<String> customName) implements ConsumableListener,
TooltipProvider
{
    private final List<MobEffectInstance> customEffects;
    public static final PotionContents EMPTY = new PotionContents(Optional.empty(), Optional.empty(), List.of(), Optional.empty());
    private static final Component NO_EFFECT = Component.translatable("effect.none").withStyle(ChatFormatting.GRAY);
    public static final int BASE_POTION_COLOR = -13083194;
    private static final Codec<PotionContents> FULL_CODEC = RecordCodecBuilder.create(i -> i.group((App)Potion.CODEC.optionalFieldOf("potion").forGetter(PotionContents::potion), (App)Codec.INT.optionalFieldOf("custom_color").forGetter(PotionContents::customColor), (App)MobEffectInstance.CODEC.listOf().optionalFieldOf("custom_effects", List.of()).forGetter(PotionContents::customEffects), (App)Codec.STRING.optionalFieldOf("custom_name").forGetter(PotionContents::customName)).apply((Applicative)i, PotionContents::new));
    public static final Codec<PotionContents> CODEC = Codec.withAlternative(FULL_CODEC, Potion.CODEC, PotionContents::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, PotionContents> STREAM_CODEC = StreamCodec.composite(Potion.STREAM_CODEC.apply(ByteBufCodecs::optional), PotionContents::potion, ByteBufCodecs.INT.apply(ByteBufCodecs::optional), PotionContents::customColor, MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()), PotionContents::customEffects, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), PotionContents::customName, PotionContents::new);

    public PotionContents(Holder<Potion> potion) {
        this(Optional.of(potion), Optional.empty(), List.of(), Optional.empty());
    }

    public static ItemStack createItemStack(Item item, Holder<Potion> potion) {
        ItemStack itemStack = new ItemStack(item);
        itemStack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return itemStack;
    }

    public boolean is(Holder<Potion> potion) {
        return this.potion.isPresent() && this.potion.get().is(potion) && this.customEffects.isEmpty();
    }

    public Iterable<MobEffectInstance> getAllEffects() {
        if (this.potion.isEmpty()) {
            return this.customEffects;
        }
        if (this.customEffects.isEmpty()) {
            return this.potion.get().value().getEffects();
        }
        return Iterables.concat(this.potion.get().value().getEffects(), this.customEffects);
    }

    public void forEachEffect(Consumer<MobEffectInstance> consumer, float durationScale) {
        if (this.potion.isPresent()) {
            for (MobEffectInstance effect : this.potion.get().value().getEffects()) {
                consumer.accept(effect.withScaledDuration(durationScale));
            }
        }
        for (MobEffectInstance effect : this.customEffects) {
            consumer.accept(effect.withScaledDuration(durationScale));
        }
    }

    public PotionContents withPotion(Holder<Potion> potion) {
        return new PotionContents(Optional.of(potion), this.customColor, this.customEffects, this.customName);
    }

    public PotionContents withEffectAdded(MobEffectInstance effect) {
        return new PotionContents(this.potion, this.customColor, Util.copyAndAdd(this.customEffects, effect), this.customName);
    }

    public int getColor() {
        return this.getColorOr(-13083194);
    }

    public int getColorOr(int defaultColor) {
        if (this.customColor.isPresent()) {
            return this.customColor.get();
        }
        return PotionContents.getColorOptional(this.getAllEffects()).orElse(defaultColor);
    }

    public Component getName(String prefix) {
        String suffix = this.customName.or(() -> this.potion.map(p -> ((Potion)p.value()).name())).orElse("empty");
        return Component.translatable(prefix + suffix);
    }

    public static OptionalInt getColorOptional(Iterable<MobEffectInstance> effects) {
        int red = 0;
        int green = 0;
        int blue = 0;
        int totalWeight = 0;
        for (MobEffectInstance effect : effects) {
            if (!effect.isVisible()) continue;
            int color = effect.getEffect().value().getColor();
            int amplifier = effect.getAmplifier() + 1;
            red += amplifier * ARGB.red(color);
            green += amplifier * ARGB.green(color);
            blue += amplifier * ARGB.blue(color);
            totalWeight += amplifier;
        }
        if (totalWeight == 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(ARGB.color(red / totalWeight, green / totalWeight, blue / totalWeight));
    }

    public boolean hasEffects() {
        if (!this.customEffects.isEmpty()) {
            return true;
        }
        return this.potion.isPresent() && !this.potion.get().value().getEffects().isEmpty();
    }

    public List<MobEffectInstance> customEffects() {
        return Lists.transform(this.customEffects, MobEffectInstance::new);
    }

    public void applyToLivingEntity(LivingEntity entity, float durationScale) {
        Player playerEntity;
        Level level = entity.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        Player player = entity instanceof Player ? (playerEntity = (Player)entity) : null;
        this.forEachEffect(effect -> {
            if (effect.getEffect().value().isInstantaneous()) {
                effect.getEffect().value().applyInstantaneousEffect(serverLevel, player, player, entity, effect.getAmplifier(), 1.0);
            } else {
                entity.addEffect((MobEffectInstance)effect);
            }
        }, durationScale);
    }

    public static void addPotionTooltip(Iterable<MobEffectInstance> effects, Consumer<Component> lines, float durationScale, float tickrate) {
        ArrayList modifiers = Lists.newArrayList();
        boolean noEffects = true;
        for (MobEffectInstance effect : effects) {
            noEffects = false;
            Holder<MobEffect> mobEffect = effect.getEffect();
            int amplifier = effect.getAmplifier();
            mobEffect.value().createModifiers(amplifier, (attribute, modifier) -> modifiers.add(new Pair(attribute, modifier)));
            MutableComponent line = PotionContents.getPotionDescription(mobEffect, amplifier);
            if (!effect.endsWithin(20)) {
                line = Component.translatable("potion.withDuration", line, MobEffectUtil.formatDuration(effect, durationScale, tickrate));
            }
            lines.accept(line.withStyle(mobEffect.value().getCategory().getTooltipFormatting()));
        }
        if (noEffects) {
            lines.accept(NO_EFFECT);
        }
        if (!modifiers.isEmpty()) {
            lines.accept(CommonComponents.EMPTY);
            lines.accept(Component.translatable("potion.whenDrank").withStyle(ChatFormatting.DARK_PURPLE));
            for (Pair entry : modifiers) {
                AttributeModifier modifier2 = (AttributeModifier)entry.getSecond();
                double amount = modifier2.amount();
                double displayAmount = modifier2.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE || modifier2.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL ? modifier2.amount() * 100.0 : modifier2.amount();
                if (amount > 0.0) {
                    lines.accept(Component.translatable("attribute.modifier.plus." + modifier2.operation().id(), ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(displayAmount), Component.translatable(((Attribute)((Holder)entry.getFirst()).value()).getDescriptionId())).withStyle(ChatFormatting.BLUE));
                    continue;
                }
                if (!(amount < 0.0)) continue;
                lines.accept(Component.translatable("attribute.modifier.take." + modifier2.operation().id(), ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(displayAmount *= -1.0), Component.translatable(((Attribute)((Holder)entry.getFirst()).value()).getDescriptionId())).withStyle(ChatFormatting.RED));
            }
        }
    }

    public static MutableComponent getPotionDescription(Holder<MobEffect> mobEffect, int amplifier) {
        MutableComponent line = Component.translatable(mobEffect.value().getDescriptionId());
        if (amplifier > 0) {
            return Component.translatable("potion.withAmplifier", line, Component.translatable("potion.potency." + amplifier));
        }
        return line;
    }

    @Override
    public void onConsume(Level level, LivingEntity user, ItemStack stack, Consumable consumable) {
        this.applyToLivingEntity(user, stack.getOrDefault(DataComponents.POTION_DURATION_SCALE, Float.valueOf(1.0f)).floatValue());
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        PotionContents.addPotionTooltip(this.getAllEffects(), consumer, components.getOrDefault(DataComponents.POTION_DURATION_SCALE, Float.valueOf(1.0f)).floatValue(), context.tickRate());
    }
}

