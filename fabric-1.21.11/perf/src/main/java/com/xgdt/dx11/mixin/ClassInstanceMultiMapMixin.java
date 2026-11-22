package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.MicroStats;
import com.xgdt.dx11.perf.PerfState;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Speeds up the per-class entity lists used by every entity query.
 *
 * <p>{@code ClassInstanceMultiMap} backs {@code EntitySection}.  Vanilla stores
 * the type buckets in a {@code HashMap} keyed by {@code Class}, and
 * {@code find(Class)} validates the requested type with
 * {@code isAssignableFrom} and then calls {@code computeIfAbsent} with a
 * capturing lambda on every query, even for a type that was resolved long
 * ago.</p>
 *
 * <p>Two changes, both from Lithium:</p>
 * <ul>
 *   <li>swap the map for fastutil's reference-keyed open hash map, which avoids
 *       the entry objects and identity boxing of {@code HashMap};</li>
 *   <li>answer already-resolved types straight from the map, skipping the
 *       assignability check and the lambda allocation.  Types can only be
 *       inserted by this class after a successful check, so the fast path
 *       cannot hide an invalid query.</li>
 * </ul>
 *
 * <p>Both run only while the perf layer is live; otherwise the vanilla map and
 * query path stay untouched.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(ClassInstanceMultiMap.class)
public class ClassInstanceMultiMapMixin {
    @Mutable
    @Shadow
    @Final
    private Map<Class<?>, List<?>> byClass;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void dx11$useReferenceMap(Class<?> elementType, CallbackInfo ci) {
        if (!PerfState.layerActive() || !PerfState.config().fastEntityLookup) {
            return;
        }
        this.byClass = new Reference2ReferenceOpenHashMap<>(this.byClass);
    }

    @Inject(method = "find", at = @At("HEAD"), cancellable = true)
    private void dx11$reuseResolvedType(
        Class<?> type, CallbackInfoReturnable<Collection<?>> cir) {
        if (!PerfState.layerActive() || !PerfState.config().fastEntityLookup) {
            return;
        }
        List<?> cached = this.byClass.get(type);
        if (cached != null) {
            MicroStats.entityLookupHit();
            // Vanilla returns the raw bucket list for an already-resolved type,
            // so returning it directly matches the vanilla contract; wrapping
            // it in Collections.unmodifiableCollection allocated a new view
            // object on every entity query.
            cir.setReturnValue(cached);
        }
    }
}
