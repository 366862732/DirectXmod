/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.core.component.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.predicates.CollectionPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public record AttributeModifiersPredicate(Optional<CollectionPredicate<ItemAttributeModifiers.Entry, EntryPredicate>> modifiers) implements SingleComponentItemPredicate<ItemAttributeModifiers>
{
    public static final Codec<AttributeModifiersPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)CollectionPredicate.codec(EntryPredicate.CODEC).optionalFieldOf("modifiers").forGetter(AttributeModifiersPredicate::modifiers)).apply((Applicative)i, AttributeModifiersPredicate::new));

    @Override
    public DataComponentType<ItemAttributeModifiers> componentType() {
        return DataComponents.ATTRIBUTE_MODIFIERS;
    }

    @Override
    public boolean matches(ItemAttributeModifiers value) {
        return !this.modifiers.isPresent() || this.modifiers.get().test(value.modifiers());
    }

    public record EntryPredicate(Optional<HolderSet<Attribute>> attribute, Optional<Identifier> id, MinMaxBounds.Doubles amount, Optional<AttributeModifier.Operation> operation, Optional<EquipmentSlotGroup> slot) implements Predicate<ItemAttributeModifiers.Entry>
    {
        public static final Codec<EntryPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)RegistryCodecs.homogeneousList(Registries.ATTRIBUTE).optionalFieldOf("attribute").forGetter(EntryPredicate::attribute), (App)Identifier.CODEC.optionalFieldOf("id").forGetter(EntryPredicate::id), (App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("amount", (Object)MinMaxBounds.Doubles.ANY).forGetter(EntryPredicate::amount), (App)AttributeModifier.Operation.CODEC.optionalFieldOf("operation").forGetter(EntryPredicate::operation), (App)EquipmentSlotGroup.CODEC.optionalFieldOf("slot").forGetter(EntryPredicate::slot)).apply((Applicative)i, EntryPredicate::new));

        @Override
        public boolean test(ItemAttributeModifiers.Entry value) {
            if (this.attribute.isPresent() && !this.attribute.get().contains(value.attribute())) {
                return false;
            }
            if (this.id.isPresent() && !this.id.get().equals(value.modifier().id())) {
                return false;
            }
            if (!this.amount.matches(value.modifier().amount())) {
                return false;
            }
            if (this.operation.isPresent() && this.operation.get() != value.modifier().operation()) {
                return false;
            }
            return !this.slot.isPresent() || this.slot.get() == value.slot();
        }
    }
}

