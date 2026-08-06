/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.jsonrpc.methods;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.jsonrpc.IncomingRpcMethod;
import net.minecraft.server.jsonrpc.OutgoingRpcMethod;
import net.minecraft.server.jsonrpc.api.MethodInfo;
import net.minecraft.server.jsonrpc.api.Schema;
import net.minecraft.server.jsonrpc.api.SchemaComponent;

public class DiscoveryService {
    public static DiscoverResponse discover(List<SchemaComponent<?>> schemaRegistry) {
        ArrayList methods = new ArrayList(BuiltInRegistries.INCOMING_RPC_METHOD.size() + BuiltInRegistries.OUTGOING_RPC_METHOD.size());
        BuiltInRegistries.INCOMING_RPC_METHOD.listElements().forEach(e -> {
            if (((IncomingRpcMethod)e.value()).attributes().discoverable()) {
                methods.add(((IncomingRpcMethod)e.value()).info().named(e.key().identifier()));
            }
        });
        BuiltInRegistries.OUTGOING_RPC_METHOD.listElements().forEach(e -> {
            if (((OutgoingRpcMethod)e.value()).attributes().discoverable()) {
                methods.add(((OutgoingRpcMethod)e.value()).info().named(e.key().identifier()));
            }
        });
        HashMap schemas = new HashMap();
        for (SchemaComponent<?> component : schemaRegistry) {
            schemas.put(component.name(), component.schema().info());
        }
        DiscoverInfo discoverInfo = new DiscoverInfo("Minecraft Server JSON-RPC", "3.0.0");
        return new DiscoverResponse("1.3.2", discoverInfo, methods, new DiscoverComponents(schemas));
    }

    public record DiscoverInfo(String title, String version) {
        public static final MapCodec<DiscoverInfo> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.STRING.fieldOf("title").forGetter(DiscoverInfo::title), (App)Codec.STRING.fieldOf("version").forGetter(DiscoverInfo::version)).apply((Applicative)i, DiscoverInfo::new));
    }

    public record DiscoverResponse(String jsonRpcProtocolVersion, DiscoverInfo discoverInfo, List<MethodInfo.Named<?, ?>> methods, DiscoverComponents components) {
        public static final MapCodec<DiscoverResponse> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.STRING.fieldOf("openrpc").forGetter(DiscoverResponse::jsonRpcProtocolVersion), (App)DiscoverInfo.CODEC.codec().fieldOf("info").forGetter(DiscoverResponse::discoverInfo), (App)Codec.list(MethodInfo.Named.CODEC).fieldOf("methods").forGetter(DiscoverResponse::methods), (App)DiscoverComponents.CODEC.codec().fieldOf("components").forGetter(DiscoverResponse::components)).apply((Applicative)i, DiscoverResponse::new));
    }

    public record DiscoverComponents(Map<String, Schema<?>> schemas) {
        public static final MapCodec<DiscoverComponents> CODEC = DiscoverComponents.typedSchema();

        private static MapCodec<DiscoverComponents> typedSchema() {
            return RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.unboundedMap((Codec)Codec.STRING, Schema.CODEC).fieldOf("schemas").forGetter(DiscoverComponents::schemas)).apply((Applicative)i, DiscoverComponents::new));
        }
    }
}

