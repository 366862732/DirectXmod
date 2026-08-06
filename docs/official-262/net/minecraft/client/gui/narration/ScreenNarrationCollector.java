/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.narration;

import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationThunk;

@Environment(value=EnvType.CLIENT)
public class ScreenNarrationCollector {
    private int generation;
    private final Map<EntryKey, NarrationEntry> entries = Maps.newTreeMap(Comparator.comparing(e -> e.type).thenComparing(e -> e.depth));

    public void update(Consumer<NarrationElementOutput> updater) {
        ++this.generation;
        updater.accept(new Output(this, 0));
    }

    public String collectNarrationText(boolean force) {
        final StringBuilder result = new StringBuilder();
        Consumer<String> appender = new Consumer<String>(this){
            private boolean firstEntry;
            {
                Objects.requireNonNull(this$0);
                this.firstEntry = true;
            }

            @Override
            public void accept(String s) {
                if (!this.firstEntry) {
                    result.append(". ");
                }
                this.firstEntry = false;
                result.append(s);
            }
        };
        this.entries.forEach((k, v) -> {
            if (v.generation == this.generation && (force || !v.alreadyNarrated)) {
                v.contents.getText(appender);
                v.alreadyNarrated = true;
            }
        });
        return result.toString();
    }

    @Environment(value=EnvType.CLIENT)
    private class Output
    implements NarrationElementOutput {
        private final int depth;
        final /* synthetic */ ScreenNarrationCollector this$0;

        private Output(ScreenNarrationCollector screenNarrationCollector, int depth) {
            ScreenNarrationCollector screenNarrationCollector2 = screenNarrationCollector;
            Objects.requireNonNull(screenNarrationCollector2);
            this.this$0 = screenNarrationCollector2;
            this.depth = depth;
        }

        @Override
        public void add(NarratedElementType type, NarrationThunk<?> contents) {
            this.this$0.entries.computeIfAbsent(new EntryKey(type, this.depth), k -> new NarrationEntry()).update(this.this$0.generation, contents);
        }

        @Override
        public NarrationElementOutput nest() {
            return new Output(this.this$0, this.depth + 1);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class NarrationEntry {
        private NarrationThunk<?> contents = NarrationThunk.EMPTY;
        private int generation = -1;
        private boolean alreadyNarrated;

        private NarrationEntry() {
        }

        public NarrationEntry update(int generation, NarrationThunk<?> contents) {
            if (!this.contents.equals(contents)) {
                this.contents = contents;
                this.alreadyNarrated = false;
            } else if (this.generation + 1 != generation) {
                this.alreadyNarrated = false;
            }
            this.generation = generation;
            return this;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record EntryKey(NarratedElementType type, int depth) {
    }
}

