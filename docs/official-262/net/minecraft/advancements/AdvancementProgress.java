/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class AdvancementProgress
implements Comparable<AdvancementProgress> {
    private static final DateTimeFormatter OBTAINED_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
    private static final Codec<Instant> OBTAINED_TIME_CODEC = ExtraCodecs.temporalCodec(OBTAINED_TIME_FORMAT).xmap(Instant::from, instant -> instant.atZone(ZoneId.systemDefault()));
    private static final Codec<Map<String, CriterionProgress>> CRITERIA_CODEC = Codec.unboundedMap((Codec)Codec.STRING, OBTAINED_TIME_CODEC).xmap(map -> Util.mapValues(map, CriterionProgress::new), map -> map.entrySet().stream().filter(e -> ((CriterionProgress)e.getValue()).isDone()).collect(Collectors.toMap(Map.Entry::getKey, e -> Objects.requireNonNull(((CriterionProgress)e.getValue()).getObtained()))));
    public static final Codec<AdvancementProgress> CODEC = RecordCodecBuilder.create(i -> i.group((App)CRITERIA_CODEC.optionalFieldOf("criteria", Map.of()).forGetter(a -> a.criteria), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "done", true).forGetter(AdvancementProgress::isDone)).apply((Applicative)i, (criteria, done) -> new AdvancementProgress(new HashMap<String, CriterionProgress>((Map<String, CriterionProgress>)criteria))));
    private final Map<String, CriterionProgress> criteria;
    private AdvancementRequirements requirements = AdvancementRequirements.EMPTY;

    private AdvancementProgress(Map<String, CriterionProgress> criteria) {
        this.criteria = criteria;
    }

    public AdvancementProgress() {
        this.criteria = Maps.newHashMap();
    }

    public void update(AdvancementRequirements requirements) {
        Set<String> names = requirements.names();
        this.criteria.entrySet().removeIf(entry -> !names.contains(entry.getKey()));
        for (String name : names) {
            this.criteria.putIfAbsent(name, new CriterionProgress());
        }
        this.requirements = requirements;
    }

    public boolean isDone() {
        return this.requirements.test(this::isCriterionDone);
    }

    public boolean hasProgress() {
        for (CriterionProgress progress : this.criteria.values()) {
            if (!progress.isDone()) continue;
            return true;
        }
        return false;
    }

    public boolean grantProgress(String name) {
        CriterionProgress progress = this.criteria.get(name);
        if (progress != null && !progress.isDone()) {
            progress.grant();
            return true;
        }
        return false;
    }

    public boolean revokeProgress(String name) {
        CriterionProgress progress = this.criteria.get(name);
        if (progress != null && progress.isDone()) {
            progress.revoke();
            return true;
        }
        return false;
    }

    public String toString() {
        return "AdvancementProgress{criteria=" + String.valueOf(this.criteria) + ", requirements=" + String.valueOf(this.requirements) + "}";
    }

    public void serializeToNetwork(FriendlyByteBuf output) {
        output.writeMap(this.criteria, FriendlyByteBuf::writeUtf, (b, v) -> v.serializeToNetwork((FriendlyByteBuf)((Object)b)));
    }

    public static AdvancementProgress fromNetwork(FriendlyByteBuf input) {
        Map<String, CriterionProgress> criteria = input.readMap(FriendlyByteBuf::readUtf, CriterionProgress::fromNetwork);
        return new AdvancementProgress(criteria);
    }

    public @Nullable CriterionProgress getCriterion(String id) {
        return this.criteria.get(id);
    }

    private boolean isCriterionDone(String criterion) {
        CriterionProgress progress = this.getCriterion(criterion);
        return progress != null && progress.isDone();
    }

    public float getPercent() {
        if (this.criteria.isEmpty()) {
            return 0.0f;
        }
        float total = this.requirements.size();
        float complete = this.countCompletedRequirements();
        return complete / total;
    }

    public @Nullable Component getProgressText() {
        if (this.criteria.isEmpty()) {
            return null;
        }
        int total = this.requirements.size();
        if (total <= 1) {
            return null;
        }
        int complete = this.countCompletedRequirements();
        return Component.translatable("advancements.progress", complete, total);
    }

    private int countCompletedRequirements() {
        return this.requirements.count(this::isCriterionDone);
    }

    public Iterable<String> getRemainingCriteria() {
        ArrayList remaining = Lists.newArrayList();
        for (Map.Entry<String, CriterionProgress> entry : this.criteria.entrySet()) {
            if (entry.getValue().isDone()) continue;
            remaining.add(entry.getKey());
        }
        return remaining;
    }

    public Iterable<String> getCompletedCriteria() {
        ArrayList completed = Lists.newArrayList();
        for (Map.Entry<String, CriterionProgress> entry : this.criteria.entrySet()) {
            if (!entry.getValue().isDone()) continue;
            completed.add(entry.getKey());
        }
        return completed;
    }

    public @Nullable Instant getFirstProgressDate() {
        return this.criteria.values().stream().map(CriterionProgress::getObtained).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
    }

    @Override
    public int compareTo(AdvancementProgress o) {
        Instant ourSmallestDate = this.getFirstProgressDate();
        Instant theirSmallestDate = o.getFirstProgressDate();
        if (ourSmallestDate == null && theirSmallestDate != null) {
            return 1;
        }
        if (ourSmallestDate != null && theirSmallestDate == null) {
            return -1;
        }
        if (ourSmallestDate == null && theirSmallestDate == null) {
            return 0;
        }
        return ourSmallestDate.compareTo(theirSmallestDate);
    }
}

