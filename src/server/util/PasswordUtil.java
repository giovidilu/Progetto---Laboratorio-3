package server.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil(){}

    public static String generateSalt(){
        byte[] saltBytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    public static String hashPassword(String password, String salt){
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password e salt non possono essere nulli.");
        }

        try{
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            String input = password + salt;
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for(byte b: hashBytes){
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1){
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch(NoSuchAlgorithmException e){
            throw new IllegalStateException("Algoritmo di hashin non disponibile: " + HASH_ALGORITHM, e);
        }
    }
}
