/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.realmsclient.client;

import com.mojang.realmsclient.client.RealmsClientConfig;
import com.mojang.realmsclient.exception.RealmsHttpException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class Request<T extends Request<T>> {
    protected HttpURLConnection connection;
    private boolean connected;
    protected String url;
    private static final int DEFAULT_READ_TIMEOUT = 60000;
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final String NETWORK_PROTOCOL_KEY = "X-NetworkProtocolVersion";
    private static final String IS_SNAPSHOT_KEY = "Is-Prerelease";
    private static final String COOKIE_KEY = "Cookie";

    public Request(String url, int connectTimeout, int readTimeout) {
        try {
            this.url = url;
            Proxy proxy = RealmsClientConfig.getProxy();
            this.connection = proxy != null ? (HttpURLConnection)new URL(url).openConnection(proxy) : (HttpURLConnection)new URL(url).openConnection();
            this.connection.setConnectTimeout(connectTimeout);
            this.connection.setReadTimeout(readTimeout);
        }
        catch (MalformedURLException e) {
            throw new RealmsHttpException(e.getMessage(), e);
        }
        catch (IOException e) {
            throw new RealmsHttpException(e.getMessage(), e);
        }
    }

    public void cookie(String key, String value) {
        Request.cookie(this.connection, key, value);
    }

    public static void cookie(HttpURLConnection connection, String key, String value) {
        String cookie = connection.getRequestProperty(COOKIE_KEY);
        if (cookie == null) {
            connection.setRequestProperty(COOKIE_KEY, key + "=" + value);
        } else {
            connection.setRequestProperty(COOKIE_KEY, cookie + ";" + key + "=" + value);
        }
    }

    public void addVersionHeaders(int networkProtocol, boolean isSnapshot) {
        this.connection.addRequestProperty(NETWORK_PROTOCOL_KEY, String.valueOf(networkProtocol));
        this.connection.addRequestProperty(IS_SNAPSHOT_KEY, String.valueOf(isSnapshot));
    }

    public int getRetryAfterHeader() {
        return Request.getRetryAfterHeader(this.connection);
    }

    public static int getRetryAfterHeader(HttpURLConnection connection) {
        String pauseTime = connection.getHeaderField("Retry-After");
        try {
            return Integer.valueOf(pauseTime);
        }
        catch (Exception ignored) {
            return 5;
        }
    }

    public int responseCode() {
        try {
            this.connect();
            return this.connection.getResponseCode();
        }
        catch (Exception e) {
            throw new RealmsHttpException(e.getMessage(), e);
        }
    }

    public String text() {
        try {
            this.connect();
            String result = this.responseCode() >= 400 ? this.read(this.connection.getErrorStream()) : this.read(this.connection.getInputStream());
            this.dispose();
            return result;
        }
        catch (IOException e) {
            throw new RealmsHttpException(e.getMessage(), e);
        }
    }

    private String read(@Nullable InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        InputStreamReader streamReader = new InputStreamReader(in, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        int x = streamReader.read();
        while (x != -1) {
            sb.append((char)x);
            x = streamReader.read();
        }
        return sb.toString();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void dispose() {
        byte[] bytes = new byte[1024];
        try {
            InputStream in = this.connection.getInputStream();
            while (in.read(bytes) > 0) {
            }
            in.close();
        }
        catch (Exception ignore) {
            InputStream errorStream;
            block13: {
                errorStream = this.connection.getErrorStream();
                if (errorStream != null) break block13;
                return;
            }
            try {
                while (errorStream.read(bytes) > 0) {
                }
                errorStream.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        finally {
            if (this.connection != null) {
                this.connection.disconnect();
            }
        }
    }

    protected T connect() {
        if (this.connected) {
            return (T)this;
        }
        T t = this.doConnect();
        this.connected = true;
        return t;
    }

    protected abstract T doConnect();

    public static Request<?> get(String url) {
        return new Get(url, 5000, 60000);
    }

    public static Request<?> get(String url, int connectTimeoutMillis, int readTimeoutMillis) {
        return new Get(url, connectTimeoutMillis, readTimeoutMillis);
    }

    public static Request<?> post(String uri, String content) {
        return new Post(uri, content, 5000, 60000);
    }

    public static Request<?> post(String uri, String content, int connectTimeoutMillis, int readTimeoutMillis) {
        return new Post(uri, content, connectTimeoutMillis, readTimeoutMillis);
    }

    public static Request<?> delete(String url) {
        return new Delete(url, 5000, 60000);
    }

    public static Request<?> put(String url, String content) {
        return new Put(url, content, 5000, 60000);
    }

    public static Request<?> put(String url, String content, int connectTimeoutMillis, int readTimeoutMillis) {
        return new Put(url, content, connectTimeoutMillis, readTimeoutMillis);
    }

    public String getHeader(String header) {
        return Request.getHeader(this.connection, header);
    }

    public static String getHeader(HttpURLConnection connection, String header) {
        try {
            return connection.getHeaderField(header);
        }
        catch (Exception ignored) {
            return "";
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Get
    extends Request<Get> {
        public Get(String uri, int connectTimeout, int readTimeout) {
            super(uri, connectTimeout, readTimeout);
        }

        @Override
        public Get doConnect() {
            try {
                this.connection.setDoInput(true);
                this.connection.setDoOutput(true);
                this.connection.setUseCaches(false);
                this.connection.setRequestMethod("GET");
                return this;
            }
            catch (Exception e) {
                throw new RealmsHttpException(e.getMessage(), e);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Post
    extends Request<Post> {
        private final String content;

        public Post(String uri, String content, int connectTimeout, int readTimeout) {
            super(uri, connectTimeout, readTimeout);
            this.content = content;
        }

        @Override
        public Post doConnect() {
            try {
                if (this.content != null) {
                    this.connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                }
                this.connection.setDoInput(true);
                this.connection.setDoOutput(true);
                this.connection.setUseCaches(false);
                this.connection.setRequestMethod("POST");
                OutputStream out = this.connection.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                writer.write(this.content);
                writer.close();
                out.flush();
                return this;
            }
            catch (Exception e) {
                throw new RealmsHttpException(e.getMessage(), e);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Delete
    extends Request<Delete> {
        public Delete(String uri, int connectTimeout, int readTimeout) {
            super(uri, connectTimeout, readTimeout);
        }

        @Override
        public Delete doConnect() {
            try {
                this.connection.setDoOutput(true);
                this.connection.setRequestMethod("DELETE");
                this.connection.connect();
                return this;
            }
            catch (Exception e) {
                throw new RealmsHttpException(e.getMessage(), e);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Put
    extends Request<Put> {
        private final String content;

        public Put(String uri, String content, int connectTimeout, int readTimeout) {
            super(uri, connectTimeout, readTimeout);
            this.content = content;
        }

        @Override
        public Put doConnect() {
            try {
                if (this.content != null) {
                    this.connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                }
                this.connection.setDoOutput(true);
                this.connection.setDoInput(true);
                this.connection.setRequestMethod("PUT");
                OutputStream out = this.connection.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                writer.write(this.content);
                writer.close();
                out.flush();
                return this;
            }
            catch (Exception e) {
                throw new RealmsHttpException(e.getMessage(), e);
            }
        }
    }
}

