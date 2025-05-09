package restaurant_parallelproject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String DB_URL = "jdbc:sqlite:RestaurantSystem.db";

    public static void main(String[] args) {
        try {
            // Explicitly load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Establish the connection
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement()) {

                // Create Users Table
                String createUsersTable = "CREATE TABLE IF NOT EXISTS Users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL, "
                    + "password TEXT NOT NULL, "
                    + "role TEXT CHECK(role IN ('customer', 'chef')) NOT NULL"
                    + ");";

                // Create Orders Table
                String createOrdersTable = "CREATE TABLE IF NOT EXISTS Orders ("
                    + "order_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "item_name TEXT NOT NULL, "
                    + "quantity INTEGER NOT NULL, "
                    + "status TEXT CHECK(status IN ('Pending', 'Completed', 'Canceled')) DEFAULT 'Pending', "
                    + "customer_id INTEGER NOT NULL, "
                    + "FOREIGN KEY (customer_id) REFERENCES Users(id)"
                    + ");";

                // Create TransactionLog Table
                String createTransactionLogTable = "CREATE TABLE IF NOT EXISTS TransactionLog ("
                    + "log_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "action TEXT NOT NULL, "
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ");";

                // Execute the table creation statements
                stmt.execute(createUsersTable);
                stmt.execute(createOrdersTable);
                stmt.execute(createTransactionLogTable);

                System.out.println("Database and tables created successfully!");
            }

        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
