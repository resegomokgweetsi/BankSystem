# BankSystem

This is the public source repository for the BankSystem desktop application:
[github.com/resegomokgweetsi/BankSystem](https://github.com/resegomokgweetsi/BankSystem).

Clone it with:

```sh
git clone https://github.com/resegomokgweetsi/BankSystem.git
cd BankSystem
```

## Run the desktop GUI

Install [JDK 25 or newer](https://adoptium.net/temurin/releases/) and make sure
`java` is available in your terminal. Maven is downloaded automatically by the
included Maven Wrapper.

From a desktop terminal in the project directory, launch the GUI:

**Windows (PowerShell):**

```powershell
.\mvnw.cmd javafx:run
```

**macOS/Linux:**

```sh
./mvnw javafx:run
```

The first launch downloads Maven dependencies and opens the JavaFX banking
dashboard. Create the owner login when prompted; subsequent launches require
that login. The GUI provides:

- Dashboard summaries and customer/account search
- Customer and account management with balances
- Complete transaction history, with an all-customers view and customer filter
- Customer profiles with contact details, accounts, balances, and transactions

The app creates `BankSystem.db` in the project directory and keeps its data
locally. A new database is seeded with fictional sample customers and accounts.
Do not commit or share the database file.

Run the GUI on a computer with a desktop display. Headless environments such
as GitHub Codespaces can build the project but cannot display the JavaFX window.

## Run in VS Code

Open the project folder in VS Code and select **Terminal > Run Task > Run
BankSystem GUI**.

## Run the optional browser application

The desktop GUI is the default. To start the optional local web server instead:

**Windows (PowerShell):**

```powershell
.\mvnw.cmd exec:java -Dexec.mainClass=LocalBankServer
```

**macOS/Linux:**

```sh
./mvnw exec:java -Dexec.mainClass=LocalBankServer
```

This is a local project application, not a production banking system. Do not
use it to store live customer financial data without a security and compliance
review.