/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.microsoft.aad.msal4j.ClientCredentialFactory
 *  com.microsoft.aad.msal4j.ClientCredentialParameters
 *  com.microsoft.aad.msal4j.ConfidentialClientApplication
 *  com.microsoft.aad.msal4j.ConfidentialClientApplication$Builder
 *  com.microsoft.aad.msal4j.IAuthenticationResult
 *  com.microsoft.aad.msal4j.IClientCertificate
 *  com.microsoft.aad.msal4j.IClientCredential
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCertificate;
import com.microsoft.aad.msal4j.IClientCredential;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerTextFilter;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

public class PlayerSafetyServiceTextFilter
extends ServerTextFilter {
    private final ConfidentialClientApplication client;
    private final ClientCredentialParameters clientParameters;
    private final Set<String> fullyFilteredEvents;
    private final int connectionReadTimeoutMs;

    private PlayerSafetyServiceTextFilter(URL chatEndpoint, ServerTextFilter.MessageEncoder chatEncoder, ServerTextFilter.IgnoreStrategy chatIgnoreStrategy, ExecutorService workerPool, ConfidentialClientApplication client, ClientCredentialParameters clientParameters, Set<String> fullyFilteredEvents, int connectionReadTimeoutMs) {
        super(chatEndpoint, chatEncoder, chatIgnoreStrategy, workerPool);
        this.client = client;
        this.clientParameters = clientParameters;
        this.fullyFilteredEvents = fullyFilteredEvents;
        this.connectionReadTimeoutMs = connectionReadTimeoutMs;
    }

    public static @Nullable ServerTextFilter createTextFilterFromConfig(String textFilteringConfig) {
        ConfidentialClientApplication client;
        IClientCertificate certificate;
        URL chatEndpoint;
        JsonObject parsedConfig = GsonHelper.parse(textFilteringConfig);
        URI host = URI.create(GsonHelper.getAsString(parsedConfig, "apiServer"));
        String apiPath = GsonHelper.getAsString(parsedConfig, "apiPath");
        String scope = GsonHelper.getAsString(parsedConfig, "scope");
        String serverId = GsonHelper.getAsString(parsedConfig, "serverId", "");
        String applicationId = GsonHelper.getAsString(parsedConfig, "applicationId");
        String tenantId = GsonHelper.getAsString(parsedConfig, "tenantId");
        String roomId = GsonHelper.getAsString(parsedConfig, "roomId", "Java:Chat");
        String certificatePath = GsonHelper.getAsString(parsedConfig, "certificatePath");
        String certificatePassword = GsonHelper.getAsString(parsedConfig, "certificatePassword", "");
        int hashesToDrop = GsonHelper.getAsInt(parsedConfig, "hashesToDrop", -1);
        int maxConcurrentRequests = GsonHelper.getAsInt(parsedConfig, "maxConcurrentRequests", 7);
        JsonArray fullyFilteredEvents = GsonHelper.getAsJsonArray(parsedConfig, "fullyFilteredEvents");
        HashSet<String> fullyFilteredEventsSet = new HashSet<String>();
        fullyFilteredEvents.forEach(elements -> fullyFilteredEventsSet.add(GsonHelper.convertToString(elements, "filteredEvent")));
        int connectionReadTimeoutMs = GsonHelper.getAsInt(parsedConfig, "connectionReadTimeoutMs", 2000);
        try {
            chatEndpoint = host.resolve(apiPath).toURL();
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        ServerTextFilter.MessageEncoder chatEncoder = (sender, message) -> {
            JsonObject object = new JsonObject();
            object.addProperty("userId", sender.id().toString());
            object.addProperty("userDisplayName", sender.name());
            object.addProperty("server", serverId);
            object.addProperty("room", roomId);
            object.addProperty("area", "JavaChatRealms");
            object.addProperty("data", message);
            object.addProperty("language", "*");
            return object;
        };
        ServerTextFilter.IgnoreStrategy ignoreStrategy = ServerTextFilter.IgnoreStrategy.select(hashesToDrop);
        ExecutorService workerPool = PlayerSafetyServiceTextFilter.createWorkerPool(maxConcurrentRequests);
        try (InputStream inputStream = Files.newInputStream(Path.of(certificatePath, new String[0]), new OpenOption[0]);){
            certificate = ClientCredentialFactory.createFromCertificate((InputStream)inputStream, (String)certificatePassword);
        }
        catch (Exception e) {
            LOGGER.warn("Failed to open certificate file");
            return null;
        }
        try {
            client = ((ConfidentialClientApplication.Builder)((ConfidentialClientApplication.Builder)ConfidentialClientApplication.builder((String)applicationId, (IClientCredential)certificate).sendX5c(true).executorService(workerPool)).authority(String.format(Locale.ROOT, "https://login.microsoftonline.com/%s/", tenantId))).build();
        }
        catch (Exception e) {
            LOGGER.warn("Failed to create confidential client application");
            return null;
        }
        ClientCredentialParameters parameters = ClientCredentialParameters.builder(Set.of(scope)).build();
        return new PlayerSafetyServiceTextFilter(chatEndpoint, chatEncoder, ignoreStrategy, workerPool, client, parameters, fullyFilteredEventsSet, connectionReadTimeoutMs);
    }

    private IAuthenticationResult aquireIAuthenticationResult() {
        return (IAuthenticationResult)this.client.acquireToken(this.clientParameters).join();
    }

    @Override
    protected void setAuthorizationProperty(HttpURLConnection connection) {
        IAuthenticationResult authenticationResult = this.aquireIAuthenticationResult();
        connection.setRequestProperty("Authorization", "Bearer " + authenticationResult.accessToken());
    }

    @Override
    protected FilteredText filterText(String message, ServerTextFilter.IgnoreStrategy ignoreStrategy, JsonObject response) {
        JsonObject result = GsonHelper.getAsJsonObject(response, "result", null);
        if (result == null) {
            return FilteredText.fullyFiltered(message);
        }
        boolean filtered = GsonHelper.getAsBoolean(result, "filtered", true);
        if (!filtered) {
            return FilteredText.passThrough(message);
        }
        JsonArray events = GsonHelper.getAsJsonArray(result, "events", new JsonArray());
        for (JsonElement element : events) {
            JsonObject object = element.getAsJsonObject();
            String event = GsonHelper.getAsString(object, "id", "");
            if (!this.fullyFilteredEvents.contains(event)) continue;
            return FilteredText.fullyFiltered(message);
        }
        JsonArray redactedTextIndices = GsonHelper.getAsJsonArray(result, "redactedTextIndex", new JsonArray());
        return new FilteredText(message, this.parseMask(message, redactedTextIndices, ignoreStrategy));
    }

    @Override
    protected int connectionReadTimeout() {
        return this.connectionReadTimeoutMs;
    }
}

