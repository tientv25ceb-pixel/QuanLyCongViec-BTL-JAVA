package client.model;

public class Request {
    private String action;
    private Object data;
    private String token;
    private String requestId;

    public Request() {}

    public Request(String action, Object data) {
        this.action = action;
        this.data = data;
    }

    public Request(String action, Object data, String requestId) {
        this.action = action;
        this.data = data;
        this.requestId = requestId;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
