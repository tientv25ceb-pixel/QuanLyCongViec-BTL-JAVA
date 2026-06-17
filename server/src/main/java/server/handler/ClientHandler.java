package server.handler;

import common.model.Request;
import common.model.Response;
import common.util.ProtocolUtil;
import com.google.gson.Gson;
import server.service.TaskService;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private TaskService taskService;
    private Gson gson;

    public ClientHandler(Socket socket, TaskService service) {
        this.clientSocket = socket;
        this.taskService = service;
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            clientSocket.setSoTimeout(15000); // 15s Read Timeout cho client socket
            
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();

            while (true) {
                String inputJson;
                try {
                    inputJson = ProtocolUtil.readMessage(in);
                } catch (SocketTimeoutException e) {
                    System.err.println("Timeout cho ket noi tu client (15 giay khong gui yeu cau): " + clientSocket.getInetAddress());
                    break;
                } catch (java.io.EOFException e) {
                    // Client ngắt kết nối bình thường
                    break;
                }

                Request request = gson.fromJson(inputJson, Request.class);
                if (request != null) {
                    try {
                        server.util.Logger.setRequestId(request.getRequestId());
                        
                        // Xử lý request
                        Response response = taskService.handleRequest(request);
                        
                        // Truyền và bảo toàn requestId từ request sang response
                        if (response != null) {
                            response.setRequestId(request.getRequestId());
                        }
                        
                        // Phản hồi sử dụng Framing
                        ProtocolUtil.writeMessage(out, gson.toJson(response));
                    } finally {
                        server.util.Logger.clearRequestId();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Loi ket noi tu client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
