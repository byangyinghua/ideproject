package bzl.task;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.boyao.boyaonetlib.MsgSendImpl;
import com.boyao.boyaonetlib.acceptor.UdpMsgAcceptor;
import com.boyao.boyaonetlib.beans.ByUdpMsgInfo;
import com.boyao.boyaonetlib.interfaces.IMsgHandler;
import com.boyao.boyaonetlib.util.SequenceUtil;

import bzl.common.Configure;
import bzl.common.Constant;
import bzl.common.MemoryCache;
import bzl.controller.VideoController;
import bzl.entity.HelpInfo;
import bzl.entity.Terminal;
import bzl.entity.TerminalLog;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import bzl.service.impl.EntityServiceImpl;
import bzl.service.impl.MapServiceImpl;
import bzl.websocket.WebSocketEndpoint;
import sun.rmi.log.LogHandler;
import utils.Log;
import utils.RedisUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;

public class SocketMsgHandler implements ApplicationContextAware {
	static Logger log = Logger.getLogger(LogHandler.class);

	private static SocketMsgHandler instance = new SocketMsgHandler();

	private static UdpMsgAcceptor msgHandler = null;
	private static Integer socketPort = 7654;
	private static Integer broadcastPort = 7655;
	private static String broadcastAddress = "192.168.255.255";

	private static int updSuccess = 1; // 回复终端消息处理成功
	private static int udpFailed = 2; // 回复终端消息处理失败

	private static Map<Integer, Object> responseMap = new HashMap<Integer, Object>();

	private static MapService ms = new MapServiceImpl();
	private static EntityService es = new EntityServiceImpl();

	// private static MemoryCache localMemCache = new MemoryCache();

	private static int HeartBeatRate = 8; // 5s
	//终端状态维持时间
	private static int TernimalExpireTime = HeartBeatRate * 3;
	
	private static MapService getMsqlMapCtr() {
		if(ms==null) {
			ms = new MapServiceImpl();
		}
		return ms;
	}
	
	
	private static EntityService getMsqlEntityCtr() {
		if(es==null) {
			es = new EntityServiceImpl();
		}
		return es;
	}

	public static SocketMsgHandler getInstance(){
		return instance;
	}
	
	private Map<String, Object>  getTaskInfoByTaskId(String task_id) {
		Map conMap = new HashMap<String, Object>();
		
		JSONObject taskTypeObj = new JSONObject();
		taskTypeObj.put("TaskInfo", "task_id");//表名与字段
		taskTypeObj.put("BootSetting", "setting_id");//表名与字段
		taskTypeObj.put("LampSetting", "setting_id");//表名与字段
		taskTypeObj.put("ShieldTask", "shield_id");//表名与字段
		taskTypeObj.put("UrgencyTask", "urgency_id");//表名与字段
		
		Set<String> taskTypes = taskTypeObj.keySet();
		for(String taskType:taskTypes) {
			conMap.clear();
			conMap.put(taskTypeObj.get(taskType), task_id);
			List<Map<String, Object>> tasklist = getMsqlMapCtr().selectList(taskType, "selectByPK", conMap);
			if(tasklist!=null && tasklist.size()==1) {
				tasklist.get(0).put("table_name", taskType);
				tasklist.get(0).put(taskType, taskTypeObj.get(taskType));
				return tasklist.get(0);
			}
		}
		return null;
	}

