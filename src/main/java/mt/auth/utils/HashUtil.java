package mt.auth.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Утилита для безопасного хэширования паролей.
 * Никогда не храните пароли в открытом виде!
 */
public class HashUtil {

    /**
     * Хэширует строку (пароль) с использованием алгоритма SHA-256.
     * * @param password Пароль в открытом виде (введенный игроком)
     * @return Захэшированная строка в формате hex (длиной 64 символа)
     */
    public static String hash(String password) {
        if (password == null) {
            return null;
        }

        try {
            // Получаем инстанс встроенного в Java алгоритма SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Превращаем строку пароля в байты и хэшируем
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Конвертируем полученные байты в читаемую 16-ричную строку (hex)
            return bytesToHex(encodedhash);

        } catch (NoSuchAlgorithmException e) {
            // Эта ошибка почти невозможна на современных версиях Java, но мы обязаны её обработать
            throw new RuntimeException("Критическая ошибка: Алгоритм SHA-256 не поддерживается вашей Java!", e);
        }
    }

    /**
     * Проверяет, совпадает ли введенный пароль с хэшем из базы данных.
     * * @param password Введенный игроком пароль
     * @param hashed Сохраненный в базе хэш
     * @return true, если пароли совпадают
     */
    public static boolean checkPassword(String password, String hashed) {
        if (password == null || hashed == null) {
            return false;
        }

        // Хэшируем введенный пароль и просто сравниваем две получившиеся строки
        String newHash = hash(password);
        return newHash.equals(hashed);
    }

    /**
     * Вспомогательный метод для конвертации массива байт в hex-строку.
     * (Стандартная операция при криптографии в Java).
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0'); // Добавляем ведущий ноль, если нужно
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}