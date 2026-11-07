import java.util.ArrayList;
import java.util.List;

public class AccountController {

    public List<Account> getCustomerAccounts(Customer customer) {

        if (customer == null) {
            return new ArrayList<>();
        }

        return customer.getAccounts();
    }

    public int getTotalAccounts(List<Customer> customers) {

        int total = 0;

        for (Customer customer : customers) {
            total += customer.getAccounts().size();
        }

        return total;
    }

    public double getTotalBalance(List<Customer> customers) {

        double balance = 0;

        for (Customer customer : customers) {

            for (Account account : customer.getAccounts()) {
                balance += account.getBalance();
            }
        }

        return balance;
    }
}