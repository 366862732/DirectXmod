package com.xgdt.dx12.dx12;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * P33 async：fence 完成回调。
 *
 * <p>由 {@link Dx12Native#dx12AsyncFenceRegisterCallback} 注册，native 侧
 * {@code AsyncFenceManager} 监控线程在 fence 达到目标值时，通过
 * {@code AttachCurrentThread} 回到 JVM 调用本方法（非轮询）。
 *
 * <p>注意：方法名与签名 {@code (JZ)V} 由 native 侧固定查找，必须严格一致；
 * 回调在 native 监控线程上执行，实现须自行保证线程安全，且应尽量短小
 * （内部会删除对应的 global ref，回调结束后该对象不再被引用）。
 */
@Environment(EnvType.CLIENT)
public interface Dx12FenceCallback {
    /**
     * fence 达到登记值（或等待超时）时调用。
     *
     * @param fenceValue native 侧本次 fence 的完成值
     * @param success    true = 正常达成；false = 超时或失败
     */
    void onFenceComplete(long fenceValue, boolean success);
}
