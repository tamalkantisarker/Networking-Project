package com.securechat.server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class ServerController {

    @FXML
    private ListView<String> logListView;

    @FXML
    private ListView<String> networkHealthListView;

    @FXML
    private ListView<String> userListView;

    @FXML
    private Label ipLabel;

    private ServerState serverState;

    @FXML
    public void initialize() {
        serverState = ServerState.getInstance();

        // Register log callbacks
        serverState.setLogCallback(this::appendLog);
        serverState.setNetworkLogCallback(this::logNetworkEvent);
        serverState.setUserChangeCallback(this::updateUserList);

        appendLog("Server GUI Initialized.");
        updateUserList();
        displayServerIp();
    }

    private void displayServerIp() {
        try {
            StringBuilder ips = new StringBuilder("Server IP: ");
            boolean first = true;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().contains(":"))
                        continue; // Skip IPv6
                    if (!first)
                        ips.append(" | ");
                    ips.append(iface.getDisplayName()).append(": ").append(addr.getHostAddress());
                    first = false;
                }
            }
            Platform.runLater(() -> ipLabel.setText(ips.toString()));
        } catch (Exception e) {
            Platform.runLater(() -> ipLabel.setText("Server IP: Unknown"));
        }
    }

    public void logNetworkEvent(String event) {
        Platform.runLater(() -> {
            networkHealthListView.getItems().add(event);
            if (networkHealthListView.getItems().size() > 1000000) {
                networkHealthListView.getItems().remove(0);
            }
            networkHealthListView.scrollTo(networkHealthListView.getItems().size() - 1);
        });
    }

    public void appendLog(String message) {
        Platform.runLater(() -> {
            logListView.getItems().add(message);
            logListView.scrollTo(logListView.getItems().size() - 1);
        });
    }

    public void updateUserList() {
        Platform.runLater(() -> {
            userListView.getItems().clear();
            for (String user : serverState.getConnectedUsers().keySet()) {
                String status = serverState.getUserStatus(user);
                userListView.getItems().add(user + " (" + status + ")");
            }
        });
    }
}
