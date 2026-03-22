package io.github.lemostic.toolsuite.modules.convert.xmljson;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.XML;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.concurrent.CompletableFuture;

/**
 * XML与JSON转换服务类
 * 使用Hutool工具类实现转换逻辑
 */
public class XmlJsonConverterService {

    private final StringProperty statusMessage = new SimpleStringProperty("就绪");
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    /**
     * 将XML字符串转换为JSON字符串
     *
     * @param xmlString XML字符串
     * @param prettyPrint 是否格式化输出
     * @return 转换后的JSON字符串
     */
    public CompletableFuture<String> xmlToJsonAsync(String xmlString, boolean prettyPrint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                updateProgress(0.3);
                statusMessage.set("正在解析XML...");

                // 使用Hutool的XML转JSON功能
                JSONObject jsonObject = XML.toJSONObject(xmlString);

                updateProgress(0.7);
                statusMessage.set("正在格式化JSON...");

                String result;
                if (prettyPrint) {
                    result = JSONUtil.toJsonPrettyStr(jsonObject);
                } else {
                    result = jsonObject.toString();
                }

                updateProgress(1.0);
                statusMessage.set("XML转JSON完成");

                return result;
            } catch (Exception e) {
                updateProgress(0);
                statusMessage.set("转换失败: " + e.getMessage());
                throw new RuntimeException("XML解析失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 将JSON字符串转换为XML字符串
     *
     * @param jsonString JSON字符串
     * @param prettyPrint 是否格式化输出
     * @return 转换后的XML字符串
     */
    public CompletableFuture<String> jsonToXmlAsync(String jsonString, boolean prettyPrint) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                updateProgress(0.3);
                statusMessage.set("正在解析JSON...");

                // 先解析JSON判断类型
                Object jsonObj = JSONUtil.parse(jsonString);

                updateProgress(0.6);
                statusMessage.set("正在转换为XML...");

                String result;
                if (jsonObj instanceof JSONObject) {
                    result = JSONUtil.toXmlStr((JSONObject) jsonObj);
                } else if (jsonObj instanceof JSONArray) {
                    // 如果是数组，包装在一个根元素中
                    JSONObject wrapper = new JSONObject();
                    wrapper.set("root", jsonObj);
                    result = JSONUtil.toXmlStr(wrapper);
                } else {
                    // 基本类型，包装处理
                    JSONObject wrapper = new JSONObject();
                    wrapper.set("value", jsonObj);
                    result = JSONUtil.toXmlStr(wrapper);
                }

                updateProgress(0.9);
                statusMessage.set("正在格式化XML...");

                // 格式化XML
                if (prettyPrint) {
                    result = formatXml(result);
                }

                updateProgress(1.0);
                statusMessage.set("JSON转XML完成");

                return result;
            } catch (Exception e) {
                updateProgress(0);
                statusMessage.set("转换失败: " + e.getMessage());
                throw new RuntimeException("JSON解析失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 格式化XML字符串
     */
    private String formatXml(String xml) {
        StringBuilder formatted = new StringBuilder();
        String[] lines = xml.split(">");
        int indent = 0;

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            // 减少缩进的情况（结束标签）
            if (line.trim().startsWith("</") && indent > 0) {
                indent--;
            }

            // 添加缩进
            for (int i = 0; i < indent; i++) {
                formatted.append("  ");
            }

            formatted.append(line.trim());

            // 如果是自闭合标签或结束标签，不换行
            if (line.trim().endsWith("/") || line.trim().startsWith("</")) {
                formatted.append(">\n");
            } else if (line.contains("</")) {
                // 一行内的闭合标签
                formatted.append(">\n");
            } else {
                // 开始标签
                formatted.append(">\n");
                if (!line.trim().endsWith("/")) {
                    indent++;
                }
            }
        }

        return formatted.toString().trim();
    }

    /**
     * 验证XML格式是否有效
     */
    public boolean isValidXml(String xmlString) {
        if (xmlString == null || xmlString.trim().isEmpty()) {
            return false;
        }
        try {
            XML.toJSONObject(xmlString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证JSON格式是否有效
     */
    public boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        try {
            JSONUtil.parse(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateProgress(double value) {
        progress.set(value);
    }

    public void resetStatus() {
        statusMessage.set("就绪");
        progress.set(0);
    }
}
