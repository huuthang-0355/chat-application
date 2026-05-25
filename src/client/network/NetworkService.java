package client.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import client.controller.ChatController;
import common.protocol.Message;

public class NetworkService {

    private ChatController controller;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private volatile boolean running = true;

    public NetworkService(String host, int port, ChatController chatController)
            throws IOException, UnknownHostException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();

        in = new ObjectInputStream(socket.getInputStream());

        this.controller = chatController;
    }

    public void start() {
        // read thread to prevent blocking UI
        Thread readThread = new Thread(() -> {
            try {
                Message msg;
                while (running && (msg = (Message) in.readObject()) != null) {

                    // inform controller to handle new messages arriving
                    controller.onMessageReceived(msg);

                }
            } catch (Exception e) {
                if (running)
                    System.out.println("Lose connection to server!");
            }
        });

        readThread.setDaemon(true); // thread dead automatically when user close app.
        readThread.start();
    }

    public void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
