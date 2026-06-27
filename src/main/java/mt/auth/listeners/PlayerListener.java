package mt.auth.listeners;

import mt.auth.AuthMT;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final AuthMT plugin;
    // Временное хранилище для локаций игроков до их авторизации
    public static final Map<UUID, Location> preAuthLocations = new HashMap<>();

    public PlayerListener(AuthMT plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Проверяем, есть ли у игрока действующая сессия (заходил ли он недавно)
        if (!plugin.getSessionManager().hasValidSession(uuid)) {

            // 1. Сохраняем ту точку, где игрок находился в основном мире
            preAuthLocations.put(uuid, player.getLocation());

            // 2. Отправляем в мир-пустоту
            String authWorldName = plugin.getConfig().getString("auth-world", "auth");
            World authWorld = Bukkit.getWorld(authWorldName);

            if (authWorld != null) {
                // Телепортируем на спавн мира авторизации
                player.teleport(authWorld.getSpawnLocation());
            } else {
                plugin.getLogger().warning("Внимание: Мир авторизации '" + authWorldName + "' не найден на сервере!");
            }

            player.sendMessage(ChatColor.YELLOW + "Добро пожаловать! Пожалуйста, используйте /login <пароль> или /register <пароль> <пароль>.");
        } else {
            // Если сессия валидна, игрок может сразу играть
            player.sendMessage(ChatColor.GREEN + "С возвращением! Ваша сессия успешно восстановлена.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // При выходе обновляем время сессии
        plugin.getSessionManager().saveSession(uuid);

        // Очищаем локацию из памяти, если игрок вышел, так и не введя пароль
        preAuthLocations.remove(uuid);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Если у игрока нет сессии, запрещаем ему ходить
        if (!plugin.getSessionManager().hasValidSession(player.getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();

            // Разрешаем крутить головой (изменение Yaw/Pitch), но запрещаем менять координаты X/Y/Z
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                // Возвращаем игрока на исходную точку (в мире auth), чтобы не было дерганий камеры
                event.setTo(from);
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Блокируем чат до авторизации
        if (!plugin.getSessionManager().hasValidSession(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Вы не можете писать в чат до авторизации!");
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String[] args = message.split(" ");
        String cmd = args[0].substring(1).toLowerCase(); // Убираем слэш '/'

        Player player = event.getPlayer();

        // 1. ПЕРЕХВАТ КОМАНД АВТОРИЗАЦИИ (ЧТОБЫ СКРЫТЬ ИХ ИЗ КОНСОЛИ)
        if (cmd.equals("login") || cmd.equals("l") || cmd.equals("log") ||
                cmd.equals("register") || cmd.equals("reg")) {

            // ОТМЕНЯЕМ событие. Ядро сервера не запишет эту строку в latest.log!
            event.setCancelled(true);

            // Собираем аргументы (сами пароли) в отдельный массив
            String[] cmdArgs = new String[args.length - 1];
            System.arraycopy(args, 1, cmdArgs, 0, args.length - 1);

            // Передаем логику вручную напрямую в наш AuthCommand
            plugin.getAuthCommand().handleAuthLogic(player, cmd, cmdArgs);
            return; // Прерываем дальнейшую проверку
        }

        // 2. БЛОКИРОВКА ОСТАЛЬНЫХ КОМАНД ДО АВТОРИЗАЦИИ
        if (!plugin.getSessionManager().hasValidSession(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Сначала авторизуйтесь с помощью /login или /register!");
        }
    }
}