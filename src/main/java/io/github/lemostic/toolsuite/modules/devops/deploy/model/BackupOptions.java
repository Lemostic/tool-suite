package io.github.lemostic.toolsuite.modules.devops.deploy.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class BackupOptions {
    public enum BackupMode {
        ALL,           // 备份所有文件 (*)
        SELECTIVE,     // 选择性备份
        PATTERN        // 按模式匹配备份
    }
    
    private ObjectProperty<BackupMode> mode;
    private ListProperty<String> selectedFiles;
    private StringProperty pattern;
    private BooleanProperty deleteAfterBackup;
    
    public BackupOptions() {
        this.mode = new SimpleObjectProperty<>(BackupMode.ALL);
        this.selectedFiles = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.pattern = new SimpleStringProperty();
        this.deleteAfterBackup = new SimpleBooleanProperty(false);
    }
    
    // mode
    public BackupMode getMode() {
        return mode.get();
    }
    
    public void setMode(BackupMode mode) {
        this.mode.set(mode);
    }
    
    public ObjectProperty<BackupMode> modeProperty() {
        return mode;
    }
    
    // selectedFiles
    public ObservableList<String> getSelectedFiles() {
        return selectedFiles.get();
    }
    
    public void setSelectedFiles(ObservableList<String> selectedFiles) {
        this.selectedFiles.set(selectedFiles);
    }
    
    public ListProperty<String> selectedFilesProperty() {
        return selectedFiles;
    }
    
    // pattern
    public String getPattern() {
        return pattern.get();
    }
    
    public void setPattern(String pattern) {
        this.pattern.set(pattern);
    }
    
    public StringProperty patternProperty() {
        return pattern;
    }
    
    // deleteAfterBackup
    public Boolean getDeleteAfterBackup() {
        return deleteAfterBackup.get();
    }
    
    public void setDeleteAfterBackup(Boolean deleteAfterBackup) {
        this.deleteAfterBackup.set(deleteAfterBackup);
    }
    
    public BooleanProperty deleteAfterBackupProperty() {
        return deleteAfterBackup;
    }
}
