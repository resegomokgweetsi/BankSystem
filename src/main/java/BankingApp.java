import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;

public class BankingApp extends Application {

    private static final String CURRENCY = "BWP ";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ObservableList<Customer> customers =
            FXCollections.observableArrayList();
    private final ObservableList<AccountRow> accountRows =
            FXCollections.observableArrayList();

    private final Label customerTotal = new Label();
    private final Label accountTotal = new Label();
    private final Label balanceTotal = new Label();
    private final Label transactionTotal = new Label();
    private final Label pageTitle = new Label("Dashboard");
    private final ComboBox<Customer> customerSelector = new ComboBox<>();
    private final ComboBox<Account> accountSelector = new ComboBox<>();
    private final ComboBox<String> historyCustomerFilter = new ComboBox<>();
    private final ComboBox<String> transactionTypeSelector = new ComboBox<>();
    private final TextField amountInput = new TextField("100.00");
    private final Label transactionStatus = new Label("Ready.");
    private final TableView<TransactionRow> transactionTable = new TableView<>();
    private final BankDatabase database = new BankDatabase(
            Path.of(System.getProperty("banksystem.db", "BankSystem.db")).toAbsolutePath());
    private final TextField globalSearch = new TextField();
    private final ComboBox<String> customerGrouping = new ComboBox<>();
    private final TableView<AccountRow> accountTable = new TableView<>(accountRows);
    private TableView<Customer> customerTable;
    private final TableView<Transaction> recentTable = new TableView<>();
    private final List<String> pageNames = List.of("Dashboard", "Customers", "Accounts", "Transactions");
    private final List<Button> navigationButtons = new ArrayList<>();
    private final List<VBox> pages = new ArrayList<>();
    private StackPane pageContainer;
    private Stage mainStage;
    private boolean authenticated;
    private String username = "";
    private Button avatarButton;
    private Stage authStage;

