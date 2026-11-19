import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BankDatabase {
    public record UserCredentials(String username, byte[] salt, byte[] passwordHash) {}
    public record CustomerData(
            String id,
            String firstName,
            String surname,
            String gender,
            String address,
            String contact,
            List<AccountData> accounts) {}
    public record AccountData(
            String number,
            String type,
            String branch,
            double balance,
            String companyName,
            String companyAddress,
            List<TransactionData> transactions) {}
    public record TransactionData(String id, String type, double amount, String timestamp) {}
    public enum TransactionStatus {
        COMPLETED,
        ACCOUNT_NOT_FOUND,
        INSUFFICIENT_FUNDS
    }
    public record TransactionOutcome(TransactionStatus status, long balanceCents) {}
    private final String jdbcUrl;
    public BankDatabase(Path file) {
        jdbcUrl = "jdbc:sqlite:" + file.toAbsolutePath();
    }

    public void initialize() throws SQLException {
        try (Connection connection = open();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        username TEXT PRIMARY KEY COLLATE NOCASE,
                        salt BLOB NOT NULL,
                        password_hash BLOB NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS customers (
                        customer_id TEXT PRIMARY KEY COLLATE NOCASE,
                        first_name TEXT NOT NULL,
                        surname TEXT NOT NULL,
                        gender TEXT NOT NULL CHECK (gender IN ('Male', 'Female', 'Unspecified')),
                        address TEXT NOT NULL,
                        contact TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        account_number TEXT PRIMARY KEY COLLATE NOCASE,
                        customer_id TEXT NOT NULL REFERENCES customers(customer_id),
                        account_type TEXT NOT NULL CHECK (account_type IN ('Savings', 'Investment', 'Cheque')),
                        branch TEXT NOT NULL,
                        balance_cents INTEGER NOT NULL CHECK (balance_cents >= 0),
                        company_name TEXT,
                        company_address TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        account_number TEXT NOT NULL REFERENCES accounts(account_number),
                        transaction_type TEXT NOT NULL CHECK (transaction_type IN ('Deposit', 'Withdraw')),
                        amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
                        created_at TEXT NOT NULL
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_accounts_customer ON accounts(customer_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_number)");
            if (countCustomers(connection) == 0) {
                seedCurrentDashboard(connection);
            }
        }
    }

    public boolean isSetupRequired() throws SQLException {
        try (Connection connection = open();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            return result.next() && result.getInt(1) == 0;
        }
    }

    public void createOwner(String username, byte[] salt, byte[] passwordHash) throws SQLException {
        try (Connection connection = open()) {
            if (countUsers(connection) != 0) {
                throw new IllegalStateException("BankSystem setup has already been completed.");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users (username, salt, password_hash, created_at) VALUES (?, ?, ?, ?)")) {
                statement.setString(1, username);
                statement.setBytes(2, salt);
                statement.setBytes(3, passwordHash);
                statement.setString(4, Instant.now().toString());
                statement.executeUpdate();
            }
        }
    }

    public Optional<UserCredentials> findUser(String username) throws SQLException {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT username, salt, password_hash FROM users WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserCredentials(
                        result.getString("username"),
                        result.getBytes("salt"),
                        result.getBytes("password_hash")));
            }
        }
    }

    public List<CustomerData> getCustomers() throws SQLException {
        List<CustomerData> customers = new ArrayList<>();
        try (Connection connection = open();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT customer_id, first_name, surname, gender, address, contact
                     FROM customers ORDER BY first_name, surname
                     """)) {
            while (result.next()) {
                String customerId = result.getString("customer_id");
                customers.add(new CustomerData(
                        customerId,
                        result.getString("first_name"),
                        result.getString("surname"),
                        result.getString("gender"),
                        result.getString("address"),
                        result.getString("contact"),
                        getAccounts(connection, customerId)));
            }
        }
        return customers;
    }

    public boolean customerExists(String customerId) throws SQLException {
        return exists("SELECT 1 FROM customers WHERE customer_id = ?", customerId);
    }

    public boolean accountExists(String accountNumber) throws SQLException {
        return exists("SELECT 1 FROM accounts WHERE account_number = ?", accountNumber);
    }

    public void addCustomer(
            String id,
            String firstName,
            String surname,
            String gender,
            String address,
            String contact) throws SQLException {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO customers (customer_id, first_name, surname, gender, address, contact)
                     VALUES (?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, id);
            statement.setString(2, firstName);
            statement.setString(3, surname);
            statement.setString(4, gender);
            statement.setString(5, address);
            statement.setString(6, contact);
            statement.executeUpdate();
        }
    }

    public void addAccount(
            String number,
            String customerId,
            String type,
            String branch,
            long balanceCents,
            String companyName,
            String companyAddress) throws SQLException {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO accounts (
                         account_number, customer_id, account_type, branch,
                         balance_cents, company_name, company_address
                     ) VALUES (?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, number);
            statement.setString(2, customerId);
            statement.setString(3, type);
            statement.setString(4, branch);
            statement.setLong(5, balanceCents);
            statement.setString(6, companyName);
            statement.setString(7, companyAddress);
            statement.executeUpdate();
        }
    }

    public TransactionOutcome transact(
            String accountNumber,
            String type,
            long amountCents) throws SQLException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                long balance;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT balance_cents FROM accounts WHERE account_number = ?")) {
                    statement.setString(1, accountNumber);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return new TransactionOutcome(TransactionStatus.ACCOUNT_NOT_FOUND, 0);
                        }
                        balance = result.getLong(1);
                    }
                }

                if (type.equals("Withdraw") && amountCents > balance) {
                    connection.rollback();
                    return new TransactionOutcome(TransactionStatus.INSUFFICIENT_FUNDS, balance);
                }
                long updatedBalance = type.equals("Deposit")
                        ? Math.addExact(balance, amountCents)
                        : balance - amountCents;
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE accounts SET balance_cents = ? WHERE account_number = ?")) {
                    statement.setLong(1, updatedBalance);
                    statement.setString(2, accountNumber);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO transactions (account_number, transaction_type, amount_cents, created_at)
                        VALUES (?, ?, ?, ?)
                        """)) {
                    statement.setString(1, accountNumber);
                    statement.setString(2, type);
                    statement.setLong(3, amountCents);
                    statement.setString(4, Instant.now().toString());
                    statement.executeUpdate();
                }
                connection.commit();
                return new TransactionOutcome(TransactionStatus.COMPLETED, updatedBalance);
            } catch (SQLException | ArithmeticException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private List<AccountData> getAccounts(Connection connection, String customerId) throws SQLException {
        List<AccountData> accounts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT account_number, account_type, branch, balance_cents, company_name, company_address
                FROM accounts WHERE customer_id = ? ORDER BY account_number
                """)) {
            statement.setString(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String accountNumber = result.getString("account_number");
                    accounts.add(new AccountData(
                            accountNumber,
                            result.getString("account_type"),
                            result.getString("branch"),
                            result.getLong("balance_cents") / 100.0,
                            result.getString("company_name"),
                            result.getString("company_address"),
                            getTransactions(connection, accountNumber)));
                }
            }
        }
        return accounts;
    }

    private List<TransactionData> getTransactions(Connection connection, String accountNumber)
            throws SQLException {
        List<TransactionData> transactions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT transaction_id, transaction_type, amount_cents, created_at
                FROM transactions WHERE account_number = ? ORDER BY transaction_id DESC
                """)) {
            statement.setString(1, accountNumber);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    transactions.add(new TransactionData(
                            "T" + result.getLong("transaction_id"),
                            result.getString("transaction_type"),
                            result.getLong("amount_cents") / 100.0,
                            result.getString("created_at")));
                }
            }
        }
        return transactions;
    }

    private boolean exists(String sql, String key) throws SQLException {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private int countCustomers(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM customers")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private int countUsers(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private void seedCurrentDashboard(Connection connection) throws SQLException {
        try (PreparedStatement customer = connection.prepareStatement("""
                     INSERT INTO customers (customer_id, first_name, surname, gender, address, contact)
                     VALUES (?, ?, ?, 'Unspecified', ?, ?)
                     """);
             PreparedStatement account = connection.prepareStatement("""
                     INSERT INTO accounts (
                         account_number, customer_id, account_type, branch, balance_cents, company_name, company_address
                     ) VALUES (?, ?, ?, ?, ?, ?, ?)
                     """)) {
            addSeedCustomer(customer, "C001", "Alex", "Example", "Demo City", "70000001");
            addSeedAccount(account, "SA001", "C001", "Savings", "Demo City", 150000, null, null);
            addSeedAccount(account, "IA001", "C001", "Investment", "Demo City", 100000, null, null);

            addSeedCustomer(customer, "C002", "Jordan", "Sample", "Sample City", "70000002");
            addSeedAccount(account, "CA001", "C002", "Cheque", "Sample City", 220000,
                    "Sample Logistics", "Sample City");

            addSeedCustomer(customer, "C003", "Taylor", "Demo", "Demo Town", "70000003");
            addSeedAccount(account, "SA002", "C003", "Savings", "Demo Town", 250000, null, null);

            addSeedCustomer(customer, "C004", "Casey", "Test", "Sample City", "70000004");
        }
    }

    private void addSeedCustomer(
            PreparedStatement statement,
            String id,
            String firstName,
            String surname,
            String address,
            String contact) throws SQLException {
        statement.setString(1, id);
        statement.setString(2, firstName);
        statement.setString(3, surname);
        statement.setString(4, address);
        statement.setString(5, contact);
        statement.executeUpdate();
    }

    private void addSeedAccount(
            PreparedStatement statement,
            String number,
            String customerId,
            String type,
            String branch,
            long balanceCents,
            String companyName,
            String companyAddress) throws SQLException {
        statement.setString(1, number);
        statement.setString(2, customerId);
        statement.setString(3, type);
        statement.setString(4, branch);
        statement.setLong(5, balanceCents);
        statement.setString(6, companyName);
        statement.setString(7, companyAddress);
        statement.executeUpdate();
    }
}
