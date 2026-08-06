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
package net.minecraft.world.item.crafting.display;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.block.entity.FuelValues;

public interface SlotDisplay {
    public static final Codec<SlotDisplay> CODEC = BuiltInRegistries.SLOT_DISPLAY.byNameCodec().dispatch(SlotDisplay::type, Type::codec);
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay> STREAM_CODEC = ByteBufCodecs.registry(Registries.SLOT_DISPLAY).dispatch(SlotDisplay::type, Type::streamCodec);

    public <T> Stream<T> resolve(ContextMap var1, DisplayContentsFactory<T> var2);

    public Type<? extends SlotDisplay> type();

    default public boolean isEnabled(FeatureFlagSet enabledFeatures) {
        return true;
    }

    default public List<ItemStack> resolveForStacks(ContextMap context) {
        return this.resolve(context, ItemStackContentsFactory.INSTANCE).toList();
    }

    default public ItemStack resolveForFirstStack(ContextMap context) {
        return this.resolve(context, ItemStackContentsFactory.INSTANCE).findFirst().orElse(ItemStack.EMPTY);
    }

    private static <T> Stream<T> applyDemoTransformation(ContextMap context, DisplayContentsFactory<T> factory, SlotDisplay firstDisplay, SlotDisplay secondDisplay, RandomSource randomSource, BinaryOperator<ItemStack> operation) {
        if (factory instanceof DisplayContentsFactory.ForStacks) {
            DisplayContentsFactory.ForStacks stacks = (DisplayContentsFactory.ForStacks)factory;
            List<ItemStack> firstItems = firstDisplay.resolveForStacks(context);
            if (firstItems.isEmpty()) {
                return Stream.empty();
            }
            List<ItemStack> secondItems = secondDisplay.resolveForStacks(context);
            if (secondItems.isEmpty()) {
                return Stream.empty();
            }
            return Stream.generate(() -> {
                ItemStack first = (ItemStack)Util.getRandom(firstItems, randomSource);
                ItemStack second = (ItemStack)Util.getRandom(secondItems, randomSource);
                return (ItemStack)operation.apply(first, second);
            }).limit(256L).filter(s -> !s.isEmpty()).limit(16L).map(stacks::forStack);
        }
        return Stream.empty();
    }

    private static <T> Stream<T> applyDemoTransformation(ContextMap context, DisplayContentsFactory<T> factory, SlotDisplay firstDisplay, SlotDisplay secondDisplay, BinaryOperator<ItemStack> operation) {
        if (factory instanceof DisplayContentsFactory.ForStacks) {
            DisplayContentsFactory.ForStacks stacks = (DisplayContentsFactory.ForStacks)factory;
            List<ItemStack> firstItems = firstDisplay.resolveForStacks(context);
            if (firstItems.isEmpty()) {
                return Stream.empty();
            }
            List<ItemStack> secondItems = secondDisplay.resolveForStacks(context);
            if (secondItems.isEmpty()) {
                return Stream.empty();
            }
            int cycle = firstItems.size() * secondItems.size();
            return IntStream.range(0, cycle).mapToObj(index -> {
                int firstItemCount = firstItems.size();
                int firstItemIndex = index % firstItemCount;
                int secondItemIndex = index / firstItemCount;
                ItemStack first = (ItemStack)firstItems.get(firstItemIndex);
                ItemStack second = (ItemStack)secondItems.get(secondItemIndex);
                return (ItemStack)operation.apply(first, second);
            }).filter(s -> !s.isEmpty()).limit(16L).map(stacks::forStack);
        }
        return Stream.empty();
    }

    public static class ItemStackContentsFactory
    implements DisplayContentsFactory.ForStacks<ItemStack> {
        public static final ItemStackContentsFactory INSTANCE = new ItemStackContentsFactory();

        @Override
        public ItemStack forStack(ItemStack stack) {
            return stack;
        }
    }

