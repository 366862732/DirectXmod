/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.network.chat.contents;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.contents.data.DataSource;
import net.minecraft.network.chat.contents.data.DataSources;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.CompilableString;
import org.slf4j.Logger;

public record NbtContents(CompilableString<NbtPathArgument.NbtPath> nbtPath, boolean interpreting, boolean plain, Optional<Component> separator, DataSource dataSource) implements ComponentContents
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<CompilableString<NbtPathArgument.NbtPath>> NBT_PATH_CODEC = CompilableString.codec(new CompilableString.CommandParserHelper<NbtPathArgument.NbtPath>(){

        @Override
        protected NbtPathArgument.NbtPath parse(StringReader reader) throws CommandSyntaxException {
            return new NbtPathArgument().parse(reader);
        }

        @Override
        protected String errorMessage(String original, CommandSyntaxException exception) {
            return "Invalid NBT path: " + original + ": " + exception.getMessage();
        }
    });
    public static final MapCodec<NbtContents> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)NBT_PATH_CODEC.fieldOf("nbt").forGetter(NbtContents::nbtPath), (App)Codec.BOOL.lenientOptionalFieldOf("interpret", (Object)false).forGetter(NbtContents::interpreting), (App)Codec.BOOL.lenientOptionalFieldOf("plain", (Object)false).forGetter(NbtContents::plain), (App)ComponentSerialization.CODEC.lenientOptionalFieldOf("separator").forGetter(NbtContents::separator), (App)DataSources.CODEC.forGetter(NbtContents::dataSource)).apply((Applicative)i, NbtContents::new)).validate(v -> {
        if (v.interpreting && v.plain) {
            return DataResult.error(() -> "'interpret' and 'plain' flags can't be both on");
        }
        return DataResult.success((Object)v);
    });

    @Override
    public MutableComponent resolve(ResolutionContext context, int recursionDepth) throws CommandSyntaxException {
        CommandSourceStack source = context.source();
        if (source == null) {
            return Component.empty();
        }
        Stream<MutableComponent> elements = this.dataSource.getData(source).flatMap(t -> {
            try {
                return this.nbtPath.compiled().get((Tag)t).stream();
            }
            catch (CommandSyntaxException ignored) {
                return Stream.empty();
            }
        });
        Component resolvedSeparator = (Component)DataFixUtils.orElse(ComponentUtils.resolve(context, this.separator, recursionDepth), (Object)ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR);
        if (this.interpreting) {
            RegistryOps<Tag> registryOps = source.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            return elements.flatMap(tag -> {
                try {
                    Component component = (Component)ComponentSerialization.CODEC.parse((DynamicOps)registryOps, tag).getOrThrow();
                    return Stream.of(ComponentUtils.resolve(context, component, recursionDepth));
                }
                catch (Exception e) {
                    LOGGER.warn("Failed to parse component: {}", tag, (Object)e);
                    return Stream.of(new MutableComponent[0]);
                }
            }).reduce((left, right) -> left.append(resolvedSeparator).append((Component)right)).orElseGet(Component::empty);
        }
        return elements.map(tag -> {
            TextComponentTagVisitor visitor = new TextComponentTagVisitor("", this.plain ? TextComponentTagVisitor.PlainStyling.INSTANCE : TextComponentTagVisitor.RichStyling.INSTANCE);
            return (MutableComponent)visitor.visit((Tag)tag);
        }).reduce((left, right) -> left.append(resolvedSeparator).append((Component)right)).orElseGet(Component::empty);
    }

    public MapCodec<NbtContents> codec() {
        return MAP_CODEC;
    }
}

