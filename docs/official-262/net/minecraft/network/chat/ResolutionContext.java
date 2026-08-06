/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.chat;

import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.contents.objects.ObjectInfo;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public record ResolutionContext(@Nullable CommandSourceStack source, @Nullable Entity defaultScoreboardEntity, Predicate<ObjectInfo> objectInfoValidator, int depthLimit, LimitBehavior depthLimitBehavior) {
    public @Nullable ObjectInfo validate(ObjectInfo description) {
        return this.objectInfoValidator.test(description) ? description : null;
    }

    public static ResolutionContext create(CommandSourceStack source) {
        return ResolutionContext.builder().withSource(source).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static enum LimitBehavior {
        DISCARD_REMAINING,
        STOP_PROCESSING_AND_COPY_REMAINING;

    }

    public static class Builder {
        private @Nullable CommandSourceStack source;
        private @Nullable Entity defaultScoreboardEntity;
        private Predicate<ObjectInfo> objectInfoValidator = objectInfo -> true;
        private int depthLimit = 100;
        private LimitBehavior depthLimitBehavior = LimitBehavior.STOP_PROCESSING_AND_COPY_REMAINING;

        public Builder withSource(CommandSourceStack source) {
            this.source = source;
            this.defaultScoreboardEntity = source.getEntity();
            return this;
        }

        public Builder withEntityOverride(@Nullable Entity defaultScoreboardEntity) {
            this.defaultScoreboardEntity = defaultScoreboardEntity;
            return this;
        }

        public Builder withObjectInfoValidator(Predicate<ObjectInfo> objectInfoValidator) {
            this.objectInfoValidator = objectInfoValidator;
            return this;
        }

        public Builder setDepthLimit(int depthLimit) {
            this.depthLimit = depthLimit;
            return this;
        }

        public Builder setDepthLimitBehavior(LimitBehavior behavior) {
            this.depthLimitBehavior = behavior;
            return this;
        }

        public ResolutionContext build() {
            return new ResolutionContext(this.source, this.defaultScoreboardEntity, this.objectInfoValidator, this.depthLimit, this.depthLimitBehavior);
        }
    }
}

