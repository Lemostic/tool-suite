package io.github.lemostic.toolsuite.util;

import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;

/**
 * UI工具类
 * 提供统一的UI组件配置方法
 */
public class UIUtil {
    
    // 空数据显示的统一样式
    private static final String EMPTY_DATA_STYLE = "-fx-text-fill: #999999; -fx-font-size: 14px;";
    private static final String EMPTY_DATA_TEXT = "暂无数据";
    
    /**
     * 为TableView设置空数据显示
     * @param tableView 表格视图
     */
    public static void setEmptyDataPlaceholder(TableView<?> tableView) {
        setEmptyDataPlaceholder(tableView, EMPTY_DATA_TEXT);
    }
    
    /**
     * 为TableView设置自定义空数据显示
     * @param tableView 表格视图
     * @param text 显示文本
     */
    public static void setEmptyDataPlaceholder(TableView<?> tableView, String text) {
        Label emptyLabel = new Label(text);
        emptyLabel.setStyle(EMPTY_DATA_STYLE);
        tableView.setPlaceholder(emptyLabel);
    }
    
    /**
     * 为ListView设置空数据显示
     * @param listView 列表视图
     */
    public static void setEmptyDataPlaceholder(ListView<?> listView) {
        setEmptyDataPlaceholder(listView, EMPTY_DATA_TEXT);
    }
    
    /**
     * 为ListView设置自定义空数据显示
     * @param listView 列表视图
     * @param text 显示文本
     */
    public static void setEmptyDataPlaceholder(ListView<?> listView, String text) {
        Label emptyLabel = new Label(text);
        emptyLabel.setStyle(EMPTY_DATA_STYLE);
        listView.setPlaceholder(emptyLabel);
    }
    
    /**
     * 创建空数据标签
     * @return 空数据标签
     */
    public static Label createEmptyDataLabel() {
        return createEmptyDataLabel(EMPTY_DATA_TEXT);
    }
    
    /**
     * 创建自定义空数据标签
     * @param text 显示文本
     * @return 空数据标签
     */
    public static Label createEmptyDataLabel(String text) {
        Label label = new Label(text);
        label.setStyle(EMPTY_DATA_STYLE);
        return label;
    }
    
    /**
     * 获取空数据显示的默认文本
     * @return 默认文本
     */
    public static String getEmptyDataText() {
        return EMPTY_DATA_TEXT;
    }
    
    /**
     * 获取空数据显示的默认样式
     * @return 默认样式
     */
    public static String getEmptyDataStyle() {
        return EMPTY_DATA_STYLE;
    }
}
