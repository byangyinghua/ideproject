package bzl.task;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import org.apache.commons.net.ntp.TimeStamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import com.alibaba.fastjson.JSONObject;

import bzl.common.Callback;
import bzl.common.Constant;
import bzl.common.SonThread;
import utils.RedisUtils;
import bzl.task.cmds;
import gnu.io.SerialPort;
import utils.SerialTool;

public class AdjustTimeTask {
	static String[] ntpTimeServers = new String[] { "ntp1.aliyun.com", "ntp2.aliyun.com", // 阿里云
			"ch.pool.ntp.org", // 阿里云
			"time.windows.com" };
	private static Timer timer = new Timer();

	public static void startTaskTimer() {
		timer.schedule(new TimerTask() {
			public void run() {
				SocketMsgHandler socketMsg = new SocketMsgHandler();
				Set<String> onlineKeys = RedisUtils.getKeys(Constant.OnlineTerminals);
				for (String tmpKey : onlineKeys) {
					String onlineInfos = RedisUtils.get(tmpKey);
					String terminal_ip = onlineInfos.split(":")[0];
					judgeDeviceTime(terminal_ip, socketMsg);
				}
			}
		}, 1000, 300 * 1000); // 12小时检查一次
	}

	public static void judgeDeviceTime(String terminal_ip, SocketMsgHandler socketMsg) {
		
		SonThread listenThread = new SonThread();
		listenThread.setCallback(new Callback() {
			@Override
			public JSONObject callBack(String key, JSONObject valObj) {
				// TODO Auto-generated method stub
				// SocketMsgHandler socketMsg = new SocketMsgHandler();
				JSONObject socketBody = new JSONObject();
				//SimpleDateFormat f=new SimpleDateFormat("yyyyMMddHHmmss");
				String correctTime = getTimeFromInternet(ntpTimeServers.length,"yyyyMMddHHmmss");
				if(correctTime ==null) {
					correctTime = getDateTimeFromGPS("yyyyMMddHHmmss");
				}
				if(correctTime ==null) {
					SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");  
					formatter.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
					correctTime = formatter.format(new Date());
				}
				socketBody.put("time", correctTime);
				if (correctTime != null) {
					socketMsg.sendMsgTo(terminal_ip, cmds.AJUST_TERMINAL_TIME, socketBody.toJSONString());
					dateTimeSynchronization();
					System.out.println("the correct time is:" + socketBody.toJSONString());
				} else {
					System.out.println("get correct time  failed!,now=" + new Date().getTime());
				}
				return null;
			}
		});
		listenThread.start();
		return;
	}
	
	
	//从网络上获取时间
	private static String getTimeFromInternet(int totalIndex,String formatSrt) {
		if (totalIndex == 0) {
			return null;
		}
		try {
			NTPUDPClient timeClient = new NTPUDPClient();
			timeClient.setDefaultTimeout(2000);
			timeClient.open();
			String timeServerUrl = ntpTimeServers[totalIndex - 1];// 中国科学院国家授时中心 
			InetAddress timeServerAddress = InetAddress.getByName(timeServerUrl);
			TimeInfo timeInfo = timeClient.getTime(timeServerAddress);
			TimeStamp timeStamp = timeInfo.getMessage().getTransmitTimeStamp();
			DateFormat dateFormat = new SimpleDateFormat(formatSrt);
			dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
			System.out.println("get time from internet server:" + ntpTimeServers[totalIndex - 1]
					+ dateFormat.format(timeStamp.getDate()));
			timeClient.close();
			return dateFormat.format(timeStamp.getDate());
		} catch (UnknownHostException e) {
			System.out.println("failed get time from internet server:" + ntpTimeServers[totalIndex - 1]);
			//e.printStackTrace();
			return getTimeFromInternet(totalIndex - 1,formatSrt);
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("failed get time from internet server:" + ntpTimeServers[totalIndex - 1]);
			return getTimeFromInternet(totalIndex - 1,formatSrt);
		}
	}
	
