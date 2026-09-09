package server.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Classe di utilità per la gestione crittografica delle password.
 * <p>
 * Fornisce funzioni per la generazione di salt crittograficamente sicuri e per il calcolo
 * dell'hash digest mediante algoritmo SHA-256.
 * Stateless e thread-safe.
 */
public class PasswordUtil {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil(){}

    /**
     * Genera un salt casuale codificato in Base64.
     *
     * @return stringa Base64 rappresentante la sequenza di byte casuali generata
     */
    public static String generateSalt(){
        byte[] saltBytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    /**
     * Calcola l'hash esadecimale SHA-256 della password concatenata al relativo salt.
     * <p>
     * Query pura (nessun effetto collaterale).
     *
     * @param password stringa in chiaro della password
     * @param salt stringa salt associata all'utente
     * @return rappresentazione esadecimale a 64 caratteri del digest calcolato
     * @throws IllegalArgumentException se password o salt sono nulli
     * @throws IllegalStateException se l'algoritmo di digest non è supportato dall'ambiente Java
     */
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
            throw new IllegalStateException("Algoritmo di hashing non disponibile: " + HASH_ALGORITHM, e);
        }
    }
}