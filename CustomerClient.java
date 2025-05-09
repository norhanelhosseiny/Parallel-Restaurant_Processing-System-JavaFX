package restaurant_parallelproject;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class CustomerClient extends Application {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int customerId;
    private Label messageLabel = new Label();

    // Predefined menu items
    private final String[] menuItems = {
        "MargheritaPizza",
        "Cheeseburger",
        "PastaCarbonara",
        "CaesarSalad",
        "GrilledChicken",
        "ChocolateCake",
        "SushiRoll",
        "Steak"
    };

    @Override
    public void start(Stage primaryStage) {
        connectToServer();

        VBox loginLayout = new VBox(10);
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Button loginButton = new Button("Login");

        loginLayout.getChildren().addAll(new Label("Username:"), usernameField, new Label("Password:"), passwordField, loginButton, messageLabel);
        Scene loginScene = new Scene(loginLayout, 300, 200);

        // Send login credentials to the server
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            sendCommand("LOGIN " + username + " " + password);
            String response = readResponse();
            if (response != null && response.startsWith("LOGIN_SUCCESS")) {
                String[] parts = response.split(" ");
                if (parts[1].equals("customer")) {
                    customerId = Integer.parseInt(parts[2]);
                    showOrderPanel(primaryStage);
                } else {
                    displayMessage("Not a customer account!", Color.RED);
                }
            } else {
                displayMessage("Invalid username or password", Color.RED);
            }
        });

        primaryStage.setTitle("Customer Login");
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private void showOrderPanel(Stage primaryStage) {
        VBox orderLayout = new VBox(10);

        ComboBox<String> itemComboBox = new ComboBox<>();
        itemComboBox.getItems().addAll(menuItems);
        itemComboBox.setPromptText("Select an item");

        TextField quantityField = new TextField();
        quantityField.setPromptText("Enter quantity");

        Button placeOrderButton = new Button("Place Order");
        Button deleteOrderButton = new Button("Delete Order");
        Button refreshButton = new Button("Refresh Orders");
        Button logoutButton = new Button("Logout");

        ListView<String> ordersList = new ListView<>();

        placeOrderButton.setOnAction(e -> {
            if (itemComboBox.getValue() == null || itemComboBox.getValue().isEmpty()) {
                displayMessage("Please select an item!", Color.ORANGE);
                return;
            }

            try {
                int qty = Integer.parseInt(quantityField.getText());
                placeOrder(itemComboBox.getValue(), qty);
                refreshOrders(ordersList);
            } catch (NumberFormatException ex) {
                displayMessage("Quantity must be a number", Color.RED);
            }
        });

        deleteOrderButton.setOnAction(e -> {
            String selected = ordersList.getSelectionModel().getSelectedItem();
            deleteOrder(selected);
            refreshOrders(ordersList);
        });

        refreshButton.setOnAction(e -> refreshOrders(ordersList));

        logoutButton.setOnAction(e -> {
            sendCommand("LOGOUT");
            readResponse(); // LOGOUT_SUCCESS expected
            closeConnection();
            primaryStage.close();
        });

        orderLayout.getChildren().addAll(
                new Label("Select Item:"), itemComboBox,
                new Label("Quantity:"), quantityField,
                placeOrderButton, deleteOrderButton, refreshButton, logoutButton,
                new Label("Active Orders:"), ordersList, messageLabel
        );
        Scene orderScene = new Scene(orderLayout, 400, 400);

        refreshOrders(ordersList);

        primaryStage.setTitle("Customer Panel");
        primaryStage.setScene(orderScene);
    }

    private void connectToServer() {
        try {
            // Connect to server
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

    private void placeOrder(String itemName, int quantity) {
        sendCommand("PLACE_ORDER " + itemName + " " + quantity + " " + customerId);
        String response = readResponse();
        if ("ORDER_PLACED".equals(response)) {
            displayMessage("Order placed successfully!", Color.GREEN);
        } else {
            displayMessage("Error placing order.", Color.RED);
        }
    }

    private void deleteOrder(String orderDetails) {
        if (orderDetails == null || orderDetails.isEmpty()) {
            displayMessage("No order selected!", Color.ORANGE);
            return;
        }

        int orderId = Integer.parseInt(orderDetails.split(",")[0].split(":")[1].trim());
        sendCommand("DELETE_ORDER " + orderId);
        String response = readResponse();
        if ("ORDER_DELETED".equals(response)) {
            displayMessage("Order deleted successfully!", Color.GREEN);
        } else {
            displayMessage("Error deleting order.", Color.RED);
        }
    }

    private void refreshOrders(ListView<String> ordersList) {
        sendCommand("FETCH_ORDERS");
        ordersList.getItems().clear();
        try {
            String line;
            while (!(line = in.readLine()).equals("END")) {
                ordersList.getItems().add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
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