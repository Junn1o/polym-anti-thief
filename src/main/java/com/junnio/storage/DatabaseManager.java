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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static com.junnio.Polymantithief.LOGGER;

public class DatabaseManager {
    private static HikariDataSource dataSource;
    private static final int BATCH_SIZE = 100;
    private static final int QUEUE_CAPACITY = 10000;
    private static final int PROCESSOR_THREADS = 4;
    private static final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private static final ExecutorService processorPool = Executors.newFixedThreadPool(PROCESSOR_THREADS);
    private static volatile boolean isShuttingDown = false;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss-dd/MM/yyyy");

    private static class LogEntry {
        final ZonedDateTime timestamp;
        final String playerName;
        final String actionName;
        final String itemName;
        final String shulkerName;
        final String dimension;
        final Float x;
        final Float y;
        final Float z;
        final boolean isContainer;

        LogEntry(String playerName, String actionName, String itemName, String shulkerName, Float x, Float y, Float z, String dimension, boolean isContainer) {
            this.timestamp = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            this.playerName = playerName;
            this.actionName = actionName;
            this.itemName = itemName;
            this.shulkerName = shulkerName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.isContainer = isContainer;
        }
    }

    public static void initDatabase() {
        try {
            File gameDir = FabricLoader.getInstance().getGameDir().toFile();
            File dbFile = new File(gameDir, "shulker_logs.db");
            String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setMaximumPoolSize(PROCESSOR_THREADS + 1); // +1 for other operations
            config.setMinimumIdle(1);
            config.setConnectionTimeout(30000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");

            dataSource = new HikariDataSource(config);

            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {

                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute("PRAGMA temp_store=MEMORY");
                stmt.execute("PRAGMA cache_size=-64000");
                stmt.execute("PRAGMA busy_timeout=30000");

                stmt.execute("""
                            CREATE TABLE IF NOT EXISTS shulker_logs (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                timestamp TEXT NOT NULL,
                                player_name TEXT NOT NULL,
                                action_name TEXT,
                                item_name TEXT,
                                shulker_name TEXT NOT NULL,
                                x REAL NOT NULL,
                                y REAL NOT NULL,
                                z REAL NOT NULL,
                                dimension TEXT NOT NULL,
                                is_container BOOLEAN NOT NULL
                            )
                        """);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_player ON shulker_logs(player_name)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_shulker_name ON shulker_logs(shulker_name)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON shulker_logs(timestamp)");
            }

            // Start multiple processor threads
            for (int i = 0; i < PROCESSOR_THREADS; i++) {
                processorPool.submit(DatabaseManager::processQueueContinuously);
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to initialize database: {}", e.getMessage(), e);
        }
    }

    private static void processQueueContinuously() {
        while (!isShuttingDown || !logQueue.isEmpty()) {
            try {
                List<LogEntry> batch = new ArrayList<>(BATCH_SIZE);
                LogEntry entry = logQueue.poll(100, TimeUnit.MILLISECONDS);

                if (entry == null) continue;

                batch.add(entry);
                logQueue.drainTo(batch, BATCH_SIZE - 1);

                if (!batch.isEmpty()) {
                    processBatch(batch);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void processBatch(List<LogEntry> batch) {
        String sql = """
                    INSERT INTO shulker_logs (timestamp, player_name, action_name, item_name, 
                    shulker_name, x, y, z, dimension, is_container)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (LogEntry entry : batch) {
                pstmt.setString(1, FORMATTER.format(entry.timestamp));
                pstmt.setString(2, entry.playerName);
                pstmt.setString(3, entry.actionName);
                pstmt.setString(4, entry.itemName);
                pstmt.setString(5, entry.shulkerName);
                pstmt.setFloat(6, entry.x);
                pstmt.setFloat(7, entry.y);
                pstmt.setFloat(8, entry.z);
                pstmt.setString(9, entry.dimension);
                pstmt.setBoolean(10, entry.isContainer);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit();
            LOGGER.debug("Inserting log entry for player: {}", pstmt);
            LOGGER.debug("Processed batch of {} entries", batch.size());
        } catch (SQLException e) {
            LOGGER.error("Failed to process batch: {}", e.getMessage(), e);
        }
    }

    public static void insertLog(String playerName, String actionName, String itemName, String shulkerName, Float x, Float y, Float z, String dimension, boolean isContainer) {
        if (dataSource == null) {
            LOGGER.error("Database is not initialized!");
            return;
        }

        try {
            LogEntry entry = new LogEntry(playerName, actionName, itemName, shulkerName, x, y, z, dimension, isContainer);
            LOGGER.debug("Inserting log entry for player: {}", playerName);
            if (!logQueue.offer(entry, 100, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Log queue is full! Dropping log entry for player: {}", playerName);
            }
        } catch (InterruptedException e) {
            LOGGER.error("Failed to queue log entry: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public static void shutdown() {
        isShuttingDown = true;
        LOGGER.info("Shutting down DatabaseManager. Remaining queue size: {}", logQueue.size());

        processorPool.shutdown();
        try {
            if (!processorPool.awaitTermination(10, TimeUnit.SECONDS)) {
                processorPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            processorPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (dataSource != null) {
            dataSource.close();
        }
    }
}