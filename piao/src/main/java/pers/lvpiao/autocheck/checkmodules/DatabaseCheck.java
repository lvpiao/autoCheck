package pers.lvpiao.autocheck.checkmodules;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import pers.lvpiao.autocheck.agents.ResultHandler;
import pers.lvpiao.autocheck.utils.ExcelUtil;
import pers.lvpiao.autocheck.utils.PropertiesUtil;
import pers.lvpiao.autocheck.utils.ThreadPoolUtil;

public class DatabaseCheck {

	private static Properties proMap;
	private static String propertiesFilePath = "database.properties";// 配置文件，目录
	static {
		proMap = PropertiesUtil.loadProperties(propertiesFilePath);
	}
	private static CountDownLatch countDown;

	public static void check() {
		try {
			countDown = new CountDownLatch(5);
			ThreadPoolUtil.submitTask(() -> tableSpaceStatusCheck());
			ThreadPoolUtil.submitTask(() -> InterfaceCheck());
			ThreadPoolUtil.submitTask(() -> InterfaceCheck2());
			ThreadPoolUtil.submitTask(() -> InterfaceErrorCheck());
			ThreadPoolUtil.submitTask(() -> WorkOrderStatistics());
			countDown.await(3, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static Connection getConnection() {
		Connection connection = null;
		try {
			Class.forName((String) proMap.get("driver"));
			connection = DriverManager.getConnection(proMap.getProperty("url"), proMap.getProperty("username"),
					proMap.getProperty("password"));
			System.out.print("正在执行SQL代码:");
		} catch (Exception e) {
			ResultHandler.offer("连接数据库失败");
			e.printStackTrace();
		}
		return connection;
	}

	// 用于 执行 接口检查的sql
	private static void interfaceCheckUtil(Connection conn, String sql, int limit, String tag, int column) {
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			pstm = conn.prepareStatement(sql);
			System.out.println(sql);
			rs = pstm.executeQuery();
			while (rs.next()) {
				if (rs.getInt(column) >= limit) {
					if (column == 1)
						ResultHandler.offer("接口  " + tag + "  检查结果 ： 异常 , 结果大于" + limit);
					else
						ResultHandler.offer("接口  " + tag + "  检查结果 ： 异常, 地区编码：" + rs.getString(1) + " , 结果大于" + limit);
				}
				System.out
						.println("接口  " + tag + "  检查结果 ： " + (rs.getInt(column) < limit ? "正常" : "异常 , 结果大于" + limit));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, pstm, null);
		}
	}

	/**
	 * 检查表空间使用情况
	 */
	public static void tableSpaceStatusCheck() {
		// 表空间使用情况sql
		String sql = "select a.tablespace_name,total,free,total-free used,round((total-free)/total,2)*100||'%' user_precent from "
				+ "  ( select tablespace_name,sum(bytes)/1024/1024 total from dba_data_files@dblink_comm_qry_219 "
				+ "  group by tablespace_name) a,"
				+ "  ( select tablespace_name,sum(bytes)/1024/1024 free from dba_free_space@dblink_comm_qry_219 "
				+ "  group by tablespace_name) b where a.tablespace_name=b.tablespace_name";
		Connection conn = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			pstm = conn.prepareStatement(sql);
			System.out.println(sql);
			rs = pstm.executeQuery();
			while (rs.next()) {
				String tmp = rs.getString(rs.getMetaData().getColumnCount()).trim();
				int val = Integer.parseInt(tmp.substring(0, tmp.length() - 1));
				if (val >= 92) {
					String errorInfo = "表空间  " + rs.getString(1) + "  已使用" + tmp;
					ResultHandler.offer(errorInfo);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, pstm, conn);
			countDown.countDown();
		}
	}

	/**
	 * 
	 * 接口检查1
	 */
	public static void InterfaceCheck() {
		try {
			String sql1 = "select  count(*)  from comm.a_endtoend_interface@dblink_comm_qry_219 where oper_flag=0";
			String sql2 = "select  count(*)  from  comm.itm_log@dblink_comm_qry_219";
			Connection conn = getConnection();
			interfaceCheckUtil(conn, sql1, 2000, "comm.a_endtoend_interface@dblink_comm_qry_219", 1);
			interfaceCheckUtil(conn, sql2, 2000, " comm.itm_log@dblink_comm_qry_219", 1);
		} finally {
			countDown.countDown();
		}
	}

	/**
	 * 
	 * 接口检查2
	 */
	public static void InterfaceCheck2() {
		String sql1 = "select area_code, count(*)\r\n"
				+ "from tif_order_bill@dblink_comm_qry_219 where input_date>sysdate-1 and oper_flag=0 and ocs_flag=1\r\n"
				+ "group by area_code order by area_code-----ocs新装";

		String sql2 = "select area_code, count(*)\r\n"
				+ "from tif_order_bill@dblink_comm_qry_219 where input_date>sysdate-1 and oper_flag=2 and ocs_flag=1\r\n"
				+ "group by area_code order by area_code-----ocs错单";

		String sql3 = "select area_code, count(*)\r\n"
				+ "from tif_order_bill@dblink_comm_qry_219 where input_date>sysdate-1 and oper_flag=0 and ocs_flag<>1\r\n"
				+ "group by area_code order by area_code-----非ocs新装";

		String sql4 = "select area_code, count(*)\r\n"
				+ "from tif_order_bill@dblink_comm_qry_219 where input_date>sysdate-1 and oper_flag=2 and ocs_flag<>1\r\n"
				+ "group by area_code order by area_code-----非ocs错单";
		Connection conn = getConnection();
		try {
			interfaceCheckUtil(conn, sql1, 3000, "ocs新装", 2);
			interfaceCheckUtil(conn, sql2, 3000, "ocs错单", 2);
			interfaceCheckUtil(conn, sql3, 3000, "非ocs新装", 2);
			interfaceCheckUtil(conn, sql4, 3000, " 非ocs错单", 2);
		} finally {
			countDown.countDown();
		}
	}

	/**
	 * 接口错误检查
	 */
	public static void InterfaceErrorCheck() {
		String sql1 = "select distinct b.proc_name,b.run_flag,b.last_date\r\n"
				+ "from comm.tif_interface_config@dblink_comm_qry_219 a,crm_interface.tif_proc_run_ctl@dblink_comm_qry_219 b\r\n"
				+ "where b.last_date<sysdate-0.5/24 \r\n" + "and a.pkg_name=b.proc_name";

		String sql2 = "select distinct b.proc_name,b.run_flag,b.last_date\r\n"
				+ "from comm.tif_interface_config@dblink_comm_qry_219 a,crm_interface.tif_proc_run_ctl_ex@dblink_comm_qry_219 b\r\n"
				+ "where b.last_date<sysdate-0.5/24 \r\n" + "and a.pkg_name=b.proc_name";
		Connection conn = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			pstm = conn.prepareStatement(sql1);
			System.out.println(sql1);
			rs = pstm.executeQuery();
			while (rs.next()) {
				String proc_name = rs.getString(1);
				String run_glag = rs.getString(2);
				Date last_date = rs.getDate(3);
				ResultHandler.offer("\n接口错误 ——proc_name : " + proc_name + "  ,run_glag : " + run_glag
						+ ",  last_date : " + last_date);
			}
			close(rs, pstm, null);
			pstm = conn.prepareStatement(sql2);
			System.out.println(sql2);
			rs = pstm.executeQuery();
			while (rs.next()) {
				String proc_name = rs.getString(1);
				String run_glag = rs.getString(2);
				Date last_date = rs.getDate(3);
				ResultHandler.offer("\n接口错误——proc_name : " + proc_name + "  ,run_glag : " + run_glag + ",  last_date : "
						+ last_date);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, pstm, conn);
			countDown.countDown();
		}
	}

	/**
	 * 工单量统计
	 */
	private static int ROWS = 5;

	private static void workOrder1(Connection conn) {
		String sql1 = "drop table a_work_order_log_t";
		String sql2 = "create table a_work_order_log_t as\r\n"
				+ "select * from acct.a_work_order_log@dblink_comm_qry_219 a\r\n"
				+ "where a.created_date > sysdate - 1\r\n" + "and  a.owe_business_type_id in (2,6)";
		String sql3 = "select trunc(created_date,'hh'),count(1)\r\n" + "from a_work_order_log_t\r\n"
				+ "group by trunc(created_date,'hh')  order by trunc(created_date,'hh')";
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			exeSql(conn, sql1);
			exeSql(conn, sql2);
			pstm = conn.prepareStatement(sql3);
			System.out.println(sql3);
			rs = pstm.executeQuery();
			while (rs.next()) {
				String time = rs.getString(1);
				if (wos.containsKey(time)) {
					wos.get(time)[1] = rs.getString(2);
				} else {
					String[] arr = new String[ROWS];
					Arrays.fill(arr, "");
					arr[1] = rs.getString(2);
					wos.put(time, arr);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, pstm, null);
		}
	}

	private static void workOrder2(Connection conn) {
		String sql1 = "drop table a_work_order_log_t";
		String sql2 = "create table a_work_order_log_t as \r\n"
				+ "select * from acct.a_work_order_log@dblink_comm_qry_219 a \r\n"
				+ "where a.created_date > sysdate - 1 \r\n" + "and  a.owe_business_type_id = 8";
		String sql3 = "select trunc(created_date,'hh'),count(1) \r\n" + "from a_work_order_log_t \r\n"
				+ "group by trunc(created_date,'hh')  order by trunc(created_date,'hh')";
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			exeSql(conn, sql1);
			exeSql(conn, sql2);
			conn = getConnection();
			pstm = conn.prepareStatement(sql3);
			System.out.println(sql3);
			rs = pstm.executeQuery();
			while (rs.next()) {
				String time = rs.getString(1);
				if (wos.containsKey(time)) {
					wos.get(time)[2] = rs.getString(2);
				} else {
					String[] arr = new String[ROWS];
					Arrays.fill(arr, "");
					arr[2] = rs.getString(2);
					wos.put(time, arr);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, pstm, null);
		}
	}

	private static void workOrder3(Connection conn) {
		String sql1 = "drop table a_work_order_log_t";
		String sql2 = "create table a_work_order_log_t as \r\n"
				+ "select * from acct.a_owe_dun_log@dblink_comm_qry_219 a \r\n"
				+ "where a.created_date > sysdate - 1 \r\n" + "and  a.owe_business_type_id = 1";
		String sql3 = "select trunc(created_date,'hh'),count(1) \r\n" + "from a_work_order_log_t \r\n"
				+ "group by trunc(created_date,'hh')  order by trunc(created_date,'hh')";
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			exeSql(conn, sql1);
			exeSql(conn, sql2);
			conn = getConnection();
			pstm = conn.prepareStatement(sql3);
			System.out.println(sql3);
			rs = pstm.executeQuery();
			while (rs.next()) {
				String time = rs.getString(1);
				if (wos.containsKey(time)) {
					wos.get(time)[3] = rs.getString(2);
				} else {
					String[] arr = new String[ROWS];
					Arrays.fill(arr, "");
					arr[3] = rs.getString(2);
					wos.put(time, arr);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, pstm, null);
		}
	}

	private static void workOrder4(Connection conn) {
		String sql1 = "drop table a_work_order_log_t";
		String sql2 = "create table a_work_order_log_t as \r\n"
				+ "select * from acct.a_owe_dun_log@dblink_comm_qry_219 a \r\n"
				+ "where a.created_date > sysdate - 1\r\n" + "and  a.owe_business_type_id in (65,66)";
		String sql3 = "select trunc(created_date,'hh'),count(1) \r\n" + "from a_work_order_log_t \r\n"
				+ "group by trunc(created_date,'hh')  order by trunc(created_date,'hh')";
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			exeSql(conn, sql1);
			exeSql(conn, sql2);
			conn = getConnection();
			pstm = conn.prepareStatement(sql3);
			System.out.println(sql3);
			rs = pstm.executeQuery();
			while (rs.next()) {
				String time = rs.getString(1);
				if (wos.containsKey(time)) {
					wos.get(time)[4] = rs.getString(2);
				} else {
					String[] arr = new String[ROWS];
					Arrays.fill(arr, "");
					arr[4] = rs.getString(2);
					wos.put(time, arr);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(rs, pstm, null);
		}
	}

	private static HashMap<String, String[]> wos = new HashMap<String, String[]>();

	public static void WorkOrderStatistics() {
		Connection conn = getConnection();
		try {
			workOrder1(conn);
			workOrder2(conn);
			workOrder3(conn);
			workOrder4(conn);
			ArrayList<String[]> data = new ArrayList<String[]>();
			Set<String> keys = wos.keySet();
			for (String key : keys) {
				String[] arr = wos.get(key);
				arr[0] = key;
				data.add(arr);
			}
			Collections.sort(data, (x, y) -> x[0].compareTo(y[0]));
			ExcelUtil.WriteExcel(data);
		} finally {
			close(null, null, conn);
			countDown.countDown();
		}
	}

	private static void exeSql(Connection conn, String sql) {
		PreparedStatement pstm = null;
		try {
			pstm = conn.prepareStatement(sql);
			System.out.println(sql);
			pstm.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (pstm != null)
					pstm.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 释放资源
	 */
	private static void close(ResultSet rs, PreparedStatement pstm, Connection conn) {
		try {
			if (rs != null)
				rs.close();
			if (pstm != null)
				pstm.close();
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
