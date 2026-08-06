/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.advancements;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public enum AdvancementWidgetType {
    OBTAINED(Identifier.withDefaultNamespace("advancements/box_obtained"), Identifier.withDefaultNamespace("advancements/task_frame_obtained"), Identifier.withDefaultNamespace("advancements/challenge_frame_obtained"), Identifier.withDefaultNamespace("advancements/goal_frame_obtained")),
    UNOBTAINED(Identifier.withDefaultNamespace("advancements/box_unobtained"), Identifier.withDefaultNamespace("advancements/task_frame_unobtained"), Identifier.withDefaultNamespace("advancements/challenge_frame_unobtained"), Identifier.withDefaultNamespace("advancements/goal_frame_unobtained"));

    private final Identifier boxSprite;
    private final Identifier taskFrameSprite;
    private final Identifier challengeFrameSprite;
    private final Identifier goalFrameSprite;

    private AdvancementWidgetType(Identifier boxSprite, Identifier taskFrameSprite, Identifier challengeFrameSprite, Identifier goalFrameSprite) {
        this.boxSprite = boxSprite;
        this.taskFrameSprite = taskFrameSprite;
        this.challengeFrameSprite = challengeFrameSprite;
        this.goalFrameSprite = goalFrameSprite;
    }

    public Identifier boxSprite() {
        return this.boxSprite;
    }

    public Identifier frameSprite(AdvancementType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case AdvancementType.TASK -> this.taskFrameSprite;
            case AdvancementType.CHALLENGE -> this.challengeFrameSprite;
            case AdvancementType.GOAL -> this.goalFrameSprite;
        };
    }
}

