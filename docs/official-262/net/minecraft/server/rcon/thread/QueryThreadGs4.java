/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.rcon.thread;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.NetworkDataOutputStream;
import net.minecraft.server.rcon.PktUtils;
import net.minecraft.server.rcon.thread.GenericThread;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class QueryThreadGs4
extends GenericThread {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GAME_TYPE = "SMP";
    private static final String GAME_ID = "MINECRAFT";
    private static final long CHALLENGE_CHECK_INTERVAL = 30000L;
    private static final long RESPONSE_CACHE_TIME = 5000L;
    private long lastChallengeCheck;
    private final int port;
    private final int serverPort;
    private final int maxPlayers;
    private final String serverName;
    private final String worldName;
    private DatagramSocket socket;
    private final byte[] buffer = new byte[1460];
    private String hostIp;
    private String serverIp;
    private final Map<SocketAddress, RequestChallenge> validChallenges;
    private final NetworkDataOutputStream rulesResponse;
    private long lastRulesResponse;
    private final ServerInterface serverInterface;

    private QueryThreadGs4(ServerInterface serverInterface, int port) {
        super("Query Listener");
        this.serverInterface = serverInterface;
        this.port = port;
        this.serverIp = serverInterface.getServerIp();
        this.serverPort = serverInterface.getServerPort();
        this.serverName = serverInterface.getServerName();
        this.maxPlayers = serverInterface.getMaxPlayers();
        this.worldName = serverInterface.getLevelIdName();
        this.lastRulesResponse = 0L;
        this.hostIp = "0.0.0.0";
        if (this.serverIp.isEmpty() || this.hostIp.equals(this.serverIp)) {
            this.serverIp = "0.0.0.0";
            try {
                InetAddress addr = InetAddress.getLocalHost();
                this.hostIp = addr.getHostAddress();
            }
            catch (UnknownHostException e) {
                LOGGER.warn("Unable to determine local host IP, please set server-ip in server.properties", (Throwable)e);
            }
        } else {
            this.hostIp = this.serverIp;
        }
        this.rulesResponse = new NetworkDataOutputStream(1460);
        this.validChallenges = Maps.newHashMap();
    }

    public static @Nullable QueryThreadGs4 create(ServerInterface serverInterface) {
        int port = serverInterface.getProperties().queryPort;
        if (0 >= port || 65535 < port) {
            LOGGER.warn("Invalid query port {} found in server.properties (queries disabled)", (Object)port);
            return null;
        }
        QueryThreadGs4 result = new QueryThreadGs4(serverInterface, port);
        if (!result.start()) {
            return null;
        }
        return result;
    }

    private void sendTo(byte[] data, DatagramPacket src) throws IOException {
        this.socket.send(new DatagramPacket(data, data.length, src.getSocketAddress()));
    }

    private boolean processPacket(DatagramPacket packet) throws IOException {
        byte[] buf = packet.getData();
        int len = packet.getLength();
        SocketAddress socketAddress = packet.getSocketAddress();
        LOGGER.debug("Packet len {} [{}]", (Object)len, (Object)socketAddress);
        if (3 > len || -2 != buf[0] || -3 != buf[1]) {
            LOGGER.debug("Invalid packet [{}]", (Object)socketAddress);
            return false;
        }
        LOGGER.debug("Packet '{}' [{}]", (Object)PktUtils.toHexString(buf[2]), (Object)socketAddress);
        switch (buf[2]) {
            case 9: {
                this.sendChallenge(packet);
                LOGGER.debug("Challenge [{}]", (Object)socketAddress);
                return true;
            }
            case 0: {
                if (!this.validChallenge(packet).booleanValue()) {
                    LOGGER.debug("Invalid challenge [{}]", (Object)socketAddress);
                    return false;
                }
                if (15 == len) {
                    this.sendTo(this.buildRuleResponse(packet), packet);
                    LOGGER.debug("Rules [{}]", (Object)socketAddress);
                    break;
                }
                NetworkDataOutputStream dos = new NetworkDataOutputStream(1460);
                dos.write(0);
                dos.writeBytes(this.getIdentBytes(packet.getSocketAddress()));
                dos.writeString(this.serverName);
                dos.writeString(GAME_TYPE);
                dos.writeString(this.worldName);
                dos.writeString(Integer.toString(this.serverInterface.getPlayerCount()));
                dos.writeString(Integer.toString(this.maxPlayers));
                dos.writeShort((short)this.serverPort);
                dos.writeString(this.hostIp);
                this.sendTo(dos.toByteArray(), packet);
                LOGGER.debug("Status [{}]", (Object)socketAddress);
            }
        }
        return true;
    }

    private byte[] buildRuleResponse(DatagramPacket packet) throws IOException {
        String[] players;
        long now = Util.getMillis();
        if (now < this.lastRulesResponse + 5000L) {
            byte[] data = this.rulesResponse.toByteArray();
            byte[] ident = this.getIdentBytes(packet.getSocketAddress());
            data[1] = ident[0];
            data[2] = ident[1];
            data[3] = ident[2];
            data[4] = ident[3];
            return data;
        }
        this.lastRulesResponse = now;
        this.rulesResponse.reset();
        this.rulesResponse.write(0);
        this.rulesResponse.writeBytes(this.getIdentBytes(packet.getSocketAddress()));
        this.rulesResponse.writeString("splitnum");
        this.rulesResponse.write(128);
        this.rulesResponse.write(0);
        this.rulesResponse.writeString("hostname");
        this.rulesResponse.writeString(this.serverName);
        this.rulesResponse.writeString("gametype");
        this.rulesResponse.writeString(GAME_TYPE);
        this.rulesResponse.writeString("game_id");
        this.rulesResponse.writeString(GAME_ID);
        this.rulesResponse.writeString("version");
        this.rulesResponse.writeString(this.serverInterface.getServerVersion());
        this.rulesResponse.writeString("plugins");
        this.rulesResponse.writeString(this.serverInterface.getPluginNames());
        this.rulesResponse.writeString("map");
        this.rulesResponse.writeString(this.worldName);
        this.rulesResponse.writeString("numplayers");
        this.rulesResponse.writeString("" + this.serverInterface.getPlayerCount());
        this.rulesResponse.writeString("maxplayers");
        this.rulesResponse.writeString("" + this.maxPlayers);
        this.rulesResponse.writeString("hostport");
        this.rulesResponse.writeString("" + this.serverPort);
        this.rulesResponse.writeString("hostip");
        this.rulesResponse.writeString(this.hostIp);
        this.rulesResponse.write(0);
        this.rulesResponse.write(1);
        this.rulesResponse.writeString("player_");
        this.rulesResponse.write(0);
        for (String player : players = this.serverInterface.getPlayerNames()) {
            this.rulesResponse.writeString(player);
        }
        this.rulesResponse.write(0);
        return this.rulesResponse.toByteArray();
    }

    private byte[] getIdentBytes(SocketAddress src) {
        return this.validChallenges.get(src).getIdentBytes();
    }

    private Boolean validChallenge(DatagramPacket src) {
        SocketAddress sockAddr = src.getSocketAddress();
        if (!this.validChallenges.containsKey(sockAddr)) {
            return false;
        }
        byte[] data = src.getData();
        return this.validChallenges.get(sockAddr).getChallenge() == PktUtils.intFromNetworkByteArray(data, 7, src.getLength());
    }

    private void sendChallenge(DatagramPacket src) throws IOException {
        RequestChallenge challenge = new RequestChallenge(src);
        this.validChallenges.put(src.getSocketAddress(), challenge);
        this.sendTo(challenge.getChallengeBytes(), src);
    }

    private void pruneChallenges() {
        if (!this.running) {
            return;
        }
        long now = Util.getMillis();
        if (now < this.lastChallengeCheck + 30000L) {
            return;
        }
        this.lastChallengeCheck = now;
        this.validChallenges.values().removeIf(challenge -> challenge.before(now));
    }

    @Override
    public void run() {
        LOGGER.info("Query running on {}:{}", (Object)this.serverIp, (Object)this.port);
        this.lastChallengeCheck = Util.getMillis();
        DatagramPacket request = new DatagramPacket(this.buffer, this.buffer.length);
        try {
            while (this.running) {
                try {
                    this.socket.receive(request);
                    this.pruneChallenges();
                    this.processPacket(request);
                }
                catch (SocketTimeoutException ignored) {
                    this.pruneChallenges();
                }
                catch (PortUnreachableException ignored) {
                }
                catch (IOException e) {
                    this.recoverSocketError(e);
                }
            }
        }
        finally {
            LOGGER.debug("closeSocket: {}:{}", (Object)this.serverIp, (Object)this.port);
            this.socket.close();
        }
    }

    @Override
    public boolean start() {
        if (this.running) {
            return true;
        }
        if (!this.initSocket()) {
            return false;
        }
        return super.start();
    }

    private void recoverSocketError(Exception e) {
        if (!this.running) {
            return;
        }
        LOGGER.warn("Unexpected exception", (Throwable)e);
        if (!this.initSocket()) {
            LOGGER.error("Failed to recover from exception, shutting down!");
            this.running = false;
        }
    }

    private boolean initSocket() {
        try {
            this.socket = new DatagramSocket(this.port, InetAddress.getByName(this.serverIp));
            this.socket.setSoTimeout(500);
            return true;
        }
        catch (Exception e) {
            LOGGER.warn("Unable to initialise query system on {}:{}", new Object[]{this.serverIp, this.port, e});
            return false;
        }
    }

    private static class RequestChallenge {
        private final long time = new Date().getTime();
        private final int challenge;
        private final byte[] identBytes;
        private final byte[] challengeBytes;
        private final String ident;

        public RequestChallenge(DatagramPacket src) {
            byte[] buf = src.getData();
            this.identBytes = new byte[4];
            this.identBytes[0] = buf[3];
            this.identBytes[1] = buf[4];
            this.identBytes[2] = buf[5];
            this.identBytes[3] = buf[6];
            this.ident = new String(this.identBytes, StandardCharsets.UTF_8);
            this.challenge = RandomSource.createThreadLocalInstance().nextInt(0x1000000);
            this.challengeBytes = String.format(Locale.ROOT, "\t%s%d\u0000", this.ident, this.challenge).getBytes(StandardCharsets.UTF_8);
        }

        public Boolean before(long time) {
            return this.time < time;
        }

        public int getChallenge() {
            return this.challenge;
        }

        public byte[] getChallengeBytes() {
            return this.challengeBytes;
        }

        public byte[] getIdentBytes() {
            return this.identBytes;
        }

        public String getIdent() {
            return this.ident;
        }
    }
}

