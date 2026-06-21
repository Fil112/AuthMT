package mt.auth.managers;

import java.util.UUID;

/**
 * Интерфейс для работы с базой данных.
 * Позволяет легко переключаться между FileStorage и SQLiteStorage.
 */
public interface DatabaseManager {

    /**
     * Инициализация базы данных (создание файлов/таблиц).
     */
    void init();

    /**
     * Проверяет, зарегистрирован ли игрок.
     * @param uuid UUID игрока
     * @return true, если игрок есть в базе
     */
    boolean isRegistered(UUID uuid);

    /**
     * Сохраняет нового пользователя или обновляет его пароль.
     * @param uuid UUID игрока
     * @param passwordHash Хэш пароля
     */
    void saveUser(UUID uuid, String passwordHash);

    /**
     * Получает хэш пароля игрока из базы.
     * @param uuid UUID игрока
     * @return Хэш пароля или null, если игрок не найден
     */
    String getPasswordHash(UUID uuid);
}