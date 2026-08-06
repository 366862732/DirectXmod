/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.debug;

import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import org.jspecify.annotations.Nullable;

public record DebugBrainDump(String name, String profession, int xp, float health, float maxHealth, String inventory, boolean wantsGolem, int angerLevel, List<String> activities, List<String> behaviors, List<String> memories, List<String> gossips, Set<BlockPos> pois, Set<BlockPos> potentialPois) {
    public static final StreamCodec<FriendlyByteBuf, DebugBrainDump> STREAM_CODEC = StreamCodec.of((output, value) -> value.write((FriendlyByteBuf)((Object)output)), DebugBrainDump::new);

    public DebugBrainDump(FriendlyByteBuf input) {
        this(input.readUtf(), input.readUtf(), input.readInt(), input.readFloat(), input.readFloat(), input.readUtf(), input.readBoolean(), input.readInt(), input.readList(FriendlyByteBuf::readUtf), input.readList(FriendlyByteBuf::readUtf), input.readList(FriendlyByteBuf::readUtf), input.readList(FriendlyByteBuf::readUtf), input.readCollection(HashSet::new, BlockPos.STREAM_CODEC), input.readCollection(HashSet::new, BlockPos.STREAM_CODEC));
    }

    public void write(FriendlyByteBuf output) {
        output.writeUtf(this.name);
        output.writeUtf(this.profession);
        output.writeInt(this.xp);
        output.writeFloat(this.health);
        output.writeFloat(this.maxHealth);
        output.writeUtf(this.inventory);
        output.writeBoolean(this.wantsGolem);
        output.writeInt(this.angerLevel);
        output.writeCollection(this.activities, FriendlyByteBuf::writeUtf);
        output.writeCollection(this.behaviors, FriendlyByteBuf::writeUtf);
        output.writeCollection(this.memories, FriendlyByteBuf::writeUtf);
        output.writeCollection(this.gossips, FriendlyByteBuf::writeUtf);
        output.writeCollection(this.pois, BlockPos.STREAM_CODEC);
        output.writeCollection(this.potentialPois, BlockPos.STREAM_CODEC);
    }

    public static DebugBrainDump takeBrainDump(ServerLevel serverLevel, LivingEntity entity) {
        List<String> list;
        int n;
        Villager villager;
        boolean wantsGolem;
        InventoryCarrier inventoryCarrier;
        SimpleContainer inventory;
        int xp;
        String profession;
        String name = DebugEntityNameGenerator.getEntityName(entity);
        if (entity instanceof Villager) {
            Villager villager2 = (Villager)entity;
            profession = villager2.getVillagerData().profession().getRegisteredName();
            xp = villager2.getVillagerXp();
        } else {
            profession = "";
            xp = 0;
        }
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        Brain<? extends LivingEntity> brain = entity.getBrain();
        long gameTime = entity.level().getGameTime();
        String inventoryStr = entity instanceof InventoryCarrier ? ((inventory = (inventoryCarrier = (InventoryCarrier)((Object)entity)).getInventory()).isEmpty() ? "" : ((Object)inventory).toString()) : "";
        boolean bl = wantsGolem = entity instanceof Villager && (villager = (Villager)entity).wantsToSpawnGolem(gameTime);
        if (entity instanceof Warden) {
            Warden warden = (Warden)entity;
            n = warden.getClientAngerLevel();
        } else {
            n = -1;
        }
        int angerLevel = n;
        List<String> activities = brain.getActiveActivities().stream().map(Activity::getName).toList();
        List<String> behaviors = brain.getRunningBehaviors().stream().map(BehaviorControl::debugString).toList();
        List<String> memories = DebugBrainDump.getMemoryDescriptions(serverLevel, entity, gameTime);
        Set<BlockPos> pois = DebugBrainDump.getKnownBlockPositions(brain, MemoryModuleType.JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT);
        Set<BlockPos> potentialPois = DebugBrainDump.getKnownBlockPositions(brain, MemoryModuleType.POTENTIAL_JOB_SITE);
        if (entity instanceof Villager) {
            Villager villager3 = (Villager)entity;
            list = DebugBrainDump.getVillagerGossips(villager3);
        } else {
            list = List.of();
        }
        List<String> gossips = list;
        return new DebugBrainDump(name, profession, xp, health, maxHealth, inventoryStr, wantsGolem, angerLevel, activities, behaviors, memories, gossips, pois, potentialPois);
    }

    @SafeVarargs
    private static Set<BlockPos> getKnownBlockPositions(Brain<?> brain, MemoryModuleType<GlobalPos> ... memories) {
        return Stream.of(memories).filter(brain::hasMemoryValue).map(brain::getMemory).flatMap(Optional::stream).map(GlobalPos::pos).collect(Collectors.toSet());
    }

