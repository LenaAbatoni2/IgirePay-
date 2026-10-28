
package DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProcessedRequestDAO {

    // CREATE: REGISTER A REFERENCE
    // Returns false if that reference has already been registered.

    public boolean registerRequest(
            Connection connection,
            String referenceID
    ) throws SQLException {

        String sql = """
                INSERT INTO processed_requests (reference_id)
                VALUES (?)
                ON CONFLICT (reference_id) DO NOTHING
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, referenceID);

            return statement.executeUpdate() == 1;
        }
    }

    // READ: CHECK WHETHER A REFERENCE EXISTS

    public boolean referenceExists(
            Connection connection,
            String referenceID
    ) throws SQLException {

        String sql = """
                SELECT id
                FROM processed_requests
                WHERE reference_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, referenceID);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    // UPDATE: REFRESH THE PROCESSING TIMESTAMP
    // Administrative use only; this does not make the reference reusable.

    public boolean updateProcessedTime(
            Connection connection,
            String referenceID
    ) throws SQLException {

        String sql = """
                UPDATE processed_requests
                SET processed_at = CURRENT_TIMESTAMP
                WHERE reference_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, referenceID);

            return statement.executeUpdate() == 1;
        }
    }

    // DELETE A REFERENCE
    // Do not use this to reset normal transaction protection.

    public boolean deleteRequest(
            Connection connection,
            String referenceID
    ) throws SQLException {

        String sql = """
                DELETE FROM processed_requests
                WHERE reference_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, referenceID);

            return statement.executeUpdate() == 1;
        }
    }

    // CUSTOM QUERY: LIST REGISTERED REFERENCES

    public List<String> getAllReferences(
            Connection connection
    ) throws SQLException {

        List<String> references = new ArrayList<>();

        String sql = """
                SELECT reference_id
                FROM processed_requests
                ORDER BY processed_at DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                references.add(result.getString("reference_id"));
            }
        }

        return references;
    }
}
