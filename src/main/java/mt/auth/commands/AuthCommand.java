package mt.auth.commands;

import mt.auth.AuthMT;
import mt.auth.utils.HashUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AuthCommand implements CommandExecutor {

    private final AuthMT plugin;

    // Передаем основной класс плагина через конструктор (Dependency Injection)
    public AuthCommand(AuthMT plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверяем, что команду вводит игрок, а не консоль сервера
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда доступна только игрокам!");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // --- ЛОГИКА РЕГИСТРАЦИИ ---
        if (command.getName().equalsIgnoreCase("register")) {

            // Если игрок уже зарегистрирован в базе
            if (plugin.getDatabaseManager().isRegistered(uuid)) {
                player.sendMessage(ChatColor.RED + "Вы уже зарегистрированы! Используйте /login <пароль>");
                return true;
            }

            // Требуем ввести пароль дважды для исключения опечаток
            if (args.length != 2) {
                player.sendMessage(ChatColor.YELLOW + "Использование: /register <пароль> <повтор_пароля>");
                return true;
            }

            String password = args[0];
            String passwordConfirm = args[1];

            if (!password.equals(passwordConfirm)) {
                player.sendMessage(ChatColor.RED + "Пароли не совпадают! Попробуйте еще раз.");
                return true;
            }

            // Хэшируем пароль перед сохранением (никогда не храним в открытом виде!)
            String hashedPassword = HashUtil.hash(password);

            // Асинхронное сохранение в БД (чтобы не фризить сервер)
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().saveUser(uuid, hashedPassword);

                // Возвращаемся в основной поток для работы с игроком (API Bukkit требует этого)
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    // Создаем сессию (автоматический вход после успешной регистрации)
                    plugin.getSessionManager().createSession(uuid);
                    player.sendMessage(ChatColor.GREEN + "Вы успешно зарегистрировались и вошли в игру!");

                    // TODO: Здесь мы добавим логику телепортации из "пустоты" в нормальный мир
                });
            });

            return true;
        }

        // --- ЛОГИКА АВТОРИЗАЦИИ ---
        if (command.getName().equalsIgnoreCase("login")) {

            // Проверяем, зарегистрирован ли вообще игрок
            if (!plugin.getDatabaseManager().isRegistered(uuid)) {
                player.sendMessage(ChatColor.RED + "Вы не зарегистрированы! Используйте /register <пароль> <повтор_пароля>");
                return true;
            }

            // Проверяем, не авторизован ли он уже (проверка активной сессии)
            if (plugin.getSessionManager().hasValidSession(uuid)) {
                player.sendMessage(ChatColor.YELLOW + "Вы уже авторизованы!");
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(ChatColor.YELLOW + "Использование: /login <пароль>");
                return true;
            }

            String password = args[0];

            // Асинхронно достаем хэш из базы и проверяем пароль
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String savedHash = plugin.getDatabaseManager().getPasswordHash(uuid);

                // Сверяем введенный пароль с хэшем из базы
                boolean isCorrect = HashUtil.checkPassword(password, savedHash);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (isCorrect) {
                        plugin.getSessionManager().createSession(uuid);
                        player.sendMessage(ChatColor.GREEN + "Вы успешно авторизовались!");

                        // TODO: Телепортация из "пустоты" обратно в мир
                    } else {
                        player.sendMessage(ChatColor.DARK_RED + "Неверный пароль!");
                    }
                });
            });

            return true;
        }

        return false;
    }
}