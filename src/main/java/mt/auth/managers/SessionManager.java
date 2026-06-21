package mt.auth.managers;

import mt.auth.AuthMT;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер сессий.
 * Хранит информацию о том, кто сейчас авторизован и когда истекает их "пропуск".
 */
public class SessionManager {

    private final AuthMT plugin;
    // Используем ConcurrentHashMap для безопасной работы с сессиями из асинхронных потоков
    private final Map<UUID, Long> sessions;

    public SessionManager(AuthMT plugin) {
        this.plugin = plugin;
        this.sessions = new ConcurrentHashMap<>();
    }

    /**
     * Создает новую сессию (вызывается после успешного /login или /register)
     * @param uuid UUID игрока
     */
    public void createSession(UUID uuid) {
        sessions.put(uuid, System.currentTimeMillis());
    }

    /**
     * Обновляет время последней активности игрока (вызывается при выходе с сервера).
     * Отсчет таймаута начнется именно с этого момента.
     * @param uuid UUID игрока
     */
    public void saveSession(UUID uuid) {
        // Обновляем время только если игрок действительно был авторизован перед выходом
        if (sessions.containsKey(uuid)) {
            sessions.put(uuid, System.currentTimeMillis());
        }
    }

    /**
     * Проверяет, действительна ли сессия игрока.
     * @param uuid UUID игрока
     * @return true, если игрок авторизован и время сессии не истекло
     */
    public boolean hasValidSession(UUID uuid) {
        if (!sessions.containsKey(uuid)) {
            return false; // Сессии вообще нет
        }

        long lastActivityTime = sessions.get(uuid);
        long currentTime = System.currentTimeMillis();

        // Получаем таймаут в минутах из конфига (по умолчанию 30 минут)
        long timeoutMinutes = plugin.getConfig().getLong("session-timeout-minutes", 30);
        long timeoutMillis = timeoutMinutes * 60 * 1000; // Переводим в миллисекунды

        // Если прошло больше времени, чем указано в таймауте
        if (currentTime - lastActivityTime > timeoutMillis) {
            invalidateSession(uuid); // Удаляем протухшую сессию
            return false;
        }

        return true;
    }

    /**
     * Принудительно удаляет сессию (например, если мы заметили смену IP или игрок сам захотел выйти).
     * @param uuid UUID игрока
     */
    public void invalidateSession(UUID uuid) {
        sessions.remove(uuid);
    }
}