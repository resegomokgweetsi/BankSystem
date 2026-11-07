import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User {

    private String userId;
    private String username;
    private String passwordHash;

    private int failedAttempts;
    private boolean locked;

    public User(String userId, String username, String password) {

        this.userId = userId;
        this.username = username;
        this.passwordHash = hashPassword(password);

        this.failedAttempts = 0;
        this.locked = false;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public boolean authenticate(String username, String password) {

        if (locked) {
            System.out.println("Account is locked.");
            return false;
        }

        if (!this.username.equals(username)) {
            failedAttempts++;
            return false;
        }

        String enteredPasswordHash = hashPassword(password);

        if (passwordHash.equals(enteredPasswordHash)) {

            failedAttempts = 0;

            System.out.println("Login successful.");

            return true;

        } else {

            failedAttempts++;

            System.out.println("Incorrect password.");

            if (failedAttempts >= 3) {
                locked = true;
                System.out.println("Account locked after 3 failed attempts.");
            }

            return false;
        }
    }

    public void logout() {
        System.out.println("User logged out successfully.");
    }

    public boolean isLocked() {
        return locked;
    }

    private String hashPassword(String password) {

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] hash = md.digest(
                    password.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {

                String hex = Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new RuntimeException(e);
        }
    }
}