    public record WithRemainder(SlotDisplay input, SlotDisplay remainder) implements SlotDisplay
    {
        public static final MapCodec<WithRemainder> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CODEC.fieldOf("input").forGetter(WithRemainder::input), (App)CODEC.fieldOf("remainder").forGetter(WithRemainder::remainder)).apply((Applicative)i, WithRemainder::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, WithRemainder> STREAM_CODEC = StreamCodec.composite(STREAM_CODEC, WithRemainder::input, STREAM_CODEC, WithRemainder::remainder, WithRemainder::new);
        public static final Type<WithRemainder> TYPE = new Type<WithRemainder>(MAP_CODEC, STREAM_CODEC);

        public Type<WithRemainder> type() {
            return TYPE;
        }

        @Override
        public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
            if (factory instanceof DisplayContentsFactory.ForRemainders) {
                DisplayContentsFactory.ForRemainders remainders = (DisplayContentsFactory.ForRemainders)factory;
                List resolvedRemainders = this.remainder.resolve(context, factory).toList();
                return this.input.resolve(context, factory).map(input -> remainders.addRemainder(input, resolvedRemainders));
            }
            return this.input.resolve(context, factory);
        }

        @Override
        public boolean isEnabled(FeatureFlagSet enabledFeatures) {
            return this.input.isEnabled(enabledFeatures) && this.remainder.isEnabled(enabledFeatures);
        }
    }

    public record Composite(List<SlotDisplay> contents) implements SlotDisplay
    {
        public static final MapCodec<Composite> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CODEC.listOf().fieldOf("contents").forGetter(Composite::contents)).apply((Applicative)i, Composite::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Composite> STREAM_CODEC = StreamCodec.composite(STREAM_CODEC.apply(ByteBufCodecs.list()), Composite::contents, Composite::new);
        public static final Type<Composite> TYPE = new Type<Composite>(MAP_CODEC, STREAM_CODEC);

        public Type<Composite> type() {
            return TYPE;
        }

        @Override
        public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
            return this.contents.stream().flatMap(d -> d.resolve(context, factory));
        }

        @Override
        public boolean isEnabled(FeatureFlagSet enabledFeatures) {
            return this.contents.stream().allMatch(c -> c.isEnabled(enabledFeatures));
        }
    }

    public record TagSlotDisplay(TagKey<Item> tag) implements SlotDisplay
    {
        public static final MapCodec<TagSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(TagSlotDisplay::tag)).apply((Applicative)i, TagSlotDisplay::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, TagSlotDisplay> STREAM_CODEC = StreamCodec.composite(TagKey.streamCodec(Registries.ITEM), TagSlotDisplay::tag, TagSlotDisplay::new);
        public static final Type<TagSlotDisplay> TYPE = new Type<TagSlotDisplay>(MAP_CODEC, STREAM_CODEC);

        public Type<TagSlotDisplay> type() {
            return TYPE;
        }

        @Override
        public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
            if (factory instanceof DisplayContentsFactory.ForStacks) {
                DisplayContentsFactory.ForStacks stacks = (DisplayContentsFactory.ForStacks)factory;
                HolderLookup.Provider registries = context.getOptional(SlotDisplayContext.REGISTRIES);
                if (registries != null) {
                    return registries.lookupOrThrow(Registries.ITEM).get(this.tag).map(t -> t.stream().map(stacks::forStack)).stream().flatMap(s -> s);
                }
            }
            return Stream.empty();
        }
    }

    public record ItemStackSlotDisplay(ItemStackTemplate stack) implements SlotDisplay
    {
        public static final MapCodec<ItemStackSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ItemStackTemplate.CODEC.fieldOf("item").forGetter(ItemStackSlotDisplay::stack)).apply((Applicative)i, ItemStackSlotDisplay::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackSlotDisplay> STREAM_CODEC = StreamCodec.composite(ItemStackTemplate.STREAM_CODEC, ItemStackSlotDisplay::stack, ItemStackSlotDisplay::new);
        public static final Type<ItemStackSlotDisplay> TYPE = new Type<ItemStackSlotDisplay>(MAP_CODEC, STREAM_CODEC);

        public Type<ItemStackSlotDisplay> type() {
            return TYPE;
        }

        @Override
        public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
            if (factory instanceof DisplayContentsFactory.ForStacks) {
                DisplayContentsFactory.ForStacks stacks = (DisplayContentsFactory.ForStacks)factory;
                return Stream.of(stacks.forStack(this.stack.create()));
            }
            return Stream.empty();
        }

        @Override
        public boolean isEnabled(FeatureFlagSet enabledFeatures) {
            return this.stack.item().value().isEnabled(enabledFeatures);
        }
    }

    public record ItemSlotDisplay(Holder<Item> item) implements SlotDisplay
    {
        public static final MapCodec<ItemSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Item.CODEC.fieldOf("item").forGetter(ItemSlotDisplay::item)).apply((Applicative)i, ItemSlotDisplay::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemSlotDisplay> STREAM_CODEC = StreamCodec.composite(Item.STREAM_CODEC, ItemSlotDisplay::item, ItemSlotDisplay::new);
        public static final Type<ItemSlotDisplay> TYPE = new Type<ItemSlotDisplay>(MAP_CODEC, STREAM_CODEC);

        public ItemSlotDisplay(Item item) {
            this(item.builtInRegistryHolder());
        }

        public Type<ItemSlotDisplay> type() {
            return TYPE;
        }

        @Override
        public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
            if (factory instanceof DisplayContentsFactory.ForStacks) {
                DisplayContentsFactory.ForStacks stacks = (DisplayContentsFactory.ForStacks)factory;
                return Stream.of(stacks.forStack(this.item));
            }
            return Stream.empty();
        }

        @Override
        public boolean isEnabled(FeatureFlagSet enabledFeatures) {
            return this.item.value().isEnabled(enabledFeatures);
        }
    }

    public record SmithingTrimDemoSlotDisplay(SlotDisplay base, SlotDisplay material, Holder<TrimPattern> pattern) implements SlotDisplay
    {
        public static final MapCodec<SmithingTrimDemoSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CODEC.fieldOf("base").forGetter(SmithingTrimDemoSlotDisplay::base), (App)CODEC.fieldOf("material").forGetter(SmithingTrimDemoSlotDisplay::material), (App)TrimPattern.CODEC.fieldOf("pattern").forGetter(SmithingTrimDemoSlotDisplay::pattern)).apply((Applicative)i, SmithingTrimDemoSlotDisplay::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, SmithingTrimDemoSlotDisplay> STREAM_CODEC = StreamCodec.composite(STREAM_CODEC, SmithingTrimDemoSlotDisplay::base, STREAM_CODEC, SmithingTrimDemoSlotDisplay::material, TrimPattern.STREAM_CODEC, SmithingTrimDemoSlotDisplay::pattern, SmithingTrimDemoSlotDisplay::new);
        public static final Type<SmithingTrimDemoSlotDisplay> TYPE = new Type<SmithingTrimDemoSlotDisplay>(MAP_CODEC, STREAM_CODEC);

        public Type<SmithingTrimDemoSlotDisplay> type() {
            return TYPE;
        }

        @Override
        public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
            RandomSource randomSource = RandomSource.createThreadLocalInstance(System.identityHashCode(this));
            BinaryOperator transformation = (base, material) -> SmithingTrimRecipe.applyTrim(base, material, this.pattern);
            return SlotDisplay.applyDemoTransformation(context, factory, this.base, this.material, randomSource, transformation);
        }
    }

    public record DyedSlotDemo(SlotDisplay dye, SlotDisplay target) implements SlotDisplay
    {
        public static final MapCodec<DyedSlotDemo> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CODEC.fieldOf("dye").forGetter(DyedSlotDemo::dye), (App)CODEC.fieldOf("target").forGetter(DyedSlotDemo::target)).apply((Applicative)i, DyedSlotDemo::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, DyedSlotDemo> STREAM_CODEC = StreamCodec.composite(STREAM_CODEC, DyedSlotDemo::dye, STREAM_CODEC, DyedSlotDemo::target, DyedSlotDemo::new);
        public static final Type<DyedSlotDemo> TYPE = new Type<DyedSlotDemo>(MAP_CODEC, STREAM_CODEC);

        public Type<DyedSlotDemo> type() {
            return TYPE;
        }

        @Override
        public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
            BinaryOperator transformation = (target, dye) -> {
                DyeColor dyeValue = dye.getOrDefault(DataComponents.DYE, DyeColor.WHITE);
                return DyedItemColor.applyDyes(target.copy(), List.of(dyeValue));
            };
            return SlotDisplay.applyDemoTransformation(context, factory, this.target, this.dye, transformation);
        }
    }

    public record OnlyWithComponent(SlotDisplay source, DataComponentType<?> component) implements SlotDisplay
    {
        public static final MapCodec<OnlyWithComponent> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CODEC.fieldOf("contents").forGetter(OnlyWithComponent::source), (App)DataComponentType.CODEC.fieldOf("component").forGetter(OnlyWithComponent::component)).apply((Applicative)i, OnlyWithComponent::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, OnlyWithComponent> STREAM_CODEC = StreamCodec.composite(STREAM_CODEC, OnlyWithComponent::source, DataComponentType.STREAM_CODEC, OnlyWithComponent::component, OnlyWithComponent::new);
        public static final Type<OnlyWithComponent> TYPE = new Type<OnlyWithComponent>(MAP_CODEC, STREAM_CODEC);

        @Override
        public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> builder) {
            if (builder instanceof DisplayContentsFactory.ForStacks) {
                DisplayContentsFactory.ForStacks stacks = (DisplayContentsFactory.ForStacks)builder;
                return this.source.resolve(context, ItemStackContentsFactory.INSTANCE).filter(s -> s.has(this.component)).map(stacks::forStack);
            }
            return Stream.empty();
        }

        public Type<OnlyWithComponent> type() {
            return TYPE;
        }
    }

    public record WithAnyPotion(SlotDisplay display) implements SlotDisplay
    {
        public static final MapCodec<WithAnyPotion> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CODEC.fieldOf("contents").forGetter(WithAnyPotion::display)).apply((Applicative)i, WithAnyPotion::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, WithAnyPotion> STREAM_CODEC = StreamCodec.composite(STREAM_CODEC, WithAnyPotion::display, WithAnyPotion::new);
        public static final Type<WithAnyPotion> TYPE = new Type<WithAnyPotion>(MAP_CODEC, STREAM_CODEC);

        public Type<WithAnyPotion> type() {
            return TYPE;
        }

        @Override
        public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
            if (factory instanceof DisplayContentsFactory.ForStacks) {
                DisplayContentsFactory.ForStacks stacks = (DisplayContentsFactory.ForStacks)factory;
                List<ItemStack> displayItems = this.display.resolveForStacks(context);
                Optional potions = Optional.ofNullable(context.getOptional(SlotDisplayContext.REGISTRIES)).flatMap(r -> r.lookup(Registries.POTION));
                return potions.stream().flatMap(HolderLookup::listElements).flatMap(potion -> {
                    PotionContents potionContents = new PotionContents((Holder<Potion>)potion);
                    return displayItems.stream().map(item -> {
                        ItemStack itemCopy = item.copy();
                        itemCopy.set(DataComponents.POTION_CONTENTS, potionContents);
                        return stacks.forStack(itemCopy);
                    });
                });
            }
            return Stream.empty();
        }
    }

    public static class AnyFuel
    implements SlotDisplay {
        public static final AnyFuel INSTANCE = new AnyFuel();
        public static final MapCodec<AnyFuel> MAP_CODEC = MapCodec.unit((Object)INSTANCE);
        public static final StreamCodec<RegistryFriendlyByteBuf, AnyFuel> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        public static final Type<AnyFuel> TYPE = new Type<AnyFuel>(MAP_CODEC, STREAM_CODEC);

        private AnyFuel() {
        }

        public Type<AnyFuel> type() {
            return TYPE;
        }

        public String toString() {
            return "<any fuel>";
        }

        @Override
        public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
            if (factory instanceof DisplayContentsFactory.ForStacks) {
                DisplayContentsFactory.ForStacks stacks = (DisplayContentsFactory.ForStacks)factory;
                FuelValues fuelValues = context.getOptional(SlotDisplayContext.FUEL_VALUES);
                if (fuelValues != null) {
                    return fuelValues.fuelItems().stream().map(stacks::forStack);
                }
            }
            return Stream.empty();
        }
    }

    public static class Empty
    implements SlotDisplay {
        public static final Empty INSTANCE = new Empty();
        public static final MapCodec<Empty> MAP_CODEC = MapCodec.unit((Object)INSTANCE);
        public static final StreamCodec<RegistryFriendlyByteBuf, Empty> STREAM_CODEC = StreamCodec.unit(INSTANCE);
        public static final Type<Empty> TYPE = new Type<Empty>(MAP_CODEC, STREAM_CODEC);

        private Empty() {
        }

        public Type<Empty> type() {
            return TYPE;
        }

        public String toString() {
            return "<empty>";
        }

        @Override
        public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
            return Stream.empty();
        }
    }

    public record Type<T extends SlotDisplay>(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
    }
}

