package client.service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClientUtil {
    private static final String SERVER_HOST = "localhost";
    private static final int UDP_PORT = 5556;
    private static final String PING_MESSAGE = "PING_SERVER";
    private static final String EXPECTED_RESPONSE = "SERVER_STATUS: ACTIVE";
    private static final int TIMEOUT_MS = 600; // Timeout nhanh cho UDP

    /**
     * Kiểm tra trạng thái hoạt động của Server thông qua UDP.
     * @return true nếu Server phản hồi hoạt động, ngược lại false.
     */
    public static boolean checkServerOnline() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            
            byte[] requestData = PING_MESSAGE.getBytes("UTF-8");
            InetAddress serverAddr = InetAddress.getByName(SERVER_HOST);
            DatagramPacket requestPacket = new DatagramPacket(
                requestData, requestData.length, serverAddr, UDP_PORT
            );
            
            // Gửi ping
            socket.send(requestPacket);
            
            // Chờ nhận phản hồi
            byte[] buffer = new byte[256];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);
            
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength()).trim();
            return EXPECTED_RESPONSE.equals(response);
        } catch (Exception e) {
            // Có lỗi (như SocketTimeoutException) tức là Server offline hoặc bị chặn
            return false;
        }
    }
}
