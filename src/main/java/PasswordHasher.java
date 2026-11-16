import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH_BITS = 256;
    private static final int ITERATIONS = 310_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, HASH_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable.", exception);
        } finally {
            spec.clearPassword();
            Arrays.fill(password, '\0');
        }
    }

    public static boolean matches(char[] password, byte[] salt, byte[] expectedHash) {
        return MessageDigest.isEqual(hash(password, salt), expectedHash);
    }
}
