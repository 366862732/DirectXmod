/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 */
package net.minecraft.util.parsing.packrat;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;

public interface DelayedException<T extends Exception> {
    public T create(String var1, int var2);

    public static DelayedException<CommandSyntaxException> create(SimpleCommandExceptionType type) {
        return (contents, position) -> type.createWithContext((ImmutableStringReader)StringReaderTerms.createReader(contents, position));
    }

    public static DelayedException<CommandSyntaxException> create(DynamicCommandExceptionType type, String argument) {
        return (contents, position) -> type.createWithContext((ImmutableStringReader)StringReaderTerms.createReader(contents, position), (Object)argument);
    }
}

