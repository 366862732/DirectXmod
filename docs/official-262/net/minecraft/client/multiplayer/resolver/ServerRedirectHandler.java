/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer.resolver;

import com.mojang.logging.LogUtils;
import java.util.Hashtable;
import java.util.Optional;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.slf4j.Logger;

@FunctionalInterface
@Environment(value=EnvType.CLIENT)
public interface ServerRedirectHandler {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ServerRedirectHandler EMPTY = originalAddress -> Optional.empty();

    public Optional<ServerAddress> lookupRedirect(ServerAddress var1);

    public static ServerRedirectHandler createDnsSrvRedirectHandler() {
        InitialDirContext context;
        try {
            String dnsContextClass = "com.sun.jndi.dns.DnsContextFactory";
            Class.forName("com.sun.jndi.dns.DnsContextFactory");
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            context = new InitialDirContext(env);
        }
        catch (Throwable e) {
            LOGGER.error("Failed to initialize SRV redirect resolved, some servers might not work", e);
            return EMPTY;
        }
        return originalAddress -> {
            if (originalAddress.getPort() == 25565) {
                try {
                    Attributes attributes = context.getAttributes("_minecraft._tcp." + originalAddress.getHost(), new String[]{"SRV"});
                    Attribute srvAttribute = attributes.get("srv");
                    if (srvAttribute != null) {
                        String[] arguments = srvAttribute.get().toString().split(" ", 4);
                        return Optional.of(new ServerAddress(arguments[3], ServerAddress.parsePort(arguments[2])));
                    }
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            return Optional.empty();
        };
    }
}

