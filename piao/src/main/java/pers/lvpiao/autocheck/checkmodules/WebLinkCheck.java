package pers.lvpiao.autocheck.checkmodules;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import pers.lvpiao.autocheck.agents.CheckAgent;
import pers.lvpiao.autocheck.agents.ResultHandler;
import pers.lvpiao.autocheck.data.webCheckData;
import pers.lvpiao.autocheck.utils.ThreadPoolUtil;
import pers.lvpiao.autocheck.utils.VCRUtil;

public class WebLinkCheck {
	// 测试网络连接是否正常访问
	public static void check() {
		// 初始化线程数
		webCheckData.latch = new CountDownLatch(
				webCheckData.hosts.size() * webCheckData.ports.size() * webCheckData.querys.size() + 1);
		webCheckData.initQuerys();// 初始化查询url
		checkSingleLink();
		checGeneralLink();
		try {
			webCheckData.latch.await(2, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			ResultHandler.offer("超时未返回！");
			e.printStackTrace();
		}
	}

	private static void checkSingleLink() {
		ThreadPoolUtil.submitTask(new Runnable() {
			public void run() {
				for (String url : webCheckData.generalNodes) {
					if (!checkLinkVisitable(url)) {
						String errorInfo = "无法连接 : " + url;
						ResultHandler.offer(errorInfo);
						continue;
					}
				}
				// 处理需要验证码的的url
				Set<String> keys = webCheckData.randCodeNodes.keySet();
				for (String key : keys) {
					Response response = getResopse(key.substring(0, "http://135.33.6.128:9010".length()) + "/invoice");
					// 获取验证码
					String code = VCRUtil.getRandCode(key, response.cookies(), response.headers());
					// 拼接url
					String url = webCheckData.randCodeNodes.get(key) + code;
					CheckAgent agent = new CheckAgent(url);
					agent.setCookies(response.cookies());
					agent.setHeaders(response.headers());
					agent.check();
				}
				webCheckData.latch.countDown();// 正在执行的线程数减一
			}
		});
	}

	// 处理一般节点
	private static void checGeneralLink() {
		for (String host : webCheckData.hosts) {
			for (String port : webCheckData.ports) {
				String root = host + port;
				Response response = getResopse(root + webCheckData.login_url);
				if (response == null) {
					String errorInfo = "无法连接或登录 : " + root;
					ResultHandler.offer(errorInfo);
					for (int i = 0; i < webCheckData.querys.size(); i++) {
						webCheckData.latch.countDown();
					}
					continue;
				}
				for (String q : webCheckData.querys) {
					CheckAgent agent = new CheckAgent(root + q);
					agent.setCookies(response.cookies());
					agent.setHeaders(response.headers());
					ThreadPoolUtil.submitTask(agent);
				}
			}
		}
	}

	// 访问url并返回Response对象
	private static Response getResopse(String url) {
		Connection conn = Jsoup.connect(url);
		Response response = null;
		try {
			response = conn.execute();
		} catch (IOException e) {
			e.printStackTrace();
			return response;
		}
		return response;
	}

	// 检查url是否可访问
	private static boolean checkLinkVisitable(String url) {
		Connection conn = Jsoup.connect(url);
		Response response = null;
		try {
			response = conn.execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response != null && response.statusCode() == 200;
	}
}
