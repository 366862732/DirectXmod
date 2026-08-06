/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.village;

public interface ReputationEventType {
    public static final ReputationEventType ZOMBIE_VILLAGER_CURED = ReputationEventType.register("zombie_villager_cured");
    public static final ReputationEventType GOLEM_KILLED = ReputationEventType.register("golem_killed");
    public static final ReputationEventType VILLAGER_HURT = ReputationEventType.register("villager_hurt");
    public static final ReputationEventType VILLAGER_KILLED = ReputationEventType.register("villager_killed");
    public static final ReputationEventType TRADE = ReputationEventType.register("trade");

    public static ReputationEventType register(final String name) {
        return new ReputationEventType(){

            public String toString() {
                return name;
            }
        };
    }
}

