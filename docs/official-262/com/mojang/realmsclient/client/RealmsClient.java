/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.mojang.logging.LogUtils
 *  com.mojang.util.UndashedUuid
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClientConfig;
import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.client.Request;
import com.mojang.realmsclient.dto.BackupList;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.OutboundPlayer;
import com.mojang.realmsclient.dto.PendingInvite;
import com.mojang.realmsclient.dto.PendingInvitesList;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.PreferredRegionsDto;
import com.mojang.realmsclient.dto.RealmsConfigurationDto;
import com.mojang.realmsclient.dto.RealmsDescriptionDto;
import com.mojang.realmsclient.dto.RealmsJoinInformation;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerList;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RealmsSetting;
import com.mojang.realmsclient.dto.RealmsSlotUpdateDto;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.RealmsWorldResetDto;
import com.mojang.realmsclient.dto.RegionDataDto;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.RegionSelectionPreferenceDto;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsHttpException;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.util.UploadTokenCache;
import com.mojang.util.UndashedUuid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@net.fabricmc.api.Environment(value=EnvType.CLIENT)
public class RealmsClient {
    public static final Environment ENVIRONMENT = Optional.ofNullable(System.getenv("realms.environment")).or(() -> Optional.ofNullable(System.getProperty("realms.environment"))).flatMap(Environment::byName).orElse(Environment.PRODUCTION);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile @Nullable RealmsClient realmsClientInstance = null;
    private final CompletableFuture<Set<String>> featureFlags;
    private final String sessionId;
    private final String username;
    private final Minecraft minecraft;
    private static final String WORLDS_RESOURCE_PATH = "worlds";
    private static final String INVITES_RESOURCE_PATH = "invites";
    private static final String MCO_RESOURCE_PATH = "mco";
    private static final String SUBSCRIPTION_RESOURCE = "subscriptions";
    private static final String ACTIVITIES_RESOURCE = "activities";
    private static final String OPS_RESOURCE = "ops";
    private static final String REGIONS_RESOURCE = "regions/ping/stat";
    private static final String PREFERRED_REGION_RESOURCE = "regions/preferredRegions";
    private static final String TRIALS_RESOURCE = "trial";
    private static final String NOTIFICATIONS_RESOURCE = "notifications";
    private static final String FEATURE_FLAGS_RESOURCE = "feature/v1";
    private static final String PATH_LIST_ALL_REALMS = "/listUserWorldsOfType/any";
    private static final String PATH_CREATE_SNAPSHOT_REALM = "/$PARENT_WORLD_ID/createPrereleaseRealm";
    private static final String PATH_SNAPSHOT_ELIGIBLE_REALMS = "/listPrereleaseEligibleWorlds";
    private static final String PATH_INITIALIZE = "/$WORLD_ID/initialize";
    private static final String PATH_GET_LIVESTATS = "/liveplayerlist";
    private static final String PATH_GET_SUBSCRIPTION = "/$WORLD_ID";
    private static final String PATH_OP = "/$WORLD_ID/$PROFILE_UUID";
    private static final String PATH_PUT_INTO_MINIGAMES_MODE = "/minigames/$MINIGAME_ID/$WORLD_ID";
    private static final String PATH_AVAILABLE = "/available";
    private static final String PATH_TEMPLATES = "/templates/$WORLD_TYPE";
    private static final String PATH_WORLD_JOIN = "/v1/$ID/join/pc";
    private static final String PATH_WORLD_GET = "/$ID";
    private static final String PATH_WORLD_INVITES = "/$WORLD_ID";
    private static final String PATH_WORLD_UNINVITE = "/$WORLD_ID/invite/$UUID";
    private static final String PATH_PENDING_INVITES = "/pending";
    private static final String PATH_ACCEPT_INVITE = "/accept/$INVITATION_ID";
    private static final String PATH_REJECT_INVITE = "/reject/$INVITATION_ID";
    private static final String PATH_UNINVITE_MYSELF = "/$WORLD_ID";
    private static final String PATH_WORLD_CONFIGURE = "/$WORLD_ID/configuration";
    private static final String PATH_SLOT = "/$WORLD_ID/slot/$SLOT_ID";
    private static final String PATH_WORLD_OPEN = "/$WORLD_ID/open";
    private static final String PATH_WORLD_CLOSE = "/$WORLD_ID/close";
    private static final String PATH_WORLD_RESET = "/$WORLD_ID/reset";
    private static final String PATH_DELETE_WORLD = "/$WORLD_ID";
    private static final String PATH_WORLD_BACKUPS = "/$WORLD_ID/backups";
    private static final String PATH_WORLD_DOWNLOAD = "/$WORLD_ID/slot/$SLOT_ID/download";
    private static final String PATH_WORLD_UPLOAD = "/$WORLD_ID/backups/upload";
    private static final String PATH_CLIENT_COMPATIBLE = "/client/compatible";
    private static final String PATH_TOS_AGREED = "/tos/agreed";
    private static final String PATH_NEWS = "/v1/news";
    private static final String PATH_MARK_NOTIFICATIONS_SEEN = "/seen";
    private static final String PATH_DISMISS_NOTIFICATIONS = "/dismiss";
    private static final GuardedSerializer GSON = new GuardedSerializer();

