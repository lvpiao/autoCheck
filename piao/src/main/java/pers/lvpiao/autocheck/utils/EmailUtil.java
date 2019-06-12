package pers.lvpiao.autocheck.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

public class EmailUtil {

	private static String from;
	private static String fromPasswrod;
	private static ArrayList<String> to = new ArrayList<String>();
	private static Properties pro;
	// 初始化，读配置文件
	static {
		pro = PropertiesUtil.loadProperties("email.properties");
		from = pro.getProperty("from");
		fromPasswrod = pro.getProperty("fromPassword");
		// 加载接受者
		to = PropertiesUtil.getParasAsList(pro, "to");
	}

	/*
	 * @para content 邮件内容
	 */
	public static void sendQQEmail(String content) {
		Properties properties = System.getProperties();
		properties.put("mail.transport.protocol", pro.getProperty("protocol"));// 连接协议
		properties.put("mail.smtp.host", pro.getProperty("host"));// 主机名
		properties.put("mail.smtp.port", pro.getProperty("port"));// 端口号
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.ssl.enable", "true");// 设置使用ssl安全连接
		// 获取默认session对象
		Session session = Session.getDefaultInstance(properties, new Authenticator() {
			public PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(from, fromPasswrod); // 发件人邮件用户名、密码
			}
		});

		try {
			// 创建默认的 MimeMessage 对象
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			InternetAddress[] receivers = new InternetAddress[to.size()];
			int i = 0;
			for (String t : to) {
				receivers[i++] = new InternetAddress(t);
			}
			message.setRecipients(Message.RecipientType.TO, receivers);
			// 设置邮件标题
			message.setSubject(TimeUtil.getFormattedTime("yyyy年MM月dd日HH点mm分的检测结果"));

			// 创建多重消息
			Multipart multipart = new MimeMultipart();

			// 添加文件部分，如果存在
			if (new File(FileUtil.excelFilePath).exists()) {
				BodyPart FilePart = new MimeBodyPart();
				DataSource source = new FileDataSource(FileUtil.excelFilePath);
				FilePart.setDataHandler(new DataHandler(source));
				FilePart.setFileName(MimeUtility.encodeText(FileUtil.excelFileName));
				multipart.addBodyPart(FilePart);
			}
			// 创建文字消息部分
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(content);
			multipart.addBodyPart(messageBodyPart);

			// 发送完整消息
			message.setContent(multipart);
			// 发送消息
			Transport.send(message);
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

//	public static void main(String[] args) {
//		EmailUtil.sendQQEmail("sss");
//	}
}