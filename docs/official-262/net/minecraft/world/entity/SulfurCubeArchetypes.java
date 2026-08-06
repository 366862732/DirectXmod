/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.SulfurCubeArchetype;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;

public class SulfurCubeArchetypes {
    public static final ResourceKey<SulfurCubeArchetype> REGULAR = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("regular"));
    public static final ResourceKey<SulfurCubeArchetype> BOUNCY = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("bouncy"));
    public static final ResourceKey<SulfurCubeArchetype> SLOW_BOUNCY = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("slow_bouncy"));
    public static final ResourceKey<SulfurCubeArchetype> SLOW_FLAT = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("slow_flat"));
    public static final ResourceKey<SulfurCubeArchetype> FAST_FLAT = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("fast_flat"));
    public static final ResourceKey<SulfurCubeArchetype> LIGHT = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("light"));
    public static final ResourceKey<SulfurCubeArchetype> FAST_SLIDING = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("fast_sliding"));
    public static final ResourceKey<SulfurCubeArchetype> SLOW_SLIDING = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("slow_sliding"));
    public static final ResourceKey<SulfurCubeArchetype> HIGH_RESISTANCE = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("high_resistance"));
    public static final ResourceKey<SulfurCubeArchetype> STICKY = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("sticky"));
    public static final ResourceKey<SulfurCubeArchetype> EXPLOSIVE = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("explosive"));
    public static final ResourceKey<SulfurCubeArchetype> HOT = SulfurCubeArchetypes.createKey(Identifier.withDefaultNamespace("hot"));

    public static void bootstrap(BootstrapContext<SulfurCubeArchetype> context) {
        SulfurCubeArchetypes.register(context, REGULAR, ItemTags.SULFUR_CUBE_ARCHETYPE_REGULAR, SulfurCubeArchetypes.archetype(1.0f, 0.5f, 0.3f, 0.1f), true, Optional.empty(), Optional.empty(), SulfurCubeArchetypes.knockBackHitScale(0.4125f, 0.09f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_REGULAR_HIT, SoundEvents.SULFUR_CUBE_REGULAR_PUSH, 0.2f, 0.5f));
        SulfurCubeArchetypes.register(context, BOUNCY, ItemTags.SULFUR_CUBE_ARCHETYPE_BOUNCY, SulfurCubeArchetypes.archetype(2.0f, 0.9f, 0.3f, 0.01f), true, Optional.empty(), Optional.empty(), SulfurCubeArchetypes.knockBackHitScale(0.4125f, 0.105f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_BOUNCY_HIT, SoundEvents.SULFUR_CUBE_BOUNCY_PUSH, 0.3f, 0.7f));
        SulfurCubeArchetypes.register(context, SLOW_BOUNCY, ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_BOUNCY, SulfurCubeArchetypes.archetype(-0.4f, 0.6f, 0.3f, 0.05f), false, Optional.empty(), Optional.empty(), SulfurCubeArchetypes.knockBackHitScale(0.4125f, 0.24f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_SLOW_BOUNCY_HIT, SoundEvents.SULFUR_CUBE_SLOW_BOUNCY_PUSH, 0.05f, 0.5f));
        SulfurCubeArchetypes.register(context, SLOW_FLAT, ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_FLAT, SulfurCubeArchetypes.archetype(-0.5f, 0.4f, 0.4f, 0.1f), false, Optional.empty(), Optional.empty(), SulfurCubeArchetypes.knockBackHitScale(0.4125f, 0.105f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_SLOW_FLAT_HIT, SoundEvents.SULFUR_CUBE_SLOW_FLAT_PUSH, 0.03f, 0.9f));
        SulfurCubeArchetypes.register(context, FAST_FLAT, ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_FLAT, SulfurCubeArchetypes.archetype(1.0f, 0.5f, 0.2f, 0.01f), false, Optional.empty(), Optional.empty(), SulfurCubeArchetypes.knockBackHitScale(0.9125f, 0.09f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_FAST_FLAT_HIT, SoundEvents.SULFUR_CUBE_FAST_FLAT_PUSH, 0.03f, 0.9f));
        SulfurCubeArchetypes.register(context, LIGHT, ItemTags.SULFUR_CUBE_ARCHETYPE_LIGHT, SulfurCubeArchetypes.archetype(1.0f, 1.0f, 0.3f, 1.8f), true, Optional.empty(), Optional.empty(), SulfurCubeArchetypes.knockBackHitScale(0.4125f, 0.18f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_LIGHT_HIT, SoundEvents.SULFUR_CUBE_LIGHT_PUSH, 0.2f, 0.7f));
        SulfurCubeArchetypes.register(context, FAST_SLIDING, ItemTags.SULFUR_CUBE_ARCHETYPE_FAST_SLIDING, SulfurCubeArchetypes.archetype(-0.5f, 0.1f, 0.05f, 0.01f), false, Optional.empty(), Optional.empty(), SulfurCubeArchetypes.knockBackHitScale(0.6625f, 0.09f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_FAST_SLIDING_HIT, SoundEvents.SULFUR_CUBE_FAST_SLIDING_PUSH, 0.05f, 1.0f));
        SulfurCubeArchetypes.register(context, SLOW_SLIDING, ItemTags.SULFUR_CUBE_ARCHETYPE_SLOW_SLIDING, SulfurCubeArchetypes.archetype(-0.8f, 0.1f, 0.05f, 0.01f), false, Optional.empty(), Optional.empty(), SulfurCubeArchetypes.knockBackHitScale(0.4125f, 0.09f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_SLOW_SLIDING_HIT, SoundEvents.SULFUR_CUBE_SLOW_SLIDING_PUSH, 0.02f, 1.0f));
        SulfurCubeArchetypes.register(context, STICKY, ItemTags.SULFUR_CUBE_ARCHETYPE_STICKY, SulfurCubeArchetypes.archetype(2.0f, 0.0f, 2.0f, 0.01f), false, Optional.empty(), Optional.empty(), SulfurCubeArchetypes.knockBackHitScale(0.4125f, 0.09f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_STICKY_HIT, SoundEvents.SULFUR_CUBE_STICKY_PUSH, 0.05f, 0.5f));
        SulfurCubeArchetypes.register(context, HIGH_RESISTANCE, ItemTags.SULFUR_CUBE_ARCHETYPE_HIGH_RESISTANCE, SulfurCubeArchetypes.archetype(-0.7f, 0.2f, 1.0f, 0.01f), false, Optional.empty(), Optional.empty(), SulfurCubeArchetypes.knockBackHitScale(0.4125f, 0.09f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_HIGH_RESISTANCE_HIT, SoundEvents.SULFUR_CUBE_HIGH_RESISTANCE_PUSH, 0.03f, 0.7f));
        SulfurCubeArchetypes.register(context, EXPLOSIVE, ItemTags.SULFUR_CUBE_ARCHETYPE_EXPLOSIVE, SulfurCubeArchetypes.archetype(1.0f, 0.5f, 0.3f, 0.3f), true, Optional.of(new SulfurCubeArchetype.ExplosionData(3, false, 120)), Optional.empty(), SulfurCubeArchetypes.knockBackHitScale(0.4125f, 0.09f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_EXPLOSIVE_HIT, SoundEvents.SULFUR_CUBE_EXPLOSIVE_PUSH, 0.1f, 0.7f));
        SulfurCubeArchetypes.register(context, HOT, ItemTags.SULFUR_CUBE_ARCHETYPE_HOT, SulfurCubeArchetypes.archetype(1.0f, 0.5f, 0.3f, 0.1f), true, Optional.empty(), Optional.of(SulfurCubeArchetypes.contactDamage(context, DamageTypes.SULFUR_CUBE_HOT, ConstantFloat.of(1.0f), false)), SulfurCubeArchetypes.knockBackHitScale(0.4125f, 0.09f), SulfurCubeArchetypes.soundSettings(SoundEvents.SULFUR_CUBE_HOT_HIT, SoundEvents.SULFUR_CUBE_HOT_PUSH, 0.2f, 0.7f));
    }

    private static ResourceKey<SulfurCubeArchetype> createKey(Identifier id) {
        return ResourceKey.create(Registries.SULFUR_CUBE_ARCHETYPE, id);
    }

    private static Function<ResourceKey<SulfurCubeArchetype>, SulfurCubeArchetype.AttributeEntry> add(Holder<Attribute> attribute, double amount) {
        return key -> SulfurCubeArchetype.AttributeEntry.add(attribute, amount, key);
    }

    private static Function<ResourceKey<SulfurCubeArchetype>, SulfurCubeArchetype.AttributeEntry> multiply(Holder<Attribute> attribute, double amount) {
        return key -> SulfurCubeArchetype.AttributeEntry.multiply(attribute, amount, key);
    }

    private static List<Function<ResourceKey<SulfurCubeArchetype>, SulfurCubeArchetype.AttributeEntry>> archetype(float speed, float bounce, float friction, float drag) {
        return List.of(SulfurCubeArchetypes.add(Attributes.KNOCKBACK_RESISTANCE, -speed), SulfurCubeArchetypes.add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, -speed), SulfurCubeArchetypes.add(Attributes.BOUNCINESS, bounce), SulfurCubeArchetypes.multiply(Attributes.FRICTION_MODIFIER, friction), SulfurCubeArchetypes.multiply(Attributes.AIR_DRAG_MODIFIER, drag));
    }

    private static SulfurCubeArchetype.ContactDamage contactDamage(BootstrapContext<SulfurCubeArchetype> context, ResourceKey<DamageType> damageType, FloatProvider amount, boolean attributeToSource) {
        return new SulfurCubeArchetype.ContactDamage(context.lookup(Registries.DAMAGE_TYPE).getOrThrow(damageType), amount, attributeToSource);
    }

    private static SulfurCubeArchetype.KnockbackModifiers knockBackHitScale(float horizontalPower, float verticalPower) {
        return new SulfurCubeArchetype.KnockbackModifiers(horizontalPower, verticalPower);
    }

    private static SulfurCubeArchetype.SoundSettings soundSettings(Holder<SoundEvent> hitSound, Holder<SoundEvent> pushSound, float threshold, float cooldown) {
        return new SulfurCubeArchetype.SoundSettings(hitSound, pushSound, threshold, cooldown);
    }

    private static void register(BootstrapContext<SulfurCubeArchetype> context, ResourceKey<SulfurCubeArchetype> name, TagKey<Item> blocks, List<Function<ResourceKey<SulfurCubeArchetype>, SulfurCubeArchetype.AttributeEntry>> modifiers, boolean floats, Optional<SulfurCubeArchetype.ExplosionData> maxFuse, Optional<SulfurCubeArchetype.ContactDamage> contactDamage, SulfurCubeArchetype.KnockbackModifiers knockbackModifiers, SulfurCubeArchetype.SoundSettings soundSettings) {
        context.register(name, new SulfurCubeArchetype(context.lookup(Registries.ITEM).getOrThrow(blocks), modifiers.stream().map(f -> (SulfurCubeArchetype.AttributeEntry)f.apply(name)).toList(), floats, maxFuse, contactDamage, knockbackModifiers, soundSettings));
    }
}

