/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.BoolArgumentType
 *  com.mojang.brigadier.arguments.FloatArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  com.mojang.brigadier.exceptions.Dynamic4CommandExceptionType
 */
package net.minecraft.server.commands;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic4CommandExceptionType;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.scores.PlayerTeam;

public class SpreadPlayersCommand {
    private static final int MAX_ITERATION_COUNT = 10000;
    private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_TEAMS = new Dynamic4CommandExceptionType((count, x, z, recommended) -> Component.translatableEscape("commands.spreadplayers.failed.teams", count, x, z, recommended));
    private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_ENTITIES = new Dynamic4CommandExceptionType((count, x, z, recommended) -> Component.translatableEscape("commands.spreadplayers.failed.entities", count, x, z, recommended));
    private static final Dynamic2CommandExceptionType ERROR_INVALID_MAX_HEIGHT = new Dynamic2CommandExceptionType((suppliedMaxHeight, worldMinHeight) -> Component.translatableEscape("commands.spreadplayers.failed.invalid.height", suppliedMaxHeight, worldMinHeight));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spreadplayers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.argument("center", Vec2Argument.vec2()).then(Commands.argument("spreadDistance", FloatArgumentType.floatArg((float)0.0f)).then(((RequiredArgumentBuilder)Commands.argument("maxRange", FloatArgumentType.floatArg((float)1.0f)).then(Commands.argument("respectTeams", BoolArgumentType.bool()).then(Commands.argument("targets", EntityArgument.entities()).executes(c -> SpreadPlayersCommand.spreadPlayers((CommandSourceStack)c.getSource(), Vec2Argument.getVec2((CommandContext<CommandSourceStack>)c, "center"), FloatArgumentType.getFloat((CommandContext)c, (String)"spreadDistance"), FloatArgumentType.getFloat((CommandContext)c, (String)"maxRange"), ((CommandSourceStack)c.getSource()).getLevel().getMaxY() + 1, BoolArgumentType.getBool((CommandContext)c, (String)"respectTeams"), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets")))))).then(Commands.literal("under").then(Commands.argument("maxHeight", IntegerArgumentType.integer()).then(Commands.argument("respectTeams", BoolArgumentType.bool()).then(Commands.argument("targets", EntityArgument.entities()).executes(c -> SpreadPlayersCommand.spreadPlayers((CommandSourceStack)c.getSource(), Vec2Argument.getVec2((CommandContext<CommandSourceStack>)c, "center"), FloatArgumentType.getFloat((CommandContext)c, (String)"spreadDistance"), FloatArgumentType.getFloat((CommandContext)c, (String)"maxRange"), IntegerArgumentType.getInteger((CommandContext)c, (String)"maxHeight"), BoolArgumentType.getBool((CommandContext)c, (String)"respectTeams"), EntityArgument.getEntities((CommandContext<CommandSourceStack>)c, "targets")))))))))));
    }

    private static int spreadPlayers(CommandSourceStack source, Vec2 center, float spreadDistance, float maxRange, int maxHeight, boolean respectTeams, Collection<? extends Entity> entities) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        int minY = level.getMinY();
        if (maxHeight < minY) {
            throw ERROR_INVALID_MAX_HEIGHT.create((Object)maxHeight, (Object)minY);
        }
        RandomSource random = RandomSource.createThreadLocalInstance();
        double minX = center.x - maxRange;
        double minZ = center.y - maxRange;
        double maxX = center.x + maxRange;
        double maxZ = center.y + maxRange;
        Position[] positions = SpreadPlayersCommand.createInitialPositions(random, respectTeams ? SpreadPlayersCommand.getNumberOfTeams(entities) : entities.size(), minX, minZ, maxX, maxZ);
        SpreadPlayersCommand.spreadPositions(center, spreadDistance, level, random, minX, minZ, maxX, maxZ, maxHeight, positions, respectTeams);
        double distance = SpreadPlayersCommand.setPlayerPositions(entities, level, positions, maxHeight, respectTeams);
        source.sendSuccess(() -> Component.translatable("commands.spreadplayers.success." + (respectTeams ? "teams" : "entities"), positions.length, Float.valueOf(center.x), Float.valueOf(center.y), String.format(Locale.ROOT, "%.2f", distance)), true);
        return positions.length;
    }

    private static int getNumberOfTeams(Collection<? extends Entity> players) {
        HashSet teams = Sets.newHashSet();
        for (Entity entity : players) {
            if (entity instanceof Player) {
                teams.add(entity.getTeam());
                continue;
            }
            teams.add(null);
        }
        return teams.size();
    }

    private static void spreadPositions(Vec2 center, double spreadDist, ServerLevel level, RandomSource random, double minX, double minZ, double maxX, double maxZ, int maxHeight, Position[] positions, boolean respectTeams) throws CommandSyntaxException {
        int iteration;
        boolean hasCollisions = true;
        double minDistance = 3.4028234663852886E38;
        for (iteration = 0; iteration < 10000 && hasCollisions; ++iteration) {
            hasCollisions = false;
            minDistance = 3.4028234663852886E38;
            for (int i = 0; i < positions.length; ++i) {
                Position position = positions[i];
                int neighbourCount = 0;
                Position averageNeighbourPos = new Position();
                for (int j = 0; j < positions.length; ++j) {
                    if (i == j) continue;
                    Position neighbour = positions[j];
                    double dist = position.dist(neighbour);
                    minDistance = Math.min(dist, minDistance);
                    if (!(dist < spreadDist)) continue;
                    ++neighbourCount;
                    averageNeighbourPos.x += neighbour.x - position.x;
                    averageNeighbourPos.z += neighbour.z - position.z;
                }
                if (neighbourCount > 0) {
                    averageNeighbourPos.x /= (double)neighbourCount;
                    averageNeighbourPos.z /= (double)neighbourCount;
                    double length = averageNeighbourPos.getLength();
                    if (length > 0.0) {
                        averageNeighbourPos.normalize();
                        position.moveAway(averageNeighbourPos);
                    } else {
                        position.randomize(random, minX, minZ, maxX, maxZ);
                    }
                    hasCollisions = true;
                }
                if (!position.clamp(minX, minZ, maxX, maxZ)) continue;
                hasCollisions = true;
            }
            if (hasCollisions) continue;
            for (Position position : positions) {
                if (position.isSafe(level, maxHeight)) continue;
                position.randomize(random, minX, minZ, maxX, maxZ);
                hasCollisions = true;
            }
        }
        if (minDistance == 3.4028234663852886E38) {
            minDistance = 0.0;
        }
        if (iteration >= 10000) {
            if (respectTeams) {
                throw ERROR_FAILED_TO_SPREAD_TEAMS.create((Object)positions.length, (Object)Float.valueOf(center.x), (Object)Float.valueOf(center.y), (Object)String.format(Locale.ROOT, "%.2f", minDistance));
            }
            throw ERROR_FAILED_TO_SPREAD_ENTITIES.create((Object)positions.length, (Object)Float.valueOf(center.x), (Object)Float.valueOf(center.y), (Object)String.format(Locale.ROOT, "%.2f", minDistance));
        }
    }

    private static double setPlayerPositions(Collection<? extends Entity> entities, ServerLevel level, Position[] positions, int maxHeight, boolean respectTeams) {
        double avgDistance = 0.0;
        int positionIndex = 0;
        HashMap teamPositions = Maps.newHashMap();
        for (Entity entity : entities) {
            Position position;
            if (respectTeams) {
                PlayerTeam team;
                PlayerTeam playerTeam = team = entity instanceof Player ? entity.getTeam() : null;
                if (!teamPositions.containsKey(team)) {
                    teamPositions.put(team, positions[positionIndex++]);
                }
                position = (Position)teamPositions.get(team);
            } else {
                position = positions[positionIndex++];
            }
            entity.teleportTo(level, (double)Mth.floor(position.x) + 0.5, position.getSpawnY(level, maxHeight), (double)Mth.floor(position.z) + 0.5, Set.of(), entity.getYRot(), entity.getXRot(), true);
            double closest = Double.MAX_VALUE;
            for (Position testPosition : positions) {
                if (position == testPosition) continue;
                double dist = position.dist(testPosition);
                closest = Math.min(dist, closest);
            }
            avgDistance += closest;
        }
        if (entities.size() < 2) {
            return 0.0;
        }
        return avgDistance /= (double)entities.size();
    }

    private static Position[] createInitialPositions(RandomSource random, int count, double minX, double minZ, double maxX, double maxZ) {
        Position[] result = new Position[count];
        for (int i = 0; i < result.length; ++i) {
            Position position = new Position();
            position.randomize(random, minX, minZ, maxX, maxZ);
            result[i] = position;
        }
        return result;
    }

    private static class Position {
        private double x;
        private double z;

        private Position() {
        }

        public double dist(Position target) {
            double dx = this.x - target.x;
            double dz = this.z - target.z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        public void normalize() {
            double dist = this.getLength();
            this.x /= dist;
            this.z /= dist;
        }

        public double getLength() {
            return Math.sqrt(this.x * this.x + this.z * this.z);
        }

        public void moveAway(Position pos) {
            this.x -= pos.x;
            this.z -= pos.z;
        }

        public boolean clamp(double minX, double minZ, double maxX, double maxZ) {
            boolean changed = false;
            if (this.x < minX) {
                this.x = minX;
                changed = true;
            } else if (this.x > maxX) {
                this.x = maxX;
                changed = true;
            }
            if (this.z < minZ) {
                this.z = minZ;
                changed = true;
            } else if (this.z > maxZ) {
                this.z = maxZ;
                changed = true;
            }
            return changed;
        }

        public int getSpawnY(BlockGetter level, int maxHeight) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(this.x, (double)(maxHeight + 1), this.z);
            boolean air2Above = level.getBlockState(pos).isAir();
            pos.move(Direction.DOWN);
            boolean air1Above = level.getBlockState(pos).isAir();
            while (pos.getY() > level.getMinY()) {
                pos.move(Direction.DOWN);
                boolean currentIsAir = level.getBlockState(pos).isAir();
                if (!currentIsAir && air1Above && air2Above) {
                    return pos.getY() + 1;
                }
                air2Above = air1Above;
                air1Above = currentIsAir;
            }
            return maxHeight + 1;
        }

        public boolean isSafe(BlockGetter level, int maxHeight) {
            BlockPos pos = BlockPos.containing(this.x, this.getSpawnY(level, maxHeight) - 1, this.z);
            BlockState state = level.getBlockState(pos);
            return pos.getY() < maxHeight && !state.liquid() && !state.is(BlockTags.FIRE);
        }

        public void randomize(RandomSource random, double minX, double minZ, double maxX, double maxZ) {
            this.x = Mth.nextDouble(random, minX, maxX);
            this.z = Mth.nextDouble(random, minZ, maxZ);
        }
    }
}

