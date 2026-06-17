package client.service;

import common.model.Request;
import common.model.Response;
import common.util.ProtocolUtil;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

public class ServerConnector {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5555;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000; // 10s Read Timeout

    private static String sessionToken;
    private final Gson gson = new Gson();

    public static void setSessionToken(String token) {
        sessionToken = token;
    }

    public static void clearSessionToken() {
        sessionToken = null;
    }

    public static boolean hasSession() {
        return sessionToken != null && !sessionToken.isEmpty();
    }

    public Response sendRequest(Request request) {
        if (request.getToken() == null) {
            request.setToken(sessionToken);
        }
        
        // Tu dong sinh RequestId neu chua co
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            request.setRequestId(UUID.randomUUID().toString());
        }

        Socket socket = null;
        try {
            socket = new Socket();
            socket.setSoTimeout(READ_TIMEOUT); // Thiết lập timeout đọc
            socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), CONNECT_TIMEOUT);
            
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Sử dụng ProtocolUtil với Framing để ghi nhận tin cậy
            ProtocolUtil.writeMessage(out, gson.toJson(request));
            
            // Đọc kết quả bằng Framing
            String responseJson = ProtocolUtil.readMessage(in);
            return gson.fromJson(responseJson, Response.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Lỗi kết nối hoặc timeout từ server: " + e.getMessage(), null, request.getRequestId());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}