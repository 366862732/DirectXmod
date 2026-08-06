/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.Maps
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 */
package net.minecraft.world.scores.criteria;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatType;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.scores.TeamColor;

public class ObjectiveCriteria {
    private static final Map<String, ObjectiveCriteria> CUSTOM_CRITERIA = Maps.newHashMap();
    private static final Map<String, ObjectiveCriteria> CRITERIA_CACHE = Maps.newHashMap();
    public static final Codec<ObjectiveCriteria> CODEC = Codec.STRING.comapFlatMap(name -> ObjectiveCriteria.byName(name).map(DataResult::success).orElse(DataResult.error(() -> "No scoreboard criteria with name: " + name)), ObjectiveCriteria::getName);
    public static final ObjectiveCriteria DUMMY = ObjectiveCriteria.registerCustom("dummy");
    public static final ObjectiveCriteria TRIGGER = ObjectiveCriteria.registerCustom("trigger");
    public static final ObjectiveCriteria DEATH_COUNT = ObjectiveCriteria.registerCustom("deathCount");
    public static final ObjectiveCriteria KILL_COUNT_PLAYERS = ObjectiveCriteria.registerCustom("playerKillCount");
    public static final ObjectiveCriteria KILL_COUNT_ALL = ObjectiveCriteria.registerCustom("totalKillCount");
    public static final ObjectiveCriteria HEALTH = ObjectiveCriteria.registerCustom("health", true, RenderType.HEARTS);
    public static final ObjectiveCriteria FOOD = ObjectiveCriteria.registerCustom("food", true, RenderType.INTEGER);
    public static final ObjectiveCriteria AIR = ObjectiveCriteria.registerCustom("air", true, RenderType.INTEGER);
    public static final ObjectiveCriteria ARMOR = ObjectiveCriteria.registerCustom("armor", true, RenderType.INTEGER);
    public static final ObjectiveCriteria EXPERIENCE = ObjectiveCriteria.registerCustom("xp", true, RenderType.INTEGER);
    public static final ObjectiveCriteria LEVEL = ObjectiveCriteria.registerCustom("level", true, RenderType.INTEGER);
    public static final Map<TeamColor, ObjectiveCriteria> TEAM_KILL = ObjectiveCriteria.registerForEveryTeamColor(format -> "teamkill." + format.getSerializedName());
    public static final Map<TeamColor, ObjectiveCriteria> KILLED_BY_TEAM = ObjectiveCriteria.registerForEveryTeamColor(format -> "killedByTeam." + format.getSerializedName());
    private final String name;
    private final boolean readOnly;
    private final RenderType renderType;

    private static Map<TeamColor, ObjectiveCriteria> registerForEveryTeamColor(Function<TeamColor, String> idFactory) {
        EnumMap<TeamColor, ObjectiveCriteria> result = new EnumMap<TeamColor, ObjectiveCriteria>(TeamColor.class);
        for (TeamColor value : TeamColor.values()) {
            String id = idFactory.apply(value);
            result.put(value, ObjectiveCriteria.registerCustom(id));
        }
        return result;
    }

    private static ObjectiveCriteria registerCustom(String name, boolean readOnly, RenderType renderType) {
        ObjectiveCriteria result = new ObjectiveCriteria(name, readOnly, renderType);
        CUSTOM_CRITERIA.put(name, result);
        return result;
    }

    private static ObjectiveCriteria registerCustom(String name) {
        return ObjectiveCriteria.registerCustom(name, false, RenderType.INTEGER);
    }

    protected ObjectiveCriteria(String name) {
        this(name, false, RenderType.INTEGER);
    }

    protected ObjectiveCriteria(String name, boolean readOnly, RenderType renderType) {
        this.name = name;
        this.readOnly = readOnly;
        this.renderType = renderType;
        CRITERIA_CACHE.put(name, this);
    }

    public static Set<String> getCustomCriteriaNames() {
        return ImmutableSet.copyOf(CUSTOM_CRITERIA.keySet());
    }

    public static Optional<ObjectiveCriteria> byName(String name) {
        ObjectiveCriteria value = CRITERIA_CACHE.get(name);
        if (value != null) {
            return Optional.of(value);
        }
        int colonPos = name.indexOf(58);
        if (colonPos < 0) {
            return Optional.empty();
        }
        return BuiltInRegistries.STAT_TYPE.getOptional(Identifier.bySeparator(name.substring(0, colonPos), '.')).flatMap(statType -> ObjectiveCriteria.getStat(statType, Identifier.bySeparator(name.substring(colonPos + 1), '.')));
    }

    private static <T> Optional<ObjectiveCriteria> getStat(StatType<T> statType, Identifier key) {
        return statType.getRegistry().getOptional(key).map(statType::get);
    }

    public String getName() {
        return this.name;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public RenderType getDefaultRenderType() {
        return this.renderType;
    }

    public static enum RenderType implements StringRepresentable
    {
        INTEGER("integer"),
        HEARTS("hearts");

        private final String id;
        public static final StringRepresentable.EnumCodec<RenderType> CODEC;

        private RenderType(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }

        public static RenderType byId(String key) {
            return CODEC.byName(key, INTEGER);
        }

        static {
            CODEC = StringRepresentable.fromEnum(RenderType::values);
        }
    }
}

