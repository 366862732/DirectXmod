package com.xgdt.dx11.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xgdt.dx11.perf.AmbientParticleChance;
import com.xgdt.dx11.perf.MicroStats;
import com.xgdt.dx11.perf.PerfState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Skips the biome-particle attribute lookup when no particle can spawn.
 *
 * <p>Vanilla's {@code doAnimateTick} guards the particle list with
 * {@code !blockState.isCollisionShapeFullBlock(...)} and then calls
 * {@code environmentAttributes().getValue(AMBIENT_PARTICLES, pos)}.  That call
 * converts the block position to a {@code Vec3}, resolves the blended attribute
 * value and allocates its result, once per animated section per tick, even
 * though the list is empty for most biomes and most of the time no entry can
 * pass its probability check.</p>
 *
 * <p>This follows Lithium's approach: draw one random number and skip the
 * lookup when it cannot beat the highest probability recorded in any biome.  A
 * false result from the wrapped call means "not a full collision shape", which
 * is the branch that enters the particle loop, so returning {@code true}
 * short-circuits the loop exactly like a full block would.  The RNG draw moves
 * earlier than vanilla's {@code AmbientParticle.canSpawn} draw, which only
 * affects cosmetic client randomness.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(ClientLevel.class)
public abstract class ClientLevelAnimateTickMixin {
    @WrapOperation(
        method = "doAnimateTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;isCollisionShapeFullBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean dx11$skipImpossibleAmbientParticles(
        BlockState state,
        BlockGetter level,
        BlockPos position,
        Operation<Boolean> original) {
        if (!PerfState.layerActive() || !PerfState.config().fastAmbientParticles) {
            return original.call(state, level, position);
        }
        float maximum = AmbientParticleChance.max();
        if (maximum <= 0.0F) {
            MicroStats.ambientParticleSkipped();
            return true;
        }
        RandomSource random = ((Level) (Object) this).random;
        if (random.nextFloat() > maximum) {
            MicroStats.ambientParticleSkipped();
            return true;
        }
        return original.call(state, level, position);
    }
}
