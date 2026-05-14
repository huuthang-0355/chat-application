package client;

import javax.swing.SwingUtilities;

import client.view.LoginView;

public class ClientApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView();

            loginView.showGUI();
        });
    }
}
