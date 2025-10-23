package org.example.ai_api.Strategy.FileReader;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 10353965
 */
@Component
@Order(3)
public class XlsxReader implements FileReaderStrategy {

   @Override
   public String read(InputStream inputStream) throws Exception {
       StringBuilder markdownBuilder = new StringBuilder();
       List<List<List<String>>> allSheetsData = new ArrayList<>();
       List<String> sheetNames = new ArrayList<>();

       // 1. 创建ExcelReader
       ExcelReader excelReader = EasyExcel.read(inputStream).build();
       try {
           List<ReadSheet> readSheets = excelReader.excelExecutor().sheetList();
           List<ReadSheet> toReadSheets = new ArrayList<>();
           for (ReadSheet readSheet : readSheets) {
               List<List<String>> allRows = new ArrayList<>();
               AnalysisEventListener<Object> listener = new AnalysisEventListener<Object>() {
                   @Override
                   public void invoke(Object row, AnalysisContext context) {
                       if (row instanceof Map) {
                           Map<Integer, String> map = (Map<Integer, String>) row;
                           int currentMax = map.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
                           List<String> rowList = new ArrayList<>();
                           for (int i = 0; i < currentMax; i++) {
                               String value = map.get(i);
                               rowList.add(value == null ? "    " : value);
                           }
                           allRows.add(rowList);
                       }
                   }
                   @Override
                   public void doAfterAllAnalysed(AnalysisContext context) {}
               };
               // 不要减1
               ReadSheet sheet = EasyExcel.readSheet(readSheet.getSheetNo())
                       .registerReadListener(listener)
                       .build();
               toReadSheets.add(sheet);
               allSheetsData.add(allRows);
               sheetNames.add(readSheet.getSheetName());
           }
           excelReader.read(toReadSheets.toArray(new ReadSheet[0]));
       } finally {
           excelReader.finish();
       }

       // 补齐每个 sheet 的所有行，确保每一行的列数一致
       for (List<List<String>> sheetData : allSheetsData) {
           int maxColumns = sheetData.stream().mapToInt(List::size).max().orElse(0);
           for (List<String> row : sheetData) {
               while (row.size() < maxColumns) {
                   row.add("    ");
               }
           }
       }
       // 拼接Markdown
       for (int i = 0; i < allSheetsData.size(); i++) {
           markdownBuilder.append("## ").append(sheetNames.get(i)).append("\n\n");
           markdownBuilder.append(toMarkdown(allSheetsData.get(i))).append("\n");
       }
       return markdownBuilder.toString();
   }

    /**
     * Markdown表格格式化
     */
   private String toMarkdown(List<List<String>> tableData) {
       // 1. 过滤全空行
       tableData = tableData.stream()
               .filter(row -> row.stream().anyMatch(cell -> cell != null && !cell.trim().isEmpty()))
               .collect(java.util.stream.Collectors.toList());
       if (tableData.isEmpty()){
           return "";
       }

       int maxColumns = tableData.stream().mapToInt(List::size).max().orElse(0);

       // 2. 过滤全空列
       boolean[] colHasContent = new boolean[maxColumns];
       for (List<String> row : tableData) {
           for (int i = 0; i < row.size(); i++) {
               String cell = row.get(i);
               if (cell != null && !cell.trim().isEmpty()) {
                   colHasContent[i] = true;
               }
           }
       }
       // 构建新表格，只保留有内容的列
       List<List<String>> filteredTable = new ArrayList<>();
       for (List<String> row : tableData) {
           List<String> newRow = new ArrayList<>();
           for (int i = 0; i < row.size(); i++) {
               if (colHasContent[i]) {
                   newRow.add(row.get(i));
               }
           }
           filteredTable.add(newRow);
       }
       if (filteredTable.isEmpty() || filteredTable.get(0).isEmpty()) {
           return "";
       }
       int filteredMaxColumns = filteredTable.get(0).size();

       StringBuilder builder = new StringBuilder();
       // 表头
       builder.append("|");
       for (String col : filteredTable.get(0)) {
           builder.append(formatCell(col)).append("|");
       }
       builder.append("\n|");
       for (int i = 0; i < filteredMaxColumns; i++) {
           builder.append("---|");
       }
       builder.append("\n");
       // 内容
       for (int i = 1; i < filteredTable.size(); i++) {
           builder.append("|");
           for (String col : filteredTable.get(i)) {
               builder.append(formatCell(col)).append("|");
           }
           builder.append("\n");
       }
       return builder.toString();
   }

    /**
     * 新增：格式化单元格内容，替换换行符为<br>
     */
   private String formatCell(String cell) {
       if (cell == null) {
           return "    ";
       }
       // 替换所有换行符为<br>，并处理其他可能的特殊字符
       return cell.replace("\\", "\\\\")
                  .replace("|", "\\|")
                  .replace("\r\n", "<br>")
                  .replace("\n", "<br>")
                  .replace("\r", "<br>");
   }

   @Override
   public Boolean support(String fileName) {
       return fileName != null && fileName.toLowerCase().endsWith(".xlsx");
   }
}



