/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.dto.RealmsServer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(value=EnvType.CLIENT)
public class RealmsServerList
implements Iterable<RealmsServer> {
    private final Minecraft minecraft;
    private final Set<RealmsServer> removedServers = new HashSet<RealmsServer>();
    private List<RealmsServer> servers = List.of();

    public RealmsServerList(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void updateServersList(List<RealmsServer> fetchedServers) {
        ArrayList<RealmsServer> sortedServers = new ArrayList<RealmsServer>(fetchedServers);
        sortedServers.sort(new RealmsServer.McoServerComparator(this.minecraft.getUser().getName()));
        boolean removedAnyServers = sortedServers.removeAll(this.removedServers);
        if (!removedAnyServers) {
            this.removedServers.clear();
        }
        this.servers = sortedServers;
    }

    public void removeItem(RealmsServer server) {
        this.servers.remove(server);
        this.removedServers.add(server);
    }

    @Override
    public Iterator<RealmsServer> iterator() {
        return this.servers.iterator();
    }

    public boolean isEmpty() {
        return this.servers.isEmpty();
    }
}

