package bzl.controller;

import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import net.sf.json.JSONArray;
//import net.sf.json.JSONObject;
import sun.rmi.log.LogHandler;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import bzl.common.Constant;
import bzl.common.SesCheck;
import bzl.entity.BootSetting;
import bzl.entity.ShieldTask;
import bzl.entity.Terminal;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import bzl.service.impl.JDBCTransaction;
import bzl.task.SocketMsgHandler;
import bzl.task.cmds;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import utils.Convert;
import utils.HttpIO;
import utils.RedisUtils;
import utils.TimeUtil;

import org.xnx.sql.util.SQLTools;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@Controller
@RequestMapping("/shield")
public class ShieldController {

	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	private int checkActionPermission(String uid,String shield_id) {
		int result =0;
		Map<String,Object> conMap = new HashMap<String,Object>();
		conMap.put("shield_id", shield_id);
		Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(uid);
		if(tmpUserSet!=null && tmpUserSet.size() >0) {
			conMap.put("creator_uids", tmpUserSet.toArray());
			List<Map<String,Object>> taskList = ms.selectList("ShieldTask", "selectByCondition", conMap);
			if(taskList!=null && taskList.get(0).get("creator_uid").equals(uid)) {
			     result =1;
			}
		}
		return result;
	}

	private JSONObject transferTaskInfo2web(Map<String, Object> oldTaskInfo) {
		JSONObject retTaskInfo = new JSONObject();

		Set<String> ready_terminals = RedisUtils.allSetData(Constant.TaskTerminalReady + oldTaskInfo.get("shield_id"));
		retTaskInfo.put("ready_terminals", ready_terminals.toArray());
		Set<String> ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + oldTaskInfo.get("shield_id"));
		retTaskInfo.put("ok_terminals", ok_terminals.toArray());
		Set<String> fail_terminals = RedisUtils
				.allSetData(Constant.TaskSentTerminalsFailed + oldTaskInfo.get("shield_id"));
		retTaskInfo.put("fail_terminals", fail_terminals.toArray());

		retTaskInfo.put("id", oldTaskInfo.get("id"));
		retTaskInfo.put("shield_id", oldTaskInfo.get("shield_id"));
		retTaskInfo.put("task_name", oldTaskInfo.get("task_name"));
		retTaskInfo.put("start_time", oldTaskInfo.get("start_time"));
		retTaskInfo.put("end_time", oldTaskInfo.get("end_time"));
		retTaskInfo.put("creator", oldTaskInfo.get("creator"));
		retTaskInfo.put("update_time", oldTaskInfo.get("update_time"));

		String weekDays = (String) oldTaskInfo.get("week_days");
		if (weekDays != null && weekDays.length() > 0) {
			retTaskInfo.put("week_days", JSONObject.parse(weekDays));
		}

