/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class BaseCommandBlock {
    private static final Component DEFAULT_NAME = Component.literal("@");
    private static final int NO_LAST_EXECUTION = -1;
    private long lastExecution = -1L;
    private boolean updateLastExecution = true;
    private int successCount;
    private boolean trackOutput = true;
    private @Nullable Component lastOutput;
    private String command = "";
    private @Nullable Component customName;

    public int getSuccessCount() {
        return this.successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public Component getLastOutput() {
        return this.lastOutput == null ? CommonComponents.EMPTY : this.lastOutput;
    }

    public void save(ValueOutput output) {
        output.putString("Command", this.command);
        output.putInt("SuccessCount", this.successCount);
        output.storeNullable("CustomName", ComponentSerialization.CODEC, this.customName);
        output.putBoolean("TrackOutput", this.trackOutput);
        if (this.trackOutput) {
            output.storeNullable("LastOutput", ComponentSerialization.CODEC, this.lastOutput);
        }
        output.putBoolean("UpdateLastExecution", this.updateLastExecution);
        if (this.updateLastExecution && this.lastExecution != -1L) {
            output.putLong("LastExecution", this.lastExecution);
        }
    }

    public void load(ValueInput input) {
        this.command = input.getStringOr("Command", "");
        this.successCount = input.getIntOr("SuccessCount", 0);
        this.setCustomName(BlockEntity.parseCustomNameSafe(input, "CustomName"));
        this.trackOutput = input.getBooleanOr("TrackOutput", true);
        this.lastOutput = this.trackOutput ? BlockEntity.parseCustomNameSafe(input, "LastOutput") : null;
        this.updateLastExecution = input.getBooleanOr("UpdateLastExecution", true);
        this.lastExecution = this.updateLastExecution ? input.getLongOr("LastExecution", -1L) : -1L;
    }

    public void setCommand(String command) {
        this.command = command;
        this.successCount = 0;
    }

    public String getCommand() {
        return this.command;
    }

    public boolean performCommand(ServerLevel level) {
        if (level.getGameTime() == this.lastExecution) {
            return false;
        }
        if ("Searge".equalsIgnoreCase(this.command)) {
            this.lastOutput = Component.literal("#itzlipofutzli");
            this.successCount = 1;
            return true;
        }
        this.successCount = 0;
        if (level.isCommandBlockEnabled() && !StringUtil.isNullOrEmpty(this.command)) {
            try {
                this.lastOutput = null;
                try (CloseableCommandBlockSource commandSource = this.createSource(level);){
                    CommandSource effectiveCommandSource = Objects.requireNonNullElse(commandSource, CommandSource.NULL);
                    CommandSourceStack commandSourceStack = this.createCommandSourceStack(level, effectiveCommandSource).withCallback((success, result) -> {
                        if (success) {
                            ++this.successCount;
                        }
                    });
                    level.getServer().getCommands().performPrefixedCommand(commandSourceStack, this.command);
                }
            }
            catch (Throwable t) {
                CrashReport report = CrashReport.forThrowable(t, "Executing command block");
                CrashReportCategory category = report.addCategory("Command to be executed");
                category.setDetail("Command", this::getCommand);
                category.setDetail("Name", () -> this.getName().getString());
                throw new ReportedException(report);
            }
        }
        this.lastExecution = this.updateLastExecution ? level.getGameTime() : -1L;
        return true;
    }

    private @Nullable CloseableCommandBlockSource createSource(ServerLevel level) {
        return this.trackOutput ? new CloseableCommandBlockSource(this, level) : null;
    }

    public Component getName() {
        return this.customName != null ? this.customName : DEFAULT_NAME;
    }

    public @Nullable Component getCustomName() {
        return this.customName;
    }

    public void setCustomName(@Nullable Component name) {
        this.customName = name;
    }

    public abstract void onUpdated(ServerLevel var1);

    public void setLastOutput(@Nullable Component lastOutput) {
        this.lastOutput = lastOutput;
    }

    public void setTrackOutput(boolean trackOutput) {
        this.trackOutput = trackOutput;
    }

    public boolean isTrackOutput() {
        return this.trackOutput;
    }

    public abstract CommandSourceStack createCommandSourceStack(ServerLevel var1, CommandSource var2);

    public abstract boolean isValid();

    protected class CloseableCommandBlockSource
    implements CommandSource,
    AutoCloseable {
        private final ServerLevel level;
        private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
        private boolean closed;
        final /* synthetic */ BaseCommandBlock this$0;

        protected CloseableCommandBlockSource(BaseCommandBlock this$0, ServerLevel level) {
            BaseCommandBlock baseCommandBlock = this$0;
            Objects.requireNonNull(baseCommandBlock);
            this.this$0 = baseCommandBlock;
            this.level = level;
        }

        @Override
        public boolean acceptsSuccess() {
            return !this.closed && this.level.getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK) != false;
        }

        @Override
        public boolean acceptsFailure() {
            return !this.closed;
        }

        @Override
        public boolean shouldInformAdmins() {
            return !this.closed && this.level.getGameRules().get(GameRules.COMMAND_BLOCK_OUTPUT) != false;
        }

        @Override
        public void sendSystemMessage(Component message) {
            if (!this.closed) {
                this.this$0.lastOutput = Component.literal("[" + TIME_FORMAT.format(ZonedDateTime.now()) + "] ").append(message);
                this.this$0.onUpdated(this.level);
            }
        }

        @Override
        public void close() throws Exception {
            this.closed = true;
        }
    }
}

