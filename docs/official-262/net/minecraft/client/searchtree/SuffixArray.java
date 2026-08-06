/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Sets
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.Arrays
 *  it.unimi.dsi.fastutil.Swapper
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntComparator
 *  it.unimi.dsi.fastutil.ints.IntList
 *  it.unimi.dsi.fastutil.ints.IntOpenHashSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.searchtree;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class SuffixArray<T> {
    private static final boolean DEBUG_COMPARISONS = Boolean.parseBoolean(System.getProperty("SuffixArray.printComparisons", "false"));
    private static final boolean DEBUG_ARRAY = Boolean.parseBoolean(System.getProperty("SuffixArray.printArray", "false"));
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int END_OF_TEXT_MARKER = -1;
    private static final int END_OF_DATA = -2;
    protected final List<T> list = Lists.newArrayList();
    private final IntList chars = new IntArrayList();
    private final IntList wordStarts = new IntArrayList();
    private IntList suffixToT = new IntArrayList();
    private IntList offsets = new IntArrayList();
    private int maxStringLength;

    public void add(T t, String text) {
        this.maxStringLength = Math.max(this.maxStringLength, text.length());
        int index = this.list.size();
        this.list.add(t);
        this.wordStarts.add(this.chars.size());
        for (int i = 0; i < text.length(); ++i) {
            this.suffixToT.add(index);
            this.offsets.add(i);
            this.chars.add((int)text.charAt(i));
        }
        this.suffixToT.add(index);
        this.offsets.add(text.length());
        this.chars.add(-1);
    }

    public void generate() {
        int charCount = this.chars.size();
        int[] positions = new int[charCount];
        int[] lefts = new int[charCount];
        int[] rights = new int[charCount];
        int[] reverse = new int[charCount];
        IntComparator comparator = (a, b) -> {
            if (lefts[a] == lefts[b]) {
                return Integer.compare(rights[a], rights[b]);
            }
            return Integer.compare(lefts[a], lefts[b]);
        };
        Swapper swapper = (a, b) -> {
            if (a != b) {
                int tmp = lefts[a];
                lefts[a] = lefts[b];
                lefts[b] = tmp;
                tmp = rights[a];
                rights[a] = rights[b];
                rights[b] = tmp;
                tmp = reverse[a];
                reverse[a] = reverse[b];
                reverse[b] = tmp;
            }
        };
        for (int i = 0; i < charCount; ++i) {
            positions[i] = this.chars.getInt(i);
        }
        int count = 1;
        int max = Math.min(charCount, this.maxStringLength);
        while (count * 2 < max) {
            int i;
            for (i = 0; i < charCount; ++i) {
                lefts[i] = positions[i];
                rights[i] = i + count < charCount ? positions[i + count] : -2;
                reverse[i] = i;
            }
            it.unimi.dsi.fastutil.Arrays.quickSort((int)0, (int)charCount, (IntComparator)comparator, (Swapper)swapper);
            for (i = 0; i < charCount; ++i) {
                positions[reverse[i]] = i > 0 && lefts[i] == lefts[i - 1] && rights[i] == rights[i - 1] ? positions[reverse[i - 1]] : i;
            }
            count *= 2;
        }
        IntList oldSuffixToT = this.suffixToT;
        IntList oldOffsets = this.offsets;
        this.suffixToT = new IntArrayList(oldSuffixToT.size());
        this.offsets = new IntArrayList(oldOffsets.size());
        for (int i = 0; i < charCount; ++i) {
            int index = reverse[i];
            this.suffixToT.add(oldSuffixToT.getInt(index));
            this.offsets.add(oldOffsets.getInt(index));
        }
        if (DEBUG_ARRAY) {
            this.print();
        }
    }

    private void print() {
        for (int i = 0; i < this.suffixToT.size(); ++i) {
            LOGGER.debug("{} {}", (Object)i, (Object)this.getString(i));
        }
        LOGGER.debug("");
    }

    private String getString(int i) {
        int start = this.offsets.getInt(i);
        int offset = this.wordStarts.getInt(this.suffixToT.getInt(i));
        StringBuilder builder = new StringBuilder();
        int j = 0;
        while (offset + j < this.chars.size()) {
            int p;
            if (j == start) {
                builder.append('^');
            }
            if ((p = this.chars.getInt(offset + j)) == -1) break;
            builder.append((char)p);
            ++j;
        }
        return builder.toString();
    }

    private int compare(String text, int index) {
        int start = this.wordStarts.getInt(this.suffixToT.getInt(index));
        int offset = this.offsets.getInt(index);
        for (int i = 0; i < text.length(); ++i) {
            char c2;
            int p = this.chars.getInt(start + offset + i);
            if (p == -1) {
                return 1;
            }
            char c = text.charAt(i);
            if (c < (c2 = (char)p)) {
                return -1;
            }
            if (c <= c2) continue;
            return 1;
        }
        return 0;
    }

    public List<T> search(String text) {
        int suffixCount = this.suffixToT.size();
        int low = 0;
        int high = suffixCount;
        while (low < high) {
            int mid = low + (high - low) / 2;
            int c = this.compare(text, mid);
            if (DEBUG_COMPARISONS) {
                LOGGER.debug("comparing lower \"{}\" with {} \"{}\": {}", new Object[]{text, mid, this.getString(mid), c});
            }
            if (c > 0) {
                low = mid + 1;
                continue;
            }
            high = mid;
        }
        if (low < 0 || low >= suffixCount) {
            return Collections.emptyList();
        }
        int lowerBound = low;
        high = suffixCount;
        while (low < high) {
            int mid = low + (high - low) / 2;
            int c = this.compare(text, mid);
            if (DEBUG_COMPARISONS) {
                LOGGER.debug("comparing upper \"{}\" with {} \"{}\": {}", new Object[]{text, mid, this.getString(mid), c});
            }
            if (c >= 0) {
                low = mid + 1;
                continue;
            }
            high = mid;
        }
        int upperBound = low;
        IntOpenHashSet matches = new IntOpenHashSet();
        for (int i = lowerBound; i < upperBound; ++i) {
            matches.add(this.suffixToT.getInt(i));
        }
        int[] ints = matches.toIntArray();
        Arrays.sort(ints);
        LinkedHashSet result = Sets.newLinkedHashSet();
        for (int t : ints) {
            result.add(this.list.get(t));
        }
        return Lists.newArrayList((Iterable)result);
    }
}

