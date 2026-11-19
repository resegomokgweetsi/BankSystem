
import java.time.LocalDateTime;

public class Transaction {

    private String transactionId;
    private TransactionType type;
    private double amount;
    private LocalDateTime date;

    public Transaction(String transactionId, TransactionType type, double amount) {
        this(transactionId, type, amount, LocalDateTime.now());
    }

    public Transaction(
            String transactionId,
            TransactionType type,
            double amount,
            LocalDateTime date) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.date = date;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void displayTransaction() {
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Type: " + type);
        System.out.println("Amount: BWP " + amount);
        System.out.println("Date: " + date);
    }
}
