package pers.lvpiao.autocheck.agents;

import java.util.concurrent.LinkedBlockingDeque;

import pers.lvpiao.autocheck.utils.EmailUtil;

public class ResultHandler {

	// 结果队列
	private static LinkedBlockingDeque<String> InfoQueue = new LinkedBlockingDeque<String>();

	// 结果报告
	private static StringBuilder resultReport;

	public static void offer(String info) {
		InfoQueue.offer(info);
	}

	// 生成结果报告
	public static void dealResult(boolean sendEmail) {
		resultReport = new StringBuilder();
		while (!InfoQueue.isEmpty()) {
			String errorItem = InfoQueue.poll();
			resultReport.append(errorItem).append('\n');
		}
		System.out.println("\n\n\n" + resultReport.toString());
		if (sendEmail)
			sentMessges();
	}

	// 发送结果报告
	private static void sentMessges() {
		String result = resultReport.toString();
		if (result.length() == 0)
			result = "一切正常!";
		EmailUtil.sendQQEmail(result);
		System.out.println("\nEmail successfully be sent");
	}
}
