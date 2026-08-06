/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.DataResult
 */
package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.ValidationContext;

public interface Validatable {
    public void validate(ValidationContext var1);

    public static void validate(ValidationContext context, String name, Validatable v) {
        v.validate(context.forField(name));
    }

    public static void validate(ValidationContext context, String name, Optional<? extends Validatable> optional) {
        optional.ifPresent(v -> v.validate(context.forField(name)));
    }

    public static void validate(ValidationContext context, String name, List<? extends Validatable> list) {
        for (int i = 0; i < list.size(); ++i) {
            list.get(i).validate(context.forIndexedField(name, i));
        }
    }

    public static void validate(ValidationContext context, List<? extends Validatable> list) {
        for (int i = 0; i < list.size(); ++i) {
            list.get(i).validate(context.forChild(new ProblemReporter.IndexedPathElement(i)));
        }
    }

    public static <T extends Validatable> void validateReference(ValidationContext context, ResourceKey<T> id) {
        if (!context.allowsReferences()) {
            context.reportProblem(new ValidationContext.ReferenceNotAllowedProblem(id));
            return;
        }
        if (context.hasVisitedElement(id)) {
            context.reportProblem(new ValidationContext.RecursiveReferenceProblem(id));
            return;
        }
        context.resolver().get(id).ifPresentOrElse(element -> ((Validatable)element.value()).validate(context.enterElement(new ProblemReporter.ElementReferencePathElement(id), id)), () -> context.reportProblem(new ValidationContext.MissingReferenceProblem(id)));
    }

    public static <T extends Validatable> Function<T, DataResult<T>> validatorForContext(ContextKeySet params) {
        return v -> {
            ProblemReporter.Collector problemCollector = new ProblemReporter.Collector();
            ValidationContext validationContext = new ValidationContext(problemCollector, params);
            v.validate(validationContext);
            if (!problemCollector.isEmpty()) {
                return DataResult.error(() -> "Validation error: " + problemCollector.getReport());
            }
            return DataResult.success((Object)v);
        };
    }

    public static <T extends Validatable> Function<List<T>, DataResult<List<T>>> listValidatorForContext(ContextKeySet params) {
        return v -> {
            ProblemReporter.Collector problemCollector = new ProblemReporter.Collector();
            ValidationContext validationContext = new ValidationContext(problemCollector, params);
            Validatable.validate(validationContext, v);
            if (!problemCollector.isEmpty()) {
                return DataResult.error(() -> "Validation error: " + problemCollector.getReport());
            }
            return DataResult.success((Object)v);
        };
    }
}

