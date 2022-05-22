package bzl.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sun.rmi.log.LogHandler;
import utils.Convert;
import utils.HttpIO;
import utils.RedisUtils;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

import bzl.common.Constant;
import bzl.common.SesCheck;
import bzl.entity.User;
import bzl.entity.BootSetting;
import bzl.entity.Terminal;
import bzl.service.EntityService;
import bzl.service.MapService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
//import org.system.socket.TaskSocket;
//import org.system.socket.ver2.SocketConnection;
//import org.system.socket.ver2.SocketServer;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import bzl.task.SocketMsgHandler;
import bzl.task.cmds;


//定时开关机设置
@Controller
@RequestMapping("/bootsetting")
public class BootSettingController {

	// private static Logger logger = Logger.getLogger(UserController.class);
	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	
	private List<Map<String,Object>> convertJsonStr2Array(List<Map<String,Object>> bootsettingList) {
		
		for(int i=0;i<bootsettingList.size();i++) {
			JSONArray weekDays = JSONObject.parseArray((String) bootsettingList.get(i).get("week_days"));
			bootsettingList.get(i).put("week_days", weekDays);
			Set<String> ready_terminals = RedisUtils.allSetData(Constant.TaskTerminalReady + bootsettingList.get(i).get("setting_id"));
			bootsettingList.get(i).put("ready_terminals", ready_terminals.toArray());
			Set<String> ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + bootsettingList.get(i).get("setting_id"));
			bootsettingList.get(i).put("ok_terminals", ok_terminals.toArray());
			Set<String> fail_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsFailed + bootsettingList.get(i).get("setting_id"));
			bootsettingList.get(i).put("fail_terminals", fail_terminals.toArray());
		}
		
		return bootsettingList;
		
	}
	
	
	private int checkActionPermission(String uid,String setting_id) {
		int result =0;
		Map<String,Object> conMap = new HashMap<String,Object>();
		conMap.put("setting_id", setting_id);
		Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(uid);
		if(tmpUserSet!=null && tmpUserSet.size() >0) {
			conMap.put("creator_uids", tmpUserSet.toArray());
			List<Map<String,Object>> bootsettingList = ms.selectList("BootSetting", "selectByCondition", conMap);
			if(bootsettingList!=null && bootsettingList.get(0).get("creator_uid").equals(uid)) {
			     result =1;
			}
		}
		return result;
	}
	
	//获取终端定时开关机设置
	@RequestMapping(value = "/getsettings",method=RequestMethod.POST)
	@ResponseBody
	public void getTerminalBottSetting(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			Map<String,Object> conMap = new HashMap<String,Object>();
			if(adminUser.getIs_supper()==0) {
				Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(adminUser.getUid());
				if(tmpUserSet!=null && tmpUserSet.size() >0) {
					conMap.put("creator_uids", tmpUserSet.toArray());
				}else {
					tmpUserSet = new HashSet<String>();
					tmpUserSet.add(RandomStringUtils.randomAlphanumeric(12));
					conMap.put("creator_uids", tmpUserSet.toArray());
				}
			}
			
			if(getTotal !=null) {
				List<Map<String,Object>> totalList= ms.selectList("BootSetting","selectCountByCondition", conMap);
				if(totalList !=null && totalList.size()==1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}
			
			conMap.put("startrom", (page-1)*pagesize);
			conMap.put("pagesize",pagesize);
			conMap.put("sort", "update_time desc");
			List<Map<String,Object>> bootsettingList = ms.selectList("BootSetting", "selectByConditionWithPage", conMap);
			bootsettingList = convertJsonStr2Array(bootsettingList);
			bootsettingList = Convert.SortDataListId(bootsettingList,page,pagesize);
			if(bootsettingList != null && bootsettingList.size() >0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result",bootsettingList);
			}else {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("msg", Constant.NodataErr);
			}
		}else {
			respJson.put("result", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		 HttpIO.writeResp(response, respJson);
	}
	
	//获取终端定时开关机设置
	@RequestMapping(value = "/getsettingById",method=RequestMethod.POST)
	@ResponseBody
	public void getTerminalBottSettingById(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String setting_id = jsonBody.getString("setting_id");
			Map<String,Object> conMap = new HashMap<String,Object>();
			conMap.put("setting_id", setting_id);
			if(adminUser.getIs_supper()==0) {
				Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(adminUser.getUid());
				if(tmpUserSet!=null && tmpUserSet.size() >0) {
					conMap.put("creator_uids", tmpUserSet.toArray());
				}else {
					tmpUserSet = new HashSet<String>();
					tmpUserSet.add(RandomStringUtils.randomAlphanumeric(12));
					conMap.put("creator_uids", tmpUserSet.toArray());
				}
			}
			
			List<Map<String,Object>> bootsettingList = ms.selectList("BootSetting", "selectByCondition", conMap);
			bootsettingList = convertJsonStr2Array(bootsettingList);
			if(bootsettingList != null && bootsettingList.size() ==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result",bootsettingList);
			}else {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("msg", Constant.NodataErr);
			}
		}else {
			respJson.put("result", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		 HttpIO.writeResp(response, respJson);
	}
	
	   //
		@RequestMapping(value="/setReadyOrFailedTerminal",method=RequestMethod.POST)
		@ResponseBody
		public void setReadyOrFailedTerminal(HttpServletRequest request,
					HttpServletResponse response) {
			JSONObject respJson = new JSONObject();
			int result = 0;
			User adminUser = SesCheck.getUserBySession(request, es,false); 
			if(adminUser != null) {
				String jsonBodyStr = HttpIO.getBody(request);
				JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
				//BootSetting newBootSet = new BootSetting();
				String setting_id = jsonBody.getString("setting_id");
				JSONArray ready_terminals = jsonBody.getJSONArray("ready_terminals");//ip
				JSONArray failed_terminals = jsonBody.getJSONArray("fail_terminals");//ip
				int hasSettPermission = 0;
				if(adminUser.getIs_supper()==0) {
					hasSettPermission=checkActionPermission(adminUser.getUid(),setting_id);
				}else {
					hasSettPermission = 1;
				}
				
				if(hasSettPermission==1) {
					Set<String> ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + setting_id);
					for(String ok_terminal:ok_terminals) {
						if(ready_terminals!=null) {
							ready_terminals.remove(ok_terminal);
						}
						if(failed_terminals!=null) {
							failed_terminals.remove(ok_terminal);
						}
					}
					
					if(ready_terminals!=null) {
						RedisUtils.del(Constant.TaskTerminalReady + setting_id);//移除然后在新增
						for(int i=0;i<ready_terminals.size();i++) {
							RedisUtils.addMember(Constant.TaskTerminalReady + setting_id, ready_terminals.getString(i), 0);
							result =1;
						}
					}
					
					if(failed_terminals!=null) {
						//RedisUtils.del(Constant.TaskSentTerminalsFailed + setting_id);//移除然后在新增
						for(int i=0;i<failed_terminals.size();i++) {
							RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + setting_id, failed_terminals.getString(i));
							result =1;
						}
					}
				}
				
				if(result==1) {
					respJson.put("status", Constant.SUCCESS);
					respJson.put("msg", Constant.SuccessMsg);
				}else {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", Constant.FailedMsg);
				}
			}else {
				respJson.put("status", Constant.UserNotLogin);
				respJson.put("msg", Constant.PermissionErr);
			}
				
			HttpIO.writeResp(response, respJson);	
		}
	
	
	//
	@RequestMapping(value="/addOrUpdate",method=RequestMethod.POST)
	@ResponseBody
	public void addOrUpdateBootSettings(HttpServletRequest request,
				HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es,false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			BootSetting newBootSet = new BootSetting();
			String setting_id = jsonBody.getString("setting_id");
			String boot_time = jsonBody.getString("boot_time");
			String shutdown_time = jsonBody.getString("shutdown_time");
			JSONArray week_days = jsonBody.getJSONArray("week_days");
			//JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			
			newBootSet.setBoot_time(boot_time);
			newBootSet.setShutdown_time(shutdown_time);
			newBootSet.setWeek_days(week_days.toJSONString());
			if(setting_id !=null && setting_id.length() >0) {
				Set<String> sendTerminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + setting_id);
				if(sendTerminals==null||sendTerminals.size()==0) {
					newBootSet.setSetting_id(setting_id);
					if(adminUser.getIs_supper()==0) {
						int hasSettPermission=checkActionPermission(adminUser.getUid(),setting_id);
						if(hasSettPermission==1) {
							result = es.update("BootSetting", "update", newBootSet);
						}
					}else {
						result = es.update("BootSetting", "update", newBootSet);
					}
				}
			}else {
				newBootSet.setCreator(adminUser.getUsername());
				newBootSet.setCreator_uid(adminUser.getUid());
				newBootSet.setSetting_id("sid" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(5));
				result = es.insert("BootSetting", "insert", newBootSet);
			}
			
			if(result==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg",Constant.SuccessMsg);
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg",Constant.FailedMsg);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
			
		HttpIO.writeResp(response, respJson);	
	}
	
	//清除定时开关机设置
	@RequestMapping(value="/clearBootSetting",method=RequestMethod.POST)
	@ResponseBody
	public void clearBootSetting(HttpServletRequest request,
				HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String setting_id = jsonBody.getString("setting_id");
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			Map<String,Object> conMap = new HashMap<String,Object> ();
			int hasSettPermission=0;
			if(adminUser.getIs_supper()==0) {
				hasSettPermission=checkActionPermission(adminUser.getUid(),setting_id);
				if(hasSettPermission==0) {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", Constant.ParemeterErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			
			if(terminal_ids==null||terminal_ids.size()==0) {
				terminal_ids = new JSONArray();
				Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + setting_id);
				for(String tmpTerminalId:tmpOKterminals) {
					terminal_ids.add(tmpTerminalId);
				}
			}
			
			if(terminal_ids != null) {
				conMap.put("setting_id", setting_id);
				List<Map<String, Object>>  theSetting = ms.selectList("BootSetting", "selectByPK", conMap);
				JSONObject socketBody = new JSONObject();
//				JSONArray sequeceList = new JSONArray();
				Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
				conMap.clear();
				conMap.put("terminal_ids", terminal_ids);
				List<Map<String, Object>>  terminalList = ms.selectList("Terminal", "selectByIds", conMap);
				if(terminalList!=null && terminalList.size()>0) {
					for(int n=0; n < terminalList.size(); n++) {
						 socketBody.clear();
						 socketBody.put("mode", 2);//2是删除
						 int sequece = newSocketMsgHandler.sendMsgTo((String) terminalList.get(n).get("ip"), cmds.SET_BOOTSHUT_TIME, socketBody.toJSONString());
						 sequeceMap.put(sequece, (String) terminalList.get(n).get("terminal_id"));
						result = 1;
					}
				}

				Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
				Map<String,Object> result2Web = new HashMap<String,Object>();
				//int failedCnt =0;	
				int successCnt =0;
				if (RespMap != null && RespMap.size() > 0) {
					for (Integer key : RespMap.keySet()) {//key 是sequece
						 String terminalRespStr = RespMap.get(key).getString("resp");
						 //String terminalIp = RespMap.get(key).getString("terminal_ip");
						 String terminal_id = RespMap.get(key).getString("terminal_id");
						 JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
					     result = (int) terminalRespJson.get("status");
					     if(result==1) {
					    	 successCnt++;
							RedisUtils.removeMember(Constant.TaskSentTerminalsOK + setting_id, terminal_id);
							RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + setting_id, terminal_id);
							RedisUtils.addMember(Constant.TaskTerminalReady + setting_id, terminal_id, 0);
					     }
					}
				}
				result2Web.put("failedCnt",sequeceMap.size());
				JSONArray resultList = new JSONArray();
				resultList.add(result2Web);
				//下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionSend);
				JSONObject content = new JSONObject();
				content.put("action_name", "取消自动开关机");
				content.put("setting_id",  setting_id);
				actionLog.put("action_content",content.toJSONString());
				es.insert("UserLog", "insert", actionLog);
				
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", resultList);
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.ParemeterErr);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}
	
	
	//设置定时开关机设置
	@RequestMapping(value="/sendToTerminal",method=RequestMethod.POST)
	@ResponseBody
	public void sendSetting2Terminal(HttpServletRequest request,
				HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String setting_id = jsonBody.getString("setting_id");
	//		String action = jsonBody.getString("action");
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			String terminal_type = jsonBody.getString("terminal_type");//ready 为设置的就绪的，ok为发送成功的,fail为发送失败的
			Map<String,Object> conMap = new HashMap<String,Object> ();
			if(adminUser.getIs_supper()==0) {
				int hasSettPermission=checkActionPermission(adminUser.getUid(),setting_id);
				if(hasSettPermission==0) {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", Constant.ParemeterErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			
			if(terminal_ids==null) {
				terminal_ids = new JSONArray();
			}
			
			if(terminal_type!=null && terminal_type.equals("ready")) {
				//terminal_ids = RedisUtils.allSetData(Constant.TaskTerminalReady + setting_id);
				Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskTerminalReady + setting_id);
				System.out.println("hhhh terminal_type1" + terminal_type);
				for(String tmpTerminalId:tmpOKterminals) {
					//terminal_ids.remove(tmpTerminalId);
					terminal_ids.add(tmpTerminalId);
				}
			}else if(terminal_type!=null && terminal_type.equals("fail")) {
				//terminal_ids = RedisUtils.allSetData(Constant.TaskSentTerminalsFailed + setting_id);
				//terminal_ids.clear();
			
				Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskSentTerminalsFailed + setting_id);
				System.out.println("hhhh terminal_type2" + terminal_type);
				for(String tmpTerminalId:tmpOKterminals) {
					//terminal_ids.remove(tmpTerminalId);
					terminal_ids.add(tmpTerminalId);
				}
			}
			System.out.println("hhhh terminal_ids" + terminal_ids.toJSONString());
			if(terminal_ids != null && terminal_ids.size() >0) {
				conMap.put("setting_id", setting_id);
				List<Map<String, Object>>  theSetting = ms.selectList("BootSetting", "selectByPK", conMap);
				JSONObject socketBody = new JSONObject();
				//JSONArray sequeceList = new JSONArray();
				System.out.println("hhhh theSetting" + theSetting.toString());
				Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
				conMap.clear();
				conMap.put("terminal_ids", terminal_ids);
				List<Map<String, Object>>  terminalList = ms.selectList("Terminal", "selectByIds", conMap);
				System.out.println("hhhh terminalList" + terminalList.toString());
				if(terminalList!=null && terminalList.size() >0) {
					for(int i=0;i<terminalList.size();i++) {
						 socketBody.clear();
						 socketBody.put("mode", 1); //1设置开关机时间，2取消自动开关机
						 if(theSetting != null && theSetting.size() ==1) { //只有设置才携带开关机时间
							 String bootTime = ((String)theSetting.get(0).get("boot_time"));
							 String shutDownTime = ((String)theSetting.get(0).get("shutdown_time"));
							 String weekDays = ((String)theSetting.get(0).get("week_days")).replace("[", "").replace("]", "");
							 socketBody.put("bootTime", bootTime);
							 socketBody.put("shutTime", shutDownTime);
							 socketBody.put("weekArray", weekDays);
							 int sequece = newSocketMsgHandler.sendMsgTo((String) terminalList.get(i).get("ip"), cmds.SET_BOOTSHUT_TIME, socketBody.toJSONString());
							 sequeceMap.put(sequece, (String) terminalList.get(i).get("terminal_id"));
							 result = 1;
						 }
					}
				}
				Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
				Map<String,Object> result2Web = new HashMap<String,Object>();
				//int failedCnt =0;	
				int successCnt =0;
				if (RespMap != null && RespMap.size() > 0) {
					for (Integer key : RespMap.keySet()) {//key 是sequece
						 String terminalRespStr = RespMap.get(key).getString("resp");
						 //String terminalIp = RespMap.get(key).getString("terminal_ip");
						 String terminal_id = RespMap.get(key).getString("terminal_id");
						 JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
					     result = (int) terminalRespJson.get("status");
					     //String memberVal = tmpInfo.getString("terminal_id") + ":" + tmpInfo.getString("terminal_ip");
					     if(result==1) {
					    	 successCnt++;
							RedisUtils.addMember(Constant.TaskSentTerminalsOK + setting_id, terminal_id, 0);
							RedisUtils.removeMember(Constant.TaskTerminalReady + setting_id,terminal_id);
							RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + setting_id,terminal_id);
					     }else {
					    	//failedCnt++;
							RedisUtils.addMember(Constant.TaskSentTerminalsFailed + setting_id, terminal_id, 0);
							RedisUtils.removeMember(Constant.TaskTerminalReady + setting_id,terminal_id);
							RedisUtils.removeMember(Constant.TaskSentTerminalsOK + setting_id,terminal_id);
					     }
					}
				}
				result2Web.put("failedCnt",sequeceMap.size());
				JSONArray resultList = new JSONArray();
				resultList.add(result2Web);
				//下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionSend);
				JSONObject content = new JSONObject();
				content.put("action_name", "设置自动开关机");
				content.put("setting_id",  setting_id);
				actionLog.put("action_content",content.toJSONString());
				es.insert("UserLog", "insert", actionLog);
				
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", resultList);
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.ParemeterErr);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}
	
	
	//删除终端开关机设置
	@RequestMapping(value="/del_setting",method=RequestMethod.POST)
	@ResponseBody
	public void delSetting(HttpServletRequest request,
				HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 1;
		User adminUser = SesCheck.getUserBySession(request, es,false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			JSONArray setting_ids = jsonBody.getJSONArray("setting_ids");
			Map<String,Object> condMap = new HashMap<String,Object>();
			if(adminUser.getIs_supper()==0) {
				for(int i=0;i<setting_ids.size();i++) {
					int hasSettPermission=checkActionPermission(adminUser.getUid(),setting_ids.getString(i));
					if(hasSettPermission==0) {
						respJson.put("status", Constant.FAILED);
						respJson.put("msg", Constant.ParemeterErr);
						HttpIO.writeResp(response, respJson);
						return;
					}
				}
			}
			
			if(setting_ids!=null && setting_ids.size() >0) {
				for(int i=0;i<setting_ids.size();i++) {
					Set<String> sendTerminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + setting_ids.getString(i));
					if(sendTerminals==null||sendTerminals.size()==0) {
						condMap.put("setting_id", setting_ids.get(i));
						result = ms.execute("BootSetting", "delete", condMap);
						if(result >0) {
							RedisUtils.del(Constant.TaskSentTerminalsOK + setting_ids.get(i));
							RedisUtils.del(Constant.TaskTerminalReady + setting_ids.get(i));
							RedisUtils.del(Constant.TaskSentTerminalsFailed + setting_ids.get(i));
						}
					}
				}
			}
			if (result >= 1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", "删除成功！");

				// 下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionDel);
				JSONObject content = new JSONObject();
				content.put("action_name", "删除定时开关机任务");
				content.put("setting_ids", setting_ids);
				actionLog.put("action_content", content.toJSONString());
				es.insert("UserLog", "insert", actionLog);

			} else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "删除失败！");
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		HttpIO.writeResp(response, respJson);
	}
	
}












