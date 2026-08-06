/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 */
package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestEvent;
import net.minecraft.gametest.framework.GameTestException;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.network.chat.Component;

public class GameTestSequence {
    private final GameTestInfo parent;
    private final List<GameTestEvent> events = Lists.newArrayList();
    private int lastTick;

    public GameTestSequence(GameTestInfo parent) {
        this.parent = parent;
        this.lastTick = parent.getTick();
    }

    public GameTestSequence thenWaitUntil(Runnable assertion) {
        this.events.add(GameTestEvent.create(assertion));
        return this;
    }

    public GameTestSequence thenWaitUntil(long expectedDelay, Runnable assertion) {
        this.events.add(GameTestEvent.create(expectedDelay, assertion));
        return this;
    }

    public GameTestSequence thenWaitAtLeast(long minimumDelay, Runnable assertion) {
        this.events.add(GameTestEvent.createWithMinimumDelay(minimumDelay, assertion));
        return this;
    }

    public GameTestSequence thenIdle(int delta) {
        return this.thenExecuteAfter(delta, () -> {});
    }

    public GameTestSequence thenExecute(Runnable assertion) {
        this.events.add(GameTestEvent.create(() -> this.executeWithoutFail(assertion)));
        return this;
    }

    public GameTestSequence thenExecuteAfter(int delta, Runnable after) {
        this.events.add(GameTestEvent.create(() -> {
            if (this.parent.getTick() < this.lastTick + delta) {
                throw new GameTestAssertException(Component.translatable("test.error.sequence.not_completed"), this.parent.getTick());
            }
            this.executeWithoutFail(after);
        }));
        return this;
    }

    public GameTestSequence thenExecuteFor(int delta, Runnable check) {
        this.events.add(GameTestEvent.create(() -> {
            if (this.parent.getTick() < this.lastTick + delta) {
                this.executeWithoutFail(check);
                throw new GameTestAssertException(Component.translatable("test.error.sequence.not_completed"), this.parent.getTick());
            }
        }));
        return this;
    }

    public void thenSucceed() {
        this.events.add(GameTestEvent.create(this.parent::succeed));
    }

    public void thenFail(Supplier<GameTestException> e) {
        this.events.add(GameTestEvent.create(() -> this.parent.fail((GameTestException)e.get())));
    }

    public Condition thenTrigger() {
        Condition result = new Condition(this);
        this.events.add(GameTestEvent.create(() -> result.trigger(this.parent.getTick())));
        return result;
    }

    public void tickAndContinue(int tick) {
        try {
            this.tick(tick);
        }
        catch (GameTestAssertException gameTestAssertException) {
            // empty catch block
        }
    }

    public void tickAndFailIfNotComplete(int tick) {
        try {
            this.tick(tick);
        }
        catch (GameTestAssertException e) {
            this.parent.fail(e);
        }
    }

    private void executeWithoutFail(Runnable assertion) {
        try {
            assertion.run();
        }
        catch (GameTestAssertException e) {
            this.parent.fail(e);
        }
    }

    private void tick(int tick) {
        Iterator<GameTestEvent> iterator = this.events.iterator();
        while (iterator.hasNext()) {
            GameTestEvent event = iterator.next();
            event.assertion.run();
            iterator.remove();
            int delay = tick - this.lastTick;
            int prevTick = this.lastTick;
            this.lastTick = tick;
            if (event.minimumDelay != null && event.minimumDelay > (long)delay) {
                this.parent.fail(new GameTestAssertException(Component.translatable("test.error.sequence.minimum_tick", (long)prevTick + event.minimumDelay), tick));
                break;
            }
            if (event.expectedDelay == null || event.expectedDelay == (long)delay) continue;
            this.parent.fail(new GameTestAssertException(Component.translatable("test.error.sequence.invalid_tick", (long)prevTick + event.expectedDelay), tick));
            break;
        }
    }

    public class Condition {
        private static final int NOT_TRIGGERED = -1;
        private int triggerTime;
        final /* synthetic */ GameTestSequence this$0;

        public Condition(GameTestSequence this$0) {
            GameTestSequence gameTestSequence = this$0;
            Objects.requireNonNull(gameTestSequence);
            this.this$0 = gameTestSequence;
            this.triggerTime = -1;
        }

        public void trigger(int time) {
            if (this.triggerTime != -1) {
                throw new IllegalStateException("Condition already triggered at " + this.triggerTime);
            }
            this.triggerTime = time;
        }

        public void assertTriggeredThisTick() {
            int tick = this.this$0.parent.getTick();
            if (this.triggerTime != tick) {
                if (this.triggerTime == -1) {
                    throw new GameTestAssertException(Component.translatable("test.error.sequence.condition_not_triggered"), tick);
                }
                throw new GameTestAssertException(Component.translatable("test.error.sequence.condition_already_triggered", this.triggerTime), tick);
            }
        }
    }
}

