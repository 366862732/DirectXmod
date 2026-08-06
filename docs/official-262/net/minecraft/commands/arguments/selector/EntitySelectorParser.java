/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.primitives.Doubles
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.arguments.selector;

import com.google.common.primitives.Doubles;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.commands.arguments.selector.options.InvertableSetOptionState;
import net.minecraft.commands.arguments.selector.options.SetOnceOptionState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSetSupplier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntitySelectorParser {
    public static final char SYNTAX_SELECTOR_START = '@';
    private static final char SYNTAX_OPTIONS_START = '[';
    private static final char SYNTAX_OPTIONS_END = ']';
    public static final char SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR = '=';
    private static final char SYNTAX_OPTIONS_SEPARATOR = ',';
    public static final char SYNTAX_NOT = '!';
    public static final char SYNTAX_TAG = '#';
    private static final char SELECTOR_NEAREST_PLAYER = 'p';
    private static final char SELECTOR_ALL_PLAYERS = 'a';
    private static final char SELECTOR_RANDOM_PLAYERS = 'r';
    private static final char SELECTOR_CURRENT_ENTITY = 's';
    private static final char SELECTOR_ALL_ENTITIES = 'e';
    private static final char SELECTOR_NEAREST_ENTITY = 'n';
    public static final SimpleCommandExceptionType ERROR_INVALID_NAME_OR_UUID = new SimpleCommandExceptionType((Message)Component.translatable("argument.entity.invalid"));
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_SELECTOR_TYPE = new DynamicCommandExceptionType(type -> Component.translatableEscape("argument.entity.selector.unknown", type));
    public static final SimpleCommandExceptionType ERROR_SELECTORS_NOT_ALLOWED = new SimpleCommandExceptionType((Message)Component.translatable("argument.entity.selector.not_allowed"));
    public static final SimpleCommandExceptionType ERROR_MISSING_SELECTOR_TYPE = new SimpleCommandExceptionType((Message)Component.translatable("argument.entity.selector.missing"));
    public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_OPTIONS = new SimpleCommandExceptionType((Message)Component.translatable("argument.entity.options.unterminated"));
    public static final DynamicCommandExceptionType ERROR_EXPECTED_OPTION_VALUE = new DynamicCommandExceptionType(name -> Component.translatableEscape("argument.entity.options.valueless", name));
    public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_NEAREST = (p, c) -> c.sort((a, b) -> Doubles.compare((double)a.distanceToSqr((Vec3)p), (double)b.distanceToSqr((Vec3)p)));
    public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_FURTHEST = (p, c) -> c.sort((a, b) -> Doubles.compare((double)b.distanceToSqr((Vec3)p), (double)a.distanceToSqr((Vec3)p)));
    public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_RANDOM = (p, c) -> Collections.shuffle(c);
    public static final BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> SUGGEST_NOTHING = (b, s) -> b.buildFuture();
    private final StringReader reader;
    private final boolean allowSelectors;
    private int maxResults;
    private boolean includesEntities;
    private boolean worldLimited;
    private @Nullable MinMaxBounds.Doubles distance;
    private @Nullable MinMaxBounds.Ints level;
    private @Nullable Double x;
    private @Nullable Double y;
    private @Nullable Double z;
    private @Nullable Double deltaX;
    private @Nullable Double deltaY;
    private @Nullable Double deltaZ;
    private @Nullable MinMaxBounds.FloatDegrees rotX;
    private @Nullable MinMaxBounds.FloatDegrees rotY;
    private final List<Predicate<Entity>> predicates = new ArrayList<Predicate<Entity>>();
    private BiConsumer<Vec3, List<? extends Entity>> order = EntitySelector.ORDER_ARBITRARY;
    private boolean currentEntity;
    private @Nullable String playerName;
    private int startPosition;
    private @Nullable UUID entityUUID;
    private BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;
    private final InvertableSetOptionState nameOption = new InvertableSetOptionState();
    private final SetOnceOptionState limitedOption = new SetOnceOptionState();
    private final SetOnceOptionState sortedOption = new SetOnceOptionState();
    private final InvertableSetOptionState gamemodeOption = new InvertableSetOptionState();
    private final InvertableSetOptionState teamOption = new InvertableSetOptionState();
    private @Nullable EntityType<?> type;
    private final InvertableSetOptionState typeOption = new InvertableSetOptionState();
    private final SetOnceOptionState scoresOption = new SetOnceOptionState();
    private final SetOnceOptionState advancementsOption = new SetOnceOptionState();
    private boolean usesSelectors;

    public EntitySelectorParser(StringReader reader, boolean allowSelectors) {
        this.reader = reader;
        this.allowSelectors = allowSelectors;
    }

    public static <S> boolean allowSelectors(S source) {
        PermissionSetSupplier sender;
        return source instanceof PermissionSetSupplier && (sender = (PermissionSetSupplier)source).permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS);
    }

    @Deprecated
    public static boolean allowSelectors(PermissionSetSupplier source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS);
    }

    public EntitySelector getSelector() {
        AABB aabb;
        if (this.deltaX != null || this.deltaY != null || this.deltaZ != null) {
            aabb = this.createAabb(this.deltaX == null ? 0.0 : this.deltaX, this.deltaY == null ? 0.0 : this.deltaY, this.deltaZ == null ? 0.0 : this.deltaZ);
        } else if (this.distance != null && this.distance.max().isPresent()) {
            double maxRange = (Double)this.distance.max().get();
            aabb = new AABB(-maxRange, -maxRange, -maxRange, maxRange + 1.0, maxRange + 1.0, maxRange + 1.0);
        } else {
            aabb = null;
        }
        Function<Vec3, Vec3> position = this.x == null && this.y == null && this.z == null ? o -> o : o -> new Vec3(this.x == null ? o.x : this.x, this.y == null ? o.y : this.y, this.z == null ? o.z : this.z);
        return new EntitySelector(this.maxResults, this.includesEntities, this.worldLimited, List.copyOf(this.predicates), this.distance, position, aabb, this.order, this.currentEntity, this.playerName, this.entityUUID, this.type, this.usesSelectors);
    }

    private AABB createAabb(double x, double y, double z) {
        boolean xNeg = x < 0.0;
        boolean yNeg = y < 0.0;
        boolean zNeg = z < 0.0;
        double xMin = xNeg ? x : 0.0;
        double yMin = yNeg ? y : 0.0;
        double zMin = zNeg ? z : 0.0;
        double xMax = (xNeg ? 0.0 : x) + 1.0;
        double yMax = (yNeg ? 0.0 : y) + 1.0;
        double zMax = (zNeg ? 0.0 : z) + 1.0;
        return new AABB(xMin, yMin, zMin, xMax, yMax, zMax);
    }

    private void finalizePredicates() {
        if (this.rotX != null) {
            this.predicates.add(this.createRotationPredicate(this.rotX, Entity::getXRot));
        }
        if (this.rotY != null) {
            this.predicates.add(this.createRotationPredicate(this.rotY, Entity::getYRot));
        }
        if (this.level != null) {
            this.predicates.add(e -> {
                if (!(e instanceof ServerPlayer)) return false;
                ServerPlayer serverPlayer = (ServerPlayer)e;
                if (!this.level.matches(serverPlayer.experienceLevel)) return false;
                return true;
            });
        }
    }

    private Predicate<Entity> createRotationPredicate(MinMaxBounds.FloatDegrees range, ToFloatFunction<Entity> function) {
        float min = Mth.wrapDegrees(range.min().orElse(Float.valueOf(0.0f)).floatValue());
        float max = Mth.wrapDegrees(range.max().orElse(Float.valueOf(359.0f)).floatValue());
        return e -> {
            float rotation = Mth.wrapDegrees(function.applyAsFloat((Entity)e));
            if (min > max) {
                return rotation >= min || rotation <= max;
            }
            return rotation >= min && rotation <= max;
        };
    }

    protected void parseSelector() throws CommandSyntaxException {
        this.usesSelectors = true;
        this.suggestions = this::suggestSelector;
        if (!this.reader.canRead()) {
            throw ERROR_MISSING_SELECTOR_TYPE.createWithContext((ImmutableStringReader)this.reader);
        }
        int start = this.reader.getCursor();
        char type = this.reader.read();
        if (switch (type) {
            case 'p' -> {
                this.maxResults = 1;
                this.includesEntities = false;
                this.order = ORDER_NEAREST;
                this.limitToType(EntityTypes.PLAYER);
                yield false;
            }
            case 'a' -> {
                this.maxResults = Integer.MAX_VALUE;
                this.includesEntities = false;
                this.order = EntitySelector.ORDER_ARBITRARY;
                this.limitToType(EntityTypes.PLAYER);
                yield false;
            }
            case 'r' -> {
                this.maxResults = 1;
                this.includesEntities = false;
                this.order = ORDER_RANDOM;
                this.limitToType(EntityTypes.PLAYER);
                yield false;
            }
            case 's' -> {
                this.maxResults = 1;
                this.includesEntities = true;
                this.currentEntity = true;
                yield false;
            }
            case 'e' -> {
                this.maxResults = Integer.MAX_VALUE;
                this.includesEntities = true;
                this.order = EntitySelector.ORDER_ARBITRARY;
                yield true;
            }
            case 'n' -> {
                this.maxResults = 1;
                this.includesEntities = true;
                this.order = ORDER_NEAREST;
                yield true;
            }
            default -> {
                this.reader.setCursor(start);
                throw ERROR_UNKNOWN_SELECTOR_TYPE.createWithContext((ImmutableStringReader)this.reader, (Object)("@" + String.valueOf(type)));
            }
        }) {
            this.predicates.add(Entity::isAlive);
        }
        this.suggestions = this::suggestOpenOptions;
        if (this.reader.canRead() && this.reader.peek() == '[') {
            this.reader.skip();
            this.suggestions = this::suggestOptionsKeyOrClose;
            this.parseOptions();
        }
    }

    protected void parseNameOrUUID() throws CommandSyntaxException {
        if (this.reader.canRead()) {
            this.suggestions = this::suggestName;
        }
        int start = this.reader.getCursor();
        String name = this.reader.readString();
        try {
            this.entityUUID = UUID.fromString(name);
            this.includesEntities = true;
        }
        catch (IllegalArgumentException ex) {
            if (name.isEmpty() || name.length() > 16) {
                this.reader.setCursor(start);
                throw ERROR_INVALID_NAME_OR_UUID.createWithContext((ImmutableStringReader)this.reader);
            }
            this.includesEntities = false;
            this.playerName = name;
        }
        this.maxResults = 1;
    }

    protected void parseOptions() throws CommandSyntaxException {
        this.suggestions = this::suggestOptionsKey;
        this.reader.skipWhitespace();
        while (this.reader.canRead() && this.reader.peek() != ']') {
            this.reader.skipWhitespace();
            int start = this.reader.getCursor();
            String key = this.reader.readString();
            EntitySelectorOptions.Modifier modifier = EntitySelectorOptions.get(this, key, start);
            this.reader.skipWhitespace();
            if (!this.reader.canRead() || this.reader.peek() != '=') {
                this.reader.setCursor(start);
                throw ERROR_EXPECTED_OPTION_VALUE.createWithContext((ImmutableStringReader)this.reader, (Object)key);
            }
            this.reader.skip();
            this.reader.skipWhitespace();
            this.suggestions = SUGGEST_NOTHING;
            modifier.handle(this);
            this.reader.skipWhitespace();
            this.suggestions = this::suggestOptionsNextOrClose;
            if (!this.reader.canRead()) continue;
            if (this.reader.peek() == ',') {
                this.reader.skip();
                this.suggestions = this::suggestOptionsKey;
                continue;
            }
            if (this.reader.peek() == ']') break;
            throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext((ImmutableStringReader)this.reader);
        }
        if (!this.reader.canRead()) {
            throw ERROR_EXPECTED_END_OF_OPTIONS.createWithContext((ImmutableStringReader)this.reader);
        }
        this.reader.skip();
        this.suggestions = SUGGEST_NOTHING;
    }

    public boolean shouldInvertValue() {
        this.reader.skipWhitespace();
        if (this.reader.canRead() && this.reader.peek() == '!') {
            this.reader.skip();
            this.reader.skipWhitespace();
            return true;
        }
        return false;
    }

    public boolean isTag() {
        this.reader.skipWhitespace();
        if (this.reader.canRead() && this.reader.peek() == '#') {
            this.reader.skip();
            this.reader.skipWhitespace();
            return true;
        }
        return false;
    }

    public StringReader getReader() {
        return this.reader;
    }

    public void addPredicate(Predicate<Entity> predicate) {
        this.predicates.add(predicate);
    }

    public void setWorldLimited() {
        this.worldLimited = true;
    }

    public @Nullable MinMaxBounds.Doubles getDistance() {
        return this.distance;
    }

    public void setDistance(MinMaxBounds.Doubles distance) {
        this.distance = distance;
    }

    public @Nullable MinMaxBounds.Ints getLevel() {
        return this.level;
    }

    public void setLevel(MinMaxBounds.Ints level) {
        this.level = level;
    }

    public @Nullable MinMaxBounds.FloatDegrees getRotX() {
        return this.rotX;
    }

    public void setRotX(MinMaxBounds.FloatDegrees rotX) {
        this.rotX = rotX;
    }

    public @Nullable MinMaxBounds.FloatDegrees getRotY() {
        return this.rotY;
    }

    public void setRotY(MinMaxBounds.FloatDegrees rotY) {
        this.rotY = rotY;
    }

    public @Nullable Double getX() {
        return this.x;
    }

    public @Nullable Double getY() {
        return this.y;
    }

    public @Nullable Double getZ() {
        return this.z;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setDeltaX(double deltaX) {
        this.deltaX = deltaX;
    }

    public void setDeltaY(double deltaY) {
        this.deltaY = deltaY;
    }

    public void setDeltaZ(double deltaZ) {
        this.deltaZ = deltaZ;
    }

    public @Nullable Double getDeltaX() {
        return this.deltaX;
    }

    public @Nullable Double getDeltaY() {
        return this.deltaY;
    }

    public @Nullable Double getDeltaZ() {
        return this.deltaZ;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public void setIncludesEntities(boolean includesEntities) {
        this.includesEntities = includesEntities;
    }

    public BiConsumer<Vec3, List<? extends Entity>> getOrder() {
        return this.order;
    }

    public void setOrder(BiConsumer<Vec3, List<? extends Entity>> order) {
        this.order = order;
    }

    public EntitySelector parse() throws CommandSyntaxException {
        this.startPosition = this.reader.getCursor();
        this.suggestions = this::suggestNameOrSelector;
        if (this.reader.canRead() && this.reader.peek() == '@') {
            if (!this.allowSelectors) {
                throw ERROR_SELECTORS_NOT_ALLOWED.createWithContext((ImmutableStringReader)this.reader);
            }
            this.reader.skip();
            this.parseSelector();
        } else {
            this.parseNameOrUUID();
        }
        this.finalizePredicates();
        return this.getSelector();
    }

    private static void fillSelectorSuggestions(SuggestionsBuilder builder) {
        builder.suggest("@p", (Message)Component.translatable("argument.entity.selector.nearestPlayer"));
        builder.suggest("@a", (Message)Component.translatable("argument.entity.selector.allPlayers"));
        builder.suggest("@r", (Message)Component.translatable("argument.entity.selector.randomPlayer"));
        builder.suggest("@s", (Message)Component.translatable("argument.entity.selector.self"));
        builder.suggest("@e", (Message)Component.translatable("argument.entity.selector.allEntities"));
        builder.suggest("@n", (Message)Component.translatable("argument.entity.selector.nearestEntity"));
    }

    private CompletableFuture<Suggestions> suggestNameOrSelector(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
        names.accept(builder);
        if (this.allowSelectors) {
            EntitySelectorParser.fillSelectorSuggestions(builder);
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestName(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
        SuggestionsBuilder sub = builder.createOffset(this.startPosition);
        names.accept(sub);
        return builder.add(sub).buildFuture();
    }

    private CompletableFuture<Suggestions> suggestSelector(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
        SuggestionsBuilder sub = builder.createOffset(builder.getStart() - 1);
        EntitySelectorParser.fillSelectorSuggestions(sub);
        builder.add(sub);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOpenOptions(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
        builder.suggest(String.valueOf('['));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOptionsKeyOrClose(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
        builder.suggest(String.valueOf(']'));
        EntitySelectorOptions.suggestNames(this, builder);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOptionsKey(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
        EntitySelectorOptions.suggestNames(this, builder);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestOptionsNextOrClose(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
        builder.suggest(String.valueOf(','));
        builder.suggest(String.valueOf(']'));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
        builder.suggest(String.valueOf('='));
        return builder.buildFuture();
    }

    public boolean isCurrentEntity() {
        return this.currentEntity;
    }

    public void setSuggestions(BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestions) {
        this.suggestions = suggestions;
    }

    public CompletableFuture<Suggestions> fillSuggestions(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
        return this.suggestions.apply(builder.createOffset(this.reader.getCursor()), names);
    }

    public InvertableSetOptionState nameOption() {
        return this.nameOption;
    }

    public SetOnceOptionState limitedOption() {
        return this.limitedOption;
    }

    public SetOnceOptionState sortedOption() {
        return this.sortedOption;
    }

    public InvertableSetOptionState gamemodeOption() {
        return this.gamemodeOption;
    }

    public InvertableSetOptionState teamOption() {
        return this.teamOption;
    }

    public void limitToType(EntityType<?> type) {
        this.type = type;
    }

    public InvertableSetOptionState typeOption() {
        return this.typeOption;
    }

    public SetOnceOptionState scoresOption() {
        return this.scoresOption;
    }

    public SetOnceOptionState advancementsOption() {
        return this.advancementsOption;
    }
}

