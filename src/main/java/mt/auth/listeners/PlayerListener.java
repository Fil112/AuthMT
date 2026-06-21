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

import java.util.UUID;

public class PlayerListener implements Listener {

    private final AuthMT plugin;

    public PlayerListener(AuthMT plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Проверяем, есть ли у игрока действующая сессия (заходил ли он недавно)
        if (!plugin.getSessionManager().hasValidSession(uuid)) {
            // Если сессии нет, отправляем в мир-пустоту
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
        // При выходе игрока обновляем время его сессии, чтобы начать отсчет таймаута
        plugin.getSessionManager().saveSession(event.getPlayer().getUniqueId());
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
                // Возвращаем игрока на исходную точку, вместо отмены события (чтобы не было дерганий камеры)
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
        String message = event.getMessage().toLowerCase();

        // Если игрок не авторизован
        if (!plugin.getSessionManager().hasValidSession(event.getPlayer().getUniqueId())) {
            // Разрешаем только команды авторизации
            if (!message.startsWith("/login") && !message.startsWith("/register")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Сначала авторизуйтесь с помощью /login или /register!");
            }
        }
    }
}