    public static RealmsClient getOrCreate() {
        Minecraft minecraft = Minecraft.getInstance();
        return RealmsClient.getOrCreate(minecraft);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static RealmsClient getOrCreate(Minecraft minecraft) {
        String username = minecraft.getUser().getName();
        String sessionId = minecraft.getUser().getSessionId();
        RealmsClient realmsClient = realmsClientInstance;
        if (realmsClient != null) {
            return realmsClient;
        }
        Class<RealmsClient> clazz = RealmsClient.class;
        synchronized (RealmsClient.class) {
            RealmsClient rc = realmsClientInstance;
            if (rc != null) {
                // ** MonitorExit[var4_4] (shouldn't be in output)
                return rc;
            }
            realmsClientInstance = rc = new RealmsClient(sessionId, username, minecraft);
            // ** MonitorExit[var4_4] (shouldn't be in output)
            return rc;
        }
    }

    private RealmsClient(String sessionId, String username, Minecraft minecraft) {
        this.sessionId = sessionId;
        this.username = username;
        this.minecraft = minecraft;
        RealmsClientConfig.setProxy(minecraft.getProxy());
        this.featureFlags = CompletableFuture.supplyAsync(this::fetchFeatureFlags, Util.nonCriticalIoPool());
    }

    public Set<String> getFeatureFlags() {
        return this.featureFlags.join();
    }

    private Set<String> fetchFeatureFlags() {
        if (Minecraft.getInstance().isOfflineDeveloperMode()) {
            return Set.of();
        }
        String asciiUrl = RealmsClient.url(FEATURE_FLAGS_RESOURCE, null, false);
        try {
            String returnJson = this.execute(Request.get(asciiUrl, 5000, 10000));
            JsonArray object = LenientJsonParser.parse(returnJson).getAsJsonArray();
            Set<String> featureFlags = object.asList().stream().map(JsonElement::getAsString).collect(Collectors.toSet());
            LOGGER.debug("Fetched Realms feature flags: {}", featureFlags);
            return featureFlags;
        }
        catch (RealmsServiceException e) {
            LOGGER.error("Failed to fetch Realms feature flags", (Throwable)e);
        }
        catch (Exception e) {
            LOGGER.error("Could not parse Realms feature flags", (Throwable)e);
        }
        return Set.of();
    }

    public RealmsServerList listRealms() throws RealmsServiceException {
        Object asciiUrl = this.url(WORLDS_RESOURCE_PATH);
        if (RealmsMainScreen.isSnapshot()) {
            asciiUrl = (String)asciiUrl + PATH_LIST_ALL_REALMS;
        }
        String json = this.execute(Request.get((String)asciiUrl));
        return RealmsServerList.parse(GSON, json);
    }

    public List<RealmsServer> listSnapshotEligibleRealms() throws RealmsServiceException {
        String asciiUrl = this.url("worlds/listPrereleaseEligibleWorlds");
        String json = this.execute(Request.get(asciiUrl));
        return RealmsServerList.parse(GSON, json).servers();
    }

    public RealmsServer createSnapshotRealm(Long parentId) throws RealmsServiceException {
        String parentIdString = String.valueOf(parentId);
        String url = this.url(WORLDS_RESOURCE_PATH + PATH_CREATE_SNAPSHOT_REALM.replace("$PARENT_WORLD_ID", parentIdString));
        return RealmsServer.parse(GSON, this.execute(Request.post(url, parentIdString)));
    }

    public List<RealmsNotification> getNotifications() throws RealmsServiceException {
        String endpoint = this.url(NOTIFICATIONS_RESOURCE);
        String responseJson = this.execute(Request.get(endpoint));
        return RealmsNotification.parseList(responseJson);
    }

    private static JsonArray uuidListToJsonArray(List<UUID> uuids) {
        JsonArray array = new JsonArray();
        for (UUID uuid : uuids) {
            if (uuid == null) continue;
            array.add(uuid.toString());
        }
        return array;
    }

    public void notificationsSeen(List<UUID> notificationUuids) throws RealmsServiceException {
        String endpoint = this.url("notifications/seen");
        this.execute(Request.post(endpoint, GSON.toJson((JsonElement)RealmsClient.uuidListToJsonArray(notificationUuids))));
    }

    public void notificationsDismiss(List<UUID> notificationUuids) throws RealmsServiceException {
        String endpoint = this.url("notifications/dismiss");
        this.execute(Request.post(endpoint, GSON.toJson((JsonElement)RealmsClient.uuidListToJsonArray(notificationUuids))));
    }

    public RealmsServer getOwnRealm(long realmId) throws RealmsServiceException {
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_GET.replace("$ID", String.valueOf(realmId)));
        String json = this.execute(Request.get(asciiUrl));
        return RealmsServer.parse(GSON, json);
    }

