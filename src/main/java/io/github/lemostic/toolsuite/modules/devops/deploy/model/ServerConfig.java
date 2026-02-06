package io.github.lemostic.toolsuite.modules.devops.deploy.model;

import javafx.beans.property.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServerConfig {
    private LongProperty id;
    private StringProperty name;
    private StringProperty host;
    private IntegerProperty port;
    private StringProperty username;
    private StringProperty password;
    private StringProperty appDirectory;
    private StringProperty backupDirectory;
    private StringProperty binDirectory;
    private StringProperty stopScript;
    private StringProperty startScript;
    private StringProperty description;
    private ObjectProperty<LocalDateTime> createdAt;
    private ObjectProperty<LocalDateTime> updatedAt;
    private BooleanProperty enabled;
    
    public ServerConfig() {
        this.id = new SimpleLongProperty();
        this.name = new SimpleStringProperty();
        this.host = new SimpleStringProperty();
        this.port = new SimpleIntegerProperty(22);
        this.username = new SimpleStringProperty();
        this.password = new SimpleStringProperty();
        this.appDirectory = new SimpleStringProperty();
        this.backupDirectory = new SimpleStringProperty();
        this.binDirectory = new SimpleStringProperty("bin");
        this.stopScript = new SimpleStringProperty("stop.sh");
        this.startScript = new SimpleStringProperty("start.sh");
        this.description = new SimpleStringProperty();
        this.createdAt = new SimpleObjectProperty<>(LocalDateTime.now());
        this.updatedAt = new SimpleObjectProperty<>(LocalDateTime.now());
        this.enabled = new SimpleBooleanProperty(true);
    }
    
    // id
    public Long getId() {
        return id.get();
    }
    
    public void setId(Long id) {
        this.id.set(id);
    }
    
    public LongProperty idProperty() {
        return id;
    }
    
    // name
    public String getName() {
        return name.get();
    }
    
    public void setName(String name) {
        this.name.set(name);
    }
    
    public StringProperty nameProperty() {
        return name;
    }
    
    // host
    public String getHost() {
        return host.get();
    }
    
    public void setHost(String host) {
        this.host.set(host);
    }
    
    public StringProperty hostProperty() {
        return host;
    }
    
    // port
    public Integer getPort() {
        return port.get();
    }
    
    public void setPort(Integer port) {
        this.port.set(port);
    }
    
    public IntegerProperty portProperty() {
        return port;
    }
    
    // username
    public String getUsername() {
        return username.get();
    }
    
    public void setUsername(String username) {
        this.username.set(username);
    }
    
    public StringProperty usernameProperty() {
        return username;
    }
    
    // password
    public String getPassword() {
        return password.get();
    }
    
    public void setPassword(String password) {
        this.password.set(password);
    }
    
    public StringProperty passwordProperty() {
        return password;
    }
    
    // appDirectory
    public String getAppDirectory() {
        return appDirectory.get();
    }
    
    public void setAppDirectory(String appDirectory) {
        this.appDirectory.set(appDirectory);
    }
    
    public StringProperty appDirectoryProperty() {
        return appDirectory;
    }
    
    // backupDirectory
    public String getBackupDirectory() {
        return backupDirectory.get();
    }
    
    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory.set(backupDirectory);
    }
    
    public StringProperty backupDirectoryProperty() {
        return backupDirectory;
    }
    
    // binDirectory
    public String getBinDirectory() {
        return binDirectory.get();
    }
    
    public void setBinDirectory(String binDirectory) {
        this.binDirectory.set(binDirectory);
    }
    
    public StringProperty binDirectoryProperty() {
        return binDirectory;
    }
    
    // stopScript
    public String getStopScript() {
        return stopScript.get();
    }
    
    public void setStopScript(String stopScript) {
        this.stopScript.set(stopScript);
    }
    
    public StringProperty stopScriptProperty() {
        return stopScript;
    }
    
    // startScript
    public String getStartScript() {
        return startScript.get();
    }
    
    public void setStartScript(String startScript) {
        this.startScript.set(startScript);
    }
    
    public StringProperty startScriptProperty() {
        return startScript;
    }
    
    // description
    public String getDescription() {
        return description.get();
    }
    
    public void setDescription(String description) {
        this.description.set(description);
    }
    
    public StringProperty descriptionProperty() {
        return description;
    }
    
    // createdAt
    public LocalDateTime getCreatedAt() {
        return createdAt.get();
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt.set(createdAt);
    }
    
    public ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }
    
    // updatedAt
    public LocalDateTime getUpdatedAt() {
        return updatedAt.get();
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt.set(updatedAt);
    }
    
    public ObjectProperty<LocalDateTime> updatedAtProperty() {
        return updatedAt;
    }
    
    // enabled
    public Boolean getEnabled() {
        return enabled.get();
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled.set(enabled);
    }
    
    public BooleanProperty enabledProperty() {
        return enabled;
    }
}
