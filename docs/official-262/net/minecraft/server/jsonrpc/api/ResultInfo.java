/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.jsonrpc.api;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.jsonrpc.api.Schema;

public record ResultInfo<Result>(String name, Schema<Result> schema) {
    public static <Result> Codec<ResultInfo<Result>> typedCodec() {
        return RecordCodecBuilder.create(i -> i.group((App)Codec.STRING.fieldOf("name").forGetter(ResultInfo::name), (App)Schema.typedCodec().fieldOf("schema").forGetter(ResultInfo::schema)).apply((Applicative)i, ResultInfo::new));
    }
}

