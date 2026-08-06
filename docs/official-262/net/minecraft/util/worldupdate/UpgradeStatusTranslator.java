/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.worldupdate;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.worldupdate.UpgradeProgress;

public class UpgradeStatusTranslator {
    private final Map<DataFixTypes, Messages> messages = Util.make(new EnumMap(DataFixTypes.class), map -> {
        map.put(DataFixTypes.CHUNK, Messages.create("chunks"));
        map.put(DataFixTypes.ENTITY_CHUNK, Messages.create("entities"));
        map.put(DataFixTypes.POI_CHUNK, Messages.create("poi"));
    });
    private static final Component FAILED = Component.translatable("optimizeWorld.stage.failed");
    private static final Component COUNTING = Component.translatable("optimizeWorld.stage.counting");
    private static final Component UPGRADING = Component.translatable("optimizeWorld.stage.upgrading");

    public Component translate(UpgradeProgress upgradeProgress) {
        UpgradeProgress.Status status = upgradeProgress.getStatus();
        if (status == UpgradeProgress.Status.FAILED) {
            return FAILED;
        }
        if (status == UpgradeProgress.Status.COUNTING) {
            return COUNTING;
        }
        DataFixTypes dataFixType = upgradeProgress.getDataFixType();
        if (dataFixType == null) {
            return COUNTING;
        }
        Messages typeMessages = this.messages.get((Object)dataFixType);
        if (typeMessages == null) {
            return UPGRADING;
        }
        return typeMessages.forStatus(status);
    }

    public record Messages(Component upgrading, Component finished) {
        public static Messages create(String type) {
            return new Messages(Component.translatable("optimizeWorld.stage.upgrading." + type), Component.translatable("optimizeWorld.stage.finished." + type));
        }

        public Component forStatus(UpgradeProgress.Status status) {
            return switch (status) {
                case UpgradeProgress.Status.UPGRADING -> this.upgrading;
                case UpgradeProgress.Status.FINISHED -> this.finished;
                default -> throw new IllegalStateException("Invalid Status received: " + String.valueOf((Object)status));
            };
        }
    }
}

