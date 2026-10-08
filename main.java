public class Main {

    public static void main(String[] args) {

        System.out.println("===== BANKING SYSTEM TEST =====");

        // Create customer
        Customer customer = new Customer(
                "C001",
                "Resego",
                "Mokgweetsi",
                "Gaborone",
                "71234567"
        );

        // Create accounts
        SavingsAccount savings =
                new SavingsAccount(
                        "SA001",
                        1000.00,
                        "Gaborone"
                );

        InvestmentAccount investment =
                new InvestmentAccount(
                        "IA001",
                        1000.00,
                        "Gaborone"
                );

        ChequeAccount cheque =
                new ChequeAccount(
                        "CA001",
                        2000.00,
                        "Gaborone",
                        "ABC Company",
                        "Gaborone"
                );

        // Add accounts to customer
        customer.addAccount(savings);
        customer.addAccount(investment);
        customer.addAccount(cheque);

        // Display customer
        customer.displayCustomerDetails();

        // Display accounts
        customer.displayAllAccounts();

        // Deposit
        System.out.println("\n===== DEPOSIT TEST =====");

        savings.deposit(500);

        // Withdrawal
        System.out.println("\n===== WITHDRAWAL TEST =====");

        investment.withdraw(200);

        // Savings withdrawal
        System.out.println("\n===== SAVINGS WITHDRAWAL TEST =====");

        savings.withdraw(100);

        // Interest
        System.out.println("\n===== INTEREST TEST =====");

        savings.payInterest();

        investment.payInterest();

        // Transaction history
        System.out.println("\n===== TRANSACTION HISTORY =====");

        savings.displayTransactionHistory();

        investment.displayTransactionHistory();

        // Login test
        System.out.println("\n===== LOGIN TEST =====");

        User user = new User(
                "U001",
                "resego",
                "1234"
        );

        user.authenticate("resego", "1234");

        user.logout();

        System.out.println("\n===== TEST COMPLETE =====");
    }
}
