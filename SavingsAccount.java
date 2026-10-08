public class SavingsAccount extends Account {

    private static final double INTEREST_RATE = 0.0005;

    public SavingsAccount(String accountNumber, double balance, String branch) {
        super(accountNumber, balance, branch);
    }

    @Override
    public double calculateInterest() {

        return getBalance() * INTEREST_RATE;
    }

    public void payInterest() {

        double interest = calculateInterest();

        addInterest(interest);

        System.out.println("Savings interest paid: BWP " + interest);
    }

    @Override
    public void withdraw(double amount) {

        System.out.println(
                "Withdrawal not allowed on a Savings Account."
        );
    }

    @Override
    public void displayAccountDetails() {

        System.out.println("Savings Account");

        super.displayAccountDetails();

        System.out.println("Monthly Interest Rate: 0.05%");
    }
}
