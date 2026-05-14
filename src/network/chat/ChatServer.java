package network.chat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ChatServer {
    public static void main(String[] args) {

        try {
            ServerSocket serverSocket = new ServerSocket(5000);
            System.out.println("Waiting for client...");

            // get client socket
            Socket socket = serverSocket.accept();
            System.out.println("Client connected!");

            BufferedReader socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            // read thread
            ReadThread readHandlder = new ReadThread(socketInput);
            Thread t1 = new Thread(readHandlder);
            t1.start();

            // write thread
            while (true) {

                String msg = scanner.nextLine();

                // send msg via socket
                printWriter.println(msg);

                if (msg.equalsIgnoreCase("/quit")) {
                    System.out.println("[Server]: Disconnecting...");
                    break;
                }
            }

            socketInput.close();
            printWriter.close();
            scanner.close();
            socket.close();
            serverSocket.close();
        } catch (Exception e) {

            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
