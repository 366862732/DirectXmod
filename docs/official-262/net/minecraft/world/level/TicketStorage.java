/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMaps
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TicketStorage
extends SavedData {
    private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<Pair<ChunkPos, Ticket>> TICKET_ENTRY = Codec.mapPair((MapCodec)ChunkPos.CODEC.fieldOf("chunk_pos"), Ticket.CODEC).codec();
    public static final Codec<TicketStorage> CODEC = RecordCodecBuilder.create(i -> i.group((App)TICKET_ENTRY.listOf().optionalFieldOf("tickets", List.of()).forGetter(TicketStorage::packTickets)).apply((Applicative)i, TicketStorage::fromPacked));
    public static final SavedDataType<TicketStorage> TYPE = new SavedDataType<TicketStorage>(Identifier.withDefaultNamespace("chunk_tickets"), TicketStorage::new, CODEC, DataFixTypes.SAVED_DATA_FORCED_CHUNKS);
    private final Long2ObjectOpenHashMap<List<Ticket>> tickets;
    private final Long2ObjectOpenHashMap<List<Ticket>> deactivatedTickets;
    private LongSet chunksWithForcedTickets = new LongOpenHashSet();
    private @Nullable ChunkUpdated loadingChunkUpdatedListener;
    private @Nullable ChunkUpdated simulationChunkUpdatedListener;

    private TicketStorage(Long2ObjectOpenHashMap<List<Ticket>> tickets, Long2ObjectOpenHashMap<List<Ticket>> deactivatedTickets) {
        this.tickets = tickets;
        this.deactivatedTickets = deactivatedTickets;
        this.updateForcedChunks();
    }

    public TicketStorage() {
        this((Long2ObjectOpenHashMap<List<Ticket>>)new Long2ObjectOpenHashMap(4), (Long2ObjectOpenHashMap<List<Ticket>>)new Long2ObjectOpenHashMap());
    }

    private static TicketStorage fromPacked(List<Pair<ChunkPos, Ticket>> tickets) {
        Long2ObjectOpenHashMap ticketsToLoad = new Long2ObjectOpenHashMap();
        for (Pair<ChunkPos, Ticket> ticket : tickets) {
            ChunkPos pos = (ChunkPos)ticket.getFirst();
            List ticketsInChunk = (List)ticketsToLoad.computeIfAbsent(pos.pack(), k -> new ObjectArrayList(4));
            ticketsInChunk.add((Ticket)ticket.getSecond());
        }
        return new TicketStorage((Long2ObjectOpenHashMap<List<Ticket>>)new Long2ObjectOpenHashMap(4), (Long2ObjectOpenHashMap<List<Ticket>>)ticketsToLoad);
    }

    private List<Pair<ChunkPos, Ticket>> packTickets() {
        ArrayList<Pair<ChunkPos, Ticket>> tickets = new ArrayList<Pair<ChunkPos, Ticket>>();
        this.forEachTicket((pos, ticket) -> {
            if (ticket.getType().persist()) {
                tickets.add(new Pair(pos, ticket));
            }
        });
        return tickets;
    }

    private void forEachTicket(BiConsumer<ChunkPos, Ticket> output) {
        TicketStorage.forEachTicket(output, this.tickets);
        TicketStorage.forEachTicket(output, this.deactivatedTickets);
    }

    private static void forEachTicket(BiConsumer<ChunkPos, Ticket> output, Long2ObjectOpenHashMap<List<Ticket>> tickets) {
        for (Long2ObjectMap.Entry entry : Long2ObjectMaps.fastIterable(tickets)) {
            ChunkPos chunkPos = ChunkPos.unpack(entry.getLongKey());
            for (Ticket ticket : (List)entry.getValue()) {
                output.accept(chunkPos, ticket);
            }
        }
    }

    public void activateAllDeactivatedTickets() {
        for (Long2ObjectMap.Entry entry : Long2ObjectMaps.fastIterable(this.deactivatedTickets)) {
            for (Ticket ticket : (List)entry.getValue()) {
                this.addTicket(entry.getLongKey(), ticket);
            }
        }
        this.deactivatedTickets.clear();
    }

    public void setLoadingChunkUpdatedListener(@Nullable ChunkUpdated loadingChunkUpdatedListener) {
        this.loadingChunkUpdatedListener = loadingChunkUpdatedListener;
    }

    public void setSimulationChunkUpdatedListener(@Nullable ChunkUpdated simulationChunkUpdatedListener) {
        this.simulationChunkUpdatedListener = simulationChunkUpdatedListener;
    }

    public boolean hasTickets() {
        return !this.tickets.isEmpty();
    }

    public boolean shouldKeepDimensionActive() {
        for (List group : this.tickets.values()) {
            for (Ticket ticket : group) {
                if (!ticket.getType().shouldKeepDimensionActive()) continue;
                return true;
            }
        }
        return false;
    }

    public List<Ticket> getTickets(long key) {
        return (List)this.tickets.getOrDefault(key, List.of());
    }

    private List<Ticket> getOrCreateTickets(long key) {
        return (List)this.tickets.computeIfAbsent(key, k -> new ObjectArrayList(4));
    }

    public void addTicketWithRadius(TicketType type, ChunkPos chunkPos, int radius) {
        Ticket ticket = new Ticket(type, ChunkLevel.byStatus(FullChunkStatus.FULL) - radius);
        this.addTicket(chunkPos.pack(), ticket);
    }

    public void addTicket(Ticket ticket, ChunkPos chunkPos) {
        this.addTicket(chunkPos.pack(), ticket);
    }

    public boolean addTicket(long key, Ticket ticket) {
        List<Ticket> tickets = this.getOrCreateTickets(key);
        for (Ticket t : tickets) {
            if (!TicketStorage.isTicketSameTypeAndLevel(ticket, t)) continue;
            t.resetTicksLeft();
            this.setDirty();
            return false;
        }
        int oldSimulationTicketLevel = TicketStorage.getTicketLevelAt(tickets, true);
        int oldLoadingTicketLevel = TicketStorage.getTicketLevelAt(tickets, false);
        tickets.add(ticket);
        if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("ATI {} {}", (Object)ChunkPos.unpack(key), (Object)ticket);
        }
        if (ticket.getType().doesSimulate() && ticket.getTicketLevel() < oldSimulationTicketLevel && this.simulationChunkUpdatedListener != null) {
            this.simulationChunkUpdatedListener.update(key, ticket.getTicketLevel(), true);
        }
        if (ticket.getType().doesLoad() && ticket.getTicketLevel() < oldLoadingTicketLevel && this.loadingChunkUpdatedListener != null) {
            this.loadingChunkUpdatedListener.update(key, ticket.getTicketLevel(), true);
        }
        if (ticket.getType().equals(TicketType.FORCED)) {
            this.chunksWithForcedTickets.add(key);
        }
        this.setDirty();
        return true;
    }

    private static boolean isTicketSameTypeAndLevel(Ticket ticket, Ticket t) {
        return t.getType() == ticket.getType() && t.getTicketLevel() == ticket.getTicketLevel();
    }

    public int getTicketLevelAt(long key, boolean simulation) {
        return TicketStorage.getTicketLevelAt(this.getTickets(key), simulation);
    }

    private static int getTicketLevelAt(List<Ticket> tickets, boolean simulation) {
        Ticket lowestTicket = TicketStorage.getLowestTicket(tickets, simulation);
        return lowestTicket == null ? ChunkLevel.MAX_LEVEL + 1 : lowestTicket.getTicketLevel();
    }

    private static @Nullable Ticket getLowestTicket(@Nullable List<Ticket> tickets, boolean simulation) {
        if (tickets == null) {
            return null;
        }
        Ticket t = null;
        for (Ticket ticket : tickets) {
            if (t != null && ticket.getTicketLevel() >= t.getTicketLevel()) continue;
            if (simulation && ticket.getType().doesSimulate()) {
                t = ticket;
                continue;
            }
            if (simulation || !ticket.getType().doesLoad()) continue;
            t = ticket;
        }
        return t;
    }

    public void removeTicketWithRadius(TicketType type, ChunkPos chunkPos, int radius) {
        Ticket ticket = new Ticket(type, ChunkLevel.byStatus(FullChunkStatus.FULL) - radius);
        this.removeTicket(chunkPos.pack(), ticket);
    }

    public void removeTicket(Ticket ticket, ChunkPos chunkPos) {
        this.removeTicket(chunkPos.pack(), ticket);
    }

    public boolean removeTicket(long key, Ticket ticket) {
        List tickets = (List)this.tickets.get(key);
        if (tickets == null) {
            return false;
        }
        boolean found = false;
        Iterator iterator = tickets.iterator();
        while (iterator.hasNext()) {
            Ticket t = (Ticket)iterator.next();
            if (!TicketStorage.isTicketSameTypeAndLevel(ticket, t)) continue;
            iterator.remove();
            if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
                LOGGER.debug("RTI {} {}", (Object)ChunkPos.unpack(key), (Object)t);
            }
            found = true;
            break;
        }
        if (!found) {
            return false;
        }
        if (tickets.isEmpty()) {
            this.tickets.remove(key);
        }
        if (ticket.getType().doesSimulate() && this.simulationChunkUpdatedListener != null) {
            this.simulationChunkUpdatedListener.update(key, TicketStorage.getTicketLevelAt(tickets, true), false);
        }
        if (ticket.getType().doesLoad() && this.loadingChunkUpdatedListener != null) {
            this.loadingChunkUpdatedListener.update(key, TicketStorage.getTicketLevelAt(tickets, false), false);
        }
        if (ticket.getType().equals(TicketType.FORCED)) {
            this.updateForcedChunks();
        }
        this.setDirty();
        return true;
    }

    private void updateForcedChunks() {
        this.chunksWithForcedTickets = this.getAllChunksWithTicketThat(t -> t.getType().equals(TicketType.FORCED));
    }

    public String getTicketDebugString(long key, boolean simulation) {
        List<Ticket> tickets = this.getTickets(key);
        Ticket lowestTicket = TicketStorage.getLowestTicket(tickets, simulation);
        return lowestTicket == null ? "no_ticket" : lowestTicket.toString();
    }

    public void purgeStaleTickets(ChunkMap chunkMap) {
        this.removeTicketIf((ticket, chunkPos) -> {
            if (this.canTicketExpire(chunkMap, ticket, chunkPos)) {
                ticket.decreaseTicksLeft();
                return ticket.isTimedOut();
            }
            return false;
        }, null);
        this.setDirty();
    }

    private boolean canTicketExpire(ChunkMap chunkMap, Ticket ticket, long chunkPos) {
        if (!ticket.getType().hasTimeout()) {
            return false;
        }
        if (ticket.getType().canExpireIfUnloaded()) {
            return true;
        }
        ChunkHolder updatingChunk = chunkMap.getUpdatingChunkIfPresent(chunkPos);
        return updatingChunk == null || updatingChunk.isReadyForSaving();
    }

    public void deactivateTicketsOnClosing() {
        this.removeTicketIf((ticket, chunkPos) -> ticket.getType() != TicketType.UNKNOWN, this.deactivatedTickets);
    }

    public void removeTicketIf(TicketPredicate predicate, @Nullable Long2ObjectOpenHashMap<List<Ticket>> removedTickets) {
        ObjectIterator ticketsPerChunkIterator = this.tickets.long2ObjectEntrySet().fastIterator();
        boolean removedForced = false;
        while (ticketsPerChunkIterator.hasNext()) {
            Long2ObjectMap.Entry entry = (Long2ObjectMap.Entry)ticketsPerChunkIterator.next();
            Iterator chunkTicketsIterator = ((List)entry.getValue()).iterator();
            long chunkPos = entry.getLongKey();
            boolean removedSimulation = false;
            boolean removedLoading = false;
            while (chunkTicketsIterator.hasNext()) {
                Ticket ticket = (Ticket)chunkTicketsIterator.next();
                if (!predicate.test(ticket, chunkPos)) continue;
                if (removedTickets != null) {
                    List tickets = (List)removedTickets.computeIfAbsent(chunkPos, k -> new ObjectArrayList(((List)entry.getValue()).size()));
                    tickets.add(ticket);
                }
                chunkTicketsIterator.remove();
                if (ticket.getType().doesLoad()) {
                    removedLoading = true;
                }
                if (ticket.getType().doesSimulate()) {
                    removedSimulation = true;
                }
                if (!ticket.getType().equals(TicketType.FORCED)) continue;
                removedForced = true;
            }
            if (!removedLoading && !removedSimulation) continue;
            if (removedLoading && this.loadingChunkUpdatedListener != null) {
                this.loadingChunkUpdatedListener.update(chunkPos, TicketStorage.getTicketLevelAt((List)entry.getValue(), false), false);
            }
            if (removedSimulation && this.simulationChunkUpdatedListener != null) {
                this.simulationChunkUpdatedListener.update(chunkPos, TicketStorage.getTicketLevelAt((List)entry.getValue(), true), false);
            }
            this.setDirty();
            if (!((List)entry.getValue()).isEmpty()) continue;
            ticketsPerChunkIterator.remove();
        }
        if (removedForced) {
            this.updateForcedChunks();
        }
    }

    public void replaceTicketLevelOfType(int newLevel, TicketType ticketType) {
        ArrayList<Pair> affectedTickets = new ArrayList<Pair>();
        for (Long2ObjectMap.Entry entry : this.tickets.long2ObjectEntrySet()) {
            for (Ticket ticket : (List)entry.getValue()) {
                if (ticket.getType() != ticketType) continue;
                affectedTickets.add(Pair.of((Object)ticket, (Object)entry.getLongKey()));
            }
        }
        for (Pair pair : affectedTickets) {
            Ticket ticket;
            Long key = (Long)pair.getSecond();
            ticket = (Ticket)pair.getFirst();
            this.removeTicket(key, ticket);
            TicketType type = ticket.getType();
            this.addTicket(key, new Ticket(type, newLevel));
        }
    }

    public boolean updateChunkForced(ChunkPos chunkPos, boolean forced) {
        Ticket ticket = new Ticket(TicketType.FORCED, ChunkMap.FORCED_TICKET_LEVEL);
        if (forced) {
            return this.addTicket(chunkPos.pack(), ticket);
        }
        return this.removeTicket(chunkPos.pack(), ticket);
    }

    public LongSet getForceLoadedChunks() {
        return this.chunksWithForcedTickets;
    }

    private LongSet getAllChunksWithTicketThat(Predicate<Ticket> ticketCheck) {
        LongOpenHashSet chunks = new LongOpenHashSet();
        block0: for (Long2ObjectMap.Entry entry : Long2ObjectMaps.fastIterable(this.tickets)) {
            for (Ticket ticket : (List)entry.getValue()) {
                if (!ticketCheck.test(ticket)) continue;
                chunks.add(entry.getLongKey());
                continue block0;
            }
        }
        return chunks;
    }

    @FunctionalInterface
    public static interface ChunkUpdated {
        public void update(long var1, int var3, boolean var4);
    }

    public static interface TicketPredicate {
        public boolean test(Ticket var1, long var2);
    }
}

