/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContextArg;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jspecify.annotations.Nullable;

public class LootContext {
    private final LootParams params;
    private final RandomSource random;
    private final HolderGetter.Provider lootDataResolver;
    private final Set<VisitedEntry<?>> visitedElements = Sets.newLinkedHashSet();

    private LootContext(LootParams params, RandomSource random, HolderGetter.Provider lootDataResolver) {
        this.params = params;
        this.random = random;
        this.lootDataResolver = lootDataResolver;
    }

    public boolean hasParameter(ContextKey<?> key) {
        return this.params.contextMap().has(key);
    }

    public <T> T getParameter(ContextKey<T> key) {
        return this.params.contextMap().getOrThrow(key);
    }

    public <T> @Nullable T getOptionalParameter(ContextKey<T> key) {
        return this.params.contextMap().getOptional(key);
    }

    public void addDynamicDrops(Identifier location, Consumer<ItemStack> output) {
        this.params.addDynamicDrops(location, output);
    }

    public boolean hasVisitedElement(VisitedEntry<?> element) {
        return this.visitedElements.contains(element);
    }

    public boolean pushVisitedElement(VisitedEntry<?> element) {
        return this.visitedElements.add(element);
    }

    public void popVisitedElement(VisitedEntry<?> element) {
        this.visitedElements.remove(element);
    }

    public HolderGetter.Provider getResolver() {
        return this.lootDataResolver;
    }

    public RandomSource getRandom() {
        return this.random;
    }

    public float getLuck() {
        return this.params.getLuck();
    }

    public ServerLevel getLevel() {
        return this.params.getLevel();
    }

    public static VisitedEntry<LootTable> createVisitedEntry(LootTable table) {
        return new VisitedEntry<LootTable>(LootDataType.TABLE, table);
    }

    public static VisitedEntry<LootItemCondition> createVisitedEntry(LootItemCondition table) {
        return new VisitedEntry<LootItemCondition>(LootDataType.PREDICATE, table);
    }

    public static VisitedEntry<LootItemFunction> createVisitedEntry(LootItemFunction table) {
        return new VisitedEntry<LootItemFunction>(LootDataType.MODIFIER, table);
    }

    public record VisitedEntry<T extends Validatable>(LootDataType<T> type, T value) {
    }

    public static enum ItemStackTarget implements StringRepresentable,
    LootContextArg.SimpleGetter<ItemInstance>
    {
        TOOL("tool", LootContextParams.TOOL);

        private final String name;
        private final ContextKey<? extends ItemInstance> param;

        private ItemStackTarget(String name, ContextKey<? extends ItemInstance> param) {
            this.name = name;
            this.param = param;
        }

        @Override
        public ContextKey<? extends ItemInstance> contextParam() {
            return this.param;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static enum BlockEntityTarget implements StringRepresentable,
    LootContextArg.SimpleGetter<BlockEntity>
    {
        BLOCK_ENTITY("block_entity", LootContextParams.BLOCK_ENTITY);

        private final String name;
        private final ContextKey<? extends BlockEntity> param;

        private BlockEntityTarget(String name, ContextKey<? extends BlockEntity> param) {
            this.name = name;
            this.param = param;
        }

        @Override
        public ContextKey<? extends BlockEntity> contextParam() {
            return this.param;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static enum EntityTarget implements StringRepresentable,
    LootContextArg.SimpleGetter<Entity>
    {
        THIS("this", LootContextParams.THIS_ENTITY),
        ATTACKER("attacker", LootContextParams.ATTACKING_ENTITY),
        DIRECT_ATTACKER("direct_attacker", LootContextParams.DIRECT_ATTACKING_ENTITY),
        ATTACKING_PLAYER("attacking_player", LootContextParams.LAST_DAMAGE_PLAYER),
        TARGET_ENTITY("target_entity", LootContextParams.TARGET_ENTITY),
        INTERACTING_ENTITY("interacting_entity", LootContextParams.INTERACTING_ENTITY);

        public static final StringRepresentable.EnumCodec<EntityTarget> CODEC;
        private final String name;
        private final ContextKey<? extends Entity> param;

        private EntityTarget(String name, ContextKey<? extends Entity> param) {
            this.name = name;
            this.param = param;
        }

        @Override
        public ContextKey<? extends Entity> contextParam() {
            return this.param;
        }

        public static EntityTarget getByName(String name) {
            EntityTarget target = CODEC.byName(name);
            if (target != null) {
                return target;
            }
            throw new IllegalArgumentException("Invalid entity target " + name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(EntityTarget::values);
        }
    }

    public static class Builder {
        private final LootParams params;
        private @Nullable RandomSource random;

        public Builder(LootParams params) {
            this.params = params;
        }

        public Builder withOptionalRandomSeed(long seed) {
            if (seed != 0L) {
                this.random = RandomSource.create(seed);
            }
            return this;
        }

        public Builder withOptionalRandomSource(RandomSource randomSource) {
            this.random = randomSource;
            return this;
        }

        public ServerLevel getLevel() {
            return this.params.getLevel();
        }

        public LootContext create(Optional<Identifier> randomSequenceKey) {
            ServerLevel level = this.getLevel();
            MinecraftServer server = level.getServer();
            RandomSource random = Optional.ofNullable(this.random).or(() -> randomSequenceKey.map(server::getRandomSequence)).orElseGet(level::getRandom);
            return new LootContext(this.params, random, server.reloadableRegistries().lookup());
        }
    }
}

