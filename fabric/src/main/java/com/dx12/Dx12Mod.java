package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GL4DX12 - Fabric client entry point.
 *
 * The wgpu-based overlay renderer (Plan C) has been archived and removed.
 * The current architecture hooks into the vanilla GPU backend loop instead:
 *
 *   PreferredGraphicsApiMixin  →  getBackendsToTry() includes Dx12Backend
 *   Dx12Backend (GpuBackend)   →  vanilla calls createDevice(window, ...)
 *   Dx12Native (JNI)           →  talks to the dx12-mc Rust D3D12 layer
 *
 * All renderer work happens inside the vanilla backend lifecycle, so this
 * class only needs to exist as the Fabric entry point.
 */
public class Dx12Mod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    @Override
    public void onInitializeClient() {
        LOGGER.info("GL4DX12 initializing");
        LOGGER.info("DX12 backend is registered via PreferredGraphicsApiMixin; "
            + "creation is driven by the vanilla backend-selection loop");
        startExitWatchdog();
    }

    /**
     * P12b：全局退出看门狗。Minecraft 关闭时若渲染线程（= main 线程）已结束
     * 但进程仍未退出（例如某个非 daemon Worker 线程卡在阻塞调用/我们后端的
     * fence 等待中），JVM 会一直挂着 → PCL 启动器作为父进程无限等待 → "启动器
     * 彻底卡死"。此 daemon 线程检测到渲染线程消失后宽限 20 秒，若 JVM 仍存活
     * 则把全部线程栈 dump 到 %TEMP%\dx12-java.log 并强制 System.exit(1)——
     * 保底让启动器恢复正常，同时留下卡死证据。
     */
    private static void startExitWatchdog() {
        Thread watchdog = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(5000);
                    boolean renderAlive = Thread.getAllStackTraces().keySet().stream()
                        .anyMatch(t -> "Render thread".equals(t.getName()));
                    if (renderAlive) {
                        continue;
                    }
                    // 渲染线程已消失：正常关闭时 JVM 会很快退出；再宽限 15 秒。
                    Thread.sleep(15000);
                    boolean stillNoRender = Thread.getAllStackTraces().keySet().stream()
                        .noneMatch(t -> "Render thread".equals(t.getName()));
                    if (stillNoRender) {
                        StringBuilder sb = new StringBuilder("[dx12-java] WATCHDOG: Render thread gone but JVM alive >20s, forcing exit\n");
                        for (java.util.Map.Entry<Thread, StackTraceElement[]> en
                            : Thread.getAllStackTraces().entrySet()) {
                            Thread t = en.getKey();
                            sb.append("Thread ").append(t.getName())
                                .append(" [").append(t.getState()).append("] daemon=")
                                .append(t.isDaemon()).append('\n');
                            for (StackTraceElement el : en.getValue()) {
                                sb.append("    at ").append(el).append('\n');
                            }
                        }
                        String msg = sb.toString();
                        System.err.println(msg);
                        System.err.flush();
                        try {
                            String path = System.getProperty("java.io.tmpdir");
                            if (path == null) path = ".";
                            java.io.FileWriter fw = new java.io.FileWriter(path + "\\dx12-java.log", true);
                            fw.write(msg + "\n");
                            fw.close();
                        } catch (Exception ignored) {
                        }
                        System.err.println("[dx12-java] WATCHDOG: System.exit(1)");
                        System.exit(1);
                    }
                }
            } catch (InterruptedException e) {
                // daemon 线程，忽略
            }
        }, "dx12-exit-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }
}
