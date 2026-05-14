import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleServer {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Waiting for Client connection...");

            Socket socket = serverSocket.accept();
            System.out.println("Connect Successfully");

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());

            for (int i = 0; i < 3; i++) {
                String msg = dis.readUTF();
                System.out.println("Client message: " + msg);

                dout.writeUTF((i + 1) + "");
                dout.flush();
            }

            dout.close();
            dis.close();
            serverSocket.close();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
