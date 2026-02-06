package io.github.lemostic.toolsuite.modules.devops.deploy.model;

import javafx.beans.property.*;

public class UploadOptions {
    private BooleanProperty updatePermissions;
    private StringProperty filePermissions;      // 如 "755"
    private StringProperty owner;                // 所属用户
    private StringProperty group;                // 所属用户组
    private BooleanProperty preserveTimestamp;
    
    public UploadOptions() {
        this.updatePermissions = new SimpleBooleanProperty(false);
        this.filePermissions = new SimpleStringProperty("644");
        this.owner = new SimpleStringProperty();
        this.group = new SimpleStringProperty();
        this.preserveTimestamp = new SimpleBooleanProperty(false);
    }
    
    // updatePermissions
    public Boolean getUpdatePermissions() {
        return updatePermissions.get();
    }
    
    public void setUpdatePermissions(Boolean updatePermissions) {
        this.updatePermissions.set(updatePermissions);
    }
    
    public BooleanProperty updatePermissionsProperty() {
        return updatePermissions;
    }
    
    // filePermissions
    public String getFilePermissions() {
        return filePermissions.get();
    }
    
    public void setFilePermissions(String filePermissions) {
        this.filePermissions.set(filePermissions);
    }
    
    public StringProperty filePermissionsProperty() {
        return filePermissions;
    }
    
    // owner
    public String getOwner() {
        return owner.get();
    }
    
    public void setOwner(String owner) {
        this.owner.set(owner);
    }
    
    public StringProperty ownerProperty() {
        return owner;
    }
    
    // group
    public String getGroup() {
        return group.get();
    }
    
    public void setGroup(String group) {
        this.group.set(group);
    }
    
    public StringProperty groupProperty() {
        return group;
    }
    
    // preserveTimestamp
    public Boolean getPreserveTimestamp() {
        return preserveTimestamp.get();
    }
    
    public void setPreserveTimestamp(Boolean preserveTimestamp) {
        this.preserveTimestamp.set(preserveTimestamp);
    }
    
    public BooleanProperty preserveTimestampProperty() {
        return preserveTimestamp;
    }
}
