/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 */
package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.server.jsonrpc.OutgoingRpcMethod;

public record PendingRpcRequest<Result>(Holder.Reference<? extends OutgoingRpcMethod<?, ? extends Result>> method, CompletableFuture<Result> resultFuture, long timeoutTime) {
    public void accept(JsonElement response) {
        try {
            Result result = this.method.value().decodeResult(response);
            this.resultFuture.complete(Objects.requireNonNull(result));
        }
        catch (Exception e) {
            this.resultFuture.completeExceptionally(e);
        }
    }

    public boolean timedOut(long currentTime) {
        return currentTime > this.timeoutTime;
    }
}

