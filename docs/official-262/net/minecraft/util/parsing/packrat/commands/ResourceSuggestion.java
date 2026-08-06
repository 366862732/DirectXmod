/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.StringReader
 */
package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;

public interface ResourceSuggestion
extends SuggestionSupplier<StringReader> {
    public Stream<Identifier> possibleResources();

    @Override
    default public Stream<String> possibleValues(ParseState<StringReader> state) {
        return this.possibleResources().map(Identifier::toString);
    }
}

