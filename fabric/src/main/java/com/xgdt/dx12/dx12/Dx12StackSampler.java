package com.xgdt.dx12.dx12;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * P53：渲染线程轻量采样分析器（poor-man's profiler）。
 *
 * 为什么需要它：P50/P51/P52 的分段计时把时间记在 MC 的方法边界上，但 MC 的
 * 那些窗口内部会调用我们的 JNI 后端——例如 {@code stagedVertexBuffer.upload()}、
 * SectionRenderDispatcher 的 buffer slice 获取、帧图资源获取、clear/lightmap/
 * outline 等。因此"MC 固有 Java"与"我们后端开销"在分段计时里是混在一起的，
 * 无法归因。
 *
 * 本类用一个后台线程每 {@link #INTERVAL_MS} 毫秒抓一次渲染线程的调用栈，统计
 * 栈顶（跳过本分析器与埋点方法）落点。这样可以直接回答：渲染线程的时间究竟花在
 * {@code Dx12Native.dx12*}（我们的前端/JNI）还是 {@code net.minecraft.*} /
 * {@code com.mojang.blaze3d.*}（MC/Blaze3D 自身逻辑）里。
 *
 * 输出为"类名.方法名"（不再只有方法名，避免 writeBytes 这类 JDK/日志方法无法
 * 区分）；并对 top3 热点额外打印 8 层调用链，用于定位真正的调用者。
 *
 * 说明：{@code Thread.getStackTrace()} 会强制目标线程进入安全点，本身有可观开销
 * （约 1~2%），仅用于诊断，DX12_PROF=1 时才启用。
 */
public final class Dx12StackSampler {
    private static final long INTERVAL_MS = 2L;
    /** 每个 key 保存一份栈摘要，上限防止异常增长。 */
    private static final int STACK_CAP = 512;
    /** 栈摘要展示的层数。 */
    private static final int STACK_DEPTH = 8;

    private static final Map<String, LongAdder> COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, String> STACKS = new ConcurrentHashMap<>();
    private static final LongAdder TOTAL = new LongAdder();
    private static final LongAdder OURS = new LongAdder();

    private static volatile Thread target;
    private static Thread sampler;
    private static boolean started;

    private Dx12StackSampler() {}

    /** 在渲染线程上调用，登记采样目标。 */
    public static void attach() {
        target = Thread.currentThread();
    }

    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        sampler = new Thread(Dx12StackSampler::loop, "dx12-stack-sampler");
        sampler.setDaemon(true);
        sampler.setPriority(Thread.MAX_PRIORITY);
        sampler.start();
    }

    private static void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread t = target;
                if (t != null && t.isAlive()) {
                    StackTraceElement[] stack = t.getStackTrace();
                    int i = 0;
                    while (i < stack.length && skip(stack[i])) {
                        i++;
                    }
                    if (i < stack.length) {
                        StackTraceElement top = stack[i];
                        String key = simpleName(top.getClassName()) + "." + top.getMethodName();
                        COUNTS.computeIfAbsent(key, k -> new LongAdder()).increment();
                        TOTAL.increment();
                        if (top.getClassName().startsWith("com.xgdt.dx12.")) {
                            OURS.increment();
                        }
                        if (STACKS.size() < STACK_CAP) {
                            final int topIndex = i;
                            STACKS.computeIfAbsent(key, k -> excerpt(stack, topIndex));
                        }
                    }
                }
                Thread.sleep(INTERVAL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable ignored) {
                // 诊断线程绝不干扰游戏
            }
        }
    }

    /** 跳过本分析器自身与 P50~P52 的埋点方法，露出下面真正的业务方法。 */
    private static boolean skip(StackTraceElement e) {
        String cn = e.getClassName();
        if (cn.startsWith("com.xgdt.dx12.dx12.Dx12StackSampler")
            || cn.startsWith("com.xgdt.dx12.dx12.Dx12RenderProf")) {
            return true;
        }
        return e.getMethodName().startsWith("dx12_prof");
    }

    /** 输出本区间采样直方图、top3 调用链（各自一行）并清零。 */
    public static String report(int topN) {
        long total = TOTAL.sum();
        long ours = OURS.sum();
        TOTAL.reset();
        OURS.reset();

        List<Map.Entry<String, LongAdder>> entries = new ArrayList<>(COUNTS.size());
        for (Map.Entry<String, LongAdder> e : COUNTS.entrySet()) {
            long v = e.getValue().sum();
            if (v > 0L) {
                entries.add(e);
            }
        }
        COUNTS.clear();
        if (entries.isEmpty()) {
            return "[dx12-java] P53 sample: no samples yet";
        }
        entries.sort(Comparator.comparingLong((Map.Entry<String, LongAdder> e)
            -> e.getValue().sum()).reversed());
        StringBuilder sb = new StringBuilder(256);
        sb.append("[dx12-java] P53 sample n=").append(total)
          .append(" ours=").append(String.format(java.util.Locale.ROOT, "%.0f%%",
              total == 0L ? 0.0 : 100.0 * ours / total))
          .append(" mc=").append(String.format(java.util.Locale.ROOT, "%.0f%%",
              total == 0L ? 0.0 : 100.0 * (total - ours) / total))
          .append(" |");
        int n = Math.min(topN, entries.size());
        for (int i = 0; i < n; i++) {
            Map.Entry<String, LongAdder> e = entries.get(i);
            sb.append(' ').append(e.getKey()).append('=').append(e.getValue().sum());
        }
        // top3 调用链：热点 <- 调用者 <- 再上层 ...（定位真实归属）
        int nt = Math.min(3, entries.size());
        for (int i = 0; i < nt; i++) {
            String key = entries.get(i).getKey();
            String ex = STACKS.get(key);
            if (ex != null) {
                sb.append("\n[dx12-java] P53 trace ").append(key).append(" <= ").append(ex);
            }
        }
        STACKS.clear();
        return sb.toString();
    }

    /** 从采样栈的第 from 层起，拼接 STACK_DEPTH 层 "类.方法" 摘要。 */
    private static String excerpt(StackTraceElement[] stack, int from) {
        StringBuilder sb = new StringBuilder(192);
        int end = Math.min(stack.length, from + STACK_DEPTH);
        for (int i = from; i < end; i++) {
            if (i > from) {
                sb.append(" <= ");
            }
            sb.append(simpleName(stack[i].getClassName())).append('.')
              .append(stack[i].getMethodName());
        }
        return sb.toString();
    }

    private static String simpleName(String fq) {
        int dot = fq.lastIndexOf('.');
        return dot < 0 ? fq : fq.substring(dot + 1);
    }
}
