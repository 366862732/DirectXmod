/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.network.chat.contents;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.ObjectInfo;
import net.minecraft.network.chat.contents.objects.ObjectInfos;

public record ObjectContents(ObjectInfo contents, Optional<Component> fallback) implements ComponentContents
{
    private static final String PLACEHOLDER = Character.toString('\ufffc');
    public static final MapCodec<ObjectContents> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ObjectInfos.CODEC.forGetter(ObjectContents::contents), (App)ComponentSerialization.CODEC.optionalFieldOf("fallback").forGetter(ObjectContents::fallback)).apply((Applicative)i, ObjectContents::new));

    public MapCodec<ObjectContents> codec() {
        return MAP_CODEC;
    }

    @Override
    public MutableComponent resolve(ResolutionContext context, int recursionDepth) throws CommandSyntaxException {
        Optional<MutableComponent> fallback = ComponentUtils.resolve(context, this.fallback, recursionDepth);
        ObjectInfo validatedContents = context.validate(this.contents);
        if (validatedContents == null) {
            return fallback.orElseGet(() -> Component.literal(this.contents.defaultFallback()));
        }
        return MutableComponent.create(new ObjectContents(validatedContents, fallback.map(o -> o)));
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> output) {
        if (this.fallback.isPresent()) {
            return this.fallback.get().visit(output);
        }
        return output.accept(this.contents.defaultFallback());
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> output, Style currentStyle) {
        return output.accept(currentStyle.withFont(this.contents.fontDescription()), PLACEHOLDER);
    }
}

