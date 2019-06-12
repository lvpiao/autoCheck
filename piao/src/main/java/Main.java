
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import pers.lvpiao.autocheck.agents.ResultHandler;
import pers.lvpiao.autocheck.checkmodules.DatabaseCheck;
import pers.lvpiao.autocheck.checkmodules.WebLinkCheck;
import pers.lvpiao.autocheck.utils.ThreadPoolUtil;
import pers.lvpiao.autocheck.utils.TimeUtil;

public class Main {

	private static boolean sendEmail = true;
	private static volatile boolean checking = false;
	private static final String START = "start";
	private static Scanner sc = new Scanner(System.in);
	private static final ReentrantLock lock = new ReentrantLock();

	public static void main(String[] args) {
		ThreadPoolUtil.submitTask(() -> autoRun());
		TimeUtil.showNotice();
		inputCommand();
	}

	public static void inputCommand() {
		while (true) {
			String input = sc.nextLine().replaceAll("\\s+", " ").trim();
			if (input.isEmpty())
				continue;
			if (input.equals("exit")) {
				System.exit(0);
				break;
			} else if (input.equals("reload time")) {
				TimeUtil.load();
				continue;
			} else if (input.equals("c")) {
				System.out.println(checking);
				continue;
			}
			if (!checking) {
				String[] para = input.split("\\s+");
				if (para[0].equals(START)) {
					if (para.length == 2 && para[1].equals("-f")) {
						sendEmail = false;
					} else if (para.length > 2) {
						System.out.println("bad input!");
					}
					startCheck();
					sendEmail = true;
				} else {
					System.out.println("bad input!");
				}
			} else {
				System.out.println("正在运行检测，无法同时运行多个检测!");
			}
		}
	}

	// 不允许同时进行
	public static void startCheck() {
		try {
			lock.tryLock(1, TimeUnit.MINUTES);
			checking = true;
			Future<?> t1 = ThreadPoolUtil.submitTask(() -> WebLinkCheck.check());
			Future<?> t2 = ThreadPoolUtil.submitTask(() -> DatabaseCheck.check());
			// 等待以上两个线程执行完毕
			t1.get(90, TimeUnit.SECONDS);
			t2.get(3, TimeUnit.MINUTES);
		} catch (TimeoutException e) {
			ResultHandler.offer("检测超时 ：" + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			ResultHandler.offer("程序异常 ：" + e.getMessage());
			e.printStackTrace();
		} finally {
			checking = false;
			lock.unlock();
			// 测试完成，输出测试结果 true-发送电子邮件
			ResultHandler.dealResult(sendEmail);
			TimeUtil.showNotice();

		}
	}

	public static void autoRun() {
		while (true) {
			try {
				Thread.sleep(996);// 务必让睡眠时间小于1秒
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (!checking) {
				if (TimeUtil.canRun()) {
					startCheck();
				}
			}
		}
	}
}
