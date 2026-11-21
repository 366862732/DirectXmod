package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.PerfState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Visits only the entity sections inside a small query box.
 *
 * <p>Vanilla {@code forEachAccessibleNonEmptySection} walks the per-column
 * sorted set of section ids for every x coordinate and filters out the y/z
 * values that fall outside the box.  A player-sized query therefore touches
 * every existing section in that x column, including all the ones far above and
 * below and in neighbouring z chunks.</p>
 *
 * <p>For boxes spanning at most four sections per horizontal axis, iterating the
 * y/z range directly performs a handful of hash lookups instead of scanning the
 * sorted set.  The section ids are packed with x in the high bits, then z, then
 * y, so the nested loops reproduce vanilla's ascending order (and its abort
 * behaviour).  Longer boxes keep the vanilla path: there the set scan is cheaper
 * than one lookup per candidate section.</p>
 *
 * <p>The bounds are recomputed from the {@link AABB} rather than captured from
 * vanilla locals: the four margins are part of the box contract (entities are
 * two blocks wide and four tall at most), and this keeps the hook independent of
 * vanilla's local variable layout.</p>
 *
 * <p>This is the Lithium approach, reimplemented for 1.21.11's
 * {@code SectionPos} layout.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(EntitySectionStorage.class)
public abstract class EntitySectionStorageMixin<T extends EntityAccess> {
    @Shadow
    @Nullable
    public abstract EntitySection<T> getSection(long sectionPos);

    @Inject(method = "forEachAccessibleNonEmptySection", at = @At("HEAD"), cancellable = true)
    private void dx11$visitOnlySectionsInSmallBox(
        AABB box,
        AbortableIterationConsumer<EntitySection<T>> action,
        CallbackInfo ci) {
        if (!PerfState.layerActive() || !PerfState.config().fastEntityQueries) {
            return;
        }

        // MAX_NON_CHONKY_ENTITY_SIZE: two blocks wide, four blocks tall.
        int minX = SectionPos.posToSectionCoord(box.minX - 2.0D);
        int minY = SectionPos.posToSectionCoord(box.minY - 4.0D);
        int minZ = SectionPos.posToSectionCoord(box.minZ - 2.0D);
        int maxX = SectionPos.posToSectionCoord(box.maxX + 2.0D);
        int maxY = SectionPos.posToSectionCoord(box.maxY);
        int maxZ = SectionPos.posToSectionCoord(box.maxZ + 2.0D);

        if (maxX >= minX + 4 || maxZ >= minZ + 4) {
            return;
        }
        ci.cancel();
        com.xgdt.dx11.perf.MicroStats.entityQueryFastPath();

        for (int x = minX; x <= maxX; x++) {
            for (int z = Math.max(minZ, 0); z <= maxZ; z++) {
                if (dx11$visitColumn(x, minY, maxY, z, action).shouldAbort()) {
                    return;
                }
            }
            int negativeZBound = Math.min(-1, maxZ);
            for (int z = minZ; z <= negativeZBound; z++) {
                if (dx11$visitColumn(x, minY, maxY, z, action).shouldAbort()) {
                    return;
                }
            }
        }
    }

    @Unique
    private AbortableIterationConsumer.Continuation dx11$visitColumn(
        int x,
        int minY,
        int maxY,
        int z,
        AbortableIterationConsumer<EntitySection<T>> action) {
        AbortableIterationConsumer.Continuation result = AbortableIterationConsumer.Continuation.CONTINUE;
        for (int y = Math.max(minY, 0); y <= maxY; y++) {
            result = dx11$visitSection(SectionPos.asLong(x, y, z), action);
            if (result.shouldAbort()) {
                return result;
            }
        }
        int negativeYBound = Math.min(-1, maxY);
        for (int y = minY; y <= negativeYBound; y++) {
            result = dx11$visitSection(SectionPos.asLong(x, y, z), action);
            if (result.shouldAbort()) {
                return result;
            }
        }
        return result;
    }

    @Unique
    private AbortableIterationConsumer.Continuation dx11$visitSection(
        long sectionPos, AbortableIterationConsumer<EntitySection<T>> action) {
        EntitySection<T> section = this.getSection(sectionPos);
        if (section != null && !section.isEmpty() && section.getStatus().isAccessible()) {
            return action.accept(section);
        }
        return AbortableIterationConsumer.Continuation.CONTINUE;
    }
}
