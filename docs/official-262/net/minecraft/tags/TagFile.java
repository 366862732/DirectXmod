/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.tags;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.tags.TagEntry;

public record TagFile(List<TagEntry> entries, boolean replace) {
    public static final Codec<TagFile> CODEC = RecordCodecBuilder.create(i -> i.group((App)TagEntry.CODEC.listOf().fieldOf("values").forGetter(TagFile::entries), (App)Codec.BOOL.optionalFieldOf("replace", (Object)false).forGetter(TagFile::replace)).apply((Applicative)i, TagFile::new));
}

