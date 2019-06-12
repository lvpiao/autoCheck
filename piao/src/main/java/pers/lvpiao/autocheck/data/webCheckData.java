package pers.lvpiao.autocheck.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import pers.lvpiao.autocheck.utils.PropertiesUtil;
import pers.lvpiao.autocheck.utils.TimeUtil;

public class webCheckData {

	public static final String FAILED_FUTURE = "login.jsp";// 爬取失败特征
	public static final String PROPERTIES_PATH = "web_link.properties";

	// 特殊的查询节点
	public static ArrayList<String> generalNodes = null;
	public static HashMap<String, String> randCodeNodes = null;

	// 用于等待多线程结束多线程
	public static CountDownLatch latch;
	// 一些配置信息
	private static Properties properties;
	public static final int HOST_NAME_LENGTH = 24;
	public static ArrayList<String> hosts;
	public static ArrayList<String> ports;
	public static ArrayList<String> urls;
	// 所有的查询
	public static ArrayList<String> querys = new ArrayList<String>();

	// 登录的url
	public static String login_url;

	static {
		properties = PropertiesUtil.loadProperties(PROPERTIES_PATH);
		// 使LOGIN_URL有效
		login_url = properties.getProperty("login_url").replace("USER", properties.getProperty("user"))
				.replace("PASSWORD", properties.getProperty("password"));

		hosts = PropertiesUtil.getParasAsList(properties, "host");
		urls = PropertiesUtil.getParasAsList(properties, "url");
		ports = PropertiesUtil.getParasAsList(properties, "port");
		generalNodes = PropertiesUtil.getParasAsList(properties, "generalNode");

		randCodeNodes = new HashMap<String, String>();

		randCodeNodes.put(properties.getProperty("randCodeNode1"), properties.getProperty("randCodeNode1Login"));
		randCodeNodes.put(properties.getProperty("randCodeNode2"), properties.getProperty("randCodeNode2Login"));

		System.out.println("主机数量 ： " + hosts.size());
		System.out.println("端口数量 ： " + ports.size());
		System.out.println("查询数量：" + (urls.size() + generalNodes.size() + randCodeNodes.size() * 2));
	}

	public static void initQuerys() {
		querys.clear();
		String date = TimeUtil.getFormattedTime("yyyyMMdd");
		for (String url : urls) {
			String item = url.replace("USER", properties.getProperty("user"))
					.replace("QUERY_VALUE", properties.getProperty("effectivePhoneNum")).replace("DATE", date)
					.replace("RANDOM", String.valueOf(Math.random()));
			System.out.println(item);
			querys.add(item);
		}
	}
}
