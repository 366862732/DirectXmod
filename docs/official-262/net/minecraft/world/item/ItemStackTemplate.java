/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult$Error
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.item;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record ItemStackTemplate(Holder<Item> item, int count, DataComponentPatch components) implements ItemInstance
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<ItemStackTemplate> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Item.CODEC.fieldOf("id").forGetter(ItemStackTemplate::item), (App)ExtraCodecs.intRange(1, 99).optionalFieldOf("count", (Object)1).forGetter(ItemStackTemplate::count), (App)DataComponentPatch.CODEC.optionalFieldOf("components", (Object)DataComponentPatch.EMPTY).forGetter(ItemStackTemplate::components)).apply((Applicative)i, ItemStackTemplate::new));
    public static final Codec<ItemStackTemplate> CODEC = Codec.withAlternative((Codec)MAP_CODEC.codec(), Item.CODEC, item -> new ItemStackTemplate((Item)item.value()));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackTemplate> STREAM_CODEC = StreamCodec.composite(Item.STREAM_CODEC, ItemStackTemplate::item, ByteBufCodecs.VAR_INT, ItemStackTemplate::count, DataComponentPatch.STREAM_CODEC, ItemStackTemplate::components, ItemStackTemplate::new);

    public ItemStackTemplate(Item item) {
        this(item.builtInRegistryHolder(), 1, DataComponentPatch.EMPTY);
    }

    public ItemStackTemplate(Item item, int count) {
        this(item.builtInRegistryHolder(), count, DataComponentPatch.EMPTY);
    }

    public ItemStackTemplate(Item item, DataComponentPatch patch) {
        this(item.builtInRegistryHolder(), 1, patch);
    }

    public ItemStackTemplate {
        if (count == 0 || item.is(Items.AIR.builtInRegistryHolder())) {
            throw new IllegalStateException("Item must be non-empty");
        }
    }

    public static ItemStackTemplate fromNonEmptyStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            throw new IllegalStateException("Stack must be non-empty");
        }
        return ItemStackTemplate.fromStack(itemStack);
    }

    public static ItemStackTemplate fromStack(ItemStack itemStack) {
        return new ItemStackTemplate(itemStack.typeHolder(), itemStack.getCount(), itemStack.getComponentsPatch());
    }

    public ItemStackTemplate withCount(int count) {
        if (this.count == count) {
            return this;
        }
        return new ItemStackTemplate(this.item, count, this.components);
    }

    public ItemStack create() {
        return this.validate(new ItemStack(this.item, this.count, this.components));
    }

    private ItemStack validate(ItemStack result) {
        Optional error = ItemStack.validateStrict(result).error();
        if (error.isPresent()) {
            LOGGER.warn("Can't create item stack with properties {}, error: {}", (Object)this, (Object)((DataResult.Error)error.get()).message());
            return ItemStack.EMPTY;
        }
        return result;
    }

    public ItemStack apply(DataComponentPatch additionalPatch) {
        return this.apply(this.count, additionalPatch);
    }

    public ItemStack apply(int count, DataComponentPatch additionalPatch) {
        ItemStack result = new ItemStack(this.item, count, additionalPatch);
        result.applyComponents(this.components);
        return this.validate(result);
    }

    @Override
    public Holder<Item> typeHolder() {
        return this.item;
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        return this.components.get(this.item.components(), type);
    }
}

