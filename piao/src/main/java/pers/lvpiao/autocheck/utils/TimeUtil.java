package pers.lvpiao.autocheck.utils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

public class TimeUtil {

	/*
	 * @para formatter yyyyMMddHHmmssSSS 年-毫秒
	 */
	public static String getFormattedTime(String pattern) {
		long currentTime = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		Date date = new Date(currentTime);
		return sdf.format(date);
	}

	private static ArrayList<String> autoRunTimes = new ArrayList<String>();

	private static SimpleDateFormat formatter = new SimpleDateFormat("HHmmss");
	private static Date date = new Date();

	public static String getCurrentTime() {
		try {
			Field time = date.getClass().getDeclaredField("fastTime");
			time.setAccessible(true);
			time.setLong(date, System.currentTimeMillis());
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		String curtime = formatter.format(date);
		return curtime;
	}

	public static boolean canRun() {
		return autoRunTimes.contains(getCurrentTime());
	}

	static {
		load();
	}

	public static void load() {
		autoRunTimes.clear();
		Properties prop = PropertiesUtil.loadProperties("time.properties");
		int cur = 1;
		while (true) {
			String thisTime = "time" + cur;
			cur++;
			if (prop.containsKey(thisTime)) {
				String val = prop.getProperty(thisTime);
				autoRunTimes.add(val);
			} else
				break;
		}
	}

	public static void showNotice() {
		System.out.println("---------------------------------------------------\n\n\n将在以下时间自动运行：");
		for (String t : autoRunTimes) {
			System.out.println("\n" + t.substring(0, 2) + "点" + t.substring(2, 4) + "分" + t.substring(4, 6) + "秒");
		}
		System.out.println("你也可以输入 start 立即运行检测！");
		System.out.print(">>");
	}

	// UnitTest
	public static void main(String[] args) {
		TimeUtil.getCurrentTime();
	}
}
