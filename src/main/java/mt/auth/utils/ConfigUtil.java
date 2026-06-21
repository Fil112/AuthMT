package mt.auth.utils;

import mt.auth.AuthMT;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Утилита для удобной работы с конфигурацией плагина (config.yml).
 */
public class ConfigUtil {

    private final AuthMT plugin;
    private FileConfiguration config;

    public ConfigUtil(AuthMT plugin) {
        this.plugin = plugin;
    }

    /**
     * Инициализация: создает config.yml, если его нет, и загружает в память.
     */
    public void setup() {
        // Копирует дефолтный config.yml из ресурсов .jar файла в папку плагина
        plugin.saveDefaultConfig();
        // Обновляем ссылку на конфигурацию
        config = plugin.getConfig();
    }

    /**
     * Получает сообщение из конфига и автоматически применяет к нему цвета Bukkit.
     * * @param path Путь к строке в конфиге (например, "messages.login-success")
     * @return Раскрашенная строка
     */
    public String getMessage(String path) {
        String message = config.getString(path);

        // Если кто-то случайно удалил сообщение из конфига, возвращаем заглушку, чтобы не было ошибок (NPE)
        if (message == null) {
            return ChatColor.RED + "Сообщение не найдено: " + path;
        }

        // Переводим привычный символ '&' в цветовые коды Minecraft (§)
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Получает обычную строку из конфига (например, название мира).
     * * @param path Путь к настройке
     * @param def Значение по умолчанию, если настройка не найдена
     * @return Строка настройки
     */
    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    /**
     * Перезагружает конфиг (полезно для команды /auth reload в будущем).
     */
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
}