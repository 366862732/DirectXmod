/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.server;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerPinger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class LanServerDetection {
    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
    private static final Logger LOGGER = LogUtils.getLogger();

    @Environment(value=EnvType.CLIENT)
    public static class LanServerDetector
    extends Thread {
        private final LanServerList serverList;
        private final InetAddress pingGroup;
        private final MulticastSocket socket;

        public LanServerDetector(LanServerList serverList) throws IOException {
            super("LanServerDetector #" + UNIQUE_THREAD_ID.incrementAndGet());
            this.serverList = serverList;
            this.setDaemon(true);
            this.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
            this.socket = new MulticastSocket(4445);
            this.pingGroup = InetAddress.getByName("224.0.2.60");
            this.socket.setSoTimeout(5000);
            this.socket.joinGroup(this.pingGroup);
        }

        @Override
        public void run() {
            byte[] buf = new byte[1024];
            while (!this.isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    this.socket.receive(packet);
                }
                catch (SocketTimeoutException ignored) {
                    continue;
                }
                catch (IOException e) {
                    LOGGER.error("Couldn't ping server", (Throwable)e);
                    break;
                }
                String received = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                LOGGER.debug("{}: {}", (Object)packet.getAddress(), (Object)received);
                this.serverList.addServer(received, packet.getAddress());
            }
            try {
                this.socket.leaveGroup(this.pingGroup);
            }
            catch (IOException iOException) {
                // empty catch block
            }
            this.socket.close();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class LanServerList {
        private final List<LanServer> servers = Lists.newArrayList();
        private boolean isDirty;

        public synchronized @Nullable List<LanServer> takeDirtyServers() {
            if (this.isDirty) {
                List<LanServer> newServers = List.copyOf(this.servers);
                this.isDirty = false;
                return newServers;
            }
            return null;
        }

        public synchronized void addServer(String pingData, InetAddress socketAddress) {
            String motd = LanServerPinger.parseMotd(pingData);
            Object address = LanServerPinger.parseAddress(pingData);
            if (address == null) {
                return;
            }
            address = socketAddress.getHostAddress() + ":" + (String)address;
            boolean found = false;
            for (LanServer server : this.servers) {
                if (!server.getAddress().equals(address)) continue;
                server.updatePingTime();
                found = true;
                break;
            }
            if (!found) {
                this.servers.add(new LanServer(motd, (String)address));
                this.isDirty = true;
            }
        }
    }
}