    public PreferredRegionsDto getPreferredRegionSelections() throws RealmsServiceException {
        String asciiUrl = this.url(PREFERRED_REGION_RESOURCE);
        String json = this.execute(Request.get(asciiUrl));
        try {
            PreferredRegionsDto preferredRegionsDto = GSON.fromJson(json, PreferredRegionsDto.class);
            if (preferredRegionsDto == null) {
                return PreferredRegionsDto.empty();
            }
            Set regionsInResponse = preferredRegionsDto.regionData().stream().map(RegionDataDto::region).collect(Collectors.toSet());
            for (RealmsRegion region : RealmsRegion.values()) {
                if (region == RealmsRegion.INVALID_REGION || regionsInResponse.contains((Object)region)) continue;
                LOGGER.debug("No realms region matching {} in server response", (Object)region);
            }
            return preferredRegionsDto;
        }
        catch (Exception e) {
            LOGGER.error("Could not parse PreferredRegionSelections", (Throwable)e);
            return PreferredRegionsDto.empty();
        }
    }

    public RealmsServerPlayerLists getLiveStats() throws RealmsServiceException {
        String asciiUrl = this.url("activities/liveplayerlist");
        String json = this.execute(Request.get(asciiUrl));
        return RealmsServerPlayerLists.parse(json);
    }

