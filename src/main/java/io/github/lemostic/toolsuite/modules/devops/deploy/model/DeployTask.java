package io.github.lemostic.toolsuite.modules.devops.deploy.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDateTime;
import java.util.UUID;

public class DeployTask {
    public enum TaskStatus {
        PENDING, CONNECTING, STOPPING_SERVICE, BACKING_UP, 
        UPLOADING, STARTING_SERVICE, COMPLETED, FAILED, CANCELLED
    }
    
    private StringProperty id;
    private ObjectProperty<ServerConfigDTO> server;
    private StringProperty packagePath;
    private ObjectProperty<TaskStatus> status;
    private StringProperty currentStep;
    private DoubleProperty progress;
    private ObservableList<String> logs;
    private ObjectProperty<LocalDateTime> startTime;
    private ObjectProperty<LocalDateTime> endTime;
    private StringProperty errorMessage;
    private ObjectProperty<BackupOptions> backupOptions;
    private ObjectProperty<UploadOptions> uploadOptions;
    
    public DeployTask() {
        this.id = new SimpleStringProperty(UUID.randomUUID().toString());
        this.server = new SimpleObjectProperty<>();
        this.packagePath = new SimpleStringProperty();
        this.status = new SimpleObjectProperty<>(TaskStatus.PENDING);
        this.currentStep = new SimpleStringProperty();
        this.progress = new SimpleDoubleProperty(0.0);
        this.logs = FXCollections.observableArrayList();
        this.startTime = new SimpleObjectProperty<>();
        this.endTime = new SimpleObjectProperty<>();
        this.errorMessage = new SimpleStringProperty();
        this.backupOptions = new SimpleObjectProperty<>(new BackupOptions());
        this.uploadOptions = new SimpleObjectProperty<>(new UploadOptions());
    }
    
    // id
    public String getId() {
        return id.get();
    }
    
    public void setId(String id) {
        this.id.set(id);
    }
    
    public StringProperty idProperty() {
        return id;
    }
    
    // server
    public ServerConfigDTO getServer() {
        return server.get();
    }
    
    public void setServer(ServerConfigDTO server) {
        this.server.set(server);
    }
    
    public ObjectProperty<ServerConfigDTO> serverProperty() {
        return server;
    }
    
    // packagePath
    public String getPackagePath() {
        return packagePath.get();
    }
    
    public void setPackagePath(String packagePath) {
        this.packagePath.set(packagePath);
    }
    
    public StringProperty packagePathProperty() {
        return packagePath;
    }
    
    // status
    public TaskStatus getStatus() {
        return status.get();
    }
    
    public void setStatus(TaskStatus status) {
        this.status.set(status);
    }
    
    public ObjectProperty<TaskStatus> statusProperty() {
        return status;
    }
    
    // currentStep
    public String getCurrentStep() {
        return currentStep.get();
    }
    
    public void setCurrentStep(String currentStep) {
        this.currentStep.set(currentStep);
    }
    
    public StringProperty currentStepProperty() {
        return currentStep;
    }
    
    // progress
    public Double getProgress() {
        return progress.get();
    }
    
    public void setProgress(Double progress) {
        this.progress.set(progress);
    }
    
    public DoubleProperty progressProperty() {
        return progress;
    }
    
    // logs
    public ObservableList<String> getLogs() {
        return logs;
    }
    
    public void setLogs(ObservableList<String> logs) {
        this.logs = logs;
    }
    
    // startTime
    public LocalDateTime getStartTime() {
        return startTime.get();
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime.set(startTime);
    }
    
    public ObjectProperty<LocalDateTime> startTimeProperty() {
        return startTime;
    }
    
    // endTime
    public LocalDateTime getEndTime() {
        return endTime.get();
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime.set(endTime);
    }
    
    public ObjectProperty<LocalDateTime> endTimeProperty() {
        return endTime;
    }
    
    // errorMessage
    public String getErrorMessage() {
        return errorMessage.get();
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage.set(errorMessage);
    }
    
    public StringProperty errorMessageProperty() {
        return errorMessage;
    }
    
    // backupOptions
    public BackupOptions getBackupOptions() {
        return backupOptions.get();
    }
    
    public void setBackupOptions(BackupOptions backupOptions) {
        this.backupOptions.set(backupOptions);
    }
    
    public ObjectProperty<BackupOptions> backupOptionsProperty() {
        return backupOptions;
    }
    
    // uploadOptions
    public UploadOptions getUploadOptions() {
        return uploadOptions.get();
    }
    
    public void setUploadOptions(UploadOptions uploadOptions) {
        this.uploadOptions.set(uploadOptions);
    }
    
    public ObjectProperty<UploadOptions> uploadOptionsProperty() {
        return uploadOptions;
    }
    
    // 辅助方法
    public void addLog(String log) {
        this.logs.add("[" + LocalDateTime.now() + "] " + log);
    }
    
    public void clearLogs() {
        this.logs.clear();
    }
}
