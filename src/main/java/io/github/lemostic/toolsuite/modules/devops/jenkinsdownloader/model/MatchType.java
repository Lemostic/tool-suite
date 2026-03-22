package io.github.lemostic.toolsuite.modules.devops.jenkinsdownloader.model;

/**
 * 文件匹配类型枚举
 */
public enum MatchType {
    PREFIX("前缀匹配", "匹配文件名开头"),
    SUFFIX("后缀匹配", "匹配文件名结尾（包含扩展名）"),
    CONTAINS("包含匹配", "匹配文件名中包含的文本"),
    REGEX("正则匹配", "使用正则表达式匹配完整文件名"),
    EXACT("精确匹配", "完全匹配文件名");
    
    private final String displayName;
    private final String description;
    
    MatchType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
