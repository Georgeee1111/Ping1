import java.sql.*;
import java.sql.DriverManager;
import java.sql.SQLException;

public class CheckConnection {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1234";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        try {
            // Check if the JDBC driver is available
            Class.forName("org.postgresql.Driver");
            System.out.println("JDBC driver loaded successfully.");

            // Establish a connection to the database
            try (Connection connection = getConnection()) {
                System.out.println("Connection to the database established successfully.\n\n");
            } catch (SQLException e) {
                System.err.println("Error: Unable to connect to the database.");
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            System.err.println("Error: PostgreSQL JDBC driver not found.");
            e.printStackTrace();
        }
    }
}
