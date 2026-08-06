/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.internal.Streams
 *  com.google.gson.stream.JsonWriter
 *  com.mojang.authlib.GameProfile
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.network;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.LegacyTextFilter;
import net.minecraft.server.network.PlayerSafetyServiceTextFilter;
import net.minecraft.server.network.TextFilter;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ConsecutiveExecutor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class ServerTextFilter
implements AutoCloseable {
    protected static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger WORKER_COUNT = new AtomicInteger(1);
    private static final ThreadFactory THREAD_FACTORY = runnable -> new Thread(runnable, "Chat-Filter-Worker-" + WORKER_COUNT.getAndIncrement());
    private final URL chatEndpoint;
    private final MessageEncoder chatEncoder;
    private final IgnoreStrategy chatIgnoreStrategy;
    private final ExecutorService workerPool;

    protected static ExecutorService createWorkerPool(int maxConcurrentRequests) {
        return Executors.newFixedThreadPool(maxConcurrentRequests, THREAD_FACTORY);
    }

    protected ServerTextFilter(URL chatEndpoint, MessageEncoder chatEncoder, IgnoreStrategy chatIgnoreStrategy, ExecutorService workerPool) {
        this.chatIgnoreStrategy = chatIgnoreStrategy;
        this.workerPool = workerPool;
        this.chatEndpoint = chatEndpoint;
        this.chatEncoder = chatEncoder;
    }

    protected static URL getEndpoint(URI host, @Nullable JsonObject source, String id, String def) throws MalformedURLException {
        String endpointConfig = ServerTextFilter.getEndpointFromConfig(source, id, def);
        return host.resolve("/" + endpointConfig).toURL();
    }

    protected static String getEndpointFromConfig(@Nullable JsonObject source, String id, String def) {
        return source != null ? GsonHelper.getAsString(source, id, def) : def;
    }

    public static @Nullable ServerTextFilter createFromConfig(DedicatedServerProperties config) {
        String textFilteringConfig = config.textFilteringConfig;
        if (StringUtil.isBlank(textFilteringConfig)) {
            return null;
        }
        return switch (config.textFilteringVersion) {
            case 0 -> LegacyTextFilter.createTextFilterFromConfig(textFilteringConfig);
            case 1 -> PlayerSafetyServiceTextFilter.createTextFilterFromConfig(textFilteringConfig);
            default -> {
                LOGGER.warn("Could not create text filter - unsupported text filtering version used");
                yield null;
            }
        };
    }

    protected CompletableFuture<FilteredText> requestMessageProcessing(GameProfile sender, String message, IgnoreStrategy ignoreStrategy, Executor executor) {
        if (message.isEmpty()) {
            return CompletableFuture.completedFuture(FilteredText.EMPTY);
        }
        return CompletableFuture.supplyAsync(() -> {
            JsonObject object = this.chatEncoder.encode(sender, message);
            try {
                JsonObject result = this.processRequestResponse(object, this.chatEndpoint);
                return this.filterText(message, ignoreStrategy, result);
            }
            catch (Exception e) {
                LOGGER.warn("Failed to validate message '{}'", (Object)message, (Object)e);
                return FilteredText.fullyFiltered(message);
            }
        }, executor);
    }

    protected abstract FilteredText filterText(String var1, IgnoreStrategy var2, JsonObject var3);

    protected FilterMask parseMask(String message, JsonArray removedChars, IgnoreStrategy ignoreStrategy) {
        if (removedChars.isEmpty()) {
            return FilterMask.PASS_THROUGH;
        }
        if (ignoreStrategy.shouldIgnore(message, removedChars.size())) {
            return FilterMask.FULLY_FILTERED;
        }
        FilterMask mask = new FilterMask(message.length());
        for (int i = 0; i < removedChars.size(); ++i) {
            mask.setFiltered(removedChars.get(i).getAsInt());
        }
        return mask;
    }

    @Override
    public void close() {
        this.workerPool.shutdownNow();
    }

    protected void drainStream(InputStream input) throws IOException {
        byte[] trashcan = new byte[1024];
        while (input.read(trashcan) != -1) {
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private JsonObject processRequestResponse(JsonObject payload, URL url) throws IOException {
        HttpURLConnection connection = this.makeRequest(payload, url);
        try (InputStream is = connection.getInputStream();){
            JsonObject jsonObject;
            if (connection.getResponseCode() == 204) {
                JsonObject jsonObject2 = new JsonObject();
                return jsonObject2;
            }
            try {
                jsonObject = LenientJsonParser.parse(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            catch (Throwable throwable) {
                this.drainStream(is);
                throw throwable;
            }
            this.drainStream(is);
            return jsonObject;
        }
    }

    protected HttpURLConnection makeRequest(JsonObject payload, URL url) throws IOException {
        HttpURLConnection connection = this.getURLConnection(url);
        this.setAuthorizationProperty(connection);
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
             JsonWriter jsonWriter = new JsonWriter((Writer)writer);){
            Streams.write((JsonElement)payload, (JsonWriter)jsonWriter);
        }
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new RequestFailedException(responseCode + " " + connection.getResponseMessage());
        }
        return connection;
    }

    protected abstract void setAuthorizationProperty(HttpURLConnection var1);

    protected int connectionReadTimeout() {
        return 2000;
    }

    protected HttpURLConnection getURLConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(this.connectionReadTimeout());
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Minecraft server" + SharedConstants.getCurrentVersion().name());
        return connection;
    }

    public TextFilter createContext(GameProfile gameProfile) {
        return new PlayerContext(this, gameProfile);
    }

    @FunctionalInterface
    public static interface IgnoreStrategy {
        public static final IgnoreStrategy NEVER_IGNORE = (message, removedCharCount) -> false;
        public static final IgnoreStrategy IGNORE_FULLY_FILTERED = (message, removedCharCount) -> message.length() == removedCharCount;

        public static IgnoreStrategy ignoreOverThreshold(int threshold) {
            return (message, removedCharCount) -> removedCharCount >= threshold;
        }

        public static IgnoreStrategy select(int hashesToDrop) {
            return switch (hashesToDrop) {
                case -1 -> NEVER_IGNORE;
                case 0 -> IGNORE_FULLY_FILTERED;
                default -> IgnoreStrategy.ignoreOverThreshold(hashesToDrop);
            };
        }

        public boolean shouldIgnore(String var1, int var2);
    }

    @FunctionalInterface
    protected static interface MessageEncoder {
        public JsonObject encode(GameProfile var1, String var2);
    }

    protected static class RequestFailedException
    extends RuntimeException {
        protected RequestFailedException(String message) {
            super(message);
        }
    }

    protected class PlayerContext
    implements TextFilter {
        protected final GameProfile profile;
        protected final Executor streamExecutor;
        final /* synthetic */ ServerTextFilter this$0;

        protected PlayerContext(ServerTextFilter this$0, GameProfile profile) {
            ServerTextFilter serverTextFilter = this$0;
            Objects.requireNonNull(serverTextFilter);
            this.this$0 = serverTextFilter;
            this.profile = profile;
            ConsecutiveExecutor streamProcessor = new ConsecutiveExecutor(this$0.workerPool, "chat stream for " + profile.name());
            this.streamExecutor = streamProcessor::schedule;
        }

        @Override
        public CompletableFuture<List<FilteredText>> processMessageBundle(List<String> messages) {
            List requests = (List)messages.stream().map(message -> this.this$0.requestMessageProcessing(this.profile, (String)message, this.this$0.chatIgnoreStrategy, this.streamExecutor)).collect(ImmutableList.toImmutableList());
            return Util.sequenceFailFast(requests).exceptionally(e -> ImmutableList.of());
        }

        @Override
        public CompletableFuture<FilteredText> processStreamMessage(String message) {
            return this.this$0.requestMessageProcessing(this.profile, message, this.this$0.chatIgnoreStrategy, this.streamExecutor);
        }
    }
}

