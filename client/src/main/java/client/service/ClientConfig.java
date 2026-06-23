package client.service;

import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {
    private static String serverHost = "localhost";
    private static int tcpPort = 5555;
    private static int udpPort = 5556;

    static {
        try (InputStream input = ClientConfig.class.getClassLoader().getResourceAsStream("client.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                serverHost = prop.getProperty("server.host", "localhost");
                tcpPort = Integer.parseInt(prop.getProperty("server.port.tcp", "5555"));
                udpPort = Integer.parseInt(prop.getProperty("server.port.udp", "5556"));
            }
        } catch (Exception e) {
            System.err.println("[CONFIG] Loi doc client.properties, su dung cau hinh mac dinh.");
        }
    }

    public static String getServerHost() {
        return serverHost;
    }

    public static int getTcpPort() {
        return tcpPort;
    }

    public static int getUdpPort() {
        return udpPort;
    }
}
