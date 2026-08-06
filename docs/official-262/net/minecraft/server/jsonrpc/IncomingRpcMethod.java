/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.ParamInfo;
import net.minecraft.server.jsonrpc.api.ResultInfo;
import net.minecraft.server.jsonrpc.api.Schema;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.EncodeJsonRpcException;
import net.minecraft.server.jsonrpc.methods.InvalidParameterJsonRpcException;
import org.jspecify.annotations.Nullable;

public interface IncomingRpcMethod<Params, Result> {
    public MethodInfo<Params, Result> info();

    public Attributes attributes();

    public JsonElement apply(MinecraftApi var1, @Nullable JsonElement var2, ClientInfo var3);

    public static <Result> IncomingRpcMethodBuilder<Void, Result> method(ParameterlessRpcMethodFunction<Result> function) {
        return new IncomingRpcMethodBuilder(function);
    }

    public static <Params, Result> IncomingRpcMethodBuilder<Params, Result> method(RpcMethodFunction<Params, Result> function) {
        return new IncomingRpcMethodBuilder<Params, Result>(function);
    }

    public static <Result> IncomingRpcMethodBuilder<Void, Result> method(Function<MinecraftApi, Result> supplier) {
        return new IncomingRpcMethodBuilder(supplier);
    }

    public static class IncomingRpcMethodBuilder<Params, Result> {
        private String description = "";
        private @Nullable ParamInfo<Params> paramInfo;
        private @Nullable ResultInfo<Result> resultInfo;
        private boolean discoverable = true;
        private boolean runOnMainThread = true;
        private @Nullable ParameterlessRpcMethodFunction<Result> parameterlessFunction;
        private @Nullable RpcMethodFunction<Params, Result> parameterFunction;
        private boolean allowPreServerInit = false;

        public IncomingRpcMethodBuilder(ParameterlessRpcMethodFunction<Result> function) {
            this.parameterlessFunction = function;
        }

        public IncomingRpcMethodBuilder(RpcMethodFunction<Params, Result> function) {
            this.parameterFunction = function;
        }

        public IncomingRpcMethodBuilder(Function<MinecraftApi, Result> supplier) {
            this.parameterlessFunction = (apiService, clientInfo) -> supplier.apply(apiService);
        }

        public IncomingRpcMethodBuilder<Params, Result> description(String description) {
            this.description = description;
            return this;
        }

        public IncomingRpcMethodBuilder<Params, Result> response(String resultName, Schema<Result> resultSchema) {
            this.resultInfo = new ResultInfo<Result>(resultName, resultSchema.info());
            return this;
        }

        public IncomingRpcMethodBuilder<Params, Result> param(String paramName, Schema<Params> paramSchema) {
            this.paramInfo = new ParamInfo<Params>(paramName, paramSchema.info());
            return this;
        }

        public IncomingRpcMethodBuilder<Params, Result> undiscoverable() {
            this.discoverable = false;
            return this;
        }

        public IncomingRpcMethodBuilder<Params, Result> notOnMainThread() {
            this.runOnMainThread = false;
            return this;
        }

        public IncomingRpcMethodBuilder<Params, Result> allowPreServerInit() {
            this.allowPreServerInit = true;
            return this;
        }

        public IncomingRpcMethod<Params, Result> build() {
            if (this.resultInfo == null) {
                throw new IllegalStateException("No response defined");
            }
            Attributes attributes = new Attributes(this.runOnMainThread, this.discoverable, this.allowPreServerInit);
            MethodInfo<Params, Result> methodInfo = new MethodInfo<Params, Result>(this.description, this.paramInfo, this.resultInfo);
            if (this.parameterlessFunction != null) {
                return new ParameterlessMethod<Params, Result>(methodInfo, attributes, this.parameterlessFunction);
            }
            if (this.parameterFunction != null) {
                if (this.paramInfo == null) {
                    throw new IllegalStateException("No param schema defined");
                }
                return new Method<Params, Result>(methodInfo, attributes, this.parameterFunction);
            }
            throw new IllegalStateException("No method defined");
        }

