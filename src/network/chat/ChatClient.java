// package network.chat;

// import java.io.BufferedReader;
// import java.io.InputStreamReader;
// import java.io.PrintWriter;
// import java.net.Socket;
// import java.util.Scanner;

// import network.protocol.Message;
// import network.protocol.MessageType;

// public class ChatClient {

// public static void main(String[] args) {
// int port = 5000;

// try {
// Socket socket = new Socket("localhost", port);
// System.out.println("Connected to server!");

// BufferedReader socketInput = new BufferedReader(new
// InputStreamReader(socket.getInputStream()));
// PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
// Scanner scanner = new Scanner(System.in);

// // Initially, client will first send username to server
// System.out.print("Enter your name: ");
// String username = scanner.nextLine();
// printWriter.println(username);

// // send the first LOGIN package to connect
// Message loginMsg = new Message(MessageType.LOGIN, username, "ALL", "");
// printWriter.println(MessageParser.encode(loginMsg));

// // read thread
// ReadThread readHandler = new ReadThread(socketInput);
// Thread t = new Thread(readHandler);
// t.start();

// // write thread
// while (true) {

// String input = scanner.nextLine();
// Message outgoingMsg;

// if (input.equalsIgnoreCase("/quit")) {
// System.out.println("[Client]: Disconnecting...");
// outgoingMsg = new Message(MessageType.LOGOUT, username, "ALL", "");

// printWriter.println(MessageParser.encode(outgoingMsg));
// break;
// }

// outgoingMsg = new Message(MessageType.MSG, username, "ALL", input);
// printWriter.println(MessageParser.encode(outgoingMsg));
// }

// socketInput.close();
// printWriter.close();
// scanner.close();
// socket.close();
// } catch (Exception e) {
// System.out.println("Something went wrong in client: " + e.getMessage());
// }
// }
// }
