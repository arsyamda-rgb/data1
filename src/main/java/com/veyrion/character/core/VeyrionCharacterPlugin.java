package com.veyrion.character.core;

import com.veyrion.character.character.CharacterRepository;
import com.veyrion.character.command.CharacterCommand;
import com.veyrion.character.config.ConfigManager;
import com.veyrion.character.config.Messages;
import com.veyrion.character.database.DatabaseException;
import com.veyrion.character.database.DatabaseManager;
import com.veyrion.character.listener.PlayerJoinListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class VeyrionCharacterPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private Messages messages;
    private DatabaseManager databaseManager;
    private CharacterRepository characterRepository;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        messages = new Messages(this);
        messages.load(configManager.settings().language());

        if (!startDatabase()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, characterRepository), this);

        CharacterCommand command = new CharacterCommand(this, characterRepository);
        var pluginCommand = Objects.requireNonNull(getCommand("veyrioncharacter"),
                "Command 'veyrioncharacter' is missing from plugin.yml");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        getLogger().info("VeyrionCharacter " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private boolean startDatabase() {
        var settings = configManager.settings().database();
        try {
            Path dataFolder = getDataFolder().toPath();
            Files.createDirectories(dataFolder);
            databaseManager = new DatabaseManager(getLogger(), dataFolder.resolve(settings.file()),
                    settings.busyTimeoutMs());
            databaseManager.initialize();
            characterRepository = new CharacterRepository(databaseManager);
            return true;
        } catch (IOException | DatabaseException e) {
            getLogger().log(Level.SEVERE, "Could not start the database. Disabling VeyrionCharacter.", e);
            return false;
        }
    }

    /** Re-reads config.yml and language files. Never touches stored character data. */
    public void reloadSettings() {
        configManager.load();
        messages.load(configManager.settings().language());
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public Messages messages() {
        return messages;
    }

    public void debug(String message) {
        if (configManager.settings().debug()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
