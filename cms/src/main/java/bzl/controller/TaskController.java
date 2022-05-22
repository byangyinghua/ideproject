package bzl.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
//import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
//import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
//import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import net.sf.json.JSONObject;
import sun.rmi.log.LogHandler;
import utils.Convert;
import utils.HttpIO;
import utils.RedisUtils;
//import utils.NetUtil;
//import utils.StringUtil;
//import utils.TimeUtil;
import utils.ZipUtil;
import utils.FileUtil;

import org.apache.commons.lang.RandomStringUtils;
//import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
//import bzl.service.impl.JDBCTransaction;
//import utils.StringUtil;
//import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RequestParam;
//import utils.SessionFactory;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
//import com.netsdk.common.SavePath;

import bzl.common.Constant;
import bzl.common.SesCheck;
import bzl.entity.ExamInfo;
//import bzl.entity.BootSetting;
import bzl.entity.ExamTask;
import bzl.entity.TaskInfo;
//import bzl.entity.Terminal;
import bzl.entity.TerminalLog;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import bzl.task.SocketMsgHandler;
import bzl.task.cmds;

/*任务管理操作 controller */

@Controller
@RequestMapping("/task")
public class TaskController {
	static Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	private int checkActionPermission(String uid,String task_id) {
		int result =0;
		Map<String,Object> conMap = new HashMap<String,Object>();
		conMap.put("task_id", task_id);
		Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(uid);
		if(tmpUserSet!=null && tmpUserSet.size() >0) {
			conMap.put("creator_uids", tmpUserSet.toArray());
			List<Map<String,Object>> taskList = ms.selectList("TaskInfo", "selectByCondition", conMap);
			if(taskList!=null && taskList.get(0).get("creator_uid").equals(uid)) {
			     result =1;
			}
		}
		return result;
	}
	
	private JSONArray sendCountDownCmd(List<Map<String, Object>> allTerminals,String examId,String action,String fromDate,String toDate,String startTime,int value) {
		JSONObject socketBody = new JSONObject();
		SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
		if(action.equals("set")) {
			socketBody.put("mode", 1);//set为设置，unset为取消设置
		}else {
			socketBody.put("mode", 2);//set为设置，unset为取消设置
		}
		socketBody.put("taskId", examId);
		socketBody.put("action", 3);// １是屏蔽器,2是灯控,3是倒计时
		socketBody.put("startDate", fromDate);
		socketBody.put("endDate", toDate);
		socketBody.put("startTime", startTime);
		socketBody.put("content",""+ value);
		System.out.println("examId socketBody!!!====="+socketBody.toJSONString());
		Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
		for(int i=0;i<allTerminals.size();i++) {
			int sequece = newSocketMsgHandler.sendMsgTo((String) allTerminals.get(i).get("ip"), cmds.SCHEDULE_TASK, socketBody.toJSONString()); // 发送视频任务到终端设备
			sequeceMap.put(sequece, (String) allTerminals.get(i).get("terminal_id"));
		}
		
		JSONArray resultList = new JSONArray();
		Map<String, Object> result2Web = new HashMap<String, Object>();
		int successCnt = 0;
		Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
		if (RespMap != null && RespMap.size() > 0) {
			for (Integer key : RespMap.keySet()) {// key 是sequece
				String terminalRespStr = RespMap.get(key).getString("resp");
				//String terminal_ip = RespMap.get(key).getString("terminal_ip");
				String terminal_id = RespMap.get(key).getString("terminal_id");
				JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
				int result = (int) terminalRespJson.get("status");
				if (result == 1) {
					successCnt++;
				}
			}
		}
		result2Web.put("failedCnt", sequeceMap.size());
		resultList.add(result2Web);
	    return resultList;
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
			List<Map<String, Object>> tasklist = ms.selectList(taskType, "selectByPK", conMap);
			if(tasklist!=null && tasklist.size()==1) {
				tasklist.get(0).put("table_name", taskType);
				tasklist.get(0).put(taskType, taskTypeObj.get(taskType));
				return tasklist.get(0);
			}
		}
		
