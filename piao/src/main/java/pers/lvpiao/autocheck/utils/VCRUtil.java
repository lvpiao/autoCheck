package pers.lvpiao.autocheck.utils;

import java.io.File;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;
import pers.lvpiao.autocheck.agents.ResultHandler;

public class VCRUtil {
	public static String readChar(String path) {
		ITesseract instance = new Tesseract();
		File imageFile = new File(path);
		File tessDataFolder = LoadLibs.extractTessResources("tessdata");
		instance.setDatapath(tessDataFolder.getAbsolutePath());
		return getOCRText(instance, imageFile);
	}

	public static String readChar(String path, String dataPath, String language) {
		File imageFile = new File(path);
		ITesseract instance = new Tesseract();
		instance.setDatapath(dataPath);
		// 英文库识别数字比较准确
		instance.setLanguage(language);
		return getOCRText(instance, imageFile);
	}

	private static String getOCRText(ITesseract instance, File imageFile) {
		String result = null;
		try {
			result = instance.doOCR(imageFile);
		} catch (TesseractException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static String getRandCode(String vcUrl, Map<String, String> cookies, Map<String, String> headers) {
		try {
			Connection conn = Jsoup.connect(vcUrl).ignoreContentType(true).cookies(cookies).headers(headers);
			String filePath = "randCode.jpg";
			FileUtil.inputStreamToFile(conn.execute().bodyStream(), filePath);
			String code = readChar(filePath).trim();
			if (code.length() != 4) {
				return getRandCode(vcUrl, cookies, headers);
			}
			for (char c : code.toCharArray()) {
				if (c < '0' && c > '9') {
					return getRandCode(vcUrl, cookies, headers);
				}
			}
			System.out.println("验证码 ： " + code);
			return code;
		} catch (Exception e) {
			ResultHandler.offer("获取验证码失败");
			e.printStackTrace();
		}
		return null;
	}
}