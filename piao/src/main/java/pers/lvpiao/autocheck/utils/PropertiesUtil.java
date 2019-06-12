package pers.lvpiao.autocheck.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

public class PropertiesUtil {

	public static Properties loadProperties(String fileName) {
		Properties prop = null;
		try (InputStream in = new FileInputStream(fileName)) {
			prop = new Properties();
			prop.load(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return prop;
	}

	public static ArrayList<String> getParasAsList(Properties properties, String itemName) {
		ArrayList<String> list = new ArrayList<>();
		int cur = 1;
		while (true) {
			String item = itemName + cur;
			cur++;
			if (properties.containsKey(item)) {
				String val = properties.getProperty(item);
				list.add(val);
			} else
				break;
		}
		return list;
	}
}