	private  IMsgHandler recvHandler = new IMsgHandler() {
		@Override
		public String onHandleData(InetSocketAddress terminalInfo, int sequece, int packageType, int cmd, String body) {
			// TODO Auto-generated method stub
			String respBody = "";
			if (packageType == 0x01) { // 0x01是终端请求命令
				respBody = dealWithTerminalRequest(terminalInfo, sequece, cmd, body);
			} else if (packageType == 0x02) { // 0x02是响应服务端的命令
				msgHandler.receivedMsg(sequece);
				dealWithTerminalResponse(terminalInfo, sequece, cmd, body);
			}

			return respBody;
		}
	};

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// TODO Auto-generated method stub
		// 初始化相关信息 spring mvc getbean的时候会调用到这里
		//setTerminalOffline(); // 启动离线检查任务
		if (msgHandler == null) {
			msgHandler = new UdpMsgAcceptor(Configure.getSocketPort());
			msgHandler.setMsgHandler(recvHandler);
			msgHandler.setListener(new MsgSendImpl.SendMaxCountListener() {
				@Override
				public void onSendMaxCount(ByUdpMsgInfo arg0) {
					// TODO Auto-generated method stub
					log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					log.error(arg0);
					log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

				}
			});
			//开启重发机制，超时设置为1秒，重发次数2
			msgHandler.setResendAble(true , 1000 , 2);
			com.boyao.boyaonetlib.util.Log.DEBUG = true;
			findAllLocalTerminals();
		}

	}
	
	
	private void judgeTerminaTime(String devicetime,String terminal_ip) {
		
		TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
		TimeZone.setDefault(timeZone);
		SimpleDateFormat f=new SimpleDateFormat("yyyyMMddHHmmss");
		f.setTimeZone(timeZone);
		try {
			long deviceTimeSec=f.parse(devicetime).getTime()/1000;
			long nowSec = new Date().getTime()/1000;
			//System.out.print("terminal time:" + deviceTimeSec + ",server time:" +nowSec );
			if(Math.abs(deviceTimeSec-nowSec) >5) {
				AdjustTimeTask.judgeDeviceTime(terminal_ip, this);
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private String dealWithTerminalRequest(InetSocketAddress terminalInfo, int sequece, int cmd, String body) {
//		log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!! cmd=" + cmd);
		JSONObject jsonBody =new JSONObject();
		if(body!=null) {
			try {
				 jsonBody = JSONObject.parseObject(body);
			}catch(JSONException e){
				System.out.println("invalide request from  terminalbody="+body + ", cmd = " + cmd);
				e.printStackTrace();
			}
		}
		//log.error("terminal request jsonBody===" + jsonBody);
		JSONObject respBody = new JSONObject();
		String terminal_id = jsonBody.getString("deviceId");
		RedisUtils.refreshExpired(Constant.OnlineTerminals + ":" + terminal_id , TernimalExpireTime);
		int result = -1;

		if (cmd == cmds.HEARTBEAT) {
			String terminal_ip = terminalInfo.getAddress().getHostAddress();
			String deviceTime = jsonBody.getString("deviceTime");
			String devControlStatus = jsonBody.getString("devControlStatus");
			
			int currentState = jsonBody.getIntValue("currentState");// 设备当前状态:1为空闲、2为任务中、3为播流中、4为其他
			int currentVolume = jsonBody.getIntValue("currentVolume");
			Terminal updateTerminal = new Terminal();
			updateTerminal.setTerminal_id(terminal_id);
			updateTerminal.setIp(terminal_ip);
			//updateTerminal.setState(currentState);
			updateTerminal.setVolume(currentVolume);
			updateTerminal.setLamp_status(devControlStatus);
		
			judgeTerminaTime(deviceTime,terminal_ip);
			
			//log.error("update heartbeat devControlStatus=" + devControlStatus);
			result = getMsqlEntityCtr().update("Terminal", "update", updateTerminal);
			if (result == 1) {
				//RedisUtils.setExpire("online:" + terminal_id, "" + currentState, HeartBeatRate + 5);
				RedisUtils.setExpire(Constant.OnlineTerminals + ":" + terminal_id, terminal_ip + ":" + currentState, TernimalExpireTime);
				
				// localMemCache.setData(terminal_id, currentState, HeartBeatRate + 5);
				respBody.put("status", updSuccess);
				respBody.put("serverTime" , System.currentTimeMillis());
			} else {
				log.error("update heartbeat online failed!");
				respBody.put("status", udpFailed);
			}
		} else if (cmd == cmds.TERMINAL_REGISTER) {
			Terminal newTerminal = new Terminal();
			Map<String,Object> condTerminal = new HashMap<String,Object>();
			JSONObject bootInfo = JSONObject.parseObject(jsonBody.getString("bootInfo"));
			newTerminal.setTerminal_id(terminal_id);// deviceId 是终端的唯一标志
			newTerminal.setBoot_time(bootInfo.getString("bootTime"));
			newTerminal.setShutdown_time(bootInfo.getString("shutTime"));
			newTerminal.setIp(terminalInfo.getAddress().getHostAddress());
			//newTerminal.setState(1);// 1 是在线 0是不在线
			newTerminal.setErr_msg("");
			newTerminal.setVolume(jsonBody.getIntValue("volume"));
			newTerminal.setApp_ver(jsonBody.getString("softVersion"));
			
			String installAddr = jsonBody.getString("address");
			String terminalName = jsonBody.getString("deviceName");
			if(installAddr !=null && installAddr.length()>0) {
				newTerminal.setInstall_addr(installAddr);
			}
			
			if(terminalName !=null && terminalName.length() >0) {
				newTerminal.setName(terminalName);
			}
			condTerminal.put("terminal_id",terminal_id);// deviceId 是终端的唯一标志
			List<Map<String, Object>> list = getMsqlMapCtr().selectList("Terminal", "selectByCondition", condTerminal);
			if (list == null || list.isEmpty()) {
				result = getMsqlEntityCtr().insert("Terminal", "insert", newTerminal);
				if (result == 1) {
					Log.d("test" , "newTerminal is registed " + terminal_id);
					respBody.put("status", updSuccess);
					RedisUtils.setExpire(Constant.OnlineTerminals + ":" + terminal_id, newTerminal.getIp() + ":" + 1, TernimalExpireTime);
				} else {
					condTerminal.clear();
					condTerminal.put("ip",terminalInfo.getAddress().getHostAddress());// deviceId 是终端的唯一标志
					list = getMsqlMapCtr().selectList("Terminal", "selectByCondition", condTerminal);
					if(list!=null && list.size() >0) {
						String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) list.get(0).get("terminal_id"));
						if(onlineInfo==null) {
							result = getMsqlEntityCtr().delete("Terminal", "delete", condTerminal);
						}
					}
					log.error("2222v the device has register return failed!newTerminal=");
					log.error(newTerminal);
					respBody.put("status", udpFailed);
				}

			} else {
				result =getMsqlEntityCtr().update("Terminal", "update", newTerminal);
				if (result == 1) {
					Log.d("test" , "newTerminal is registed " + terminal_id);
					respBody.put("status", updSuccess);
					RedisUtils.setExpire(Constant.OnlineTerminals + ":" + terminal_id, newTerminal.getIp() + ":" + 1, TernimalExpireTime);
				} else {
					condTerminal.clear();
					condTerminal.put("ip",terminalInfo.getAddress().getHostAddress());// deviceId 是终端的唯一标志
					list = getMsqlMapCtr().selectList("Terminal", "selectByCondition", condTerminal);
					if(list!=null && list.size() >0) {
						String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) list.get(0).get("terminal_id"));
						if(onlineInfo==null) {
							result = getMsqlEntityCtr().delete("Terminal", "delete", condTerminal);
						}
					}
					log.error("2222v the device has register,return failed!newTerminal=" + newTerminal.toString());
					respBody.put("status", udpFailed);
				}
			}
		} else if (cmd == cmds.REPORT_HELP) {
			HelpInfo newHelpInfo = new HelpInfo();
			System.out.println("terminal report help information!");
			System.out.println(jsonBody);
			newHelpInfo.setHelp_id(jsonBody.getString("helpId"));
			newHelpInfo.setTerminal_id(jsonBody.getString("terminal_id"));
			newHelpInfo.setTerminal_ip(jsonBody.getString("terminal_ip"));
			newHelpInfo.setHelp_status(0);// 0为未处理
			newHelpInfo.setHelp_time(jsonBody.getString("help_time"));
			newHelpInfo.setVideo_list("");
//			{"helpId":"1568108478262","terminal_ip":"192.168.101.14","terminal_id":"103001014108ab02c2a4","help_time":"20190910174118"}
			result = getMsqlEntityCtr().insert("HelpInfo", "insert", newHelpInfo);
			if (result == 1) {
				System.out.println("1111 the device has newHelpInfo!device_id=" + jsonBody.getString("terminal_id"));
				respBody.put("status", updSuccess);
				Map<String, Object> condTerminal1 = new HashMap<String, Object>();
				condTerminal1.put("ip", jsonBody.getString("terminal_ip"));
				//condTerminal1.setIp();
				List<Map<String,Object>> terminalList =  getMsqlMapCtr().selectList("Terminal", "selectByCondition", condTerminal1);
				if(terminalList!=null && terminalList.size()==1) {
					JSONObject msgBody = new JSONObject();
					String terminalName = "未命名";
					if(terminalList.get(0).get("name")!=null && ((String)terminalList.get(0).get("name")).length() >0) {
						terminalName = (String) terminalList.get(0).get("name");
					}
					msgBody.put("id", jsonBody.getString("helpId"));
					msgBody.put("title", "来自终端[" + terminalName + "]("+jsonBody.getString("terminal_ip")+")" +"的求助信息!");
					msgBody.put("content", "来自终端[" + terminalName + "]("+jsonBody.getString("terminal_ip")+")" +"的求助信息!");
					msgBody.put("terminal_id", jsonBody.getString("terminal_id"));
					msgBody.put("msgtype", 1);//1为求助
					WebSocketEndpoint.pushMsg(WebSocketEndpoint.PUSH_MSG_SES, jsonBody.getString("terminal_ip"),jsonBody.getString("terminal_id"), msgBody);
				}	
			} else {
				log.info("2222v the device has newHelpInfo!device_id=" + terminal_id);
				respBody.put("status", udpFailed);
			}
		} else if (cmd == cmds.REPORT_FILEDOWNLOAD_STATUS) {
			TerminalLog tlog = new TerminalLog();
			tlog.setAction("file_download");
			tlog.setContent(body);
			log.info("REPORT_FILEDOWNLOAD_STATUS download file failed!device_id=" + body);

		} else if (cmd == cmds.TERMINAL_CHECK_UPDATE) {
			Map<String, String> appUpdateInfo = RedisUtils.hgetAll(Constant.AppUpdateInfo); // 查看升级信息设置
			System.out.println("terminal check update:" + appUpdateInfo.toString());
			Map<String, Object> condMap = new HashMap<String, Object>();
			String updating = appUpdateInfo.get("updating");
			String attach_id = appUpdateInfo.get("attach_id");
			condMap.put("attach_id", attach_id);
			if (updating!=null && updating.equals("on")) { // on是升级中
				List<Map<String, Object>> attachList = getMsqlMapCtr().selectList("Attachment", "selectByPK", condMap);
				if (attachList != null && attachList.size() == 1) {
					respBody.put("status", updSuccess);
					respBody.put("newVersion", appUpdateInfo.get("new_version"));
					respBody.put("download_url", ":8080/cms/file/download/" + attach_id);// 提供下载链接给终端
				} else {
					System.out.println("appUpdateInfo.get(\"new_version\")=" + appUpdateInfo.get("new_version"));
					System.out.println("appUpdateInfo.get(\"current_ver\")=" + jsonBody.get("current_ver"));
					respBody.put("status", updSuccess);
					respBody.put("download_url", "");
				}
			} else {
				respBody.put("status", updSuccess);
				respBody.put("download_url", "");
			}

		} else if (cmd == cmds.TERMINAL_REPORT_UPLOAD_PROCESS) {
			String terminal_ip = terminalInfo.getAddress().getHostAddress();
			int currentStep = jsonBody.getIntValue("currentStep");
			int info = jsonBody.getIntValue("info");
			String msg = jsonBody.getString("msg");

			Map<String, Object> condMap1 = new HashMap<String, Object>();
			condMap1.clear();
			condMap1.put("ip", terminal_ip);

			System.out.println("update report jsonBody:" + jsonBody.toJSONString());

			List<Map<String, Object>> terminalList = getMsqlMapCtr().selectList("Terminal", "selectByCondition", condMap1);
			System.out.println("terminals toString:" + terminalList.toString());
			if (terminalList != null && terminalList.size() == 1) {
				System.out.println("currentStep currentStep:" + currentStep);
				if (currentStep == 3) {
					if (info == 1) {
						RedisUtils.addMember(Constant.UpdateOkTerminals,
								(String) terminalList.get(0).get("terminal_id"), 0); // 升级成功的终端集合
					} else if (info == 0) {
						RedisUtils.addMember(Constant.UpdateFailedTerminals,
								(String) terminalList.get(0).get("terminal_id"), 0); // 升级失败的终端集合
					}
				}
			}
		} else if (cmd == cmds.TERMINAL_REPORT_ERR_LOG) {
			String terminal_ip = terminalInfo.getAddress().getHostAddress();
			String ErrMsg = jsonBody.getString("msg");
			String reportTime = jsonBody.getString("time");
			Terminal updateTerminal = new Terminal();
			updateTerminal.setIp(terminal_ip);
			updateTerminal.setErr_msg(ErrMsg + "(" + reportTime +")");
			
			result = getMsqlEntityCtr().update("Terminal", "updateByIp", updateTerminal);
			//System.out.println(" TERMINAL_REPORT_ERR_LOG result=" + cmd +",ErrMsg:"+ErrMsg);
			if (result == 1) {
				respBody.put("status", updSuccess);
			} else {
				log.error("update heartbeat online failed!");
				respBody.put("status", udpFailed);
			}

		}else if(cmd ==cmds.CHECK_TASK_STATUS) {
			//String terminal_ip = terminalInfo.getAddress().getHostAddress();
			JSONArray taskIdList = jsonBody.getJSONArray("taskIds");
			JSONArray startTaskIds = new JSONArray();
			JSONArray notStartTaskIds = new JSONArray();
			JSONArray notExistTaskIds = new JSONArray();
			if(taskIdList !=null && taskIdList.size() >0) {
				for(int i=0;i<taskIdList.size();i++) {
					Set<String> ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + taskIdList.getString(i));
					if(ok_terminals.contains(terminal_id)) {
						startTaskIds.add(taskIdList.getString(i));
					}else if(null!=getTaskInfoByTaskId(taskIdList.getString(i))) {
						notStartTaskIds.add(taskIdList.getString(i));
					}else {
						notExistTaskIds.add(taskIdList.getString(i));
					}
				}
			}
			respBody.put("startTaskIds", startTaskIds);
			respBody.put("notStartTaskIds", notStartTaskIds);
			respBody.put("notExistTaskIds", notExistTaskIds);
		}else {
			System.out.println("unknow termina cmd=" + cmd);
		}
		
		//log.error("response to terminal:" + respBody.toJSONString());

		return respBody.toJSONString();
	}

	private  String dealWithTerminalResponse(InetSocketAddress terminalInfo, int sequece, int cmd, String body) {
		// 这个方法处理终端回复
		log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!! cmd=" + cmd);
		log.info("terminal resp jsonBody===" + body);
		log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!! cmd = " + cmd);
		if(body==null) {
			body="{}";
		}

		if (body != null) {
			JSONObject jsonBody = JSONObject.parseObject(body);
			JSONObject respBody = new JSONObject();
			JSONObject returnBody = new JSONObject();
			if (body != null && body.length() > 0) {
				//System.out.println("map receive response  msg from terminal,cmd=" + cmd + ",sequece=" + sequece + ",body=" +body);
				String terminal_ip = terminalInfo.getAddress().getHostAddress();
				returnBody.put("terminal_ip", terminal_ip);
				returnBody.put("resp", body);
				responseMap.put(sequece, returnBody); // 放入返回map
			}
		} else {
			log.info("terminal resp jsonBody is null");
		}

		switch (cmd) {
		case cmds.BROADCAST: // 终端回复广播消息
			System.out.println("receive broadcast msg from terminal:" + terminalInfo.getAddress());
			break;
		default:
			System.out.println("default receive response  msg from terminal,cmd=" + cmd + ",sequece=" + sequece + ",body=" +body);

		}

		return null;
	}
	
	private void findTerminalTask() { //  not use now
//		Timer timer = new Timer();
//		timer.schedule(new TimerTask() {
//			public void run() {
//				findAllLocalTerminals();
//		        }
//		}, 1000 , 1000*1800); // 30minutes 检查一次
	}

	public  void findAllLocalTerminals() {
		msgHandler.sendBroastMsg("255.255.255.255", broadcastPort, SequenceUtil.getNewSequence(), 0x01, 0x01,"{by:123456}");
		msgHandler.sendBroastMsg("192.168.255.255", broadcastPort, SequenceUtil.getNewSequence(), 0x01, 0x01,"{by:123456}");
		msgHandler.sendBroastMsg("10.168.255.255", broadcastPort, SequenceUtil.getNewSequence(), 0x01, 0x01,"{by:123456}");
		System.out.println("\nsend find terminal broadcast msg to 192.168.255.255 !!!");
		System.out.println("\nsend find terminal broadcast msg to 255.255.255.255 !!!");
		System.out.println("\nsend find terminal broadcast msg to 10.168.255.255 !!!");
	}

	/**
	 * 广播信息，暂用于校时，保证终端系统时间一致性
	 */
	public void broastMsg(String msgInfo){
		if(msgHandler != null) {
			msgHandler.sendBroastMsg("255.255.255.255", broadcastPort, SequenceUtil.getNewSequence(), 0x26, 0x01, String.valueOf(msgInfo));
		}
	}

	// 异步转同步发送接口，等待终端返回后，接口再返回
	public JSONObject syncSendMsgTo(String terminalIp, int cmd, String body, long timeout) {
		int sequece = SequenceUtil.getNewSequence();
		JSONObject resultObj = null;
		if (!responseMap.containsKey(sequece)) {
			msgHandler.sendMsg(terminalIp, sequece, cmd, 0x01, body);

			System.out.println("at " +System.currentTimeMillis() + " syncSendMsgTo socke msg to :" + terminalIp + ",cmd:" + cmd + ",sequece:"+sequece+ ",body:" + body);
			long beginTime = System.currentTimeMillis();

			while ((System.currentTimeMillis() - beginTime < timeout)) {
				//resultObj = JSONObject.par responseMap.get(sequece);
				Object tmpObj = responseMap.get(sequece);
				if(tmpObj !=null) {
					resultObj = (JSONObject) JSONObject.toJSON(tmpObj);
					//System.out.println("at " +System.currentTimeMillis() + " recieve:terminal" + terminalIp + ",cmd:" + cmd + ",sequece:"+sequece+  ",body:" + resultObj);
					break;
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}//等待两毫秒
			}
		} else {
			System.out.println("\nthe is same sequece in response map:" + sequece);
			resultObj = (JSONObject) JSONObject.toJSON(responseMap.get(sequece));
		}

		responseMap.remove(sequece);
		return resultObj;
	}
	
	// 异步发送，无需等待结果,返回sequese
	public int sendMsgTo(String terminalIp, int cmd, String body) {
		
		int sequece = SequenceUtil.getNewSequence();
		if (!responseMap.containsKey(sequece)) {
			msgHandler.sendMsg(terminalIp, sequece, cmd, 0x01, body);
			System.out.println("send socket msg to :" + terminalIp + ",sequece" + sequece +  ",cmd:" + cmd + ",body:" + body);
		} else {
			System.out.println("\nthe is same sequece in response map:" + sequece);
		}
		return sequece;
		
		//msgHandler.sendRequestMsg(terminalIp, socketPort, cmd, body);
	}
	
	public static Map<Integer,JSONObject> getTerminalRespBySequece(Map<Integer,String> sequeceMap,long timeout){
		Map<Integer,JSONObject> resultList = new HashMap<Integer,JSONObject>();
		if(sequeceMap != null && sequeceMap.size() >0) {
			long beginTime = System.currentTimeMillis();
			while ((System.currentTimeMillis() - beginTime < timeout)) {
				for(Map.Entry<Integer, String> entry : sequeceMap.entrySet()) {
					Object tmpObj = responseMap.get(entry.getKey());
					if(tmpObj !=null) {
						JSONObject resultObj = (JSONObject) JSONObject.toJSON(tmpObj);
						resultObj.put("terminal_id", entry.getValue());
						resultList.put(entry.getKey(), resultObj);
						//System.out.println("at " +System.currentTimeMillis() + "read:terminal" + resultObj.getString("terminal_ip") + ",sequece:"+sequeceList.getIntValue(i)+  ",body:" + resultObj.getString("resp"));
						sequeceMap.remove(entry.getKey());
						break;
					}
				}
				
				if(sequeceMap.size()==0) {
					break;
				}
				try {
					Thread.sleep(100);//等待5毫秒
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else {
			resultList =null;
		}
		return resultList;
	}
}