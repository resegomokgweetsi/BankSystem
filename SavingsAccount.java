public class SavingsAccount extends Account {

    private static final double INTEREST_RATE = 0.0005; // 0.05%

    public SavingsAccount(String accountNumber, double balance, String branch) {
        super(accountNumber, balance, branch);
    }
  
    @Override
    public double calculateInterest() {
        return getBalance() * INTEREST_RATE;
    }

    @Override
    public void withdraw(double amount) {
        System.out.println("Withdrawal not allowed on a Savings Account.");
    }
}
