/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  org.apache.commons.lang3.exception.ExceptionUtils
 */
package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.ExhaustedAttemptsException;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.RetryOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class ReportGameListener
implements GameTestListener {
    private int attempts = 0;
    private int successes = 0;

    @Override
    public void testStructureLoaded(GameTestInfo testInfo) {
        ++this.attempts;
    }

    private void handleRetry(GameTestInfo testInfo, GameTestRunner runner, boolean passed) {
        RetryOptions retryOptions = testInfo.retryOptions();
        Object reportAs = String.format(Locale.ROOT, "[Run: %4d, Ok: %4d, Fail: %4d", this.attempts, this.successes, this.attempts - this.successes);
        if (!retryOptions.unlimitedTries()) {
            reportAs = (String)reportAs + String.format(Locale.ROOT, ", Left: %4d", retryOptions.numberOfTries() - this.attempts);
        }
        reportAs = (String)reportAs + "]";
        String namePart = String.valueOf(testInfo.id()) + " " + (passed ? "passed" : "failed") + "! " + testInfo.getRunTime() + "ms";
        String text = String.format(Locale.ROOT, "%-53s%s", reportAs, namePart);
        if (passed) {
            ReportGameListener.reportPassed(testInfo, text);
        } else {
            ReportGameListener.say(testInfo.getLevel(), ChatFormatting.RED, text);
        }
        if (retryOptions.hasTriesLeft(this.attempts, this.successes)) {
            runner.rerunTest(testInfo);
        }
    }

    @Override
    public void testPassed(GameTestInfo testInfo, GameTestRunner runner) {
        ++this.successes;
        if (testInfo.retryOptions().hasRetries()) {
            this.handleRetry(testInfo, runner, true);
            return;
        }
        if (!testInfo.isFlaky()) {
            ReportGameListener.reportPassed(testInfo, String.valueOf(testInfo.id()) + " passed! (" + testInfo.getRunTime() + "ms / " + testInfo.getTick() + "gameticks)");
            return;
        }
        if (this.successes >= testInfo.requiredSuccesses()) {
            ReportGameListener.reportPassed(testInfo, String.valueOf(testInfo) + " passed " + this.successes + " times of " + this.attempts + " attempts.");
        } else {
            ReportGameListener.say(testInfo.getLevel(), ChatFormatting.GREEN, "Flaky test " + String.valueOf(testInfo) + " succeeded, attempt: " + this.attempts + " successes: " + this.successes);
            runner.rerunTest(testInfo);
        }
    }

    @Override
    public void testFailed(GameTestInfo testInfo, GameTestRunner runner) {
        if (!testInfo.isFlaky()) {
            ReportGameListener.reportFailure(testInfo, testInfo.getError());
            if (testInfo.retryOptions().hasRetries()) {
                this.handleRetry(testInfo, runner, false);
            }
            return;
        }
        GameTestInstance testFunction = testInfo.getTest();
        String text = "Flaky test " + String.valueOf(testInfo) + " failed, attempt: " + this.attempts + "/" + testFunction.maxAttempts();
        if (testFunction.requiredSuccesses() > 1) {
            text = text + ", successes: " + this.successes + " (" + testFunction.requiredSuccesses() + " required)";
        }
        ReportGameListener.say(testInfo.getLevel(), ChatFormatting.YELLOW, text);
        if (testInfo.maxAttempts() - this.attempts + this.successes >= testInfo.requiredSuccesses()) {
            runner.rerunTest(testInfo);
        } else {
            ReportGameListener.reportFailure(testInfo, new ExhaustedAttemptsException(this.attempts, this.successes, testInfo));
        }
    }

    @Override
    public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
        copy.addListener(this);
    }

    public static void reportPassed(GameTestInfo testInfo, String text) {
        ReportGameListener.getTestInstanceBlockEntity(testInfo).ifPresent(blockEntity -> blockEntity.setSuccess());
        ReportGameListener.visualizePassedTest(testInfo, text);
    }

    private static void visualizePassedTest(GameTestInfo testInfo, String text) {
        ReportGameListener.say(testInfo.getLevel(), ChatFormatting.GREEN, text);
        GlobalTestReporter.onTestSuccess(testInfo);
    }

    protected static void reportFailure(GameTestInfo testInfo, Throwable error) {
        Component description;
        if (error instanceof GameTestAssertException) {
            GameTestAssertException testException = (GameTestAssertException)error;
            description = testException.getDescription();
        } else {
            description = Component.literal(Util.describeError(error));
        }
        ReportGameListener.getTestInstanceBlockEntity(testInfo).ifPresent(blockEntity -> blockEntity.setErrorMessage(description));
        ReportGameListener.visualizeFailedTest(testInfo, error);
    }

    protected static void visualizeFailedTest(GameTestInfo testInfo, Throwable error) {
        String errorMessage = error.getMessage() + (String)(error.getCause() == null ? "" : " cause: " + Util.describeError(error.getCause()));
        String failureMessage = (testInfo.isRequired() ? "" : "(optional) ") + String.valueOf(testInfo.id()) + " failed! " + errorMessage;
        ReportGameListener.say(testInfo.getLevel(), testInfo.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, failureMessage);
        Throwable rootCause = (Throwable)MoreObjects.firstNonNull((Object)ExceptionUtils.getRootCause((Throwable)error), (Object)error);
        if (rootCause instanceof GameTestAssertPosException) {
            GameTestAssertPosException assertError = (GameTestAssertPosException)rootCause;
            testInfo.getTestInstanceBlockEntity().markError(assertError.getAbsolutePos(), assertError.getMessageToShowAtBlock());
        }
        GlobalTestReporter.onTestFailed(testInfo);
    }

    private static Optional<TestInstanceBlockEntity> getTestInstanceBlockEntity(GameTestInfo testInfo) {
        ServerLevel level = testInfo.getLevel();
        Optional<BlockPos> testPos = Optional.ofNullable(testInfo.getTestBlockPos());
        Optional<TestInstanceBlockEntity> test = testPos.flatMap(pos -> level.getBlockEntity((BlockPos)pos, BlockEntityTypes.TEST_INSTANCE_BLOCK));
        return test;
    }

    protected static void say(ServerLevel level, ChatFormatting format, String text) {
        level.getPlayers(player -> true).forEach(player -> player.sendSystemMessage(Component.literal(text).withStyle(format)));
    }
}

