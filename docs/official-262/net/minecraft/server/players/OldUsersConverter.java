/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.common.io.Files
 *  com.mojang.authlib.ProfileLookupCallback
 *  com.mojang.authlib.yggdrasil.ProfileNotFoundException
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.notifications.EmptyNotificationService;
import net.minecraft.server.players.BanListEntry;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class OldUsersConverter {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final File OLD_IPBANLIST = new File("banned-ips.txt");
    public static final File OLD_USERBANLIST = new File("banned-players.txt");
    public static final File OLD_OPLIST = new File("ops.txt");
    public static final File OLD_WHITELIST = new File("white-list.txt");

    private static List<String> readOldListFormat(File file, Map<String, String[]> userMap) throws IOException {
        List lines = Files.readLines((File)file, (Charset)StandardCharsets.UTF_8);
        for (String line : lines) {
            if ((line = line.trim()).startsWith("#") || line.isEmpty()) continue;
            String[] parts = line.split("\\|");
            userMap.put(parts[0].toLowerCase(Locale.ROOT), parts);
        }
        return lines;
    }

    private static void lookupPlayers(MinecraftServer server, Collection<String> names, ProfileLookupCallback callback) {
        String[] filteredNames = (String[])names.stream().filter(s -> !StringUtil.isNullOrEmpty(s)).toArray(String[]::new);
        if (server.usesAuthentication()) {
            server.services().profileRepository().findProfilesByNames(filteredNames, callback);
        } else {
            for (String name : filteredNames) {
                callback.onProfileLookupSucceeded(name, UUIDUtil.createOfflinePlayerUUID(name));
            }
        }
    }

    public static boolean convertUserBanlist(final MinecraftServer server) {
        final UserBanList bans = new UserBanList(PlayerList.USERBANLIST_FILE, new EmptyNotificationService());
        if (OLD_USERBANLIST.exists() && OLD_USERBANLIST.isFile()) {
            if (bans.getFile().exists()) {
                try {
                    bans.load();
                }
                catch (IOException e) {
                    LOGGER.warn("Could not load existing file {}", (Object)bans.getFile().getName(), (Object)e);
                }
            }
            try {
                final HashMap userMap = Maps.newHashMap();
                OldUsersConverter.readOldListFormat(OLD_USERBANLIST, userMap);
                ProfileLookupCallback callback = new ProfileLookupCallback(){

                    public void onProfileLookupSucceeded(String profileName, UUID profileId) {
                        NameAndId profile = new NameAndId(profileId, profileName);
                        server.services().nameToIdCache().add(profile);
                        String[] userDef = (String[])userMap.get(profile.name().toLowerCase(Locale.ROOT));
                        if (userDef == null) {
                            LOGGER.warn("Could not convert user banlist entry for {}", (Object)profile.name());
                            throw new ConversionError("Profile not in the conversionlist");
                        }
                        Date created = userDef.length > 1 ? OldUsersConverter.parseDate(userDef[1], null) : null;
                        String source = userDef.length > 2 ? userDef[2] : null;
                        Date expires = userDef.length > 3 ? OldUsersConverter.parseDate(userDef[3], null) : null;
                        String reason = userDef.length > 4 ? userDef[4] : null;
                        bans.add(new UserBanListEntry(profile, created, source, expires, reason));
                    }

                    public void onProfileLookupFailed(String profileName, Exception exception) {
                        LOGGER.warn("Could not lookup user banlist entry for {}", (Object)profileName, (Object)exception);
                        if (!(exception instanceof ProfileNotFoundException)) {
                            throw new ConversionError("Could not request user " + profileName + " from backend systems", exception);
                        }
                    }
                };
                OldUsersConverter.lookupPlayers(server, userMap.keySet(), callback);
                bans.save();
                OldUsersConverter.renameOldFile(OLD_USERBANLIST);
            }
            catch (IOException e) {
                LOGGER.warn("Could not read old user banlist to convert it!", (Throwable)e);
                return false;
            }
            catch (ConversionError e) {
                LOGGER.error("Conversion failed, please try again later", (Throwable)e);
                return false;
            }
            return true;
        }
        return true;
    }

    public static boolean convertIpBanlist(MinecraftServer server) {
        IpBanList ipBans = new IpBanList(PlayerList.IPBANLIST_FILE, new EmptyNotificationService());
        if (OLD_IPBANLIST.exists() && OLD_IPBANLIST.isFile()) {
            if (ipBans.getFile().exists()) {
                try {
                    ipBans.load();
                }
                catch (IOException e) {
                    LOGGER.warn("Could not load existing file {}", (Object)ipBans.getFile().getName(), (Object)e);
                }
            }
            try {
                HashMap userMap = Maps.newHashMap();
                OldUsersConverter.readOldListFormat(OLD_IPBANLIST, userMap);
                for (String key : userMap.keySet()) {
                    String[] userDef = (String[])userMap.get(key);
                    Date created = userDef.length > 1 ? OldUsersConverter.parseDate(userDef[1], null) : null;
                    String source = userDef.length > 2 ? userDef[2] : null;
                    Date expires = userDef.length > 3 ? OldUsersConverter.parseDate(userDef[3], null) : null;
                    String reason = userDef.length > 4 ? userDef[4] : null;
                    ipBans.add(new IpBanListEntry(key, created, source, expires, reason));
                }
                ipBans.save();
                OldUsersConverter.renameOldFile(OLD_IPBANLIST);
            }
            catch (IOException e) {
                LOGGER.warn("Could not parse old ip banlist to convert it!", (Throwable)e);
                return false;
            }
            return true;
        }
        return true;
    }

    public static boolean convertOpsList(final MinecraftServer server) {
        final ServerOpList opsList = new ServerOpList(PlayerList.OPLIST_FILE, new EmptyNotificationService());
        if (OLD_OPLIST.exists() && OLD_OPLIST.isFile()) {
            if (opsList.getFile().exists()) {
                try {
                    opsList.load();
                }
                catch (IOException e) {
                    LOGGER.warn("Could not load existing file {}", (Object)opsList.getFile().getName(), (Object)e);
                }
            }
            try {
                List lines = Files.readLines((File)OLD_OPLIST, (Charset)StandardCharsets.UTF_8);
                ProfileLookupCallback callback = new ProfileLookupCallback(){

                    public void onProfileLookupSucceeded(String profileName, UUID profileId) {
                        NameAndId profile = new NameAndId(profileId, profileName);
                        server.services().nameToIdCache().add(profile);
                        opsList.add(new ServerOpListEntry(profile, server.operatorUserPermissions(), false));
                    }

                    public void onProfileLookupFailed(String profileName, Exception exception) {
                        LOGGER.warn("Could not lookup oplist entry for {}", (Object)profileName, (Object)exception);
                        if (!(exception instanceof ProfileNotFoundException)) {
                            throw new ConversionError("Could not request user " + profileName + " from backend systems", exception);
                        }
                    }
                };
                OldUsersConverter.lookupPlayers(server, lines, callback);
                opsList.save();
                OldUsersConverter.renameOldFile(OLD_OPLIST);
            }
            catch (IOException e) {
                LOGGER.warn("Could not read old oplist to convert it!", (Throwable)e);
                return false;
            }
            catch (ConversionError e) {
                LOGGER.error("Conversion failed, please try again later", (Throwable)e);
                return false;
            }
            return true;
        }
        return true;
    }

    public static boolean convertWhiteList(final MinecraftServer server) {
        final UserWhiteList whitelist = new UserWhiteList(PlayerList.WHITELIST_FILE, new EmptyNotificationService());
        if (OLD_WHITELIST.exists() && OLD_WHITELIST.isFile()) {
            if (whitelist.getFile().exists()) {
                try {
                    whitelist.load();
                }
                catch (IOException e) {
                    LOGGER.warn("Could not load existing file {}", (Object)whitelist.getFile().getName(), (Object)e);
                }
            }
            try {
                List lines = Files.readLines((File)OLD_WHITELIST, (Charset)StandardCharsets.UTF_8);
                ProfileLookupCallback callback = new ProfileLookupCallback(){

                    public void onProfileLookupSucceeded(String profileName, UUID profileId) {
                        NameAndId profile = new NameAndId(profileId, profileName);
                        server.services().nameToIdCache().add(profile);
                        whitelist.add(new UserWhiteListEntry(profile));
                    }

                    public void onProfileLookupFailed(String profileName, Exception exception) {
                        LOGGER.warn("Could not lookup user whitelist entry for {}", (Object)profileName, (Object)exception);
                        if (!(exception instanceof ProfileNotFoundException)) {
                            throw new ConversionError("Could not request user " + profileName + " from backend systems", exception);
                        }
                    }
                };
                OldUsersConverter.lookupPlayers(server, lines, callback);
                whitelist.save();
                OldUsersConverter.renameOldFile(OLD_WHITELIST);
            }
            catch (IOException e) {
                LOGGER.warn("Could not read old whitelist to convert it!", (Throwable)e);
                return false;
            }
            catch (ConversionError e) {
                LOGGER.error("Conversion failed, please try again later", (Throwable)e);
                return false;
            }
            return true;
        }
        return true;
    }

    public static @Nullable UUID convertMobOwnerIfNecessary(final MinecraftServer server, String owner) {
        if (StringUtil.isNullOrEmpty(owner) || owner.length() > 16) {
            try {
                return UUID.fromString(owner);
            }
            catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        Optional<UUID> profileId = server.services().nameToIdCache().get(owner).map(NameAndId::id);
        if (profileId.isPresent()) {
            return profileId.get();
        }
        if (server.isSingleplayer() || !server.usesAuthentication()) {
            return UUIDUtil.createOfflinePlayerUUID(owner);
        }
        final ArrayList profiles = new ArrayList();
        ProfileLookupCallback callback = new ProfileLookupCallback(){

            public void onProfileLookupSucceeded(String profileName, UUID profileId) {
                NameAndId profile = new NameAndId(profileId, profileName);
                server.services().nameToIdCache().add(profile);
                profiles.add(profile);
            }

            public void onProfileLookupFailed(String profileName, Exception exception) {
                LOGGER.warn("Could not lookup user whitelist entry for {}", (Object)profileName, (Object)exception);
            }
        };
        OldUsersConverter.lookupPlayers(server, Lists.newArrayList((Object[])new String[]{owner}), callback);
        if (!profiles.isEmpty()) {
            return ((NameAndId)profiles.getFirst()).id();
        }
        return null;
    }

    public static boolean convertPlayers(final DedicatedServer server) {
        final File worldPlayerDirectory = server.getWorldPath(LevelResource.PLAYER_OLD_DATA_DIR).toFile();
        final File worldNewPlayerDirectory = new File(worldPlayerDirectory.getParentFile(), LevelResource.PLAYER_DATA_DIR.id());
        final File unknownPlayerDirectory = new File(worldPlayerDirectory.getParentFile(), "unknownplayers");
        if (!worldPlayerDirectory.exists() || !worldPlayerDirectory.isDirectory()) {
            return true;
        }
        File[] playerFiles = worldPlayerDirectory.listFiles();
        ArrayList playerNames = Lists.newArrayList();
        for (File file : playerFiles) {
            String playerName;
            String fileName = file.getName();
            if (!fileName.toLowerCase(Locale.ROOT).endsWith(".dat") || (playerName = fileName.substring(0, fileName.length() - ".dat".length())).isEmpty()) continue;
            playerNames.add(playerName);
        }
        try {
            Object[] names = playerNames.toArray(new String[playerNames.size()]);
            ProfileLookupCallback callback = new ProfileLookupCallback(){
                final /* synthetic */ String[] val$names;
                {
                    this.val$names = stringArray;
                }

                public void onProfileLookupSucceeded(String profileName, UUID profileId) {
                    NameAndId profile = new NameAndId(profileId, profileName);
                    server.services().nameToIdCache().add(profile);
                    this.movePlayerFile(worldNewPlayerDirectory, this.getFileNameForProfile(profileName), profileId.toString());
                }

                public void onProfileLookupFailed(String profileName, Exception exception) {
                    LOGGER.warn("Could not lookup user uuid for {}", (Object)profileName, (Object)exception);
                    if (!(exception instanceof ProfileNotFoundException)) {
                        throw new ConversionError("Could not request user " + profileName + " from backend systems", exception);
                    }
                    String fileNameForProfile = this.getFileNameForProfile(profileName);
                    this.movePlayerFile(unknownPlayerDirectory, fileNameForProfile, fileNameForProfile);
                }

                private void movePlayerFile(File directory, String oldName, String newName) {
                    File oldFileName = new File(worldPlayerDirectory, oldName + ".dat");
                    File newFileName = new File(directory, newName + ".dat");
                    OldUsersConverter.ensureDirectoryExists(directory);
                    if (!oldFileName.renameTo(newFileName)) {
                        throw new ConversionError("Could not convert file for " + oldName);
                    }
                }

                private String getFileNameForProfile(String profileName) {
                    String fileName = null;
                    for (String name : this.val$names) {
                        if (name == null || !name.equalsIgnoreCase(profileName)) continue;
                        fileName = name;
                        break;
                    }
                    if (fileName == null) {
                        throw new ConversionError("Could not find the filename for " + profileName + " anymore");
                    }
                    return fileName;
                }
            };
            OldUsersConverter.lookupPlayers(server, Lists.newArrayList((Object[])names), callback);
        }
        catch (ConversionError e) {
            LOGGER.error("Conversion failed, please try again later", (Throwable)e);
            return false;
        }
        return true;
    }

    private static void ensureDirectoryExists(File directory) {
        if (directory.exists() ? !directory.isDirectory() : !directory.mkdirs()) {
            throw new ConversionError("Can't create directory " + directory.getName() + " in world save directory.");
        }
    }

    public static boolean areOldUserlistsRemoved() {
        boolean foundUserBanlist = false;
        if (OLD_USERBANLIST.exists() && OLD_USERBANLIST.isFile()) {
            foundUserBanlist = true;
        }
        boolean foundIpBanlist = false;
        if (OLD_IPBANLIST.exists() && OLD_IPBANLIST.isFile()) {
            foundIpBanlist = true;
        }
        boolean foundOpList = false;
        if (OLD_OPLIST.exists() && OLD_OPLIST.isFile()) {
            foundOpList = true;
        }
        boolean foundWhitelist = false;
        if (OLD_WHITELIST.exists() && OLD_WHITELIST.isFile()) {
            foundWhitelist = true;
        }
        if (foundUserBanlist || foundIpBanlist || foundOpList || foundWhitelist) {
            LOGGER.warn("**** FAILED TO START THE SERVER AFTER ACCOUNT CONVERSION!");
            LOGGER.warn("** please remove the following files and restart the server:");
            if (foundUserBanlist) {
                LOGGER.warn("* {}", (Object)OLD_USERBANLIST.getName());
            }
            if (foundIpBanlist) {
                LOGGER.warn("* {}", (Object)OLD_IPBANLIST.getName());
            }
            if (foundOpList) {
                LOGGER.warn("* {}", (Object)OLD_OPLIST.getName());
            }
            if (foundWhitelist) {
                LOGGER.warn("* {}", (Object)OLD_WHITELIST.getName());
            }
            return false;
        }
        return true;
    }

    private static void renameOldFile(File file) {
        File newFile = new File(file.getName() + ".converted");
        file.renameTo(newFile);
    }

    private static Date parseDate(String dateString, Date defaultValue) {
        Date parsedDate;
        try {
            parsedDate = BanListEntry.DATE_FORMAT.parse(dateString);
        }
        catch (ParseException ignored) {
            parsedDate = defaultValue;
        }
        return parsedDate;
    }

    private static class ConversionError
    extends RuntimeException {
        private ConversionError(String message, Throwable cause) {
            super(message, cause);
        }

        private ConversionError(String message) {
            super(message);
        }
    }
}

