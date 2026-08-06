/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Collection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.ArrayListDeque;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class CommandHistory {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_PERSISTED_COMMAND_HISTORY = 50;
    private static final String PERSISTED_COMMANDS_FILE_NAME = "command_history.txt";
    private final Path commandsPath;
    private final ArrayListDeque<String> lastCommands = new ArrayListDeque(50);

    public CommandHistory(Path gameFolder) {
        this.commandsPath = gameFolder.resolve(PERSISTED_COMMANDS_FILE_NAME);
        if (Files.exists(this.commandsPath, new LinkOption[0])) {
            try (BufferedReader reader = Files.newBufferedReader(this.commandsPath, StandardCharsets.UTF_8);){
                this.lastCommands.addAll(reader.lines().toList());
            }
            catch (Exception exception) {
                LOGGER.error("Failed to read {}, command history will be missing", (Object)PERSISTED_COMMANDS_FILE_NAME, (Object)exception);
            }
        }
    }

    public void addCommand(String command) {
        if (!command.equals(this.lastCommands.peekLast())) {
            if (this.lastCommands.size() >= 50) {
                this.lastCommands.removeFirst();
            }
            this.lastCommands.addLast(command);
            this.save();
        }
    }

    private void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(this.commandsPath, StandardCharsets.UTF_8, new OpenOption[0]);){
            for (String command : this.lastCommands) {
                writer.write(command);
                writer.newLine();
            }
        }
        catch (IOException exception) {
            LOGGER.error("Failed to write {}, command history will be missing", (Object)PERSISTED_COMMANDS_FILE_NAME, (Object)exception);
        }
    }

    public Collection<String> history() {
        return this.lastCommands;
    }
}

