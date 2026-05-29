package client.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerConfigManager {
    private static final String FILE_PATH = "servers.config";

    public static class ServerItem {
        public String name;
        public String host;
        public int port;

        public ServerItem(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
        }

        @Override
        public String toString() {
            return name + " (" + host + ":" + port + ")";
        }
    }

    public static List<ServerItem> loadServers() {
        List<ServerItem> servers = new ArrayList<>();
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            // Write defaults
            servers.add(new ServerItem("Local Server", "localhost", 5000));
            servers.add(new ServerItem("Backup Server", "127.0.0.1", 5001));
            saveServers(servers);
            return servers;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(",", 3);
                if (parts.length == 3) {
                    try {
                        String name = parts[0].trim();
                        String host = parts[1].trim();
                        int port = Integer.parseInt(parts[2].trim());
                        servers.add(new ServerItem(name, host, port));
                    } catch (NumberFormatException e) {
                        System.out.println("[ServerConfigManager] Invalid port in config: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[ServerConfigManager] Error reading servers.config: " + e.getMessage());
        }

        // Safeguard if file is empty
        if (servers.isEmpty()) {
            servers.add(new ServerItem("Local Server", "localhost", 5000));
            servers.add(new ServerItem("Backup Server", "127.0.0.1", 5001));
            saveServers(servers);
        }

        return servers;
    }

    public static void saveServers(List<ServerItem> servers) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            bw.write("# Saved Server List Bookmarks for Java Chat Client\n");
            bw.write("# Format: ServerName,HostName,PortNumber\n");
            for (ServerItem item : servers) {
                bw.write(item.name + "," + item.host + "," + item.port + "\n");
            }
        } catch (IOException e) {
            System.out.println("[ServerConfigManager] Error writing servers.config: " + e.getMessage());
        }
    }
}
