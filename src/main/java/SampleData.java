import java.util.ArrayList;
import java.util.List;

public class SampleData {

    public static List<Customer> createCustomers() {
        List<Customer> customers = new ArrayList<>();

        Customer customer1 = new Customer(
                "C001",
                "Resego",
                "Mokgweetsi",
                "Gaborone",
                "71234567"
        );
        customer1.addAccount(new SavingsAccount("SA001", 1500.00, "Gaborone"));
        customer1.addAccount(new InvestmentAccount("IA001", 1000.00, "Gaborone"));
        customers.add(customer1);

        Customer customer2 = new Customer(
                "C002",
                "Mpho",
                "Molefe",
                "Francistown",
                "76543210"
        );
        customer2.addAccount(new ChequeAccount(
                "CA001",
                2200.00,
                "Francistown",
                "Blue Sky Logistics",
                "Francistown"
        ));
        customers.add(customer2);

        Customer customer3 = new Customer(
                "C003",
                "Keabetswe",
                "Ntsima",
                "Maun",
                "73333333"
        );
        customer3.addAccount(new SavingsAccount("SA002", 2500.00, "Maun"));
        customers.add(customer3);

        return customers;
    }
}
