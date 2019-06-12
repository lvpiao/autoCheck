package pers.lvpiao.autocheck.agents;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import pers.lvpiao.autocheck.data.webCheckData;

public class CheckAgent implements Runnable {
	// 本次连接的cookie和请求头
	private Map<String, String> cookies;
	private Map<String, String> headers;

	public Map<String, String> getCookies() {
		return cookies;
	}

	public void setCookies(Map<String, String> cookies) {
		this.cookies = cookies;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	private String url;
	private static int again = 3;

	public CheckAgent(String url) {
		this.url = url;
	}

	// 直接调用可以不创建线程
	public void check() {
		Connection conn = Jsoup.connect(url).cookies(cookies).headers(headers);// 设置headers和cookies
		conn.method(Method.GET);
		conn.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko");// IE浏览器
		Response response = null;
		try {
			response = conn.execute();
		} catch (IOException e) {
			if (again-- > 0) {
				try {
					Thread.sleep(1000);
					check();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} else {
				e.printStackTrace();
				ResultHandler.offer(e.getMessage() + e.getCause());
				return;
			}
		}
		if (response == null) {
			String errorInfo = "无法打开 : " + url;
			ResultHandler.offer(errorInfo);
			return;
		}
		int statusCode = response.statusCode();
		try (Scanner in = new Scanner(response.bodyStream())) {
			while (in.hasNextLine()) {
				String line = in.nextLine();
				System.out.println(line);
				if (line.contains("登陆成功")) {
					return;
				}
				if (line.contains(webCheckData.FAILED_FUTURE)) {
					ResultHandler.offer("爬取链接 ：" + url + " 失败，返回值：" + line);
				}
			}
		}
		if (statusCode != 200) {
			String errorInfo = url.substring(0, webCheckData.HOST_NAME_LENGTH + 1) + "：服务器内部错误 ！" + "\n 错误状态码 ： "
					+ statusCode;
			ResultHandler.offer(errorInfo);
		}
	}

	@Override
	public void run() {
		try {
			check();
		} finally {
			webCheckData.latch.countDown();
		}
	}
}
