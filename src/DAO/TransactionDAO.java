
package DAO;

import Model.Transactions;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TransactionDAO {

    // CREATE TRANSACTION

    public void saveTransaction(
            Connection connection,
            int accountID,
            double amount,
            String transactionType,
            String referenceID
    ) throws SQLException {

        String sql = """
                INSERT INTO transactions
                    (reference_id, account_id, amount, transaction_type)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, referenceID);
            statement.setInt(2, accountID);
            statement.setDouble(3, amount);
            statement.setString(4, transactionType);

            statement.executeUpdate();
        }
    }

    // READ TRANSACTION HISTORY FOR AN ACCOUNT

    public List<Transactions> getTransactionHistory(
            Connection connection,
            int accountID
    ) throws SQLException {

        List<Transactions> transactions = new ArrayList<>();

        String sql = """
                SELECT id, reference_id, amount,
                       transaction_type, created_at
                FROM transactions
                WHERE account_id = ?
                ORDER BY created_at DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    Timestamp timestamp = result.getTimestamp("created_at");

                    Transactions transaction = new Transactions(
                            result.getInt("id"),
                            result.getString("reference_id"),
                            result.getDouble("amount"),
                            result.getString("transaction_type"),
                            timestamp.toLocalDateTime()
                    );

                    transactions.add(transaction);
                }
            }
        }

        return transactions;
    }

    // READ ONE TRANSACTION BY ID

    public Transactions getTransactionByID(
            Connection connection,
            int transactionID
    ) throws SQLException {

        String sql = """
                SELECT id, reference_id, amount,
                       transaction_type, created_at
                FROM transactions
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, transactionID);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {

                    Timestamp timestamp = result.getTimestamp("created_at");

                    return new Transactions(
                            result.getInt("id"),
                            result.getString("reference_id"),
                            result.getDouble("amount"),
                            result.getString("transaction_type"),
                            timestamp.toLocalDateTime()
                    );
                }
            }
        }

        return null;
    }

    // UPDATE TRANSACTION TYPE

    public boolean updateTransactionType(
            Connection connection,
            int transactionID,
            String transactionType
    ) throws SQLException {

        String sql = """
                UPDATE transactions
                SET transaction_type = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, transactionType);
            statement.setInt(2, transactionID);

            return statement.executeUpdate() == 1;
        }
    }

    // DELETE A TRANSACTION BY ID
    // Do not use this during normal banking operations.

    public boolean deleteTransaction(
            Connection connection,
            int transactionID
    ) throws SQLException {

        String sql = """
                DELETE FROM transactions
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, transactionID);

            return statement.executeUpdate() == 1;
        }
    }

    // COUNT TRANSACTIONS FOR AN ACCOUNT

    public int countTransactions(
            Connection connection,
            int accountID
    ) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM transactions
                WHERE account_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    // 1. DAILY TRANSACTION SUMMARY
    // Returns total amounts grouped by transaction type for a date.

    public Map<String, Double> getDailyTransactionSummary(
            Connection connection,
            LocalDate date
    ) throws SQLException {

        Map<String, Double> summary = new LinkedHashMap<>();

        String sql = """
                SELECT transaction_type, SUM(amount) AS total_amount
                FROM transactions
                WHERE CAST(created_at AS DATE) = ?
                GROUP BY transaction_type
                ORDER BY transaction_type
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDate(1, java.sql.Date.valueOf(date));

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    String transactionType =
                            result.getString("transaction_type");

                    double totalAmount =
                            result.getDouble("total_amount");

                    summary.put(transactionType, totalAmount);
                }
            }
        }

        return summary;
    }

    // 2. EXPORT TRANSACTION HISTORY TO CSV
    // Creates a CSV file for one account.

    public void exportTransactionHistoryToCSV(
            Connection connection,
            int accountID,
            String fileName
    ) throws SQLException, IOException {

        String sql = """
                SELECT id, reference_id, amount,
                       transaction_type, created_at
                FROM transactions
                WHERE account_id = ?
                ORDER BY created_at DESC
                """;

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                PrintWriter writer =
                        new PrintWriter(new FileWriter(fileName))
        ) {

            statement.setInt(1, accountID);

            writer.println(
                    "Transaction ID,Reference ID,Amount,Transaction Type,Date"
            );

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    int transactionID = result.getInt("id");
                    String referenceID = result.getString("reference_id");
                    double amount = result.getDouble("amount");
                    String transactionType =
                            result.getString("transaction_type");
                    Timestamp timestamp = result.getTimestamp("created_at");

                    writer.println(
                            transactionID + ","
                                    + csvValue(referenceID) + ","
                                    + amount + ","
                                    + csvValue(transactionType) + ","
                                    + csvValue(timestamp.toString())
                    );
                }
            }

            if (writer.checkError()) {
                throw new IOException("Could not write the CSV file.");
            }
        }
    }

    // HELPER METHOD FOR CSV VALUES

    private String csvValue(String value) {

        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // 3. CUSTOMER TRANSACTION STATEMENT
    // Retrieves transactions for one account.
    // In this project, the statement is account-specific.

    public List<Transactions> getCustomerTransactionStatement(
            Connection connection,
            int accountID
    ) throws SQLException {

        String sql = """
                SELECT id, reference_id, amount,
                       transaction_type, created_at
                FROM transactions
                WHERE account_id = ?
                ORDER BY created_at ASC, id ASC
                """;

        List<Transactions> transactions = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, accountID);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    Timestamp timestamp = result.getTimestamp("created_at");

                    Transactions transaction = new Transactions(
                            result.getInt("id"),
                            result.getString("reference_id"),
                            result.getDouble("amount"),
                            result.getString("transaction_type"),
                            timestamp.toLocalDateTime()
                    );

                    transactions.add(transaction);
                }
            }
        }

        return transactions;
    }
}
