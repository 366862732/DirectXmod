/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat.contents;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableFormatException;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

public class TranslatableContents
implements ComponentContents {
    public static final Object[] NO_ARGS = new Object[0];
    private static final Codec<Object> PRIMITIVE_ARG_CODEC = ExtraCodecs.JAVA.validate(TranslatableContents::filterAllowedArguments);
    private static final Codec<Object> ARG_CODEC = Codec.either(PRIMITIVE_ARG_CODEC, ComponentSerialization.CODEC).xmap(e -> e.map(o -> o, component -> Objects.requireNonNullElse(component.tryCollapseToString(), component)), o -> {
        Either either;
        if (o instanceof Component) {
            Component c = (Component)o;
            either = Either.right((Object)c);
        } else {
            either = Either.left((Object)o);
        }
        return either;
    });
    public static final MapCodec<TranslatableContents> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.STRING.fieldOf("translate").forGetter(o -> o.key), (App)Codec.STRING.lenientOptionalFieldOf("fallback").forGetter(o -> Optional.ofNullable(o.fallback)), (App)ARG_CODEC.listOf().optionalFieldOf("with").forGetter(o -> TranslatableContents.adjustArgs(o.args))).apply((Applicative)i, TranslatableContents::create));
    private static final FormattedText TEXT_PERCENT = FormattedText.of("%");
    private static final FormattedText TEXT_NULL = FormattedText.of("null");
    private final String key;
    private final @Nullable String fallback;
    private final Object[] args;
    private @Nullable Language decomposedWith;
    private List<FormattedText> decomposedParts = ImmutableList.of();
    private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    private static DataResult<Object> filterAllowedArguments(@Nullable Object result) {
        if (!TranslatableContents.isAllowedPrimitiveArgument(result)) {
            return DataResult.error(() -> "This value needs to be parsed as component");
        }
        return DataResult.success((Object)result);
    }

    public static boolean isAllowedPrimitiveArgument(@Nullable Object object) {
        return object instanceof Number || object instanceof Boolean || object instanceof String;
    }

    private static Optional<List<Object>> adjustArgs(Object[] args) {
        return args.length == 0 ? Optional.empty() : Optional.of(Arrays.asList(args));
    }

    private static Object[] adjustArgs(Optional<List<Object>> args) {
        return args.map(a -> a.isEmpty() ? NO_ARGS : a.toArray()).orElse(NO_ARGS);
    }

    private static TranslatableContents create(String key, Optional<String> fallback, Optional<List<Object>> args) {
        return new TranslatableContents(key, fallback.orElse(null), TranslatableContents.adjustArgs(args));
    }

    public TranslatableContents(String key, @Nullable String fallback, Object[] args) {
        this.key = key;
        this.fallback = fallback;
        this.args = args;
    }

    public MapCodec<TranslatableContents> codec() {
        return MAP_CODEC;
    }

    private void decompose() {
        Language currentLanguage = Language.getInstance();
        if (currentLanguage == this.decomposedWith) {
            return;
        }
        this.decomposedWith = currentLanguage;
        String format = this.fallback != null ? currentLanguage.getOrDefault(this.key, this.fallback) : currentLanguage.getOrDefault(this.key);
        try {
            ImmutableList.Builder parts = ImmutableList.builder();
            this.decomposeTemplate(format, arg_0 -> ((ImmutableList.Builder)parts).add(arg_0));
            this.decomposedParts = parts.build();
        }
        catch (TranslatableFormatException e) {
            this.decomposedParts = ImmutableList.of((Object)FormattedText.of(format));
        }
    }

    private void decomposeTemplate(String template, Consumer<FormattedText> decomposedParts) {
        Matcher matcher = FORMAT_PATTERN.matcher(template);
        try {
            int replacementIndex = 0;
            int current = 0;
            while (matcher.find(current)) {
                int start = matcher.start();
                int end = matcher.end();
                if (start > current) {
                    String prefix = template.substring(current, start);
                    if (prefix.indexOf(37) != -1) {
                        throw new IllegalArgumentException();
                    }
                    decomposedParts.accept(FormattedText.of(prefix));
                }
                String formatType = matcher.group(2);
                String formatString = template.substring(start, end);
                if ("%".equals(formatType) && "%%".equals(formatString)) {
                    decomposedParts.accept(TEXT_PERCENT);
                } else if ("s".equals(formatType)) {
                    String possiblePositionIndex = matcher.group(1);
                    int index = possiblePositionIndex != null ? Integer.parseInt(possiblePositionIndex) - 1 : replacementIndex++;
                    decomposedParts.accept(this.getArgument(index));
                } else {
                    throw new TranslatableFormatException(this, "Unsupported format: '" + formatString + "'");
                }
                current = end;
            }
            if (current < template.length()) {
                String tail = template.substring(current);
                if (tail.indexOf(37) != -1) {
                    throw new IllegalArgumentException();
                }
                decomposedParts.accept(FormattedText.of(tail));
            }
        }
        catch (IllegalArgumentException e) {
            throw new TranslatableFormatException(this, (Throwable)e);
        }
    }

    private FormattedText getArgument(int index) {
        if (index < 0 || index >= this.args.length) {
            throw new TranslatableFormatException(this, index);
        }
        Object arg = this.args[index];
        if (arg instanceof Component) {
            Component componentArg = (Component)arg;
            return componentArg;
        }
        return arg == null ? TEXT_NULL : FormattedText.of(arg.toString());
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> output, Style currentStyle) {
        this.decompose();
        for (FormattedText part : this.decomposedParts) {
            Optional<T> result = part.visit(output, currentStyle);
            if (!result.isPresent()) continue;
            return result;
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> output) {
        this.decompose();
        for (FormattedText part : this.decomposedParts) {
            Optional<T> result = part.visit(output);
            if (!result.isPresent()) continue;
            return result;
        }
        return Optional.empty();
    }

    @Override
    public MutableComponent resolve(ResolutionContext context, int recursionDepth) throws CommandSyntaxException {
        Object[] argsCopy = new Object[this.args.length];
        for (int i = 0; i < argsCopy.length; ++i) {
            Object param = this.args[i];
            if (param instanceof Component) {
                Component component = (Component)param;
                argsCopy[i] = ComponentUtils.resolve(context, component, recursionDepth);
                continue;
            }
            argsCopy[i] = param;
        }
        return MutableComponent.create(new TranslatableContents(this.key, this.fallback, argsCopy));
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TranslatableContents)) return false;
        TranslatableContents that = (TranslatableContents)o;
        if (!Objects.equals(this.key, that.key)) return false;
        if (!Objects.equals(this.fallback, that.fallback)) return false;
        if (!Arrays.equals(this.args, that.args)) return false;
        return true;
    }

    public int hashCode() {
        int result = Objects.hashCode(this.key);
        result = 31 * result + Objects.hashCode(this.fallback);
        result = 31 * result + Arrays.hashCode(this.args);
        return result;
    }

    public String toString() {
        return "translation{key='" + this.key + "'" + (String)(this.fallback != null ? ", fallback='" + this.fallback + "'" : "") + ", args=" + Arrays.toString(this.args) + "}";
    }

    public String getKey() {
        return this.key;
    }

    public @Nullable String getFallback() {
        return this.fallback;
    }

    public Object[] getArgs() {
        return this.args;
    }
}

