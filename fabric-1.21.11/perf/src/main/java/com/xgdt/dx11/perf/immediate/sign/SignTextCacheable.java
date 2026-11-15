package com.xgdt.dx11.perf.immediate.sign;

/**
 * Per-{@code SignText} state for the sign atlas.
 *
 * <p>Implemented by the {@code SignText} mixin.  The key is cached on the
 * instance because {@code SignText} is effectively immutable (setters build a
 * new object), so the renderer can reuse the same key object every frame
 * without allocating arrays or recomputing hashes.</p>
 */
public interface SignTextCacheable {
    boolean dx11$shouldCache();

    void dx11$setShouldCache(boolean shouldCache);

    SignTextKey dx11$signTextKey(boolean filtering, int lineHeight, int maxLineWidth);
}
