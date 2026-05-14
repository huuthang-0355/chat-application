import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class SimpleClient {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 12345);

            // open dout to write data down socket
            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            for (int i = 0; i < 3; i++) {
                dout.writeUTF("Hello Server");
                dout.flush();
                String response = dis.readUTF();
                System.out.println("Server response: " + response);
            }

            dis.close();
            dout.close();
            socket.close();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}