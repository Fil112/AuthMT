package mt.auth;

import mt.auth.commands.AuthCommand;
import mt.auth.listeners.PlayerListener;
import mt.auth.managers.DatabaseManager;
import mt.auth.managers.SessionManager;
import mt.auth.managers.storage.FileStorage;
import mt.auth.managers.storage.SQLiteStorage;
import mt.auth.utils.ConfigUtil;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Главный класс плагина AuthMT.
 */
public class AuthMT extends JavaPlugin {

    private DatabaseManager databaseManager;
    private SessionManager sessionManager;
    private ConfigUtil configUtil;

    @Override
    public void onEnable() {
        // 1. Инициализация конфига
        configUtil = new ConfigUtil(this);
        configUtil.setup();

        // 2. Выбор и запуск базы данных на основе настроек
        String storageType = configUtil.getString("storage-type", "sqlite");
        if (storageType.equalsIgnoreCase("sqlite")) {
            databaseManager = new SQLiteStorage(this);
        } else {
            databaseManager = new FileStorage(this);
        }
        databaseManager.init();

        // 3. Инициализация менеджера сессий (для оперативной памяти)
        sessionManager = new SessionManager(this);

        // 4. Регистрация команд /login и /register
        AuthCommand authCommand = new AuthCommand(this);
        // Важно: эти команды должны быть прописаны в plugin.yml!
        if (getCommand("login") != null) getCommand("login").setExecutor(authCommand);
        if (getCommand("register") != null) getCommand("register").setExecutor(authCommand);

        // 5. Регистрация слушателя событий (перемещение, чат, вход)
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("=====================================");
        getLogger().info(" AuthMT успешно включен! ");
        getLogger().info(" Выбранная база данных: " + storageType.toUpperCase());
        getLogger().info("=====================================");
    }

    @Override
    public void onDisable() {
        // При выключении сервера обязательно закрываем SQLite (если он выбран),
        // чтобы не повредить файл database.db
        if (databaseManager instanceof SQLiteStorage) {
            ((SQLiteStorage) databaseManager).closeConnection();
        }
        getLogger().info("Плагин AuthMT выключен.");
    }

    // Геттеры для доступа к менеджерам из других классов
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }
}