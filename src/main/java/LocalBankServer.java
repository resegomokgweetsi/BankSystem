import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LocalBankServer {

    private static final Logger LOGGER = Logger.getLogger(LocalBankServer.class.getName());
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "4173"));
    private static final long SESSION_LIFETIME_MILLIS = 8 * 60 * 60 * 1000L;
    private static final int MAX_REQUEST_BYTES = 16_384;
    private static final String SESSION_COOKIE = "BANKSESSION";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ObjectMapper JSON = new ObjectMapper();

    private final BankDatabase database;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private LocalBankServer(BankDatabase database) {
        this.database = database;
    }

    public static void main(String[] args) throws IOException, SQLException {
        Path databasePath = Path.of(System.getProperty("banksystem.db", "BankSystem.db"))
                .toAbsolutePath();
        BankDatabase database = new BankDatabase(databasePath);
        database.initialize();

        String defaultAddress = System.getenv("CODESPACES") == null ? "127.0.0.1" : "0.0.0.0";
        String bindAddress = System.getenv().getOrDefault("BANKSYSTEM_HOST", defaultAddress);
        InetAddress address = InetAddress.getByName(bindAddress);
        HttpServer server = HttpServer.create(new InetSocketAddress(address, PORT), 0);
        LocalBankServer application = new LocalBankServer(database);
        server.createContext("/", application::handle);
        ExecutorService executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1);
            executor.shutdownNow();
        }));
        server.start();

        LOGGER.info(() -> "BankSystem available at http://localhost:" + PORT
                + " using database " + databasePath);
    }

    private void handle(HttpExchange exchange) throws IOException {
        setSecurityHeaders(exchange);
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                handleApi(exchange, path);
            } else {
                serveDashboard(exchange, path);
            }
        } catch (RequestFailure failure) {
            sendJson(exchange, failure.status, Map.of("error", failure.getMessage()));
        } catch (JsonProcessingException exception) {
            sendJson(exchange, 400, Map.of("error", "Request body must be valid JSON."));
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "BankSystem database operation failed.", exception);
            sendJson(exchange, 500, Map.of("error", "The local database operation failed."));
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Unexpected BankSystem request failure.", exception);
            sendJson(exchange, 500, Map.of("error", "The local server could not complete the request."));
        } finally {
            exchange.close();
        }
    }

    private void handleApi(HttpExchange exchange, String path)
            throws IOException, SQLException, RequestFailure {
        String method = exchange.getRequestMethod();
        if (path.equals("/api/status")) {
            requireMethod(method, "GET");
            Optional<Session> session = sessionFor(exchange);
            Map<String, Object> status = new HashMap<>();
            status.put("initialized", !database.isSetupRequired());
            status.put("authenticated", session.isPresent());
            session.ifPresent(value -> status.put("username", value.username));
            sendJson(exchange, 200, status);
            return;
        }
        if (path.equals("/api/setup")) {
            requireMethod(method, "POST");
            setupOwner(exchange);
            return;
        }
        if (path.equals("/api/login")) {
            requireMethod(method, "POST");
            login(exchange);
            return;
        }
        if (path.equals("/api/logout")) {
            requireMethod(method, "POST");
            logout(exchange);
            return;
        }

        requireSession(exchange);
        if (path.equals("/api/customers")) {
            if (method.equals("GET")) {
                sendJson(exchange, 200, database.getCustomers());
            } else if (method.equals("POST")) {
                addCustomer(exchange);
            } else {
                methodNotAllowed();
            }
            return;
        }
        if (path.equals("/api/accounts")) {
            requireMethod(method, "POST");
            addAccount(exchange);
            return;
        }
        if (path.equals("/api/transactions")) {
            requireMethod(method, "POST");
            transact(exchange);
            return;
        }
        throw new RequestFailure(404, "API endpoint not found.");
    }

    private void setupOwner(HttpExchange exchange) throws IOException, SQLException, RequestFailure {
        if (!database.isSetupRequired()) {
            throw new RequestFailure(409, "Initial setup is already complete. Please log in.");
        }
        Map<String, Object> body = readJson(exchange);
        String username = requiredText(body, "username").trim();
        char[] password = requiredText(body, "password").toCharArray();
        if (!username.matches("[A-Za-z0-9._-]{3,32}")) {
            java.util.Arrays.fill(password, '\0');
            throw new RequestFailure(400, "Username must be 3–32 letters, numbers, dots, underscores, or hyphens.");
        }
        if (password.length < 12) {
            java.util.Arrays.fill(password, '\0');
            throw new RequestFailure(400, "Choose a password with at least 12 characters.");
        }

        byte[] salt = PasswordHasher.newSalt();
        byte[] passwordHash = PasswordHasher.hash(password, salt);
        try {
            database.createOwner(username, salt, passwordHash);
        } catch (IllegalStateException exception) {
            throw new RequestFailure(409, "Initial setup was already completed. Please log in.");
        }
        createSession(exchange, username);
        sendJson(exchange, 201, Map.of("authenticated", true, "username", username));
    }

    private void login(HttpExchange exchange) throws IOException, SQLException, RequestFailure {
        Map<String, Object> body = readJson(exchange);
        String username = requiredText(body, "username").trim();
        char[] password = requiredText(body, "password").toCharArray();
        Optional<BankDatabase.UserCredentials> credentials = database.findUser(username);
        boolean matches = false;
        if (credentials.isPresent()) {
            BankDatabase.UserCredentials user = credentials.get();
            matches = PasswordHasher.matches(password, user.salt(), user.passwordHash());
        } else {
            PasswordHasher.hash(password, PasswordHasher.newSalt());
        }
        if (!matches) {
            throw new RequestFailure(401, "Incorrect username or password.");
        }
        String canonicalUsername = credentials.orElseThrow().username();
        createSession(exchange, canonicalUsername);
        sendJson(exchange, 200, Map.of("authenticated", true, "username", canonicalUsername));
    }

    private void logout(HttpExchange exchange) throws IOException {
        sessionToken(exchange).ifPresent(sessions::remove);
        exchange.getResponseHeaders().add("Set-Cookie",
                SESSION_COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
        sendJson(exchange, 200, Map.of("authenticated", false));
    }

    private void addCustomer(HttpExchange exchange)
            throws IOException, SQLException, RequestFailure {
        Map<String, Object> body = readJson(exchange);
        String id = requiredText(body, "id").trim();
        String firstName = requiredText(body, "firstName").trim();
        String surname = requiredText(body, "surname").trim();
        String gender = requiredText(body, "gender").trim();
        String address = requiredText(body, "address").trim();
        String contact = requiredText(body, "contact").trim();
        if (id.isEmpty() || firstName.isEmpty() || surname.isEmpty()
                || address.isEmpty() || contact.isEmpty()) {
            throw new RequestFailure(400, "Complete all customer fields.");
        }
        if (!gender.equals("Male") && !gender.equals("Female")) {
            throw new RequestFailure(400, "Choose Male or Female for customer gender.");
        }
        if (database.customerExists(id)) {
            throw new RequestFailure(409, "That customer ID is already in use.");
        }
        database.addCustomer(id, firstName, surname, gender, address, contact);
        sendJson(exchange, 201, Map.of("created", true));
    }

    private void addAccount(HttpExchange exchange)
            throws IOException, SQLException, RequestFailure {
        Map<String, Object> body = readJson(exchange);
        String customerId = requiredText(body, "customer").trim();
        String number = requiredText(body, "number").trim();
        String type = requiredText(body, "type").trim();
        String branch = requiredText(body, "branch").trim();
        long balanceCents = amountInCents(requiredText(body, "balance"), true);
        String companyName = optionalText(body, "companyName");
        String companyAddress = optionalText(body, "companyAddress");
        if (!database.customerExists(customerId)) {
            throw new RequestFailure(404, "Customer not found.");
        }
        if (number.isEmpty() || branch.isEmpty()) {
            throw new RequestFailure(400, "Account number and branch are required.");
        }
        if (!List.of("Savings", "Investment", "Cheque").contains(type)) {
            throw new RequestFailure(400, "Choose Savings, Investment, or Cheque.");
        }
        if (database.accountExists(number)) {
            throw new RequestFailure(409, "That account number is already in use.");
        }
        if (type.equals("Investment") && balanceCents < 50_000) {
            throw new RequestFailure(400, "Investment accounts require an initial balance of at least BWP 500.00.");
        }
        if (type.equals("Cheque") && (companyName.isBlank() || companyAddress.isBlank())) {
            throw new RequestFailure(400, "Company name and address are required for a cheque account.");
        }
        database.addAccount(number, customerId, type, branch, balanceCents,
                type.equals("Cheque") ? companyName : null,
                type.equals("Cheque") ? companyAddress : null);
        sendJson(exchange, 201, Map.of("created", true));
    }

    private void transact(HttpExchange exchange)
            throws IOException, SQLException, RequestFailure {
        Map<String, Object> body = readJson(exchange);
        String accountNumber = requiredText(body, "account").trim();
        String type = requiredText(body, "type").trim();
        long amountCents = amountInCents(requiredText(body, "amount"), false);
        if (!type.equals("Deposit") && !type.equals("Withdraw")) {
            throw new RequestFailure(400, "Choose Deposit or Withdraw.");
        }
        BankDatabase.TransactionOutcome outcome =
                database.transact(accountNumber, type, amountCents);
        if (outcome.status() == BankDatabase.TransactionStatus.ACCOUNT_NOT_FOUND) {
            throw new RequestFailure(404, "Account not found.");
        }
        if (outcome.status() == BankDatabase.TransactionStatus.INSUFFICIENT_FUNDS) {
            throw new RequestFailure(409,
                    "Insufficient funds. The withdrawal amount exceeds the available balance.");
        }
        sendJson(exchange, 201, Map.of("created", true, "balance", outcome.balanceCents() / 100.0));
    }

    private void serveDashboard(HttpExchange exchange, String path) throws IOException, RequestFailure {
        requireMethod(exchange.getRequestMethod(), "GET");
        if (!path.equals("/") && !path.equals("/index.html")) {
            throw new RequestFailure(404, "Page not found.");
        }
        Path page = Path.of("web", "index.html").toAbsolutePath().normalize();
        if (!Files.isRegularFile(page)) {
            throw new RequestFailure(404, "Dashboard file was not found. Run the server from the project directory.");
        }
        byte[] content = Files.readAllBytes(page);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, content.length);
        exchange.getResponseBody().write(content);
    }

    private Map<String, Object> readJson(HttpExchange exchange) throws IOException, RequestFailure {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            throw new RequestFailure(415, "Content-Type must be application/json.");
        }
        byte[] content = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (content.length > MAX_REQUEST_BYTES) {
            throw new RequestFailure(413, "Request body is too large.");
        }
        return JSON.readValue(content, new TypeReference<>() {});
    }

    private String requiredText(Map<String, Object> body, String key) throws RequestFailure {
        Object value = body.get(key);
        if (!(value instanceof String text)) {
            throw new RequestFailure(400, "Missing or invalid field: " + key + ".");
        }
        return text;
    }

    private String optionalText(Map<String, Object> body, String key) throws RequestFailure {
        Object value = body.get(key);
        if (value == null) {
            return "";
        }
        if (!(value instanceof String text)) {
            throw new RequestFailure(400, "Invalid field: " + key + ".");
        }
        return text.trim();
    }

    private long amountInCents(String input, boolean allowZero) throws RequestFailure {
        try {
            BigDecimal amount = new BigDecimal(input).setScale(2, RoundingMode.UNNECESSARY);
            long cents = amount.movePointRight(2).longValueExact();
            if (cents < 0 || (!allowZero && cents == 0)) {
                throw new RequestFailure(400,
                        allowZero ? "Amount must be zero or more." : "Amount must be greater than zero.");
            }
            return cents;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new RequestFailure(400, "Enter a valid amount with no more than two decimal places.");
        }
    }

    private Optional<Session> sessionFor(HttpExchange exchange) {
        Optional<String> token = sessionToken(exchange);
        if (token.isEmpty()) {
            return Optional.empty();
        }
        Session session = sessions.get(token.get());
        if (session == null) {
            return Optional.empty();
        }
        if (session.expiresAtMillis < System.currentTimeMillis()) {
            sessions.remove(token.get());
            return Optional.empty();
        }
        Session refreshed = new Session(session.username,
                System.currentTimeMillis() + SESSION_LIFETIME_MILLIS);
        sessions.put(token.get(), refreshed);
        return Optional.of(refreshed);
    }

    private void requireSession(HttpExchange exchange) throws RequestFailure {
        if (sessionFor(exchange).isEmpty()) {
            throw new RequestFailure(401, "Please log in to continue.");
        }
    }

    private Optional<String> sessionToken(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            return Optional.empty();
        }
        for (String cookie : cookieHeader.split(";")) {
            String trimmed = cookie.trim();
            String prefix = SESSION_COOKIE + "=";
            if (trimmed.startsWith(prefix)) {
                String token = trimmed.substring(prefix.length());
                return token.isBlank() ? Optional.empty() : Optional.of(token);
            }
        }
        return Optional.empty();
    }

    private void createSession(HttpExchange exchange, String username) {
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        sessions.put(token, new Session(username,
                System.currentTimeMillis() + SESSION_LIFETIME_MILLIS));
        exchange.getResponseHeaders().add("Set-Cookie",
                SESSION_COOKIE + "=" + token + "; Path=/; Max-Age=28800; HttpOnly; SameSite=Strict");
    }

    private void requireMethod(String actual, String expected) throws RequestFailure {
        if (!actual.equals(expected)) {
            methodNotAllowed();
        }
    }

    private void methodNotAllowed() throws RequestFailure {
        throw new RequestFailure(405, "Method not allowed.");
    }

    private void setSecurityHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
    }

    private void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] content = JSON.writeValueAsBytes(value);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, content.length);
        exchange.getResponseBody().write(content);
    }

    private record Session(String username, long expiresAtMillis) {}

    private static final class RequestFailure extends Exception {
        private final int status;

        private RequestFailure(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
