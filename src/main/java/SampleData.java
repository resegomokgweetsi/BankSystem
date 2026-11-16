import java.util.ArrayList;
import java.util.List;

public class SampleData {

    public static List<Customer> createCustomers() {
        List<Customer> customers = new ArrayList<>();

        Customer customer1 = new Customer(
                "C001",
                "Alex",
                "Example",
                "Demo City",
                "70000001"
        );
        customer1.addAccount(new SavingsAccount("SA001", 1500.00, "Demo City"));
        customer1.addAccount(new InvestmentAccount("IA001", 1000.00, "Demo City"));
        customers.add(customer1);

        Customer customer2 = new Customer(
                "C002",
                "Jordan",
                "Sample",
                "Sample City",
                "70000002"
        );
        customer2.addAccount(new ChequeAccount(
                "CA001",
                2200.00,
                "Sample City",
                "Sample Logistics",
                "Sample City"
        ));
        customers.add(customer2);

        Customer customer3 = new Customer(
                "C003",
                "Taylor",
                "Demo",
                "Demo Town",
                "70000003"
        );
        customer3.addAccount(new SavingsAccount("SA002", 2500.00, "Demo Town"));
        customers.add(customer3);

        customers.add(new Customer(
                "C004",
                "Casey",
                "Test",
                "Sample City",
                "70000004"
        ));

        return customers;
    }
}
