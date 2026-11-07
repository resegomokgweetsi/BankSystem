import java.math.BigDecimal;

public class TransactionController {

    public String deposit(Account account,
                          String amountText) {

        if (account == null) {
            return "No account selected.";
        }

        try {

            BigDecimal amount =
                    new BigDecimal(amountText);

            if (amount.doubleValue() <= 0) {
                return "Amount must be greater than zero.";
            }

            account.deposit(amount.doubleValue());

            return "Deposit successful.";

        } catch (Exception e) {

            return "Invalid amount.";
        }
    }

    public String withdraw(Account account,
                           String amountText) {

        if (account == null) {
            return "No account selected.";
        }

        try {

            BigDecimal amount =
                    new BigDecimal(amountText);

            if (amount.doubleValue() <= 0) {
                return "Amount must be greater than zero.";
            }

            if (amount.doubleValue() > account.getBalance()) {
                return "Insufficient funds.";
            }

            account.withdraw(amount.doubleValue());

            return "Withdrawal successful.";

        } catch (Exception e) {

            return "Invalid amount.";
        }
    }

    public int getTransactionCount(Customer customer) {

        int total = 0;

        for (Account account : customer.getAccounts()) {
            total += account.getTransactions().size();
        }

        return total;
    }
}