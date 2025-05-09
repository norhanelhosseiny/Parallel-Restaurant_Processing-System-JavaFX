package restaurant_parallelproject;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ChefClient extends Application {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int chefId;
    private Label messageLabel = new Label();

    @Override
    public void start(Stage primaryStage) {
        connectToServer();

        VBox loginLayout = new VBox(10);
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Button loginButton = new Button("Login");

        loginLayout.getChildren().addAll(new Label("Username:"), usernameField, new Label("Password:"), passwordField, loginButton, messageLabel);
        Scene loginScene = new Scene(loginLayout, 300, 200);

        // Send login credentials to server
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            sendCommand("LOGIN " + username + " " + password);
            String response = readResponse();
            if (response != null && response.startsWith("LOGIN_SUCCESS")) {
                String[] parts = response.split(" ");
                if (parts[1].equals("chef")) {
                    chefId = Integer.parseInt(parts[2]);
                    showOrderPanel(primaryStage);
                } else {
                    displayMessage("Not a chef account!", Color.RED);
                }
            } else {
                displayMessage("Invalid username or password", Color.RED);
            }
        });

        primaryStage.setTitle("Chef Login");
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private void showOrderPanel(Stage primaryStage) {
        VBox orderLayout = new VBox(10);
        ListView<String> ordersList = new ListView<>();
        Button markCompletedButton = new Button("Mark as Completed");
        Button cancelOrderButton = new Button("Cancel Order");
        Button refreshButton = new Button("Refresh Orders");
        Button logoutButton = new Button("Logout");

        markCompletedButton.setOnAction(e -> {
            String selected = ordersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                int orderId = Integer.parseInt(selected.split(",")[0].split(":")[1].trim());
                markOrderStatus(orderId, "MARK_COMPLETED");
                refreshOrders(ordersList);
            } else {
                displayMessage("No order selected!", Color.ORANGE);
            }
        });

        cancelOrderButton.setOnAction(e -> {
            String selected = ordersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                int orderId = Integer.parseInt(selected.split(",")[0].split(":")[1].trim());
                markOrderStatus(orderId, "CANCEL_ORDER");
                refreshOrders(ordersList);
            } else {
                displayMessage("No order selected!", Color.ORANGE);
            }
        });

        refreshButton.setOnAction(e -> refreshOrders(ordersList));

        logoutButton.setOnAction(e -> {
            sendCommand("LOGOUT");
            readResponse(); // LOGOUT_SUCCESS
            closeConnection();
            primaryStage.close();
        });

        orderLayout.getChildren().addAll(
                new Label("Pending Orders:"), ordersList,
                markCompletedButton, cancelOrderButton, refreshButton, logoutButton,
                messageLabel
        );
        Scene orderScene = new Scene(orderLayout, 400, 400);

        refreshOrders(ordersList);

        primaryStage.setTitle("Chef Panel");
        primaryStage.setScene(orderScene);
    }

    private void connectToServer() {
        try {
            // Connect to the server
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void sendCommand(String cmd) {
        out.println(cmd);
    }

    private String readResponse() {
        try {
            return in.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void refreshOrders(ListView<String> ordersList) {
        sendCommand("FETCH_ORDERS");
        ordersList.getItems().clear();
        try {
            String line;
            while (!(line = in.readLine()).equals("END")) {
                // Only add pending orders for the chef
                if (line.contains("Status: Pending")) {
                    ordersList.getItems().add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void markOrderStatus(int orderId, String command) {
        // MARK_COMPLETED orderId chefId or CANCEL_ORDER orderId chefId
        sendCommand(command + " " + orderId + " " + chefId);
        String response = readResponse();
        if ("ORDER_COMPLETED".equals(response) || "ORDER_CANCELED".equals(response)) {
            displayMessage("Order updated successfully!", Color.GREEN);
        } else {
            displayMessage("Error updating order.", Color.RED);
        }
    }

    private void displayMessage(String message, Color color) {
        messageLabel.setText(message);
        messageLabel.setTextFill(color);
    }

    private void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}