	//从ＧＰＳ获取时间
	private static String getDateTimeFromGPS(String formatSrt) {
		   SerialTool serialTool = SerialTool.getSerialTool();
           List<String> portList = serialTool.findPort();
           StringBuffer gnrmcStr =  new StringBuffer();
           String retDateTime =null;

           for(String portName : portList){
                SerialPort serialPort = SerialTool.openPort(portName, 9600);
                byte data[] = SerialTool.readFromPort(serialPort);
           		if(data != null && data.length >= 1){//当串口中有数据时进行解析
					String dataStr = new String(data);
					gnrmcStr.append(dataStr);
					int index = dataStr.indexOf("$GNVTG");
					if(index != -1){
						SerialTool.closePort(serialPort);
						String[] tempDataStr = gnrmcStr.toString().split("\\$GNRMC");//第一层以$GNRMC为关键字拆分
						String[] tempDataStr2 = tempDataStr[1].split("\n");//第二层以换行符拆分
						String[] finalDataStr = tempDataStr2[0].split(",");//第三层以逗号拆分

						//System.out.println("@@@@@@@@@@@@@" + finalDataStr[1]);
						//System.out.println("@@@@@@@@@@@@@" + finalDataStr[9]);
						//拆分年，月，日
						String year = "20" + finalDataStr[9].substring(0, 2);
						String month = finalDataStr[9].substring(2, 4);
						String day = finalDataStr[9].substring(4);
						String dateStr = null;
						if(formatSrt.equals("yyyyMMddHHmmss")) {
							dateStr = year + month  + day;
						}else {
							dateStr = year + "-" + month + "-" + day;
						}
						retDateTime = dateStr;
						
						//拆分小时，分钟，秒
						int hourInt = Integer.valueOf(finalDataStr[1].substring(0, 2)) + 8;
						String hour = String.valueOf(hourInt);
						String minute = finalDataStr[1].substring(2, 4);
						String second = finalDataStr[1].substring(4, 6);
						String timeStr =null;
						if(formatSrt.equals("yyyyMMddHHmmss")) {
							timeStr = hour  + minute  + second;
							retDateTime += timeStr;
						}else {
							timeStr = hour + ":" + minute + ":" + second;
							retDateTime += " " + timeStr;
						}
						break;
					}
           		}
           }
		return retDateTime;
	}
	
	/**
	 * @author  2013-11-21 下午05:55:43
	 * @功能：应用服务器时间与ntp服务器时间同步
	 */
	public static void dateTimeSynchronization() {
		 String osName = System.getProperty("os.name");
		 String datetime = getTimeFromInternet(ntpTimeServers.length,"yyyy-MM-dd HH:mm:ss");
		 if(datetime ==null) {
			 datetime = getDateTimeFromGPS("yyyy-MM-dd HH:mm:ss");
		 }
		 String date = datetime.substring(0, 10);
		 String time = datetime.substring(11);
		    try {
		        if (osName.matches("^(?i)Windows.*$")) { // Window 系统
		            String cmd;
		            cmd = " cmd /c date " + date; // 格式：yyyy-MM-dd
		            Runtime.getRuntime().exec(cmd);
		            cmd = " cmd /c time " + time; // 格式 HH:mm:ss
		            Runtime.getRuntime().exec(cmd);
		        } else if (osName.matches("^(?i)Linux.*$")) {// Linux 系统
		            String cmd = "date -s " + "\"" + date + " " + time + "\"";// 格式：yyyy-MM-dd HH:mm:ss
		            String[] comands = new String[] { "/bin/sh", "-p", "-c", cmd };
		            Process p = Runtime.getRuntime().exec(comands);
		            try {
						p.waitFor();
						if(p.exitValue()!=0){
							InputStream stderr = p.getErrorStream();
							InputStream stdin = p.getInputStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));
							BufferedReader err= new BufferedReader(new InputStreamReader(stderr));
							System.out.println("与ntp服务器同步时间错误！p.err(reader)=" + reader.readLine()); 
							System.out.println("与ntp服务器同步时间错误！p.err(err)=" + err.readLine()); 
						}else {
							System.out.println("服务器同步网络时间成功!"); 
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		            System.out.println("!!!!cmd=" + cmd);
		        } 
		    } catch (IOException e) {
		    	System.out.println("与ntp服务器同步时间错误！" + e.toString());
		    }
	}
}