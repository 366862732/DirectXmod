/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.io.IOUtils
 */
package com.mojang.realmsclient.client;

import com.google.common.collect.Lists;
import com.mojang.realmsclient.dto.RegionPingResult;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;

@Environment(value=EnvType.CLIENT)
public class Ping {
    public static List<RegionPingResult> ping(Region ... regions) {
        for (Region region : regions) {
            Ping.ping(region.endpoint);
        }
        ArrayList results = Lists.newArrayList();
        for (Region region : regions) {
            results.add(new RegionPingResult(region.name, Ping.ping(region.endpoint)));
        }
        results.sort(Comparator.comparingInt(RegionPingResult::ping));
        return results;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static int ping(String host) {
        int timeout = 700;
        long sum = 0L;
        Socket socket = null;
        for (int i = 0; i < 5; ++i) {
            try {
                InetSocketAddress sockAddr = new InetSocketAddress(host, 80);
                socket = new Socket();
                long t1 = Ping.now();
                socket.connect(sockAddr, 700);
                sum += Ping.now() - t1;
                IOUtils.closeQuietly((Socket)socket);
                continue;
            }
            catch (Exception ignored) {
                sum += 700L;
                continue;
            }
            finally {
                IOUtils.closeQuietly(socket);
            }
        }
        return (int)((double)sum / 5.0);
    }

    private static long now() {
        return Util.getMillis();
    }

    public static List<RegionPingResult> pingAllRegions() {
        return Ping.ping(Region.values());
    }

    @Environment(value=EnvType.CLIENT)
    private static enum Region {
        US_EAST_1("us-east-1", "ec2.us-east-1.amazonaws.com"),
        US_WEST_2("us-west-2", "ec2.us-west-2.amazonaws.com"),
        US_WEST_1("us-west-1", "ec2.us-west-1.amazonaws.com"),
        EU_WEST_1("eu-west-1", "ec2.eu-west-1.amazonaws.com"),
        AP_SOUTHEAST_1("ap-southeast-1", "ec2.ap-southeast-1.amazonaws.com"),
        AP_SOUTHEAST_2("ap-southeast-2", "ec2.ap-southeast-2.amazonaws.com"),
        AP_NORTHEAST_1("ap-northeast-1", "ec2.ap-northeast-1.amazonaws.com"),
        SA_EAST_1("sa-east-1", "ec2.sa-east-1.amazonaws.com");

        private final String name;
        private final String endpoint;

        private Region(String name, String endpoint) {
            this.name = name;
            this.endpoint = endpoint;
        }
    }
}

