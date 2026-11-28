package com.xgdt.dx12.mixin;

import com.xgdt.dx12.lod.LodRegionCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 区块保存时只失效这一块的 LOD 缓存。
 *
 * <p>{@code SerializableChunkData.copyOf} 是服务端/单人服务器把区块写成 NBT 前的
 * 序列化入口，每次保存都会经过。这里拿到 {@link ChunkPos}，让
 * {@link LodRegionCache#invalidate(int, int)} 删掉该区块的列/体素存储记录和网格，
 * 下次 LOD 采样会重新读这份新 NBT。其它区块的缓存保持命中，不会因为一次自动存档
 * 触发十万级 NBT 重读。
 */
@Mixin(SerializableChunkData.class)
public class SerializableChunkDataSaveMixin {

    @Inject(method = "copyOf", at = @At("RETURN"), remap = false)
    private static void dx12$invalidateLodOnSave(ServerLevel level, ChunkAccess chunk,
                                                 CallbackInfoReturnable<SerializableChunkData> cir) {
        try {
            // 只失效真正改过的区块。世界加载/自动存档也会序列化干净区块，
            // 无差别失效会让 LOD 网格不断重建，直接表现为闪烁。
            if (chunk.isUnsaved()) {
                ChunkPos pos = chunk.getPos();
                LodRegionCache.getInstance().invalidate(pos.x(), pos.z());
            }
        } catch (Throwable ignored) {
            // LOD 缓存是派生数据，失效失败不能影响存档
        }
    }
}
