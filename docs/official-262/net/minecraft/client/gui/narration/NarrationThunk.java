/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.narration;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;

@Environment(value=EnvType.CLIENT)
public class NarrationThunk<T> {
    private final T contents;
    private final BiConsumer<Consumer<String>, T> converter;
    public static final NarrationThunk<?> EMPTY = new NarrationThunk<Unit>(Unit.INSTANCE, (o, c) -> {});

    private NarrationThunk(T contents, BiConsumer<Consumer<String>, T> converter) {
        this.contents = contents;
        this.converter = converter;
    }

    public static NarrationThunk<?> from(String text) {
        return new NarrationThunk<String>(text, Consumer::accept);
    }

    public static NarrationThunk<?> from(Component text) {
        return new NarrationThunk<Component>(text, (o, c) -> o.accept(c.getString()));
    }

    public static NarrationThunk<?> from(List<Component> lines) {
        return new NarrationThunk<List>(lines, (o, c) -> lines.stream().map(Component::getString).forEach((Consumer<String>)o));
    }

    public void getText(Consumer<String> output) {
        this.converter.accept(output, (Consumer<String>)this.contents);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof NarrationThunk) {
            NarrationThunk thunk = (NarrationThunk)o;
            return thunk.converter == this.converter && thunk.contents.equals(this.contents);
        }
        return false;
    }

    public int hashCode() {
        int result = this.contents.hashCode();
        result = 31 * result + this.converter.hashCode();
        return result;
    }
}

