/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public record SulfurCubeContent(ItemStackTemplate absorbedBlockItemStack) implements TooltipProvider
{
    public static final Codec<SulfurCubeContent> CODEC = ItemStackTemplate.CODEC.xmap(SulfurCubeContent::new, SulfurCubeContent::absorbedBlockItemStack);
    public static final StreamCodec<RegistryFriendlyByteBuf, SulfurCubeContent> STREAM_CODEC = ItemStackTemplate.STREAM_CODEC.map(SulfurCubeContent::new, SulfurCubeContent::absorbedBlockItemStack);

    public static SulfurCubeContent ofNonEmpty(ItemStack itemStack) {
        return new SulfurCubeContent(ItemStackTemplate.fromNonEmptyStack(itemStack));
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        ItemStack currentStack = this.absorbedBlockItemStack.create();
        consumer.accept(Component.translatable("entity.minecraft.sulfur_cube.content", currentStack.getHoverName()).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }
}

