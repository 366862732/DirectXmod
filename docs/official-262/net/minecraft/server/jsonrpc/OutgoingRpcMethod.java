/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.jsonrpc;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.ParamInfo;
import net.minecraft.server.jsonrpc.api.ResultInfo;
import net.minecraft.server.jsonrpc.api.Schema;
import org.jspecify.annotations.Nullable;

public interface OutgoingRpcMethod<Params, Result> {
    public static final String NOTIFICATION_PREFIX = "notification/";

    public MethodInfo<Params, Result> info();

    public Attributes attributes();

    default public @Nullable JsonElement encodeParams(Params params) {
        return null;
    }

    default public @Nullable Result decodeResult(JsonElement result) {
        return null;
    }

    public static OutgoingRpcMethodBuilder<Void, Void> notification() {
        return new OutgoingRpcMethodBuilder<Void, Void>(ParmeterlessNotification::new);
    }

    public static <Params> OutgoingRpcMethodBuilder<Params, Void> notificationWithParams() {
        return new OutgoingRpcMethodBuilder(Notification::new);
    }

    public static <Result> OutgoingRpcMethodBuilder<Void, Result> request() {
        return new OutgoingRpcMethodBuilder(ParameterlessMethod::new);
    }

    public static <Params, Result> OutgoingRpcMethodBuilder<Params, Result> requestWithParams() {
        return new OutgoingRpcMethodBuilder(Method::new);
    }

    public static class OutgoingRpcMethodBuilder<Params, Result> {
        public static final Attributes DEFAULT_ATTRIBUTES = new Attributes(true, false);
        private final Factory<Params, Result> method;
        private String description = "";
        private @Nullable ParamInfo<Params> paramInfo;
        private @Nullable ResultInfo<Result> resultInfo;
        private boolean allowPreServerInit = false;

        public OutgoingRpcMethodBuilder(Factory<Params, Result> method) {
            this.method = method;
        }

        public OutgoingRpcMethodBuilder<Params, Result> description(String description) {
            this.description = description;
            return this;
        }

        public OutgoingRpcMethodBuilder<Params, Result> response(String resultName, Schema<Result> resultSchema) {
            this.resultInfo = new ResultInfo<Result>(resultName, resultSchema);
            return this;
        }

        public OutgoingRpcMethodBuilder<Params, Result> param(String paramName, Schema<Params> paramSchema) {
            this.paramInfo = new ParamInfo<Params>(paramName, paramSchema);
            return this;
        }

        public OutgoingRpcMethodBuilder<Params, Result> allowPreServerInit() {
            this.allowPreServerInit = true;
            return this;
        }

        private OutgoingRpcMethod<Params, Result> build() {
            MethodInfo<Params, Result> methodInfo = new MethodInfo<Params, Result>(this.description, this.paramInfo, this.resultInfo);
            Attributes attributes = this.allowPreServerInit ? new Attributes(DEFAULT_ATTRIBUTES.discoverable(), true) : DEFAULT_ATTRIBUTES;
            return this.method.create(methodInfo, attributes);
        }

        public Holder.Reference<OutgoingRpcMethod<Params, Result>> register(String key) {
            return this.register(Identifier.withDefaultNamespace(OutgoingRpcMethod.NOTIFICATION_PREFIX + key));
        }

        private Holder.Reference<OutgoingRpcMethod<Params, Result>> register(Identifier id) {
            return Registry.registerForHolder(BuiltInRegistries.OUTGOING_RPC_METHOD, id, this.build());
        }
    }

    @FunctionalInterface
    public static interface Factory<Params, Result> {
        public OutgoingRpcMethod<Params, Result> create(MethodInfo<Params, Result> var1, Attributes var2);
    }

    public record Method<Params, Result>(MethodInfo<Params, Result> info, Attributes attributes) implements OutgoingRpcMethod<Params, Result>
    {
        @Override
        public @Nullable JsonElement encodeParams(Params params) {
            if (this.info.params().isEmpty()) {
                throw new IllegalStateException("Method defined as having no parameters");
            }
            return (JsonElement)this.info.params().get().schema().codec().encodeStart((DynamicOps)JsonOps.INSTANCE, params).getOrThrow();
        }

        @Override
        public Result decodeResult(JsonElement result) {
            if (this.info.result().isEmpty()) {
                throw new IllegalStateException("Method defined as having no result");
            }
            return (Result)this.info.result().get().schema().codec().parse((DynamicOps)JsonOps.INSTANCE, (Object)result).getOrThrow();
        }
    }

    public record ParameterlessMethod<Result>(MethodInfo<Void, Result> info, Attributes attributes) implements OutgoingRpcMethod<Void, Result>
    {
        @Override
        public Result decodeResult(JsonElement result) {
            if (this.info.result().isEmpty()) {
                throw new IllegalStateException("Method defined as having no result");
            }
            return (Result)this.info.result().get().schema().codec().parse((DynamicOps)JsonOps.INSTANCE, (Object)result).getOrThrow();
        }
    }

    public record Notification<Params>(MethodInfo<Params, Void> info, Attributes attributes) implements OutgoingRpcMethod<Params, Void>
    {
        @Override
        public @Nullable JsonElement encodeParams(Params params) {
            if (this.info.params().isEmpty()) {
                throw new IllegalStateException("Method defined as having no parameters");
            }
            return (JsonElement)this.info.params().get().schema().codec().encodeStart((DynamicOps)JsonOps.INSTANCE, params).getOrThrow();
        }
    }

    public record ParmeterlessNotification(MethodInfo<Void, Void> info, Attributes attributes) implements OutgoingRpcMethod<Void, Void>
    {
    }

    public record Attributes(boolean discoverable, boolean allowPreServerInit) {
    }
}

