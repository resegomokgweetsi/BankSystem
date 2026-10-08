import java.util.ArrayList;
import java.util.List;

public abstract class Account implements IBankAccount {

    private String accountNumber;
    private double balance;
    private String branch;

    private List<Transaction> transactions;

    private static int transactionCounter = 1;

    public Account(String accountNumber, double balance, String branch) {

        this.accountNumber = accountNumber;
        this.balance = balance;
        this.branch = branch;

        transactions = new ArrayList<>();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public String getBranch() {
        return branch;
    }

    @Override
    public void deposit(double amount) {

        if (amount <= 0) {
            System.out.println("Deposit amount must be greater than zero.");
            return;
        }

        balance += amount;

        recordTransaction(TransactionType.DEPOSIT, amount);

        System.out.println("Deposit successful.");
        System.out.println("New balance: BWP " + balance);
    }

    @Override
    public void withdraw(double amount) {

        if (amount <= 0) {
            System.out.println("Withdrawal amount must be greater than zero.");
            return;
        }

        if (amount > balance) {
            System.out.println("Insufficient funds.");
            return;
        }

        balance -= amount;

        recordTransaction(TransactionType.WITHDRAWAL, amount);

        System.out.println("Withdrawal successful.");
        System.out.println("New balance: BWP " + balance);
    }

    protected void addInterest(double amount) {

        if (amount <= 0) {
            return;
        }

        balance += amount;

        recordTransaction(TransactionType.INTEREST, amount);
    }

    protected void recordTransaction(TransactionType type, double amount) {

        String transactionId = "T" + transactionCounter++;

        Transaction transaction =
                new Transaction(transactionId, type, amount);

        transactions.add(transaction);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void displayTransactionHistory() {

        System.out.println("\n===== TRANSACTION HISTORY =====");

        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        for (Transaction transaction : transactions) {

            transaction.displayTransaction();

            System.out.println("----------------------------");
        }
    }

    @Override
    public abstract double calculateInterest();

    public void displayAccountDetails() {

        System.out.println("Account Number: " + accountNumber);
        System.out.println("Balance: BWP " + balance);
        System.out.println("Branch: " + branch);
    }

    // Method overloading
    public void displayAccountDetails(boolean showTransactions) {

        displayAccountDetails();

        if (showTransactions) {
            displayTransactionHistory();
        }
    }
}
