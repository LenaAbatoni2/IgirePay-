
package Model;

import DAO.AccountDAO;
import DAO.CustomerDAO;
import DAO.LoanDAO;
import DAO.ProcessedRequestDAO;
import DAO.TransactionDAO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class Main {

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        String url = "jdbc:postgresql://localhost:5432/IgirePay";
        String username = "postgres";
        String password = "Rwandanziza@123";

        try (Connection connection =
                     DriverManager.getConnection(url, username, password)) {

            boolean running = true;

            while (running) {
                System.out.println("\n===== IGIREPAY =====");
                System.out.println("1. Create Account");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Choose an option: ");

                int option = readInt(input);

                switch (option) {
                    case 1:
                        try {
                            createAccount(connection, input);
                        } catch (SQLException e) {
                            System.out.println(
                                    "Registration failed. Please try again."
                            );
                            System.out.println(
                                    "Database error: " + e.getMessage()
                            );
                        }
                        break;

                    case 2: {
                        System.out.print("Enter your customer ID: ");
                        int customerID = readInt(input);

                        boolean authenticated = false;

                        try {
                            for (int attempt = 1; attempt <= 3; attempt++) {
                                System.out.print("Enter your 4-digit PIN: ");
                                String enteredPIN = input.nextLine().trim();

                                if (validateCustomerPIN(
                                        connection, customerID, enteredPIN)) {
                                    authenticated = true;
                                    System.out.println("Login successful!");
                                    break;
                                }

                                if (attempt < 3) {
                                    System.out.println(
                                            "Incorrect customer ID or PIN."
                                    );
                                    System.out.println(
                                            "Attempts remaining: "
                                                    + (3 - attempt)
                                    );
                                }
                            }

                            if (!authenticated) {
                                System.out.println(
                                        "Three incorrect attempts. Access denied."
                                );
                                break;
                            }

                            Map<Integer, Account> accounts = new HashMap<>();
                            Map<Integer, String> accountTypes = new HashMap<>();

                            loadCustomerAccounts(
                                    connection, customerID,
                                    accounts, accountTypes
                            );

                            if (accounts.isEmpty()) {
                                System.out.println(
                                        "No active accounts found for this customer."
                                );
                            } else {
                                try {
                                    bankingMenu(
                                            connection, input, customerID,
                                            accounts, accountTypes
                                    );
                                } catch (SQLException e) {
                                    System.out.println(
                                            "A database operation failed. "
                                                    + "Check your account balance "
                                                    + "before retrying."
                                    );
                                    System.out.println(
                                            "Database error: " + e.getMessage()
                                    );
                                }
                            }

                        } catch (SQLException e) {
                            System.out.println(
                                    "Login or account loading failed."
                            );
                            System.out.println(
                                    "Database error: " + e.getMessage()
                            );
                        }
                        break;
                    }

                    case 3:
                        running = false;
                        System.out.println("Thank you for using IgirePay!");
                        break;

                    default:
                        System.out.println("Invalid option.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not connect to the database.");
            System.out.println(
                    "Check that PostgreSQL is running and your connection "
                            + "settings are correct."
            );
            System.out.println("Database error: " + e.getMessage());

        } finally {
            input.close();
        }
    }

    // REGISTER CUSTOMER AND CREATE ACCOUNT

    public static void createAccount(
            Connection connection, Scanner input) throws SQLException {

        System.out.println("\n===== REGISTER CUSTOMER =====");

        System.out.print("Enter your full names: ");
        String fullName = input.nextLine().trim();

        System.out.print("Enter your email: ");
        String email = input.nextLine().trim();

        System.out.print("Enter your phone number: ");
        String phone = input.nextLine().trim();

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            System.out.println("All customer details are required.");
            return;
        }

        System.out.print("Create a 4-digit PIN: ");
        String pin = input.nextLine().trim();

        if (!pin.matches("\\d{4}")) {
            System.out.println(
                    "Your PIN must contain exactly four digits."
            );
            return;
        }

        System.out.println("\nChoose Account Type:");
        System.out.println("1. Savings Account");
        System.out.println("2. Wallet Account");
        System.out.print("Choose: ");

        int type = readInt(input);
        String accountType;

        if (type == 1) {
            accountType = "Savings";
        } else if (type == 2) {
            accountType = "Wallet";
        } else {
            System.out.println("Invalid account type.");
            return;
        }

        System.out.print("Enter initial deposit (minimum 10000 FRW): ");
        double balance = readDouble(input);

        if (balance < 10000) {
            System.out.println(
                    "Initial deposit must be at least 10000 FRW."
            );
            return;
        }

        String referenceID = generateReferenceID();
        boolean previousAutoCommit = connection.getAutoCommit();

        CustomerDAO customerDAO = new CustomerDAO();
        AccountDAO accountDAO = new AccountDAO();
        TransactionDAO transactionDAO = new TransactionDAO();
        ProcessedRequestDAO processedRequestDAO =
                new ProcessedRequestDAO();

        try {
            connection.setAutoCommit(false);

            if (!processedRequestDAO.registerRequest(
                    connection, referenceID)) {
                connection.rollback();
                System.out.println("This reference was already processed.");
                return;
            }

            int customerID = customerDAO.createCustomer(
                    connection, fullName, email, phone, pin
            );

            int accountID = accountDAO.createAccount(
                    connection, customerID, accountType, balance
            );

            transactionDAO.saveTransaction(
                    connection, accountID, balance,
                    "Deposit", referenceID
            );

            connection.commit();

            System.out.println("\nRegistration successful!");
            System.out.println("Customer ID: " + customerID);
            System.out.println("Account ID: " + accountID);
            System.out.println("Account Type: " + accountType);
            System.out.println("Balance: " + balance + " FRW");
            System.out.println(
                    "Initial deposit reference: " + referenceID
            );
            System.out.println(
                    "Keep your customer ID and PIN to access your accounts."
            );

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            throw e;

        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException e) {
                System.out.println(
                        "Warning: Could not restore database transaction "
                                + "settings: " + e.getMessage()
                );
            }
        }
    }

    // CREATE ANOTHER ACCOUNT FOR AN EXISTING CUSTOMER

    public static void createAnotherAccount(
            Connection connection,
            Scanner input,
            int customerID,
            Map<Integer, Account> accounts,
            Map<Integer, String> accountTypes) throws SQLException {

        boolean hasSavings = accountTypes.containsValue("Savings");
        boolean hasWallet = accountTypes.containsValue("Wallet");

        if (hasSavings && hasWallet) {
            System.out.println(
                    "You already have both a Savings Account and a Wallet Account."
            );
            return;
        }

        System.out.println("\n===== CREATE ANOTHER ACCOUNT =====");

        if (!hasSavings) {
            System.out.println("1. Savings Account");
        }

        if (!hasWallet) {
            System.out.println("2. Wallet Account");
        }

        System.out.println("0. Go Back");
        System.out.print("Choose an account type: ");

        int choice = readInt(input);

        if (choice == 0) {
            return;
        }

        String accountType;

        if (choice == 1 && !hasSavings) {
            accountType = "Savings";
        } else if (choice == 2 && !hasWallet) {
            accountType = "Wallet";
        } else {
            System.out.println(
                    "Invalid choice or you already have that account type."
            );
            return;
        }

        System.out.print("Enter initial deposit (minimum 10000 FRW): ");
        double balance = readDouble(input);

        if (balance < 10000) {
            System.out.println(
                    "Initial deposit must be at least 10000 FRW."
            );
            return;
        }

        String referenceID = generateReferenceID();
        boolean previousAutoCommit = connection.getAutoCommit();

        AccountDAO accountDAO = new AccountDAO();
        TransactionDAO transactionDAO = new TransactionDAO();
        ProcessedRequestDAO processedRequestDAO =
                new ProcessedRequestDAO();

        try {
            connection.setAutoCommit(false);

            if (!processedRequestDAO.registerRequest(
                    connection, referenceID)) {
                connection.rollback();
                System.out.println("This request has already been processed.");
                return;
            }

            int accountID = accountDAO.createAccount(
                    connection, customerID, accountType, balance
            );

            transactionDAO.saveTransaction(
                    connection, accountID, balance,
                    "Deposit", referenceID
            );

            connection.commit();

            Account newAccount;

            if (accountType.equals("Savings")) {
                newAccount = new SavingsAccount(accountID, balance);
            } else {
                newAccount = new WalletAccount(accountID, balance);
            }

            accounts.put(accountID, newAccount);
            accountTypes.put(accountID, accountType);

            System.out.println("\nAccount created successfully!");
            System.out.println("Account ID: " + accountID);
            System.out.println("Account Type: " + accountType);
            System.out.println("Initial Balance: " + balance + " FRW");
            System.out.println("Deposit Reference: " + referenceID);

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            throw e;

        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException e) {
                System.out.println(
                        "Warning: Could not restore transaction settings: "
                                + e.getMessage()
                );
            }
        }
    }

    // VALIDATE CUSTOMER PIN

    public static boolean validateCustomerPIN(
            Connection connection, int customerID, String enteredPIN)
            throws SQLException {

        CustomerDAO customerDAO = new CustomerDAO();

        return customerDAO.validateCustomerPIN(
                connection, customerID, enteredPIN
        );
    }

    // LOAD ACTIVE CUSTOMER ACCOUNTS

    public static void loadCustomerAccounts(
            Connection connection,
            int customerID,
            Map<Integer, Account> accounts,
            Map<Integer, String> accountTypes) throws SQLException {

        AccountDAO accountDAO = new AccountDAO();

        Map<Integer, Account> loadedAccounts =
                accountDAO.getCustomerAccounts(connection, customerID);

        accounts.putAll(loadedAccounts);

        for (Account account : loadedAccounts.values()) {
            int accountID = account.getAccountID();

            if (account instanceof SavingsAccount) {
                accountTypes.put(accountID, "Savings");
            } else if (account instanceof WalletAccount) {
                accountTypes.put(accountID, "Wallet");
            }
        }
    }

    // LET THE CUSTOMER CHOOSE AN ACCOUNT

    public static Account chooseAccount(
            Scanner input,
            Map<Integer, Account> accounts,
            Map<Integer, String> accountTypes) {

        if (accounts.isEmpty()) {
            System.out.println("You have no active accounts.");
            return null;
        }

        List<Account> accountList = new ArrayList<>(accounts.values());

        System.out.println("\n===== YOUR ACCOUNTS =====");

        for (int i = 0; i < accountList.size(); i++) {
            Account account = accountList.get(i);

            System.out.println(
                    (i + 1) + ". "
                            + accountTypes.get(account.getAccountID())
                            + " Account - "
                            + account.getBalance() + " FRW"
            );
        }

        System.out.println("0. Go Back");
        System.out.print("Choose an account: ");

        int choice = readInt(input);

        if (choice == 0) {
            return null;
        }

        if (choice < 1 || choice > accountList.size()) {
            System.out.println("Invalid account choice.");
            return null;
        }

        return accountList.get(choice - 1);
    }

    // BANKING MENU

    public static void bankingMenu(
            Connection connection,
            Scanner input,
            int customerID,
            Map<Integer, Account> accounts,
            Map<Integer, String> accountTypes) throws SQLException {

        boolean running = true;

        while (running) {
            System.out.println("\n===== IGIREPAY BANKING MENU =====");
            System.out.println("1. Check Balance");
            System.out.println("2. Send Money");
            System.out.println("3. Deposit Money");
            System.out.println("4. Withdraw Money");
            System.out.println("5. Manage Savings");
            System.out.println("6. Request a Loan");
            System.out.println("7. View Transaction History");
            System.out.println("8. Close Inactive Account");
            System.out.println("9. Update Customer Details");
            System.out.println("10. Transaction Reports");
            System.out.println("11. Create Another Account");
            System.out.println("12. Log Out");
            System.out.print("Choose an option: ");

            int option = readInt(input);

            switch (option) {

                case 1:
                    System.out.println(
                            "\n===== YOUR ACCOUNT BALANCES ====="
                    );

                    for (Account account : accounts.values()) {
                        System.out.println(
                                accountTypes.get(account.getAccountID())
                                        + " Account: "
                                        + account.getBalance() + " FRW"
                        );
                    }
                    break;

                // SEND MONEY

                case 2: {
                    System.out.println(
                            "\nChoose the account to send money from."
                    );

                    Account sender = chooseAccount(
                            input, accounts, accountTypes
                    );

                    if (sender == null) {
                        break;
                    }

                    int senderID = sender.getAccountID();

                    System.out.print("Enter receiver account ID: ");
                    int receiverID = readInt(input);

                    if (senderID == receiverID) {
                        System.out.println(
                                "You cannot send money to the same account."
                        );
                        break;
                    }

                    Account receiver = getAccount(connection, receiverID);

                    if (receiver == null) {
                        System.out.println(
                                "Receiver account does not exist or is closed."
                        );
                        break;
                    }

                    System.out.print("Enter amount to send: ");
                    double transferAmount = readDouble(input);

                    if (transferAmount <= 0
                            || transferAmount > sender.getBalance()) {
                        System.out.println(
                                "Invalid amount or insufficient balance."
                        );
                        break;
                    }

                    String referenceID = generateReferenceID();
                    double oldSenderBalance = sender.getBalance();
                    double oldReceiverBalance = receiver.getBalance();
                    boolean previousAutoCommit = connection.getAutoCommit();

                    try {
                        connection.setAutoCommit(false);

                        if (!registerProcessedRequest(
                                connection, referenceID)) {
                            connection.rollback();
                            System.out.println(
                                    "This request has already been processed. "
                                            + "No money was transferred."
                            );
                            break;
                        }

                        sender.setBalance(
                                oldSenderBalance - transferAmount
                        );
                        receiver.setBalance(
                                oldReceiverBalance + transferAmount
                        );

                        updateBalance(
                                connection, senderID, sender.getBalance()
                        );
                        updateBalance(
                                connection, receiverID, receiver.getBalance()
                        );

                        saveTransaction(
                                connection, senderID, transferAmount,
                                "Transfer", referenceID + "-S"
                        );

                        saveTransaction(
                                connection, receiverID, transferAmount,
                                "Transfer", referenceID + "-R"
                        );

                        connection.commit();

                        System.out.println("Transfer successful!");
                        System.out.println(
                                "Transaction reference: " + referenceID
                        );
                        System.out.println(
                                "Remaining balance: "
                                        + sender.getBalance() + " FRW"
                        );

                        if (accounts.containsKey(receiverID)) {
                            accounts.get(receiverID).setBalance(
                                    receiver.getBalance()
                            );
                        }

                    } catch (SQLException e) {
                        try {
                            connection.rollback();
                        } catch (SQLException rollbackError) {
                            e.addSuppressed(rollbackError);
                        }

                        sender.setBalance(oldSenderBalance);
                        receiver.setBalance(oldReceiverBalance);
                        throw e;

                    } finally {
                        try {
                            connection.setAutoCommit(previousAutoCommit);
                        } catch (SQLException e) {
                            System.out.println(
                                    "Warning: Could not restore transaction "
                                            + "settings: " + e.getMessage()
                            );
                        }
                    }
                    break;
                }

                // DEPOSIT MONEY

                case 3: {
                    System.out.println(
                            "\nChoose the account to deposit into."
                    );

                    Account depositAccount = chooseAccount(
                            input, accounts, accountTypes
                    );

                    if (depositAccount == null) {
                        break;
                    }

                    int depositID = depositAccount.getAccountID();

                    System.out.print("Enter deposit amount: ");
                    double depositAmount = readDouble(input);

                    if (depositAmount <= 0) {
                        System.out.println(
                                "Amount must be greater than zero."
                        );
                        break;
                    }

                    String referenceID = generateReferenceID();
                    double oldBalance = depositAccount.getBalance();
                    boolean previousAutoCommit = connection.getAutoCommit();

                    try {
                        connection.setAutoCommit(false);

                        if (!registerProcessedRequest(
                                connection, referenceID)) {
                            connection.rollback();
                            System.out.println(
                                    "This request has already been processed. "
                                            + "No deposit was made."
                            );
                            break;
                        }

                        depositAccount.setBalance(
                                oldBalance + depositAmount
                        );

                        updateBalance(
                                connection, depositID,
                                depositAccount.getBalance()
                        );

                        saveTransaction(
                                connection, depositID, depositAmount,
                                "Deposit", referenceID
                        );

                        connection.commit();

                        System.out.println("Deposit successful!");
                        System.out.println(
                                "Transaction reference: " + referenceID
                        );
                        System.out.println(
                                "New balance: "
                                        + depositAccount.getBalance() + " FRW"
                        );

                    } catch (SQLException e) {
                        try {
                            connection.rollback();
                        } catch (SQLException rollbackError) {
                            e.addSuppressed(rollbackError);
                        }

                        depositAccount.setBalance(oldBalance);
                        throw e;

                    } finally {
                        try {
                            connection.setAutoCommit(previousAutoCommit);
                        } catch (SQLException e) {
                            System.out.println(
                                    "Warning: Could not restore transaction "
                                            + "settings: " + e.getMessage()
                            );
                        }
                    }
                    break;
                }

                // WITHDRAW MONEY

                case 4: {
                    System.out.println(
                            "\nChoose the account to withdraw from."
                    );

                    Account withdrawAccount = chooseAccount(
                            input, accounts, accountTypes
                    );

                    if (withdrawAccount == null) {
                        break;
                    }

                    int withdrawID = withdrawAccount.getAccountID();

                    System.out.print("Enter withdrawal amount: ");
                    double withdrawAmount = readDouble(input);

                    double fee = 0;

                    if ("Savings".equals(accountTypes.get(withdrawID))) {
                        fee = 100;
                    }

                    if (withdrawAmount <= 0
                            || withdrawAmount + fee
                            > withdrawAccount.getBalance()) {
                        System.out.println(
                                "Invalid amount or insufficient balance."
                        );
                        break;
                    }

                    String referenceID = generateReferenceID();
                    double oldBalance = withdrawAccount.getBalance();
                    boolean previousAutoCommit = connection.getAutoCommit();

                    try {
                        connection.setAutoCommit(false);

                        if (!registerProcessedRequest(
                                connection, referenceID)) {
                            connection.rollback();
                            System.out.println(
                                    "This request has already been processed. "
                                            + "No withdrawal was made."
                            );
                            break;
                        }

                        withdrawAccount.setBalance(
                                oldBalance - withdrawAmount - fee
                        );

                        updateBalance(
                                connection, withdrawID,
                                withdrawAccount.getBalance()
                        );

                        saveTransaction(
                                connection, withdrawID, withdrawAmount,
                                "Withdrawal", referenceID
                        );

                        connection.commit();

                        System.out.println("Withdrawal successful!");
                        System.out.println(
                                "Transaction reference: " + referenceID
                        );

                        if (fee > 0) {
                            System.out.println(
                                    "Savings withdrawal fee: "
                                            + fee + " FRW"
                            );
                        }

                        System.out.println(
                                "Remaining balance: "
                                        + withdrawAccount.getBalance() + " FRW"
                        );

                    } catch (SQLException e) {
                        try {
                            connection.rollback();
                        } catch (SQLException rollbackError) {
                            e.addSuppressed(rollbackError);
                        }

                        withdrawAccount.setBalance(oldBalance);
                        throw e;

                    } finally {
                        try {
                            connection.setAutoCommit(previousAutoCommit);
                        } catch (SQLException e) {
                            System.out.println(
                                    "Warning: Could not restore transaction "
                                            + "settings: " + e.getMessage()
                            );
                        }
                    }
                    break;
                }

                // MANAGE SAVINGS

                case 5:
                    System.out.println("\n===== MANAGE SAVINGS =====");

                    boolean foundSavings = false;

                    for (Account account : accounts.values()) {
                        int id = account.getAccountID();

                        if ("Savings".equals(accountTypes.get(id))) {
                            foundSavings = true;
                            System.out.println(
                                    "Savings Balance: "
                                            + account.getBalance() + " FRW"
                            );
                            System.out.println("Withdrawal fee: 100 FRW");
                        }
                    }

                    if (!foundSavings) {
                        System.out.println(
                                "You do not have a Savings Account."
                        );
                    }
                    break;

                // REQUEST A LOAN

                case 6:
                    requestLoan(
                            connection, input, customerID,
                            accounts, accountTypes
                    );
                    break;

                // TRANSACTION HISTORY

                case 7: {
                    System.out.println(
                            "\nChoose the account whose history you want to view."
                    );

                    Account historyAccount = chooseAccount(
                            input, accounts, accountTypes
                    );

                    if (historyAccount == null) {
                        break;
                    }

                    TransactionDAO transactionDAO = new TransactionDAO();

                    List<Transactions> history =
                            transactionDAO.getTransactionHistory(
                                    connection,
                                    historyAccount.getAccountID()
                            );

                    if (history.isEmpty()) {
                        System.out.println("No transactions found.");
                    } else {
                        for (Transactions transaction : history) {
                            System.out.println(
                                    "\nTransaction ID: "
                                            + transaction.getTransactionID()
                            );
                            System.out.println(
                                    "Reference: "
                                            + transaction.getReferenceID()
                            );
                            System.out.println(
                                    "Type: "
                                            + transaction.getTransactionType()
                            );
                            System.out.println(
                                    "Amount: "
                                            + transaction.getAmount() + " FRW"
                            );
                            System.out.println(
                                    "Date: " + transaction.getTimestamp()
                            );
                        }

                        // RESTORED: DISPLAY THE TOTAL NUMBER OF TRANSACTIONS

                        System.out.println(
                                "\nTotal transactions: " + history.size()
                        );
                    }
                    break;
                }

                case 8:
                    closeInactiveAccount(
                            connection, input, customerID,
                            accounts, accountTypes
                    );
                    break;

                case 9:
                    updateCustomerDetails(connection, input, customerID);
                    break;

                // TRANSACTION REPORTS

                case 10:
                    transactionReportsMenu(
                            connection, input, accounts, accountTypes
                    );
                    break;

                // CREATE ANOTHER ACCOUNT

                case 11:
                    createAnotherAccount(
                            connection, input, customerID,
                            accounts, accountTypes
                    );
                    break;

                // LOG OUT

                case 12:
                    running = false;
                    System.out.println("You have logged out.");
                    break;

                default:
                    System.out.println("Invalid option.");
            }

            // NAVIGATION AFTER BANKING OPERATIONS

            if ((option >= 1 && option <= 9) || option == 11) {
                boolean returnToMenu = showPostOperationMenu(input);

                if (!returnToMenu) {
                    running = false;
                }
            }
        }
    }

    // TRANSACTION REPORTS MENU

    public static void transactionReportsMenu(
            Connection connection,
            Scanner input,
            Map<Integer, Account> accounts,
            Map<Integer, String> accountTypes) throws SQLException {

        boolean running = true;
        TransactionDAO transactionDAO = new TransactionDAO();

        while (running) {
            System.out.println("\n===== TRANSACTION REPORTS =====");
            System.out.println("1. Export Transaction History to CSV");
            System.out.println("2. View Daily Transaction Summary");
            System.out.println("3. Display Customer Transaction Statement");
            System.out.println("4. Back to Banking Menu");
            System.out.print("Choose an option: ");

            int choice = readInt(input);

            switch (choice) {

                case 1: {
                    System.out.println("\nChoose the account to export.");

                    Account selectedAccount = chooseAccount(
                            input, accounts, accountTypes
                    );

                    if (selectedAccount == null) {
                        break;
                    }

                    int accountID = selectedAccount.getAccountID();

                    String fileName =
                            "transaction_history_" + accountID + ".csv";

                    try {
                        transactionDAO.exportTransactionHistoryToCSV(
                                connection, accountID, fileName
                        );

                        System.out.println(
                                "Transaction history exported successfully!"
                        );
                        System.out.println("File location: " + fileName);

                    } catch (IOException e) {
                        System.out.println(
                                "Could not create the CSV file: "
                                        + e.getMessage()
                        );
                    }
                    break;
                }

                case 2: {
                    LocalDate today = LocalDate.now();

                    Map<String, Double> summary =
                            transactionDAO.getDailyTransactionSummary(
                                    connection, today
                            );

                    System.out.println(
                            "\n===== DAILY TRANSACTION SUMMARY ====="
                    );
                    System.out.println("Date: " + today);

                    if (summary.isEmpty()) {
                        System.out.println(
                                "No transactions recorded today."
                        );
                    } else {
                        double grandTotal = 0;

                        for (Map.Entry<String, Double> entry
                                : summary.entrySet()) {

                            System.out.println(
                                    entry.getKey() + ": "
                                            + entry.getValue() + " FRW"
                            );

                            grandTotal += entry.getValue();
                        }

                        System.out.println("------------------------------");
                        System.out.println(
                                "Total of recorded transaction amounts: "
                                        + grandTotal + " FRW"
                        );
                    }
                    break;
                }

                case 3: {
                    System.out.println(
                            "\nChoose the account for the statement."
                    );

                    Account selectedAccount = chooseAccount(
                            input, accounts, accountTypes
                    );

                    if (selectedAccount == null) {
                        break;
                    }

                    List<Transactions> statement =
                            transactionDAO.getCustomerTransactionStatement(
                                    connection,
                                    selectedAccount.getAccountID()
                            );

                    System.out.println(
                            "\n===== CUSTOMER TRANSACTION STATEMENT ====="
                    );
                    System.out.println(
                            "Account ID: "
                                    + selectedAccount.getAccountID()
                    );
                    System.out.println(
                            "Account Type: "
                                    + accountTypes.get(
                                    selectedAccount.getAccountID())
                    );

                    if (statement.isEmpty()) {
                        System.out.println(
                                "No transactions found for this account."
                        );
                    } else {
                        double totalRecorded = 0;

                        for (Transactions transaction : statement) {
                            System.out.println(
                                    "\nTransaction ID: "
                                            + transaction.getTransactionID()
                            );
                            System.out.println(
                                    "Reference: "
                                            + transaction.getReferenceID()
                            );
                            System.out.println(
                                    "Type: "
                                            + transaction.getTransactionType()
                            );
                            System.out.println(
                                    "Amount: "
                                            + transaction.getAmount() + " FRW"
                            );
                            System.out.println(
                                    "Date: "
                                            + transaction.getTimestamp()
                            );

                            totalRecorded += transaction.getAmount();
                        }

                        System.out.println("\n------------------------------");
                        System.out.println(
                                "Total recorded transaction amounts: "
                                        + totalRecorded + " FRW"
                        );
                        System.out.println(
                                "Current account balance: "
                                        + selectedAccount.getBalance()
                                        + " FRW"
                        );
                    }
                    break;
                }

                case 4:
                    running = false;
                    break;

                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    // NAVIGATION AFTER A BANKING OPERATION

    public static boolean showPostOperationMenu(Scanner input) {
        System.out.println("\n1. Back to Banking Menu");
        System.out.println("2. Log Out");
        System.out.print("Choose an option: ");

        while (true) {
            int choice = readInt(input);

            if (choice == 1) {
                return true;
            } else if (choice == 2) {
                System.out.println("You have logged out.");
                return false;
            } else {
                System.out.println("Invalid option.");
                System.out.print(
                        "Choose 1 to return or 2 to log out: "
                );
            }
        }
    }

    // REQUEST A LOAN USING LoanDAO

    public static void requestLoan(
            Connection connection,
            Scanner input,
            int customerID,
            Map<Integer, Account> accounts,
            Map<Integer, String> accountTypes) throws SQLException {

        System.out.println("\n===== REQUEST A LOAN =====");

        Account selectedAccount = chooseAccount(
                input, accounts, accountTypes
        );

        if (selectedAccount == null) {
            return;
        }

        int accountID = selectedAccount.getAccountID();

        LoanDAO loanDAO = new LoanDAO();

        if (!loanDAO.isAccountAtLeast30DaysOld(connection, accountID)) {
            System.out.println(
                    "You are not eligible yet. Your account must be at "
                            + "least 30 days old and active."
            );
            return;
        }

        double currentBalance =
                loanDAO.getAccountBalance(connection, accountID);

        if (currentBalance < 0) {
            System.out.println(
                    "This account is not active or could not be found."
            );
            return;
        }

        if (currentBalance < 20000) {
            System.out.println(
                    "You need a balance of at least 20000 FRW to qualify."
            );
            return;
        }

        int transactionCount =
                loanDAO.getTransactionCount(connection, accountID);

        if (transactionCount < 2) {
            System.out.println(
                    "You need at least 2 transactions on this account "
                            + "to qualify."
            );
            return;
        }

        if (loanDAO.hasOutstandingLoan(connection, accountID)) {
            System.out.println(
                    "This account already has an outstanding loan. "
                            + "Repay it before requesting another loan."
            );
            return;
        }

        double maximumLoan =
                loanDAO.calculateMaximumLoan(currentBalance);

        System.out.println("\nYou meet the basic loan requirements!");
        System.out.println("Current balance: " + currentBalance + " FRW");
        System.out.println("Minimum loan: 5000 FRW");
        System.out.println("Maximum loan: " + maximumLoan + " FRW");

        System.out.print("Enter the loan amount you want: ");
        double loanAmount = readDouble(input);

        if (loanAmount < 5000) {
            System.out.println(
                    "The minimum loan amount is 5000 FRW."
            );
            return;
        }

        if (loanAmount > maximumLoan) {
            System.out.println(
                    "The maximum loan for your current balance is "
                            + maximumLoan + " FRW."
            );
            return;
        }

        // CONFIRM THE LOAN REQUEST BEFORE PROCESSING IT

        System.out.println("\n===== CONFIRM LOAN REQUEST =====");
        System.out.println("Loan amount: " + loanAmount + " FRW");
        System.out.println(
                "Account: " + accountTypes.get(accountID) + " Account"
        );
        System.out.println("1. Confirm loan request");
        System.out.println("2. Cancel loan request");
        System.out.print("Choose an option: ");

        int confirmation = readInt(input);

        if (confirmation != 1) {
            System.out.println(
                    "Loan request cancelled. No changes were made."
            );
            return;
        }

        String referenceID = generateReferenceID();
        double oldBalance = selectedAccount.getBalance();
        boolean previousAutoCommit = connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);

            if (!registerProcessedRequest(connection, referenceID)) {
                connection.rollback();
                System.out.println(
                        "This loan request has already been processed."
                );
                return;
            }

            loanDAO.saveLoan(
                    connection, customerID, accountID,
                    loanAmount, referenceID
            );

            double newBalance = currentBalance + loanAmount;

            updateBalance(connection, accountID, newBalance);

            saveTransaction(
                    connection, accountID, loanAmount,
                    "Loan", referenceID
            );

            connection.commit();

            selectedAccount.setBalance(newBalance);

            System.out.println(
                    "\nCongratulations! Your loan was approved."
            );
            System.out.println("Loan amount: " + loanAmount + " FRW");
            System.out.println("Loan reference: " + referenceID);
            System.out.println(
                    "Updated account balance: " + newBalance + " FRW"
            );

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }

            selectedAccount.setBalance(oldBalance);
            throw e;

        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException e) {
                System.out.println(
                        "Warning: Could not restore transaction settings: "
                                + e.getMessage()
                );
            }
        }
    }

    // CLOSE AN ACCOUNT WITHOUT DELETING TRANSACTION HISTORY

    public static void closeInactiveAccount(
            Connection connection,
            Scanner input,
            int customerID,
            Map<Integer, Account> accounts,
            Map<Integer, String> accountTypes) throws SQLException {

        System.out.println("\n===== CLOSE ACCOUNT =====");
        System.out.println(
                "Only accounts with a zero balance can be closed."
        );
        System.out.println("Transaction history will be preserved.");

        Account selectedAccount = chooseAccount(
                input, accounts, accountTypes
        );

        if (selectedAccount == null) {
            return;
        }

        int accountID = selectedAccount.getAccountID();
        boolean previousAutoCommit = connection.getAutoCommit();
        AccountDAO accountDAO = new AccountDAO();

        try {
            connection.setAutoCommit(false);

            String checkSQL = """
                    SELECT balance
                    FROM accounts
                    WHERE id = ?
                      AND customer_id = ?
                      AND is_active = TRUE
                    FOR UPDATE
                    """;

            double balance;

            try (PreparedStatement statement =
                         connection.prepareStatement(checkSQL)) {

                statement.setInt(1, accountID);
                statement.setInt(2, customerID);

                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        connection.rollback();
                        System.out.println(
                                "Account not found or already closed."
                        );
                        return;
                    }

                    balance = result.getDouble("balance");
                }
            }

            if (balance != 0.0) {
                connection.rollback();
                System.out.println(
                        "This account still has money. Transfer or withdraw "
                                + "the remaining balance before closing it."
                );
                return;
            }

            if (!accountDAO.closeAccount(
                    connection, customerID, accountID)) {
                connection.rollback();
                System.out.println("Account could not be closed.");
                return;
            }

            connection.commit();

            accounts.remove(accountID);
            accountTypes.remove(accountID);

            System.out.println("Account closed successfully.");
            System.out.println(
                    "Its transaction history remains in the database."
            );

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            throw e;

        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException e) {
                System.out.println(
                        "Warning: Could not restore transaction settings: "
                                + e.getMessage()
                );
            }
        }
    }

    // UPDATE CUSTOMER DETAILS

    public static void updateCustomerDetails(
            Connection connection, Scanner input, int customerID) {

        System.out.println("\n===== UPDATE CUSTOMER DETAILS =====");
        System.out.println("1. Update Full Name");
        System.out.println("2. Update Email");
        System.out.println("3. Update Phone Number");
        System.out.println("4. Change PIN");
        System.out.println("5. Go Back");
        System.out.print("Choose what you want to update: ");

        int choice = readInt(input);
        String field;
        String newValue;

        switch (choice) {
            case 1:
                field = "name";
                System.out.print("Enter your new full name: ");
                newValue = input.nextLine().trim();

                if (newValue.isEmpty()) {
                    System.out.println("Name cannot be empty.");
                    return;
                }
                break;

            case 2:
                field = "email";
                System.out.print("Enter your new email: ");
                newValue = input.nextLine().trim();

                if (newValue.isEmpty()) {
                    System.out.println("Email cannot be empty.");
                    return;
                }
                break;

            case 3:
                field = "phone";
                System.out.print("Enter your new phone number: ");
                newValue = input.nextLine().trim();

                if (newValue.isEmpty()) {
                    System.out.println("Phone cannot be empty.");
                    return;
                }
                break;

            case 4:
                field = "pin";
                System.out.print("Enter your new 4-digit PIN: ");
                String newPIN = input.nextLine().trim();

                if (!newPIN.matches("\\d{4}")) {
                    System.out.println(
                            "PIN must contain exactly four digits."
                    );
                    return;
                }

                System.out.print("Confirm your new PIN: ");
                String confirmPIN = input.nextLine().trim();

                if (!newPIN.equals(confirmPIN)) {
                    System.out.println("The PINs do not match.");
                    return;
                }

                newValue = newPIN;
                break;

            case 5:
                System.out.println("Returning to the banking menu.");
                return;

            default:
                System.out.println("Invalid option.");
                return;
        }

        CustomerDAO customerDAO = new CustomerDAO();

        try {
            boolean updated = customerDAO.updateCustomerField(
                    connection, customerID, field, newValue
            );

            if (updated) {
                System.out.println(
                        "Your details were updated successfully!"
                );
            } else {
                System.out.println("Customer was not found.");
            }

        } catch (SQLException e) {
            System.out.println(
                    "Update failed. The email may already be registered, "
                            + "or a database error occurred."
            );
            System.out.println("Database message: " + e.getMessage());
        }
    }

    // GET AN ACTIVE ACCOUNT

    public static Account getAccount(
            Connection connection, int accountID) throws SQLException {

        AccountDAO accountDAO = new AccountDAO();
        return accountDAO.getAccount(connection, accountID);
    }

    // UPDATE ACCOUNT BALANCE

    public static void updateBalance(
            Connection connection, int accountID, double balance)
            throws SQLException {

        AccountDAO accountDAO = new AccountDAO();

        boolean updated = accountDAO.updateBalance(
                connection, accountID, balance
        );

        if (!updated) {
            throw new SQLException(
                    "Account was not found or is no longer active."
            );
        }
    }

    // GENERATE UNIQUE TRANSACTION REFERENCE

    public static String generateReferenceID() {
        return UUID.randomUUID().toString();
    }

    // REGISTER PROCESSED REQUEST

    public static boolean registerProcessedRequest(
            Connection connection, String referenceID) throws SQLException {

        ProcessedRequestDAO processedRequestDAO =
                new ProcessedRequestDAO();

        return processedRequestDAO.registerRequest(
                connection, referenceID
        );
    }

    // SAVE TRANSACTION

    public static void saveTransaction(
            Connection connection,
            int accountID,
            double amount,
            String transactionType,
            String referenceID) throws SQLException {

        TransactionDAO transactionDAO = new TransactionDAO();

        transactionDAO.saveTransaction(
                connection, accountID, amount,
                transactionType, referenceID
        );
    }

    // READ AND VALIDATE DECIMAL INPUT

    public static double readDouble(Scanner input) {
        while (true) {
            try {
                double amount = Double.parseDouble(
                        input.nextLine().trim()
                );

                if (!Double.isFinite(amount)) {
                    System.out.print(
                            "Invalid amount. Enter a normal number: "
                    );
                    continue;
                }

                return amount;

            } catch (NumberFormatException e) {
                System.out.print(
                        "Invalid amount. Please enter a valid number: "
                );
            }
        }
    }

    // READ WHOLE-NUMBER INPUT

    public static int readInt(Scanner input) {
        while (true) {
            try {
                return Integer.parseInt(input.nextLine().trim());

            } catch (NumberFormatException e) {
                System.out.print(
                        "Please enter a valid whole number: "
                );
            }
        }
    }
}
