package com.xgdt.dx11.perf.texture;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Counters for the stb atlas packer.
 *
 * <p>Stitching runs on the resource-reload worker threads, so the counters are
 * atomics and the last-atlas description is volatile.  The numbers are cold
 * (a handful of atlases per reload), so the cost is irrelevant next to packing
 * a few thousand rectangles.</p>
 *
 * <p>{@code /dx11perf stats} prints one line.  "packed" counts atlases that
 * really went through stb, "skipped" counts atlases below the sprite threshold
 * that intentionally stayed vanilla, and "fellBack" counts atlases that were
 * large enough but could not be packed.  A non-zero "fellBack" with no visible
 * problem just means vanilla packed that atlas.</p>
 */
public final class StitcherStats {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");

    private static final AtomicLong atlasesPacked = new AtomicLong();
    private static final AtomicLong spritesPacked = new AtomicLong();
    private static final AtomicLong atlasesSkipped = new AtomicLong();
    private static final AtomicLong atlasesFellBack = new AtomicLong();
    private static volatile String lastAtlas = "none";

    private StitcherStats() {
    }

    /** Records an atlas that stb packed.  Called once per atlas, before upload. */
    public static void packed(int spriteCount, int width, int height) {
        atlasesPacked.incrementAndGet();
        spritesPacked.addAndGet(spriteCount);
        lastAtlas = spriteCount + " sprites/" + width + "x" + height;
        LOGGER.info(
            "[dx11-perf] stb packed {} sprites into {}x{}", spriteCount, width, height);
    }

    /** Records an atlas below the sprite threshold that kept the vanilla pack. */
    public static void skipped() {
        atlasesSkipped.incrementAndGet();
    }

    /** Records an atlas the packer refused, so vanilla packed it instead. */
    public static void fellBack() {
        atlasesFellBack.incrementAndGet();
    }

    public static void reset() {
        atlasesPacked.set(0);
        spritesPacked.set(0);
        atlasesSkipped.set(0);
        atlasesFellBack.set(0);
        lastAtlas = "none";
    }

    public static String describe() {
        return "stitcher | packed=" + atlasesPacked.get()
            + " sprites=" + spritesPacked.get()
            + " last=" + lastAtlas
            + " skipped=" + atlasesSkipped.get()
            + " fellBack=" + atlasesFellBack.get();
    }
}
