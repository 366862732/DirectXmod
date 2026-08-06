/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.gametest.framework;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Unit;
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.timeline.Timeline;
import org.slf4j.Logger;

public interface TestEnvironmentDefinition<SavedDataType> {
    public static final Codec<TestEnvironmentDefinition<?>> DIRECT_CODEC = BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE.byNameCodec().dispatch(TestEnvironmentDefinition::codec, c -> c);
    public static final Codec<Holder<TestEnvironmentDefinition<?>>> CODEC = RegistryFileCodec.create(Registries.TEST_ENVIRONMENT, DIRECT_CODEC);

    public static MapCodec<? extends TestEnvironmentDefinition<?>> bootstrap(Registry<MapCodec<? extends TestEnvironmentDefinition<?>>> registry) {
        Registry.register(registry, "all_of", AllOf.CODEC);
        Registry.register(registry, "clock_time", ClockTime.CODEC);
        Registry.register(registry, "difficulty", SetDifficulty.CODEC);
        Registry.register(registry, "function", Functions.CODEC);
        Registry.register(registry, "game_rules", SetGameRules.CODEC);
        Registry.register(registry, "timeline_attributes", Timelines.CODEC);
        return Registry.register(registry, "weather", Weather.CODEC);
    }

    public SavedDataType setup(ServerLevel var1);

    public void teardown(ServerLevel var1, SavedDataType var2);

    public MapCodec<? extends TestEnvironmentDefinition<SavedDataType>> codec();

    public static <T> Activation<T> activate(TestEnvironmentDefinition<T> environment, ServerLevel level) {
        return new Activation<T>(environment.setup(level), environment, level);
    }

