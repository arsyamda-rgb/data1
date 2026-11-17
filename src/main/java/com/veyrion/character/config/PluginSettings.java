package com.veyrion.character.config;

import com.veyrion.character.character.CharacterData;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

/** Validated, immutable view of {@code config.yml}. Invalid values fall back to defaults with a warning. */
public record PluginSettings(
        String language,
        Database database,
        boolean debug,
        DefaultCharacter defaultCharacter) {

    public record Database(String file, int busyTimeoutMs) {}

    public record DefaultCharacter(long adventureExp, String adventureRank, boolean hasCompletedGacha) {

        /** Builds a brand-new character using these defaults. The race starts unassigned. */
        public CharacterData create(UUID uuid, String playerName) {
            return new CharacterData(uuid, playerName, null, adventureExp, adventureRank,
                    Instant.now(), hasCompletedGacha);
        }
    }

    static PluginSettings from(FileConfiguration config, Logger logger) {
        String language = config.getString("language", "en").trim().toLowerCase(Locale.ROOT);
        if (language.isEmpty()) {
            logger.warning("config.yml: 'language' is empty; using 'en'.");
            language = "en";
        }

        String file = config.getString("database.file", "characters.db").trim();
        if (!isPlainFileName(file)) {
            logger.warning("config.yml: 'database.file' must be a plain file name; using 'characters.db'.");
            file = "characters.db";
        }

        int busyTimeout = config.getInt("database.busy-timeout-ms", 5000);
        if (busyTimeout < 0) {
            logger.warning("config.yml: 'database.busy-timeout-ms' must not be negative; using 5000.");
            busyTimeout = 5000;
        }

        long exp = config.getLong("default-character.adventure-exp", 0);
        if (exp < 0) {
            logger.warning("config.yml: 'default-character.adventure-exp' must not be negative; using 0.");
            exp = 0;
        }

        String rank = config.getString("default-character.adventure-rank", "Novice").trim();
        if (rank.isEmpty()) {
            logger.warning("config.yml: 'default-character.adventure-rank' is empty; using 'Novice'.");
            rank = "Novice";
        }

        return new PluginSettings(
                language,
                new Database(file, busyTimeout),
                config.getBoolean("debug", false),
                new DefaultCharacter(exp, rank, config.getBoolean("default-character.has-completed-gacha", false)));
    }

    private static boolean isPlainFileName(String name) {
        if (name.isEmpty()) {
            return false;
        }
        try {
            Path path = Path.of(name);
            return path.getNameCount() == 1 && !path.isAbsolute() && !name.equals("..");
        } catch (InvalidPathException e) {
            return false;
        }
    }
}
