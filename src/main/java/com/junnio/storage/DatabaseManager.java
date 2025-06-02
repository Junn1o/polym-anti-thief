package com.junnio.storage;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss-dd/MM/yyyy");

    private static class LogEntry {
        final ZonedDateTime timestamp;
        final String playerName;
        final String actionName;
        final String itemName;
        final String shulkerName;
        final String position;
        final String dimension;
        final boolean isContainer;

        LogEntry(String playerName, String actionName, String itemName,
                 String shulkerName, String position, String dimension, boolean isContainer) {
            this.timestamp = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
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

                    // Optional performance tweaks
                    stmt.execute("PRAGMA temp_store=MEMORY");
                    stmt.execute("PRAGMA cache_size=-64000");

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
                    1,
                    1,
                    TimeUnit.SECONDS
            );

        } catch (SQLException e) {
            LOGGER.error("Failed to initialize database: {}", e.getMessage(), e);
        }
    }

    private static void processBatch() {
        if (isShuttingDown && logQueue.isEmpty()) {
            return;
        }

        List<LogEntry> batch = new ArrayList<>();
        logQueue.drainTo(batch, BATCH_SIZE);

        if (batch.isEmpty()) return;

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
                INSERT INTO shulker_logs (timestamp, player_name, action_name, item_name, shulker_name, position, dimension, is_container)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (LogEntry entry : batch) {
                    pstmt.setString(1, FORMATTER.format(entry.timestamp));
                    pstmt.setString(2, entry.playerName);
                    pstmt.setString(3, entry.actionName);
                    pstmt.setString(4, entry.itemName);
                    pstmt.setString(5, entry.shulkerName);
                    pstmt.setString(6, entry.position);
                    pstmt.setString(7, entry.dimension);
                    pstmt.setBoolean(8, entry.isContainer);
                    pstmt.addBatch();
                }

                pstmt.executeBatch();
                conn.commit();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to process batch: {}", e.getMessage(), e);
        }
    }

    public static void insertLog(String playerName, String actionName, String itemName,
                                 String shulkerName, String position, String dimension,
                                 boolean isContainer) {
        if (DB_URL == null) {
            LOGGER.error("Database URL is not initialized!");
            return;
        }

        try {
            LogEntry entry = new LogEntry(playerName, actionName, itemName,
                    shulkerName, position, dimension, isContainer);

            if (!logQueue.offer(entry)) {
                LOGGER.warn("Log queue is full! Dropping log entry for player: {}", playerName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create log entry: {}", e.getMessage(), e);
        }
    }

    public static void shutdown() {
        isShuttingDown = true;
        LOGGER.info("Shutting down DatabaseManager. Remaining queue size: {}", logQueue.size());

        processBatch();
        scheduledExecutor.shutdown();

        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Forcing executor shutdown.");
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Shutdown interrupted: {}", e.getMessage(), e);
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
