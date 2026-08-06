/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.predicates.AttributeModifiersPredicate;
import net.minecraft.core.component.predicates.BundlePredicate;
import net.minecraft.core.component.predicates.ContainerPredicate;
import net.minecraft.core.component.predicates.CustomDataPredicate;
import net.minecraft.core.component.predicates.DamagePredicate;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.component.predicates.FireworkExplosionPredicate;
import net.minecraft.core.component.predicates.FireworksPredicate;
import net.minecraft.core.component.predicates.JukeboxPlayablePredicate;
import net.minecraft.core.component.predicates.PotionsPredicate;
import net.minecraft.core.component.predicates.TrimPredicate;
import net.minecraft.core.component.predicates.VillagerTypePredicate;
import net.minecraft.core.component.predicates.WritableBookPredicate;
import net.minecraft.core.component.predicates.WrittenBookPredicate;
import net.minecraft.core.registries.BuiltInRegistries;

public class DataComponentPredicates {
    public static final DataComponentPredicate.Type<DamagePredicate> DAMAGE = DataComponentPredicates.register("damage", DamagePredicate.CODEC);
    public static final DataComponentPredicate.Type<EnchantmentsPredicate.Enchantments> ENCHANTMENTS = DataComponentPredicates.register("enchantments", EnchantmentsPredicate.Enchantments.CODEC);
    public static final DataComponentPredicate.Type<EnchantmentsPredicate.StoredEnchantments> STORED_ENCHANTMENTS = DataComponentPredicates.register("stored_enchantments", EnchantmentsPredicate.StoredEnchantments.CODEC);
    public static final DataComponentPredicate.Type<PotionsPredicate> POTIONS = DataComponentPredicates.register("potion_contents", PotionsPredicate.CODEC);
    public static final DataComponentPredicate.Type<CustomDataPredicate> CUSTOM_DATA = DataComponentPredicates.register("custom_data", CustomDataPredicate.CODEC);
    public static final DataComponentPredicate.Type<ContainerPredicate> CONTAINER = DataComponentPredicates.register("container", ContainerPredicate.CODEC);
    public static final DataComponentPredicate.Type<BundlePredicate> BUNDLE_CONTENTS = DataComponentPredicates.register("bundle_contents", BundlePredicate.CODEC);
    public static final DataComponentPredicate.Type<FireworkExplosionPredicate> FIREWORK_EXPLOSION = DataComponentPredicates.register("firework_explosion", FireworkExplosionPredicate.CODEC);
    public static final DataComponentPredicate.Type<FireworksPredicate> FIREWORKS = DataComponentPredicates.register("fireworks", FireworksPredicate.CODEC);
    public static final DataComponentPredicate.Type<WritableBookPredicate> WRITABLE_BOOK = DataComponentPredicates.register("writable_book_content", WritableBookPredicate.CODEC);
    public static final DataComponentPredicate.Type<WrittenBookPredicate> WRITTEN_BOOK = DataComponentPredicates.register("written_book_content", WrittenBookPredicate.CODEC);
    public static final DataComponentPredicate.Type<AttributeModifiersPredicate> ATTRIBUTE_MODIFIERS = DataComponentPredicates.register("attribute_modifiers", AttributeModifiersPredicate.CODEC);
    public static final DataComponentPredicate.Type<TrimPredicate> ARMOR_TRIM = DataComponentPredicates.register("trim", TrimPredicate.CODEC);
    public static final DataComponentPredicate.Type<JukeboxPlayablePredicate> JUKEBOX_PLAYABLE = DataComponentPredicates.register("jukebox_playable", JukeboxPlayablePredicate.CODEC);
    public static final DataComponentPredicate.Type<VillagerTypePredicate> VILLAGER_VARIANT = DataComponentPredicates.register("villager/variant", VillagerTypePredicate.CODEC);

    private static <T extends DataComponentPredicate> DataComponentPredicate.Type<T> register(String id, Codec<T> codec) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE, id, new DataComponentPredicate.ConcreteType<T>(codec));
    }

    public static DataComponentPredicate.Type<?> bootstrap(Registry<DataComponentPredicate.Type<?>> registry) {
        return DAMAGE;
    }
}

