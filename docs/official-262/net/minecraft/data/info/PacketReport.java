/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.StatusProtocols;

public class PacketReport
implements DataProvider {
    private final PackOutput output;

    public PacketReport(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("packets.json");
        return DataProvider.saveStable(cache, this.serializePackets(), path);
    }

    private JsonElement serializePackets() {
        JsonObject protocols = new JsonObject();
        Stream.of(HandshakeProtocols.SERVERBOUND_TEMPLATE, StatusProtocols.CLIENTBOUND_TEMPLATE, StatusProtocols.SERVERBOUND_TEMPLATE, LoginProtocols.CLIENTBOUND_TEMPLATE, LoginProtocols.SERVERBOUND_TEMPLATE, ConfigurationProtocols.CLIENTBOUND_TEMPLATE, ConfigurationProtocols.SERVERBOUND_TEMPLATE, GameProtocols.CLIENTBOUND_TEMPLATE, GameProtocols.SERVERBOUND_TEMPLATE).map(ProtocolInfo.DetailsProvider::details).collect(Collectors.groupingBy(ProtocolInfo.Details::id)).forEach((protocolId, flows) -> {
            JsonObject protocolData = new JsonObject();
            protocols.add(protocolId.id(), (JsonElement)protocolData);
            flows.forEach(flow -> {
                JsonObject protocolFlowData = new JsonObject();
                protocolData.add(flow.flow().id(), (JsonElement)protocolFlowData);
                flow.listPackets((type, networkId) -> {
                    JsonObject packetInfo = new JsonObject();
                    packetInfo.addProperty("protocol_id", (Number)networkId);
                    protocolFlowData.add(type.id().toString(), (JsonElement)packetInfo);
                });
            });
        });
        return protocols;
    }

    @Override
    public String getName() {
        return "Packet Report";
    }
}

