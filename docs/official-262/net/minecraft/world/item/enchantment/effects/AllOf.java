/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.enchantment.effects;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.phys.Vec3;

public interface AllOf {
    public static <T, A extends T> MapCodec<A> codec(Codec<T> topLevelCodec, Function<List<T>, A> constructor, Function<A, List<T>> accessor) {
        return RecordCodecBuilder.mapCodec(i -> i.group((App)topLevelCodec.listOf().fieldOf("effects").forGetter(accessor)).apply((Applicative)i, constructor));
    }

    public static EntityEffects entityEffects(EnchantmentEntityEffect ... effects) {
        return new EntityEffects(List.of(effects));
    }

    public static LocationBasedEffects locationBasedEffects(EnchantmentLocationBasedEffect ... effects) {
        return new LocationBasedEffects(List.of(effects));
    }

    public static ValueEffects valueEffects(EnchantmentValueEffect ... effects) {
        return new ValueEffects(List.of(effects));
    }

    public record EntityEffects(List<EnchantmentEntityEffect> effects) implements EnchantmentEntityEffect
    {
        public static final MapCodec<EntityEffects> CODEC = AllOf.codec(EnchantmentEntityEffect.CODEC, EntityEffects::new, EntityEffects::effects);

        @Override
        public void apply(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 position) {
            for (EnchantmentEntityEffect effect : this.effects) {
                effect.apply(serverLevel, enchantmentLevel, item, entity, position);
            }
        }

        public MapCodec<EntityEffects> codec() {
            return CODEC;
        }
    }

    public record LocationBasedEffects(List<EnchantmentLocationBasedEffect> effects) implements EnchantmentLocationBasedEffect
    {
        public static final MapCodec<LocationBasedEffects> CODEC = AllOf.codec(EnchantmentLocationBasedEffect.CODEC, LocationBasedEffects::new, LocationBasedEffects::effects);

        @Override
        public void onChangedBlock(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 position, boolean becameActive) {
            for (EnchantmentLocationBasedEffect effect : this.effects) {
                effect.onChangedBlock(serverLevel, enchantmentLevel, item, entity, position, becameActive);
            }
        }

        @Override
        public void onDeactivated(EnchantedItemInUse item, Entity entity, Vec3 position, int level) {
            for (EnchantmentLocationBasedEffect effect : this.effects) {
                effect.onDeactivated(item, entity, position, level);
            }
        }

        public MapCodec<LocationBasedEffects> codec() {
            return CODEC;
        }
    }

    public record ValueEffects(List<EnchantmentValueEffect> effects) implements EnchantmentValueEffect
    {
        public static final MapCodec<ValueEffects> CODEC = AllOf.codec(EnchantmentValueEffect.CODEC, ValueEffects::new, ValueEffects::effects);

        @Override
        public float process(int enchantmentLevel, RandomSource random, float value) {
            for (EnchantmentValueEffect effect : this.effects) {
                value = effect.process(enchantmentLevel, random, value);
            }
            return value;
        }

        public MapCodec<ValueEffects> codec() {
            return CODEC;
        }
    }
}

