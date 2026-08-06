/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.commands.execution;

import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;

@FunctionalInterface
public interface EntryAction<T> {
    public void execute(ExecutionContext<T> var1, Frame var2);
}

