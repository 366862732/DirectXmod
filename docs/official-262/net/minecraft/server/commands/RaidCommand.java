/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Set;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RaidCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("raid").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))).then(Commands.literal("start").then(Commands.argument("omenlvl", IntegerArgumentType.integer((int)0)).executes(c -> RaidCommand.start((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"omenlvl")))))).then(Commands.literal("stop").executes(c -> RaidCommand.stop((CommandSourceStack)c.getSource())))).then(Commands.literal("check").executes(c -> RaidCommand.check((CommandSourceStack)c.getSource())))).then(Commands.literal("sound").then(Commands.argument("type", ComponentArgument.textComponent(context)).executes(c -> RaidCommand.playSound((CommandSourceStack)c.getSource(), ComponentArgument.getResolvedComponent((CommandContext<CommandSourceStack>)c, "type")))))).then(Commands.literal("spawnleader").executes(c -> RaidCommand.spawnLeader((CommandSourceStack)c.getSource())))).then(Commands.literal("setomen").then(Commands.argument("level", IntegerArgumentType.integer((int)0)).executes(c -> RaidCommand.setRaidOmenLevel((CommandSourceStack)c.getSource(), IntegerArgumentType.getInteger((CommandContext)c, (String)"level")))))).then(Commands.literal("glow").executes(c -> RaidCommand.glow((CommandSourceStack)c.getSource()))));
    }

    private static int glow(CommandSourceStack source) throws CommandSyntaxException {
        Raid raid = RaidCommand.getRaid(source.getPlayerOrException());
        if (raid != null) {
            Set<Raider> raiders = raid.getAllRaiders();
            for (Raider raider : raiders) {
                raider.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1000, 1));
            }
        }
        return 1;
    }

    private static int setRaidOmenLevel(CommandSourceStack source, int level) throws CommandSyntaxException {
        Raid raid = RaidCommand.getRaid(source.getPlayerOrException());
        if (raid != null) {
            int max = raid.getMaxRaidOmenLevel();
            if (level > max) {
                source.sendFailure(Component.literal("Sorry, the max raid omen level you can set is " + max));
            } else {
                int before = raid.getRaidOmenLevel();
                raid.setRaidOmenLevel(level);
                source.sendSuccess(() -> Component.literal("Changed village's raid omen level from " + before + " to " + level), false);
            }
        } else {
            source.sendFailure(Component.literal("No raid found here"));
        }
        return 1;
    }

    private static int spawnLeader(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Spawned a raid captain"), false);
        Raider raider = EntityTypes.PILLAGER.create((Level)source.getLevel(), EntitySpawnReason.COMMAND);
        if (raider == null) {
            source.sendFailure(Component.literal("Pillager failed to spawn"));
            return 0;
        }
        raider.setPatrolLeader(true);
        raider.setItemSlot(EquipmentSlot.HEAD, Raid.getOminousBannerInstance(source.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
        raider.setPos(source.getPosition().x, source.getPosition().y, source.getPosition().z);
        raider.finalizeSpawn(source.getLevel(), source.getLevel().getCurrentDifficultyAt(BlockPos.containing(source.getPosition())), EntitySpawnReason.COMMAND, null);
        source.getLevel().addFreshEntityWithPassengers(raider);
        return 1;
    }

    private static int playSound(CommandSourceStack source, @Nullable Component type) {
        if (type != null && type.getString().equals("local")) {
            ServerLevel level = source.getLevel();
            Vec3 pos = source.getPosition().add(5.0, 0.0, 0.0);
            level.playSeededSound(null, pos.x, pos.y, pos.z, SoundEvents.RAID_HORN, SoundSource.NEUTRAL, 2.0f, 1.0f, level.getRandom().nextLong());
        }
        return 1;
    }

    private static int start(CommandSourceStack source, int raidOmenLevel) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos pos = player.blockPosition();
        if (player.level().isRaided(pos)) {
            source.sendFailure(Component.literal("Raid already started close by"));
            return -1;
        }
        Raids raids = player.level().getRaids();
        Raid raid = raids.createOrExtendRaid(player, player.blockPosition());
        if (raid != null) {
            raid.setRaidOmenLevel(raidOmenLevel);
            raids.setDirty();
            source.sendSuccess(() -> Component.literal("Created a raid in your local village"), false);
        } else {
            source.sendFailure(Component.literal("Failed to create a raid in your local village"));
        }
        return 1;
    }

    private static int stop(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos pos = player.blockPosition();
        Raid raid = player.level().getRaidAt(pos);
        if (raid != null) {
            raid.stop();
            source.sendSuccess(() -> Component.literal("Stopped raid"), false);
            return 1;
        }
        source.sendFailure(Component.literal("No raid here"));
        return -1;
    }

    private static int check(CommandSourceStack source) throws CommandSyntaxException {
        Raid raid = RaidCommand.getRaid(source.getPlayerOrException());
        if (raid != null) {
            StringBuilder status = new StringBuilder();
            status.append("Found a started raid! ");
            source.sendSuccess(() -> Component.literal(status.toString()), false);
            StringBuilder status2 = new StringBuilder();
            status2.append("Num groups spawned: ");
            status2.append(raid.getGroupsSpawned());
            status2.append(" Raid omen level: ");
            status2.append(raid.getRaidOmenLevel());
            status2.append(" Num mobs: ");
            status2.append(raid.getTotalRaidersAlive());
            status2.append(" Raid health: ");
            status2.append(raid.getHealthOfLivingRaiders());
            status2.append(" / ");
            status2.append(raid.getTotalHealth());
            source.sendSuccess(() -> Component.literal(status2.toString()), false);
            return 1;
        }
        source.sendFailure(Component.literal("Found no started raids"));
        return 0;
    }

    private static @Nullable Raid getRaid(ServerPlayer player) {
        return player.level().getRaidAt(player.blockPosition());
    }
}

