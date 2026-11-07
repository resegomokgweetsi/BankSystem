import java.util.ArrayList;
import java.util.List;

public class Customer {

    private String customerId;
    private String firstName;
    private String surname;
    private String address;
    private String contactDetails;

    private List<Account> accounts;

    public Customer(String customerId, String firstName, String surname,
                    String address, String contactDetails) {

        this.customerId = customerId;
        this.firstName = firstName;
        this.surname = surname;
        this.address = address;
        this.contactDetails = contactDetails;

        accounts = new ArrayList<>();
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getSurname() {
        return surname;
    }

    public String getAddress() {
        return address;
    }

    public String getContactDetails() {
        return contactDetails;
    }

    public void addAccount(Account account) {

        if (account == null) {
            System.out.println("Cannot add an empty account.");
            return;
        }

        accounts.add(account);

        System.out.println("Account added successfully.");
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void displayCustomerDetails() {

        System.out.println("\n===== CUSTOMER DETAILS =====");
        System.out.println("Customer ID: " + customerId);
        System.out.println("Name: " + firstName + " " + surname);
        System.out.println("Address: " + address);
        System.out.println("Contact: " + contactDetails);

        System.out.println("\nNumber of Accounts: " + accounts.size());
    }

    public void displayAllAccounts() {

        System.out.println("\n===== CUSTOMER ACCOUNTS =====");

        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }

        for (Account account : accounts) {
            account.displayAccountDetails();
            System.out.println("----------------------------");
        }
    }
}
