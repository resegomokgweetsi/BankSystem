# BankSystem

## Run the JavaFX banking dashboard

Requires JDK 25 or newer and Maven. From the project directory, run:

```sh
mvn javafx:run
```

The desktop dashboard opens as a JavaFX window with customer search, account
management, balances, transaction history, and deposit/withdraw actions. On
first start, create the owner login; subsequent starts require that login.
Credentials are checked locally and only a salted password hash is stored.
Customer, account, and transaction changes use the local `BankSystem.db`
SQLite database, shared with the optional browser application.
The Transactions view shows all recorded transactions newest first, with an
option to filter the history to one customer across all of their accounts.
Selecting a customer opens a profile with their contact details, accounts,
balances, and complete transaction history. Employment details are marked as
not recorded because the current customer records do not store them.

Run this command in a desktop terminal on your ProBook to open the JavaFX
window. A headless Codespace has no desktop display, so it can compile the app
but cannot show the window.

## Run the desktop GUI

From the project directory, run:

```sh
mvn exec:java
```

This opens the JavaFX banking dashboard as a desktop window.

## Run the optional browser application

From the project directory, start the Java server explicitly:

```sh
mvn exec:java -Dexec.mainClass=LocalBankServer
```

Customer, account, and transaction changes are stored in the local
`BankSystem.db` SQLite file. A new database starts with fictional sample
customers and accounts. Existing databases are preserved. New customers must
have Male or Female selected. Do not commit or share the database file.

This is a local project application, not a production banking system. Do not
use it to store live customer financial data without a security and compliance
review.