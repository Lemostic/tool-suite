package io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Data;

/**
 * 文件信息实体类
 * 用于展示和选择要下载的文件
 */
@Data
public class FileInfo {
    
    /**
     * 文件名
     */
    private final StringProperty fileName = new SimpleStringProperty();
    
    /**
     * 文件大小（可读格式）
     */
    private final StringProperty fileSize = new SimpleStringProperty();
    
    /**
     * 文件大小（字节）
     */
    private long sizeInBytes;
    
    /**
     * 文件在Jenkins工作区中的相对路径
     */
    private final StringProperty relativePath = new SimpleStringProperty();
    
    /**
     * 文件完整下载URL
     */
    private final StringProperty downloadUrl = new SimpleStringProperty();
    
    /**
     * 是否被选中下载
     */
    private final BooleanProperty selected = new SimpleBooleanProperty(true);
    
    /**
     * 下载状态
     */
    private final StringProperty status = new SimpleStringProperty("等待下载");
    
    public FileInfo() {}
    
    public FileInfo(String fileName, String fileSize, long sizeInBytes, 
                    String relativePath, String downloadUrl) {
        this.fileName.set(fileName);
        this.fileSize.set(fileSize);
        this.sizeInBytes = sizeInBytes;
        this.relativePath.set(relativePath);
        this.downloadUrl.set(downloadUrl);
    }
    
    // JavaFX属性访问方法
    public String getFileName() { return fileName.get(); }
    public void setFileName(String value) { fileName.set(value); }
    public StringProperty fileNameProperty() { return fileName; }
    
    public String getFileSize() { return fileSize.get(); }
    public void setFileSize(String value) { fileSize.set(value); }
    public StringProperty fileSizeProperty() { return fileSize; }
    
    public String getRelativePath() { return relativePath.get(); }
    public void setRelativePath(String value) { relativePath.set(value); }
    public StringProperty relativePathProperty() { return relativePath; }
    
    public String getDownloadUrl() { return downloadUrl.get(); }
    public void setDownloadUrl(String value) { downloadUrl.set(value); }
    public StringProperty downloadUrlProperty() { return downloadUrl; }
    
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean value) { selected.set(value); }
    public BooleanProperty selectedProperty() { return selected; }
    
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
    public StringProperty statusProperty() { return status; }
    
    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
