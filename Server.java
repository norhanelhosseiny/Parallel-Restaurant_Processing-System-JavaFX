package restaurant_parallelproject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Application {

    private static final int PORT = 12345;
    private static final String DB_URL = "jdbc:sqlite:RestaurantSystem.db";
    private TextArea logArea;
    private ListView<String> ordersList;

    @Override
    public void start(Stage primaryStage) {
        // GUI Setup
        logArea = new TextArea();
        logArea.setEditable(false);
        ordersList = new ListView<>();

        VBox root = new VBox(10, new Label("Active Orders:"), ordersList, new Label("Transaction Log:"), logArea);
        Scene scene = new Scene(root, 400, 600);

        primaryStage.setTitle("Server");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start the server on a separate thread
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(this::startServer);

        // Update the orders list initially
        Platform.runLater(this::updateOrdersList);
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logMessage("Server started on port " + PORT);

            // Concurrency: For each client, start a new ClientHandler thread
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logMessage("Client connected: " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            logMessage("Error: " + e.getMessage());
        }
    }

    private void logMessage(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
        });
    }

    private void updateOrdersList() {
        Platform.runLater(() -> {
            ordersList.getItems().clear();
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM Orders")) {

                while (rs.next()) {
                    int orderId = rs.getInt("order_id");
                    String itemName = rs.getString("item_name");
                    int quantity = rs.getInt("quantity");
                    String status = rs.getString("status");
                    ordersList.getItems().add("Order ID: " + orderId + ", Item: " + itemName + ", Quantity: " + quantity + ", Status: " + status);
                }
            } catch (SQLException e) {
                logMessage("Error fetching orders: " + e.getMessage());
            }
        });
    }

    private class ClientHandler extends Thread {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split(" ");
                    String command = parts[0].toUpperCase();

                    if ("LOGOUT".equals(command)) {
                        out.println("LOGOUT_SUCCESS");
                        break; // End this client's loop
                    }

                    switch (command) {
                        case "LOGIN":
                            // Send login credentials: LOGIN username password
                            if (parts.length == 3) {
                                String username = parts[1];
                                String password = parts[2];
                                // Validate login against DB
                                String role = validateLogin(username, password);
                                if (role != null) {
                                    out.println("LOGIN_SUCCESS " + role + " " + getUserId(username));
                                } else {
                                    out.println("LOGIN_FAILURE");
                                }
                            } else {
                                out.println("LOGIN_FAILURE");
                            }
                            break;

                        case "PLACE_ORDER":
                            // PLACE_ORDER itemName quantity customerId
                            if (parts.length >= 4) {
                                String itemName = parts[1];
                                int quantity = Integer.parseInt(parts[2]);
                                int customerId = Integer.parseInt(parts[3]);
                                if (placeOrder(itemName, quantity, customerId)) {
                                    out.println("ORDER_PLACED");
                                    updateOrdersList();
                                } else {
                                    out.println("ORDER_ERROR");
                                }
                            } else {
                                out.println("ORDER_ERROR");
                            }
                            break;

                        case "DELETE_ORDER":
                            // DELETE_ORDER orderId
                            if (parts.length == 2) {
                                int orderId = Integer.parseInt(parts[1]);
                                if (deleteOrder(orderId)) {
                                    out.println("ORDER_DELETED");
                                    updateOrdersList();
                                } else {
                                    out.println("DELETE_ERROR");
                                }
                            } else {
                                out.println("DELETE_ERROR");
                            }
                            break;

                        case "FETCH_ORDERS":
                            // FETCH_ORDERS
                            List<String> orders = getActiveOrders();
                            for (String o : orders) {
                                out.println(o);
                            }
                            out.println("END");
                            break;

                        case "MARK_COMPLETED":
                            // MARK_COMPLETED orderId chefId
                            if (parts.length == 3) {
                                int orderId = Integer.parseInt(parts[1]);
                                int chefId = Integer.parseInt(parts[2]);
                                if (updateOrderStatus(orderId, "Completed", chefId)) {
                                    out.println("ORDER_COMPLETED");
                                    updateOrdersList();
                                } else {
                                    out.println("ERROR_COMPLETING");
                                }
                            } else {
                                out.println("ERROR_COMPLETING");
                            }
                            break;

                        case "CANCEL_ORDER":
                            // CANCEL_ORDER orderId chefId
                            if (parts.length == 3) {
                                int orderId = Integer.parseInt(parts[1]);
                                int chefId = Integer.parseInt(parts[2]);
                                if (updateOrderStatus(orderId, "Canceled", chefId)) {
                                    out.println("ORDER_CANCELED");
                                    updateOrdersList();
                                } else {
                                    out.println("ERROR_CANCELING");
                                }
                            } else {
                                out.println("ERROR_CANCELING");
                            }
                            break;

                        default:
                            out.println("UNKNOWN_COMMAND");
                            break;
                    }
                }

            } catch (IOException e) {
                logMessage("Client disconnected: " + e.getMessage());
            }
        }
    }

    // Database helper methods

    private String validateLogin(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT role FROM Users WHERE username = ? AND password = ?")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            logMessage("Login error: " + e.getMessage());
        }
        return null;
    }

    private int getUserId(String username) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM Users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            logMessage("Error getting user ID: " + e.getMessage());
        }
        return -1;
    }

    private boolean placeOrder(String itemName, int quantity, int customerId) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Orders (item_name, quantity, customer_id, status) VALUES (?, ?, ?, 'Pending')")) {
            pstmt.setString(1, itemName);
            pstmt.setInt(2, quantity);
            pstmt.setInt(3, customerId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logMessage("Error placing order: " + e.getMessage());
        }
        return false;
    }

    private boolean deleteOrder(int orderId) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Orders WHERE order_id = ?")) {
            pstmt.setInt(1, orderId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logMessage("Error deleting order: " + e.getMessage());
        }
        return false;
    }

    private List<String> getActiveOrders() {
        List<String> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Orders")) {

            while (rs.next()) {
                int orderId = rs.getInt("order_id");
                String itemName = rs.getString("item_name");
                int quantity = rs.getInt("quantity");
                String status = rs.getString("status");
                result.add("Order ID: " + orderId + ", Item: " + itemName + ", Quantity: " + quantity + ", Status: " + status);
            }
        } catch (SQLException e) {
            logMessage("Error fetching orders: " + e.getMessage());
        }
        return result;
    }

    private boolean updateOrderStatus(int orderId, String newStatus, int chefId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE Orders SET status = ? WHERE order_id = ?")) {
                pstmt.setString(1, newStatus);
                pstmt.setInt(2, orderId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement logStmt = conn.prepareStatement("INSERT INTO TransactionLog (action) VALUES (?)")) {
                logStmt.setString(1, "Order ID " + orderId + " marked as " + newStatus + " by Chef ID " + chefId);
                logStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            logMessage("Error updating order: " + e.getMessage());
        }
        return false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}