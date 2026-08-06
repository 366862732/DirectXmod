/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.configuration;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundResetChatPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;

public interface ClientConfigurationPacketListener
extends ClientCommonPacketListener {
    @Override
    default public ConnectionProtocol protocol() {
        return ConnectionProtocol.CONFIGURATION;
    }

    public void handleCodeOfConduct(ClientboundCodeOfConductPacket var1);

    public void handleConfigurationFinished(ClientboundFinishConfigurationPacket var1);

    public void handleRegistryData(ClientboundRegistryDataPacket var1);

    public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket var1);

    public void handleSelectKnownPacks(ClientboundSelectKnownPacks var1);

    public void handleResetChat(ClientboundResetChatPacket var1);
}

