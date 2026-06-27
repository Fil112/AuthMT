package mt.auth;

import mt.auth.commands.AuthCommand;
import mt.auth.listeners.PlayerListener;
import mt.auth.managers.DatabaseManager;
import mt.auth.managers.SessionManager;
import mt.auth.managers.storage.FileStorage;
import mt.auth.managers.storage.SQLiteStorage;
import mt.auth.utils.ConfigUtil;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;

public class AuthMT extends JavaPlugin {

    private DatabaseManager databaseManager;
    private SessionManager sessionManager;
    private ConfigUtil configUtil;
    private AuthCommand authCommand; // Ссылка на наш обработчик

    @Override
    public void onEnable() {
        configUtil = new ConfigUtil(this);
        configUtil.setup();

        setupAuthWorld();

        String storageType = configUtil.getString("storage-type", "sqlite");
        if (storageType.equalsIgnoreCase("sqlite")) {
            databaseManager = new SQLiteStorage(this);
        } else {
            databaseManager = new FileStorage(this);
        }
        databaseManager.init();

        sessionManager = new SessionManager(this);

        // Инициализируем и регистрируем команды
        authCommand = new AuthCommand(this);
        if (getCommand("login") != null) getCommand("login").setExecutor(authCommand);
        if (getCommand("register") != null) getCommand("register").setExecutor(authCommand);
        if (getCommand("amt") != null) getCommand("amt").setExecutor(authCommand);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("=====================================");
        getLogger().info(" AuthMT успешно включен! ");
        getLogger().info(" Выбранная база данных: " + storageType.toUpperCase());
        getLogger().info("=====================================");
    }

    @Override
    public void onDisable() {
        if (databaseManager instanceof SQLiteStorage) {
            ((SQLiteStorage) databaseManager).closeConnection();
        }
        getLogger().info("Плагин AuthMT выключен.");
    }

    private void setupAuthWorld() {
        String worldName = configUtil.getString("auth-world", "auth");
        if (getServer().getWorld(worldName) == null) {
            getLogger().info("Мир авторизации не найден. Создаю/загружаю мир: " + worldName);
            WorldCreator wc = new WorldCreator(worldName);
            wc.type(WorldType.FLAT);
            wc.generateStructures(false);
            getServer().createWorld(wc);
            getLogger().info("Мир " + worldName + " успешно инициализирован!");
        }
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public SessionManager getSessionManager() { return sessionManager; }
    public ConfigUtil getConfigUtil() { return configUtil; }
    public AuthCommand getAuthCommand() { return authCommand; } // Геттер для команд
}