//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import network.multiClient.MultiChatServer;

public class Main {
    public static void main(String[] args) {

        MultiChatServer server = new MultiChatServer(5000);

        server.startServer();
    }
}