        public IncomingRpcMethod<?, ?> register(Registry<IncomingRpcMethod<?, ?>> methodRegistry, String key) {
            return this.register(methodRegistry, Identifier.withDefaultNamespace(key));
        }

        private IncomingRpcMethod<?, ?> register(Registry<IncomingRpcMethod<?, ?>> methodRegistry, Identifier id) {
            return Registry.register(methodRegistry, id, this.build());
        }
    }

    @FunctionalInterface
    public static interface ParameterlessRpcMethodFunction<Result> {
        public Result apply(MinecraftApi var1, ClientInfo var2);
    }

    @FunctionalInterface
    public static interface RpcMethodFunction<Params, Result> {
        public Result apply(MinecraftApi var1, Params var2, ClientInfo var3);
    }

    public record Method<Params, Result>(MethodInfo<Params, Result> info, Attributes attributes, RpcMethodFunction<Params, Result> function) implements IncomingRpcMethod<Params, Result>
    {
        @Override
        public JsonElement apply(MinecraftApi minecraftApi, @Nullable JsonElement paramsJson, ClientInfo clientInfo) {
            JsonElement paramsJsonElement;
            if (paramsJson == null || !paramsJson.isJsonArray() && !paramsJson.isJsonObject()) {
                throw new InvalidParameterJsonRpcException("Expected params as array or named");
            }
            if (this.info.params().isEmpty()) {
                throw new IllegalArgumentException("Method defined as having parameters without describing them");
            }
            if (paramsJson.isJsonObject()) {
                String parameterName = this.info.params().get().name();
                JsonElement jsonElement = paramsJson.getAsJsonObject().get(parameterName);
                if (jsonElement == null) {
                    throw new InvalidParameterJsonRpcException(String.format(Locale.ROOT, "Params passed by-name, but expected param [%s] does not exist", parameterName));
                }
                paramsJsonElement = jsonElement;
            } else {
                JsonArray jsonArray = paramsJson.getAsJsonArray();
                if (jsonArray.isEmpty() || jsonArray.size() > 1) {
                    throw new InvalidParameterJsonRpcException("Expected exactly one element in the params array");
                }
                paramsJsonElement = jsonArray.get(0);
            }
            Object params = this.info.params().get().schema().codec().parse((DynamicOps)JsonOps.INSTANCE, (Object)paramsJsonElement).getOrThrow(InvalidParameterJsonRpcException::new);
            Result result = this.function.apply(minecraftApi, params, clientInfo);
            if (this.info.result().isEmpty()) {
                throw new IllegalStateException("No result codec defined");
            }
            return (JsonElement)this.info.result().get().schema().codec().encodeStart((DynamicOps)JsonOps.INSTANCE, result).getOrThrow(EncodeJsonRpcException::new);
        }
    }

    public record ParameterlessMethod<Params, Result>(MethodInfo<Params, Result> info, Attributes attributes, ParameterlessRpcMethodFunction<Result> supplier) implements IncomingRpcMethod<Params, Result>
    {
        @Override
        public JsonElement apply(MinecraftApi minecraftApi, @Nullable JsonElement paramsJson, ClientInfo clientInfo) {
            if (!(paramsJson == null || paramsJson.isJsonArray() && paramsJson.getAsJsonArray().isEmpty())) {
                throw new InvalidParameterJsonRpcException("Expected no params, or an empty array");
            }
            if (this.info.params().isPresent()) {
                throw new IllegalArgumentException("Parameterless method unexpectedly has parameter description");
            }
            Result result = this.supplier.apply(minecraftApi, clientInfo);
            if (this.info.result().isEmpty()) {
                throw new IllegalStateException("No result codec defined");
            }
            return (JsonElement)this.info.result().get().schema().codec().encodeStart((DynamicOps)JsonOps.INSTANCE, result).getOrThrow(InvalidParameterJsonRpcException::new);
        }
    }

    public record Attributes(boolean runOnMainThread, boolean discoverable, boolean allowPreServerInit) {
    }
}

