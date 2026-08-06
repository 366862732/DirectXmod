/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  it.unimi.dsi.fastutil.ints.IntList
 *  it.unimi.dsi.fastutil.ints.IntLists
 *  it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.functions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.lang.runtime.SwitchBootstraps;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.commands.functions.PlainTextFunction;
import net.minecraft.commands.functions.StringTemplate;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class MacroFunction<T extends ExecutionCommandSource<T>>
implements CommandFunction<T> {
    private static final DecimalFormat DECIMAL_FORMAT = Util.make(new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.ROOT)), format -> format.setMaximumFractionDigits(15));
    private static final int MAX_CACHE_ENTRIES = 8;
    private final List<String> parameters;
    private final Object2ObjectLinkedOpenHashMap<List<String>, InstantiatedFunction<T>> cache = new Object2ObjectLinkedOpenHashMap(8, 0.25f);
    private final Identifier id;
    private final List<Entry<T>> entries;

    public MacroFunction(Identifier id, List<Entry<T>> entries, List<String> parameters) {
        this.id = id;
        this.entries = entries;
        this.parameters = parameters;
    }

    @Override
    public Identifier id() {
        return this.id;
    }

    @Override
    public InstantiatedFunction<T> instantiate(@Nullable CompoundTag arguments, CommandDispatcher<T> dispatcher) throws FunctionInstantiationException {
        if (arguments == null) {
            throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_arguments", Component.translationArg(this.id())));
        }
        ArrayList<String> parameterValues = new ArrayList<String>(this.parameters.size());
        for (String argument : this.parameters) {
            Tag argumentValue = arguments.get(argument);
            if (argumentValue == null) {
                throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_argument", Component.translationArg(this.id()), argument));
            }
            parameterValues.add(MacroFunction.stringify(argumentValue));
        }
        InstantiatedFunction cachedFunction = (InstantiatedFunction)this.cache.getAndMoveToLast(parameterValues);
        if (cachedFunction != null) {
            return cachedFunction;
        }
        if (this.cache.size() >= 8) {
            this.cache.removeFirst();
        }
        InstantiatedFunction<T> function = this.substituteAndParse(this.parameters, parameterValues, dispatcher);
        this.cache.put(parameterValues, function);
        return function;
    }

    /*
     * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static String stringify(Tag tag) {
        String string;
        float f;
        Tag tag2 = tag;
        Objects.requireNonNull(tag2);
        Tag tag3 = tag2;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{FloatTag.class, DoubleTag.class, ByteTag.class, ShortTag.class, LongTag.class, StringTag.class}, (Tag)tag3, n)) {
            case 0: {
                FloatTag floatTag = (FloatTag)tag3;
                try {
                    float f2 = f = floatTag.value();
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
            }
            float value = f;
            string = DECIMAL_FORMAT.format(value);
            return string;
            case 1: {
                double d;
                DoubleTag doubleTag = (DoubleTag)tag3;
                {
                    double d2 = d = doubleTag.value();
                }
                double value2 = d;
                string = DECIMAL_FORMAT.format(value2);
                return string;
            }
            case 2: {
                byte by;
                ByteTag byteTag = (ByteTag)tag3;
                {
                    byte by2 = by = byteTag.value();
                }
                byte value3 = by;
                string = String.valueOf(value3);
                return string;
            }
            case 3: {
                short s;
                ShortTag shortTag = (ShortTag)tag3;
                {
                    short s2 = s = shortTag.value();
                }
                short value4 = s;
                string = String.valueOf(value4);
                return string;
            }
            case 4: {
                long l;
                LongTag longTag = (LongTag)tag3;
                {
                    long l2 = l = longTag.value();
                }
                long value5 = l;
                string = String.valueOf(value5);
                return string;
            }
            case 5: {
                StringTag stringTag = (StringTag)tag3;
                {
                    String string2;
                    String value6;
                    string = value6 = (string2 = stringTag.value());
                    return string;
                }
            }
        }
        string = tag.toString();
        return string;
    }

    private static void lookupValues(List<String> values, IntList indicesToSelect, List<String> selectedValuesOutput) {
        selectedValuesOutput.clear();
        indicesToSelect.forEach(index -> selectedValuesOutput.add((String)values.get(index)));
    }

    private InstantiatedFunction<T> substituteAndParse(List<String> keys, List<String> values, CommandDispatcher<T> dispatcher) throws FunctionInstantiationException {
        ArrayList newEntries = new ArrayList(this.entries.size());
        ArrayList<String> entryArguments = new ArrayList<String>(values.size());
        for (Entry<T> entry : this.entries) {
            MacroFunction.lookupValues(values, entry.parameters(), entryArguments);
            newEntries.add(entry.instantiate(entryArguments, dispatcher, this.id));
        }
        return new PlainTextFunction(this.id().withPath(id -> id + "/" + keys.hashCode()), newEntries);
    }

    static interface Entry<T> {
        public IntList parameters();

        public UnboundEntryAction<T> instantiate(List<String> var1, CommandDispatcher<T> var2, Identifier var3) throws FunctionInstantiationException;
    }

    static class MacroEntry<T extends ExecutionCommandSource<T>>
    implements Entry<T> {
        private final StringTemplate template;
        private final IntList parameters;
        private final T compilationContext;

        public MacroEntry(StringTemplate template, IntList parameters, T compilationContext) {
            this.template = template;
            this.parameters = parameters;
            this.compilationContext = compilationContext;
        }

        @Override
        public IntList parameters() {
            return this.parameters;
        }

        @Override
        public UnboundEntryAction<T> instantiate(List<String> substitutions, CommandDispatcher<T> dispatcher, Identifier functionId) throws FunctionInstantiationException {
            String command = this.template.substitute(substitutions);
            try {
                return CommandFunction.parseCommand(dispatcher, this.compilationContext, new StringReader(command));
            }
            catch (CommandSyntaxException e) {
                throw new FunctionInstantiationException(Component.translatable("commands.function.error.parse", Component.translationArg(functionId), command, e.getMessage()));
            }
        }
    }

    static class PlainTextEntry<T>
    implements Entry<T> {
        private final UnboundEntryAction<T> compiledAction;

        public PlainTextEntry(UnboundEntryAction<T> compiledAction) {
            this.compiledAction = compiledAction;
        }

        @Override
        public IntList parameters() {
            return IntLists.emptyList();
        }

        @Override
        public UnboundEntryAction<T> instantiate(List<String> substitutions, CommandDispatcher<T> dispatcher, Identifier functionId) {
            return this.compiledAction;
        }
    }
}

