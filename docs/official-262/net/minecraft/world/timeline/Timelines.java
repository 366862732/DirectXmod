/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.timeline;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.EasingType;
import net.minecraft.util.TriState;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.BooleanModifier;
import net.minecraft.world.attribute.modifier.ColorModifier;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.timeline.Timeline;

public interface Timelines {
    public static final ResourceKey<Timeline> OVERWORLD_DAY = Timelines.key("day");
    public static final ResourceKey<Timeline> MOON = Timelines.key("moon");
    public static final ResourceKey<Timeline> VILLAGER_SCHEDULE = Timelines.key("villager_schedule");
    public static final ResourceKey<Timeline> EARLY_GAME = Timelines.key("early_game");
    public static final float DAY_SKY_LIGHT_LEVEL = 15.0f;
    public static final float NIGHT_SKY_LIGHT_LEVEL = 4.0f;
    public static final int NIGHT_SKY_LIGHT_COLOR = ARGB.colorFromFloat(1.0f, 0.48f, 0.48f, 1.0f);
    public static final float NIGHT_SKY_LIGHT_FACTOR = 0.24f;
    public static final int NIGHT_SKY_COLOR_MULTIPLIER = -16777216;
    public static final int NIGHT_FOG_COLOR_MULTIPLIER_START = ARGB.colorFromFloat(1.0f, 0.05f, 0.05f, 0.09f);
    public static final int NIGHT_FOG_COLOR_MULTIPLIER_END = ARGB.colorFromFloat(1.0f, 0.09f, 0.09f, 0.09f);
    public static final int NIGHT_CLOUD_COLOR_MULTIPLIER = ARGB.colorFromFloat(1.0f, 0.1f, 0.1f, 0.15f);

