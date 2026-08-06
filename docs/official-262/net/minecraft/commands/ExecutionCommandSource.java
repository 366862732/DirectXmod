/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.ResultConsumer
 *  com.mojang.brigadier.exceptions.CommandExceptionType
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.server.permissions.PermissionSetSupplier;
import org.jspecify.annotations.Nullable;

public interface ExecutionCommandSource<T extends ExecutionCommandSource<T>>
extends PermissionSetSupplier {
    public T withCallback(CommandResultCallback var1);

    public CommandResultCallback callback();

    default public T clearCallbacks() {
        return this.withCallback(CommandResultCallback.EMPTY);
    }

    public CommandDispatcher<T> dispatcher();

    public void handleError(CommandExceptionType var1, Message var2, boolean var3, @Nullable TraceCallbacks var4);

    public boolean isSilent();

    default public void handleError(CommandSyntaxException e, boolean forked, @Nullable TraceCallbacks tracer) {
        this.handleError(e.getType(), e.getRawMessage(), forked, tracer);
    }

    public static <T extends ExecutionCommandSource<T>> ResultConsumer<T> resultConsumer() {
        return (context, success, result) -> ((ExecutionCommandSource)context.getSource()).callback().onResult(success, result);
    }
}

