/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 */
package net.minecraft.commands.functions;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.commands.functions.CommandFunction;

public record StringTemplate(List<String> segments, List<String> variables) {
    public static StringTemplate fromString(String input) {
        ImmutableList.Builder segments = ImmutableList.builder();
        ImmutableList.Builder variables = ImmutableList.builder();
        int length = input.length();
        int start = 0;
        int index = input.indexOf(36);
        while (index != -1) {
            if (index == length - 1 || input.charAt(index + 1) != '(') {
                index = input.indexOf(36, index + 1);
                continue;
            }
            segments.add((Object)input.substring(start, index));
            int variableEnd = input.indexOf(41, index + 1);
            if (variableEnd == -1) {
                throw new IllegalArgumentException("Unterminated macro variable");
            }
            String variable = input.substring(index + 2, variableEnd);
            if (!StringTemplate.isValidVariableName(variable)) {
                throw new IllegalArgumentException("Invalid macro variable name '" + variable + "'");
            }
            variables.add((Object)variable);
            start = variableEnd + 1;
            index = input.indexOf(36, start);
        }
        if (start == 0) {
            throw new IllegalArgumentException("No variables in macro");
        }
        if (start != length) {
            segments.add((Object)input.substring(start));
        }
        return new StringTemplate((List<String>)segments.build(), (List<String>)variables.build());
    }

    public static boolean isValidVariableName(String variable) {
        for (int i = 0; i < variable.length(); ++i) {
            char character = variable.charAt(i);
            if (Character.isLetterOrDigit(character) || character == '_') continue;
            return false;
        }
        return true;
    }

    public String substitute(List<String> arguments) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.variables.size(); ++i) {
            builder.append(this.segments.get(i)).append(arguments.get(i));
            CommandFunction.checkCommandLineLength(builder);
        }
        if (this.segments.size() > this.variables.size()) {
            builder.append(this.segments.getLast());
        }
        CommandFunction.checkCommandLineLength(builder);
        return builder.toString();
    }
}

