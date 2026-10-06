public class InvestmentAccount extends Account {

    private static final double INTEREST_RATE = 0.05;
    private static final double MINIMUM_INITIAL_DEPOSIT = 500.00;

    public InvestmentAccount(String accountNumber, double balance, String branch) {
        super(accountNumber, balance, branch);

        if (balance < MINIMUM_INITIAL_DEPOSIT) {
            System.out.println("Warning: Investment Account requires a minimum initial deposit of BWP 500.");
        }
    }

    @Override
    public double calculateInterest() {
        return getBalance() * INTEREST_RATE;
    }

    @Override
    public void displayAccountDetails() {
        System.out.println("Investment Account");
        super.displayAccountDetails();
        System.out.println("Monthly Interest Rate: 5%");
    }
}
