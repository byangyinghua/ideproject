package bzl.common;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

import utils.NetUtil;

public class Configure {
	
	private static String localHostIP;
	
	private static String userHome;
	private static String uploadPath; //文件上传目录
	private static String socketPort; //udp 命令数据传输接口
	
	private static String rmtpBaseUrl;// rmtp直播基础url
	
	private static boolean hasRead = false;
	
	public static boolean  readAllConf() {
		
		if(!hasRead||"127.0.0.1".equals(localHostIP)) {
			Properties prop = new Properties();
			try {
				prop.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("properties/config.properties"));
				try {
					localHostIP =NetUtil.getNetcardIP("en");//ubuntu18.04　网卡命名方式变化
					rmtpBaseUrl = "rtmp://" + localHostIP + "/live/";
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					rmtpBaseUrl = prop.getProperty("rmtpBaseUrl").trim(); //从配置文件读取 文件存放路劲
				}
				uploadPath = prop.getProperty("uploadpath").trim(); //从配置文件读取 文件存放路劲
				socketPort = prop.getProperty("socketPort").trim(); //从配置文件读取 文件存放路劲
				
				userHome = System.getProperties().getProperty("user.home");
				
				System.out.println("localhost ip:" + localHostIP);
				
				System.out.println("uploadPath=" + uploadPath);
				System.out.println("socketPort=" + socketPort);
				System.out.println("rmtpBaseUrl=" + rmtpBaseUrl);
				System.out.println("userHome=" + userHome);
				hasRead = true;
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
	
		return hasRead;
	}
	
	public static String getUploadPath() {
		readAllConf();
		return uploadPath;
	}
	public static String getUserHomePath() {
		readAllConf();
		return userHome;
	}
	
	public static Integer getSocketPort() {
		readAllConf();
		return Integer.parseInt(socketPort);
	}
	
	public static String getRtmpBasePath() {
		readAllConf();
		return rmtpBaseUrl;
	}
	
	
}
