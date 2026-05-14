package network.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;

import network.protocol.Message;
import network.protocol.MessageParser;

public class ReadThread implements Runnable {

    private BufferedReader reader;
    private volatile boolean isRunning = true;

    public ReadThread(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public void run() {
        try {
            while (isRunning) {
                String line = reader.readLine();
                if (line == null)
                    break;

                Message msg = MessageParser.decode(line);
                if (msg == null)
                    continue;

                switch (msg.getType()) {
                    case LOGOUT:
                        isRunning = false;
                        System.out.println("Other side has disconnected.");
                        System.in.close();
                        return;

                    case MSG:
                        System.out.println("[" + msg.getSender() + "]: " + msg.getContent());
                        break;

                    case ERROR:
                        System.out.println("[ERROR]: " + msg.getContent());
                        break;
                    default:
                        break;
                }

            }
        } catch (SocketException e) {
            System.out.println("Connection closed.");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}
