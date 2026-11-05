public class ChequeAccount extends Account {

    private String companyName;
    private String companyAddress;

    public ChequeAccount(String accountNumber,
                         double balance,
                         String branch,
                         String companyName,
                         String companyAddress) {

        super(accountNumber, balance, branch);

        this.companyName = companyName;
        this.companyAddress = companyAddress;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    @Override
    public double calculateInterest() {

        return 0.0;
    }

    @Override
    public void displayAccountDetails() {

        System.out.println("Cheque Account");

        super.displayAccountDetails();

        System.out.println("Company Name: " + companyName);
        System.out.println("Company Address: " + companyAddress);
        System.out.println("Monthly Interest Rate: 0%");
    }
}
