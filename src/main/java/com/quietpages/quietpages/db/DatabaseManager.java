package com.quietpages.quietpages.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the single SQLite connection for QuietPages.
 *
 * Database file location:
 *   Windows : %APPDATA%\QuietPages\quietpages.db
 *   macOS   : ~/Library/Application Support/QuietPages/quietpages.db
 *   Linux   : ~/.config/QuietPages/quietpages.db
 *
 * Call DatabaseManager.getInstance() anywhere to get the shared instance.
 * Call DatabaseManager.getInstance().getConnection() to run SQL.
 */
public class DatabaseManager {

    private static final String DB_FILE_NAME = "quietpages.db";
    private static DatabaseManager instance;

    private Connection connection;

    // ── Singleton ─────────────────────────────────────────────────────────────
    private DatabaseManager() {
        initConnection();
        createTables();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // ── Connection ────────────────────────────────────────────────────────────
    private void initConnection() {
        try {
            String dbPath = resolveDbPath();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            // Enable WAL mode for better concurrent performance
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL;");
                st.execute("PRAGMA foreign_keys=ON;");
            }
            System.out.println("[DB] Connected → " + dbPath);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to SQLite database", e);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initConnection();
            }
        } catch (SQLException e) {
            initConnection();
        }
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── Schema ────────────────────────────────────────────────────────────────
    private void createTables() {
        String createBooks = """
            CREATE TABLE IF NOT EXISTS books (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                title            TEXT    NOT NULL DEFAULT 'Unknown Title',
                author           TEXT    NOT NULL DEFAULT 'Unknown Author',
                file_path        TEXT    NOT NULL UNIQUE,
                file_type        TEXT    NOT NULL DEFAULT 'epub',
                publisher        TEXT    DEFAULT '',
                language         TEXT    DEFAULT 'en',
                genre            TEXT    DEFAULT '',
                series           TEXT    DEFAULT '',
                series_number    INTEGER DEFAULT 0,
                description      TEXT    DEFAULT '',
                word_count       INTEGER DEFAULT 0,
                line_count       INTEGER DEFAULT 0,
                reading_progress REAL    DEFAULT 0.0,
                reading_status   TEXT    DEFAULT 'NOT_STARTED',
                favourite        INTEGER DEFAULT 0,
                pinned_to_start  INTEGER DEFAULT 0,
                date_added       TEXT    NOT NULL DEFAULT (datetime('now')),
                last_read        TEXT,
                cover_image      BLOB
            );
            """;

        // Reading sessions table — used by Statistics tab teammate
        String createSessions = """
            CREATE TABLE IF NOT EXISTS reading_sessions (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id     INTEGER NOT NULL REFERENCES books(id) ON DELETE CASCADE,
                started_at  TEXT NOT NULL,
                ended_at    TEXT,
                pages_read  INTEGER DEFAULT 0
            );
            """;

        // Annotations table — used by Collections tab (you)
        String createAnnotations = """
            CREATE TABLE IF NOT EXISTS annotations (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id     INTEGER NOT NULL REFERENCES books(id) ON DELETE CASCADE,
                type        TEXT    NOT NULL DEFAULT 'HIGHLIGHT',
                content     TEXT    NOT NULL DEFAULT '',
                note        TEXT    DEFAULT '',
                cfi_location TEXT,
                chapter     TEXT    DEFAULT '',
                created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
                color       TEXT    DEFAULT '#FFD700'
            );
            """;

        // Bookmarks table — used by Collections tab (you)
        String createBookmarks = """
            CREATE TABLE IF NOT EXISTS bookmarks (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id     INTEGER NOT NULL REFERENCES books(id) ON DELETE CASCADE,
                label       TEXT    DEFAULT '',
                cfi_location TEXT,
                chapter     TEXT    DEFAULT '',
                created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
            );
            """;

        // App settings table — used by Settings tab teammate
        String createSettings = """
            CREATE TABLE IF NOT EXISTS app_settings (
                key   TEXT PRIMARY KEY,
                value TEXT NOT NULL DEFAULT ''
            );
            """;

        try (Statement st = connection.createStatement()) {
            st.execute(createBooks);
            st.execute(createSessions);
            st.execute(createAnnotations);
            st.execute(createBookmarks);
            st.execute(createSettings);
            System.out.println("[DB] Schema ready.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database schema", e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String resolveDbPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String base;
        if (os.contains("win")) {
            base = System.getenv("APPDATA");
        } else if (os.contains("mac")) {
            base = System.getProperty("user.home") + "/Library/Application Support";
        } else {
            base = System.getProperty("user.home") + "/.config";
        }
        File dir = new File(base, "QuietPages");
        dir.mkdirs();
        return new File(dir, DB_FILE_NAME).getAbsolutePath();
    }
}