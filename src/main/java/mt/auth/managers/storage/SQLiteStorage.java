package mt.auth.managers.storage;

import mt.auth.AuthMT;
import mt.auth.managers.DatabaseManager;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteStorage implements DatabaseManager {

    private final AuthMT plugin;
    private Connection connection;
    private final String dbName = "database.db";

    public SQLiteStorage(AuthMT plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        // Убеждаемся, что папка плагина существует
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        File dataFolder = new File(plugin.getDataFolder(), dbName);
        // Формируем URL для подключения через JDBC
        String url = "jdbc:sqlite:" + dataFolder;

        try {
            // Подгружаем драйвер явно (полезно для старых версий Java/Spigot)
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(url);
            createTable();
            plugin.getLogger().info("Успешно подключено к базе данных SQLite!");
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Критическая ошибка при подключении к базе данных SQLite!", e);
        }
    }

    /**
     * Создает таблицу users, если она еще не существует.
     */
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "password_hash VARCHAR(255) NOT NULL" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при создании таблицы пользователей!", e);
        }
    }

    @Override
    public boolean isRegistered(UUID uuid) {
        String sql = "SELECT uuid FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                // Если rs.next() возвращает true, значит запись найдена
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при проверке регистрации игрока!", e);
            return false;
        }
    }

    @Override
    public void saveUser(UUID uuid, String passwordHash) {
        // Используем REPLACE INTO: если игрока нет — он создастся, если есть — обновится его пароль
        String sql = "REPLACE INTO users (uuid, password_hash) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, passwordHash);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при сохранении пользователя!", e);
        }
    }

    @Override
    public String getPasswordHash(UUID uuid) {
        String sql = "SELECT password_hash FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при получении пароля игрока!", e);
        }
        return null;
    }

    /**
     * Метод для корректного закрытия базы данных.
     * Обязательно вызывай его в методе onDisable() главного класса AuthMT!
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Соединение с базой данных SQLite закрыто.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при закрытии соединения с БД!", e);
        }
    }
}