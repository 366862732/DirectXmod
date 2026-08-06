/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Util;
import net.minecraft.util.parsing.packrat.ErrorEntry;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;
import org.jspecify.annotations.Nullable;

public interface ErrorCollector<S> {
    public void store(int var1, SuggestionSupplier<S> var2, Object var3);

    default public void store(int cursor, Object reason) {
        this.store(cursor, SuggestionSupplier.empty(), reason);
    }

    public void finish(int var1);

    public static class LongestOnly<S>
    implements ErrorCollector<S> {
        private @Nullable MutableErrorEntry<S>[] entries = new MutableErrorEntry[16];
        private int nextErrorEntry;
        private int lastCursor = -1;

        private void discardErrorsFromShorterParse(int cursor) {
            if (cursor > this.lastCursor) {
                this.lastCursor = cursor;
                this.nextErrorEntry = 0;
            }
        }

        @Override
        public void finish(int finalCursor) {
            this.discardErrorsFromShorterParse(finalCursor);
        }

        @Override
        public void store(int cursor, SuggestionSupplier<S> suggestions, Object reason) {
            this.discardErrorsFromShorterParse(cursor);
            if (cursor == this.lastCursor) {
                this.addErrorEntry(suggestions, reason);
            }
        }

        private void addErrorEntry(SuggestionSupplier<S> suggestions, Object reason) {
            int entryIndex;
            MutableErrorEntry<S> entry;
            int currentSize = this.entries.length;
            if (this.nextErrorEntry >= currentSize) {
                int newSize = Util.growByHalf(currentSize, this.nextErrorEntry + 1);
                MutableErrorEntry[] newEntries = new MutableErrorEntry[newSize];
                System.arraycopy(this.entries, 0, newEntries, 0, currentSize);
                this.entries = newEntries;
            }
            if ((entry = this.entries[entryIndex = this.nextErrorEntry++]) == null) {
                this.entries[entryIndex] = entry = new MutableErrorEntry();
            }
            entry.suggestions = suggestions;
            entry.reason = reason;
        }

        public List<ErrorEntry<S>> entries() {
            int errorCount = this.nextErrorEntry;
            if (errorCount == 0) {
                return List.of();
            }
            ArrayList<ErrorEntry<S>> result = new ArrayList<ErrorEntry<S>>(errorCount);
            for (int i = 0; i < errorCount; ++i) {
                MutableErrorEntry<S> entry = this.entries[i];
                result.add(new ErrorEntry(this.lastCursor, entry.suggestions, entry.reason));
            }
            return result;
        }

        public int cursor() {
            return this.lastCursor;
        }

        private static class MutableErrorEntry<S> {
            private SuggestionSupplier<S> suggestions = SuggestionSupplier.empty();
            private Object reason = "empty";

            private MutableErrorEntry() {
            }
        }
    }

    public static class Nop<S>
    implements ErrorCollector<S> {
        @Override
        public void store(int cursor, SuggestionSupplier<S> suggestions, Object reason) {
        }

        @Override
        public void finish(int finalCursor) {
        }
    }
}

