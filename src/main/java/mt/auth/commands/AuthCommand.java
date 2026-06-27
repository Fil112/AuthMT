package mt.auth.commands;

import mt.auth.AuthMT;
import mt.auth.listeners.PlayerListener;
import mt.auth.utils.HashUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AuthCommand implements CommandExecutor {

    private final AuthMT plugin;

    public AuthCommand(AuthMT plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Логика для админской команды /amt
        if (command.getName().equalsIgnoreCase("amt")) {
            // Разрешаем перезагрузку только операторам
            if (!sender.isOp() && !sender.hasPermission("authmt.admin")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав для этой команды!");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                plugin.getConfigUtil().reload();
                sender.sendMessage(ChatColor.GREEN + "Конфигурация AuthMT успешно перезагружена!");
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "Использование: /amt reload");
            return true;
        }

        // Защита от запуска консолью команд, которые мы перехватываем
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда доступна только игрокам!");
            return true;
        }

        return false;
    }

    /**
     * Этот метод теперь вызывается НАПРЯМУЮ из PlayerListener.
     * Это позволяет обойти ядро сервера, чтобы пароль не записался в консоль.
     */
    public void handleAuthLogic(Player player, String cmdName, String[] args) {
        UUID uuid = player.getUniqueId();

        // --- ЛОГИКА РЕГИСТРАЦИИ ---
        if (cmdName.equals("register") || cmdName.equals("reg")) {
            if (plugin.getDatabaseManager().isRegistered(uuid)) {
                player.sendMessage(ChatColor.RED + "Вы уже зарегистрированы! Используйте /login <пароль>");
                return;
            }

            if (args.length != 2) {
                player.sendMessage(ChatColor.YELLOW + "Использование: /register <пароль> <повтор_пароля>");
                return;
            }

            String password = args[0];
            String passwordConfirm = args[1];

            if (!password.equals(passwordConfirm)) {
                player.sendMessage(ChatColor.RED + "Пароли не совпадают! Попробуйте еще раз.");
                return;
            }

            String hashedPassword = HashUtil.hash(password);

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().saveUser(uuid, hashedPassword);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getSessionManager().createSession(uuid);
                    player.sendMessage(ChatColor.GREEN + "Вы успешно зарегистрировались и вошли в игру!");
                    // Выводим в лог безопасное сообщение без пароля
                    plugin.getLogger().info("Игрок " + player.getName() + " успешно зарегистрировался.");

                    Location originalLoc = PlayerListener.preAuthLocations.remove(uuid);
                    if (originalLoc != null) {
                        player.teleport(originalLoc);
                    }
                });
            });
            return;
        }

        // --- ЛОГИКА АВТОРИЗАЦИИ ---
        if (cmdName.equals("login") || cmdName.equals("l") || cmdName.equals("log")) {
            if (!plugin.getDatabaseManager().isRegistered(uuid)) {
                player.sendMessage(ChatColor.RED + "Вы не зарегистрированы! Используйте /register <пароль> <повтор_пароля>");
                return;
            }

            if (plugin.getSessionManager().hasValidSession(uuid)) {
                player.sendMessage(ChatColor.YELLOW + "Вы уже авторизованы!");
                return;
            }

            if (args.length != 1) {
                player.sendMessage(ChatColor.YELLOW + "Использование: /login <пароль>");
                return;
            }

            String password = args[0];

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String savedHash = plugin.getDatabaseManager().getPasswordHash(uuid);
                boolean isCorrect = HashUtil.checkPassword(password, savedHash);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (isCorrect) {
                        plugin.getSessionManager().createSession(uuid);
                        player.sendMessage(ChatColor.GREEN + "Вы успешно авторизовались!");
                        plugin.getLogger().info("Игрок " + player.getName() + " вошел на сервер.");

                        Location originalLoc = PlayerListener.preAuthLocations.remove(uuid);
                        if (originalLoc != null) {
                            player.teleport(originalLoc);
                        }
                    } else {
                        player.sendMessage(ChatColor.DARK_RED + "Неверный пароль!");
                        plugin.getLogger().warning("Игрок " + player.getName() + " ввел неверный пароль!");
                    }
                });
            });
        }
    }
}