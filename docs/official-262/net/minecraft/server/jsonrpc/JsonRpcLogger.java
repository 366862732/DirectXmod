/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.server.jsonrpc;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import org.slf4j.Logger;

public class JsonRpcLogger {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PREFIX = "RPC Connection #{}: ";

    public void log(ClientInfo clientInfo, String message, Object ... args) {
        if (args.length == 0) {
            LOGGER.info(PREFIX + message, (Object)clientInfo.connectionId());
        } else {
            ArrayList<Object> list = new ArrayList<Object>(Arrays.asList(args));
            list.addFirst(clientInfo.connectionId());
            LOGGER.info(PREFIX + message, list.toArray());
        }
    }
}

