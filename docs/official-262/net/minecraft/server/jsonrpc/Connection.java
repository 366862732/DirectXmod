/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonNull
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.mojang.logging.LogUtils
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.SimpleChannelInboundHandler
 *  io.netty.handler.timeout.ReadTimeoutException
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMaps
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  org.jetbrains.annotations.Contract
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.jsonrpc;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.jsonrpc.IncomingRpcMethod;
import net.minecraft.server.jsonrpc.JsonRPCErrors;
import net.minecraft.server.jsonrpc.JsonRPCUtils;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.ManagementServer;
import net.minecraft.server.jsonrpc.OutgoingRpcMethod;
import net.minecraft.server.jsonrpc.PendingRpcRequest;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.EncodeJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidRequestJsonRpcException;
import net.minecraft.server.jsonrpc.methods.MethodNotFoundJsonRpcException;
import net.minecraft.server.jsonrpc.methods.RemoteRpcErrorException;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Connection
extends SimpleChannelInboundHandler<JsonElement> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger CONNECTION_ID_COUNTER = new AtomicInteger(0);
    private final JsonRpcLogger jsonRpcLogger;
    private final ClientInfo clientInfo;
    private final ManagementServer managementServer;
    private final Channel channel;
    private final MinecraftApi minecraftApi;
    private final AtomicInteger transactionId = new AtomicInteger();
    private final Int2ObjectMap<PendingRpcRequest<?>> pendingRequests = Int2ObjectMaps.synchronize((Int2ObjectMap)new Int2ObjectOpenHashMap());

    public Connection(Channel channel, ManagementServer managementServer, MinecraftApi minecraftApi, JsonRpcLogger jsonrpcLogger) {
        this.clientInfo = ClientInfo.of(CONNECTION_ID_COUNTER.incrementAndGet());
        this.managementServer = managementServer;
        this.minecraftApi = minecraftApi;
        this.channel = channel;
        this.jsonRpcLogger = jsonrpcLogger;
    }

    public void tick() {
        long time = Util.getMillis();
        this.pendingRequests.int2ObjectEntrySet().removeIf(entry -> {
            boolean timedOut = ((PendingRpcRequest)entry.getValue()).timedOut(time);
            if (timedOut) {
                ((PendingRpcRequest)entry.getValue()).resultFuture().completeExceptionally((Throwable)new ReadTimeoutException("RPC method " + String.valueOf(((PendingRpcRequest)entry.getValue()).method().key().identifier()) + " timed out waiting for response"));
            }
            return timedOut;
        });
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.jsonRpcLogger.log(this.clientInfo, "Management connection opened for {}", this.channel.remoteAddress());
        super.channelActive(ctx);
        this.managementServer.onConnected(this);
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.jsonRpcLogger.log(this.clientInfo, "Management connection closed for {}", this.channel.remoteAddress());
        super.channelInactive(ctx);
        this.managementServer.onDisconnected(this);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause.getCause() instanceof JsonParseException) {
            this.channel.writeAndFlush((Object)JsonRPCErrors.PARSE_ERROR.createWithUnknownId(cause.getMessage()));
            return;
        }
        super.exceptionCaught(ctx, cause);
        this.channel.close().awaitUninterruptibly();
    }

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            JsonObject response = this.handleJsonObject(jsonElement.getAsJsonObject());
            if (response != null) {
                this.channel.writeAndFlush((Object)response);
            }
        } else if (jsonElement.isJsonArray()) {
            this.channel.writeAndFlush((Object)this.handleBatchRequest(jsonElement.getAsJsonArray().asList()));
        } else {
            this.channel.writeAndFlush((Object)JsonRPCErrors.INVALID_REQUEST.createWithUnknownId(null));
        }
    }

    private JsonArray handleBatchRequest(List<JsonElement> batchRequests) {
        JsonArray batchResponses = new JsonArray();
        batchRequests.stream().map(batchEntry -> this.handleJsonObject(batchEntry.getAsJsonObject())).filter(Objects::nonNull).forEach(arg_0 -> ((JsonArray)batchResponses).add(arg_0));
        return batchResponses;
    }

    public void sendNotification(Holder.Reference<? extends OutgoingRpcMethod<Void, ?>> method) {
        this.sendRequest(method, null, false);
    }

    public <Params> void sendNotification(Holder.Reference<? extends OutgoingRpcMethod<Params, ?>> method, Params params) {
        this.sendRequest(method, params, false);
    }

    public <Result> CompletableFuture<Result> sendRequest(Holder.Reference<? extends OutgoingRpcMethod<Void, Result>> method) {
        return this.sendRequest(method, null, true);
    }

    public <Params, Result> CompletableFuture<Result> sendRequest(Holder.Reference<? extends OutgoingRpcMethod<Params, Result>> method, Params params) {
        return this.sendRequest(method, params, true);
    }

    @Contract(value="_,_,false->null;_,_,true->!null")
    private <Params, Result> @Nullable CompletableFuture<Result> sendRequest(Holder.Reference<? extends OutgoingRpcMethod<Params, ? extends Result>> methodHolder, @Nullable Params params, boolean expectReply) {
        List<JsonElement> jsonParams;
        OutgoingRpcMethod<Params, Result> method = methodHolder.value();
        if (this.minecraftApi.notificationManager().server() == null && !method.attributes().allowPreServerInit()) {
            return CompletableFuture.failedFuture(new InvalidRequestJsonRpcException("Method cannot be dispatched pre server initialization: " + String.valueOf(method)));
        }
        List<JsonElement> list = jsonParams = params != null ? List.of(Objects.requireNonNull(method.encodeParams(params))) : List.of();
        if (expectReply) {
            CompletableFuture future = new CompletableFuture();
            int id = this.transactionId.incrementAndGet();
            long time = Util.getMillis();
            this.pendingRequests.put(id, new PendingRpcRequest(methodHolder, future, time + 5000L));
            this.channel.writeAndFlush((Object)JsonRPCUtils.createRequest((Integer)id, methodHolder.key().identifier(), jsonParams));
            return future;
        }
        this.channel.writeAndFlush((Object)JsonRPCUtils.createRequest(null, methodHolder.key().identifier(), jsonParams));
        return null;
    }

    @VisibleForTesting
    @Nullable JsonObject handleJsonObject(JsonObject jsonObject) {
        try {
            JsonElement id = JsonRPCUtils.getRequestId(jsonObject);
            String method = JsonRPCUtils.getMethodName(jsonObject);
            JsonElement result = JsonRPCUtils.getResult(jsonObject);
            JsonElement params = JsonRPCUtils.getParams(jsonObject);
            JsonObject error = JsonRPCUtils.getError(jsonObject);
            if (method != null && result == null && error == null) {
                if (id != null && !Connection.isValidRequestId(id)) {
                    return JsonRPCErrors.INVALID_REQUEST.createWithUnknownId("Invalid request id - only String, Number and NULL supported");
                }
                return this.handleIncomingRequest(id, method, params);
            }
            if (method == null && result != null && error == null && id != null) {
                if (Connection.isValidResponseId(id)) {
                    this.handleRequestResponse(id.getAsInt(), result);
                } else {
                    LOGGER.warn("Received respose {} with id {} we did not request", (Object)result, (Object)id);
                }
                return null;
            }
            if (method == null && result == null && error != null) {
                return this.handleError(id, error);
            }
            return JsonRPCErrors.INVALID_REQUEST.createWithoutData((JsonElement)Objects.requireNonNullElse(id, JsonNull.INSTANCE));
        }
        catch (Exception e) {
            LOGGER.error("Error while handling rpc request", (Throwable)e);
            return JsonRPCErrors.INTERNAL_ERROR.createWithUnknownId("Unknown error handling request - check server logs for stack trace");
        }
    }

    private static boolean isValidRequestId(JsonElement id) {
        return id.isJsonNull() || GsonHelper.isNumberValue(id) || GsonHelper.isStringValue(id);
    }

    private static boolean isValidResponseId(JsonElement id) {
        return GsonHelper.isNumberValue(id);
    }

    private @Nullable JsonObject handleIncomingRequest(@Nullable JsonElement id, String method, @Nullable JsonElement params) {
        boolean sendResponse = id != null;
        try {
            JsonElement result = this.dispatchIncomingRequest(method, params);
            if (result == null || !sendResponse) {
                return null;
            }
            return JsonRPCUtils.createSuccessResult(id, result);
        }
        catch (InvalidParameterJsonRpcException e) {
            LOGGER.debug("Invalid parameter invocation {}: {}, {}", new Object[]{method, params, e.getMessage()});
            return sendResponse ? JsonRPCErrors.INVALID_PARAMS.create(id, e.getMessage()) : null;
        }
        catch (EncodeJsonRpcException e) {
            LOGGER.error("Failed to encode json rpc response {}: {}", (Object)method, (Object)e.getMessage());
            return sendResponse ? JsonRPCErrors.INTERNAL_ERROR.create(id, e.getMessage()) : null;
        }
        catch (InvalidRequestJsonRpcException e) {
            return sendResponse ? JsonRPCErrors.INVALID_REQUEST.create(id, e.getMessage()) : null;
        }
        catch (MethodNotFoundJsonRpcException e) {
            return sendResponse ? JsonRPCErrors.METHOD_NOT_FOUND.create(id, e.getMessage()) : null;
        }
        catch (Exception e) {
            LOGGER.error("Error while dispatching rpc method {}", (Object)method, (Object)e);
            return sendResponse ? JsonRPCErrors.INTERNAL_ERROR.createWithoutData(id) : null;
        }
    }

    public @Nullable JsonElement dispatchIncomingRequest(String method, @Nullable JsonElement params) {
        Identifier identifier = Identifier.tryParse(method);
        if (identifier == null) {
            throw new InvalidRequestJsonRpcException("Failed to parse method value: " + method);
        }
        Optional<IncomingRpcMethod<?, ?>> incomingRpcMethod = BuiltInRegistries.INCOMING_RPC_METHOD.getOptional(identifier);
        if (incomingRpcMethod.isEmpty()) {
            throw new MethodNotFoundJsonRpcException("Method not found: " + method);
        }
        IncomingRpcMethod.Attributes attributes = incomingRpcMethod.get().attributes();
        NotificationManager notificationManager = this.minecraftApi.notificationManager();
        if (notificationManager != null && notificationManager.server() == null && !attributes.allowPreServerInit()) {
            throw new InvalidRequestJsonRpcException("Method cannot be dispatched pre server initialization: " + method);
        }
        if (attributes.runOnMainThread()) {
            try {
                return this.minecraftApi.submit(() -> ((IncomingRpcMethod)incomingRpcMethod.get()).apply(this.minecraftApi, params, this.clientInfo)).join();
            }
            catch (CompletionException e) {
                Throwable throwable = e.getCause();
                if (throwable instanceof RuntimeException) {
                    RuntimeException re = (RuntimeException)throwable;
                    throw re;
                }
                throw e;
            }
        }
        return incomingRpcMethod.get().apply(this.minecraftApi, params, this.clientInfo);
    }

    private void handleRequestResponse(int id, JsonElement result) {
        PendingRpcRequest request = (PendingRpcRequest)this.pendingRequests.remove(id);
        if (request == null) {
            LOGGER.warn("Received unknown response (id: {}): {}", (Object)id, (Object)result);
        } else {
            request.accept(result);
        }
    }

    private @Nullable JsonObject handleError(@Nullable JsonElement id, JsonObject error) {
        PendingRpcRequest request;
        if (id != null && Connection.isValidResponseId(id) && (request = (PendingRpcRequest)this.pendingRequests.remove(id.getAsInt())) != null) {
            request.resultFuture().completeExceptionally(new RemoteRpcErrorException(id, error));
        }
        LOGGER.error("Received error (id: {}): {}", (Object)id, (Object)error);
        return null;
    }
}

