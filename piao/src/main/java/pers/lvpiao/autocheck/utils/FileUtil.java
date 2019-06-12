package pers.lvpiao.autocheck.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.filechooser.FileSystemView;

public class FileUtil {
	private static FileSystemView fsv = FileSystemView.getFileSystemView();
	public static String deskTopPath = fsv.getHomeDirectory().getAbsolutePath();
	public static String excelFileName = "工单统计量.xls";
	public static String excelFilePath = deskTopPath + File.separator + excelFileName;

	public static void inputStreamToFile(InputStream is, String filePath) {
		int length = 0;
		byte[] buffer = new byte[1024];
		try (FileOutputStream fos = new FileOutputStream(filePath);
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			while ((length = is.read(buffer)) > 0) {
				output.write(buffer, 0, length);
			}
			buffer = output.toByteArray();
			fos.write(output.toByteArray());
			while ((length = is.read(buffer, 0, buffer.length)) != -1) {
				fos.write(buffer, 0, length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