		return retTaskInfo;
	}

	/**
	 * 获取屏蔽器任务列表
	 * 
	 * @param
	 * @param
	 */
	@RequestMapping(value = "/task_list", method = RequestMethod.POST)
	public void getTaskList(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 1;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			Map conMap = new HashMap<String, Object>();
			if(adminUser.getIs_supper()==0) {
				Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(adminUser.getUid());
				if(tmpUserSet!=null && tmpUserSet.size() >0) {
					conMap.put("creator_uids", tmpUserSet.toArray());
				}else {
					conMap.put("creator_uid", adminUser.getUid());
				}
			}
			
			if (getTotal != null) {
				List<Map<String, Object>> totalList = ms.selectList("ShieldTask", "selectCountByCondition", conMap);
				if (totalList != null && totalList.size() == 1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}

			conMap.put("startrom", (page - 1) * pagesize);
			conMap.put("pagesize", pagesize);
			JSONArray retTaskList = new JSONArray();
			List<Map<String, Object>> tasklist = ms.selectList("ShieldTask", "selectByConditionWithPage", conMap);
			if (tasklist != null && tasklist.size() > 0) {
				tasklist = Convert.SortDataListId(tasklist, page, pagesize);
				for (int i = 0; i < tasklist.size(); i++) {
					retTaskList.add(transferTaskInfo2web(tasklist.get(i)));
				}
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", retTaskList);
			} else {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("msg", Constant.NodataErr);
			}
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}

		HttpIO.writeResp(response, respJson);
	}

	// 获取终端定时开关机设置
	@RequestMapping(value = "/getTaskByShieldId", method = RequestMethod.POST)
	@ResponseBody
	public void getTaskByShieldId(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String shield_id = jsonBody.getString("shield_id");
			Map<String, Object> conMap = new HashMap<String, Object>();
			JSONArray retTaskList = new JSONArray();
			conMap.put("shield_id", shield_id);
			if(adminUser.getIs_supper()==0) {
				int isHasPermission = checkActionPermission(adminUser.getUid(),shield_id);
				if(isHasPermission==0) {
					respJson.put("status", Constant.UserNotLogin);
					respJson.put("msg", Constant.PermissionErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			
			List<Map<String, Object>> tasklist = ms.selectList("ShieldTask", "selectByPK", conMap);
			// tasklist = Convert.SortDataListId(tasklist, page, pagesize);
			for (int i = 0; i < tasklist.size(); i++) {
				retTaskList.add(transferTaskInfo2web(tasklist.get(i)));
			}
			if (retTaskList != null && retTaskList.size() > 0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", retTaskList);
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

	// 设置屏蔽任务的终端
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
			String shield_id = jsonBody.getString("shield_id");
			JSONArray ready_terminals = jsonBody.getJSONArray("ready_terminals");// ip
			JSONArray failed_terminals = jsonBody.getJSONArray("fail_terminals");// ip
			if(adminUser.getIs_supper()==0) {
				int isHasPermission = checkActionPermission(adminUser.getUid(),shield_id);
				if(isHasPermission==0) {
					respJson.put("status", Constant.UserNotLogin);
					respJson.put("msg", Constant.PermissionErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			
			Set<String> ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + shield_id);
			for(String ok_terminal:ok_terminals) {
				if(ready_terminals!=null) {
					ready_terminals.remove(ok_terminal);
				}
				if(failed_terminals!=null) {
					failed_terminals.remove(ok_terminal);
				}
			}
			if (ready_terminals != null) {
				RedisUtils.del(Constant.TaskTerminalReady + shield_id);// 移除然后在新增
				for (int i = 0; i < ready_terminals.size(); i++) {
					RedisUtils.addMember(Constant.TaskTerminalReady + shield_id, ready_terminals.getString(i), 0);
					result = 1;
				}
			}

			if (failed_terminals != null) {
				// RedisUtils.del(Constant.TaskSentTerminalsFailed + setting_id);//移除然后在新增
				for (int i = 0; i < failed_terminals.size(); i++) {
					RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + shield_id,
							failed_terminals.getString(i));
					result = 1;
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

	/**
	 * 增加屏蔽器任务
	 * 
	 * @param
	 * @param
	 */
	@RequestMapping(value = "/addOrUpdate", method = RequestMethod.POST)
	public void addOrUpdate(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String shieldId = jsonBody.getString("shield_id");
			JSONArray weekDays = jsonBody.getJSONArray("week_days");
			String task_name = jsonBody.getString("task_name");
			
			if(adminUser.getIs_supper()==0 && shieldId!=null) {
				int isHasPermission = checkActionPermission(adminUser.getUid(),shieldId);
				if(isHasPermission==0) {
					respJson.put("status", Constant.UserNotLogin);
					respJson.put("msg", Constant.PermissionErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			

			ShieldTask shieldTask = new ShieldTask();
			shieldTask.setStart_time(jsonBody.getString("start_time"));
			shieldTask.setEnd_time(jsonBody.getString("end_time"));
			shieldTask.setTask_name(task_name);
			if (weekDays != null && weekDays.size() > 0) {
				shieldTask.setWeek_days(weekDays.toString());
			}

			if (shieldId != null && shieldId.length() > 0) {
				shieldTask.setShield_id(shieldId);
				System.out.println("shieldTask:" + shieldTask.toString());
				Set<String> sendTerminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + shieldId);
				if (sendTerminals == null || sendTerminals.size() == 0) {
//					if(jsonBody.getString("creator").equals(adminUser.getUsername())) {
//						shieldTask.setCreator_uid(adminUser.getUid());
//					}
					result = es.update("ShieldTask", "update", shieldTask);
				}
			} else {
				SimpleDateFormat myFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				myFmt.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
				shieldTask.setShield_id("shield" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(7));
				shieldTask.setStart_time(myFmt.format(new Date()));
				shieldTask.setEnd_time(myFmt.format(new Date()));
				shieldTask.setCreator_uid(adminUser.getUid());
				shieldTask.setCreator(adminUser.getUsername());
				result = es.insert("ShieldTask", "insert", shieldTask);
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

	/**
	 * 删除屏蔽任务消息
	 * 
	 * @param
	 * @param
	 */
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	public void deleteShieldTask(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			JSONArray shieldIds = jsonBody.getJSONArray("shield_ids");
			Map<String, Object> condMap = new HashMap<String, Object>();
			if(adminUser.getIs_supper()==0) {
				for (int i = 0; i < shieldIds.size(); i++) {
					int isHasPermission = checkActionPermission(adminUser.getUid(),shieldIds.getString(i));
					if(isHasPermission==0) {
						respJson.put("status", Constant.UserNotLogin);
						respJson.put("msg", Constant.PermissionErr);
						HttpIO.writeResp(response, respJson);
						return;
					}
				}
			}
			
			if (shieldIds != null && shieldIds.size() > 0) {
				for (int i = 0; i < shieldIds.size(); i++) {
					Set<String> sendTerminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + shieldIds.getString(i));
					if (sendTerminals == null || sendTerminals.size() == 0) {
						condMap.put("shield_id", shieldIds.get(i));
						result = ms.execute("ShieldTask", "delete", condMap);
						if (result > 0) {
							RedisUtils.del(Constant.TaskSentTerminalsOK + shieldIds.get(i));
							RedisUtils.del(Constant.TaskTerminalReady + shieldIds.get(i));
							RedisUtils.del(Constant.TaskSentTerminalsFailed + shieldIds.get(i));
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
				content.put("action_name", "删除屏蔽任务");
				content.put("shieldIds", shieldIds);
				actionLog.put("action_content", content.toJSONString());
				es.insert("UserLog", "insert", actionLog);

			} else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "删除失败！");
			}
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}

	/**
	 * 往设备发送设置屏蔽或者取消屏蔽任务
	 * 
	 * @param
	 * @param
	 */
	@RequestMapping(value = "/send2terminal", method = RequestMethod.POST)
	public void sendShieldTask2Terminal(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			// String action = jsonBody.getString("action");
			String shield_id = jsonBody.getString("shield_id");
			String terminal_type = jsonBody.getString("terminal_type");
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			
			if(adminUser.getIs_supper()==0) {
				int isHasPermission = checkActionPermission(adminUser.getUid(),shield_id);
				if(isHasPermission==0) {
					respJson.put("status", Constant.UserNotLogin);
					respJson.put("msg", Constant.PermissionErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			

			Map<String, Object> condMap = new HashMap<String, Object>();

			if(terminal_ids==null) {
				terminal_ids = new JSONArray();
			}
			if(terminal_type!=null && terminal_type.equals("ready")) {
				//terminal_ids = RedisUtils.allSetData(Constant.TaskTerminalReady + setting_id);
				Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskTerminalReady + shield_id);
				for(String tmpTerminalId:tmpOKterminals) {
					terminal_ids.remove(tmpTerminalId);
					terminal_ids.add(tmpTerminalId);
				}
			}else if(terminal_type!=null && terminal_type.equals("fail")) {
				//terminal_ids = RedisUtils.allSetData(Constant.TaskSentTerminalsFailed + setting_id);
				Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskSentTerminalsFailed + shield_id);
				for(String tmpTerminalId:tmpOKterminals) {
					terminal_ids.remove(tmpTerminalId);
					terminal_ids.add(tmpTerminalId);
				}
			}

			condMap.put("shield_id", shield_id);
			List<Map<String, Object>> shieldTaskList = ms.selectList("ShieldTask", "selectByPK", condMap);
			if (shieldTaskList != null && shieldTaskList.size() == 1) {
				SocketMsgHandler newSocketHandler = new SocketMsgHandler();
				Map<String, Object> shieldTaskInfo = shieldTaskList.get(0);
				if (terminal_ids != null && terminal_ids.size() > 0) {
					JSONObject socketBody = new JSONObject();
					//JSONArray sequeceList = new JSONArray();
					Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
					condMap.clear();
					condMap.put("terminal_ids", terminal_ids);
					List<Map<String, Object>>  terminalList = ms.selectList("Terminal", "selectByIds", condMap);
					if(terminalList!=null && terminalList.size()>0) {
						for (int i=0;i<terminalList.size();i++) {
							socketBody.clear();
							socketBody.put("taskId", shield_id);
							socketBody.put("action", 1);// １是屏蔽器,2是灯控
							socketBody.put("mode", 1);// １为启用
							String startTime = (String) shieldTaskInfo.get("start_time");
							String endTime = (String) shieldTaskInfo.get("end_time");
							String weekDays = (String) shieldTaskInfo.get("week_days");
							socketBody.put("startDate", startTime.split(" ")[0]);
							socketBody.put("endDate", endTime.split(" ")[0]);
							socketBody.put("startTime", startTime.split(" ")[1]);
							socketBody.put("endTime", endTime.split(" ")[1]);
	
							socketBody.put("weekArray", weekDays.replace("[", "").replace("]", ""));
							int sequece = newSocketHandler.sendMsgTo((String) terminalList.get(i).get("ip"), cmds.SCHEDULE_TASK,socketBody.toJSONString());
							 sequeceMap.put(sequece, (String) terminalList.get(i).get("terminal_id"));
						}
					}
					
					Map<Integer, JSONObject> RespMap = newSocketHandler.getTerminalRespBySequece(sequeceMap, 2000);
					Map<String, Object> result2Web = new HashMap<String, Object>();
					// int failedCnt = 0;
					int successCnt = 0;
					if (RespMap != null && RespMap.size() > 0) {
						for (Integer key : RespMap.keySet()) {// key 是sequece
							String terminalRespStr = RespMap.get(key).getString("resp");
							//String terminal_ip = RespMap.get(key).getString("terminal_ip");
							String terminal_id = RespMap.get(key).getString("terminal_id");
							JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
							result = (int) terminalRespJson.get("status");
							if (result == 1) {
								successCnt++;
								RedisUtils.addMember(Constant.TaskSentTerminalsOK + shield_id, terminal_id, 0);
								RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + shield_id, terminal_id);
								RedisUtils.removeMember(Constant.TaskTerminalReady + shield_id, terminal_id);
							} else {
								RedisUtils.addMember(Constant.TaskSentTerminalsFailed + shield_id,terminal_id, 0);
								RedisUtils.removeMember(Constant.TaskSentTerminalsOK + shield_id,terminal_id);
								RedisUtils.removeMember(Constant.TaskTerminalReady + shield_id,terminal_id);
							}
						}
					}
					result2Web.put("failedCnt", sequeceMap.size());
					JSONArray resultList = new JSONArray();
					resultList.add(result2Web);
					respJson.put("status", Constant.SUCCESS);
					respJson.put("result", resultList);
					// 下面添加用户操作日志
					JSONObject actionLog = new JSONObject();
					actionLog.put("uid", adminUser.getUid());
					actionLog.put("username", adminUser.getUsername());
					actionLog.put("realname", adminUser.getReal_name());
					actionLog.put("action_type", Constant.ActionSend);
					JSONObject content = new JSONObject();
					content.put("action_name", "下发屏蔽任务");
					content.put("shield_id", shield_id);
					actionLog.put("action_content", content.toJSONString());
					es.insert("UserLog", "insert", actionLog);
				}
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

	// 停用屏蔽任务
	@RequestMapping(value = "/clearShieldSetting", method = RequestMethod.POST)
	@ResponseBody
	public void clearShieldSetting(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String shield_id = jsonBody.getString("shield_id");
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			Map<String, Object> conMap = new HashMap<String, Object>();
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			
			if(adminUser.getIs_supper()==0) {
				int isHasPermission = checkActionPermission(adminUser.getUid(),shield_id);
				if(isHasPermission==0) {
					respJson.put("status", Constant.UserNotLogin);
					respJson.put("msg", Constant.PermissionErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}

			if (terminal_ids == null || terminal_ids.size() == 0) {
				//terminalIds = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + shield_id);
				terminal_ids = new JSONArray();
				Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + shield_id);
				for(String tmpTerminalId:tmpOKterminals) {
					terminal_ids.add(tmpTerminalId);
				}
			} 
			
			conMap.put("shield_id", shield_id);
			List<Map<String, Object>> theSetting = ms.selectList("ShieldTask", "selectByPK", conMap);
			if (theSetting != null && shield_id != null) {
				JSONObject socketBody = new JSONObject();
				//JSONArray sequeceList = new JSONArray();
				Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
				conMap.clear();
				conMap.put("terminal_ids", terminal_ids);
				List<Map<String, Object>>  terminalList = ms.selectList("Terminal", "selectByIds", conMap);
				if(terminalList!=null && terminalList.size() >0) {
					for (int i=0;i<terminalList.size();i++) {
						socketBody.clear();
						socketBody.put("taskId", shield_id);
						socketBody.put("action", 1);// 1为屏蔽器，２灯控
						socketBody.put("mode", 2);// 2是删除
						int sequece = newSocketMsgHandler.sendMsgTo((String) terminalList.get(i).get("ip"), cmds.SCHEDULE_TASK,socketBody.toJSONString());
						sequeceMap.put(sequece, (String) terminalList.get(i).get("terminal_id"));
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
						//String terminalIp = RespMap.get(key).getString("terminal_ip");
						String terminal_id = RespMap.get(key).getString("terminal_id");
						JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
						result = (int) terminalRespJson.get("status");
						if (result == 1) {
							successCnt++;
							RedisUtils.removeMember(Constant.TaskSentTerminalsOK + shield_id, terminal_id);
							RedisUtils.addMember(Constant.TaskTerminalReady + shield_id, terminal_id, 0);
							RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + shield_id, terminal_id);
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
				content.put("action_name", "取消屏蔽任务");
				content.put("shield_id", shield_id);
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

   //send shield task by exam
	@RequestMapping(value = "/startOrStopShieldForExam", method = RequestMethod.POST)
	@ResponseBody
	public void startOrStopShieldForExam(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String exam_id = jsonBody.getString("exam_id");
			Integer action = jsonBody.getInteger("action");//1 or 0
			Map<String, Object> conMap = new HashMap<String, Object>();
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			conMap.put("exam_id", exam_id);
			List<Map<String,Object>> examInfolist = ms.selectList("ExamInfo", "selectByPK", conMap);
			if(examInfolist != null && examInfolist.size() > 0) {
				JSONObject socketBody = new JSONObject();
				String shieldId = RedisUtils.get("exam_shield-" + exam_id);
				if(shieldId==null || shieldId.length() ==0) {
					shieldId = "shield" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(7);
					RedisUtils.set("exam_shield-" + exam_id, shieldId);
				}
				socketBody.put("action", 1);// １是屏蔽器,2是灯控
				socketBody.put("mode", action);// １为启用,0 is stop
				String startDate = (String)examInfolist.get(0).get("start_date");
				String endDate = (String)examInfolist.get(0).get("end_date");
				String AMstartTime = (String)examInfolist.get(0).get("am_start_time");
				String AMendTime = (String)examInfolist.get(0).get("am_end_time");
				String PMstartTime = (String)examInfolist.get(0).get("pm_start_time");
				String PMendTime = (String)examInfolist.get(0).get("pm_end_time");
				
			    String weeDay = "1";
				try {
					   weeDay = TimeUtil.dayForWeek(startDate);
					    List<Map<String, Object>> taskList = ms.selectList("ExamTask", "selectByPK", conMap);
						if (taskList != null) {
							Set<String> terminal_ids = new HashSet<String>();
							for(int i=0;i<taskList.size();i++) {
								Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + taskList.get(i).get("task_id"));
								for(String tmpTerminalId:tmpOKterminals) {
									terminal_ids.add(tmpTerminalId);
								}
							}
							
							List<Map<String, Object>> allTerminals = null;
							if (terminal_ids != null && terminal_ids.size() > 0) {
								conMap.clear();
								conMap.put("terminal_ids", terminal_ids.toArray());
								allTerminals = ms.selectList("Terminal", "selectByIds", conMap);
								if(allTerminals!=null) {
									socketBody.put("startDate", startDate);
									socketBody.put("endDate", endDate);
									socketBody.put("weekArray", "1,2,3,4,5,6,7");
									if(AMstartTime!=null && AMendTime!=null && AMstartTime.length() >0 && AMendTime.length() >0) {
										socketBody.put("startTime", AMstartTime);
										socketBody.put("endTime", AMendTime);
										socketBody.put("taskId", shieldId + "am");
										for(int n=0;n<allTerminals.size();n++) {
											newSocketMsgHandler.sendMsgTo((String) allTerminals.get(n).get("ip"), cmds.SCHEDULE_TASK,socketBody.toJSONString());
										}
									}
									if(PMstartTime!=null && PMendTime!=null && PMstartTime.length() >0 && PMendTime.length() >0) {
										socketBody.put("startTime", PMstartTime);
										socketBody.put("endTime", PMendTime);
										socketBody.put("taskId", shieldId + "pm");
										for(int n=0;n<allTerminals.size();n++) {
											newSocketMsgHandler.sendMsgTo((String) allTerminals.get(n).get("ip"), cmds.SCHEDULE_TASK,socketBody.toJSONString());
										}
									}
								}
							}
							
							if(action==0) {
								RedisUtils.del("exam_shield-" + exam_id);
							}
							respJson.put("status", Constant.SUCCESS);
						}
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					respJson.put("status", Constant.FAILED);
					e.printStackTrace();
				}
			}else {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("msg", Constant.NodataErr);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}
}

















