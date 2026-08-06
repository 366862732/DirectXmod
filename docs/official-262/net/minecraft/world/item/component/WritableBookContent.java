/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.item.component;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.BookContent;

public record WritableBookContent(List<Filterable<String>> pages) implements BookContent<String, WritableBookContent>
{
    public static final WritableBookContent EMPTY = new WritableBookContent(List.of());
    public static final int PAGE_EDIT_LENGTH = 1024;
    public static final int MAX_PAGES = 100;
    private static final Codec<Filterable<String>> PAGE_CODEC = Filterable.codec(Codec.string((int)0, (int)1024));
    public static final Codec<List<Filterable<String>>> PAGES_CODEC = PAGE_CODEC.sizeLimitedListOf(100);
    public static final Codec<WritableBookContent> CODEC = RecordCodecBuilder.create(i -> i.group((App)PAGES_CODEC.optionalFieldOf("pages", List.of()).forGetter(WritableBookContent::pages)).apply((Applicative)i, WritableBookContent::new));
    public static final StreamCodec<ByteBuf, WritableBookContent> STREAM_CODEC = Filterable.streamCodec(ByteBufCodecs.stringUtf8(1024)).apply(ByteBufCodecs.list(100)).map(WritableBookContent::new, WritableBookContent::pages);

    public WritableBookContent {
        if (pages.size() > 100) {
            throw new IllegalArgumentException("Got " + pages.size() + " pages, but maximum is 100");
        }
    }

    public Stream<String> getPages(boolean filterEnabled) {
        return this.pages.stream().map(page -> (String)page.get(filterEnabled));
    }

    @Override
    public WritableBookContent withReplacedPages(List<Filterable<String>> newPages) {
        return new WritableBookContent(newPages);
    }
}

