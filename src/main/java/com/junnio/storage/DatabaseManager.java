package com.junnio.storage;

import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.junnio.Polymantithief.LOGGER;

public class DatabaseManager {
    private static String DB_URL;
    private static final int BATCH_SIZE = 100;
    private static final int QUEUE_CAPACITY = 1000;
    private static final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private static volatile boolean isShuttingDown = false;

    private static class LogEntry {
        final String timestamp;
        final String playerName;
        final String actionName;
        final String itemName;
        final String shulkerName;
        final String position;
        final String dimension;
        final boolean isContainer;

        LogEntry(String playerName, String actionName, String itemName,
                 String shulkerName, String position, String dimension, boolean isContainer) {
            this.timestamp = Instant.now().toString();
            this.playerName = playerName;
            this.actionName = actionName;
            this.itemName = itemName;
            this.shulkerName = shulkerName;
            this.position = position;
            this.dimension = dimension;
            this.isContainer = isContainer;
        }
    }

    public static void initDatabase() {
        try {
            File gameDir = FabricLoader.getInstance().getGameDir().toFile();
            File dbFile = new File(gameDir, "shulker_logs.db");

            DB_URL = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA journal_mode=WAL");
                    stmt.execute("PRAGMA synchronous=NORMAL");
                    stmt.execute("""
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
                    """);
                }
            }

            // Start the batch processing scheduler
            scheduledExecutor.scheduleWithFixedDelay(
                    DatabaseManager::processBatch,
                    1, // initial delay
                    1, // period
                    TimeUnit.SECONDS
            );

        } catch (SQLException e) {
            LOGGER.error("Failed to initialize database: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processBatch() {
        if (isShuttingDown && logQueue.isEmpty()) {
            return;
        }

        List<LogEntry> batch = new ArrayList<>();
        logQueue.drainTo(batch, BATCH_SIZE);

        if (batch.isEmpty()) {
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
                INSERT INTO shulker_logs (timestamp, player_name, action_name, item_name, shulker_name, position, dimension, is_container)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (LogEntry entry : batch) {
                    pstmt.setString(1, entry.timestamp);
                    pstmt.setString(2, entry.playerName);
                    pstmt.setString(3, entry.actionName);
                    pstmt.setString(4, entry.itemName);
                    pstmt.setString(5, entry.shulkerName);
                    pstmt.setString(6, entry.position);
                    pstmt.setString(7, entry.dimension);
                    pstmt.setBoolean(8, entry.isContainer);
                    pstmt.addBatch();
                }

                int[] results = pstmt.executeBatch();
                conn.commit();
                LOGGER.info("Batch processed: {} entries", results.length);
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to process batch: {}", e.getMessage());
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

        LogEntry entry = new LogEntry(playerName, actionName, itemName,
                shulkerName, position, dimension, isContainer);

        if (!logQueue.offer(entry)) {
            LOGGER.warn("Log queue is full! Dropping log entry for player: {}", playerName);
        }
    }

    public static void shutdown() {
        isShuttingDown = true;
        // Process remaining entries
        processBatch();
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}