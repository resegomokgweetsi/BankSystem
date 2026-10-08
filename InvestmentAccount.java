public class InvestmentAccount extends Account {

    private static final double INTEREST_RATE = 0.05;
    private static final double MINIMUM_INITIAL_DEPOSIT = 500.00;

    public InvestmentAccount(String accountNumber,
                             double balance,
                             String branch) {

        super(accountNumber, balance, branch);

        if (balance < MINIMUM_INITIAL_DEPOSIT) {

            throw new IllegalArgumentException(
                    "Investment Account requires a minimum initial deposit of BWP 500."
            );
        }
    }

    @Override
    public double calculateInterest() {

        return getBalance() * INTEREST_RATE;
    }

    public void payInterest() {

        double interest = calculateInterest();

        addInterest(interest);

        System.out.println("Investment interest paid: BWP " + interest);
    }

    @Override
    public void displayAccountDetails() {

        System.out.println("Investment Account");

        super.displayAccountDetails();

        System.out.println("Monthly Interest Rate: 5%");
        System.out.println("Minimum Initial Deposit: BWP 500");
    }
}