    @Override
    public void start(Stage stage) {
        mainStage = stage;
        stage.initStyle(StageStyle.DECORATED);
        try {
            database.initialize();
            loadCustomersFromDatabase();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not open the BankSystem database.", exception);
        }
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f7fb;");
        root.setLeft(createSidebar());

        VBox header = new VBox(6);
        header.setPadding(new Insets(26, 42, 18, 42));
        Label date = new Label(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        date.setStyle("-fx-text-fill: #78879b; -fx-font-size: 11px; -fx-font-weight: bold;");
        pageTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #17243b;");
        globalSearch.setPromptText(
                "Search customers by ID, phone, surname, address or account type");
        globalSearch.setPrefWidth(350);
        globalSearch.setStyle("-fx-background-color: white; -fx-background-radius: 9; "
                + "-fx-border-color: #e1e7ef; -fx-border-radius: 9; -fx-padding: 10 13; "
                + "-fx-font-size: 12px;");
        avatarButton = new Button("BS");
        avatarButton.setMinSize(38, 38);
        avatarButton.setMaxSize(38, 38);
        avatarButton.setStyle("-fx-background-color: #dceaff; -fx-background-radius: 20; "
                + "-fx-text-fill: #2e5b9e; -fx-font-size: 12px; -fx-font-weight: bold;");
        avatarButton.setOnAction(event -> showAuthDialog(stage));
        HBox actions = new HBox(12, globalSearch, avatarButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox heading = new HBox();
        VBox titleBox = new VBox(6, date, pageTitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        heading.getChildren().addAll(titleBox, actions);
        heading.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(heading);
        root.setTop(header);

        pages.addAll(List.of(
                createDashboard(), createCustomersView(), createAccountsView(),
                createTransactionsView()));
        pageContainer = new StackPane();
        pageContainer.getChildren().addAll(pages);
        showPage(0);
        BorderPane.setMargin(pageContainer, new Insets(0, 42, 36, 42));
        root.setCenter(pageContainer);
        globalSearch.textProperty().addListener((observable, oldValue, value) -> applyGlobalSearch());
        refreshSummary();
        refreshAccountRows();

        stage.setTitle("Bank System Dashboard");
        double appWidth = Math.min(1440, Screen.getPrimary().getVisualBounds().getWidth() * 0.92);
        double appHeight = Math.min(900, Screen.getPrimary().getVisualBounds().getHeight() * 0.90);
        stage.setMinWidth(Math.min(1050, appWidth));
        stage.setMinHeight(Math.min(650, appHeight));
        stage.setScene(new Scene(root, appWidth, appHeight));
        stage.centerOnScreen();
        stage.show();
        Platform.runLater(() -> showAuthDialog(stage));
    }

    private VBox createSidebar() {
        Label mark = new Label("B");
        mark.setAlignment(Pos.CENTER);
        mark.setMinSize(38, 38);
        mark.setStyle("-fx-background-color: #3978e8; -fx-background-radius: 12; "
                + "-fx-text-fill: white; -fx-font-size: 19px; -fx-font-weight: bold;");
        Label brandName = new Label("BankSystem");
        brandName.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        Button brand = new Button();
        brand.setGraphic(new HBox(12, mark, brandName));
        brand.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0 10 0 10;");
        brand.setOnAction(event -> showAuthDialog(mainStage));

        Label workspace = new Label("W O R K S P A C E");
        workspace.setStyle("-fx-text-fill: #8296b4; -fx-font-size: 10px; -fx-font-weight: bold; "
                + "-fx-padding: 0 0 6 11;");
        VBox nav = new VBox(6);
        String[] icons = {"⌂", "♙", "▤", "↔"};
        for (int i = 0; i < pageNames.size(); i++) {
            final int index = i;
            Label icon = new Label(icons[i]);
            icon.setMinWidth(18);
            icon.setStyle("-fx-text-fill: #95afd1; -fx-font-size: 16px;");
            Label text = new Label(pageNames.get(i));
            text.setStyle("-fx-text-fill: #adbed5; -fx-font-size: 13px; -fx-font-weight: bold;");
            Button button = new Button();
            button.setGraphic(new HBox(13, icon, text));
            button.setAlignment(Pos.CENTER_LEFT);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setMinHeight(42);
            button.setStyle("-fx-background-color: transparent; -fx-background-radius: 9; "
                    + "-fx-padding: 0 12; -fx-cursor: hand;");
            button.setOnAction(event -> showPage(index));
            navigationButtons.add(button);
            nav.getChildren().add(button);
        }
        Label footer = new Label("Banking operations\nGaborone, Botswana");
        footer.setStyle("-fx-text-fill: #8296b4; -fx-font-size: 11px; -fx-line-spacing: 5; "
                + "-fx-padding: 16 11 0 11; -fx-border-color: #293d59 transparent transparent transparent;");
        VBox sidebar = new VBox(25, brand, workspace, nav);
        sidebar.setPadding(new Insets(27, 18, 20, 18));
        sidebar.getChildren().add(footer);
        VBox.setVgrow(footer, Priority.ALWAYS);
        sidebar.setPrefWidth(246);
        sidebar.setStyle("-fx-background-color: #10233f;");
        return sidebar;
    }

    private void showPage(int index) {
        for (int i = 0; i < pages.size(); i++) {
            pages.get(i).setVisible(i == index);
            pages.get(i).setManaged(i == index);
            Button button = navigationButtons.get(i);
            boolean selected = i == index;
            button.setStyle("-fx-background-color: " + (selected ? "#24466f" : "transparent")
                    + "; -fx-background-radius: 9; -fx-padding: 0 12; -fx-cursor: hand;");
            HBox graphic = (HBox) button.getGraphic();
            ((Label) graphic.getChildren().get(1)).setStyle("-fx-text-fill: "
                    + (selected ? "white" : "#adbed5")
                    + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        }
        pageTitle.setText(pageNames.get(index));
    }

    private void applyGlobalSearch() {
        String query = globalSearch.getText().trim().toLowerCase();
        int selected = pages.indexOf(pages.stream()
                .filter(javafx.scene.Node::isVisible).findFirst().orElse(null));
        if (selected == 2) {
            accountTable.setItems(FXCollections.observableArrayList(accountRows.stream()
                    .filter(row -> accountMatches(row, query)).toList()));
        } else if (selected == 1) {
            applyCustomerSearch(query);
        } else if (!query.isEmpty()) {
            showPage(1);
            applyCustomerSearch(query);
        }
    }

    private boolean accountMatches(AccountRow row, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return List.of(row.getCustomerId(), row.getCustomerName(), row.getAccountNumber(),
                        row.getType(), row.getBranch())
                .stream().anyMatch(value -> value.toLowerCase().contains(query));
    }

    private void applyCustomerSearch(String query) {
        List<Customer> filtered = customers.stream()
                .filter(customer -> query.isEmpty()
                        || customer.getCustomerId().toLowerCase().contains(query)
                        || (customer.getFirstName() + " " + customer.getSurname())
                                .toLowerCase().contains(query)
                        || customer.getAddress().toLowerCase().contains(query)
                        || customer.getContactDetails().toLowerCase().contains(query)
                        || customer.getAccounts().stream().anyMatch(account ->
                                List.of(account.getAccountNumber(), account.getBranch(),
                                                account.getClass().getSimpleName().replace("Account", ""))
                                        .stream().anyMatch(value -> value.toLowerCase().contains(query))))
                .sorted((left, right) -> {
                    String grouping = customerGrouping.getValue();
                    if ("Gender".equals(grouping)) {
                        return left.getGender().compareToIgnoreCase(right.getGender());
                    }
                    if ("Account type".equals(grouping)) {
                        return customerAccountTypes(left).compareToIgnoreCase(customerAccountTypes(right));
                    }
                    if ("Gender and account type".equals(grouping)) {
                        int gender = left.getGender().compareToIgnoreCase(right.getGender());
                        return gender != 0 ? gender
                                : customerAccountTypes(left).compareToIgnoreCase(customerAccountTypes(right));
                    }
                    return left.getCustomerId().compareToIgnoreCase(right.getCustomerId());
                }).toList();
        if (customerTable != null) {
            customerTable.setItems(FXCollections.observableArrayList(filtered));
        }
    }

    private void showAuthDialog(Stage owner) {
        if (authStage != null && authStage.isShowing()) {
            authStage.toFront();
            return;
        }

        boolean setupRequired;
        String databaseError = "";
        try {
            setupRequired = database.isSetupRequired();
        } catch (SQLException exception) {
            setupRequired = false;
            databaseError = "Could not read the local BankSystem database.";
        }

        authStage = new Stage(StageStyle.TRANSPARENT);
        authStage.initOwner(owner);
        authStage.initModality(Modality.APPLICATION_MODAL);
        authStage.setTitle("BankSystem Login");
        authStage.setOnCloseRequest(event -> {
            if (!authenticated) {
                event.consume();
            }
        });

        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: transparent;");
        VBox card = new VBox(0);
        card.setMaxWidth(410);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 17; "
                + "-fx-border-color: #e5ebf3; -fx-border-radius: 17; "
                + "-fx-effect: dropshadow(gaussian, rgba(16,35,63,0.27), 30, 0.25, 0, 12);");
        StackPane.setAlignment(card, Pos.CENTER);

        Label mark = new Label("B");
        mark.setAlignment(Pos.CENTER);
        mark.setMinSize(34, 34);
        mark.setStyle("-fx-background-color: #3978e8; -fx-background-radius: 10; "
                + "-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");
        Label brand = new Label("BankSystem");
        brand.setStyle("-fx-text-fill: #1b2e49; -fx-font-size: 14px; -fx-font-weight: bold;");
        HBox brandRow = new HBox(10, mark, brand);
        brandRow.setAlignment(Pos.CENTER_LEFT);
        brandRow.setPadding(new Insets(0, 0, 24, 0));
        card.getChildren().add(brandRow);

        Label heading = new Label();
        heading.setStyle("-fx-text-fill: #1d2e47; -fx-font-size: 21px; -fx-font-weight: bold;");
        Label copy = new Label();
        copy.setWrapText(true);
        copy.setStyle("-fx-text-fill: #78879b; -fx-font-size: 12px; -fx-padding: 7 0 16 0;");
        Label error = new Label(databaseError);
        error.setWrapText(true);
        error.setMinHeight(20);
        error.setStyle("-fx-text-fill: #c05252; -fx-font-size: 11px;");
        VBox.setMargin(error, new Insets(3, 0, 6, 0));

        if (!databaseError.isEmpty()) {
            heading.setText("BankSystem unavailable");
            copy.setText(databaseError);
            Button retry = authButton("Try again");
            retry.setOnAction(event -> {
                authStage.hide();
                Platform.runLater(() -> showAuthDialog(owner));
            });
            card.getChildren().addAll(heading, copy, retry);
        } else if (authenticated) {
            heading.setText("You’re signed in");
            copy.setText("Your local BankSystem session is active.");
            Label signedIn = new Label("Signed in as  " + username);
            signedIn.setMaxWidth(Double.MAX_VALUE);
            signedIn.setPadding(new Insets(12));
            signedIn.setStyle("-fx-background-color: #f3f7fd; -fx-background-radius: 9; "
                    + "-fx-text-fill: #496381; -fx-font-size: 12px;");
            Button logout = authButton("Log out");
            logout.setOnAction(event -> {
                authenticated = true;
                authStage.close();
                authenticated = false;
                username = "";
                updateAvatar();
                Platform.runLater(() -> showAuthDialog(owner));
            });
            Button close = new Button("×");
            close.setStyle("-fx-background-color: #f2f5f9; -fx-background-radius: 20; "
                    + "-fx-text-fill: #718198; -fx-font-size: 18px; -fx-padding: 2 9;");
            close.setOnAction(event -> authStage.close());
            HBox headingRow = new HBox(10, heading, close);
            headingRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(heading, Priority.ALWAYS);
            card.getChildren().addAll(headingRow, copy, signedIn, error, logout);
        } else {
            boolean createLogin = setupRequired;
            heading.setText(createLogin ? "Create your owner login" : "Log in to BankSystem");
            copy.setText(createLogin
                    ? "Set up the first account for this local BankSystem installation."
                    : "Enter your local account details to continue.");

            TextField usernameInput = new TextField();
            usernameInput.setPromptText("Username");
            styleAuthInput(usernameInput);
            PasswordField passwordInput = new PasswordField();
            passwordInput.setPromptText("Password");
            styleAuthInput(passwordInput);
            VBox usernameField = authField("Username", usernameInput);
            VBox passwordField = authField("Password", passwordInput);
            Button submit = authButton(createLogin ? "Create owner login" : "Log in");
            submit.setDefaultButton(true);
            submit.setMaxWidth(Double.MAX_VALUE);
            submit.setOnAction(event -> {
                String enteredUsername = usernameInput.getText().trim();
                char[] password = passwordInput.getText().toCharArray();
                try {
                    if (createLogin) {
                        if (!enteredUsername.matches("[A-Za-z0-9._-]{3,32}")) {
                            java.util.Arrays.fill(password, '\0');
                            throw new IllegalArgumentException(
                                    "Username must be 3–32 letters, numbers, dots, underscores, or hyphens.");
                        }
                        if (password.length < 12) {
                            java.util.Arrays.fill(password, '\0');
                            throw new IllegalArgumentException(
                                    "Choose a password with at least 12 characters.");
                        }
                        byte[] salt = PasswordHasher.newSalt();
                        byte[] passwordHash = PasswordHasher.hash(password, salt);
                        database.createOwner(enteredUsername, salt, passwordHash);
                    } else {
                        Optional<BankDatabase.UserCredentials> credentials =
                                database.findUser(enteredUsername);
                        boolean matches = credentials.isPresent()
                                && PasswordHasher.matches(password,
                                        credentials.get().salt(), credentials.get().passwordHash());
                        if (credentials.isEmpty()) {
                            PasswordHasher.hash(password, PasswordHasher.newSalt());
                        }
                        if (!matches) {
                            error.setText("Incorrect username or password.");
                            passwordInput.clear();
                            return;
                        }
                        enteredUsername = credentials.orElseThrow().username();
                    }
                    loadCustomersFromDatabase();
                    username = enteredUsername;
                    authenticated = true;
                    updateAvatar();
                    authStage.close();
                } catch (IllegalArgumentException exception) {
                    error.setText(exception.getMessage());
                    passwordInput.clear();
                } catch (IllegalStateException exception) {
                    error.setText("Initial setup was already completed. Please log in.");
                    passwordInput.clear();
                } catch (SQLException exception) {
                    error.setText("The local database could not complete this request.");
                    passwordInput.clear();
                }
            });

            VBox form = new VBox(14, usernameField, passwordField, error, submit);
            card.getChildren().addAll(heading, copy, form);
            Label hint = new Label(createLogin
                    ? "The password is hashed locally; only its salted hash is stored in the local database. Use a unique password."
                    : "Login is checked locally. Your account is stored in the local database.");
            hint.setWrapText(true);
            hint.setPadding(new Insets(10, 12, 10, 12));
            hint.setStyle("-fx-background-color: #f4f7fb; -fx-background-radius: 8; "
                    + "-fx-text-fill: #6f7f94; -fx-font-size: 10px;");
            VBox.setMargin(hint, new Insets(16, 0, 0, 0));
            card.getChildren().add(hint);
            Platform.runLater(usernameInput::requestFocus);
        }

        backdrop.getChildren().add(card);
        double dialogWidth = 480;
        double dialogHeight = 540;
        Scene dialogScene = new Scene(backdrop, dialogWidth, dialogHeight);
        dialogScene.setFill(Color.TRANSPARENT);
        authStage.setScene(dialogScene);
        authStage.setWidth(dialogWidth);
        authStage.setHeight(dialogHeight);
        authStage.setX(owner.getX() + (owner.getWidth() - dialogWidth) / 2);
        authStage.setY(owner.getY() + (owner.getHeight() - dialogHeight) / 2);
        owner.getScene().getRoot().setOpacity(0.68);
        authStage.showAndWait();
        owner.getScene().getRoot().setOpacity(1.0);
    }

    private VBox authField(String title, javafx.scene.Node input) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #687990; -fx-font-size: 10px; -fx-font-weight: bold;");
        return new VBox(6, label, input);
    }

    private void styleAuthInput(TextField input) {
        input.setMaxWidth(Double.MAX_VALUE);
        input.setPrefHeight(40);
        input.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-border-color: #dfe6ef; -fx-border-radius: 8; "
                + "-fx-text-fill: #283b55; -fx-font-size: 12px; -fx-padding: 8 10;");
    }

    private Button authButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(40);
        button.setStyle("-fx-background-color: #3478df; -fx-background-radius: 8; "
                + "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; "
                + "-fx-padding: 0 14;");
        return button;
    }

