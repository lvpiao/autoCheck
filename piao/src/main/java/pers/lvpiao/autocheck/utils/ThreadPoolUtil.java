package pers.lvpiao.autocheck.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPoolUtil {

	private static ExecutorService pool = Executors.newCachedThreadPool();

	public static Future<?> submitTask(Runnable runnable) {
		return pool.submit(runnable);
	}

	public static void shuwtdownThreadPool() {
		pool.shutdown();
	}
}
