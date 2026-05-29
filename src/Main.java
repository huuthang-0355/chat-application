import javax.swing.SwingUtilities;
import server.view.ServerView;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ServerView().setVisible(true);
        });
    }
}