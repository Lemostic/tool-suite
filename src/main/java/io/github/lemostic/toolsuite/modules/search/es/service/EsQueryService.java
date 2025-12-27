package io.github.lemostic.toolsuite.modules.search.es.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Elasticsearch查询服务
 */
public class EsQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(EsQueryService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    
    private final StringProperty statusMessage = new SimpleStringProperty("就绪");
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    
    public EsQueryService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    public DoubleProperty progressProperty() {
        return progress;
    }
    
    /**
     * 连接信息
     */
    public static class ConnectionInfo {
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final boolean useHttps;
        
        public ConnectionInfo(String host, int port, String username, String password, boolean useHttps) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.useHttps = useHttps;
        }
        
        public String getBaseUrl() {
            return (useHttps ? "https://" : "http://") + host + ":" + port;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getPassword() {
            return password;
        }
    }
    
    /**
     * 测试连接
     */
    public boolean testConnection(ConnectionInfo conn) throws Exception {
        String url = conn.getBaseUrl() + "/";
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(5));
        
        if (conn.getUsername() != null && !conn.getUsername().isEmpty()) {
            String auth = conn.getUsername() + ":" + conn.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            requestBuilder.header("Authorization", "Basic " + encodedAuth);
        }
        
        HttpResponse<String> response = httpClient.send(requestBuilder.build(), 
                HttpResponse.BodyHandlers.ofString());
        
        return response.statusCode() == 200;
    }
    
    /**
     * 获取所有索引列表
     */
    public List<String> getIndices(ConnectionInfo conn) throws Exception {
        String url = conn.getBaseUrl() + "/_cat/indices?format=json";
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10));
        
        if (conn.getUsername() != null && !conn.getUsername().isEmpty()) {
            String auth = conn.getUsername() + ":" + conn.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            requestBuilder.header("Authorization", "Basic " + encodedAuth);
        }
        
        HttpResponse<String> response = httpClient.send(requestBuilder.build(), 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("获取索引列表失败: " + response.statusCode());
        }
        
        JsonNode jsonArray = objectMapper.readTree(response.body());
        List<String> indices = new ArrayList<>();
        jsonArray.forEach(node -> indices.add(node.get("index").asText()));
        
        return indices;
    }
    
    /**
     * 获取索引的字段映射
     */
    public List<String> getIndexFields(ConnectionInfo conn, String index) throws Exception {
        String url = conn.getBaseUrl() + "/" + index + "/_mapping";
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10));
        
        if (conn.getUsername() != null && !conn.getUsername().isEmpty()) {
            String auth = conn.getUsername() + ":" + conn.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            requestBuilder.header("Authorization", "Basic " + encodedAuth);
        }
        
        HttpResponse<String> response = httpClient.send(requestBuilder.build(), 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("获取字段映射失败: " + response.statusCode());
        }
        
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode properties = root.get(index).get("mappings").get("properties");
        
        List<String> fields = new ArrayList<>();
        properties.fieldNames().forEachRemaining(fields::add);
        
        return fields;
    }
    
    /**
     * 执行查询
     */
    public QueryResult executeQuery(ConnectionInfo conn, String index, String queryJson) throws Exception {
        String url = conn.getBaseUrl() + "/" + index + "/_search";
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(queryJson))
                .timeout(Duration.ofSeconds(30));
        
        if (conn.getUsername() != null && !conn.getUsername().isEmpty()) {
            String auth = conn.getUsername() + ":" + conn.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            requestBuilder.header("Authorization", "Basic " + encodedAuth);
        }
        
        HttpResponse<String> response = httpClient.send(requestBuilder.build(), 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("查询失败: " + response.statusCode() + " - " + response.body());
        }
        
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode hits = root.get("hits");
        int total = hits.get("total").get("value").asInt();
        
        List<Map<String, Object>> documents = new ArrayList<>();
        hits.get("hits").forEach(hit -> {
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("_id", hit.get("_id").asText());
            
            JsonNode source = hit.get("_source");
            source.fieldNames().forEachRemaining(fieldName -> {
                JsonNode value = source.get(fieldName);
                doc.put(fieldName, nodeToObject(value));
            });
            
            documents.add(doc);
        });
        
        return new QueryResult(total, documents);
    }
    
    private Object nodeToObject(JsonNode node) {
        if (node.isNull()) {
            return null;
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isNumber()) {
            return node.numberValue();
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(n -> list.add(nodeToObject(n)));
            return list;
        } else if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.fieldNames().forEachRemaining(fieldName -> 
                map.put(fieldName, nodeToObject(node.get(fieldName))));
            return map;
        }
        return node.toString();
    }
    
    /**
     * 导出到Excel
     */
    public void exportToExcel(List<Map<String, Object>> data, List<String> selectedColumns, 
                             File outputFile) throws IOException {
        statusMessage.set("正在导出Excel...");
        progress.set(0);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("ES Data");
            
            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < selectedColumns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(selectedColumns.get(i));
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            int totalRows = data.size();
            for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                Map<String, Object> doc = data.get(rowIdx);
                
                for (int colIdx = 0; colIdx < selectedColumns.size(); colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    String columnName = selectedColumns.get(colIdx);
                    Object value = doc.get(columnName);
                    
                    if (value == null) {
                        cell.setCellValue("");
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
                
                // 更新进度
                double progressValue = (double) (rowIdx + 1) / totalRows;
                progress.set(progressValue);
                statusMessage.set(String.format("导出中: %d/%d", rowIdx + 1, totalRows));
            }
            
            // 自动调整列宽
            for (int i = 0; i < selectedColumns.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
            
            progress.set(1.0);
            statusMessage.set("导出完成");
            
        } catch (Exception e) {
            statusMessage.set("导出失败: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * 查询结果
     */
    public static class QueryResult {
        private final int total;
        private final List<Map<String, Object>> documents;
        
        public QueryResult(int total, List<Map<String, Object>> documents) {
            this.total = total;
            this.documents = documents;
        }
        
        public int getTotal() {
            return total;
        }
        
        public List<Map<String, Object>> getDocuments() {
            return documents;
        }
    }
}