    public record AllOf(List<Holder<TestEnvironmentDefinition<?>>> definitions) implements TestEnvironmentDefinition<List<? extends Activation<?>>>
    {
        public static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CODEC.listOf().fieldOf("definitions").forGetter(AllOf::definitions)).apply((Applicative)i, AllOf::new));

        public AllOf(TestEnvironmentDefinition<?> ... defs) {
            this(Arrays.stream(defs).map(AllOf::holder).toList());
        }

        private static Holder<TestEnvironmentDefinition<?>> holder(TestEnvironmentDefinition<?> holder) {
            return Holder.direct(holder);
        }

        @Override
        public List<? extends Activation<?>> setup(ServerLevel level) {
            return this.definitions.stream().map(b -> TestEnvironmentDefinition.activate((TestEnvironmentDefinition)b.value(), level)).toList();
        }

        @Override
        public void teardown(ServerLevel level, List<? extends Activation<?>> activations) {
            activations.reversed().forEach(Activation::teardown);
        }

        @Override
        public MapCodec<AllOf> codec() {
            return CODEC;
        }
    }

    public record ClockTime(Holder<WorldClock> clock, int time) implements TestEnvironmentDefinition<Long>
    {
        public static final MapCodec<ClockTime> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)WorldClock.CODEC.fieldOf("clock").forGetter(ClockTime::clock), (App)ExtraCodecs.NON_NEGATIVE_INT.fieldOf("time").forGetter(ClockTime::time)).apply((Applicative)i, ClockTime::new));

        @Override
        public Long setup(ServerLevel level) {
            MinecraftServer server = level.getServer();
            long previous = server.clockManager().getTotalTicks(this.clock);
            server.clockManager().setTotalTicks(this.clock, this.time);
            return previous;
        }

        @Override
        public void teardown(ServerLevel level, Long saveData) {
            MinecraftServer server = level.getServer();
            server.clockManager().setTotalTicks(this.clock, saveData);
        }

        @Override
        public MapCodec<ClockTime> codec() {
            return CODEC;
        }
    }

    public record SetDifficulty(Difficulty difficulty) implements TestEnvironmentDefinition<Difficulty>
    {
        public static final MapCodec<SetDifficulty> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Difficulty.CODEC.fieldOf("difficulty").forGetter(SetDifficulty::difficulty)).apply((Applicative)i, SetDifficulty::new));

        @Override
        public Difficulty setup(ServerLevel level) {
            Difficulty oldDifficulty = level.getDifficulty();
            level.getServer().setDifficulty(this.difficulty, true);
            return oldDifficulty;
        }

        @Override
        public void teardown(ServerLevel level, Difficulty saveData) {
            level.getServer().setDifficulty(saveData, true);
        }

        @Override
        public MapCodec<SetDifficulty> codec() {
            return CODEC;
        }
    }

    public record Functions(Optional<Identifier> setupFunction, Optional<Identifier> teardownFunction) implements TestEnvironmentDefinition<Unit>
    {
        private static final Logger LOGGER = LogUtils.getLogger();
        public static final MapCodec<Functions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.optionalFieldOf("setup").forGetter(Functions::setupFunction), (App)Identifier.CODEC.optionalFieldOf("teardown").forGetter(Functions::teardownFunction)).apply((Applicative)i, Functions::new));

        @Override
        public Unit setup(ServerLevel level) {
            this.setupFunction.ifPresent(p -> Functions.run(level, p));
            return Unit.INSTANCE;
        }

        @Override
        public void teardown(ServerLevel level, Unit saveData) {
            this.teardownFunction.ifPresent(p -> Functions.run(level, p));
        }

        private static void run(ServerLevel level, Identifier functionId) {
            MinecraftServer server = level.getServer();
            ServerFunctionManager functions = server.getFunctions();
            Optional<CommandFunction<CommandSourceStack>> function = functions.get(functionId);
            if (function.isPresent()) {
                CommandSourceStack source = server.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput().withLevel(level);
                functions.execute(function.get(), source);
            } else {
                LOGGER.error("Test Batch failed for non-existent function {}", (Object)functionId);
            }
        }

        @Override
        public MapCodec<Functions> codec() {
            return CODEC;
        }
    }

    public record SetGameRules(GameRuleMap gameRulesMap) implements TestEnvironmentDefinition<GameRuleMap>
    {
        public static final MapCodec<SetGameRules> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)GameRuleMap.CODEC.fieldOf("rules").forGetter(SetGameRules::gameRulesMap)).apply((Applicative)i, SetGameRules::new));

        @Override
        public GameRuleMap setup(ServerLevel level) {
            GameRuleMap originalState = GameRuleMap.of();
            GameRules gameRules = level.getGameRules();
            this.gameRulesMap.keySet().forEach(rule -> SetGameRules.setFromActive(originalState, rule, gameRules));
            gameRules.setAll(this.gameRulesMap, level.getServer());
            return originalState;
        }

        private static <T> void setFromActive(GameRuleMap map, GameRule<T> rule, GameRules rules) {
            map.set(rule, rules.get(rule));
        }

        @Override
        public void teardown(ServerLevel level, GameRuleMap saveData) {
            level.getGameRules().setAll(saveData, level.getServer());
        }

        @Override
        public MapCodec<SetGameRules> codec() {
            return CODEC;
        }
    }

    public record Timelines(List<Holder<Timeline>> timelines) implements TestEnvironmentDefinition<EnvironmentAttributeSystem>
    {
        public static final MapCodec<Timelines> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Timeline.CODEC.listOf().fieldOf("timelines").forGetter(Timelines::timelines)).apply((Applicative)i, Timelines::new));

        @Override
        public EnvironmentAttributeSystem setup(ServerLevel level) {
            EnvironmentAttributeSystem.Builder builder = EnvironmentAttributeSystem.builder().addDefaultLayers(level);
            for (Holder<Timeline> timeline : this.timelines) {
                builder.addTimelineLayer(timeline, level.clockManager());
            }
            return level.setEnvironmentAttributes(builder.build());
        }

        @Override
        public void teardown(ServerLevel level, EnvironmentAttributeSystem saveData) {
            level.setEnvironmentAttributes(saveData);
        }

        @Override
        public MapCodec<Timelines> codec() {
            return CODEC;
        }
    }

    public record Weather(Type weather) implements TestEnvironmentDefinition<Type>
    {
        public static final MapCodec<Weather> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Type.CODEC.fieldOf("weather").forGetter(Weather::weather)).apply((Applicative)i, Weather::new));

        @Override
        public Type setup(ServerLevel level) {
            Type previous = level.isThundering() ? Type.THUNDER : (level.isRaining() ? Type.RAIN : Type.CLEAR);
            this.weather.apply(level);
            return previous;
        }

        @Override
        public void teardown(ServerLevel level, Type saveData) {
            level.resetWeatherCycle();
            saveData.apply(level);
        }

        @Override
        public MapCodec<Weather> codec() {
            return CODEC;
        }

        public static enum Type implements StringRepresentable
        {
            CLEAR("clear", 100000, 0, false, false),
            RAIN("rain", 0, 100000, true, false),
            THUNDER("thunder", 0, 100000, true, true);

            public static final Codec<Type> CODEC;
            private final String id;
            private final int clearTime;
            private final int rainTime;
            private final boolean raining;
            private final boolean thundering;

            private Type(String id, int clearTime, int rainTime, boolean raining, boolean thundering) {
                this.id = id;
                this.clearTime = clearTime;
                this.rainTime = rainTime;
                this.raining = raining;
                this.thundering = thundering;
            }

            public void apply(ServerLevel level) {
                level.getServer().setWeatherParameters(this.clearTime, this.rainTime, this.raining, this.thundering);
            }

            @Override
            public String getSerializedName() {
                return this.id;
            }

            static {
                CODEC = StringRepresentable.fromEnum(Type::values);
            }
        }
    }

    public static class Activation<T> {
        private final T value;
        private final TestEnvironmentDefinition<T> definition;
        private final ServerLevel level;

        private Activation(T value, TestEnvironmentDefinition<T> definition, ServerLevel level) {
            this.value = value;
            this.definition = definition;
            this.level = level;
        }

        public void teardown() {
            this.definition.teardown(this.level, this.value);
        }
    }
}

