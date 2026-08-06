/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 */
package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Unit;

public interface FormattedText {
    public static final Optional<Unit> STOP_ITERATION = Optional.of(Unit.INSTANCE);
    public static final FormattedText EMPTY = new FormattedText(){

        @Override
        public <T> Optional<T> visit(ContentConsumer<T> output) {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> visit(StyledContentConsumer<T> output, Style parentStyle) {
            return Optional.empty();
        }
    };

    public <T> Optional<T> visit(ContentConsumer<T> var1);

    public <T> Optional<T> visit(StyledContentConsumer<T> var1, Style var2);

    public static FormattedText of(final String text) {
        return new FormattedText(){

            @Override
            public <T> Optional<T> visit(ContentConsumer<T> output) {
                return output.accept(text);
            }

            @Override
            public <T> Optional<T> visit(StyledContentConsumer<T> output, Style parentStyle) {
                return output.accept(parentStyle, text);
            }
        };
    }

    public static FormattedText of(final String text, final Style style) {
        return new FormattedText(){

            @Override
            public <T> Optional<T> visit(ContentConsumer<T> output) {
                return output.accept(text);
            }

            @Override
            public <T> Optional<T> visit(StyledContentConsumer<T> output, Style parentStyle) {
                return output.accept(style.applyTo(parentStyle), text);
            }
        };
    }

    public static FormattedText composite(FormattedText ... parts) {
        return FormattedText.composite((List<? extends FormattedText>)ImmutableList.copyOf((Object[])parts));
    }

    public static FormattedText composite(final List<? extends FormattedText> parts) {
        return new FormattedText(){

            @Override
            public <T> Optional<T> visit(ContentConsumer<T> output) {
                for (FormattedText part : parts) {
                    Optional<T> result = part.visit(output);
                    if (!result.isPresent()) continue;
                    return result;
                }
                return Optional.empty();
            }

            @Override
            public <T> Optional<T> visit(StyledContentConsumer<T> output, Style parentStyle) {
                for (FormattedText part : parts) {
                    Optional<T> result = part.visit(output, parentStyle);
                    if (!result.isPresent()) continue;
                    return result;
                }
                return Optional.empty();
            }
        };
    }

    default public String getString() {
        StringBuilder builder = new StringBuilder();
        this.visit(contents -> {
            builder.append(contents);
            return Optional.empty();
        });
        return builder.toString();
    }

    public static interface ContentConsumer<T> {
        public Optional<T> accept(String var1);
    }

    public static interface StyledContentConsumer<T> {
        public Optional<T> accept(Style var1, String var2);
    }
}

