/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.tags;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

public class TagEntry {
    private static final Codec<TagEntry> FULL_CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.TAG_OR_ELEMENT_ID.fieldOf("id").forGetter(TagEntry::elementOrTag), (App)Codec.BOOL.optionalFieldOf("required", (Object)true).forGetter(e -> e.required)).apply((Applicative)i, TagEntry::new));
    public static final Codec<TagEntry> CODEC = Codec.either(ExtraCodecs.TAG_OR_ELEMENT_ID, FULL_CODEC).xmap(e -> (TagEntry)e.map(l -> new TagEntry((ExtraCodecs.TagOrElementLocation)l, true), r -> r), entry -> entry.required ? Either.left((Object)entry.elementOrTag()) : Either.right((Object)entry));
    private final Identifier id;
    private final boolean tag;
    private final boolean required;

    private TagEntry(Identifier id, boolean tag, boolean required) {
        this.id = id;
        this.tag = tag;
        this.required = required;
    }

    private TagEntry(ExtraCodecs.TagOrElementLocation elementOrTag, boolean required) {
        this.id = elementOrTag.id();
        this.tag = elementOrTag.tag();
        this.required = required;
    }

    private ExtraCodecs.TagOrElementLocation elementOrTag() {
        return new ExtraCodecs.TagOrElementLocation(this.id, this.tag);
    }

    public static TagEntry element(Identifier id) {
        return new TagEntry(id, false, true);
    }

    public static TagEntry optionalElement(Identifier id) {
        return new TagEntry(id, false, false);
    }

    public static TagEntry tag(Identifier id) {
        return new TagEntry(id, true, true);
    }

    public static TagEntry optionalTag(Identifier id) {
        return new TagEntry(id, true, false);
    }

    public <T> boolean build(Lookup<T> lookup, Consumer<T> output) {
        if (this.tag) {
            Collection<T> result = lookup.tag(this.id);
            if (result == null) {
                return !this.required;
            }
            result.forEach(output);
        } else {
            T result = lookup.element(this.id, this.required);
            if (result == null) {
                return !this.required;
            }
            output.accept(result);
        }
        return true;
    }

    public void visitRequiredDependencies(Consumer<Identifier> output) {
        if (this.tag && this.required) {
            output.accept(this.id);
        }
    }

    public void visitOptionalDependencies(Consumer<Identifier> output) {
        if (this.tag && !this.required) {
            output.accept(this.id);
        }
    }

    public boolean verifyIfPresent(Predicate<Identifier> elementCheck, Predicate<Identifier> tagCheck) {
        return !this.required || (this.tag ? tagCheck : elementCheck).test(this.id);
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        if (this.tag) {
            result.append('#');
        }
        result.append(this.id);
        if (!this.required) {
            result.append('?');
        }
        return result.toString();
    }

    public static interface Lookup<T> {
        public @Nullable T element(Identifier var1, boolean var2);

        public @Nullable Collection<T> tag(Identifier var1);
    }
}

