package com.xgdt.dx11.perf.texture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class StbStitcherTest {
    private static final int MIP_LEVEL = 4;
    private static final int UNIT = 1 << MIP_LEVEL;
    private static final int MAX_TEXTURE_SIZE = 16384;
    private static final int SPRITE_COUNT = 200;

    private record TestEntry(Identifier name, int width, int height)
        implements Stitcher.Entry {
    }

    private record TestHolder(Stitcher.Entry entry, int width, int height)
        implements StitcherHolderView {
        @Override
        public Stitcher.Entry dx11$entry() {
            return this.entry;
        }

        @Override
        public int dx11$width() {
            return this.width;
        }

        @Override
        public int dx11$height() {
            return this.height;
        }
    }

    /**
     * Vanilla sorts holders largest-first before packing; the skyline heuristic
     * depends on that order, so the fixtures are sorted the same way.
     */
    private static List<TestHolder> holders() {
        List<TestHolder> holders = new ArrayList<>(SPRITE_COUNT);
        for (int i = 0; i < SPRITE_COUNT; i++) {
            // Sizes the block atlas actually contains: mip-aligned, padded, and
            // spread over several powers of two.
            int width = UNIT * (1 + (i * 7) % 16);
            int height = UNIT * (1 + (i * 5) % 16);
            TestEntry entry = new TestEntry(
                Identifier.fromNamespaceAndPath("gl4dx11", "sprite_" + i), width, height);
            holders.add(new TestHolder(entry, width, height));
        }
        holders.sort((a, b) -> Integer.compare(
            b.dx11$width() * b.dx11$height(), a.dx11$width() * a.dx11$height()));
        return holders;
    }

    @Test
    void packsEverySpriteIntoAPowerOfTwoAtlas() {
        assumeStbAvailable();
        List<TestHolder> holders = holders();

        StbStitcher.PackedAtlas atlas =
            StbStitcher.pack(holders, MAX_TEXTURE_SIZE, MAX_TEXTURE_SIZE, MIP_LEVEL);

        assertNotNull(atlas, "stb should pack a normal block-atlas sprite set");
        assertEquals(holders.size(), atlas.sprites().size());
        assertTrue(atlas.width() <= MAX_TEXTURE_SIZE);
        assertTrue(atlas.height() <= MAX_TEXTURE_SIZE);
        assertTrue(Integer.bitCount(atlas.width()) == 1, "atlas width is a power of two");
        assertTrue(Integer.bitCount(atlas.height()) == 1, "atlas height is a power of two");
        assertEquals(0, atlas.width() % UNIT, "atlas width keeps the mip quantum");
        assertEquals(0, atlas.height() % UNIT, "atlas height keeps the mip quantum");
    }

    @Test
    void keepsSpritesMipAlignedInsideTheAtlas() {
        assumeStbAvailable();
        List<TestHolder> holders = holders();

        StbStitcher.PackedAtlas atlas =
            StbStitcher.pack(holders, MAX_TEXTURE_SIZE, MAX_TEXTURE_SIZE, MIP_LEVEL);

        assertNotNull(atlas);
        Set<Identifier> seen = new HashSet<>();
        for (StbStitcher.PackedSprite sprite : atlas.sprites()) {
            TestHolder holder = findHolder(holders, sprite);
            assertEquals(0, sprite.x() % UNIT, "sprite x is mip-aligned");
            assertEquals(0, sprite.y() % UNIT, "sprite y is mip-aligned");
            assertTrue(sprite.x() + holder.dx11$width() <= atlas.width(), "sprite stays inside");
            assertTrue(sprite.y() + holder.dx11$height() <= atlas.height(), "sprite stays inside");
            assertTrue(seen.add(sprite.entry().name()), "every sprite is placed once");
        }
        assertEquals(holders.size(), seen.size());
    }

    @Test
    void noTwoSpritesOverlap() {
        assumeStbAvailable();
        List<TestHolder> holders = holders();

        StbStitcher.PackedAtlas atlas =
            StbStitcher.pack(holders, MAX_TEXTURE_SIZE, MAX_TEXTURE_SIZE, MIP_LEVEL);

        assertNotNull(atlas);
        List<StbStitcher.PackedSprite> sprites = atlas.sprites();
        for (int i = 0; i < sprites.size(); i++) {
            StbStitcher.PackedSprite first = sprites.get(i);
            TestHolder firstHolder = findHolder(holders, first);
            for (int j = i + 1; j < sprites.size(); j++) {
                StbStitcher.PackedSprite second = sprites.get(j);
                TestHolder secondHolder = findHolder(holders, second);
                boolean apart = first.x() + firstHolder.dx11$width() <= second.x()
                    || second.x() + secondHolder.dx11$width() <= first.x()
                    || first.y() + firstHolder.dx11$height() <= second.y()
                    || second.y() + secondHolder.dx11$height() <= first.y();
                assertTrue(apart, () -> "overlap between " + first.entry().name()
                    + " and " + second.entry().name());
            }
        }
    }

    @Test
    void smallAtlasesStayWithVanilla() {
        List<TestHolder> holders = holders().subList(0, StbStitcher.MIN_SPRITES - 1);
        assertFalse(StbStitcher.isWorthPacking(holders.size()));
        assertNull(StbStitcher.pack(holders, MAX_TEXTURE_SIZE, MAX_TEXTURE_SIZE, MIP_LEVEL));
    }

    @Test
    void misalignedHoldersFallBackToVanilla() {
        assumeStbAvailable();
        List<TestHolder> holders = new ArrayList<>(holders());
        TestHolder first = holders.remove(0);
        holders.add(new TestHolder(first.entry(), first.dx11$width() + 1, first.dx11$height()));

        assertNull(StbStitcher.pack(holders, MAX_TEXTURE_SIZE, MAX_TEXTURE_SIZE, MIP_LEVEL));
    }

    @Test
    void spritesThatDoNotFitFallBackToVanilla() {
        assumeStbAvailable();
        List<TestHolder> holders = holders();

        assertNull(StbStitcher.pack(holders, 64, 64, MIP_LEVEL));
    }

    private static TestHolder findHolder(
        List<TestHolder> holders, StbStitcher.PackedSprite sprite) {
        for (TestHolder holder : holders) {
            if (holder.dx11$entry() == sprite.entry()) {
                return holder;
            }
        }
        throw new AssertionError("packed sprite " + sprite.entry().name() + " has no holder");
    }

    /** stb natives are not on every test runtime, and the packer falls back when absent. */
    private static void assumeStbAvailable() {
        boolean available;
        try {
            Class.forName("org.lwjgl.stb.STBRectPack");
            available = true;
        } catch (Throwable t) {
            available = false;
        }
        assumeTrue(available, "stb natives unavailable on this test runtime");
    }
}
