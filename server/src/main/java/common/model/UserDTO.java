package common.model;

public class UserDTO {
    private int id;
    private String username;
    private String password;
    private String passwordHash;
    private String role;
    private String fullName;
    private String phone;
    private String status;

    public UserDTO() {}

    public UserDTO(int id, String username, String role, String fullName, String status) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
