/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.telemetry.events;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.TelemetryPropertyMap;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class WorldLoadEvent {
    private boolean eventSent;
    private @Nullable TelemetryProperty.GameMode gameMode;
    private @Nullable String serverBrand;
    private final @Nullable String minigameName;

    public WorldLoadEvent(@Nullable String minigameName) {
        this.minigameName = minigameName;
    }

    public void addProperties(TelemetryPropertyMap.Builder properties) {
        if (this.serverBrand != null) {
            properties.put(TelemetryProperty.SERVER_MODDED, !this.serverBrand.equals("vanilla"));
        }
        properties.put(TelemetryProperty.SERVER_TYPE, this.getServerType());
    }

    private TelemetryProperty.ServerType getServerType() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server != null && server.isRealm()) {
            return TelemetryProperty.ServerType.REALM;
        }
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            return TelemetryProperty.ServerType.LOCAL;
        }
        return TelemetryProperty.ServerType.OTHER;
    }

    public boolean send(TelemetryEventSender eventSender, boolean lastChance) {
        if (this.eventSent || this.gameMode == null || this.serverBrand == null && !lastChance) {
            return false;
        }
        this.eventSent = true;
        eventSender.send(TelemetryEventType.WORLD_LOADED, properties -> {
            properties.put(TelemetryProperty.GAME_MODE, this.gameMode);
            if (this.minigameName != null) {
                properties.put(TelemetryProperty.REALMS_MAP_CONTENT, this.minigameName);
            }
        });
        return true;
    }

    public boolean wasSent() {
        return this.eventSent;
    }

    public void setGameMode(GameType type, boolean hardcore) {
        this.gameMode = switch (type) {
            default -> throw new MatchException(null, null);
            case GameType.SURVIVAL -> {
                if (hardcore) {
                    yield TelemetryProperty.GameMode.HARDCORE;
                }
                yield TelemetryProperty.GameMode.SURVIVAL;
            }
            case GameType.CREATIVE -> TelemetryProperty.GameMode.CREATIVE;
            case GameType.ADVENTURE -> TelemetryProperty.GameMode.ADVENTURE;
            case GameType.SPECTATOR -> TelemetryProperty.GameMode.SPECTATOR;
        };
    }

    public void setServerBrand(String serverBrand) {
        this.serverBrand = serverBrand;
    }
}