    public static void bootstrap(BootstrapContext<Timeline> context) {
        HolderGetter<WorldClock> clocks = context.lookup(Registries.WORLD_CLOCK);
        Holder.Reference<WorldClock> overworldClock = clocks.getOrThrow(WorldClocks.OVERWORLD);
        EasingType skyAngleEase = EasingType.symmetricCubicBezier(0.362f, 0.241f);
        int nightStart = 12600;
        int nightEnd = 23401;
        int noon = 6000;
        context.register(OVERWORLD_DAY, Timeline.builder(overworldClock).setPeriodTicks(24000).addTimeMarker(ClockTimeMarkers.DAY, 1000, true).addTimeMarker(ClockTimeMarkers.NOON, 6000, true).addTimeMarker(ClockTimeMarkers.NIGHT, 13000, true).addTimeMarker(ClockTimeMarkers.MIDNIGHT, 18000, true).addTimeMarker(ClockTimeMarkers.WAKE_UP_FROM_SLEEP, 0).addTimeMarker(ClockTimeMarkers.ROLL_VILLAGE_SIEGE, 18000).addTrack(EnvironmentAttributes.SUN_ANGLE, track -> track.setEasing(skyAngleEase).addKeyframe(6000, Float.valueOf(360.0f)).addKeyframe(6000, Float.valueOf(0.0f))).addTrack(EnvironmentAttributes.MOON_ANGLE, track -> track.setEasing(skyAngleEase).addKeyframe(6000, Float.valueOf(540.0f)).addKeyframe(6000, Float.valueOf(180.0f))).addTrack(EnvironmentAttributes.STAR_ANGLE, track -> track.setEasing(skyAngleEase).addKeyframe(6000, Float.valueOf(360.0f)).addKeyframe(6000, Float.valueOf(0.0f))).addModifierTrack(EnvironmentAttributes.FIREFLY_BUSH_SOUNDS, BooleanModifier.OR, track -> track.addKeyframe(12600, true).addKeyframe(23401, false)).addModifierTrack(EnvironmentAttributes.FOG_COLOR, ColorModifier.MULTIPLY_RGB, track -> track.addKeyframe(133, -1).addKeyframe(11867, -1).addKeyframe(13670, NIGHT_FOG_COLOR_MULTIPLIER_START).addKeyframe(22330, NIGHT_FOG_COLOR_MULTIPLIER_END)).addModifierTrack(EnvironmentAttributes.SKY_COLOR, ColorModifier.MULTIPLY_RGB, track -> track.addKeyframe(133, -1).addKeyframe(11867, -1).addKeyframe(13670, -16777216).addKeyframe(22330, -16777216)).addModifierTrack(EnvironmentAttributes.SKY_LIGHT_COLOR, ColorModifier.MULTIPLY_RGB, track -> track.addKeyframe(730, -1).addKeyframe(11270, -1).addKeyframe(13140, NIGHT_SKY_LIGHT_COLOR).addKeyframe(22860, NIGHT_SKY_LIGHT_COLOR)).addModifierTrack(EnvironmentAttributes.SKY_LIGHT_FACTOR, FloatModifier.MULTIPLY, track -> track.addKeyframe(730, Float.valueOf(1.0f)).addKeyframe(11270, Float.valueOf(1.0f)).addKeyframe(13140, Float.valueOf(0.24f)).addKeyframe(22860, Float.valueOf(0.24f))).addModifierTrack(EnvironmentAttributes.SKY_LIGHT_LEVEL, FloatModifier.MULTIPLY, track -> track.addKeyframe(133, Float.valueOf(1.0f)).addKeyframe(11867, Float.valueOf(1.0f)).addKeyframe(13670, Float.valueOf(0.26666668f)).addKeyframe(22330, Float.valueOf(0.26666668f))).addTrack(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, track -> track.addKeyframe(71, 1609540403).addKeyframe(310, 703969843).addKeyframe(565, 117167155).addKeyframe(730, 16770355).addKeyframe(11270, 16770355).addKeyframe(11397, 83679283).addKeyframe(11522, 268028723).addKeyframe(11690, 703969843).addKeyframe(11929, 1609540403).addKeyframe(12243, -1310226637).addKeyframe(12358, -857440717).addKeyframe(12512, -371166669).addKeyframe(12613, -153261261).addKeyframe(12732, -19242189).addKeyframe(12841, -19440589).addKeyframe(13035, -321760973).addKeyframe(13252, -1043577037).addKeyframe(13775, 918435635).addKeyframe(13888, 532362547).addKeyframe(14039, 163001139).addKeyframe(14192, 0xB33333).addKeyframe(21807, 0xB23333).addKeyframe(21961, 163001139).addKeyframe(22112, 532362547).addKeyframe(22225, 918435635).addKeyframe(22748, -1043577037).addKeyframe(22965, -321760973).addKeyframe(23159, -19440589).addKeyframe(23272, -19242189).addKeyframe(23488, -371166669).addKeyframe(23642, -857440717).addKeyframe(23757, -1310226637)).addModifierTrack(EnvironmentAttributes.STAR_BRIGHTNESS, FloatModifier.MAXIMUM, track -> track.addKeyframe(92, Float.valueOf(0.037f)).addKeyframe(627, Float.valueOf(0.0f)).addKeyframe(11373, Float.valueOf(0.0f)).addKeyframe(11732, Float.valueOf(0.016f)).addKeyframe(11959, Float.valueOf(0.044f)).addKeyframe(12399, Float.valueOf(0.143f)).addKeyframe(12729, Float.valueOf(0.258f)).addKeyframe(13228, Float.valueOf(0.5f)).addKeyframe(22772, Float.valueOf(0.5f)).addKeyframe(23032, Float.valueOf(0.364f)).addKeyframe(23356, Float.valueOf(0.225f)).addKeyframe(23758, Float.valueOf(0.101f))).addModifierTrack(EnvironmentAttributes.CLOUD_COLOR, ColorModifier.MULTIPLY_ARGB, track -> track.addKeyframe(133, -1).addKeyframe(11867, -1).addKeyframe(13670, NIGHT_CLOUD_COLOR_MULTIPLIER).addKeyframe(22330, NIGHT_CLOUD_COLOR_MULTIPLIER)).addTrack(EnvironmentAttributes.EYEBLOSSOM_OPEN, track -> track.addKeyframe(12600, TriState.TRUE).addKeyframe(23401, TriState.FALSE)).addModifierTrack(EnvironmentAttributes.CREAKING_ACTIVE, BooleanModifier.OR, track -> track.addKeyframe(12600, true).addKeyframe(23401, false)).addModifierTrack(EnvironmentAttributes.TURTLE_EGG_HATCH_CHANCE, FloatModifier.MAXIMUM, track -> track.setEasing(EasingType.CONSTANT).addKeyframe(21062, Float.valueOf(1.0f)).addKeyframe(21905, Float.valueOf(0.002f))).addModifierTrack(EnvironmentAttributes.CAT_WAKING_UP_GIFT_CHANCE, FloatModifier.MAXIMUM, track -> track.setEasing(EasingType.CONSTANT).addKeyframe(362, Float.valueOf(0.0f)).addKeyframe(23667, Float.valueOf(0.7f))).addModifierTrack(EnvironmentAttributes.BEES_STAY_IN_HIVE, BooleanModifier.OR, track -> track.addKeyframe(12542, true).addKeyframe(23460, false)).addModifierTrack(EnvironmentAttributes.MONSTERS_BURN, BooleanModifier.OR, track -> track.addKeyframe(12542, false).addKeyframe(23460, true)).build());
        Timeline.Builder moonPhases = Timeline.builder(overworldClock).setPeriodTicks(24000 * MoonPhase.COUNT).addTrack(EnvironmentAttributes.MOON_PHASE, track -> {
            for (MoonPhase phase : MoonPhase.values()) {
                track.addKeyframe(phase.startTick(), phase);
            }
        }).addModifierTrack(EnvironmentAttributes.SURFACE_SLIME_SPAWN_CHANCE, FloatModifier.MAXIMUM, track -> {
            track.setEasing(EasingType.CONSTANT);
            for (MoonPhase phase : MoonPhase.values()) {
                track.addKeyframe(phase.startTick(), Float.valueOf(DimensionType.MOON_BRIGHTNESS_PER_PHASE[phase.index()] * 0.5f));
            }
        });
        context.register(MOON, moonPhases.build());
        int workStartTime = 2000;
        int totalWorkTime = 7000;
        context.register(VILLAGER_SCHEDULE, Timeline.builder(overworldClock).setPeriodTicks(24000).addTrack(EnvironmentAttributes.VILLAGER_ACTIVITY, track -> track.addKeyframe(10, Activity.IDLE).addKeyframe(2000, Activity.WORK).addKeyframe(9000, Activity.MEET).addKeyframe(11000, Activity.IDLE).addKeyframe(12000, Activity.REST)).addTrack(EnvironmentAttributes.BABY_VILLAGER_ACTIVITY, track -> track.addKeyframe(10, Activity.IDLE).addKeyframe(3000, Activity.PLAY).addKeyframe(6000, Activity.IDLE).addKeyframe(10000, Activity.PLAY).addKeyframe(12000, Activity.REST)).build());
        context.register(EARLY_GAME, Timeline.builder(overworldClock).addModifierTrack(EnvironmentAttributes.CAN_PILLAGER_PATROL_SPAWN, BooleanModifier.AND, track -> track.addKeyframe(0, false).addKeyframe(120000, true)).build());
    }

    private static ResourceKey<Timeline> key(String id) {
        return ResourceKey.create(Registries.TIMELINE, Identifier.withDefaultNamespace(id));
    }
}

