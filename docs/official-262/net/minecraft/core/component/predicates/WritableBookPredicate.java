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
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WritableBookContent;

public record WritableBookPredicate(Optional<CollectionPredicate<Filterable<String>, PagePredicate>> pages) implements SingleComponentItemPredicate<WritableBookContent>
{
    public static final Codec<WritableBookPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)CollectionPredicate.codec(PagePredicate.CODEC).optionalFieldOf("pages").forGetter(WritableBookPredicate::pages)).apply((Applicative)i, WritableBookPredicate::new));

    @Override
    public DataComponentType<WritableBookContent> componentType() {
        return DataComponents.WRITABLE_BOOK_CONTENT;
    }

    @Override
    public boolean matches(WritableBookContent value) {
        return !this.pages.isPresent() || this.pages.get().test(value.pages());
    }

    public record PagePredicate(String contents) implements Predicate<Filterable<String>>
    {
        public static final Codec<PagePredicate> CODEC = Codec.STRING.xmap(PagePredicate::new, PagePredicate::contents);

        @Override
        public boolean test(Filterable<String> value) {
            return value.raw().equals(this.contents);
        }
    }
}

