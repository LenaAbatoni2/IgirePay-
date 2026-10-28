
package DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoanDAO {

    // CHECK WHETHER THE ACCOUNT HAS AN OUTSTANDING LOAN

    public boolean hasOutstandingLoan(
            Connection connection, int accountID) throws SQLException {

        String sql = """
                SELECT loan_id
                FROM loans
                WHERE account_id = ?
                  AND status = 'Outstanding'
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    // CHECK THE NUMBER OF COMPLETED TRANSACTIONS

    public int getTransactionCount(
            Connection connection, int accountID) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM transactions
                WHERE account_id = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
        }

        return 0;
    }

    // CHECK WHEN THE ACCOUNT WAS CREATED

    public boolean isAccountAtLeast30DaysOld(
            Connection connection, int accountID) throws SQLException {

        String sql = """
                SELECT created_at
                FROM accounts
                WHERE id = ?
                  AND is_active = TRUE
                  AND created_at <= CURRENT_TIMESTAMP - INTERVAL '30 days'
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    // CHECK THE ACCOUNT BALANCE

    public double getAccountBalance(
            Connection connection, int accountID) throws SQLException {

        String sql = """
                SELECT balance
                FROM accounts
                WHERE id = ?
                  AND is_active = TRUE
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getDouble("balance");
                }
            }
        }

        return -1;
    }

    // CALCULATE THE MAXIMUM LOAN AMOUNT

    public double calculateMaximumLoan(double balance) {
        return balance * 3;
    }

    // SAVE AN APPROVED LOAN

    public void saveLoan(
            Connection connection,
            int customerID,
            int accountID,
            double amount,
            String referenceID) throws SQLException {

        String sql = """
                INSERT INTO loans
                    (customer_id, account_id, amount, status, reference_id)
                VALUES (?, ?, ?, 'Outstanding', ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);
            statement.setInt(2, accountID);
            statement.setDouble(3, amount);
            statement.setString(4, referenceID);

            statement.executeUpdate();
        }
    }
}
