package bzl.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import utils.SerialTool;

/**
 * 串口监听
 * <p>mark on 2019年4月18日.</p>
 * @author 刘亚一
 */

public class SerialListener implements SerialPortEventListener  {

	private SerialPort serialPort = null;
	private StringBuffer gnrmcStr = null;
	
	public SerialListener(SerialPort serialPort){
		gnrmcStr = new StringBuffer();
		this.serialPort = serialPort;
	}
	
	@Override
	public void serialEvent(SerialPortEvent serialPortEvent) {
		// TODO Auto-generated method stub
		switch(serialPortEvent.getEventType()){
			case SerialPortEvent.DATA_AVAILABLE://成功获取串口数据
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				byte data[] = null;
				if(serialPort != null){//检测到了串口号，开始读取
					
					data = SerialTool.readFromPort(serialPort);
					
					if(data != null && data.length >= 1){//当串口中有数据时进行解析
						
						String dataStr = new String(data);
						
						gnrmcStr.append(dataStr);
						int index = dataStr.indexOf("$GNVTG");
				    	
						//System.out.println("?????????????" + index);
						
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
							
							String dateStr = year + "-" + month + "-" + day;
							System.out.println(dateStr);
							
							//拆分小时，分钟，秒
							int hourInt = Integer.valueOf(finalDataStr[1].substring(0, 2)) + 8;
							String hour = String.valueOf(hourInt);
							String minute = finalDataStr[1].substring(2, 4);
							String second = finalDataStr[1].substring(4, 6);
							
							String timeStr = hour + ":" + minute + ":" + second;
							System.out.println(timeStr);
							
							String osName = System.getProperty("os.name");
							
							try{
								
								if(osName.matches("^(?i)Windows.*$")){//设置windows系统时间
									
									Runtime.getRuntime().exec("cmd /c time " + timeStr);
									//Process p = Runtime.getRuntime().exec("g:/nircmd-x64/nircmd.exe elevate time 08:55:00");
									//Process p2 = Runtime.getRuntime().exec("ping 127.0.0.1");
									/*InputStream in = p.getErrorStream();
									
									BufferedReader reader = new BufferedReader(new InputStreamReader(in));
									String line;
									while((line = reader.readLine()) != null){
										System.out.println(line);
									}
									p.waitFor();
									in.close();
									reader.close();
									p.destroy();*/
									
									Runtime.getRuntime().exec("cmd /c date " + dateStr);
									
								}else{//设置linux系统时间
									 String cmd = "date -s " + "\"" + dateStr + " " + timeStr + "\"";// 格式：yyyy-MM-dd HH:mm:ss
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
								}
								
							}catch(Exception e){
								e.printStackTrace();
							}
						}
						
					}else{
						System.out.println("串口无数据！");
					}
				}else{
					
				}
				
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY://输出缓冲区已清空
				break;
			case SerialPortEvent.BI://通讯中断
				System.out.println("与串口通讯已中断......");
				break;
		}
	}

}
