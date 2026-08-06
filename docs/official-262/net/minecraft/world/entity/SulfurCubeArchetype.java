/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.entity;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;

public record SulfurCubeArchetype(HolderSet<Item> items, List<AttributeEntry> attributeModifiers, boolean buoyant, Optional<ExplosionData> explosion, Optional<ContactDamage> contactDamage, KnockbackModifiers knockbackModifiers, SoundSettings soundSettings) {
    public static final Codec<SulfurCubeArchetype> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group((App)RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(SulfurCubeArchetype::items), (App)AttributeEntry.CODEC.listOf().fieldOf("attribute_modifiers").forGetter(SulfurCubeArchetype::attributeModifiers), (App)Codec.BOOL.optionalFieldOf("buoyant", (Object)false).forGetter(SulfurCubeArchetype::buoyant), (App)ExplosionData.CODEC.optionalFieldOf("explosion").forGetter(SulfurCubeArchetype::explosion), (App)ContactDamage.CODEC.optionalFieldOf("contact_damage").forGetter(SulfurCubeArchetype::contactDamage), (App)KnockbackModifiers.CODEC.fieldOf("knockback_modifiers").forGetter(SulfurCubeArchetype::knockbackModifiers), (App)SoundSettings.CODEC.fieldOf("sound_settings").forGetter(SulfurCubeArchetype::soundSettings)).apply((Applicative)i, SulfurCubeArchetype::new));
    public static KnockbackModifiers DEFAULT_KNOCKBACK_MODIFIERS = new KnockbackModifiers(0.33f, 0.06f);
    public static SoundSettings DEFAULT_SOUND_SETTINGS = new SoundSettings(SoundEvents.SULFUR_CUBE_REGULAR_HIT, SoundEvents.SULFUR_CUBE_REGULAR_PUSH, 0.2f, 0.5f);

    public record KnockbackModifiers(float horizontalPower, float verticalPower) {
        public static final Codec<KnockbackModifiers> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.FLOAT.fieldOf("horizontal_power").forGetter(KnockbackModifiers::horizontalPower), (App)Codec.FLOAT.fieldOf("vertical_power").forGetter(KnockbackModifiers::verticalPower)).apply((Applicative)i, KnockbackModifiers::new));
    }

    public record SoundSettings(Holder<SoundEvent> hitSound, Holder<SoundEvent> pushSound, float pushSoundImpulseThreshold, float pushSoundCooldown) {
        public static final Codec<SoundSettings> CODEC = RecordCodecBuilder.create(i -> i.group((App)SoundEvent.CODEC.fieldOf("hit_sound").forGetter(SoundSettings::hitSound), (App)SoundEvent.CODEC.fieldOf("push_sound").forGetter(SoundSettings::pushSound), (App)Codec.FLOAT.fieldOf("push_sound_impulse_threshold").forGetter(SoundSettings::pushSoundImpulseThreshold), (App)Codec.FLOAT.fieldOf("push_sound_cooldown").forGetter(SoundSettings::pushSoundCooldown)).apply((Applicative)i, SoundSettings::new));
    }

    public record AttributeEntry(Holder<Attribute> attribute, AttributeModifier modifier) {
        public static final Codec<AttributeEntry> CODEC = RecordCodecBuilder.create(i -> i.group((App)Attribute.CODEC.fieldOf("attribute").forGetter(AttributeEntry::attribute), (App)AttributeModifier.MAP_CODEC.forGetter(AttributeEntry::modifier)).apply((Applicative)i, AttributeEntry::new));

        public static AttributeEntry add(Holder<Attribute> attribute, double amount, ResourceKey<SulfurCubeArchetype> archetype) {
            return new AttributeEntry(attribute, new AttributeModifier(Identifier.withDefaultNamespace(archetype.identifier().getPath() + "_add_" + attribute.unwrapKey().get().identifier().getPath()), amount, AttributeModifier.Operation.ADD_VALUE));
        }

        public static AttributeEntry multiply(Holder<Attribute> attribute, double amount, ResourceKey<SulfurCubeArchetype> archetype) {
            return new AttributeEntry(attribute, new AttributeModifier(Identifier.withDefaultNamespace(archetype.identifier().getPath() + "_mul_" + attribute.unwrapKey().get().identifier().getPath()), amount - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public record ExplosionData(int power, boolean causesFire, int fuse) {
        public static final Codec<ExplosionData> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.NON_NEGATIVE_INT.fieldOf("power").forGetter(ExplosionData::power), (App)Codec.BOOL.fieldOf("causes_fire").forGetter(ExplosionData::causesFire), (App)ExtraCodecs.POSITIVE_INT.fieldOf("fuse").forGetter(ExplosionData::fuse)).apply((Applicative)i, ExplosionData::new));
    }

    public record ContactDamage(Holder<DamageType> damageType, FloatProvider amount, boolean attributeToSource) {
        public static final Codec<ContactDamage> CODEC = RecordCodecBuilder.create(i -> i.group((App)DamageType.CODEC.fieldOf("damage_type").forGetter(ContactDamage::damageType), (App)FloatProviders.codec(0.0f).fieldOf("amount").forGetter(ContactDamage::amount), (App)Codec.BOOL.fieldOf("attribute_to_source").forGetter(ContactDamage::attributeToSource)).apply((Applicative)i, ContactDamage::new));
    }
}

