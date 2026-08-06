/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.language;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;

@Environment(value=EnvType.CLIENT)
public record LanguageInfo(String region, String name, boolean bidirectional) {
    public static final Codec<LanguageInfo> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.NON_EMPTY_STRING.fieldOf("region").forGetter(LanguageInfo::region), (App)ExtraCodecs.NON_EMPTY_STRING.fieldOf("name").forGetter(LanguageInfo::name), (App)Codec.BOOL.optionalFieldOf("bidirectional", (Object)false).forGetter(LanguageInfo::bidirectional)).apply((Applicative)i, LanguageInfo::new));

    public Component toComponent() {
        return Component.literal(this.name + " (" + this.region + ")");
    }
}

