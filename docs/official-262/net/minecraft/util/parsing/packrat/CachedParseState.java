/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.parsing.packrat;

import java.util.Objects;
import net.minecraft.util.Util;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Control;
import net.minecraft.util.parsing.packrat.ErrorCollector;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import org.jspecify.annotations.Nullable;

public abstract class CachedParseState<S>
implements ParseState<S> {
    private @Nullable PositionCache[] positionCache = new PositionCache[256];
    private final ErrorCollector<S> errorCollector;
    private final Scope scope = new Scope();
    private @Nullable SimpleControl[] controlCache = new SimpleControl[16];
    private int nextControlToReturn;
    private final Silent silent = new Silent(this);

    protected CachedParseState(ErrorCollector<S> errorCollector) {
        this.errorCollector = errorCollector;
    }

    @Override
    public Scope scope() {
        return this.scope;
    }

    @Override
    public ErrorCollector<S> errorCollector() {
        return this.errorCollector;
    }

    @Override
    public <T> @Nullable T parse(NamedRule<S, T> rule) {
        CacheEntry entry;
        T result;
        int markBeforeParse = this.mark();
        PositionCache positionCache = this.getCacheForPosition(markBeforeParse);
        int entryIndex = positionCache.findKeyIndex(rule.name());
        if (entryIndex != -1) {
            CacheEntry value = positionCache.getValue(entryIndex);
            if (value != null) {
                if (value == CacheEntry.NEGATIVE) {
                    return null;
                }
                this.restore(value.markAfterParse);
                return value.value;
            }
        } else {
            entryIndex = positionCache.allocateNewEntry(rule.name());
        }
        if ((result = rule.value().parse(this)) == null) {
            entry = CacheEntry.negativeEntry();
        } else {
            int markAfterParse = this.mark();
            entry = new CacheEntry<T>(result, markAfterParse);
        }
        positionCache.setValue(entryIndex, entry);
        return result;
    }

    private PositionCache getCacheForPosition(int index) {
        PositionCache result;
        int currentSize = this.positionCache.length;
        if (index >= currentSize) {
            int newSize = Util.growByHalf(currentSize, index + 1);
            PositionCache[] newCache = new PositionCache[newSize];
            System.arraycopy(this.positionCache, 0, newCache, 0, currentSize);
            this.positionCache = newCache;
        }
        if ((result = this.positionCache[index]) == null) {
            this.positionCache[index] = result = new PositionCache();
        }
        return result;
    }

    @Override
    public Control acquireControl() {
        int controlIndex;
        SimpleControl entry;
        int currentSize = this.controlCache.length;
        if (this.nextControlToReturn >= currentSize) {
            int newSize = Util.growByHalf(currentSize, this.nextControlToReturn + 1);
            SimpleControl[] newControlCache = new SimpleControl[newSize];
            System.arraycopy(this.controlCache, 0, newControlCache, 0, currentSize);
            this.controlCache = newControlCache;
        }
        if ((entry = this.controlCache[controlIndex = this.nextControlToReturn++]) == null) {
            this.controlCache[controlIndex] = entry = new SimpleControl();
        } else {
            entry.reset();
        }
        return entry;
    }

    @Override
    public void releaseControl() {
        --this.nextControlToReturn;
    }

    @Override
    public ParseState<S> silent() {
        return this.silent;
    }

    private static class PositionCache {
        public static final int ENTRY_STRIDE = 2;
        private static final int NOT_FOUND = -1;
        private Object[] atomCache = new Object[16];
        private int nextKey;

        private PositionCache() {
        }

        public int findKeyIndex(Atom<?> key) {
            for (int i = 0; i < this.nextKey; i += 2) {
                if (this.atomCache[i] != key) continue;
                return i;
            }
            return -1;
        }

        public int allocateNewEntry(Atom<?> key) {
            int newKeyIndex = this.nextKey;
            this.nextKey += 2;
            int newValueIndex = newKeyIndex + 1;
            int currentSize = this.atomCache.length;
            if (newValueIndex >= currentSize) {
                int newSize = Util.growByHalf(currentSize, newValueIndex + 1);
                Object[] newCache = new Object[newSize];
                System.arraycopy(this.atomCache, 0, newCache, 0, currentSize);
                this.atomCache = newCache;
            }
            this.atomCache[newKeyIndex] = key;
            return newKeyIndex;
        }

        public <T> @Nullable CacheEntry<T> getValue(int keyIndex) {
            return (CacheEntry)this.atomCache[keyIndex + 1];
        }

        public void setValue(int keyIndex, CacheEntry<?> entry) {
            this.atomCache[keyIndex + 1] = entry;
        }
    }

    private static class SimpleControl
    implements Control {
        private boolean hasCut;

        private SimpleControl() {
        }

        @Override
        public void cut() {
            this.hasCut = true;
        }

        @Override
        public boolean hasCut() {
            return this.hasCut;
        }

        public void reset() {
            this.hasCut = false;
        }
    }

    private class Silent
    implements ParseState<S> {
        private final ErrorCollector<S> silentCollector;
        final /* synthetic */ CachedParseState this$0;

        private Silent(CachedParseState cachedParseState) {
            CachedParseState cachedParseState2 = cachedParseState;
            Objects.requireNonNull(cachedParseState2);
            this.this$0 = cachedParseState2;
            this.silentCollector = new ErrorCollector.Nop();
        }

        @Override
        public ErrorCollector<S> errorCollector() {
            return this.silentCollector;
        }

        @Override
        public Scope scope() {
            return this.this$0.scope();
        }

        @Override
        public <T> @Nullable T parse(NamedRule<S, T> rule) {
            return this.this$0.parse(rule);
        }

        @Override
        public S input() {
            return this.this$0.input();
        }

        @Override
        public int mark() {
            return this.this$0.mark();
        }

        @Override
        public void restore(int mark) {
            this.this$0.restore(mark);
        }

        @Override
        public Control acquireControl() {
            return this.this$0.acquireControl();
        }

        @Override
        public void releaseControl() {
            this.this$0.releaseControl();
        }

        @Override
        public ParseState<S> silent() {
            return this;
        }
    }

    private record CacheEntry<T>(@Nullable T value, int markAfterParse) {
        public static final CacheEntry<?> NEGATIVE = new CacheEntry<Object>(null, -1);

        public static <T> CacheEntry<T> negativeEntry() {
            return NEGATIVE;
        }
    }
}

