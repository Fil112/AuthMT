package mt.auth.managers.storage;

import mt.auth.AuthMT;
import mt.auth.managers.DatabaseManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class FileStorage implements DatabaseManager {

    private final AuthMT plugin;
    private File file;
    private FileConfiguration config;

    public FileStorage(AuthMT plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        // Проверяем, существует ли папка плагина (plugins/AuthMT/), если нет — создаем
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        // Указываем файл, в котором будут храниться данные игроков
        file = new File(plugin.getDataFolder(), "players.yml");

        // Если файла нет, создаем его
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Критическая ошибка: Не удалось создать файл players.yml!", e);
            }
        }

        // Загружаем конфигурацию из файла
        config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public boolean isRegistered(UUID uuid) {
        // Проверяем, есть ли запись с таким UUID в файле
        return config.contains(uuid.toString());
    }

    @Override
    public void saveUser(UUID uuid, String passwordHash) {
        // Записываем пару "UUID: Хэш" в память
        config.set(uuid.toString(), passwordHash);

        // Сохраняем изменения в физический файл
        save();
    }

    @Override
    public String getPasswordHash(UUID uuid) {
        // Возвращаем сохраненный хэш (или null, если его нет)
        return config.getString(uuid.toString());
    }

    /**
     * Внутренний метод для сохранения данных из памяти на жесткий диск.
     */
    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при сохранении данных в players.yml!", e);
        }
    }
}