/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixUtils
 */
package net.minecraft.network;

import com.mojang.datafixers.DataFixUtils;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface HashedStack {
    public static final HashedStack EMPTY = new HashedStack(){

        public String toString() {
            return "<empty>";
        }

        @Override
        public boolean matches(ItemStack stack, HashedPatchMap.HashGenerator hasher) {
            return stack.isEmpty();
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, HashedStack> STREAM_CODEC = ByteBufCodecs.optional(ActualItem.STREAM_CODEC).map(actualItem -> (HashedStack)DataFixUtils.orElse((Optional)actualItem, (Object)EMPTY), hashedStack -> {
        Optional<Object> optional;
        if (hashedStack instanceof ActualItem) {
            ActualItem actualItem = (ActualItem)hashedStack;
            optional = Optional.of(actualItem);
        } else {
            optional = Optional.empty();
        }
        return optional;
    });

    public boolean matches(ItemStack var1, HashedPatchMap.HashGenerator var2);

    public static HashedStack create(ItemStack itemStack, HashedPatchMap.HashGenerator hasher) {
        if (itemStack.isEmpty()) {
            return EMPTY;
        }
        return new ActualItem(itemStack.typeHolder(), itemStack.getCount(), HashedPatchMap.create(itemStack.getComponentsPatch(), hasher));
    }

    public record ActualItem(Holder<Item> item, int count, HashedPatchMap components) implements HashedStack
    {
        public static final StreamCodec<RegistryFriendlyByteBuf, ActualItem> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.holderRegistry(Registries.ITEM), ActualItem::item, ByteBufCodecs.VAR_INT, ActualItem::count, HashedPatchMap.STREAM_CODEC, ActualItem::components, ActualItem::new);

        @Override
        public boolean matches(ItemStack itemStack, HashedPatchMap.HashGenerator hasher) {
            if (this.count != itemStack.getCount()) {
                return false;
            }
            if (!this.item.equals(itemStack.typeHolder())) {
                return false;
            }
            return this.components.matches(itemStack.getComponentsPatch(), hasher);
        }
    }
}

