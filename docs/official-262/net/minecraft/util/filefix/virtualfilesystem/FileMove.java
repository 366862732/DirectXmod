/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.util.filefix.virtualfilesystem;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Path;
import net.minecraft.util.ExtraCodecs;

public record FileMove(Path from, Path to) {
    public static Codec<FileMove> moveCodec(Path fromDirectory, Path toDirectory) {
        return RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.guardedPathCodec(fromDirectory).fieldOf("from").forGetter(r -> r.from), (App)ExtraCodecs.guardedPathCodec(toDirectory).fieldOf("to").forGetter(r -> r.to)).apply((Applicative)i, FileMove::new));
    }
}

