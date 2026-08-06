/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 *  org.apache.commons.lang3.function.TriConsumer
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.component;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

public record ItemAttributeModifiers(List<Entry> modifiers) {
    public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of());
    public static final Codec<ItemAttributeModifiers> CODEC = Entry.CODEC.listOf().xmap(ItemAttributeModifiers::new, ItemAttributeModifiers::modifiers);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers> STREAM_CODEC = StreamCodec.composite(Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemAttributeModifiers::modifiers, ItemAttributeModifiers::new);
    public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    public static Builder builder() {
        return new Builder();
    }

    public ItemAttributeModifiers withModifierAdded(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
        ImmutableList.Builder newModifiers = ImmutableList.builderWithExpectedSize((int)(this.modifiers.size() + 1));
        for (Entry entry : this.modifiers) {
            if (entry.matches(attribute, modifier.id())) continue;
            newModifiers.add((Object)entry);
        }
        newModifiers.add((Object)new Entry(attribute, modifier, slot));
        return new ItemAttributeModifiers((List<Entry>)newModifiers.build());
    }

    public void forEach(EquipmentSlotGroup slot, TriConsumer<Holder<Attribute>, AttributeModifier, Display> consumer) {
        for (Entry entry : this.modifiers) {
            if (!entry.slot.equals(slot)) continue;
            consumer.accept(entry.attribute, (Object)entry.modifier, (Object)entry.display);
        }
    }

    public void forEach(EquipmentSlotGroup slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        for (Entry entry : this.modifiers) {
            if (!entry.slot.equals(slot)) continue;
            consumer.accept(entry.attribute, entry.modifier);
        }
    }

    public void forEach(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        for (Entry entry : this.modifiers) {
            if (!entry.slot.test(slot)) continue;
            consumer.accept(entry.attribute, entry.modifier);
        }
    }

    public double compute(Holder<Attribute> attribute, double baseValue, EquipmentSlot slot) {
        double value = baseValue;
        for (Entry entry : this.modifiers) {
            if (!entry.slot.test(slot) || entry.attribute != attribute) continue;
            double amount = entry.modifier.amount();
            value += (switch (entry.modifier.operation()) {
                default -> throw new MatchException(null, null);
                case AttributeModifier.Operation.ADD_VALUE -> amount;
                case AttributeModifier.Operation.ADD_MULTIPLIED_BASE -> amount * baseValue;
                case AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL -> amount * value;
            });
        }
        return value;
    }

    public static class Builder {
        private final ImmutableList.Builder<Entry> entries = ImmutableList.builder();

        private Builder() {
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
            this.entries.add((Object)new Entry(attribute, modifier, slot));
            return this;
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot, Display display) {
            this.entries.add((Object)new Entry(attribute, modifier, slot, display));
            return this;
        }

        public ItemAttributeModifiers build() {
            return new ItemAttributeModifiers((List<Entry>)this.entries.build());
        }
    }

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot, Display display) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group((App)Attribute.CODEC.fieldOf("type").forGetter(Entry::attribute), (App)AttributeModifier.MAP_CODEC.forGetter(Entry::modifier), (App)EquipmentSlotGroup.CODEC.optionalFieldOf("slot", (Object)EquipmentSlotGroup.ANY).forGetter(Entry::slot), (App)Display.CODEC.optionalFieldOf("display", (Object)Display.Default.INSTANCE).forGetter(Entry::display)).apply((Applicative)i, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(Attribute.STREAM_CODEC, Entry::attribute, AttributeModifier.STREAM_CODEC, Entry::modifier, EquipmentSlotGroup.STREAM_CODEC, Entry::slot, Display.STREAM_CODEC, Entry::display, Entry::new);

        public Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
            this(attribute, modifier, slot, Display.attributeModifiers());
        }

        public boolean matches(Holder<Attribute> attribute, Identifier id) {
            return attribute.equals(this.attribute) && this.modifier.is(id);
        }
    }

    public static interface Display {
        public static final Codec<Display> CODEC = Type.CODEC.dispatch("type", Display::type, type -> type.codec);
        public static final StreamCodec<RegistryFriendlyByteBuf, Display> STREAM_CODEC = Type.STREAM_CODEC.cast().dispatch(Display::type, Type::streamCodec);

        public static Display attributeModifiers() {
            return Default.INSTANCE;
        }

        public static Display hidden() {
            return Hidden.INSTANCE;
        }

        public static Display override(Component component) {
            return new OverrideText(component);
        }

        public Type type();

        public void apply(Consumer<Component> var1, @Nullable Player var2, Holder<Attribute> var3, AttributeModifier var4);

        public record Default() implements Display
        {
            private static final Default INSTANCE = new Default();
            private static final MapCodec<Default> CODEC = MapCodec.unit((Object)INSTANCE);
            private static final StreamCodec<RegistryFriendlyByteBuf, Default> STREAM_CODEC = StreamCodec.unit(INSTANCE);

            @Override
            public Type type() {
                return Type.DEFAULT;
            }

            @Override
            public void apply(Consumer<Component> consumer, @Nullable Player player, Holder<Attribute> attribute, AttributeModifier modifier) {
                double amount = modifier.amount();
                boolean displayWithBase = false;
                if (player != null) {
                    if (modifier.is(Item.BASE_ATTACK_DAMAGE_ID)) {
                        amount += player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                        displayWithBase = true;
                    } else if (modifier.is(Item.BASE_ATTACK_SPEED_ID)) {
                        amount += player.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                        displayWithBase = true;
                    }
                }
                double displayAmount = modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE || modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL ? amount * 100.0 : (attribute.is(Attributes.KNOCKBACK_RESISTANCE) ? amount * 10.0 : amount);
                if (displayWithBase) {
                    consumer.accept(CommonComponents.space().append(Component.translatable("attribute.modifier.equals." + modifier.operation().id(), ATTRIBUTE_MODIFIER_FORMAT.format(displayAmount), Component.translatable(attribute.value().getDescriptionId()))).withStyle(ChatFormatting.DARK_GREEN));
                } else if (amount > 0.0) {
                    consumer.accept(Component.translatable("attribute.modifier.plus." + modifier.operation().id(), ATTRIBUTE_MODIFIER_FORMAT.format(displayAmount), Component.translatable(attribute.value().getDescriptionId())).withStyle(attribute.value().getStyle(true)));
                } else if (amount < 0.0) {
                    consumer.accept(Component.translatable("attribute.modifier.take." + modifier.operation().id(), ATTRIBUTE_MODIFIER_FORMAT.format(-displayAmount), Component.translatable(attribute.value().getDescriptionId())).withStyle(attribute.value().getStyle(false)));
                }
            }
        }

        public record Hidden() implements Display
        {
            private static final Hidden INSTANCE = new Hidden();
            private static final MapCodec<Hidden> CODEC = MapCodec.unit((Object)INSTANCE);
            private static final StreamCodec<RegistryFriendlyByteBuf, Hidden> STREAM_CODEC = StreamCodec.unit(INSTANCE);

            @Override
            public Type type() {
                return Type.HIDDEN;
            }

            @Override
            public void apply(Consumer<Component> consumer, @Nullable Player player, Holder<Attribute> attribute, AttributeModifier modifier) {
            }
        }

        public record OverrideText(Component component) implements Display
        {
            private static final MapCodec<OverrideText> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ComponentSerialization.CODEC.fieldOf("value").forGetter(OverrideText::component)).apply((Applicative)i, OverrideText::new));
            private static final StreamCodec<RegistryFriendlyByteBuf, OverrideText> STREAM_CODEC = StreamCodec.composite(ComponentSerialization.STREAM_CODEC, OverrideText::component, OverrideText::new);

            @Override
            public Type type() {
                return Type.OVERRIDE;
            }

            @Override
            public void apply(Consumer<Component> consumer, @Nullable Player player, Holder<Attribute> attribute, AttributeModifier modifier) {
                consumer.accept(this.component);
            }
        }

        public static enum Type implements StringRepresentable
        {
            DEFAULT("default", 0, Default.CODEC, Default.STREAM_CODEC),
            HIDDEN("hidden", 1, Hidden.CODEC, Hidden.STREAM_CODEC),
            OVERRIDE("override", 2, OverrideText.CODEC, OverrideText.STREAM_CODEC);

            private static final Codec<Type> CODEC;
            private static final IntFunction<Type> BY_ID;
            private static final StreamCodec<ByteBuf, Type> STREAM_CODEC;
            private final String name;
            private final int id;
            private final MapCodec<? extends Display> codec;
            private final StreamCodec<RegistryFriendlyByteBuf, ? extends Display> streamCodec;

            private Type(String name, int id, MapCodec<? extends Display> codec, StreamCodec<RegistryFriendlyByteBuf, ? extends Display> streamCodec) {
                this.name = name;
                this.id = id;
                this.codec = codec;
                this.streamCodec = streamCodec;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }

            private int id() {
                return this.id;
            }

            private StreamCodec<RegistryFriendlyByteBuf, ? extends Display> streamCodec() {
                return this.streamCodec;
            }

            static {
                CODEC = StringRepresentable.fromEnum(Type::values);
                BY_ID = ByIdMap.continuous(Type::id, Type.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
                STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Type::id);
            }
        }
    }
}

