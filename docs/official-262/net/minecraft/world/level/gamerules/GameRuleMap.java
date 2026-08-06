/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.gamerules;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public final class GameRuleMap
extends SavedData {
    public static final Codec<GameRuleMap> CODEC = Codec.dispatchedMap(BuiltInRegistries.GAME_RULE.byNameCodec(), GameRule::valueCodec).xmap(GameRuleMap::ofTrusted, GameRuleMap::map);
    public static final SavedDataType<GameRuleMap> TYPE = new SavedDataType<GameRuleMap>(Identifier.withDefaultNamespace("game_rules"), GameRuleMap::of, CODEC, DataFixTypes.SAVED_DATA_GAME_RULES);
    private final Reference2ObjectMap<GameRule<?>, Object> map;

    private GameRuleMap(Reference2ObjectMap<GameRule<?>, Object> map) {
        this.map = map;
    }

    private static GameRuleMap ofTrusted(Map<GameRule<?>, Object> map) {
        return new GameRuleMap((Reference2ObjectMap<GameRule<?>, Object>)new Reference2ObjectOpenHashMap(map));
    }

    public static GameRuleMap of() {
        return new GameRuleMap((Reference2ObjectMap<GameRule<?>, Object>)new Reference2ObjectOpenHashMap());
    }

    public static GameRuleMap of(Stream<GameRule<?>> gameRuleTypeStream) {
        Reference2ObjectOpenHashMap map = new Reference2ObjectOpenHashMap();
        gameRuleTypeStream.forEach(gameRule -> map.put(gameRule, gameRule.defaultValue()));
        return new GameRuleMap((Reference2ObjectMap<GameRule<?>, Object>)map);
    }

    public static GameRuleMap copyOf(GameRuleMap gameRuleMap) {
        return new GameRuleMap((Reference2ObjectMap<GameRule<?>, Object>)new Reference2ObjectOpenHashMap(gameRuleMap.map));
    }

    public boolean has(GameRule<?> gameRule) {
        return this.map.containsKey(gameRule);
    }

    public <T> @Nullable T get(GameRule<T> gameRule) {
        return (T)this.map.get(gameRule);
    }

    public <T> void set(GameRule<T> gameRule, T value) {
        this.setDirty();
        this.map.put(gameRule, value);
    }

    public <T> void reset(GameRule<T> gameRule) {
        this.set(gameRule, gameRule.defaultValue());
    }

    public <T> @Nullable T remove(GameRule<T> gameRule) {
        this.setDirty();
        return (T)this.map.remove(gameRule);
    }

    public Set<GameRule<?>> keySet() {
        return this.map.keySet();
    }

    public int size() {
        return this.map.size();
    }

    public String toString() {
        return this.map.toString();
    }

    public GameRuleMap withOther(GameRuleMap other) {
        GameRuleMap result = GameRuleMap.copyOf(this);
        result.setFromIf(other, r -> true);
        return result;
    }

    public void setFromIf(GameRuleMap other, Predicate<GameRule<?>> predicate) {
        for (GameRule<?> gameRule : other.keySet()) {
            if (!predicate.test(gameRule)) continue;
            GameRuleMap.setGameRule(other, gameRule, this);
        }
    }

    private static <T> void setGameRule(GameRuleMap other, GameRule<T> gameRule, GameRuleMap result) {
        result.set(gameRule, Objects.requireNonNull(other.get(gameRule)));
    }

    private Reference2ObjectMap<GameRule<?>, Object> map() {
        return this.map;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        GameRuleMap that = (GameRuleMap)obj;
        return Objects.equals(this.map, that.map);
    }

    public int hashCode() {
        return Objects.hash(this.map);
    }

    public static class Builder {
        private final Reference2ObjectMap<GameRule<?>, Object> map = new Reference2ObjectOpenHashMap();

        public <T> Builder set(GameRule<T> gameRule, T value) {
            this.map.put(gameRule, value);
            return this;
        }

        public GameRuleMap build() {
            return new GameRuleMap(this.map);
        }
    }
}

