package server.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PasswordUtil {
    private static final String AES_KEY = "MySecretKeyTasks"; // Khoa AES co dinh 16 ky tu
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // 1. Ham Hash mot chieu cho mat khau bang PBKDF2
    public static String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            String saltHex = HexFormat.of().formatHex(salt);
            String hashHex = HexFormat.of().formatHex(hash);
            return saltHex + "$" + hashHex;
        } catch (Exception ex) {
            throw new RuntimeException("Loi khi bam mat khau", ex);
        }
    }

    // 2. Kiem tra mat khau: doi chieu mat khau nhap vao voi hash PBKDF2
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.contains("$")) {
            return false;
        }
        try {
            String[] parts = hashedPassword.split("\\$");
            if (parts.length != 2) {
                return false;
            }
            byte[] salt = HexFormat.of().parseHex(parts[0]);
            byte[] hash = HexFormat.of().parseHex(parts[1]);

            byte[] testHash = pbkdf2(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            return MessageDigest.isEqual(hash, testHash);
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    // 3. Ham ma hoa du lieu hai chieu bang thuat toan AES
    public static String encryptAES(String data) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            return null;
        }
    }

    // 4. Ham giai ma du lieu hai chieu AES khi hien thi len Client
    public static String decryptAES(String encryptedData) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
        } catch (Exception e) {
            return null;
        }
    }
}
