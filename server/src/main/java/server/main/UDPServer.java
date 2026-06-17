package server.main;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPServer implements Runnable {
    private static final int UDP_PORT = 5556;
    private static final String PING_MESSAGE = "PING_SERVER";
    private static final String RESPONSE_MESSAGE = "SERVER_STATUS: ACTIVE";
    private volatile boolean running = true;

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        System.out.println("[UDP] UDP Server dang khoi dong tren port " + UDP_PORT + "...");
        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            byte[] buffer = new byte[256];
            
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                if (PING_MESSAGE.equals(message)) {
                    byte[] responseBytes = RESPONSE_MESSAGE.getBytes("UTF-8");
                    InetAddress clientAddress = packet.getAddress();
                    int clientPort = packet.getPort();
                    
                    DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes, responseBytes.length, clientAddress, clientPort
                    );
                    socket.send(responsePacket);
                }
            }
        } catch (Exception e) {
            System.err.println("[UDP] Loi UDP Server: " + e.getMessage());
        }
    }
}