    private void updateAvatar() {
        if (avatarButton == null) {
            return;
        }
        avatarButton.setText(authenticated && !username.isBlank()
                ? username.substring(0, Math.min(2, username.length())).toUpperCase()
                : "BS");
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
        recentTable.setPlaceholder(new Label("No recent activity."));
        recentTable.getColumns().setAll(
                transactionColumn("Reference", transaction -> transaction.getTransactionId(), 120),
                transactionColumn("Type", transaction -> transaction.getType().toString(), 120),
                transactionColumn("Amount", transaction -> formatCurrency(transaction.getAmount()), 140),
                transactionColumn("Date", transaction -> transaction.getDate().format(DATE_FORMAT), 180));
        refreshRecentTransactions();
        recentTable.setPrefHeight(220);
        VBox content = new VBox(18, cards, sectionTitle("Registered Customers"), table,
                sectionTitle("Recent Activity"), recentTable);
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
        customerTable = createCustomerTable();
        customerGrouping.getItems().setAll(
                "No grouping", "Gender", "Account type", "Gender and account type");
        customerGrouping.getSelectionModel().selectFirst();
        HBox customerTools = new HBox(12, formField("Group results", customerGrouping));
        customerTools.setAlignment(Pos.BOTTOM_LEFT);
        customerGrouping.valueProperty().addListener(
                (observable, oldValue, value) -> applyCustomerSearch(globalSearch.getText().trim().toLowerCase()));
        customerTable.setRowFactory(view -> {
            javafx.scene.control.TableRow<Customer> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()
                        && event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                        && event.getClickCount() == 1) {
                    Customer selected = row.getItem();
                    historyCustomerFilter.setValue(customerLabel(selected));
                    showCustomerProfile(selected);
                }
            });
            return row;
        });

        VBox.setVgrow(customerTable, Priority.ALWAYS);
        applyCustomerSearch(globalSearch.getText().trim().toLowerCase());
        Label profileHint = new Label(
                "Select a customer row to open their complete profile, accounts, and transaction history.");
        profileHint.setStyle("-fx-text-fill: #78879b; -fx-font-size: 11px;");
        VBox content = new VBox(14, createCustomerRegistrationForm(), customerTools,
                sectionTitle("Customer Directory"), profileHint, customerTable);
        content.setPadding(new Insets(18, 8, 8, 8));
        return content;
    }

    private void showCustomerProfile(Customer customer) {
        Stage profileStage = new Stage();
        profileStage.initOwner(mainStage);
        profileStage.initModality(Modality.WINDOW_MODAL);
        profileStage.setTitle("Customer Profile - "
                + customer.getFirstName() + " " + customer.getSurname());

        Label name = new Label(customer.getFirstName() + " " + customer.getSurname());
        name.setStyle("-fx-text-fill: #17243b; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label subtitle = new Label("Customer ID: " + customer.getCustomerId());
        subtitle.setStyle("-fx-text-fill: #78879b; -fx-font-size: 12px;");
        VBox header = new VBox(5, name, subtitle);
        Button closeProfile = new Button("×");
        closeProfile.setAccessibleText("Close customer profile");
        closeProfile.setMinSize(36, 36);
        closeProfile.setStyle("-fx-background-color: #f2f5f9; -fx-background-radius: 18; "
                + "-fx-text-fill: #718198; -fx-font-size: 20px; -fx-cursor: hand;");
        closeProfile.setOnAction(event -> profileStage.close());
        HBox profileHeader = new HBox(header, closeProfile);
        profileHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header, Priority.ALWAYS);
        profileHeader.setPadding(new Insets(24, 28, 16, 28));

        GridPane personalDetails = new GridPane();
        personalDetails.setHgap(20);
        personalDetails.setVgap(16);
        personalDetails.add(profileField("First name", customer.getFirstName()), 0, 0);
        personalDetails.add(profileField("Surname", customer.getSurname()), 1, 0);
        personalDetails.add(profileField("Gender", customer.getGender()), 2, 0);
        personalDetails.add(profileField("Contact number", customer.getContactDetails()), 0, 1);
        personalDetails.add(profileField("Address / town", customer.getAddress()), 1, 1, 2, 1);
        for (int column = 0; column < 3; column++) {
            GridPane.setHgrow(personalDetails.getChildren().get(column), Priority.ALWAYS);
        }
        GridPane.setHgrow(personalDetails.getChildren().get(4), Priority.ALWAYS);

        Label employment = new Label(
                "Employment details are not recorded for this customer in the current system.");
        employment.setWrapText(true);
        employment.setStyle("-fx-text-fill: #6f7f94; -fx-font-size: 12px;");

        List<AccountRow> customerAccounts = customer.getAccounts().stream()
                .map(account -> new AccountRow(customer, account)).toList();
        TableView<AccountRow> accounts = new TableView<>(
                FXCollections.observableArrayList(customerAccounts));
        accounts.setPlaceholder(new Label("No accounts assigned to this customer."));
        accounts.getColumns().setAll(
                accountColumn("Account number", AccountRow::getAccountNumber, 130),
                accountColumn("Type", AccountRow::getType, 100),
                accountColumn("Branch", AccountRow::getBranch, 130),
                accountColumn("Balance", row -> formatCurrency(row.getBalance()), 130),
                accountColumn("Company", AccountRow::getCompanyName, 160),
                accountColumn("Company address", AccountRow::getCompanyAddress, 180));
        accounts.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        accounts.setPrefHeight(Math.max(100, Math.min(230, 44 + customerAccounts.size() * 34)));

        List<TransactionRow> customerTransactions = new ArrayList<>();
        for (Account account : customer.getAccounts()) {
            for (Transaction transaction : account.getTransactions()) {
                customerTransactions.add(new TransactionRow(customer, account, transaction));
            }
        }
        customerTransactions.sort(Comparator.comparing(TransactionRow::transactionDate)
                .thenComparing(TransactionRow::getTransactionId).reversed());
        TableView<TransactionRow> transactions = new TableView<>(
                FXCollections.observableArrayList(customerTransactions));
        transactions.setPlaceholder(new Label("No transactions recorded for this customer."));
        transactions.getColumns().setAll(
                historyColumn("Reference", TransactionRow::getTransactionId, 120),
                historyColumn("Type", TransactionRow::getType, 110),
                historyColumn("Account", TransactionRow::getAccountNumber, 130),
                historyColumn("Amount", row -> (row.isDeposit() ? "+" : "−")
                        + formatCurrency(row.getAmount()), 150),
                historyColumn("Date", TransactionRow::getDate, 180));
        transactions.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        transactions.setPrefHeight(Math.max(120, Math.min(300, 44 + customerTransactions.size() * 34)));

        double totalBalance = customer.getAccounts().stream()
                .mapToDouble(Account::getBalance).sum();
        HBox summary = new HBox(12,
                profileStat("Accounts", Integer.toString(customerAccounts.size())),
                profileStat("Transactions", Integer.toString(customerTransactions.size())),
                profileStat("Total balance", formatCurrency(totalBalance)));
        for (javafx.scene.Node item : summary.getChildren()) {
            HBox.setHgrow(item, Priority.ALWAYS);
        }

        VBox content = new VBox(16,
                summary,
                profileSection("Personal details", personalDetails),
                profileSection("Employment details", employment),
                profileSection("Accounts", accounts),
                profileSection("Transaction history", transactions));
        content.setPadding(new Insets(8, 28, 28, 28));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #f4f7fb; -fx-background-color: #f4f7fb;");

        BorderPane root = new BorderPane(scroll, profileHeader, null, null, null);
        root.setStyle("-fx-background-color: #f4f7fb;");
        profileStage.setScene(new Scene(root, 1050, 760));
        profileStage.setMinWidth(850);
        profileStage.setMinHeight(620);
        profileStage.centerOnScreen();
        profileStage.show();
    }

    private VBox profileSection(String title, javafx.scene.Node content) {
        Label heading = sectionTitle(title);
        VBox section = new VBox(14, heading, content);
        section.setPadding(new Insets(18));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #e8edf3; -fx-border-radius: 12;");
        return section;
    }

    private VBox profileField(String title, String value) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #78879b; -fx-font-size: 11px;");
        Label text = new Label(value == null || value.isBlank() ? "Not recorded" : value);
        text.setWrapText(true);
        text.setStyle("-fx-text-fill: #253650; -fx-font-size: 13px; -fx-font-weight: bold;");
        return new VBox(5, label, text);
    }

    private VBox profileStat(String title, String value) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #75849a; -fx-font-size: 11px;");
        Label amount = new Label(value);
        amount.setStyle("-fx-text-fill: #192a44; -fx-font-size: 18px; -fx-font-weight: bold;");
        VBox stat = new VBox(8, label, amount);
        stat.setPadding(new Insets(14, 16, 14, 16));
        stat.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: #e8edf3; -fx-border-radius: 10;");
        return stat;
    }

    private TableView<Customer> createCustomerTable() {
        TableView<Customer> table = new TableView<>(customers);
        table.setPlaceholder(new Label("No customers found."));
        table.getColumns().setAll(
                customerColumn("Customer ID", "customerId", 120),
                customerColumn("First Name", "firstName", 150),
                customerColumn("Surname", "surname", 160),
                customerColumn("Gender", "gender", 100),
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
        accountTable.setPlaceholder(new Label("No accounts found."));
        accountTable.getColumns().setAll(
                accountColumn("Customer ID", AccountRow::getCustomerId, 120),
                accountColumn("Account Number", AccountRow::getAccountNumber, 160),
                accountColumn("Type", AccountRow::getType, 180),
                accountColumn("Branch", AccountRow::getBranch, 170),
                accountColumn("Balance", row -> formatCurrency(row.getBalance()), 160)
        );
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(accountTable, Priority.ALWAYS);

        ComboBox<Customer> customerInput = new ComboBox<>(customers);
        customerInput.setPromptText("Choose customer");
        customerInput.setConverter(new StringConverter<>() {
            @Override public String toString(Customer customer) {
                return customer == null ? "" : customer.getCustomerId() + " - "
                        + customer.getFirstName() + " " + customer.getSurname();
            }
            @Override public Customer fromString(String value) { return null; }
        });
        customerInput.setMaxWidth(Double.MAX_VALUE);

        TextField numberInput = new TextField();
        numberInput.setPromptText("e.g. SA003");
        TextField branchInput = new TextField();
        branchInput.setPromptText("e.g. Gaborone");
        ComboBox<String> typeInput = new ComboBox<>();
        typeInput.getItems().setAll("Savings", "Investment", "Cheque");
        typeInput.getSelectionModel().selectFirst();
        typeInput.setMaxWidth(Double.MAX_VALUE);
        TextField balanceInput = new TextField("0.00");
        TextField companyNameInput = new TextField();
        companyNameInput.setPromptText("Company name");
        TextField companyAddressInput = new TextField();
        companyAddressInput.setPromptText("Company address");
        Label status = new Label("Assign a new account to a customer.");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(formField("Customer", customerInput), 0, 0);
        form.add(formField("Account number", numberInput), 1, 0);
        form.add(formField("Account type", typeInput), 2, 0);
        form.add(formField("Branch", branchInput), 3, 0);
        form.add(formField("Initial balance", balanceInput), 0, 1);
        form.add(formField("Company name", companyNameInput), 1, 1);
        form.add(formField("Company address", companyAddressInput), 2, 1);

        Button createButton = new Button("Create Account");
        createButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;");
        createButton.setOnAction(event -> {
            try {
                Customer customer = customerInput.getValue();
                String number = numberInput.getText().trim();
                String type = typeInput.getValue();
                String branch = branchInput.getText().trim();
                String balanceText = balanceInput.getText().trim();
                BigDecimal balance = new BigDecimal(balanceText);
                if (customer == null || number.isEmpty() || branch.isEmpty()) {
                    throw new IllegalArgumentException("Choose a customer and complete the account fields.");
                }
                if (balance.signum() < 0 || balance.stripTrailingZeros().scale() > 2) {
                    throw new IllegalArgumentException("Enter a valid initial balance.");
                }
                long cents = balance.movePointRight(2).longValueExact();
                if ("Investment".equals(type) && cents < 50000) {
                    throw new IllegalArgumentException("Investment accounts require at least BWP 500.00.");
                }
                if ("Cheque".equals(type)
                        && (companyNameInput.getText().trim().isEmpty()
                        || companyAddressInput.getText().trim().isEmpty())) {
                    throw new IllegalArgumentException("Company name and address are required for cheque accounts.");
                }
                if (accountRows.stream().anyMatch(row ->
                        row.getAccountNumber().equalsIgnoreCase(number))) {
                    throw new IllegalArgumentException("That account number is already in use.");
                }
                database.addAccount(number, customer.getCustomerId(), type, branch, cents,
                        "Cheque".equals(type) ? companyNameInput.getText().trim() : null,
                        "Cheque".equals(type) ? companyAddressInput.getText().trim() : null);
                loadCustomersFromDatabase();
                numberInput.clear();
                branchInput.clear();
                balanceInput.setText("0.00");
                companyNameInput.clear();
                companyAddressInput.clear();
                status.setText(type + " account created successfully.");
                status.setStyle("-fx-text-fill: #15803d;");
            } catch (IllegalArgumentException exception) {
                status.setText(exception.getMessage());
                status.setStyle("-fx-text-fill: #b91c1c;");
            } catch (SQLException exception) {
                status.setText("Could not save the account to the local database.");
                status.setStyle("-fx-text-fill: #b91c1c;");
            }
        });
        form.add(createButton, 3, 1);

        HBox balanceSummary = new HBox(12);
        for (String type : List.of("Savings", "Investment", "Cheque")) {
            double total = accountRows.stream()
                    .filter(row -> row.getType().equals(type))
                    .mapToDouble(AccountRow::getBalance).sum();
            Label label = new Label(type + ": " + formatCurrency(total));
            label.setStyle("-fx-padding: 12; -fx-background-color: white; "
                    + "-fx-border-color: #e2e8f0; -fx-background-radius: 8;");
            HBox.setHgrow(label, Priority.ALWAYS);
            balanceSummary.getChildren().add(label);
        }

        VBox content = new VBox(14, sectionTitle("Create Account"), form, status,
                sectionTitle("Balances by Account Type"), balanceSummary,
                sectionTitle("Accounts"), accountTable);
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

        historyCustomerFilter.getItems().setAll("All customers");
        for (Customer customer : customers) {
            historyCustomerFilter.getItems().add(customerLabel(customer));
        }
        historyCustomerFilter.getSelectionModel().selectFirst();
        historyCustomerFilter.setMaxWidth(Double.MAX_VALUE);
        historyCustomerFilter.valueProperty().addListener(
                (observable, oldValue, value) -> refreshTransactionHistory());

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
            } else {
                accountSelector.getSelectionModel().selectFirst();
            }
        });
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
        transactionTable.setPlaceholder(new Label("No transactions recorded."));
        transactionTable.getColumns().setAll(
                historyColumn("Reference", TransactionRow::getTransactionId, 120),
                historyColumn("Type", TransactionRow::getType, 120),
                historyColumn("Account", TransactionRow::getAccountNumber, 130),
                historyColumn("Customer", TransactionRow::getCustomerName, 190),
                historyColumn("Amount", row -> (row.isDeposit() ? "+" : "−")
                        + formatCurrency(row.getAmount()), 150),
                historyColumn("Date", TransactionRow::getDate, 180)
        );
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(transactionTable, Priority.ALWAYS);

        HBox historyControls = new HBox(
                formField("Show transactions for", historyCustomerFilter));
        historyControls.setAlignment(Pos.CENTER_LEFT);
        refreshTransactionHistory();
        VBox content = new VBox(16, sectionTitle("Quick Actions"), form, transactionStatus,
                sectionTitle("Transaction History"), historyControls, transactionTable);
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

    private TableColumn<TransactionRow, String> historyColumn(
            String title, java.util.function.Function<TransactionRow, String> value, double width) {
        TableColumn<TransactionRow, String> column = new TableColumn<>(title);
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
                throw new IllegalArgumentException("Insufficient funds. Available balance: "
                        + formatCurrency(account.getBalance()) + ".");
            }

            double value = amount.doubleValue();
            BankDatabase.TransactionOutcome outcome = database.transact(
                    account.getAccountNumber(), type, amount.movePointRight(2).longValueExact());
            if (outcome.status() == BankDatabase.TransactionStatus.ACCOUNT_NOT_FOUND) {
                throw new IllegalArgumentException("Account not found.");
            }
            if (outcome.status() == BankDatabase.TransactionStatus.INSUFFICIENT_FUNDS) {
                throw new IllegalArgumentException("Insufficient funds. Available balance: "
                        + formatCurrency(outcome.balanceCents() / 100.0) + ".");
            }
            loadCustomersFromDatabase();
            Customer refreshedCustomer = customers.stream()
                    .filter(item -> item.getCustomerId().equalsIgnoreCase(customer.getCustomerId()))
                    .findFirst().orElse(null);
            account = findAccount(account.getAccountNumber());
            if (account == null) {
                throw new IllegalArgumentException("The updated account could not be loaded.");
            }

            amountInput.clear();
            if (refreshedCustomer != null) {
                customerSelector.getSelectionModel().select(refreshedCustomer);
                accountSelector.setItems(
                        FXCollections.observableArrayList(refreshedCustomer.getAccounts()));
            }
            accountSelector.getSelectionModel().select(account);
            refreshTransactionHistory();
            showTransactionStatus(type + " successful. New balance: "
                    + formatCurrency(account.getBalance()), false);
        } catch (NumberFormatException exception) {
            showTransactionStatus("Enter a valid amount, for example 100.00.", true);
        } catch (IllegalArgumentException exception) {
            showTransactionStatus(exception.getMessage(), true);
        } catch (SQLException exception) {
            showTransactionStatus("The database could not save this transaction.", true);
        }
    }

    private Account findAccount(String accountNumber) {
        for (Customer customer : customers) {
            for (Account account : customer.getAccounts()) {
                if (account.getAccountNumber().equalsIgnoreCase(accountNumber)) {
                    return account;
                }
            }
        }
        return null;
    }

    private void showTransactionStatus(String message, boolean error) {
        transactionStatus.setText(message);
        transactionStatus.setStyle(error
                ? "-fx-text-fill: #b91c1c; -fx-font-weight: bold;"
                : "-fx-text-fill: #15803d; -fx-font-weight: bold;");
    }

    private void refreshTransactionHistory() {
        String selection = historyCustomerFilter.getValue();
        Customer selectedCustomer = customers.stream()
                .filter(customer -> customerLabel(customer).equals(selection))
                .findFirst().orElse(null);
        List<TransactionRow> rows = new ArrayList<>();
        for (Customer customer : customers) {
            if (selectedCustomer != null && customer != selectedCustomer) {
                continue;
            }
            for (Account account : customer.getAccounts()) {
                for (Transaction transaction : account.getTransactions()) {
                    rows.add(new TransactionRow(customer, account, transaction));
                }
            }
        }
        rows.sort(Comparator.comparing(TransactionRow::transactionDate)
                .thenComparing(TransactionRow::getTransactionId).reversed());
        transactionTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void loadCustomersFromDatabase() throws SQLException {
        String selectedHistoryCustomer = historyCustomerFilter.getValue();
        customers.clear();
        for (BankDatabase.CustomerData data : database.getCustomers()) {
            Customer customer = new Customer(
                    data.id(), data.firstName(), data.surname(),
                    data.gender(), data.address(), data.contact());
            for (BankDatabase.AccountData accountData : data.accounts()) {
                Account account;
                switch (accountData.type()) {
                    case "Investment" ->
                        account = new InvestmentAccount(
                                accountData.number(), accountData.balance(), accountData.branch());
                    case "Cheque" ->
                        account = new ChequeAccount(
                                accountData.number(), accountData.balance(), accountData.branch(),
                                accountData.companyName(), accountData.companyAddress());
                    default ->
                        account = new SavingsAccount(
                                accountData.number(), accountData.balance(), accountData.branch());
                }
                for (BankDatabase.TransactionData transactionData : accountData.transactions()) {
                    TransactionType type = "Deposit".equals(transactionData.type())
                            ? TransactionType.DEPOSIT : TransactionType.WITHDRAWAL;
                    account.getTransactions().add(
                            new Transaction(transactionData.id(), type, transactionData.amount(),
                                    Instant.parse(transactionData.timestamp())
                                            .atZone(ZoneId.systemDefault()).toLocalDateTime()));
                }
                customer.addAccount(account);
            }
            customers.add(customer);
        }
        if (customerSelector != null) {
            customerSelector.setItems(customers);
        }
        historyCustomerFilter.getItems().setAll("All customers");
        for (Customer customer : customers) {
            historyCustomerFilter.getItems().add(customerLabel(customer));
        }
        if (selectedHistoryCustomer != null
                && historyCustomerFilter.getItems().contains(selectedHistoryCustomer)) {
            historyCustomerFilter.setValue(selectedHistoryCustomer);
        } else {
            historyCustomerFilter.setValue("All customers");
        }
        refreshTransactionHistory();
        refreshSummary();
        refreshAccountRows();
    }

    private String customerAccountTypes(Customer customer) {
        return customer.getAccounts().stream()
                .map(account -> account.getClass().getSimpleName())
                .distinct()
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("No accounts");
    }

    private String customerLabel(Customer customer) {
        return customer.getCustomerId() + " - "
                + customer.getFirstName() + " " + customer.getSurname();
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
        refreshRecentTransactions();
    }

    private void refreshRecentTransactions() {
        List<Transaction> recentTransactions = new ArrayList<>();
        for (Customer customer : customers) {
            for (Account account : customer.getAccounts()) {
                recentTransactions.addAll(account.getTransactions());
            }
        }
        recentTransactions.sort((left, right) ->
                right.getTransactionId().compareToIgnoreCase(left.getTransactionId()));
        recentTable.setItems(FXCollections.observableArrayList(
                recentTransactions.stream().limit(10).toList()));
    }

    private void refreshAccountRows() {
        List<AccountRow> rows = new ArrayList<>();
        for (Customer customer : customers) {
            for (Account account : customer.getAccounts()) {
                rows.add(new AccountRow(customer, account));
            }
        }
        accountRows.setAll(rows);
        accountTable.setItems(FXCollections.observableArrayList(rows.stream()
                .filter(row -> accountMatches(row, globalSearch.getText().trim().toLowerCase()))
                .toList()));
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

        public String getCustomerName() {
            return customer.getFirstName() + " " + customer.getSurname();
        }

        public String getAccountNumber() {
            return account.getAccountNumber();
        }

        public String getType() {
            String className = account.getClass().getSimpleName();
            return className.endsWith("Account")
                    ? className.substring(0, className.length() - "Account".length())
                    : className;
        }

        public String getBranch() {
            return account.getBranch();
        }

        public String getCompanyName() {
            return account instanceof ChequeAccount cheque ? cheque.getCompanyName() : "—";
        }

        public String getCompanyAddress() {
            return account instanceof ChequeAccount cheque ? cheque.getCompanyAddress() : "—";
        }

        public double getBalance() {
            return account.getBalance();
        }
    }

    private static final class TransactionRow {
        private final Customer customer;
        private final Account account;
        private final Transaction transaction;

        private TransactionRow(Customer customer, Account account, Transaction transaction) {
            this.customer = customer;
            this.account = account;
            this.transaction = transaction;
        }

        public String getTransactionId() {
            return transaction.getTransactionId();
        }

        public String getType() {
            return transaction.getType() == TransactionType.DEPOSIT ? "Deposit" : "Withdraw";
        }

        public String getAccountNumber() {
            return account.getAccountNumber();
        }

        public String getCustomerName() {
            return customer.getFirstName() + " " + customer.getSurname();
        }

        public double getAmount() {
            return transaction.getAmount();
        }

        public boolean isDeposit() {
            return transaction.getType() == TransactionType.DEPOSIT;
        }

        public String getDate() {
            return transaction.getDate().format(DATE_FORMAT);
        }

        private java.time.LocalDateTime transactionDate() {
            return transaction.getDate();
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

        ComboBox<String> genderInput = new ComboBox<>();
        genderInput.getItems().setAll("Male", "Female");
        genderInput.setPromptText("Gender");
        genderInput.setMaxWidth(Double.MAX_VALUE);

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
        form.add(genderInput, 0, 1);
        form.add(addressInput, 1, 1);
        form.add(contactInput, 2, 1);
        form.add(registerButton, 0, 2);

        registerButton.setOnAction(event -> {
            String id = idInput.getText().trim();
            String firstName = firstNameInput.getText().trim();
            String surname = surnameInput.getText().trim();
            String gender = genderInput.getValue();
            String address = addressInput.getText().trim();
            String contact = contactInput.getText().trim();

            if (id.isEmpty() || firstName.isEmpty()
                    || surname.isEmpty() || gender == null || address.isEmpty()
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
                database.addCustomer(id, firstName, surname, gender, address, contact);
                loadCustomersFromDatabase();

                status.setText("Customer registered successfully.");
                status.setStyle("-fx-text-fill: #15803d;");

                idInput.clear();
                firstNameInput.clear();
                surnameInput.clear();
                addressInput.clear();
                contactInput.clear();
                genderInput.getSelectionModel().clearSelection();
            } catch (IllegalArgumentException exception) {
                status.setText("Could not register customer: "
                        + exception.getMessage());
                status.setStyle("-fx-text-fill: #b91c1c;");
            } catch (SQLException exception) {
                status.setText("Could not save customer to the local database.");
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
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("No display detected. Running the console banking demo instead.");
            Main.main(args);
            return;
        }
        launch(args);
    }
}
