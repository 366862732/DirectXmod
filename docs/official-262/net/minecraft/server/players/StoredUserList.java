/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.common.io.Files
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class StoredUserList<K, V extends StoredUserEntry<K>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private final Map<String, V> map = Maps.newHashMap();
    protected final NotificationService notificationService;

    public StoredUserList(File file, NotificationService notificationService) {
        this.file = file;
        this.notificationService = notificationService;
    }

    public File getFile() {
        return this.file;
    }

    public boolean add(V infos) {
        String keyForUser = this.getKeyForUser(((StoredUserEntry)infos).getUser());
        StoredUserEntry previous = (StoredUserEntry)this.map.get(keyForUser);
        if (infos.equals(previous)) {
            return false;
        }
        this.map.put(keyForUser, infos);
        try {
            this.save();
        }
        catch (IOException e) {
            LOGGER.warn("Could not save the list after adding a user.", (Throwable)e);
        }
        return true;
    }

    public @Nullable V get(K user) {
        this.removeExpired();
        return (V)((StoredUserEntry)this.map.get(this.getKeyForUser(user)));
    }

    public boolean remove(K user) {
        StoredUserEntry removed = (StoredUserEntry)this.map.remove(this.getKeyForUser(user));
        if (removed == null) {
            return false;
        }
        try {
            this.save();
        }
        catch (IOException e) {
            LOGGER.warn("Could not save the list after removing a user.", (Throwable)e);
        }
        return true;
    }

    public boolean remove(StoredUserEntry<K> infos) {
        return this.remove(Objects.requireNonNull(infos.getUser()));
    }

    public void clear() {
        this.map.clear();
        try {
            this.save();
        }
        catch (IOException e) {
            LOGGER.warn("Could not save the list after removing a user.", (Throwable)e);
        }
    }

    public String[] getUserList() {
        return this.map.keySet().toArray(new String[0]);
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    protected String getKeyForUser(K user) {
        return user.toString();
    }

    protected boolean contains(K user) {
        return this.map.containsKey(this.getKeyForUser(user));
    }

    private void removeExpired() {
        ArrayList toRemove = Lists.newArrayList();
        for (StoredUserEntry entry : this.map.values()) {
            if (!entry.hasExpired()) continue;
            toRemove.add(entry.getUser());
        }
        for (Object user : toRemove) {
            this.map.remove(this.getKeyForUser(user));
        }
    }

    protected abstract StoredUserEntry<K> createEntry(JsonObject var1);

    public Collection<V> getEntries() {
        return this.map.values();
    }

    public void save() throws IOException {
        JsonArray result = new JsonArray();
        this.map.values().stream().map(entry -> Util.make(new JsonObject(), entry::serialize)).forEach(arg_0 -> ((JsonArray)result).add(arg_0));
        try (BufferedWriter writer = Files.newWriter((File)this.file, (Charset)StandardCharsets.UTF_8);){
            GSON.toJson((JsonElement)result, GSON.newJsonWriter((Writer)writer));
        }
    }

    public void load() throws IOException {
        if (!this.file.exists()) {
            return;
        }
        try (BufferedReader reader = Files.newReader((File)this.file, (Charset)StandardCharsets.UTF_8);){
            this.map.clear();
            JsonArray contents = (JsonArray)GSON.fromJson((Reader)reader, JsonArray.class);
            if (contents == null) {
                return;
            }
            for (JsonElement element : contents) {
                JsonObject object = GsonHelper.convertToJsonObject(element, "entry");
                StoredUserEntry<K> entry = this.createEntry(object);
                if (entry.getUser() == null) continue;
                this.map.put(this.getKeyForUser(entry.getUser()), entry);
            }
        }
    }
}

