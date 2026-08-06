/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.Message
 *  com.mojang.datafixers.util.Either
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.datafixers.util.Either;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.data.DataSource;
import net.minecraft.network.chat.contents.objects.ObjectInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CompilableString;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public interface Component
extends Message,
FormattedText {
    public Style getStyle();

    public ComponentContents getContents();

    @Override
    default public String getString() {
        return FormattedText.super.getString();
    }

    default public String getString(int limit) {
        StringBuilder builder = new StringBuilder();
        this.visit(contents -> {
            int remaining = limit - builder.length();
            if (remaining <= 0) {
                return STOP_ITERATION;
            }
            builder.append(contents.length() <= remaining ? contents : contents.substring(0, remaining));
            return Optional.empty();
        });
        return builder.toString();
    }

    public List<Component> getSiblings();

    default public @Nullable String tryCollapseToString() {
        ComponentContents componentContents = this.getContents();
        if (componentContents instanceof PlainTextContents) {
            PlainTextContents text = (PlainTextContents)componentContents;
            if (this.getSiblings().isEmpty() && this.getStyle().isEmpty()) {
                return text.text();
            }
        }
        return null;
    }

    default public MutableComponent plainCopy() {
        return MutableComponent.create(this.getContents());
    }

    default public MutableComponent copy() {
        return new MutableComponent(this.getContents(), new ArrayList<Component>(this.getSiblings()), this.getStyle());
    }

    public FormattedCharSequence getVisualOrderText();

    @Override
    default public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> output, Style parentStyle) {
        Style selfStyle = this.getStyle().applyTo(parentStyle);
        Optional<T> selfResult = this.getContents().visit(output, selfStyle);
        if (selfResult.isPresent()) {
            return selfResult;
        }
        for (Component sibling : this.getSiblings()) {
            Optional<T> result = sibling.visit(output, selfStyle);
            if (!result.isPresent()) continue;
            return result;
        }
        return Optional.empty();
    }

    @Override
    default public <T> Optional<T> visit(FormattedText.ContentConsumer<T> output) {
        Optional<T> selfResult = this.getContents().visit(output);
        if (selfResult.isPresent()) {
            return selfResult;
        }
        for (Component sibling : this.getSiblings()) {
            Optional<T> result = sibling.visit(output);
            if (!result.isPresent()) continue;
            return result;
        }
        return Optional.empty();
    }

    default public List<Component> toFlatList() {
        return this.toFlatList(Style.EMPTY);
    }

    default public List<Component> toFlatList(Style rootStyle) {
        ArrayList result = Lists.newArrayList();
        this.visit((style, contents) -> {
            if (!contents.isEmpty()) {
                result.add(Component.literal(contents).withStyle(style));
            }
            return Optional.empty();
        }, rootStyle);
        return result;
    }

    default public boolean contains(Component other) {
        List<Component> otherFlat;
        if (this.equals(other)) {
            return true;
        }
        List<Component> flat = this.toFlatList();
        return Collections.indexOfSubList(flat, otherFlat = other.toFlatList(this.getStyle())) != -1;
    }

    public static Component nullToEmpty(@Nullable String text) {
        return text != null ? Component.literal(text) : CommonComponents.EMPTY;
    }

    public static MutableComponent literal(String text) {
        return MutableComponent.create(PlainTextContents.create(text));
    }

    public static MutableComponent translatable(String key) {
        return MutableComponent.create(new TranslatableContents(key, null, TranslatableContents.NO_ARGS));
    }

    public static MutableComponent translatable(String key, Object ... args) {
        return MutableComponent.create(new TranslatableContents(key, null, args));
    }

    public static MutableComponent translatableEscape(String key, Object ... args) {
        for (int i = 0; i < args.length; ++i) {
            Object arg = args[i];
            if (TranslatableContents.isAllowedPrimitiveArgument(arg) || arg instanceof Component) continue;
            args[i] = String.valueOf(arg);
        }
        return Component.translatable(key, args);
    }

    public static MutableComponent translatableWithFallback(String key, @Nullable String fallback) {
        return MutableComponent.create(new TranslatableContents(key, fallback, TranslatableContents.NO_ARGS));
    }

    public static MutableComponent translatableWithFallback(String key, @Nullable String fallback, Object ... args) {
        return MutableComponent.create(new TranslatableContents(key, fallback, args));
    }

    public static MutableComponent empty() {
        return MutableComponent.create(PlainTextContents.EMPTY);
    }

    public static MutableComponent keybind(String name) {
        return MutableComponent.create(new KeybindContents(name));
    }

    public static MutableComponent nbt(CompilableString<NbtPathArgument.NbtPath> nbtPath, boolean interpreting, boolean plain, Optional<Component> separator, DataSource dataSource) {
        return MutableComponent.create(new NbtContents(nbtPath, interpreting, plain, separator, dataSource));
    }

    public static MutableComponent score(CompilableString<EntitySelector> pattern, String objective) {
        return MutableComponent.create(new ScoreContents((Either<CompilableString<EntitySelector>, String>)Either.left(pattern), objective));
    }

    public static MutableComponent score(String name, String objective) {
        return MutableComponent.create(new ScoreContents((Either<CompilableString<EntitySelector>, String>)Either.right((Object)name), objective));
    }

    public static MutableComponent selector(CompilableString<EntitySelector> pattern, Optional<Component> separator) {
        return MutableComponent.create(new SelectorContents(pattern, separator));
    }

    public static MutableComponent object(ObjectInfo info) {
        return MutableComponent.create(new ObjectContents(info, Optional.empty()));
    }

    public static MutableComponent object(ObjectInfo info, Component fallback) {
        return MutableComponent.create(new ObjectContents(info, Optional.of(fallback)));
    }

    public static Component translationArg(Date date) {
        return Component.literal(date.toString());
    }

    public static Component translationArg(Message message) {
        Component component;
        if (message instanceof Component) {
            Component component2 = (Component)message;
            component = component2;
        } else {
            component = Component.literal(message.getString());
        }
        return component;
    }

    public static Component translationArg(UUID uuid) {
        return Component.literal(uuid.toString());
    }

    public static Component translationArg(Identifier id) {
        return Component.literal(id.toString());
    }

    public static Component translationArg(ChunkPos chunkPos) {
        return Component.literal(chunkPos.toString());
    }

    public static Component translationArg(URI uri) {
        return Component.literal(uri.toString());
    }
}

