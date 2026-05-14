package client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import client.controller.ChatController;
import network.protocol.Message;
import network.protocol.MessageParser;

public class NetworkService {

    private ChatController controller;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = true;

    public NetworkService(String host, int port, ChatController chatController)
            throws IOException, UnknownHostException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true); // autoFlush: sends immediately
        this.controller = chatController;
    }

    public void start() {
        // read thread to prevent blocking UI
        Thread readThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    Message msg = MessageParser.decode(line);

                    if (msg != null) {
                        // inform controller to handle new messages arriving
                        controller.onMessageReceived(msg);
                    }
                }
            } catch (IOException e) {
                if (running)
                    System.out.println("Lose connection to server!");
            }
        });

        readThread.setDaemon(true); // thread dead automatically when user close app.
        readThread.start();
    }

    public void send(Message msg) {
        out.println(MessageParser.encode(msg));
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
