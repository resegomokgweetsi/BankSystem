import javafx.collections.ObservableList;

public class CustomerController {

    private ObservableList<Customer> customers;

    public CustomerController(ObservableList<Customer> customers) {
        this.customers = customers;
    }

    public boolean addCustomer(String id,
                               String firstName,
                               String surname,
                               String address,
                               String contact) {

        if (id.isEmpty() || firstName.isEmpty()
                || surname.isEmpty()
                || address.isEmpty()
                || contact.isEmpty()) {
            return false;
        }

        for (Customer customer : customers) {
            if (customer.getCustomerId().equalsIgnoreCase(id)) {
                return false;
            }
        }

        Customer newCustomer =
                new Customer(id, firstName, surname, address, contact);

        customers.add(newCustomer);

        return true;
    }

    public ObservableList<Customer> getCustomers() {
        return customers;
    }
}