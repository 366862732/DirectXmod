/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  it.unimi.dsi.fastutil.chars.CharOpenHashSet
 *  it.unimi.dsi.fastutil.chars.CharSet
 *  org.apache.commons.lang3.ArrayUtils
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.state.pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class BlockPatternBuilder {
    private final List<String[]> pattern = Lists.newArrayList();
    private final Map<Character, Predicate<@Nullable BlockInWorld>> lookup = Maps.newHashMap();
    private int height;
    private int width;
    private final CharSet unknownCharacters = new CharOpenHashSet();

    private BlockPatternBuilder() {
        this.lookup.put(Character.valueOf(' '), blockInWorld -> true);
    }

    public BlockPatternBuilder aisle(String ... aisle) {
        if (ArrayUtils.isEmpty((Object[])aisle) || StringUtils.isEmpty((CharSequence)aisle[0])) {
            throw new IllegalArgumentException("Empty pattern for aisle");
        }
        if (this.pattern.isEmpty()) {
            this.height = aisle.length;
            this.width = aisle[0].length();
        }
        if (aisle.length != this.height) {
            throw new IllegalArgumentException("Expected aisle with height of " + this.height + ", but was given one with a height of " + aisle.length + ")");
        }
        for (String row : aisle) {
            if (row.length() != this.width) {
                throw new IllegalArgumentException("Not all rows in the given aisle are the correct width (expected " + this.width + ", found one with " + row.length() + ")");
            }
            for (char c : row.toCharArray()) {
                if (this.lookup.containsKey(Character.valueOf(c))) continue;
                this.unknownCharacters.add(c);
            }
        }
        this.pattern.add(aisle);
        return this;
    }

    public static BlockPatternBuilder start() {
        return new BlockPatternBuilder();
    }

    public BlockPatternBuilder where(char character, Predicate<@Nullable BlockInWorld> predicate) {
        this.lookup.put(Character.valueOf(character), predicate);
        this.unknownCharacters.remove(character);
        return this;
    }

    public BlockPattern build() {
        return new BlockPattern(this.createPattern());
    }

    private Predicate<BlockInWorld>[][][] createPattern() {
        if (!this.unknownCharacters.isEmpty()) {
            throw new IllegalStateException("Predicates for character(s) " + String.valueOf(this.unknownCharacters) + " are missing");
        }
        Predicate[][][] result = (Predicate[][][])Array.newInstance(Predicate.class, this.pattern.size(), this.height, this.width);
        for (int aisle = 0; aisle < this.pattern.size(); ++aisle) {
            for (int row = 0; row < this.height; ++row) {
                for (int col = 0; col < this.width; ++col) {
                    result[aisle][row][col] = this.lookup.get(Character.valueOf(this.pattern.get(aisle)[row].charAt(col)));
                }
            }
        }
        return result;
    }
}

