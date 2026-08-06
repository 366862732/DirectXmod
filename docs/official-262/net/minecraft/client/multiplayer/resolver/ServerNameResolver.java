/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.multiplayer.resolver;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.resolver.AddressCheck;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver;
import net.minecraft.client.multiplayer.resolver.ServerRedirectHandler;

@Environment(value=EnvType.CLIENT)
public class ServerNameResolver {
    public static final ServerNameResolver DEFAULT = new ServerNameResolver(ServerAddressResolver.SYSTEM, ServerRedirectHandler.createDnsSrvRedirectHandler(), AddressCheck.createFromService());
    private final ServerAddressResolver resolver;
    private final ServerRedirectHandler redirectHandler;
    private final AddressCheck addressCheck;

    @VisibleForTesting
    ServerNameResolver(ServerAddressResolver resolver, ServerRedirectHandler redirectHandler, AddressCheck addressCheck) {
        this.resolver = resolver;
        this.redirectHandler = redirectHandler;
        this.addressCheck = addressCheck;
    }

    public Optional<ResolvedServerAddress> resolveAddress(ServerAddress address) {
        Optional<ResolvedServerAddress> resolvedAddress = this.resolver.resolve(address);
        if (resolvedAddress.isPresent() && !this.addressCheck.isAllowed(resolvedAddress.get()) || !this.addressCheck.isAllowed(address)) {
            return Optional.empty();
        }
        Optional<ServerAddress> redirectedAddress = this.redirectHandler.lookupRedirect(address);
        if (redirectedAddress.isPresent()) {
            resolvedAddress = this.resolver.resolve(redirectedAddress.get()).filter(this.addressCheck::isAllowed);
        }
        return resolvedAddress;
    }
}