    private static List<String> getVillagerGossips(Villager villager) {
        ArrayList<String> gossips = new ArrayList<String>();
        villager.getGossips().getGossipEntries().forEach((uuid, entries) -> {
            String gossipeeName = DebugEntityNameGenerator.getEntityName(uuid);
            entries.forEach((gossipType, value) -> gossips.add(gossipeeName + ": " + String.valueOf(gossipType) + ": " + value));
        });
        return gossips;
    }

    private static List<String> getMemoryDescriptions(final ServerLevel level, LivingEntity body, final long timestamp) {
        final ArrayList<String> result = new ArrayList<String>();
        body.getBrain().forEach(new Brain.Visitor(){

            @Override
            public <U> void acceptEmpty(MemoryModuleType<U> type) {
                this.collectResult(type, Optional.empty(), OptionalLong.empty());
            }

            @Override
            public <U> void accept(MemoryModuleType<U> type, U value) {
                this.collectResult(type, Optional.of(value), OptionalLong.empty());
            }

            @Override
            public <U> void accept(MemoryModuleType<U> type, U value, long timeToLive) {
                this.collectResult(type, Optional.of(value), OptionalLong.of(timestamp));
            }

            private void collectResult(MemoryModuleType<?> memoryType, Optional<?> value, OptionalLong ttl) {
                String description = DebugBrainDump.getMemoryDescription(level, timestamp, memoryType, value, ttl);
                result.add(StringUtil.truncateStringIfNecessary(description, 255, true));
            }
        });
        Collections.sort(result);
        return result;
    }

    private static String getMemoryDescription(ServerLevel level, long timestamp, MemoryModuleType<?> memoryType, Optional<?> maybeValue, OptionalLong ttl) {
        Object description;
        if (maybeValue.isPresent()) {
            Object value = maybeValue.get();
            if (memoryType == MemoryModuleType.HEARD_BELL_TIME) {
                long timeSince = timestamp - (Long)value;
                description = timeSince + " ticks ago";
            } else {
                description = ttl.isPresent() ? DebugBrainDump.getShortDescription(level, value) + " (ttl: " + ttl.getAsLong() + ")" : DebugBrainDump.getShortDescription(level, value);
            }
        } else {
            description = "-";
        }
        return BuiltInRegistries.MEMORY_MODULE_TYPE.getKey(memoryType).getPath() + ": " + (String)description;
    }

    private static String getShortDescription(ServerLevel level, @Nullable Object obj) {
        Object object = obj;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{UUID.class, Entity.class, WalkTarget.class, EntityTracker.class, GlobalPos.class, BlockPosTracker.class, DamageSource.class, NearestVisibleLivingEntities.class, Collection.class}, (Object)object, n)) {
            case -1 -> "-";
            case 0 -> {
                UUID uuid = (UUID)object;
                yield DebugBrainDump.getShortDescription(level, level.getEntity(uuid));
            }
            case 1 -> {
                Entity entity = (Entity)object;
                yield DebugEntityNameGenerator.getEntityName(entity);
            }
            case 2 -> {
                WalkTarget walkTarget = (WalkTarget)object;
                yield DebugBrainDump.getShortDescription(level, walkTarget.getTarget());
            }
            case 3 -> {
                EntityTracker entityTracker = (EntityTracker)object;
                yield DebugBrainDump.getShortDescription(level, entityTracker.getEntity());
            }
            case 4 -> {
                GlobalPos globalPos = (GlobalPos)object;
                yield DebugBrainDump.getShortDescription(level, globalPos.pos());
            }
            case 5 -> {
                BlockPosTracker tracker = (BlockPosTracker)object;
                yield DebugBrainDump.getShortDescription(level, tracker.currentBlockPosition());
            }
            case 6 -> {
                DamageSource damageSource = (DamageSource)object;
                Entity entity = damageSource.getEntity();
                if (entity == null) {
                    yield obj.toString();
                }
                yield DebugBrainDump.getShortDescription(level, entity);
            }
            case 7 -> {
                NearestVisibleLivingEntities visibleEntities = (NearestVisibleLivingEntities)object;
                yield DebugBrainDump.getShortDescription(level, visibleEntities.nearbyEntities());
            }
            case 8 -> {
                Collection collection = (Collection)object;
                yield "[" + collection.stream().map(element -> DebugBrainDump.getShortDescription(level, element)).collect(Collectors.joining(", ")) + "]";
            }
            default -> obj.toString();
        };
    }

    public boolean hasPoi(BlockPos poiPos) {
        return this.pois.contains(poiPos);
    }

    public boolean hasPotentialPoi(BlockPos poiPos) {
        return this.potentialPois.contains(poiPos);
    }
}

