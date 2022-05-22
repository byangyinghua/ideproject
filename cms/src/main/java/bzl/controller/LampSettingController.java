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
import bzl.entity.LampSetting;
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
@RequestMapping("/lampsetting")
public class LampSettingController {

	// private static Logger logger = Logger.getLogger(UserController.class);
	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	private int checkActionPermission(String uid,String setting_id) {
		int result =0;
		Map<String,Object> conMap = new HashMap<String,Object>();
		conMap.put("setting_id", setting_id);
		Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(uid);
		if(tmpUserSet!=null && tmpUserSet.size() >0) {
			conMap.put("creator_uids", tmpUserSet.toArray());
			List<Map<String,Object>> bootsettingList = ms.selectList("LampSetting", "selectByCondition", conMap);
			if(bootsettingList!=null && bootsettingList.get(0).get("creator_uid").equals(uid)) {
			     result =1;
			}
		}
		return result;
	}

	private List<Map<String, Object>> convertJsonStr2Array(List<Map<String, Object>> LampSettingList) {
		for (int i = 0; i < LampSettingList.size(); i++) {
			JSONArray weekDays = JSONObject.parseArray((String) LampSettingList.get(i).get("week_days"));
			LampSettingList.get(i).put("week_days", weekDays);
			Set<String> ready_terminals = RedisUtils
					.allSetData(Constant.TaskTerminalReady + LampSettingList.get(i).get("setting_id"));
			LampSettingList.get(i).put("ready_terminals", ready_terminals.toArray());
			Set<String> ok_terminals = RedisUtils
					.allSetData(Constant.TaskSentTerminalsOK + LampSettingList.get(i).get("setting_id"));
			LampSettingList.get(i).put("ok_terminals", ok_terminals.toArray());
			Set<String> fail_terminals = RedisUtils
					.allSetData(Constant.TaskSentTerminalsFailed + LampSettingList.get(i).get("setting_id"));
			LampSettingList.get(i).put("fail_terminals", fail_terminals.toArray());
		}

		return LampSettingList;

	}

	// 获取终端定时开关机设置
	@RequestMapping(value = "/getsettings", method = RequestMethod.POST)
	@ResponseBody
	public void getTerminalBottSetting(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			Map<String, Object> conMap = new HashMap<String, Object>();
			
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
			

			if (getTotal != null) {
				List<Map<String, Object>> totalList = ms.selectList("LampSetting", "selectCountByCondition", conMap);
				if (totalList != null && totalList.size() == 1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}

			conMap.put("startrom", (page - 1) * pagesize);
			conMap.put("pagesize", pagesize);
			conMap.put("sort", "update_time desc");
			List<Map<String, Object>> LampSettingList = ms.selectList("LampSetting", "selectByConditionWithPage",
					conMap);
			LampSettingList = convertJsonStr2Array(LampSettingList);
			LampSettingList = Convert.SortDataListId(LampSettingList, page, pagesize);
			if (LampSettingList != null && LampSettingList.size() > 0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", LampSettingList);
			} else {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("msg", Constant.NodataErr);
			}
		} else {
			respJson.put("result", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}

		HttpIO.writeResp(response, respJson);
	}

	// 获取终端定时开关机设置
	@RequestMapping(value = "/getsettingById", method = RequestMethod.POST)
	@ResponseBody
	public void getTerminalLampSettingById(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String setting_id = jsonBody.getString("setting_id");
			Map<String, Object> conMap = new HashMap<String, Object>();
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
			
			conMap.put("setting_id", setting_id);
			List<Map<String, Object>> bootsettingList = ms.selectList("LampSetting", "selectByCondition", conMap);
			bootsettingList = convertJsonStr2Array(bootsettingList);
			if (bootsettingList != null && bootsettingList.size() > 0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", bootsettingList);
			} else {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("msg", Constant.NodataErr);
			}
		} else {
			respJson.put("result", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}

		HttpIO.writeResp(response, respJson);
	}

	// 获取终端定时开关机设置
	@RequestMapping(value = "/setReadyOrFailedTerminal", method = RequestMethod.POST)
	@ResponseBody
	public void setReadyOrFailedTerminal(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			BootSetting newBootSet = new BootSetting();
			String setting_id = jsonBody.getString("setting_id");
			JSONArray ready_terminals = jsonBody.getJSONArray("ready_terminals");// ip
			JSONArray failed_terminals = jsonBody.getJSONArray("fail_terminals");// ip
			
			int hasSettPermission = 0;
			if(adminUser.getIs_supper()==0) {
				hasSettPermission=checkActionPermission(adminUser.getUid(),setting_id);
			}else {
				hasSettPermission = 1;
			}
			
			if(hasSettPermission==1) {
				Set<String> ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + setting_id);
				for (String ok_terminal : ok_terminals) {
					if (ready_terminals != null) {
						ready_terminals.remove(ok_terminal);
					}
					if (failed_terminals != null) {
						failed_terminals.remove(ok_terminal);
					}
				}

				if (ready_terminals != null) {
					RedisUtils.del(Constant.TaskTerminalReady + setting_id);// 移除然后在新增
					for (int i = 0; i < ready_terminals.size(); i++) {
						RedisUtils.addMember(Constant.TaskTerminalReady + setting_id, ready_terminals.getString(i), 0);
						result = 1;
					}
				}

				if (failed_terminals != null) {
					// RedisUtils.del(Constant.TaskSentTerminalsFailed + setting_id);//移除然后在新增
					for (int i = 0; i < failed_terminals.size(); i++) {
						RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + setting_id,
								failed_terminals.getString(i));
						result = 1;
					}
				}
			}

