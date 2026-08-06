/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 */
package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulator;

public interface ContainerComponentManipulators {
    public static final ContainerComponentManipulator<ItemContainerContents> CONTAINER = new ContainerComponentManipulator<ItemContainerContents>(){

        @Override
        public DataComponentType<ItemContainerContents> type() {
            return DataComponents.CONTAINER;
        }

        @Override
        public Stream<ItemStack> getContents(ItemContainerContents component) {
            return component.allItemsCopyStream();
        }

        @Override
        public ItemContainerContents empty() {
            return ItemContainerContents.EMPTY;
        }

        @Override
        public ItemContainerContents setContents(ItemContainerContents component, Stream<ItemStack> newContents) {
            return ItemContainerContents.fromItems(newContents.toList());
        }
    };
    public static final ContainerComponentManipulator<BundleContents> BUNDLE_CONTENTS = new ContainerComponentManipulator<BundleContents>(){

        @Override
        public DataComponentType<BundleContents> type() {
            return DataComponents.BUNDLE_CONTENTS;
        }

        @Override
        public BundleContents empty() {
            return BundleContents.EMPTY;
        }

        @Override
        public Stream<ItemStack> getContents(BundleContents component) {
            return component.itemCopyStream();
        }

        @Override
        public BundleContents setContents(BundleContents component, Stream<ItemStack> newContents) {
            BundleContents.Mutable builder = new BundleContents.Mutable(component).clearItems();
            newContents.forEach(builder::tryInsert);
            return builder.toImmutable();
        }
    };
    public static final ContainerComponentManipulator<ChargedProjectiles> CHARGED_PROJECTILES = new ContainerComponentManipulator<ChargedProjectiles>(){

        @Override
        public DataComponentType<ChargedProjectiles> type() {
            return DataComponents.CHARGED_PROJECTILES;
        }

        @Override
        public ChargedProjectiles empty() {
            return ChargedProjectiles.EMPTY;
        }

        @Override
        public Stream<ItemStack> getContents(ChargedProjectiles component) {
            return component.itemCopies().stream();
        }

        @Override
        public ChargedProjectiles setContents(ChargedProjectiles component, Stream<ItemStack> newContents) {
            return ChargedProjectiles.ofNonEmpty(newContents.filter(s -> !s.isEmpty()).toList());
        }
    };
    public static final Map<DataComponentType<?>, ContainerComponentManipulator<?>> ALL_MANIPULATORS = Stream.of(CONTAINER, BUNDLE_CONTENTS, CHARGED_PROJECTILES).collect(Collectors.toMap(ContainerComponentManipulator::type, e -> e));
    public static final Codec<ContainerComponentManipulator<?>> CODEC = BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec().comapFlatMap(type -> {
        ContainerComponentManipulator<?> manipulator = ALL_MANIPULATORS.get(type);
        return manipulator != null ? DataResult.success(manipulator) : DataResult.error(() -> "No items in component");
    }, ContainerComponentManipulator::type);
}

