public class Main {

    public static void main(String[] args) {

        System.out.println("===== BANKING SYSTEM TEST =====");

        Customer customer = new Customer(
                "C001",
                "Resego",
                "Mokgweetsi",
                "Gaborone",
                "71234567"
        );

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

        customer.addAccount(savings);
        customer.addAccount(investment);
        customer.addAccount(cheque);

        customer.displayCustomerDetails();
        customer.displayAllAccounts();

        System.out.println("\n===== DEPOSIT TEST =====");
        savings.deposit(500);

        System.out.println("\n===== WITHDRAWAL TEST =====");
        investment.withdraw(200);

        System.out.println("\n===== SAVINGS WITHDRAWAL TEST =====");
        savings.withdraw(100);

        System.out.println("\n===== INTEREST TEST =====");
        savings.payInterest();
        investment.payInterest();

        System.out.println("\n===== TRANSACTION HISTORY =====");
        savings.displayTransactionHistory();
        investment.displayTransactionHistory();

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