			if (result == 1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
			} else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.FailedMsg);
			}
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}

		HttpIO.writeResp(response, respJson);
	}

	// 获取终端定时开关机设置
	@RequestMapping(value = "/addOrUpdate", method = RequestMethod.POST)
	@ResponseBody
	public void addOrUpdateLampSettings(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			LampSetting newLampSet = new LampSetting();
			String setting_id = jsonBody.getString("setting_id");
			String on_time = jsonBody.getString("on_time");
			String off_time = jsonBody.getString("off_time");
			JSONArray week_days = jsonBody.getJSONArray("week_days");
			// JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");

			newLampSet.setOn_time(on_time);
			newLampSet.setOff_time(off_time);
			newLampSet.setWeek_days(week_days.toJSONString());
			if (setting_id != null && setting_id.length() > 0) {
				Set<String> sendTerminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + setting_id);
				if (sendTerminals == null || sendTerminals.size() == 0) {
					newLampSet.setSetting_id(setting_id);
					if(adminUser.getIs_supper()==0) {
						int hasSettPermission=checkActionPermission(adminUser.getUid(),setting_id);
						if(hasSettPermission==1) {
							result = es.update("LampSetting", "update", newLampSet);
						}
					}else {
						result = es.update("LampSetting", "update", newLampSet);
					}
				}
			} else {
				newLampSet.setSetting_id("lid" + new Date().getTime());
				newLampSet.setCreator(adminUser.getUsername());
				newLampSet.setCreator_uid(adminUser.getUid());
				newLampSet.setUpdate_time(new Date());
				result = es.insert("LampSetting", "insert", newLampSet);
			}

			if (result == 1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
			} else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.FailedMsg);
			}
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}

		HttpIO.writeResp(response, respJson);
	}

	// 清除定时开关机设置
	@RequestMapping(value = "/clearLampSetting", method = RequestMethod.POST)
	@ResponseBody
	public void clearLampSetting(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String setting_id = jsonBody.getString("setting_id");
			// String terminal_type = jsonBody.getString("terminal_type");//ready
			// 为设置的就绪的，ok为发送成功的,fail为发送失败的
			
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
			
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			Map<String, Object> conMap = new HashMap<String, Object>();
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			
			if(terminal_ids==null||terminal_ids.size()==0) {
				terminal_ids = new JSONArray();
				Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + setting_id);
				for(String tmpTerminalId:tmpOKterminals) {
					terminal_ids.add(tmpTerminalId);
				}
			}
			
			if (terminal_ids != null) {
				conMap.put("setting_id", setting_id);
				List<Map<String, Object>> theSetting = ms.selectList("LampSetting", "selectByPK", conMap);
				JSONObject socketBody = new JSONObject();
				// JSONArray sequeceList = new JSONArray();
				Map<Integer, String> sequeceMap = new HashMap<Integer, String>();
				conMap.clear();
				conMap.put("terminal_ids", terminal_ids);
				List<Map<String, Object>> terminalList = ms.selectList("Terminal", "selectByIds", conMap);
				if (terminalList != null && terminalList.size() > 0) {
					for (int n = 0; n < terminalList.size(); n++) {
						socketBody.clear();
						socketBody.put("taskId", setting_id);// 2是删除
						socketBody.put("action", 2);// 1为屏蔽器，２灯控
						socketBody.put("mode", 2);// 2是删除
						int sequece = newSocketMsgHandler.sendMsgTo((String) terminalList.get(n).get("ip"),
								cmds.SCHEDULE_TASK, socketBody.toJSONString());
						sequeceMap.put(sequece, (String) terminalList.get(n).get("terminal_id"));
						result = 1;
					}
				}

				Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
				Map<String, Object> result2Web = new HashMap<String, Object>();
				// int failedCnt = 0;
				int successCnt = 0;
				if (RespMap != null && RespMap.size() > 0) {
					for (Integer key : RespMap.keySet()) {// key 是sequece
						String terminalRespStr = RespMap.get(key).getString("resp");
						String terminalIp = RespMap.get(key).getString("terminal_ip");
						String terminal_id = RespMap.get(key).getString("terminal_id");
						JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
						result = (int) terminalRespJson.get("status");
						if (result == 1) {
							successCnt++;
							RedisUtils.removeMember(Constant.TaskSentTerminalsOK + setting_id, terminal_id);
							RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + setting_id, terminal_id);
							RedisUtils.addMember(Constant.TaskTerminalReady + setting_id, terminal_id, 0);
						}
					}
				}
				result2Web.put("failedCnt", sequeceMap.size());
				JSONArray resultList = new JSONArray();
				resultList.add(result2Web);
				// 下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionSend);
				JSONObject content = new JSONObject();
				content.put("action_name", "取消自动开关灯");
				content.put("setting_id", setting_id);
				actionLog.put("action_content", content.toJSONString());
				es.insert("UserLog", "insert", actionLog);

				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", resultList);
			} else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.ParemeterErr);
			}
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}

	// 下发定时开关灯
	@RequestMapping(value = "/sendToTerminal", method = RequestMethod.POST)
	@ResponseBody
	public void sendSetting2Terminal(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String setting_id = jsonBody.getString("setting_id");
			// String action = jsonBody.getString("action");
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			String terminal_type = jsonBody.getString("terminal_type");// ready 为设置的就绪的，ok为发送成功的,fail为发送失败的
			
			if(adminUser.getIs_supper()==0) {
				int hasSettPermission=checkActionPermission(adminUser.getUid(),setting_id);
				if(hasSettPermission==0) {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", Constant.ParemeterErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			
			
			Map<String, Object> conMap = new HashMap<String, Object>();
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();

			if(terminal_ids==null) {
				terminal_ids = new JSONArray();
			}
			if(terminal_type!=null && terminal_type.equals("ready")) {
				//terminal_ids = RedisUtils.allSetData(Constant.TaskTerminalReady + setting_id);
				Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskTerminalReady + setting_id);
				for(String tmpTerminalId:tmpOKterminals) {
					terminal_ids.remove(tmpTerminalId);
					terminal_ids.add(tmpTerminalId);
				}
			}else if(terminal_type!=null && terminal_type.equals("fail")) {
				//terminal_ids = RedisUtils.allSetData(Constant.TaskSentTerminalsFailed + setting_id);
				Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskSentTerminalsFailed + setting_id);
				for(String tmpTerminalId:tmpOKterminals) {
					terminal_ids.remove(tmpTerminalId);
					terminal_ids.add(tmpTerminalId);
				}
			}

			if (terminal_ids != null && terminal_ids.size()>0) {
				conMap.put("setting_id", setting_id);
				List<Map<String, Object>> theSetting = ms.selectList("LampSetting", "selectByPK", conMap);
				JSONObject socketBody = new JSONObject();
				//JSONArray sequeceList = new JSONArray();
				Map<Integer, String> sequeceMap = new HashMap<Integer, String>();
				conMap.clear();
				conMap.put("terminal_ids", terminal_ids);
				List<Map<String, Object>> terminalList = ms.selectList("Terminal", "selectByIds", conMap);
				if (terminalList != null && terminalList.size() > 0) {
					for (int n = 0; n < terminalList.size(); n++) {
						socketBody.clear();
						socketBody.put("action", 2);// 1为屏蔽器，２灯控
						socketBody.put("mode", 1); // 1为启用
						if (theSetting != null && theSetting.size() == 1) { // 只有设置才携带开关机时间
//							String onTime = "20191111" + ((String) theSetting.get(0).get("on_time")) + "00";
//							String offTime = "25001111" + ((String) theSetting.get(0).get("off_time")) + "00";
							String weekDays = ((String) theSetting.get(0).get("week_days")).replace("[", "").replace("]", "");
							socketBody.put("startDate", "20191111");
							socketBody.put("endDate", "25001111");
							socketBody.put("startTime", ((String) theSetting.get(0).get("on_time")));
							socketBody.put("endTime", ((String) theSetting.get(0).get("off_time")));
							socketBody.put("weekArray", weekDays);
						}
						socketBody.put("taskId", setting_id);
						int sequece = newSocketMsgHandler.sendMsgTo((String) terminalList.get(n).get("ip"), cmds.SCHEDULE_TASK,socketBody.toJSONString());
						sequeceMap.put(sequece, (String) terminalList.get(n).get("terminal_id"));
						result = 1;
					}
				}

				Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
				Map<String, Object> result2Web = new HashMap<String, Object>();
				// int failedCnt = 0;
				int successCnt = 0;
				if (RespMap != null && RespMap.size() > 0) {
					for (Integer key : RespMap.keySet()) {// key 是sequece
						String terminalRespStr = RespMap.get(key).getString("resp");
						String terminalIp = RespMap.get(key).getString("terminal_ip");
						String terminal_id = RespMap.get(key).getString("terminal_id");
						JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
						result = (int) terminalRespJson.get("status");
						if (result == 1) {
							successCnt++;
							RedisUtils.addMember(Constant.TaskSentTerminalsOK + setting_id, terminal_id, 0);
							RedisUtils.removeMember(Constant.TaskTerminalReady + setting_id, terminal_id);
							RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + setting_id, terminal_id);
						} else {
							// failedCnt++;
							RedisUtils.addMember(Constant.TaskSentTerminalsFailed + setting_id, terminal_id, 0);
							RedisUtils.removeMember(Constant.TaskTerminalReady + setting_id, terminal_id);
							RedisUtils.removeMember(Constant.TaskSentTerminalsOK + setting_id, terminal_id);
						}
					}
				}
				result2Web.put("failedCnt", sequeceMap.size());
				JSONArray resultList = new JSONArray();
				resultList.add(result2Web);
				// 下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionSend);
				JSONObject content = new JSONObject();
				content.put("action_name", "设置自动开关灯");
				content.put("setting_id", setting_id);
				actionLog.put("action_content", content.toJSONString());
				es.insert("UserLog", "insert", actionLog);

				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", resultList);
			} else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.ParemeterErr);
			}
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}
	
	//立即开关灯
	@RequestMapping(value = "/setLampOnOrOff", method = RequestMethod.POST)
	@ResponseBody
	public void setLampOnOrOff(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			JSONArray setting_ids = jsonBody.getJSONArray("setting_ids");
			String  action  = jsonBody.getString("action");//on 为开灯，off为关灯   
			JSONArray  terminal_ids  = jsonBody.getJSONArray("terminal_ids");//on 为开灯，off为关灯   
			
			if(adminUser.getIs_supper()==0 ) {
				if(setting_ids!=null) {
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
			}
			
			Map<String, Object> conMap = new HashMap<String, Object>();
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			if(terminal_ids==null) {
				terminal_ids = new JSONArray();
			}
			
			if(setting_ids!=null) {
				for(int i=0;i<setting_ids.size();i++) {
					Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + setting_ids.getString(i));
					for(String tmpTerminalId:tmpOKterminals) {
						terminal_ids.add(tmpTerminalId);
					}
				}
			}
			int intAction = 2;
			if(action.equals("on")) {
				intAction = 1;
			}
			
			JSONObject socketBody = new JSONObject();
			//JSONArray sequeceList = new JSONArray();
			Map<Integer, String> sequeceMap = new HashMap<Integer, String>();
			conMap.clear();
			List<Map<String, Object>> terminalList =null;
			if(terminal_ids.size() >0) {
				conMap.put("terminal_ids", terminal_ids);
				terminalList = ms.selectList("Terminal", "selectByIds", conMap);
			}else if(setting_ids==null) {
				if(adminUser.getIs_supper()==1) {
					terminalList = ms.selectList("Terminal", "selectAll", conMap);
				}else {
					Set<String> gids = UserGroupController.getGroupListByUser(adminUser.getUid());
					for(String gid:gids) {
						conMap.put("gid", gid);
						List<Map<String, Object>>  tmpList = ms.selectList("Terminal", "selectByGroupId", conMap);
						if(terminalList==null) {
							terminalList = tmpList;
						}else if(tmpList!=null) {
							terminalList.addAll(tmpList);
						}
					}
				}
			}
			
			if (terminalList != null && terminalList.size() > 0) {
				for (int n = 0; n < terminalList.size(); n++) {
					String lampStatus = (String) terminalList.get(n).get("lamp_status");
					if(lampStatus!=null && lampStatus.length() >0) {
						socketBody.clear();
						socketBody.put("cmd", intAction);
						String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) terminalList.get(n).get("terminal_id"));
						if(null != onlineInfo) {
							String state = onlineInfo.split(":")[1];
							if(Integer.parseInt(state) >0) {
								int sequece = newSocketMsgHandler.sendMsgTo((String) terminalList.get(n).get("ip"), cmds.LAMP_ON_OFF,socketBody.toJSONString());
								sequeceMap.put(sequece, (String) terminalList.get(n).get("terminal_id"));
								result = 1;
							}
						}
					}
				}
			}
			
			Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
			Map<String, Object> result2Web = new HashMap<String, Object>();
			// int failedCnt = 0;
			int successCnt = 0;
			if (RespMap != null && RespMap.size() > 0) {
				for (Integer key : RespMap.keySet()) {// key 是sequece
					String terminalRespStr = RespMap.get(key).getString("resp");
//					String terminalIp = RespMap.get(key).getString("terminal_ip");
//					String terminal_id = RespMap.get(key).getString("terminal_id");
					JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
					result = (int) terminalRespJson.get("status");
					if (result == 1) {
						successCnt++;
					}
				}
			}
			
			result2Web.put("failedCnt", sequeceMap.size());
			result2Web.put("successCnt", successCnt);
			JSONArray resultList = new JSONArray();
			resultList.add(result2Web);
			// 下面添加用户操作日志
			JSONObject actionLog = new JSONObject();
			actionLog.put("uid", adminUser.getUid());
			actionLog.put("username", adminUser.getUsername());
			actionLog.put("realname", adminUser.getReal_name());
			actionLog.put("action_type", Constant.ActionSend);
			JSONObject content = new JSONObject();
			if(intAction==1) {
				content.put("action_name", "立即开灯");
			}else {
				content.put("action_name", "立即关灯");
			}
			content.put("successCnt", successCnt);
			content.put("failedCnt",  sequeceMap.size());
			actionLog.put("action_content", content.toJSONString());
			es.insert("UserLog", "insert", actionLog);

			respJson.put("status", Constant.SUCCESS);
			respJson.put("result", resultList);
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}

	
	// 删除开关灯设置任务
	@RequestMapping(value = "/del_setting", method = RequestMethod.POST)
	@ResponseBody
	public void delSetting(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			JSONArray setting_ids = jsonBody.getJSONArray("setting_ids");
			Map<String, Object> condMap = new HashMap<String, Object>();
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

			if (setting_ids != null && setting_ids.size() > 0) {
				for (int i = 0; i < setting_ids.size(); i++) {
					Set<String> tmpTerminalIPs = RedisUtils
							.allSetData(Constant.TaskSentTerminalsOK + setting_ids.get(i));
					if (tmpTerminalIPs == null || tmpTerminalIPs.size() == 0) {
						condMap.put("setting_id", setting_ids.get(i));
						result = ms.execute("LampSetting", "delete", condMap);
						if (result > 0) {
							RedisUtils.del(Constant.TaskSentTerminalsOK + setting_ids.get(i));
							RedisUtils.del(Constant.TaskTerminalReady + setting_ids.get(i));
							RedisUtils.del(Constant.TaskSentTerminalsFailed + setting_ids.get(i));
						}
					}
				}
			}
			if (result > 0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
			} else {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.FailedMsg);
			}
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}

		HttpIO.writeResp(response, respJson);
	}

}
