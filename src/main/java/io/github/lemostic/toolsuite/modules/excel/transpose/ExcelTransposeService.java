package io.github.lemostic.toolsuite.modules.excel.transpose;

import javafx.beans.property.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Excel转置服务 - 提供Excel读取、预览、行列转置功能
 */
public class ExcelTransposeService {
    
    private final StringProperty statusMessage;
    private final DoubleProperty progress;
    private final IntegerProperty totalRows;
    private final IntegerProperty totalCols;
    
    public ExcelTransposeService() {
        this.statusMessage = new SimpleStringProperty("就绪");
        this.progress = new SimpleDoubleProperty(0);
        this.totalRows = new SimpleIntegerProperty(0);
        this.totalCols = new SimpleIntegerProperty(0);
    }
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    public DoubleProperty progressProperty() {
        return progress;
    }
    
    public IntegerProperty totalRowsProperty() {
        return totalRows;
    }
    
    public IntegerProperty totalColsProperty() {
        return totalCols;
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
     * 读取指定sheet的数据，返回二维列表（行优先）
     * @param maxRows 最大预览行数，-1表示全部
     */
    public List<List<String>> readSheetData(File excelFile, String sheetName, int maxRows) throws IOException {
        statusMessage.set("正在读取数据...");
        progress.set(0.3);
        
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = createWorkbook(fis, excelFile.getName())) {
            
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("工作表不存在: " + sheetName);
            }
            
            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = sheet.getLastRowNum();
            int totalRowsCount = lastRowNum - firstRowNum + 1;
            totalRows.set(totalRowsCount);
            
            // 计算最大列数
            int maxColCount = 0;
            for (int i = firstRowNum; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    maxColCount = Math.max(maxColCount, row.getLastCellNum());
                }
            }
            totalCols.set(maxColCount);
            
            // 决定实际读取的行数
            int rowsToRead = maxRows == -1 ? totalRowsCount : Math.min(maxRows, totalRowsCount);
            
            List<List<String>> data = new ArrayList<>();
            for (int i = firstRowNum; i < firstRowNum + rowsToRead; i++) {
                Row row = sheet.getRow(i);
                List<String> rowData = new ArrayList<>();
                if (row != null) {
                    for (int j = 0; j < maxColCount; j++) {
                        Cell cell = row.getCell(j);
                        rowData.add(getCellValueAsString(cell));
                    }
                } else {
                    // 空行填充
                    for (int j = 0; j < maxColCount; j++) {
                        rowData.add("");
                    }
                }
                data.add(rowData);
            }
            
            // 更新进度
            progress.set(1.0);
            statusMessage.set(String.format("读取完成 - 共 %d 行 %d 列，预览 %d 行", 
                totalRowsCount, maxColCount, data.size()));
            
            return data;
        }
    }
    
    /**
     * 行列转置 - 将行优先数据转为列优先数据
     */
    public List<List<String>> transposeData(List<List<String>> data) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        
        int rowCount = data.size();
        int colCount = data.get(0).size();
        
        List<List<String>> transposed = new ArrayList<>();
        for (int j = 0; j < colCount; j++) {
            List<String> newRow = new ArrayList<>();
            for (int i = 0; i < rowCount; i++) {
                if (j < data.get(i).size()) {
                    newRow.add(data.get(i).get(j));
                } else {
                    newRow.add("");
                }
            }
            transposed.add(newRow);
        }
        
        return transposed;
    }
    
    /**
     * 获取指定列的数据（用于自定义复制）
     */
    public List<String> getColumnData(List<List<String>> data, int columnIndex) {
        List<String> columnData = new ArrayList<>();
        for (List<String> row : data) {
            if (columnIndex < row.size()) {
                columnData.add(row.get(columnIndex));
            } else {
                columnData.add("");
            }
        }
        return columnData;
    }
    
    /**
     * 将列数据拼接为单行字符串
     */
    public String joinColumnData(List<String> columnData, String prefix, String suffix, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columnData.size(); i++) {
            sb.append(prefix);
            sb.append(columnData.get(i));
            sb.append(suffix);
            if (i < columnData.size() - 1) {
                sb.append(separator);
            }
        }
        return sb.toString();
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
     * 将单元格值转换为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        return cell.getStringCellValue();
                    case NUMERIC:
                        double val = cell.getNumericCellValue();
                        if (val == Math.floor(val) && !Double.isInfinite(val)) {
                            return String.valueOf((long) val);
                        } else {
                            return String.valueOf(val);
                        }
                    case BOOLEAN:
                        return String.valueOf(cell.getBooleanCellValue());
                    default:
                        return "";
                }
            case BLANK:
                return "";
            default:
                return cell.toString();
        }
    }
}
