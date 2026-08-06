/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.common.collect.Lists
 *  com.google.gson.JsonElement
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BookContent;
import net.minecraft.world.item.component.TooltipProvider;
import org.jspecify.annotations.Nullable;

public record WrittenBookContent(Filterable<String> title, String author, int generation, List<Filterable<Component>> pages, boolean resolved) implements BookContent<Component, WrittenBookContent>,
TooltipProvider
{
    public static final WrittenBookContent EMPTY = new WrittenBookContent(Filterable.passThrough(""), "", 0, List.of(), true);
    public static final int PAGE_LENGTH = Short.MAX_VALUE;
    public static final int TITLE_LENGTH = 16;
    public static final int TITLE_MAX_LENGTH = 32;
    public static final int MAX_GENERATION = 3;
    public static final Codec<Component> CONTENT_CODEC = ComponentSerialization.flatRestrictedCodec(Short.MAX_VALUE);
    public static final Codec<List<Filterable<Component>>> PAGES_CODEC = WrittenBookContent.pagesCodec(CONTENT_CODEC);
    public static final Codec<WrittenBookContent> CODEC = RecordCodecBuilder.create(i -> i.group((App)Filterable.codec(Codec.string((int)0, (int)32)).fieldOf("title").forGetter(WrittenBookContent::title), (App)Codec.STRING.fieldOf("author").forGetter(WrittenBookContent::author), (App)ExtraCodecs.intRange(0, 3).optionalFieldOf("generation", (Object)0).forGetter(WrittenBookContent::generation), (App)PAGES_CODEC.optionalFieldOf("pages", List.of()).forGetter(WrittenBookContent::pages), (App)Codec.BOOL.optionalFieldOf("resolved", (Object)false).forGetter(WrittenBookContent::resolved)).apply((Applicative)i, WrittenBookContent::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WrittenBookContent> STREAM_CODEC = StreamCodec.composite(Filterable.streamCodec(ByteBufCodecs.stringUtf8(32)), WrittenBookContent::title, ByteBufCodecs.STRING_UTF8, WrittenBookContent::author, ByteBufCodecs.VAR_INT, WrittenBookContent::generation, Filterable.streamCodec(ComponentSerialization.STREAM_CODEC).apply(ByteBufCodecs.list()), WrittenBookContent::pages, ByteBufCodecs.BOOL, WrittenBookContent::resolved, WrittenBookContent::new);

    public WrittenBookContent {
        if (generation < 0 || generation > 3) {
            throw new IllegalArgumentException("Generation was " + generation + ", but must be between 0 and 3");
        }
    }

    private static Codec<Filterable<Component>> pageCodec(Codec<Component> contentCodec) {
        return Filterable.codec(contentCodec);
    }

    public static Codec<List<Filterable<Component>>> pagesCodec(Codec<Component> contentCodec) {
        return WrittenBookContent.pageCodec(contentCodec).listOf();
    }

    public WrittenBookContent craftCopy() {
        return new WrittenBookContent(this.title, this.author, this.generation + 1, this.pages, this.resolved);
    }

    public static boolean resolveForItem(ItemStack itemStack, ResolutionContext context, HolderLookup.Provider registries) {
        WrittenBookContent content = itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content != null && !content.resolved()) {
            WrittenBookContent resolvedContent = content.resolve(context, registries);
            if (resolvedContent != null) {
                itemStack.set(DataComponents.WRITTEN_BOOK_CONTENT, resolvedContent);
                return true;
            }
            itemStack.set(DataComponents.WRITTEN_BOOK_CONTENT, content.markResolved());
        }
        return false;
    }

    public @Nullable WrittenBookContent resolve(ResolutionContext context, HolderLookup.Provider registries) {
        if (this.resolved) {
            return null;
        }
        ImmutableList.Builder newPages = ImmutableList.builderWithExpectedSize((int)this.pages.size());
        for (Filterable<Component> page : this.pages) {
            Optional<Filterable<Component>> resolvedPage = WrittenBookContent.resolvePage(context, registries, page);
            if (resolvedPage.isEmpty()) {
                return null;
            }
            newPages.add(resolvedPage.get());
        }
        return new WrittenBookContent(this.title, this.author, this.generation, (List<Filterable<Component>>)newPages.build(), true);
    }

    public WrittenBookContent markResolved() {
        return new WrittenBookContent(this.title, this.author, this.generation, this.pages, true);
    }

    private static Optional<Filterable<Component>> resolvePage(ResolutionContext context, HolderLookup.Provider registries, Filterable<Component> page) {
        return page.resolve(component -> {
            try {
                MutableComponent newComponent = ComponentUtils.resolve(context, component);
                if (WrittenBookContent.isPageTooLarge(newComponent, registries)) {
                    return Optional.empty();
                }
                return Optional.of(newComponent);
            }
            catch (Exception ignored) {
                return Optional.of(component);
            }
        });
    }

    private static boolean isPageTooLarge(Component page, HolderLookup.Provider registries) {
        DataResult json = ComponentSerialization.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), (Object)page);
        return json.isSuccess() && GsonHelper.encodesLongerThan((JsonElement)json.getOrThrow(), Short.MAX_VALUE);
    }

    public List<Component> getPages(boolean filterEnabled) {
        return Lists.transform(this.pages, page -> (Component)page.get(filterEnabled));
    }

    @Override
    public WrittenBookContent withReplacedPages(List<Filterable<Component>> newPages) {
        return new WrittenBookContent(this.title, this.author, this.generation, newPages, false);
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        if (!StringUtil.isBlank(this.author)) {
            consumer.accept(Component.translatable("book.byAuthor", this.author).withStyle(ChatFormatting.GRAY));
        }
        consumer.accept(Component.translatable("book.generation." + this.generation).withStyle(ChatFormatting.GRAY));
    }
}

