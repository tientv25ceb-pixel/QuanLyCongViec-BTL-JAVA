package common.model;

public class GroupDTO {
    private int id;
    private String groupName;
    private String description;
    private int createdBy;
    private String createdByName;

    public GroupDTO() {}

    public GroupDTO(int id, String groupName, String description, int createdBy) {
        this.id = id;
        this.groupName = groupName;
        this.description = description;
        this.createdBy = createdBy;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
}
