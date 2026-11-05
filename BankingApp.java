import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class BankingApp extends Application {

    private static final String CURRENCY = "BWP ";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ObservableList<Customer> customers =
            FXCollections.observableArrayList(SampleData.createCustomers());
    private final ObservableList<AccountRow> accountRows =
            FXCollections.observableArrayList();

    private final Label customerTotal = new Label();
    private final Label accountTotal = new Label();
    private final Label balanceTotal = new Label();
    private final Label transactionTotal = new Label();
    private final ComboBox<Customer> customerSelector = new ComboBox<>();
    private final ComboBox<Account> accountSelector = new ComboBox<>();
    private final ComboBox<String> transactionTypeSelector = new ComboBox<>();
    private final TextField amountInput = new TextField("100.00");
    private final Label transactionStatus = new Label("Ready.");
    private final TableView<Transaction> transactionTable = new TableView<>();

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f7fb;");

        Label title = new Label("Banking Dashboard");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #142b4a;");
        Label subtitle = new Label("Manage customers, accounts and transactions");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        VBox heading = new VBox(6, title, subtitle);
        heading.setPadding(new Insets(24, 28, 16, 28));
        root.setTop(heading);

        TabPane tabs = new TabPane(
                createTab("Dashboard", createDashboard()),
                createTab("Customers", createCustomersView()),
                createTab("Accounts", createAccountsView()),
                createTab("Transactions", createTransactionsView())
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPadding(new Insets(0, 20, 20, 20));
        root.setCenter(tabs);

        refreshSummary();
        refreshAccountRows();

        stage.setTitle("Bank System Dashboard");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(new Scene(root, 1200, 780));
        stage.show();
    }

    private Tab createTab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private VBox createDashboard() {
        HBox cards = new HBox(14,
                createSummaryCard("Total Customers", customerTotal),
                createSummaryCard("Total Accounts", accountTotal),
                createSummaryCard("Total Balance", balanceTotal),
                createSummaryCard("Transactions", transactionTotal));
        cards.setAlignment(Pos.CENTER);
        for (var child : cards.getChildren()) {
            HBox.setHgrow(child, Priority.ALWAYS);
        }

        TableView<Customer> table = createCustomerTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox content = new VBox(18, cards, sectionTitle("Registered Customers"), table);
        content.setPadding(new Insets(18, 8, 8, 8));
        return content;
    }

    private VBox createSummaryCard(String title, Label value) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        value.setStyle("-fx-text-fill: #142b4a; -fx-font-size: 24px; -fx-font-weight: bold;");
        VBox card = new VBox(10, label, value);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: #e2e8f0; -fx-border-radius: 10;");
        return card;
    }

    private VBox createCustomersView() {
        TableView<Customer> table = createCustomerTable();
        Label details = new Label("Select a customer to view details.");
        details.setWrapText(true);
        details.setPadding(new Insets(14));
        details.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-border-color: #e2e8f0; -fx-text-fill: #334155;");
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldCustomer, customer) -> {
            if (customer == null) {
                details.setText("Select a customer to view details.");
                return;
            }
            String accountDetails = customer.getAccounts().stream()
                    .map(account -> account.getClass().getSimpleName() + " ("
                            + account.getAccountNumber() + ") - "
                            + formatCurrency(account.getBalance()))
                    .reduce((first, second) -> first + "\n" + second)
                    .orElse("No accounts assigned.");
            details.setText("Customer ID: " + customer.getCustomerId()
                    + "\nName: " + customer.getFirstName() + " " + customer.getSurname()
                    + "\nAddress: " + customer.getAddress()
                    + "\nContact: " + customer.getContactDetails()
                    + "\n\nAccounts:\n" + accountDetails);
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        VBox content = new VBox(14, createCustomerRegistrationForm(),
                sectionTitle("Customer Details"), table, details);
        content.setPadding(new Insets(18, 8, 8, 8));
        return content;
    }

    private TableView<Customer> createCustomerTable() {
        TableView<Customer> table = new TableView<>(customers);
        table.setPlaceholder(new Label("No customers found."));
        table.getColumns().setAll(
                customerColumn("Customer ID", "customerId", 120),
                customerColumn("First Name", "firstName", 150),
                customerColumn("Surname", "surname", 160),
                customerColumn("Address", "address", 200),
                customerColumn("Contact", "contactDetails", 150)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return table;
    }

    private TableColumn<Customer, String> customerColumn(String title, String property, double width) {
        TableColumn<Customer, String> column = new TableColumn<>(title);
        column.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private VBox createAccountsView() {
        TableView<AccountRow> table = new TableView<>(accountRows);
        table.setPlaceholder(new Label("No accounts found."));
        table.getColumns().setAll(
                accountColumn("Customer ID", AccountRow::getCustomerId, 120),
                accountColumn("Account Number", AccountRow::getAccountNumber, 160),
                accountColumn("Type", AccountRow::getType, 180),
                accountColumn("Branch", AccountRow::getBranch, 170),
                accountColumn("Balance", row -> formatCurrency(row.getBalance()), 160)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox content = new VBox(14, sectionTitle("Accounts"), table);
        content.setPadding(new Insets(18, 8, 8, 8));
        return content;
    }

    private TableColumn<AccountRow, String> accountColumn(
            String title, java.util.function.Function<AccountRow, String> value, double width) {
        TableColumn<AccountRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private VBox createTransactionsView() {
        customerSelector.setItems(customers);
        customerSelector.setPromptText("Choose a customer");
        customerSelector.setMaxWidth(Double.MAX_VALUE);
        customerSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getCustomerId() + " - "
                        + customer.getFirstName() + " " + customer.getSurname();
            }

            @Override
            public Customer fromString(String value) {
                return null;
            }
        });

        accountSelector.setPromptText("Choose an account");
        accountSelector.setMaxWidth(Double.MAX_VALUE);
        accountSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Account account) {
                return account == null ? "" : account.getClass().getSimpleName()
                        + " (" + account.getAccountNumber() + ") - "
                        + formatCurrency(account.getBalance());
            }

            @Override
            public Account fromString(String value) {
                return null;
            }
        });
        accountSelector.setCellFactory(view -> createAccountCell());
        accountSelector.setButtonCell(createAccountCell());

        transactionTypeSelector.setItems(FXCollections.observableArrayList("Deposit", "Withdraw"));
        transactionTypeSelector.getSelectionModel().selectFirst();
        amountInput.setPromptText("Amount in BWP");

        customerSelector.valueProperty().addListener((observable, oldCustomer, customer) -> {
            ObservableList<Account> accounts = FXCollections.observableArrayList();
            if (customer != null) {
                accounts.addAll(customer.getAccounts());
            }
            accountSelector.setItems(accounts);
            if (accounts.isEmpty()) {
                accountSelector.getSelectionModel().clearSelection();
                transactionTable.getItems().clear();
            } else {
                accountSelector.getSelectionModel().selectFirst();
            }
            refreshTransactionHistory(accountSelector.getValue());
        });
        accountSelector.valueProperty().addListener((observable, oldAccount, account) ->
                refreshTransactionHistory(account));
        if (!customers.isEmpty()) {
            customerSelector.getSelectionModel().selectFirst();
        }

        Button applyButton = new Button("Apply Transaction");
        applyButton.setDefaultButton(true);
        applyButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-padding: 10 18;");
        applyButton.setOnAction(event -> applyTransaction());

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.add(formField("Customer", customerSelector), 0, 0);
        form.add(formField("Account", accountSelector), 1, 0);
        form.add(formField("Action", transactionTypeSelector), 2, 0);
        form.add(formField("Amount", amountInput), 3, 0);
        form.add(applyButton, 4, 0);
        for (int column = 0; column < 4; column++) {
            GridPane.setHgrow(form.getChildren().get(column), Priority.ALWAYS);
        }

        transactionStatus.setWrapText(true);
        transactionStatus.setStyle("-fx-text-fill: #475569;");
        transactionTable.setPlaceholder(new Label("No transactions for this account."));
        transactionTable.getColumns().setAll(
                transactionColumn("Transaction ID", transaction -> transaction.getTransactionId(), 150),
                transactionColumn("Type", transaction -> transaction.getType().toString(), 150),
                transactionColumn("Amount", transaction -> formatCurrency(transaction.getAmount()), 160),
                transactionColumn("Date", transaction -> transaction.getDate().format(DATE_FORMAT), 220)
        );
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(transactionTable, Priority.ALWAYS);

        VBox content = new VBox(16, sectionTitle("Quick Actions"), form, transactionStatus,
                sectionTitle("Transaction History"), transactionTable);
        content.setPadding(new Insets(18, 8, 8, 8));
        return content;
    }

    private VBox formField(String title, javafx.scene.Node input) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        VBox field = new VBox(6, label, input);
        field.setMinWidth(120);
        return field;
    }

    private TableColumn<Transaction, String> transactionColumn(
            String title, java.util.function.Function<Transaction, String> value, double width) {
        TableColumn<Transaction, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void applyTransaction() {
        Customer customer = customerSelector.getValue();
        Account account = accountSelector.getValue();
        String type = transactionTypeSelector.getValue();
        if (customer == null || account == null || type == null) {
            showTransactionStatus("Choose a customer, account, and transaction type.", true);
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountInput.getText().trim());
            if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException("Enter an amount greater than zero with up to 2 decimal places.");
            }
            if (!Double.isFinite(amount.doubleValue())) {
                throw new IllegalArgumentException("Enter a valid transaction amount.");
            }
            if ("Withdraw".equals(type)
                    && amount.compareTo(BigDecimal.valueOf(account.getBalance())) > 0) {
                throw new IllegalArgumentException("Insufficient funds.");
            }

            double value = amount.doubleValue();
            if ("Deposit".equals(type)) {
                account.deposit(value);
            } else {
                account.withdraw(value);
            }

            amountInput.clear();
            refreshSummary();
            refreshAccountRows();
            accountSelector.setItems(
                    FXCollections.observableArrayList(customer.getAccounts()));
            accountSelector.getSelectionModel().select(account);
            refreshTransactionHistory(account);
            showTransactionStatus(type + " successful. New balance: "
                    + formatCurrency(account.getBalance()), false);
        } catch (NumberFormatException exception) {
            showTransactionStatus("Enter a valid amount, for example 100.00.", true);
        } catch (IllegalArgumentException exception) {
            showTransactionStatus(exception.getMessage(), true);
        }
    }

    private void showTransactionStatus(String message, boolean error) {
        transactionStatus.setText(message);
        transactionStatus.setStyle(error
                ? "-fx-text-fill: #b91c1c; -fx-font-weight: bold;"
                : "-fx-text-fill: #15803d; -fx-font-weight: bold;");
    }

    private void refreshTransactionHistory(Account account) {
        if (account == null) {
            transactionTable.setItems(FXCollections.observableArrayList());
            return;
        }
        transactionTable.setItems(FXCollections.observableArrayList(account.getTransactions()));
    }

    private void refreshSummary() {
        int accounts = 0;
        int transactions = 0;
        double balance = 0;
        for (Customer customer : customers) {
            for (Account account : customer.getAccounts()) {
                accounts++;
                balance += account.getBalance();
                transactions += account.getTransactions().size();
            }
        }
        customerTotal.setText(Integer.toString(customers.size()));
        accountTotal.setText(Integer.toString(accounts));
        balanceTotal.setText(formatCurrency(balance));
        transactionTotal.setText(Integer.toString(transactions));
    }

    private void refreshAccountRows() {
        List<AccountRow> rows = new ArrayList<>();
        for (Customer customer : customers) {
            for (Account account : customer.getAccounts()) {
                rows.add(new AccountRow(customer, account));
            }
        }
        accountRows.setAll(rows);
    }

    private javafx.scene.control.ListCell<Account> createAccountCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : accountSelector.getConverter().toString(item));
            }
        };
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #142b4a;");
        return label;
    }

    private String formatCurrency(double amount) {
        return CURRENCY + String.format(java.util.Locale.US, "%,.2f", amount);
    }

    private static final class AccountRow {
        private final Customer customer;
        private final Account account;

        private AccountRow(Customer customer, Account account) {
            this.customer = customer;
            this.account = account;
        }

        public String getCustomerId() {
            return customer.getCustomerId();
        }

        public String getAccountNumber() {
            return account.getAccountNumber();
        }

        public String getType() {
            return account.getClass().getSimpleName();
        }

        public String getBranch() {
            return account.getBranch();
        }

        public double getBalance() {
            return account.getBalance();
        }
    }
    private VBox createCustomerRegistrationForm() {
        Label heading = sectionTitle("Register New Customer");

        TextField idInput = new TextField();
        idInput.setPromptText("Customer ID");

        TextField firstNameInput = new TextField();
        firstNameInput.setPromptText("First name");

        TextField surnameInput = new TextField();
        surnameInput.setPromptText("Surname");

        TextField addressInput = new TextField();
        addressInput.setPromptText("Address");

        TextField contactInput = new TextField();
        contactInput.setPromptText("Contact details");

        Button registerButton = new Button("Register Customer");
        registerButton.setStyle(
                "-fx-background-color: #2563eb; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10 18;"
        );

        Label status = new Label("Enter the customer's details.");
        status.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        form.add(idInput, 0, 0);
        form.add(firstNameInput, 1, 0);
        form.add(surnameInput, 2, 0);
        form.add(addressInput, 0, 1);
        form.add(contactInput, 1, 1);
        form.add(registerButton, 2, 1);

        registerButton.setOnAction(event -> {
            String id = idInput.getText().trim();
            String firstName = firstNameInput.getText().trim();
            String surname = surnameInput.getText().trim();
            String address = addressInput.getText().trim();
            String contact = contactInput.getText().trim();

            if (id.isEmpty() || firstName.isEmpty()
                    || surname.isEmpty() || address.isEmpty()
                    || contact.isEmpty()) {
                status.setText("Please complete all fields.");
                status.setStyle("-fx-text-fill: #b91c1c;");
                return;
            }

            boolean duplicateId = customers.stream()
                    .anyMatch(customer ->
                            customer.getCustomerId().equalsIgnoreCase(id));

            if (duplicateId) {
                status.setText("A customer with this ID already exists.");
                status.setStyle("-fx-text-fill: #b91c1c;");
                return;
            }

            try {
                Customer newCustomer = new Customer(
                        id, firstName, surname, address, contact
                );

                customers.add(newCustomer);
                customerSelector.setItems(customers);

                refreshSummary();
                refreshAccountRows();

                status.setText("Customer registered successfully.");
                status.setStyle("-fx-text-fill: #15803d;");

                idInput.clear();
                firstNameInput.clear();
                surnameInput.clear();
                addressInput.clear();
                contactInput.clear();
            } catch (IllegalArgumentException exception) {
                status.setText("Could not register customer: "
                        + exception.getMessage());
                status.setStyle("-fx-text-fill: #b91c1c;");
            }
        });

        VBox formContainer = new VBox(12, heading, form, status);
        formContainer.setPadding(new Insets(16));
        formContainer.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 10;"
        );

        return formContainer;
    }
    public static void main(String[] args) {
        launch(args);
    }
}
