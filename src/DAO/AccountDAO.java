
package DAO;

import Model.Account;
import Model.SavingsAccount;
import Model.WalletAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class AccountDAO {

    // CREATE ACCOUNT

    public int createAccount(
            Connection connection,
            int customerID,
            String accountType,
            double balance
    ) throws SQLException {

        String sql = """
                INSERT INTO accounts
                    (customer_id, account_type, balance)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, customerID);
            statement.setString(2, accountType);
            statement.setDouble(3, balance);

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }

                throw new SQLException("Account ID was not generated.");
            }
        }
    }

    // READ ONE ACTIVE ACCOUNT

    public Account getAccount(
            Connection connection,
            int accountID
    ) throws SQLException {

        String sql = """
                SELECT id, balance, account_type
                FROM accounts
                WHERE id = ? AND is_active = TRUE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {

                    double balance = result.getDouble("balance");
                    String type = result.getString("account_type");

                    if ("Savings".equals(type)) {
                        return new SavingsAccount(accountID, balance);
                    }

                    if ("Wallet".equals(type)) {
                        return new WalletAccount(accountID, balance);
                    }
                }
            }
        }

        return null;
    }

    // READ ALL ACTIVE ACCOUNTS BELONGING TO A CUSTOMER

    public Map<Integer, Account> getCustomerAccounts(
            Connection connection,
            int customerID
    ) throws SQLException {

        Map<Integer, Account> accounts = new HashMap<>();

        String sql = """
                SELECT id, balance, account_type
                FROM accounts
                WHERE customer_id = ? AND is_active = TRUE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {

                    int accountID = result.getInt("id");
                    double balance = result.getDouble("balance");
                    String type = result.getString("account_type");

                    Account account = null;

                    if ("Savings".equals(type)) {
                        account = new SavingsAccount(accountID, balance);
                    } else if ("Wallet".equals(type)) {
                        account = new WalletAccount(accountID, balance);
                    }

                    if (account != null) {
                        accounts.put(accountID, account);
                    }
                }
            }
        }

        return accounts;
    }

    // UPDATE ACCOUNT BALANCE

    public boolean updateBalance(
            Connection connection,
            int accountID,
            double newBalance
    ) throws SQLException {

        String sql = """
                UPDATE accounts
                SET balance = ?
                WHERE id = ? AND is_active = TRUE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDouble(1, newBalance);
            statement.setInt(2, accountID);

            return statement.executeUpdate() == 1;
        }
    }

    // CLOSE AN ACCOUNT WHEN ITS BALANCE IS ZERO

    public boolean closeAccount(
            Connection connection,
            int customerID,
            int accountID
    ) throws SQLException {

        String sql = """
                UPDATE accounts
                SET is_active = FALSE
                WHERE id = ?
                  AND customer_id = ?
                  AND is_active = TRUE
                  AND balance = 0
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);
            statement.setInt(2, customerID);

            return statement.executeUpdate() == 1;
        }
    }

    // CUSTOM QUERY: COUNT ACTIVE ACCOUNTS

    public int countActiveAccounts(
            Connection connection,
            int customerID
    ) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM accounts
                WHERE customer_id = ? AND is_active = TRUE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
}
