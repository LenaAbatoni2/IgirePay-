
package DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CustomerDAO {

    public int createCustomer(Connection connection, String fullName,
                              String email, String phone, String pin)
            throws SQLException {

        String sql = """
                INSERT INTO customers
                    (full_name, email, phone_number, pin)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, phone);
            statement.setString(4, pin);

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }

                throw new SQLException("Customer ID was not generated.");
            }
        }
    }

    public boolean customerExists(Connection connection, int customerID)
            throws SQLException {

        String sql = "SELECT id FROM customers WHERE id = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean validateCustomerPIN(Connection connection,
                                       int customerID, String pin)
            throws SQLException {

        String sql = """
                SELECT id
                FROM customers
                WHERE id = ? AND pin = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);
            statement.setString(2, pin);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean updateCustomerField(Connection connection,
                                       int customerID,
                                       String field, String newValue)
            throws SQLException {

        String column;

        switch (field.toLowerCase()) {
            case "name":
                column = "full_name";
                break;

            case "email":
                column = "email";
                break;

            case "phone":
                column = "phone_number";
                break;

            case "pin":
                if (newValue == null || !newValue.matches("\\d{4}")) {
                    throw new IllegalArgumentException(
                            "PIN must contain exactly four digits.");
                }
                column = "pin";
                break;

            default:
                throw new IllegalArgumentException(
                        "Invalid customer field.");
        }

        String sql = "UPDATE customers SET " + column + " = ? WHERE id = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, newValue);
            statement.setInt(2, customerID);

            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteCustomer(Connection connection, int customerID)
            throws SQLException {

        String sql = """
                DELETE FROM customers c
                WHERE c.id = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM accounts a
                      WHERE a.customer_id = c.id
                  )
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, customerID);

            return statement.executeUpdate() == 1;
        }
    }

    public boolean emailExists(Connection connection, String email)
            throws SQLException {

        String sql = "SELECT id FROM customers WHERE email = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }
}
