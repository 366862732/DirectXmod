/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.quickplay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class QuickPlayLog {
    private static final QuickPlayLog INACTIVE = new QuickPlayLog(""){

        @Override
        public void log(Minecraft minecraft) {
        }

        @Override
        public void setWorldData(Type type, String id, String name) {
        }
    };
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private final Path path;
    private @Nullable QuickPlayWorld worldData;

    private QuickPlayLog(String quickPlayPath) {
        this.path = Minecraft.getInstance().gameDirectory.toPath().resolve(quickPlayPath);
    }

    public static QuickPlayLog of(@Nullable String path) {
        if (path == null) {
            return INACTIVE;
        }
        return new QuickPlayLog(path);
    }

    public void setWorldData(Type type, String id, String name) {
        this.worldData = new QuickPlayWorld(type, id, name);
    }

    public void log(Minecraft minecraft) {
        if (minecraft.gameMode == null || this.worldData == null) {
            LOGGER.error("Failed to log session for quickplay. Missing world data or gamemode");
            return;
        }
        Util.ioPool().execute(() -> {
            try {
                Files.deleteIfExists(this.path);
            }
            catch (IOException e) {
                LOGGER.error("Failed to delete quickplay log file {}", (Object)this.path, (Object)e);
            }
            QuickPlayEntry quickPlayEntry = new QuickPlayEntry(this.worldData, Instant.now(), minecraft.gameMode.getPlayerMode());
            Codec.list(QuickPlayEntry.CODEC).encodeStart((DynamicOps)JsonOps.INSTANCE, List.of(quickPlayEntry)).resultOrPartial(Util.prefix("Quick Play: ", arg_0 -> ((Logger)LOGGER).error(arg_0))).ifPresent(json -> {
                try {
                    Files.createDirectories(this.path.getParent(), new FileAttribute[0]);
                    Files.writeString(this.path, (CharSequence)GSON.toJson(json), new OpenOption[0]);
                }
                catch (IOException e) {
                    LOGGER.error("Failed to write to quickplay log file {}", (Object)this.path, (Object)e);
                }
            });
        });
    }

    @Environment(value=EnvType.CLIENT)
    private record QuickPlayWorld(Type type, String id, String name) {
        public static final MapCodec<QuickPlayWorld> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Type.CODEC.fieldOf("type").forGetter(QuickPlayWorld::type), (App)ExtraCodecs.ESCAPED_STRING.fieldOf("id").forGetter(QuickPlayWorld::id), (App)Codec.STRING.fieldOf("name").forGetter(QuickPlayWorld::name)).apply((Applicative)i, QuickPlayWorld::new));
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Type implements StringRepresentable
    {
        SINGLEPLAYER("singleplayer"),
        MULTIPLAYER("multiplayer"),
        REALMS("realms");

        private static final Codec<Type> CODEC;
        private final String name;

        private Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Type::values);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record QuickPlayEntry(QuickPlayWorld quickPlayWorld, Instant lastPlayedTime, GameType gamemode) {
        public static final Codec<QuickPlayEntry> CODEC = RecordCodecBuilder.create(i -> i.group((App)QuickPlayWorld.MAP_CODEC.forGetter(QuickPlayEntry::quickPlayWorld), (App)ExtraCodecs.INSTANT_ISO8601.fieldOf("lastPlayedTime").forGetter(QuickPlayEntry::lastPlayedTime), (App)GameType.CODEC.fieldOf("gamemode").forGetter(QuickPlayEntry::gamemode)).apply((Applicative)i, QuickPlayEntry::new));
    }
}

