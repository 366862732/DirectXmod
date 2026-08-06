/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.AllOf;
import net.minecraft.world.item.enchantment.effects.ApplyEntityImpulse;
import net.minecraft.world.item.enchantment.effects.ApplyExhaustion;
import net.minecraft.world.item.enchantment.effects.ApplyMobEffect;
import net.minecraft.world.item.enchantment.effects.ChangeItemDamage;
import net.minecraft.world.item.enchantment.effects.DamageEntity;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.ExplodeEffect;
import net.minecraft.world.item.enchantment.effects.Ignite;
import net.minecraft.world.item.enchantment.effects.PlaySoundEffect;
import net.minecraft.world.item.enchantment.effects.ReplaceBlock;
import net.minecraft.world.item.enchantment.effects.ReplaceDisk;
import net.minecraft.world.item.enchantment.effects.RunFunction;
import net.minecraft.world.item.enchantment.effects.SetBlockProperties;
import net.minecraft.world.item.enchantment.effects.SpawnParticlesEffect;
import net.minecraft.world.item.enchantment.effects.SummonEntityEffect;
import net.minecraft.world.phys.Vec3;

public interface EnchantmentLocationBasedEffect {
    public static final Codec<EnchantmentLocationBasedEffect> CODEC = BuiltInRegistries.ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE.byNameCodec().dispatch(EnchantmentLocationBasedEffect::codec, Function.identity());

    public static MapCodec<? extends EnchantmentLocationBasedEffect> bootstrap(Registry<MapCodec<? extends EnchantmentLocationBasedEffect>> registry) {
        Registry.register(registry, "all_of", AllOf.LocationBasedEffects.CODEC);
        Registry.register(registry, "apply_mob_effect", ApplyMobEffect.CODEC);
        Registry.register(registry, "attribute", EnchantmentAttributeEffect.MAP_CODEC);
        Registry.register(registry, "change_item_damage", ChangeItemDamage.CODEC);
        Registry.register(registry, "damage_entity", DamageEntity.CODEC);
        Registry.register(registry, "explode", ExplodeEffect.CODEC);
        Registry.register(registry, "ignite", Ignite.CODEC);
        Registry.register(registry, "apply_impulse", ApplyEntityImpulse.CODEC);
        Registry.register(registry, "apply_exhaustion", ApplyExhaustion.CODEC);
        Registry.register(registry, "play_sound", PlaySoundEffect.CODEC);
        Registry.register(registry, "replace_block", ReplaceBlock.CODEC);
        Registry.register(registry, "replace_disk", ReplaceDisk.CODEC);
        Registry.register(registry, "run_function", RunFunction.CODEC);
        Registry.register(registry, "set_block_properties", SetBlockProperties.CODEC);
        Registry.register(registry, "spawn_particles", SpawnParticlesEffect.CODEC);
        return Registry.register(registry, "summon_entity", SummonEntityEffect.CODEC);
    }

    public void onChangedBlock(ServerLevel var1, int var2, EnchantedItemInUse var3, Entity var4, Vec3 var5, boolean var6);

    default public void onDeactivated(EnchantedItemInUse item, Entity entity, Vec3 position, int level) {
    }

    public MapCodec<? extends EnchantmentLocationBasedEffect> codec();
}