		return null;
	}
	

	private JSONObject buildTerminalTaskData(TaskInfo theTaskInfo) {

		JSONObject taskDataJson = new JSONObject();

		taskDataJson.put("taskId", theTaskInfo.getTask_id());
		taskDataJson.put("taskName", theTaskInfo.getTask_name());
		taskDataJson.put("taskType", theTaskInfo.getTask_type());
		taskDataJson.put("taskUser", theTaskInfo.getCreate_user());
		taskDataJson.put("createTime", theTaskInfo.getCreate_time());
		JSONArray content = JSONObject.parseArray(theTaskInfo.getContent());
		taskDataJson.put("title", content.get(0)); // 第一个为标题，
		taskDataJson.put("text", content.get(1));// 第二个为内容
		taskDataJson.put("level", theTaskInfo.getPriority()); // 优先级

		JSONObject playDate = new JSONObject();
		playDate.put("mode", theTaskInfo.getPlay_mode());
		if(theTaskInfo.getPlan_type().equals("temporary")) {//临时任务使用当前日期
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");  
			playDate.put("from", formatter.format(new Date()));
			playDate.put("to", formatter.format(new Date()));
		}else if(theTaskInfo.getPlan_type().equals("exam")) {
			playDate.put("from", theTaskInfo.getStart_date());
			playDate.put("to", theTaskInfo.getEnd_date());
		}else {
			playDate.put("from", theTaskInfo.getStart_date());
			playDate.put("to", theTaskInfo.getEnd_date());
		}
		
		JSONArray mediaUrl = new JSONArray();
		Map<String,Object> conditionMap = new HashMap<String,Object>();
		File checkFile = null;
		if (theTaskInfo.getTask_type() != 1) {
			for (int i = 2; i < content.size(); i++) {
				String attachId = (String) content.get(i);
				if(null!=attachId && attachId.length() >0) {
					conditionMap.put("attach_id", attachId);
					Map<String,Object> attachInfo=(Map<String, Object>) ms.selectOne("Attachment","selectByCondition", conditionMap);
					if(attachInfo!=null) {
						checkFile = new File((String) attachInfo.get("save_path"));
			    		if(checkFile.exists()) {
			    			mediaUrl.add(":8080/cms/file/download/" + attachId);
			    		}else {
			    			return null;
			    		}
					}
				}else {
					return null;
				}
			}
			if(mediaUrl.size()==0) {
				return null;
			}else if (theTaskInfo.getTask_type() == 2) {
				taskDataJson.put("imageUrls", mediaUrl);
			} else if (theTaskInfo.getTask_type() == 3) {
				taskDataJson.put("audioUrls", mediaUrl);
			} else if (theTaskInfo.getTask_type() == 4) {
				taskDataJson.put("videoUrls", mediaUrl);
			}
		}

		// String []weekDay = theTaskInfo.getPlay_weekdays().split(",");
		playDate.put("weekDay", JSONObject.parseArray(theTaskInfo.getPlay_weekdays()));
		taskDataJson.put("playDate", playDate);
		taskDataJson.put("playTime", JSONObject.parseArray(theTaskInfo.getPlay_periods()));

		return taskDataJson;
	}
	
	

	private JSONObject transferTaskInfo2web(Map<String, Object> oldTaskInfo) {
		JSONObject retTaskInfo = new JSONObject();
		retTaskInfo.put("id", oldTaskInfo.get("id"));
		retTaskInfo.put("task_id", oldTaskInfo.get("task_id"));

	
		Set<String> ready_terminals = RedisUtils.allSetData(Constant.TaskTerminalReady + oldTaskInfo.get("task_id"));
		retTaskInfo.put("ready_terminals", ready_terminals.toArray());
		Set<String> ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + oldTaskInfo.get("task_id"));
		retTaskInfo.put("ok_terminals", ok_terminals.toArray());
		Set<String> fail_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsFailed + oldTaskInfo.get("task_id"));
		retTaskInfo.put("fail_terminals", fail_terminals.toArray());

		retTaskInfo.put("create_user", oldTaskInfo.get("create_user"));
		retTaskInfo.put("create_time", oldTaskInfo.get("create_time"));
		retTaskInfo.put("task_name", oldTaskInfo.get("task_name"));
		retTaskInfo.put("task_type", oldTaskInfo.get("task_type"));
		retTaskInfo.put("plan_type", oldTaskInfo.get("plan_type"));
		retTaskInfo.put("priority", oldTaskInfo.get("priority"));
		retTaskInfo.put("start_date", oldTaskInfo.get("start_date"));
		retTaskInfo.put("end_date", oldTaskInfo.get("end_date"));
		retTaskInfo.put("play_mode", oldTaskInfo.get("play_mode"));

		String weekDays = (String) oldTaskInfo.get("play_weekdays");
		if (weekDays != null && weekDays.length() > 0) {
			retTaskInfo.put("week_days", JSONObject.parse(weekDays));
		}

		String periods = (String) oldTaskInfo.get("play_periods");
		if (periods != null && periods.length() > 0) {
			retTaskInfo.put("play_periods", JSONObject.parse(periods));
		}

		String content = (String) oldTaskInfo.get("content");
		if (periods != null && periods.length() > 0) {
			retTaskInfo.put("content", JSONObject.parse(content));
		}
		return retTaskInfo;
	}

	public JSONArray doSendTask(JSONArray taskInfoList, List<Map<String, Object>> allTerminals, String action) {
		boolean sendResult = false;
		if (taskInfoList == null || allTerminals == null || taskInfoList.size() == 0 || allTerminals.size() == 0) {
			log.error("\ndo SendTask parameter is null!");
			return null;
		}
		JSONObject socketBody = null;
		TaskInfo tmpTaskInfo = null;
		String terminalIP = "";

		TerminalLog newTerminalLog = new TerminalLog();
//		Map<String, Object> updateTask = new HashMap<String, Object>();
		SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();

		//JSONArray sequeceList = new JSONArray();
		Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
		Map<Integer, Object> sequeceTaskMap = new HashMap<Integer, Object>();
		int sequece = 0;

		for (int i = 0; i < allTerminals.size(); i++) {
//			String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + allTerminals.get(i).get("terminal_id"));
//			if (null == onlineInfo) {
//				continue;// 剔除掉不在线的设备
//			}
			terminalIP = (String) allTerminals.get(i).get("ip");
			for (int j = 0; j < taskInfoList.size(); j++) {
				tmpTaskInfo = (TaskInfo) taskInfoList.get(j);
				socketBody = buildTerminalTaskData(tmpTaskInfo);
				if(socketBody==null) {
					return null;
				}
				if (tmpTaskInfo.getTask_type() == Constant.TextTask) {
					sequece = newSocketMsgHandler.sendMsgTo(terminalIP, cmds.TEXT_JOB, socketBody.toJSONString()); // 发送文本任务到终端设备
				} else if (tmpTaskInfo.getTask_type() == Constant.ImageTask) {
					sequece = newSocketMsgHandler.sendMsgTo(terminalIP, cmds.IMAGE_JOB, socketBody.toJSONString()); // 发送图片任务到终端设备
				} else if (tmpTaskInfo.getTask_type() == Constant.AudioTask) {
					sequece = newSocketMsgHandler.sendMsgTo(terminalIP, cmds.AUDIO_JOB, socketBody.toJSONString()); // 发送音频任务到终端设备
				} else if (tmpTaskInfo.getTask_type() == Constant.VideoTask) {
					sequece = newSocketMsgHandler.sendMsgTo(terminalIP, cmds.VIDEO_JOB, socketBody.toJSONString()); // 发送视频任务到终端设备
				} else {
					log.error("unkown task type! task type==" + tmpTaskInfo.getTask_type());
					return null;
				}

				//sequeceList.add(sequece);
				sequeceMap.put(sequece, (String) allTerminals.get(i).get("terminal_id"));
				JSONObject tmpInfo = new JSONObject();
				tmpInfo.put("task_id", tmpTaskInfo.getTask_id());
				tmpInfo.put("terminal_id", (String) allTerminals.get(i).get("terminal_id"));
				tmpInfo.put("terminal_ip", terminalIP);
				tmpInfo.put("socketBody", socketBody);

				sequeceTaskMap.put(sequece, tmpInfo);
			}
		}
		//修改某个终端不在线是，导致的任务下发过慢的问题
		Map<String, Object> result2Web = new HashMap<String, Object>();
		Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 8000);
		JSONArray resultList = new JSONArray();
		int successCnt = 0;
		if (RespMap != null && RespMap.size() > 0) {
			for (Integer key : RespMap.keySet()) {// key 是sequece
				JSONObject tmpInfo = (JSONObject) sequeceTaskMap.get(key);
				socketBody = tmpInfo.getJSONObject("socketBody");
				String terminalRespStr = RespMap.get(key).getString("resp");
				//String terminal_ip = RespMap.get(key).getString("terminal_ip");
				String terminal_id = RespMap.get(key).getString("terminal_id");
				JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
				int result = (int) terminalRespJson.get("status");
				if (result == 1) {
					successCnt++;
					RedisUtils.addMember(Constant.TaskSentTerminalsOK + tmpInfo.getString("task_id"), terminal_id,0);
					RedisUtils.removeMember(Constant.TaskTerminalReady + tmpInfo.getString("task_id"), terminal_id);
					RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + tmpInfo.getString("task_id"), terminal_id);
					newTerminalLog.setTerminal_id(tmpInfo.getString("terminal_id"));
					newTerminalLog.setTerminal_ip(tmpInfo.getString("terminal_ip"));
					newTerminalLog.setAction(action);
					newTerminalLog.setContent(tmpInfo.getJSONObject("socketBody").toJSONString());
					newTerminalLog.setResult("ok");
					es.insert("TerminalLog", "insert", newTerminalLog);
				} else {
					RedisUtils.addMember(Constant.TaskSentTerminalsFailed + tmpInfo.getString("task_id"), terminal_id,0);
					RedisUtils.removeMember(Constant.TaskTerminalReady + tmpInfo.getString("task_id"), terminal_id);
					RedisUtils.removeMember(Constant.TaskSentTerminalsOK + tmpInfo.getString("task_id"), terminal_id);
				}
			}
		}
		result2Web.put("failedCnt", sequeceMap.size());
		resultList.add(result2Web);

		return resultList;
	}

	// 获取任务列表
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
			String planType = jsonBody.getString("plan_type");
			String panId = jsonBody.getString("plan_id");
			Map conMap = new HashMap<String, Object>();
			if(adminUser.getIs_supper()==0) {
				Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(adminUser.getUid());
				if(tmpUserSet!=null && tmpUserSet.size() >0) {
					conMap.put("creator_uids", tmpUserSet.toArray());
				}else {
					conMap.put("creator_uid", adminUser.getUid());
				}
			}
			
			conMap.put("plan_type", planType);
			if (getTotal != null) {
				String tableName = "TaskInfo";
				if (planType.equals("normal")) {
					tableName = "TaskInfo";
				} else if (planType.equals("exam")) {
					tableName = "ExamTask";
					conMap.put("exam_id", panId);
				}
				List<Map<String, Object>> totalList = ms.selectList(tableName, "selectCountByCondition", conMap);
				if (totalList != null && totalList.size() == 1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}

			conMap.put("startrom", (page - 1) * pagesize);
			conMap.put("pagesize", pagesize);
			List<Map<String, Object>> examTaskList = null;
			if (planType != null && planType.length() > 0 && planType.equals("exam")) {
				// conMap.put("plan_type", planType);
				conMap.put("exam_id", panId);
				conMap.put("sort", "create_time asc");
				examTaskList = ms.selectList("ExamTask", "selectByConditionWithPage", conMap);
			}

			JSONArray retTaskList = new JSONArray();
			if (examTaskList != null && examTaskList.size() > 0) {
				examTaskList = Convert.SortDataListId(examTaskList, page, pagesize);
				int tmpCount =0;
				conMap.clear();
				JSONArray tmpTaskIds = new JSONArray();
				for (int i = 0; i < examTaskList.size(); i++) {
					tmpTaskIds.add(examTaskList.get(i).get("task_id"));
				}
				conMap.put("task_ids", tmpTaskIds);
				List<Map<String, Object>> tasklist = ms.selectList("TaskInfo", "getTaskByIds", conMap);
				if(tasklist!=null && tasklist.size()>0) {
					for(int i=0;i<tasklist.size();i++) {
						JSONObject retTaskInfo = transferTaskInfo2web(tasklist.get(i));
						retTaskInfo.put("plan_id", panId);
						retTaskInfo.put("id", (page - 1) * pagesize + tmpCount+1);
						retTaskList.add(retTaskInfo);
						tmpCount++;
					}
				}
			} else if (planType.equals("normal") || planType.equals("temporary")) {
				conMap.put("sort", "update_time desc");// 最新更新的排在前面
				List<Map<String, Object>> tasklist = ms.selectList("TaskInfo", "selectByConditionWithPage", conMap);
				tasklist = Convert.SortDataListId(tasklist, page, pagesize);
				if (tasklist != null && tasklist.size() > 0) {
					for (int i = 0; i < tasklist.size(); i++) {
						JSONObject retTaskInfo = transferTaskInfo2web(tasklist.get(i));
						retTaskList.add(retTaskInfo);
					}
				}
			}

			if (retTaskList != null && retTaskList.size() > 0) {
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
	
	
	// 获取任务列表
	@RequestMapping(value = "/getTaskByTaskId", method = RequestMethod.POST)
	public void getTaskByTaskId(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 1;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);

			String planType = jsonBody.getString("plan_type");
			String panId = jsonBody.getString("plan_id");
			String task_id = jsonBody.getString("task_id");
			Map conMap = new HashMap<String, Object>();
			conMap.put("plan_type", planType);
			
			if(adminUser.getIs_supper()==0) {
				int isHasPermission = checkActionPermission(adminUser.getUid(),task_id);
				if(isHasPermission==0) {
					respJson.put("status", Constant.UserNotLogin);
					respJson.put("msg", Constant.PermissionErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}

			List<Map<String, Object>> examTaskList = null;
			if (planType != null && planType.length() > 0 && planType.equals("exam")) {
				conMap.put("exam_id", panId);
				examTaskList = ms.selectList("ExamTask", "selectByPK", conMap);
			}

			JSONArray retTaskList = new JSONArray();
			if (examTaskList != null && examTaskList.size() > 0) {
				for (int i = 0; i < examTaskList.size(); i++) {
					if(task_id.equals(examTaskList.get(i).get("task_id"))) {
						conMap.clear();
						conMap.put("task_id", task_id);
						List<Map<String, Object>> tasklist = ms.selectList("TaskInfo", "selectByPK", conMap);
						if (tasklist != null && tasklist.size() == 1) {
							JSONObject retTaskInfo = transferTaskInfo2web(tasklist.get(0));
							retTaskInfo.put("plan_id", examTaskList.get(i).get("exam_id"));
							retTaskInfo.put("id", examTaskList.get(i).get("id"));
							retTaskList.add(retTaskInfo);
							break;
						}
					}
					
				}
			} else if (planType.equals("normal") || planType.equals("temporary")) {
				conMap.put("task_id", task_id);
				List<Map<String, Object>> tasklist = ms.selectList("TaskInfo", "selectByPK", conMap);
				if (tasklist != null && tasklist.size() == 1) {
					JSONObject retTaskInfo = transferTaskInfo2web(tasklist.get(0));
					retTaskList.add(retTaskInfo);
				}
			}

			if (retTaskList != null && retTaskList.size() > 0) {
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
	
	
	// 设置未启用或者下发失败的终端
		@RequestMapping(value = "/setReadyOrFailedTerminal", method = RequestMethod.POST)
		@ResponseBody
		public void setReadyOrFailedTerminal(HttpServletRequest request, HttpServletResponse response) {
			JSONObject respJson = new JSONObject();
			int result = 0;
			User adminUser = SesCheck.getUserBySession(request, es, false);
			if (adminUser != null) {
				String jsonBodyStr = HttpIO.getBody(request);
				JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
				String task_id = jsonBody.getString("task_id");
				JSONArray ready_terminals = jsonBody.getJSONArray("ready_terminals");// ip
				JSONArray failed_terminals = jsonBody.getJSONArray("fail_terminals");// ip
				if(adminUser.getIs_supper()==0) {
					int isHasPermission = checkActionPermission(adminUser.getUid(),task_id);
					if(isHasPermission==0) {
						respJson.put("status", Constant.UserNotLogin);
						respJson.put("msg", Constant.PermissionErr);
						HttpIO.writeResp(response, respJson);
						return;
					}
				}
				Set<String> ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + task_id);
				for(String ok_terminal:ok_terminals) {
					if(ready_terminals!=null) {
						ready_terminals.remove(ok_terminal);
					}
					if(failed_terminals!=null) {
						failed_terminals.remove(ok_terminal);
					}
				}
				
				if (ready_terminals != null) {
					RedisUtils.del(Constant.TaskTerminalReady + task_id);// 移除然后在新增
					for (int i = 0; i < ready_terminals.size(); i++) {
						RedisUtils.addMember(Constant.TaskTerminalReady + task_id, ready_terminals.getString(i), 0);
						result = 1;
					}
				}

				if (failed_terminals != null) {
					// RedisUtils.del(Constant.TaskSentTerminalsFailed + setting_id);//移除然后在新增
					for (int i = 0; i < failed_terminals.size(); i++) {
						RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + task_id,failed_terminals.getString(i));
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
	
	

	// 创建或者修改任务信息
	@RequestMapping(value = "/addOrUpdate", method = RequestMethod.POST)
	public void addTask(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String task_id = jsonBody.getString("task_id");
			String plan_id = jsonBody.getString("plan_id");
			String planType = jsonBody.getString("plan_type");
			if(adminUser.getIs_supper()==0 && task_id!=null) {
				int isHasPermission = checkActionPermission(adminUser.getUid(),task_id);
				if(isHasPermission==0) {
					respJson.put("status", Constant.UserNotLogin);
					respJson.put("msg", Constant.PermissionErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			if(task_id!=null) {
				Set<String> ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + task_id);
				if(ok_terminals!=null && ok_terminals.size() >0) {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", "存在启用的任务不可以修改!");
					HttpIO.writeResp(response, respJson);
				}
			}
			

			TaskInfo theTaskInfo = new TaskInfo();
			theTaskInfo.setTask_name(jsonBody.getString("task_name"));
			theTaskInfo.setTask_type(jsonBody.getIntValue("task_type"));
			theTaskInfo.setPriority(jsonBody.getIntValue("priority"));
			theTaskInfo.setStart_date(jsonBody.getString("start_date"));
			theTaskInfo.setEnd_date(jsonBody.getString("end_date"));
			theTaskInfo.setPlay_mode(jsonBody.getIntValue("play_mode"));
			if (planType != null && planType.length() > 0) {
				theTaskInfo.setPlan_type(planType);
			}

			JSONArray weekDays = jsonBody.getJSONArray("week_days");
			if (jsonBody.getIntValue("play_mode") == 2) { // 按周循环
				if (weekDays != null && weekDays.size() > 0) {
					theTaskInfo.setPlay_weekdays(weekDays.toJSONString());
				} else {
					theTaskInfo.setPlay_weekdays("[]");
				}
			} else {
				weekDays = new JSONArray();
				theTaskInfo.setPlay_weekdays(weekDays.toJSONString());
			}

			JSONArray periods = jsonBody.getJSONArray("play_periods");
			if (periods != null && periods.size() > 0) {
				theTaskInfo.setPlay_periods(periods.toJSONString());
			}

			JSONArray content = jsonBody.getJSONArray("content");
			if (content != null && content.size() > 0) {
				theTaskInfo.setContent(content.toJSONString());
			} else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.ParemeterErr);
				HttpIO.writeResp(response, respJson);
				return;
			}

			if (task_id != null && task_id.length() > 0) {
				theTaskInfo.setTask_id(task_id);
				Map<String, Object> conMap = new HashMap<String, Object>();
				conMap.put("task_id", task_id);
				List<Map<String, Object>> tasklist = ms.selectList("TaskInfo", "selectByPK", conMap);
				if (tasklist != null && tasklist.size() == 1) {
					Set<String> sendTerminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + task_id);
					if(sendTerminals==null||sendTerminals.size()==0) {
						if(jsonBody.getString("create_user").equals(adminUser.getUsername())) {
							theTaskInfo.setCreator_uid(adminUser.getUid());
						}
						result = es.update("TaskInfo", "update", theTaskInfo);
					}
				} else {
					result = Constant.notExistStatus;
				}
			} else {
				task_id = "taskid" + new Date().getTime();
				task_id = task_id + RandomStringUtils.randomAlphanumeric(32 - task_id.length()); // id总长度为32个字符
				theTaskInfo.setTask_id(task_id);
				theTaskInfo.setCreate_time(new Date());
				theTaskInfo.setCreate_user(adminUser.getUsername());
				theTaskInfo.setCreator_uid(adminUser.getUid());

				SimpleDateFormat myFmt = new SimpleDateFormat("yyyy-MM-dd");
				myFmt.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
				theTaskInfo.setStart_date(myFmt.format(new Date()));
				theTaskInfo.setEnd_date(myFmt.format(new Date()));

				result = es.insert("TaskInfo", "insert", theTaskInfo);

				if (result == 1 && planType.equals("exam") && plan_id != null && plan_id.length() > 0) { // 如果是考试预案任务，则新增关系记录
					ExamTask newExamTask = new ExamTask();
					newExamTask.setExam_id(plan_id);
					newExamTask.setTask_id(task_id);
					es.insert("ExamTask", "insert", newExamTask);
				}
			}

			if (result == 1) {
				JSONArray taskIds = new JSONArray();
				taskIds.add(task_id);
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", taskIds);
			} else if (result == Constant.notExistStatus) {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("msg", "任务不存在!");
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

	// 删除任务信息
	@RequestMapping(value = "/delTask", method = RequestMethod.POST)
	public void delTask(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 1;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			JSONArray task_ids = jsonBody.getJSONArray("task_ids");
			String plan_type = jsonBody.getString("plan_type");
			String plan_id = jsonBody.getString("plan_id");
			if(adminUser.getIs_supper()==0) {
				for(int i=0;i<task_ids.size();i++) {
					int isHasPermission = checkActionPermission(adminUser.getUid(),task_ids.getString(i));
					if(isHasPermission==0) {
						respJson.put("status", Constant.UserNotLogin);
						respJson.put("msg", Constant.PermissionErr);
						HttpIO.writeResp(response, respJson);
						return;
					}
				}
			}
			
			Map<String, Object> condMap = new HashMap<String, Object>();
			if(task_ids!=null && task_ids.size() >0) {
				for(int i=0;i<task_ids.size();i++) {
					Set<String> ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + task_ids.getString(i));
					if(ok_terminals==null||ok_terminals.size()==0) { //不存在启用的终端的任务才可以删除
						condMap.clear();
						condMap.put("task_id", task_ids.getString(i));
						result = ms.execute("TaskInfo", "delete", condMap);
						if(result >0) {
							if (plan_type != null && plan_type.equals("exam")) {
								condMap.clear();
								condMap.put("exam_id", plan_id);
								condMap.put("task_id", task_ids.getString(i));
								result = ms.execute("ExamTask", "delete", condMap);
							}
							RedisUtils.del(Constant.TaskSentTerminalsOK + task_ids.getString(i));
							RedisUtils.del(Constant.TaskTerminalReady + task_ids.getString(i));
							RedisUtils.del(Constant.TaskSentTerminalsFailed + task_ids.getString(i));
						}
					}
				}
			}
			if (result >= 1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", "删除成功!");
				// 下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionDel);
				JSONObject content = new JSONObject();
				content.put("action_name", "删除任务");
				content.put("plan_type", plan_type);
				content.put("task_ids", task_ids.toJSONString());
				actionLog.put("action_content", content);
				es.insert("UserLog", "insert", actionLog);
			} else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "删除失败!");
			}
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}

	// 往终端下发任务
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/send2terminal", method = RequestMethod.POST)
	public void sendTask2Terminals(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			//JSONArray GroupIds = jsonBody.getJSONArray("group_ids");// 终端分组
			String task_id = jsonBody.getString("task_id");
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			String  terminal_type = jsonBody.getString("terminal_type");
			//int isAll = jsonBody.getIntValue("isAll");
			//int isResend = jsonBody.getIntValue("resend");
			if(adminUser.getIs_supper()==0) {
				int isHasPermission = checkActionPermission(adminUser.getUid(),task_id);
				if(isHasPermission==0) {
					respJson.put("status", Constant.UserNotLogin);
					respJson.put("msg", Constant.PermissionErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			
			if(terminal_ids!=null && (terminal_type==null||terminal_type=="ready")) {
				RedisUtils.del(Constant.TaskTerminalReady + task_id);// 移除然后在新增
				for (int i = 0; i < terminal_ids.size(); i++) {
					RedisUtils.addMember(Constant.TaskTerminalReady + task_id, terminal_ids.getString(i), 0);
					result = 1;
				}
			}
			String planType = "normal";
			
			if(terminal_ids==null) {
				terminal_ids = new JSONArray();
			}
			Map<String, Object> condMap = new HashMap<String, Object>();
			JSONArray TaskNames = new JSONArray();
			JSONArray taskInfoList = new JSONArray();
			TaskInfo conTaskInfo = new TaskInfo();
			conTaskInfo.setTask_id(task_id);
			List<TaskInfo> tmptaskInfoList = es.select("TaskInfo", "selectByPK", conTaskInfo);
			if (tmptaskInfoList != null && tmptaskInfoList.size() == 1) {
				taskInfoList.add(tmptaskInfoList.get(0));
				planType = tmptaskInfoList.get(0).getPlan_type();
				
				if(planType.equals("exam")) {
					Map conMap = new HashMap<String,Object>();
					conMap.put("task_id", task_id);
					List<Map<String,Object>> examTasklist = ms.selectList("ExamTask", "selectByCondition", conMap);
					if(examTasklist!=null && examTasklist.size()==1) {
						conMap.clear();
						conMap.put("exam_id", examTasklist.get(0).get("exam_id"));
						List<Map<String,Object>> examInfolist = ms.selectList("ExamInfo", "selectByCondition", conMap);
						if(examInfolist!=null && examInfolist.size()==0) {
							tmptaskInfoList.get(0).setStart_date((String) examInfolist.get(0).get("start_date"));
							tmptaskInfoList.get(0).setEnd_date((String) examInfolist.get(0).get("end_date"));
						}
					}
				}
				
				TaskNames.add(tmptaskInfoList.get(0).getTask_name());
				Set<String> tmpTerminals = null;
				if (terminal_type!=null && terminal_type.equals("ready")) {
					tmpTerminals = RedisUtils.allSetData(Constant.TaskTerminalReady + task_id);
				} else if (terminal_type!=null && terminal_type.equals("fail")) {
					tmpTerminals = RedisUtils.allSetData(Constant.TaskSentTerminalsFailed + task_id);
				}
				
				if(tmpTerminals!=null) {
					for(String tmpTerminal_id:tmpTerminals) {
						terminal_ids.remove(tmpTerminal_id);
						terminal_ids.add(tmpTerminal_id);
					}
				}
				
				List<Map<String, Object>> allTerminals = null;
				if (terminal_ids != null && terminal_ids.size() > 0) {
					condMap.clear();
					condMap.put("terminal_ids", terminal_ids);
					allTerminals = ms.selectList("Terminal", "selectByIds", condMap);
				}
				JSONArray sendResult = doSendTask(taskInfoList, allTerminals, planType + "_plan"); // 实际发送任务到终端
				if (sendResult != null && sendResult.size() > 0) {
					respJson.put("status", Constant.SUCCESS);
					respJson.put("result", sendResult);
					// 下面添加用户操作日志
					JSONObject actionLog = new JSONObject();
					actionLog.put("uid", adminUser.getUid());
					actionLog.put("username", adminUser.getUsername());
					actionLog.put("realname", adminUser.getReal_name());
					actionLog.put("action_type", Constant.ActionSend);
					JSONObject content = new JSONObject();
					content.put("action_name", "发送任务");
					content.put("task_id", task_id);
					content.put("task_nam", TaskNames);
					actionLog.put("action_content", content.toJSONString());
					es.insert("UserLog", "insert", actionLog);
				} else {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg","操作失败，请检查任务执行时间以及内容是否正确");
				}
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg","操作失败，请检查任务执行时间以及内容是否正确");
			}
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}

		HttpIO.writeResp(response, respJson);
	}

	// 停止任务
	@RequestMapping(value = "/stopTask", method = RequestMethod.POST)
	public void stopTaskById(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String task_id = jsonBody.getString("task_id");
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			
			if(adminUser.getIs_supper()==0) {
				int isHasPermission = checkActionPermission(adminUser.getUid(),task_id);
				if(isHasPermission==0) {
					respJson.put("status", Constant.UserNotLogin);
					respJson.put("msg", Constant.PermissionErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			
			JSONObject socketBody = new JSONObject();
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			//JSONArray sequeceList = new JSONArray();
			Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
			JSONArray tmpTaskIds = new JSONArray();

			Map<String, Object> condMap = new HashMap<String, Object>();
			
			if(terminal_ids==null || terminal_ids.size() ==0) {
				Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + task_id);
				terminal_ids = new JSONArray();
				for(String tmpTerminalId:tmpOKterminals) {
					terminal_ids.add(tmpTerminalId);
				}
			}
			condMap.clear();
			condMap.put("terminal_ids", terminal_ids);
			List<Map<String, Object>>  terminalInfoList = ms.selectList("Terminal", "selectByIds", condMap);
			if(terminalInfoList!=null && terminalInfoList.size()>0) {
				tmpTaskIds.clear();
				tmpTaskIds.add(task_id);
				for (int i=0;i<terminalInfoList.size();i++) {
					socketBody.put("deleteMode", 1);// 删除指定的任务ＩＤ
					socketBody.put("taskIds", tmpTaskIds);
					int sequece = newSocketMsgHandler.sendMsgTo((String) terminalInfoList.get(i).get("ip"), cmds.DELETE_JOBS, socketBody.toJSONString());
					 sequeceMap.put(sequece, (String) terminalInfoList.get(i).get("terminal_id"));
				}
			}
			Map<String, Object> result2Web = new HashMap<String, Object>();
			Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 8000);
			//int failedCnt = 0;
			int successCnt = 0;
			if (RespMap != null && RespMap.size() > 0) {
				for (Integer key : RespMap.keySet()) {// key 是sequece
					String terminalRespStr = RespMap.get(key).getString("resp");
					String terminal_ip = RespMap.get(key).getString("terminal_ip");
					String terminal_id = RespMap.get(key).getString("terminal_id");
					JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
					result = (int) terminalRespJson.get("status");
					if (result == 1) {
						successCnt++;
						//RedisUtils.removeMember(Constant.TaskSentTerminalsOK + task_id,terminal_id_ip);
						RedisUtils.removeMember(Constant.TaskSentTerminalsOK + task_id,terminal_id);
						RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + task_id,terminal_id);
						RedisUtils.addMember(Constant.TaskTerminalReady + task_id,terminal_id,0);
						condMap.clear();
						condMap.put("terminal_ip", terminal_ip);
						condMap.put("result", "terminal_del");// 标记为终端已删除
						condMap.put("content", task_id);
						ms.execute("TerminalLog", "update", condMap);
					}
				}
			}
			result2Web.put("failedCnt",sequeceMap.size());
			JSONArray resultList = new JSONArray();
			resultList.add(result2Web);
			if (result == 1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", resultList);
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

	// 停止某个终端的任务
	@RequestMapping(value = "/stopTerminalTask", method = RequestMethod.POST)
	public void stopTerminalTask(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			JSONArray task_ids = jsonBody.getJSONArray("task_ids");
			String terminal_ip = jsonBody.getString("terminal_ip");
			String terminal_id = jsonBody.getString("terminal_id");
			String terminal_id_ip = terminal_id + ":" + terminal_ip;
			
			if(adminUser.getIs_supper()==0) {
				for(int i=0;i<task_ids.size();i++) {
					int isHasPermission = checkActionPermission(adminUser.getUid(),task_ids.getString(i));
					if(isHasPermission==0) {
						respJson.put("status", Constant.UserNotLogin);
						respJson.put("msg", Constant.PermissionErr);
						HttpIO.writeResp(response, respJson);
						return;
					}
				}
			}
			
			
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			//JSONArray sequeceList = new JSONArray();
			Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
			JSONObject socketBody = new JSONObject();
			
			if(task_ids!=null && task_ids.size()>0) {
				int sequece =0;
				JSONArray tmpTaskIds = new JSONArray();
				Map<Integer, Object> sequeceTerminalMap = new HashMap<Integer, Object>();
				for(int i=0;i<task_ids.size();i++) {
					socketBody.clear();
					Map<String, Object> taskInfo = getTaskInfoByTaskId(task_ids.getString(i));
					if(taskInfo!=null) {
						//普通定时任务
						String tableName = (String) taskInfo.get("table_name");
						if(tableName.equals("TaskInfo")) {
							tmpTaskIds.clear();
							tmpTaskIds.add((String) taskInfo.get("task_id"));
							socketBody.put("deleteMode", 1);// 删除指定的任务ID
							socketBody.put("taskIds", tmpTaskIds);
							sequece = newSocketMsgHandler.sendMsgTo(terminal_ip, cmds.DELETE_JOBS, socketBody.toJSONString());
							 sequeceMap.put(sequece, terminal_id);
						}else if(tableName.equals("BootSetting")) {
							 socketBody.put("mode", 2);//2为取消
							 sequece = newSocketMsgHandler.sendMsgTo(terminal_ip, cmds.SET_BOOTSHUT_TIME, socketBody.toJSONString());
							 sequeceMap.put(sequece, terminal_id);
						}else if(tableName.equals("LampSetting")) {
							 socketBody.put("action", 2);//1为屏蔽器，２灯控
							 socketBody.put("mode", 2);
							 socketBody.put("taskId", task_ids.getString(i));
							 sequece = newSocketMsgHandler.sendMsgTo(terminal_ip, cmds.SCHEDULE_TASK, socketBody.toJSONString());
							 sequeceMap.put(sequece, terminal_id);
						}else if(tableName.equals("ShieldTask")) {
							socketBody.put("taskId", task_ids.getString(i));
							socketBody.put("action", 1);//１是屏蔽器,2是灯控
							socketBody.put("mode", 2);
							sequece = newSocketMsgHandler.sendMsgTo(terminal_ip,cmds.SCHEDULE_TASK, socketBody.toJSONString());
							 sequeceMap.put(sequece, terminal_id);
						}else if(tableName.equals("UrgencyTask")) {
							tmpTaskIds.clear();
							tmpTaskIds.add((String) taskInfo.get("urgency_id"));
							socketBody.put("deleteMode", 1);// 删除指定的任务ID
							socketBody.put("taskIds", tmpTaskIds);
							sequece = newSocketMsgHandler.sendMsgTo(terminal_ip, cmds.DELETE_JOBS, socketBody.toJSONString());
							sequeceMap.put(sequece, terminal_id);
						}
						sequeceTerminalMap.put(sequece, task_ids.getString(i));
					}
				}
				
				JSONArray failedList = new JSONArray();
				Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
				if (RespMap != null && RespMap.size() > 0) {
					for (Integer key : RespMap.keySet()) {// key 是sequece
						String taskId = (String) sequeceTerminalMap.get(key);
						//String terminal_id2 = RespMap.get(key).getString("terminal_id");
						String terminalRespStr = RespMap.get(key).getString("resp");
						JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
						result = (int) terminalRespJson.get("status");
						if (result == 1) {
							RedisUtils.removeMember(Constant.TaskSentTerminalsOK + taskId,terminal_id);
							RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + taskId,terminal_id);
							RedisUtils.addMember(Constant.TaskTerminalReady + taskId,terminal_id,0);
						} else {
							failedList.add(taskId);
						}
					}
				}
				
				// 下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionClean);
				JSONObject content = new JSONObject();
				content.put("action_name", "删除终端任务");
				content.put("TaskIds", task_ids);
				content.put("terminal_ids", terminal_id);
				content.put("cleanType", "terminal");

				actionLog.put("action_content", content.toJSONString());
				es.insert("UserLog", "insert", actionLog);

				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", failedList);
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.FailedMsg);
			}
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}
	
	
	@RequestMapping(value = "/sendCountDownTime", method = RequestMethod.POST)
	public void sendCountDownTime(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String plan_id = jsonBody.getString("plan_id");
			String plan_type = jsonBody.getString("plan_type");
			String start_date = jsonBody.getString("start_date");
			String end_date = jsonBody.getString("end_date");
			String am_end_time = jsonBody.getString("am_end_time");
			String pm_end_time = jsonBody.getString("pm_end_time");
			Integer countDownVal =  jsonBody.getInteger("count_down");
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			String action = jsonBody.getString("action");//set为设置，unset为取消设置
			Map<String, Object> condMap = new HashMap<String, Object>();
			
			if(terminal_ids==null) {
				terminal_ids= new JSONArray();
			}
			
			if (plan_type != null && plan_type.equals("exam") && plan_id!=null) {
				condMap.put("exam_id", plan_id);
				List<Map<String, Object>> examList = ms.selectList("ExamTask", "selectByCondition", condMap);
				if(examList!=null && examList.size() >0) {
					for(int i=0;i<examList.size();i++) {
						Set<String> tmpTerminalIds = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + examList.get(i).get("task_id"));
						for(String tmpTerminalId:tmpTerminalIds) {
							terminal_ids.remove(tmpTerminalId);
							terminal_ids.add(tmpTerminalId);
						}
					}
					condMap.clear();
					condMap.put("exam_id", plan_id);
					List<Map<String, Object>> examInfoList = ms.selectList("ExamInfo", "selectByCondition", condMap);
					if(examInfoList!=null && examInfoList.size()>0) {
						start_date = (String) examInfoList.get(0).get("start_date");
						end_date = (String) examInfoList.get(0).get("end_date");
						am_end_time = (String) examInfoList.get(0).get("am_end_time");
						pm_end_time = (String) examInfoList.get(0).get("pm_end_time");
						String tmpContDown = (String) examInfoList.get(0).get("count_down");
						System.out.println("before get count_down:" + tmpContDown);
						countDownVal = Integer.parseInt(tmpContDown);
						System.out.println("after get count_down:" + countDownVal);
					}
				}
			}
			List<Map<String, Object>> allTerminals = null;
			if (terminal_ids != null && terminal_ids.size() > 0) {
				condMap.clear();
				condMap.put("terminal_ids", terminal_ids);
				allTerminals = ms.selectList("Terminal", "selectByIds", condMap);
		
			}
			
			if(allTerminals!=null && countDownVal >0) {
				SimpleDateFormat f=new SimpleDateFormat("HH:mm");
				String theEndTime = null;
				try {
					JSONArray resultList =null;
					if(am_end_time!=null && am_end_time.length() >0) {
						theEndTime =  f.format(new Date(f.parse(am_end_time).getTime() - countDownVal*60 * 1000));
					    resultList =sendCountDownCmd(allTerminals,plan_id,action,start_date,end_date,theEndTime,countDownVal*60);
					}
					if(pm_end_time!=null && pm_end_time.length() >0) {
						theEndTime =  f.format(new Date(f.parse(pm_end_time).getTime() - countDownVal*60 * 1000));
						JSONArray tmpResultList =sendCountDownCmd(allTerminals,plan_id,action,start_date,end_date,theEndTime,countDownVal*60);
						if(resultList!=null && tmpResultList!=null) {
							resultList.addAll(tmpResultList);
						}
					}
					respJson.put("status", Constant.SUCCESS);
					respJson.put("result", resultList);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", Constant.FailedMsg);
				}
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.FailedMsg);
			}	
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}

	// 清除终端任务信息 该接口目前暂未使用
	@RequestMapping(value = "/clean", method = RequestMethod.POST)
	public void cleanTerminalTask(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String cleanType = jsonBody.getString("clean_type");
			JSONArray TerminalIds = jsonBody.getJSONArray("terminal_ids");
			JSONArray TaskIds = jsonBody.getJSONArray("task_ids");
			
			if(adminUser.getIs_supper()==0) {
				for(int i=0;i<TaskIds.size();i++) {
					int isHasPermission = checkActionPermission(adminUser.getUid(),TaskIds.getString(i));
					if(isHasPermission==0) {
						respJson.put("status", Constant.UserNotLogin);
						respJson.put("msg", Constant.PermissionErr);
						HttpIO.writeResp(response, respJson);
						return;
					}
				}
			}
			
			Map<String, Object> condMap = new HashMap<String, Object>();
			JSONObject socketBody = new JSONObject();

			List<Map<String, Object>> allTaskList = null;
			if (TaskIds != null) {
				for (int i = 0; i < TaskIds.size(); i++) {
					condMap.clear();
					condMap.put("task_id", TaskIds.get(i));
					List<Map<String, Object>> taskList = ms.selectList("TaskInfo", "selectByPK", condMap);
					if (allTaskList == null) {
						allTaskList = taskList;
					} else {
						allTaskList.removeAll(taskList);
						allTaskList.addAll(taskList);
					}
				}
			}

			boolean hasSend = false;
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
			String terminalResp = null;
			int sequece = 0;
			//JSONArray sequeceList = new JSONArray();
			Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
			Map<Integer, Object> sequeceTerminalMap = new HashMap<Integer, Object>();
			for (int j = 0; j < TerminalIds.size(); j++) {
				condMap.clear();
				condMap.put("terminal_id", TerminalIds.get(j));
				List<Map<String, Object>> terminalList = ms.selectList("Terminal", "selectByCondition", condMap);
				if (terminalList != null && terminalList.size() == 1) {
					socketBody.clear();
					if (cleanType.equals("task_queue")) { // 清除终端任务
						if (allTaskList == null || allTaskList.size() == 0) {
							socketBody.put("deleteMode", 3);// 如果不传任何任务id，将会删除该终端的所有任务
						} else {
							socketBody.put("deleteMode", 1);// 删除指定的任务ＩＤ
							socketBody.put("taskIds", TaskIds);
						}
						sequece = newSocketMsgHandler.sendMsgTo((String) terminalList.get(0).get("ip"),
								cmds.DELETE_JOBS, socketBody.toJSONString());
					} else if (cleanType.equals("current_task")) {
						socketBody.put("deleteMode", 2);// 删除当前任务
						sequece = newSocketMsgHandler.sendMsgTo((String) terminalList.get(0).get("ip"),
								cmds.DELETE_JOBS, socketBody.toJSONString());
					} else if (cleanType.equals("task_file")) { // 清除终端文件
						socketBody.put("mode", 2);
						sequece = newSocketMsgHandler.sendMsgTo((String) terminalList.get(0).get("ip"),
								cmds.DELETE_FILES, socketBody.toJSONString());
					}
					 sequeceMap.put(sequece, (String) TerminalIds.get(j));
					//sequeceTerminalMap.put(sequece, TerminalIds.get(j));
				}
			}

			Map<String, Object> result2Web = new HashMap<String, Object>();
			Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
			if (RespMap != null && RespMap.size() > 0) {
				for (Integer key : RespMap.keySet()) {// key 是sequece
					String terminalId = (String) sequeceTerminalMap.get(key);
					String terminalRespStr = RespMap.get(key).getString("resp");
					 String terminal_id = RespMap.get(key).getString("terminal_id");
					JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
					result = (int) terminalRespJson.get("status");
					if (result == 1) {
						result2Web.put(terminal_id, result);
					} else {
						result2Web.put(terminal_id, 0);
					}
				}
			}

			// 下面添加用户操作日志
			JSONObject actionLog = new JSONObject();
			actionLog.put("uid", adminUser.getUid());
			actionLog.put("username", adminUser.getUsername());
			actionLog.put("realname", adminUser.getReal_name());
			actionLog.put("action_type", Constant.ActionClean);
			JSONObject content = new JSONObject();
			content.put("action_name", "删除终端任务");
			content.put("TaskIds", TaskIds);
			content.put("terminal_ids", TerminalIds);
			content.put("cleanType", cleanType);

			actionLog.put("action_content", content.toJSONString());
			es.insert("UserLog", "insert", actionLog);

			respJson.put("status", Constant.SUCCESS);
			respJson.put("result", result2Web);

		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}

	// 导出定时任务
	@RequestMapping(value = "/export", method = RequestMethod.POST)
	public void exportTask(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			JSONArray taskInfoList = new JSONArray();
			String postData = request.getHeader("postData");
			JSONArray TaskIds = JSONObject.parseObject(postData).getJSONArray("task_ids");
			boolean copyFileOk = true;
			
			if(adminUser.getIs_supper()==0) {
				for(int i=0;i<TaskIds.size();i++) {
					int isHasPermission = checkActionPermission(adminUser.getUid(),TaskIds.getString(i));
					if(isHasPermission==0) {
						respJson.put("status", Constant.UserNotLogin);
						respJson.put("msg", Constant.PermissionErr);
						HttpIO.writeResp(response, respJson);
						return;
					}
				}
			}

			if (TaskIds != null && TaskIds.size() > 0) {
				TaskInfo conTaskInfo = new TaskInfo();
				for (int i = 0; i < TaskIds.size(); i++) {
					// condMap.put("task_id", TaskIds.get(i));
					conTaskInfo.setTask_id((String) TaskIds.get(i));
					List<TaskInfo> tmptaskInfoList = es.select("TaskInfo", "selectByPK", conTaskInfo);
					if (tmptaskInfoList != null && tmptaskInfoList.size() == 1) {
						taskInfoList.add(tmptaskInfoList.get(0));
					}
				}
			}
			// 文件流写到服务器端
			String exportBaseDir = "/tmp/boyao_export_task_" + new Date().getTime();
			File exportDir = new File(exportBaseDir);
//			if (exportDir.exists()) {
//				exportDir.delete();
//			}
			exportDir.mkdirs();
			Map<String, Object> conditionMap = new HashMap<String, Object>();
			JSONArray socketJsonList = new JSONArray();
			for (int j = 0; j < taskInfoList.size(); j++) {
				TaskInfo tmpTaskInfo = (TaskInfo) taskInfoList.get(j);
				JSONObject socketBody = buildTerminalTaskData(tmpTaskInfo);
				socketJsonList.add(socketBody);
				if (tmpTaskInfo.getTask_type() >= 2) {
					JSONArray content = JSONObject.parseArray(tmpTaskInfo.getContent());
					if (content.size() >= 3) {
						for (int i = 2; i < content.size(); i++) {
							conditionMap.put("attach_id", content.get(i));
							List<Map<String,Object>> fileList= ms.selectList("Attachment", "selectByCondition", conditionMap);
							if (fileList == null || fileList.size() == 0) {
								copyFileOk = false;
								break;
							} else {
								String savePath = (String) fileList.get(0).get("save_path");
								String[] tmpList = savePath.split("/");
								//String[] subfix = tmpList[tmpList.length - 1].split("\\.");
								FileUtil.copyFile(savePath, exportBaseDir + "/" + content.get(i));
							}
						}
					}
				}
			}
			
			//开始写入json文件并压缩
			File zipFile =null;
			try {
				if (copyFileOk && taskInfoList.size() >0) {
					String exportFileName = exportBaseDir + "/task_infos.json";
					FileOutputStream outputStream = new FileOutputStream(exportFileName);
					outputStream.write(socketJsonList.toString().getBytes());
					outputStream.close();

					/** 压缩方法1 */
					String zipFileName = exportBaseDir + ".zip";
					zipFile = new File(zipFileName);
					FileOutputStream fos1 = new FileOutputStream(zipFile);
					ZipUtil.compress(exportBaseDir, zipFileName);
					FileUtil.transferFile(request, response, zipFileName); // 实际传输文件
					fos1.close();
					respJson.put("status", Constant.SUCCESS);
					respJson.put("result", new JSONArray());
				} else {
					respJson.put("status", Constant.notExistStatus);
					respJson.put("result", Constant.NodataErr);
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
				FileUtil.delete(exportDir);
				FileUtil.delete(zipFile);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("result", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}
	
	
	// 获取终端定时任务列表
	@RequestMapping(value = "/terminal_task", method = RequestMethod.POST)
	public void getTerminalTask(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			JSONArray GroupIds = jsonBody.getJSONArray("group_ids");
			JSONArray TerminalIds = jsonBody.getJSONArray("terminalIds");
			int task_type = jsonBody.getIntValue("task_type");//1文本任务，2图片任务，3音频任务，4视频任务，10所有
			
			List<Map<String, Object>> allTerminals = null;
			Map<String, Object> condMap = new HashMap<String, Object>();
			if (GroupIds != null && GroupIds.size() > 0) {
				for (int i = 0; i < GroupIds.size(); i++) {
					condMap.put("gid", GroupIds.get(i));
					List<Map<String, Object>> terminalList = ms.selectList("Terminal", "selectByGroupId", condMap);
					if (allTerminals == null) {
						allTerminals = terminalList;
					} else {
						allTerminals.removeAll(terminalList);
						allTerminals.addAll(terminalList);
					}
				}
			} else if (TerminalIds != null && TerminalIds.size() > 0) {
				for (int i = 0; i < TerminalIds.size(); i++) {
					condMap.clear();
					condMap.put("terminal_id", TerminalIds.get(i));
					List<Map<String, Object>> terminalList = ms.selectList("Terminal", "selectByPK", condMap);
					if (allTerminals == null) {
						allTerminals = terminalList;
					} else {
						allTerminals.removeAll(terminalList);
						allTerminals.addAll(terminalList);
					}
				}
			}

			List<Map<String, Object>> allTaskList =  new ArrayList<Map<String, Object>>();
			//JSONArray sequeceList = new JSONArray();
			Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
			Map<Integer, Object> sequeceTerminalMap = new HashMap<Integer, Object>();
			if (allTerminals != null) {
				SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
				JSONObject socketBody = new JSONObject();
				for (int i = 0; i < allTerminals.size(); i++) {
					socketBody.put("type", task_type);//1文本任务，2图片任务，3音频任务，4视频任务，10所有
					int sequece = newSocketMsgHandler.sendMsgTo((String) allTerminals.get(i).get("ip"), cmds.GET_TASK,socketBody.toJSONString());
					 sequeceMap.put(sequece, (String) allTerminals.get(i).get("terminal_id"));
					//sequeceTerminalMap.put(sequece, allTerminals.get(i).get("terminal_id"));
				}

				Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
				if (RespMap != null && RespMap.size() > 0) {
					for (Integer key : RespMap.keySet()) {// key 是sequece
						String terminalIP =  RespMap.get(key).getString("terminal_ip");
						//String terminalId = RespMap.get(key).getString("terminal_id");
						String terminalRespStr = RespMap.get(key).getString("resp");
						JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
						result = (int) terminalRespJson.get("status");
						if (result == 1) {
							JSONObject respResult = terminalRespJson.getJSONObject("result");
							JSONArray taskId_list = respResult.getJSONArray("taskIds");
							if (taskId_list != null) {
								for (int n = 0; n < taskId_list.size(); n++) {
									condMap.clear();
									condMap.put("task_id", taskId_list.get(n));
									Map<String, Object> taskInfo = getTaskInfoByTaskId((String) taskId_list.get(n));
									if (taskInfo != null) {
										taskInfo.put("terminal_ip", terminalIP);
										allTaskList.add(taskInfo);
									}
								}
							}
						}
					}
				}
			}

			respJson.put("status", Constant.SUCCESS);
			respJson.put("result", allTaskList);
		} else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
	}
	
}