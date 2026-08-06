/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonObject
 *  com.mojang.authlib.GameProfile
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerTextFilter;
import net.minecraft.server.network.TextFilter;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

public class LegacyTextFilter
extends ServerTextFilter {
    private static final String ENDPOINT = "v1/chat";
    private final URL joinEndpoint;
    private final JoinOrLeaveEncoder joinEncoder;
    private final URL leaveEndpoint;
    private final JoinOrLeaveEncoder leaveEncoder;
    private final String authKey;

    private LegacyTextFilter(URL chatEndpoint, ServerTextFilter.MessageEncoder chatEncoder, URL joinEndpoint, JoinOrLeaveEncoder joinEncoder, URL leaveEndpoint, JoinOrLeaveEncoder leaveEncoder, String authKey, ServerTextFilter.IgnoreStrategy chatIgnoreStrategy, ExecutorService workerPool) {
        super(chatEndpoint, chatEncoder, chatIgnoreStrategy, workerPool);
        this.joinEndpoint = joinEndpoint;
        this.joinEncoder = joinEncoder;
        this.leaveEndpoint = leaveEndpoint;
        this.leaveEncoder = leaveEncoder;
        this.authKey = authKey;
    }

    public static @Nullable ServerTextFilter createTextFilterFromConfig(String config) {
        try {
            ServerTextFilter.MessageEncoder chatEncoder;
            JsonObject parsedConfig = GsonHelper.parse(config);
            URI host = new URI(GsonHelper.getAsString(parsedConfig, "apiServer"));
            String key = GsonHelper.getAsString(parsedConfig, "apiKey");
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Missing API key");
            }
            int ruleId = GsonHelper.getAsInt(parsedConfig, "ruleId", 1);
            String serverId = GsonHelper.getAsString(parsedConfig, "serverId", "");
            String roomId = GsonHelper.getAsString(parsedConfig, "roomId", "Java:Chat");
            int hashesToDrop = GsonHelper.getAsInt(parsedConfig, "hashesToDrop", -1);
            int maxConcurrentRequests = GsonHelper.getAsInt(parsedConfig, "maxConcurrentRequests", 7);
            JsonObject endpoints = GsonHelper.getAsJsonObject(parsedConfig, "endpoints", null);
            String chatEndpointConfig = LegacyTextFilter.getEndpointFromConfig(endpoints, "chat", ENDPOINT);
            boolean isLegacyChatEndpoint = chatEndpointConfig.equals(ENDPOINT);
            URL chatEndpoint = host.resolve("/" + chatEndpointConfig).toURL();
            URL joinEndpoint = LegacyTextFilter.getEndpoint(host, endpoints, "join", "v1/join");
            URL leaveEndpoint = LegacyTextFilter.getEndpoint(host, endpoints, "leave", "v1/leave");
            JoinOrLeaveEncoder commonJoinOrLeaveEncoder = user -> {
                JsonObject object = new JsonObject();
                object.addProperty("server", serverId);
                object.addProperty("room", roomId);
                object.addProperty("user_id", user.id().toString());
                object.addProperty("user_display_name", user.name());
                return object;
            };
            if (isLegacyChatEndpoint) {
                chatEncoder = (sender, message) -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("rule", (Number)ruleId);
                    object.addProperty("server", serverId);
                    object.addProperty("room", roomId);
                    object.addProperty("player", sender.id().toString());
                    object.addProperty("player_display_name", sender.name());
                    object.addProperty("text", message);
                    object.addProperty("language", "*");
                    return object;
                };
            } else {
                String ruleIdStr = String.valueOf(ruleId);
                chatEncoder = (sender, message) -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("rule_id", ruleIdStr);
                    object.addProperty("category", serverId);
                    object.addProperty("subcategory", roomId);
                    object.addProperty("user_id", sender.id().toString());
                    object.addProperty("user_display_name", sender.name());
                    object.addProperty("text", message);
                    object.addProperty("language", "*");
                    return object;
                };
            }
            ServerTextFilter.IgnoreStrategy ignoreStrategy = ServerTextFilter.IgnoreStrategy.select(hashesToDrop);
            ExecutorService workerPool = LegacyTextFilter.createWorkerPool(maxConcurrentRequests);
            String encodedKey = Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.US_ASCII));
            return new LegacyTextFilter(chatEndpoint, chatEncoder, joinEndpoint, commonJoinOrLeaveEncoder, leaveEndpoint, commonJoinOrLeaveEncoder, encodedKey, ignoreStrategy, workerPool);
        }
        catch (Exception e) {
            LOGGER.warn("Failed to parse chat filter config {}", (Object)config, (Object)e);
            return null;
        }
    }

    @Override
    public TextFilter createContext(GameProfile gameProfile) {
        return new ServerTextFilter.PlayerContext(this, gameProfile){
            final /* synthetic */ LegacyTextFilter this$0;
            {
                LegacyTextFilter legacyTextFilter = this$0;
                Objects.requireNonNull(legacyTextFilter);
                this.this$0 = legacyTextFilter;
                super(this$0, profile);
            }

            @Override
            public void join() {
                this.this$0.processJoinOrLeave(this.profile, this.this$0.joinEndpoint, this.this$0.joinEncoder, this.streamExecutor);
            }

            @Override
            public void leave() {
                this.this$0.processJoinOrLeave(this.profile, this.this$0.leaveEndpoint, this.this$0.leaveEncoder, this.streamExecutor);
            }
        };
    }

    private void processJoinOrLeave(GameProfile user, URL endpoint, JoinOrLeaveEncoder encoder, Executor executor) {
        executor.execute(() -> {
            JsonObject object = encoder.encode(user);
            try {
                this.processRequest(object, endpoint);
            }
            catch (Exception e) {
                LOGGER.warn("Failed to send join/leave packet to {} for player {}", new Object[]{endpoint, user, e});
            }
        });
    }

    private void processRequest(JsonObject payload, URL url) throws IOException {
        HttpURLConnection connection = this.makeRequest(payload, url);
        try (InputStream is = connection.getInputStream();){
            this.drainStream(is);
        }
    }

    @Override
    protected void setAuthorizationProperty(HttpURLConnection connection) {
        connection.setRequestProperty("Authorization", "Basic " + this.authKey);
    }

    @Override
    protected FilteredText filterText(String message, ServerTextFilter.IgnoreStrategy ignoreStrategy, JsonObject result) {
        boolean response = GsonHelper.getAsBoolean(result, "response", false);
        if (response) {
            return FilteredText.passThrough(message);
        }
        String filteredMessage = GsonHelper.getAsString(result, "hashed", null);
        if (filteredMessage == null) {
            return FilteredText.fullyFiltered(message);
        }
        JsonArray removedChars = GsonHelper.getAsJsonArray(result, "hashes");
        FilterMask mask = this.parseMask(message, removedChars, ignoreStrategy);
        return new FilteredText(message, mask);
    }

    @FunctionalInterface
    private static interface JoinOrLeaveEncoder {
        public JsonObject encode(GameProfile var1);
    }
}

