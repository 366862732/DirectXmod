/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.scores;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

public interface ScoreHolder {
    public static final String WILDCARD_NAME = "*";
    public static final ScoreHolder WILDCARD = new ScoreHolder(){

        @Override
        public String getScoreboardName() {
            return ScoreHolder.WILDCARD_NAME;
        }
    };

    public String getScoreboardName();

    default public @Nullable Component getDisplayName() {
        return null;
    }

    default public Component getFeedbackDisplayName() {
        Component displayName = this.getDisplayName();
        if (displayName != null) {
            return displayName.copy().withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(this.getScoreboardName()))));
        }
        return Component.literal(this.getScoreboardName());
    }

    public static ScoreHolder forNameOnly(final String name) {
        if (name.equals(WILDCARD_NAME)) {
            return WILDCARD;
        }
        final MutableComponent feedbackName = Component.literal(name);
        return new ScoreHolder(){

            @Override
            public String getScoreboardName() {
                return name;
            }

            @Override
            public Component getFeedbackDisplayName() {
                return feedbackName;
            }
        };
    }

    public static ScoreHolder fromGameProfile(GameProfile profile) {
        final String name = profile.name();
        return new ScoreHolder(){

            @Override
            public String getScoreboardName() {
                return name;
            }
        };
    }
}

