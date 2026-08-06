/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.google.common.collect.Sets$SetView
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 */
package net.minecraft.advancements;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.network.FriendlyByteBuf;

public record AdvancementRequirements(List<List<String>> requirements) {
    public static final Codec<AdvancementRequirements> CODEC = Codec.STRING.listOf().listOf().xmap(AdvancementRequirements::new, AdvancementRequirements::requirements);
    public static final AdvancementRequirements EMPTY = new AdvancementRequirements(List.of());

    public AdvancementRequirements(FriendlyByteBuf input) {
        this(input.readList(in -> in.readList(FriendlyByteBuf::readUtf)));
    }

    public void write(FriendlyByteBuf output) {
        output.writeCollection(this.requirements, (out, set) -> out.writeCollection(set, FriendlyByteBuf::writeUtf));
    }

    public static AdvancementRequirements allOf(Collection<String> criteria) {
        return new AdvancementRequirements(criteria.stream().map(List::of).toList());
    }

    public static AdvancementRequirements anyOf(Collection<String> criteria) {
        return new AdvancementRequirements(List.of(List.copyOf(criteria)));
    }

    public int size() {
        return this.requirements.size();
    }

    public boolean test(Predicate<String> predicate) {
        if (this.requirements.isEmpty()) {
            return false;
        }
        for (List<String> set : this.requirements) {
            if (AdvancementRequirements.anyMatch(set, predicate)) continue;
            return false;
        }
        return true;
    }

    public int count(Predicate<String> predicate) {
        int count = 0;
        for (List<String> set : this.requirements) {
            if (!AdvancementRequirements.anyMatch(set, predicate)) continue;
            ++count;
        }
        return count;
    }

    private static boolean anyMatch(List<String> criteria, Predicate<String> predicate) {
        for (String criterion : criteria) {
            if (!predicate.test(criterion)) continue;
            return true;
        }
        return false;
    }

    public DataResult<AdvancementRequirements> validate(Set<String> expectedCriteria) {
        ObjectOpenHashSet referencedCriteria = new ObjectOpenHashSet();
        for (List<String> set : this.requirements) {
            if (set.isEmpty() && expectedCriteria.isEmpty()) {
                return DataResult.error(() -> "Requirement entry cannot be empty");
            }
            referencedCriteria.addAll(set);
        }
        if (!expectedCriteria.equals(referencedCriteria)) {
            Sets.SetView missingCriteria = Sets.difference(expectedCriteria, (Set)referencedCriteria);
            Sets.SetView unknownCriteria = Sets.difference((Set)referencedCriteria, expectedCriteria);
            return DataResult.error(() -> AdvancementRequirements.lambda$validate$1((Set)missingCriteria, (Set)unknownCriteria));
        }
        return DataResult.success((Object)this);
    }

    public boolean isEmpty() {
        return this.requirements.isEmpty();
    }

    @Override
    public String toString() {
        return this.requirements.toString();
    }

    public Set<String> names() {
        ObjectOpenHashSet names = new ObjectOpenHashSet();
        for (List<String> set : this.requirements) {
            names.addAll(set);
        }
        return names;
    }

    private static /* synthetic */ String lambda$validate$1(Set missingCriteria, Set unknownCriteria) {
        return "Advancement completion requirements did not exactly match specified criteria. Missing: " + String.valueOf(missingCriteria) + ". Unknown: " + String.valueOf(unknownCriteria);
    }

    public static interface Strategy {
        public static final Strategy AND = AdvancementRequirements::allOf;
        public static final Strategy OR = AdvancementRequirements::anyOf;

        public AdvancementRequirements create(Collection<String> var1);
    }
}

