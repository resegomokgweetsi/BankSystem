# BankSystem

## Run the JavaFX banking dashboard

Requires JDK 25 or newer and Maven. From the project directory, run:

```sh
mvn javafx:run
```

The desktop dashboard loads the customers and accounts from `SampleData`. It
includes customer and account views, account balances, transaction history,
and deposit/withdraw actions.

Run this command in a desktop terminal on your ProBook to open the JavaFX
window. A headless Codespace has no desktop display, so it can compile the app
but cannot show the window.

## Run the persistent browser application

From the project directory, start the Java server:

```sh
mvn exec:java
```

Open `http://localhost:4173` (or the forwarded port 4173 in a Codespace). On
first start, create your owner username and password. Passwords must be at
least 12 characters; the server stores a salted password hash, not the
password. Click the BankSystem logo to open the sign-in/sign-out dialog.

Customer, account, and transaction changes are stored in the local
`BankSystem.db` SQLite file. A new database starts with fictional sample
customers and accounts. Existing databases are preserved. New customers must
have Male or Female selected. Do not commit or share the database file.

This is a local project application, not a production banking system. Do not
use it to store live customer financial data without a security and compliance
review.