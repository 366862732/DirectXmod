package com.xgdt.dx12.lod;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link LodSectionStore} 的离线测试：往返、覆盖、签名失效、重开、压实、坏尾截断。 */
class LodSectionStoreTest {

    private static byte[] payload(int seed, int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (seed * 31 + i * 7);
        }
        return data;
    }

    @Test
    void writeThenRead(@TempDir Path dir) throws IOException {
        try (LodSectionStore store = LodSectionStore.open(dir)) {
            byte[] data = payload(1, 4096);
            store.write(42L, data, 100, 200);
            assertArrayEquals(data, store.read(42L, 100, 200));
            assertNull(store.read(43L, 100, 200), "未知 key 应返回 null");
        }
    }

    @Test
    void overwriteReplacesValue(@TempDir Path dir) throws IOException {
        try (LodSectionStore store = LodSectionStore.open(dir)) {
            store.write(7L, payload(1, 1024), 1, 2);
            byte[] second = payload(2, 1024);
            store.write(7L, second, 1, 3);
            assertArrayEquals(second, store.read(7L, 1, 3));
            assertNull(store.read(7L, 1, 2), "旧签名应失效");
            assertEquals(1, store.entryCount());
        }
    }

    @Test
    void staleRegionSignatureMisses(@TempDir Path dir) throws IOException {
        try (LodSectionStore store = LodSectionStore.open(dir)) {
            byte[] data = payload(3, 512);
            store.write(9L, data, 111, 222);
            assertNotNull(store.read(9L, 111, 222));
            assertNull(store.read(9L, 112, 222), "region size 变了应 miss");
            assertNull(store.read(9L, 111, 223), "region mtime 变了应 miss");
        }
    }

    @Test
    void reopenKeepsEntries(@TempDir Path dir) throws IOException {
        byte[] data = payload(4, 2048);
        try (LodSectionStore store = LodSectionStore.open(dir)) {
            store.write(123L, data, 10, 20);
            store.flush();
        }
        try (LodSectionStore store = LodSectionStore.open(dir)) {
            assertEquals(1, store.entryCount());
            assertArrayEquals(data, store.read(123L, 10, 20));
        }
    }

    @Test
    void compactionKeepsLiveEntriesAndShrinks(@TempDir Path dir) throws IOException {
        try (LodSectionStore store = LodSectionStore.open(dir)) {
            int keys = 200;
            for (int i = 0; i < keys; i++) {
                store.write(i, payload(i, 4096), 1, 1);
            }
            for (int i = 0; i < keys; i++) {
                store.write(i, payload(i + 1000, 4096), 1, 2);
            }
            assertEquals(keys, store.entryCount());
            long before = store.sizeBytes();
            store.compactAll();
            long after = store.sizeBytes();
            assertTrue(after < before, "压实后磁盘占用应下降: " + before + " -> " + after);
            for (int i = 0; i < keys; i++) {
                assertArrayEquals(payload(i + 1000, 4096), store.read(i, 1, 2), "key " + i);
            }
        }
    }

    @Test
    void corruptTailIsTruncatedOnReopen(@TempDir Path dir) throws IOException {
        byte[] data = payload(5, 1024);
        try (LodSectionStore store = LodSectionStore.open(dir)) {
            store.write(55L, data, 1, 1);
        }
        // 模拟崩溃留下的半截记录：往每个分片末尾追加垃圾
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.toList()) {
                if (file.getFileName().toString().endsWith(".bin")) {
                    Files.write(file, new byte[97], StandardOpenOption.APPEND);
                }
            }
        }
        try (LodSectionStore store = LodSectionStore.open(dir)) {
            assertArrayEquals(data, store.read(55L, 1, 1), "坏尾不应影响已有记录");
            store.write(56L, payload(6, 512), 2, 2);
            assertArrayEquals(payload(6, 512), store.read(56L, 2, 2), "截断后仍可追加");
        }
    }

    @Test
    void emptyPayloadIsIgnored(@TempDir Path dir) throws IOException {
        try (LodSectionStore store = LodSectionStore.open(dir)) {
            store.write(1L, new byte[0], 0, 0);
            assertEquals(0, store.entryCount());
            assertNull(store.read(1L, 0, 0));
        }
    }

    @Test
    void shardingDistributesKeys(@TempDir Path dir) throws IOException {
        try (LodSectionStore store = LodSectionStore.open(dir)) {
            for (int i = 0; i < 64; i++) {
                store.write(i, payload(i, 128), 0, 0);
            }
            assertEquals(64, store.entryCount());
            long nonEmpty = 0;
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.toList()) {
                    if (file.getFileName().toString().endsWith(".bin") && Files.size(file) > 0) {
                        nonEmpty++;
                    }
                }
            }
            assertTrue(nonEmpty > 1, "64 个 key 不应全挤在一个分片里");
        }
    }
}
