package pers.lvpiao.autocheck.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class ExcelUtil {
	public static void WriteExcel(ArrayList<String[]> data) {
		HSSFWorkbook workBook = new HSSFWorkbook();
		HSSFSheet sheet = workBook.createSheet("工单统计量");
		HSSFCellStyle style = workBook.createCellStyle();
		style.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 创建一个居中格式
		sheet.setColumnWidth(0, 18 * 256);
		sheet.setColumnWidth(1, 12 * 256);
		sheet.setColumnWidth(2, 12 * 256);
		sheet.setColumnWidth(3, 12 * 256);
		sheet.setColumnWidth(4, 12 * 256);
		HSSFRow row = sheet.createRow(0);
		row.createCell(0).setCellValue("时间");
		row.createCell(1).setCellValue("停机工单量");
		row.createCell(2).setCellValue("开机工单量");
		row.createCell(3).setCellValue("实时催缴量");
		row.createCell(4).setCellValue("信用度提醒量");
		row.getCell(0).setCellStyle(style);
		row.getCell(1).setCellStyle(style);
		row.getCell(2).setCellStyle(style);
		row.getCell(3).setCellStyle(style);
		row.getCell(4).setCellStyle(style);
		row.setHeightInPoints(15);
		int rows = data.size();
		int cols = data.get(0).length;
		for (int i = 1; i < rows - 1; i++) {
			row = sheet.createRow(i);
			for (int j = 0; j < cols; j++) {
				HSSFCell cell = row.createCell(j);
				String d = data.get(i)[j].trim();
				if (j == 0 || d == "")
					cell.setCellValue(data.get(i)[j]);
				else
					cell.setCellValue(Integer.parseInt(d));
				cell.setCellStyle(style);
				row.setHeightInPoints(15);
			}
		}
		try (FileOutputStream fo = new FileOutputStream(FileUtil.excelFilePath)) {
			workBook.write(fo);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				workBook.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
