package io.github.lemostic.toolsuite.modules.excel.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.NumberToTextConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class ExcelToJSONService {
    
    private final ObjectMapper objectMapper;
    private final StringProperty statusMessage;
    private final DoubleProperty progress;
    
    public ExcelToJSONService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.statusMessage = new SimpleStringProperty("就绪");
        this.progress = new SimpleDoubleProperty(0);
    }
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    public DoubleProperty progressProperty() {
        return progress;
    }
    
    /**
     * 获取Excel文件中的工作表名称列表
     */
    public String[] getSheetNames(File excelFile) throws IOException {
        statusMessage.set("正在读取Excel文件结构...");
        progress.set(0.1);
        
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = createWorkbook(fis, excelFile.getName())) {
            
            int numberOfSheets = workbook.getNumberOfSheets();
            String[] sheetNames = new String[numberOfSheets];
            
            for (int i = 0; i < numberOfSheets; i++) {
                sheetNames[i] = workbook.getSheetName(i);
            }
            
            statusMessage.set("成功读取工作表信息");
            progress.set(0.2);
            
            return sheetNames;
        }
    }
    
    /**
     * 将Excel文件转换为JSON字符串
     */
    public String convertExcelToJSON(File excelFile, String sheetName, boolean hasHeaderRow, boolean formatJson) throws Exception {
        statusMessage.set("开始转换Excel到JSON...");
        progress.set(0);
        
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = createWorkbook(fis, excelFile.getName())) {
            
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("工作表不存在: " + sheetName);
            }
            
            // 获取总行数用于进度计算
            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = sheet.getLastRowNum();
            int totalRows = lastRowNum - firstRowNum + 1;
            
            if (totalRows <= 0) {
                statusMessage.set("工作表为空");
                return "[]";
            }
            
            // 读取表头（如果存在）
            List<String> headers = new ArrayList<>();
            if (hasHeaderRow) {
                Row headerRow = sheet.getRow(firstRowNum);
                if (headerRow != null) {
                    int lastCellNum = headerRow.getLastCellNum();
                    for (int i = 0; i < lastCellNum; i++) {
                        Cell cell = headerRow.getCell(i);
                        String headerName = getCellValueAsString(cell);
                        if (headerName == null || headerName.trim().isEmpty()) {
                            headerName = "Column" + (i + 1); // 默认列名，从1开始编号
                        }
                        headers.add(headerName);
                    }
                }
            }
            
            // 读取数据
            List<Map<String, Object>> jsonList = new ArrayList<>();
            int startRow = hasHeaderRow ? firstRowNum + 1 : firstRowNum; // 如果有表头，从第二行开始读取
            
            for (int rowIndex = startRow; rowIndex <= lastRowNum; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                
                if (row == null) {
                    continue; // 跳过空行
                }
                
                Map<String, Object> rowMap = new HashMap<>();
                
                int lastCellNum = row.getLastCellNum();
                for (int colIndex = 0; colIndex < lastCellNum; colIndex++) {
                    Cell cell = row.getCell(colIndex);
                    Object cellValue = getCellValueAsObject(cell);
                    
                    if (hasHeaderRow && colIndex < headers.size()) {
                        rowMap.put(headers.get(colIndex), cellValue);
                    } else {
                        rowMap.put("Column" + (colIndex + 1), cellValue); // 列名从1开始编号
                    }
                }
                
                jsonList.add(rowMap);
                
                // 更新进度（仅在处理大量数据时才更新，避免频繁更新UI影响性能）
                if (totalRows > 100 && (rowIndex - startRow) % Math.max(1, totalRows / 50) == 0) {
                    double progressValue = (double) (rowIndex - startRow) / totalRows * 0.8 + 0.2;
                    progress.set(progressValue);
                    statusMessage.set(String.format("正在转换... (%d/%d)", rowIndex - startRow + 1, totalRows));
                }
            }
            
            statusMessage.set("转换完成，正在生成JSON...");
            progress.set(0.9);
            
            // 根据格式化选项设置ObjectMapper
            if (formatJson) {
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            } else {
                objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
            }
            
            String jsonResult = objectMapper.writeValueAsString(jsonList);
            
            statusMessage.set(String.format("转换完成！共处理 %d 行数据", jsonList.size()));
            progress.set(1.0);
            
            return jsonResult;
        }
    }
    
    /**
     * 根据文件扩展名创建合适的Workbook
     */
    private Workbook createWorkbook(FileInputStream fis, String fileName) throws IOException {
        if (fileName.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (fileName.toLowerCase().endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IllegalArgumentException("不支持的文件格式: " + fileName);
        }
    }
    
    /**
     * 将单元格值转换为对象，保留原始数据类型
     */
    private Object getCellValueAsObject(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 处理日期时间
                    if (DateUtil.isCellInternalDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        return cell.getDateCellValue().toString();
                    }
                } else {
                    // 检查是否为整数
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                        // 检查数值范围以确定是否转换为long
                        if (numericValue >= Long.MIN_VALUE && numericValue <= Long.MAX_VALUE) {
                            return (long) numericValue;
                        } else {
                            // 如果超出long范围，返回原始double值
                            return numericValue;
                        }
                    } else {
                        // 返回BigDecimal以保持精度
                        return new BigDecimal(NumberToTextConverter.toText(cell.getNumericCellValue()));
                    }
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                // 对于公式，返回计算后的值
                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        return cell.getStringCellValue();
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            return cell.getDateCellValue().toString();
                        } else {
                            double numericValue = cell.getNumericCellValue();
                            if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                                if (numericValue >= Long.MIN_VALUE && numericValue <= Long.MAX_VALUE) {
                                    return (long) numericValue;
                                } else {
                                    return numericValue;
                                }
                            } else {
                                return new BigDecimal(NumberToTextConverter.toText(numericValue));
                            }
                        }
                    case BOOLEAN:
                        return cell.getBooleanCellValue();
                    case BLANK:
                        return null;
                    default:
                        return cell.getCellFormula();
                }
            case BLANK:
                return null;
            default:
                return cell.toString();
        }
    }
    
    /**
     * 将单元格值转换为字符串（保留原有方法用于向后兼容）
     */
    private String getCellValueAsString(Cell cell) {
        Object value = getCellValueAsObject(cell);
        return value != null ? value.toString() : null;
    }
}