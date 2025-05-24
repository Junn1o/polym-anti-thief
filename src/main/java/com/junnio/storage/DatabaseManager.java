package com.junnio.storage;

import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.junnio.Polymantithief.LOGGER;

public class DatabaseManager {
    private static String DB_URL;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void initDatabase() {
        try {
            // Get the game's run directory
            File gameDir = FabricLoader.getInstance().getGameDir().toFile();
            File dbFile = new File(gameDir, "shulker_logs.db");

            // Create sv directory if it doesn't exist
            File svDir = new File(gameDir, "sv");
            if (!svDir.exists()) {
                svDir.mkdirs();
            }

            // Set the database URL with absolute path
            DB_URL = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                // Enable foreign keys and WAL mode for better concurrent access
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA journal_mode=WAL");
                    stmt.execute("PRAGMA synchronous=NORMAL");
                }

                String sql = """
                    CREATE TABLE IF NOT EXISTS shulker_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        timestamp TEXT NOT NULL,
                        player_name TEXT NOT NULL,
                        action_name TEXT,
                        item_name TEXT,
                        shulker_name TEXT NOT NULL,
                        position TEXT NOT NULL,
                        dimension TEXT NOT NULL,
                        is_container BOOLEAN NOT NULL
                    )
                """;

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    LOGGER.info("Database initialized successfully at: {}", dbFile.getAbsolutePath());
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to initialize database: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void insertLog(String playerName, String actionName, String itemName,
                                 String shulkerName, String position, String dimension,
                                 boolean isContainer) {
        if (DB_URL == null) {
            LOGGER.error("Database URL is not initialized!");
            return;
        }

        // Queue the database operation to be executed asynchronously
        executor.submit(() -> {
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                String sql = """
                    INSERT INTO shulker_logs (timestamp, player_name, action_name, item_name, shulker_name, position, dimension, is_container)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, Instant.now().toString());
                    pstmt.setString(2, playerName);
                    pstmt.setString(3, actionName);
                    pstmt.setString(4, itemName);
                    pstmt.setString(5, shulkerName);
                    pstmt.setString(6, position);
                    pstmt.setString(7, dimension);
                    pstmt.setBoolean(8, isContainer);

                    int result = pstmt.executeUpdate();
                    LOGGER.info("Successfully inserted log entry for player: {} (rows affected: {})",
                            playerName, result);
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to insert log: {} - {}", e.getMessage(), e.getCause());
                e.printStackTrace();
            }
        });
    }

    public static void shutdown() {
        executor.shutdown();
    }
}