    public RealmsJoinInformation join(long realmId) throws RealmsServiceException {
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_JOIN.replace("$ID", "" + realmId));
        String json = this.execute(Request.get(asciiUrl, 5000, 30000));
        return RealmsJoinInformation.parse(GSON, json);
    }

    public void initializeRealm(long realmId, String name, String motd) throws RealmsServiceException {
        RealmsDescriptionDto realmsDescription = new RealmsDescriptionDto(name, motd);
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_INITIALIZE.replace("$WORLD_ID", String.valueOf(realmId)));
        String json = GSON.toJson(realmsDescription);
        this.execute(Request.post(asciiUrl, json, 5000, 10000));
    }

    public boolean hasParentalConsent() throws RealmsServiceException {
        String asciiUrl = this.url("mco/available");
        String json = this.execute(Request.get(asciiUrl));
        return Boolean.parseBoolean(json);
    }

    public CompatibleVersionResponse clientCompatible() throws RealmsServiceException {
        CompatibleVersionResponse result;
        String asciiUrl = this.url("mco/client/compatible");
        String response = this.execute(Request.get(asciiUrl));
        try {
            result = CompatibleVersionResponse.valueOf(response);
        }
        catch (IllegalArgumentException ignored) {
            throw new RealmsServiceException(RealmsError.CustomError.unknownCompatibilityResponse(response));
        }
        return result;
    }

    public void uninvite(long realmId, UUID profileId) throws RealmsServiceException {
        String asciiUrl = this.url(INVITES_RESOURCE_PATH + PATH_WORLD_UNINVITE.replace("$WORLD_ID", String.valueOf(realmId)).replace("$UUID", UndashedUuid.toString((UUID)profileId)));
        this.execute(Request.delete(asciiUrl));
    }

    public void uninviteMyselfFrom(long realmId) throws RealmsServiceException {
        String asciiUrl = this.url(INVITES_RESOURCE_PATH + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(realmId)));
        this.execute(Request.delete(asciiUrl));
    }

    public List<PlayerInfo> invite(long realmId, String profileName) throws RealmsServiceException {
        OutboundPlayer playerInfo = new OutboundPlayer();
        playerInfo.name = profileName;
        String asciiUrl = this.url(INVITES_RESOURCE_PATH + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(realmId)));
        String json = this.execute(Request.post(asciiUrl, GSON.toJson(playerInfo)));
        return RealmsServer.parse((GuardedSerializer)RealmsClient.GSON, (String)json).players;
    }

    public BackupList backupsFor(long realmId) throws RealmsServiceException {
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_BACKUPS.replace("$WORLD_ID", String.valueOf(realmId)));
        String json = this.execute(Request.get(asciiUrl));
        return BackupList.parse(json);
    }

    public void updateConfiguration(long realmId, String name, String description, @Nullable RegionSelectionPreferenceDto regionSelectionPreference, int slotId, RealmsWorldOptions options, List<RealmsSetting> settings) throws RealmsServiceException {
        RegionSelectionPreferenceDto preferenceDto = regionSelectionPreference != null ? regionSelectionPreference : new RegionSelectionPreferenceDto(RegionSelectionPreference.DEFAULT_SELECTION, null);
        RealmsDescriptionDto realmsDescription = new RealmsDescriptionDto(name, description);
        RealmsSlotUpdateDto slotUpdateDto = new RealmsSlotUpdateDto(slotId, options, RealmsSetting.isHardcore(settings));
        RealmsConfigurationDto realmsConfiguration = new RealmsConfigurationDto(slotUpdateDto, settings, preferenceDto, realmsDescription);
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_CONFIGURE.replace("$WORLD_ID", String.valueOf(realmId)));
        this.execute(Request.post(asciiUrl, GSON.toJson(realmsConfiguration)));
    }

    public void updateSlot(long realmId, int slotId, RealmsWorldOptions options, List<RealmsSetting> settings) throws RealmsServiceException {
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_SLOT.replace("$WORLD_ID", String.valueOf(realmId)).replace("$SLOT_ID", String.valueOf(slotId)));
        String json = GSON.toJson(new RealmsSlotUpdateDto(slotId, options, RealmsSetting.isHardcore(settings)));
        this.execute(Request.post(asciiUrl, json));
    }

    public boolean switchSlot(long realmId, int slot) throws RealmsServiceException {
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_SLOT.replace("$WORLD_ID", String.valueOf(realmId)).replace("$SLOT_ID", String.valueOf(slot)));
        String json = this.execute(Request.put(asciiUrl, ""));
        return Boolean.valueOf(json);
    }

    public void restoreWorld(long realmId, String backupId) throws RealmsServiceException {
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_BACKUPS.replace("$WORLD_ID", String.valueOf(realmId)), "backupId=" + backupId);
        this.execute(Request.put(asciiUrl, "", 40000, 600000));
    }

    public WorldTemplatePaginatedList fetchWorldTemplates(int page, int pageSize, RealmsServer.WorldType type) throws RealmsServiceException {
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_TEMPLATES.replace("$WORLD_TYPE", type.toString()), String.format(Locale.ROOT, "page=%d&pageSize=%d", page, pageSize));
        String json = this.execute(Request.get(asciiUrl));
        return WorldTemplatePaginatedList.parse(json);
    }

    public Boolean putIntoMinigameMode(long realmId, String minigameId) throws RealmsServiceException {
        String path = PATH_PUT_INTO_MINIGAMES_MODE.replace("$MINIGAME_ID", minigameId).replace("$WORLD_ID", String.valueOf(realmId));
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + path);
        return Boolean.valueOf(this.execute(Request.put(asciiUrl, "")));
    }

    public Ops op(long realmId, UUID profileId) throws RealmsServiceException {
        String path = PATH_OP.replace("$WORLD_ID", String.valueOf(realmId)).replace("$PROFILE_UUID", UndashedUuid.toString((UUID)profileId));
        String asciiUrl = this.url(OPS_RESOURCE + path);
        return Ops.parse(this.execute(Request.post(asciiUrl, "")));
    }

    public Ops deop(long realmId, UUID profileId) throws RealmsServiceException {
        String path = PATH_OP.replace("$WORLD_ID", String.valueOf(realmId)).replace("$PROFILE_UUID", UndashedUuid.toString((UUID)profileId));
        String asciiUrl = this.url(OPS_RESOURCE + path);
        return Ops.parse(this.execute(Request.delete(asciiUrl)));
    }

    public Boolean open(long realmId) throws RealmsServiceException {
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_OPEN.replace("$WORLD_ID", String.valueOf(realmId)));
        String json = this.execute(Request.put(asciiUrl, ""));
        return Boolean.valueOf(json);
    }

    public Boolean close(long realmId) throws RealmsServiceException {
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_CLOSE.replace("$WORLD_ID", String.valueOf(realmId)));
        String json = this.execute(Request.put(asciiUrl, ""));
        return Boolean.valueOf(json);
    }

    public Boolean resetWorldWithTemplate(long realmId, String worldTemplateId) throws RealmsServiceException {
        RealmsWorldResetDto worldReset = new RealmsWorldResetDto(null, Long.valueOf(worldTemplateId), -1, false, Set.of());
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_RESET.replace("$WORLD_ID", String.valueOf(realmId)));
        String json = this.execute(Request.post(asciiUrl, GSON.toJson(worldReset), 30000, 80000));
        return Boolean.valueOf(json);
    }

    public Subscription subscriptionFor(long realmId) throws RealmsServiceException {
        String asciiUrl = this.url(SUBSCRIPTION_RESOURCE + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(realmId)));
        String json = this.execute(Request.get(asciiUrl));
        return Subscription.parse(json);
    }

    public int pendingInvitesCount() throws RealmsServiceException {
        return this.pendingInvites().pendingInvites().size();
    }

    public PendingInvitesList pendingInvites() throws RealmsServiceException {
        String asciiUrl = this.url("invites/pending");
        String json = this.execute(Request.get(asciiUrl));
        PendingInvitesList list = PendingInvitesList.parse(json);
        list.pendingInvites().removeIf(this::isBlocked);
        return list;
    }

    private boolean isBlocked(PendingInvite invite) {
        return this.minecraft.getPlayerSocialManager().isBlocked(invite.realmOwnerUuid());
    }

    public void acceptInvitation(String invitationId) throws RealmsServiceException {
        String asciiUrl = this.url(INVITES_RESOURCE_PATH + PATH_ACCEPT_INVITE.replace("$INVITATION_ID", invitationId));
        this.execute(Request.put(asciiUrl, ""));
    }

    public WorldDownload requestDownloadInfo(long realmId, int slotId) throws RealmsServiceException {
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_DOWNLOAD.replace("$WORLD_ID", String.valueOf(realmId)).replace("$SLOT_ID", String.valueOf(slotId)));
        String json = this.execute(Request.get(asciiUrl));
        return WorldDownload.parse(json);
    }

    public @Nullable UploadInfo requestUploadInfo(long realmId) throws RealmsServiceException {
        String uploadToken;
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_UPLOAD.replace("$WORLD_ID", String.valueOf(realmId)));
        UploadInfo uploadInfo = UploadInfo.parse(this.execute(Request.put(asciiUrl, UploadInfo.createRequest(uploadToken = UploadTokenCache.get(realmId)))));
        if (uploadInfo != null) {
            UploadTokenCache.put(realmId, uploadInfo.token());
        }
        return uploadInfo;
    }

    public void rejectInvitation(String invitationId) throws RealmsServiceException {
        String asciiUrl = this.url(INVITES_RESOURCE_PATH + PATH_REJECT_INVITE.replace("$INVITATION_ID", invitationId));
        this.execute(Request.put(asciiUrl, ""));
    }

    public void agreeToTos() throws RealmsServiceException {
        String asciiUrl = this.url("mco/tos/agreed");
        this.execute(Request.post(asciiUrl, ""));
    }

    public RealmsNews getNews() throws RealmsServiceException {
        String asciiUrl = this.url("mco/v1/news");
        String returnJson = this.execute(Request.get(asciiUrl, 5000, 10000));
        return RealmsNews.parse(returnJson);
    }

    public void sendPingResults(PingResult pingResult) throws RealmsServiceException {
        String asciiUrl = this.url(REGIONS_RESOURCE);
        this.execute(Request.post(asciiUrl, GSON.toJson(pingResult)));
    }

    public Boolean trialAvailable() throws RealmsServiceException {
        String asciiUrl = this.url(TRIALS_RESOURCE);
        String json = this.execute(Request.get(asciiUrl));
        return Boolean.valueOf(json);
    }

    public void deleteRealm(long realmId) throws RealmsServiceException {
        String asciiUrl = this.url(WORLDS_RESOURCE_PATH + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(realmId)));
        this.execute(Request.delete(asciiUrl));
    }

    private String url(String path) throws RealmsServiceException {
        return this.url(path, null);
    }

    private String url(String path, @Nullable String queryString) {
        return RealmsClient.url(path, queryString, this.getFeatureFlags().contains("realms_in_aks"));
    }

    private static String url(String path, @Nullable String queryString, boolean useAlternativeURL) {
        try {
            return new URI(RealmsClient.ENVIRONMENT.protocol, useAlternativeURL ? RealmsClient.ENVIRONMENT.alternativeUrl : RealmsClient.ENVIRONMENT.baseUrl, "/" + path, queryString, null).toASCIIString();
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(path, e);
        }
    }

    private String execute(Request<?> request) throws RealmsServiceException {
        request.cookie("sid", this.sessionId);
        request.cookie("user", this.username);
        WorldVersion version = SharedConstants.getCurrentVersion();
        request.cookie("version", version.name());
        request.addVersionHeaders(version.protocolVersion(), RealmsMainScreen.isSnapshot());
        try {
            int responseCode = request.responseCode();
            if (responseCode == 503 || responseCode == 277) {
                int pauseTime = request.getRetryAfterHeader();
                throw new RetryCallException(pauseTime, responseCode);
            }
            String responseText = request.text();
            if (responseCode < 200 || responseCode >= 300) {
                if (responseCode == 401) {
                    String authenticationHeader = request.getHeader("WWW-Authenticate");
                    LOGGER.info("Could not authorize you against Realms server: {}", (Object)authenticationHeader);
                    throw new RealmsServiceException(new RealmsError.AuthenticationError(authenticationHeader));
                }
                String contentType = request.connection.getContentType();
                if (contentType != null && contentType.startsWith("text/html")) {
                    throw new RealmsServiceException(RealmsError.CustomError.htmlPayload(responseCode, responseText));
                }
                RealmsError error = RealmsError.parse(responseCode, responseText);
                throw new RealmsServiceException(error);
            }
            return responseText;
        }
        catch (RealmsHttpException e) {
            throw new RealmsServiceException(RealmsError.CustomError.connectivityError(e));
        }
    }

    @net.fabricmc.api.Environment(value=EnvType.CLIENT)
    public static enum CompatibleVersionResponse {
        COMPATIBLE,
        OUTDATED,
        OTHER;

    }

    @net.fabricmc.api.Environment(value=EnvType.CLIENT)
    public static enum Environment {
        PRODUCTION("pc.realms.minecraft.net", "java.frontendlegacy.realms.minecraft-services.net", "https"),
        STAGE("pc-stage.realms.minecraft.net", "java.frontendlegacy.stage-c2a40e62.realms.minecraft-services.net", "https"),
        LOCAL("localhost:8080", "localhost:8080", "http");

        public final String baseUrl;
        public final String alternativeUrl;
        public final String protocol;

        private Environment(String baseUrl, String alternativeUrl, String protocol) {
            this.baseUrl = baseUrl;
            this.alternativeUrl = alternativeUrl;
            this.protocol = protocol;
        }

        public static Optional<Environment> byName(String name) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "production" -> Optional.of(PRODUCTION);
                case "local" -> Optional.of(LOCAL);
                case "stage", "staging" -> Optional.of(STAGE);
                default -> Optional.empty();
            };
        }
    }
}

