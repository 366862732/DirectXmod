/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  io.netty.handler.ssl.SslContext
 *  io.netty.handler.ssl.SslContextBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.server.jsonrpc.security;

import com.mojang.logging.LogUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.slf4j.Logger;

public class JsonRpcSslContextProvider {
    private static final String PASSWORD_ENV_VARIABLE_KEY = "MINECRAFT_MANAGEMENT_TLS_KEYSTORE_PASSWORD";
    private static final String PASSWORD_SYSTEM_PROPERTY_KEY = "management.tls.keystore.password";
    private static final Logger log = LogUtils.getLogger();

    public static SslContext createFrom(String keystorePath, String keystorePasswordFromServerProperties) throws Exception {
        if (keystorePath.isEmpty()) {
            throw new IllegalArgumentException("TLS is enabled but keystore is not configured");
        }
        File file = new File(keystorePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Supplied keystore is not a file or does not exist: '" + keystorePath + "'");
        }
        String keystorePassword = JsonRpcSslContextProvider.getKeystorePassword(keystorePasswordFromServerProperties);
        return JsonRpcSslContextProvider.loadKeystoreFromPath(file, keystorePassword);
    }

    private static String getKeystorePassword(String keystorePasswordFromServerProperties) {
        String keystorePassword = System.getenv().get(PASSWORD_ENV_VARIABLE_KEY);
        if (keystorePassword != null) {
            return keystorePassword;
        }
        String systemPropertyKeystorePassword = System.getProperty(PASSWORD_SYSTEM_PROPERTY_KEY, null);
        if (systemPropertyKeystorePassword != null) {
            return systemPropertyKeystorePassword;
        }
        return keystorePasswordFromServerProperties;
    }

    private static SslContext loadKeystoreFromPath(File keyStoreFile, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream keystoreStream = new FileInputStream(keyStoreFile);){
            keyStore.load(keystoreStream, password.toCharArray());
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password.toCharArray());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        return SslContextBuilder.forServer((KeyManagerFactory)keyManagerFactory).trustManager(trustManagerFactory).build();
    }

    public static void printInstructions() {
        log.info("To use TLS for the management server, please follow these steps:");
        log.info("1. Set the server property 'management-server-tls-enabled' to 'true' to enable TLS");
        log.info("2. Create a keystore file of type PKCS12 containing your server certificate and private key");
        log.info("3. Set the server property 'management-server-tls-keystore' to the path of your keystore file");
        log.info("4. Set the keystore password via the environment variable 'MINECRAFT_MANAGEMENT_TLS_KEYSTORE_PASSWORD', or system property 'management.tls.keystore.password', or server property 'management-server-tls-keystore-password'");
        log.info("5. Restart the server to apply the changes.");
    }
}

