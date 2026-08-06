/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.functions;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.MacroFunction;
import net.minecraft.commands.functions.PlainTextFunction;
import net.minecraft.commands.functions.StringTemplate;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

class FunctionBuilder<T extends ExecutionCommandSource<T>> {
    private @Nullable List<UnboundEntryAction<T>> plainEntries = new ArrayList<UnboundEntryAction<T>>();
    private @Nullable List<MacroFunction.Entry<T>> macroEntries;
    private final List<String> macroArguments = new ArrayList<String>();

    FunctionBuilder() {
    }

    public void addCommand(UnboundEntryAction<T> command) {
        if (this.macroEntries != null) {
            this.macroEntries.add(new MacroFunction.PlainTextEntry<T>(command));
        } else {
            this.plainEntries.add(command);
        }
    }

    private int getArgumentIndex(String id) {
        int index = this.macroArguments.indexOf(id);
        if (index == -1) {
            index = this.macroArguments.size();
            this.macroArguments.add(id);
        }
        return index;
    }

    private IntList convertToIndices(List<String> ids) {
        IntArrayList result = new IntArrayList(ids.size());
        for (String id : ids) {
            result.add(this.getArgumentIndex(id));
        }
        return result;
    }

    public void addMacro(String command, int line, T compilationContext) {
        StringTemplate parseResults;
        try {
            parseResults = StringTemplate.fromString(command);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Can't parse function line " + line + ": '" + command + "'", e);
        }
        if (this.plainEntries != null) {
            this.macroEntries = new ArrayList<MacroFunction.Entry<T>>(this.plainEntries.size() + 1);
            for (UnboundEntryAction<T> plainEntry : this.plainEntries) {
                this.macroEntries.add(new MacroFunction.PlainTextEntry<T>(plainEntry));
            }
            this.plainEntries = null;
        }
        this.macroEntries.add(new MacroFunction.MacroEntry<T>(parseResults, this.convertToIndices(parseResults.variables()), compilationContext));
    }

    public CommandFunction<T> build(Identifier id) {
        if (this.macroEntries != null) {
            return new MacroFunction<T>(id, this.macroEntries, this.macroArguments);
        }
        return new PlainTextFunction<T>(id, this.plainEntries);
    }
}

