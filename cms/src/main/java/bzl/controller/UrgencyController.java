package bzl.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import bzl.common.Constant;
import bzl.common.SesCheck;
import bzl.entity.BootSetting;
import bzl.entity.ExamInfo;
import bzl.entity.ExamTask;
import bzl.entity.TaskInfo;
import bzl.entity.TerminalLog;
import bzl.entity.UrgencyTask;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import bzl.task.SocketMsgHandler;
import bzl.task.cmds;
import sun.rmi.log.LogHandler;
import utils.Convert;
import utils.HttpIO;
import utils.RedisUtils;
import utils.TimeUtil;

//下发紧急任务
@Controller
@RequestMapping("/urgency")
public class UrgencyController {

	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	private int checkActionPermission(String uid,String urgency_id) {
		int result =0;
		Map<String,Object> conMap = new HashMap<String,Object>();
		conMap.put("urgency_id", urgency_id);
		Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(uid);
		if(tmpUserSet!=null && tmpUserSet.size() >0) {
			conMap.put("creator_uids", tmpUserSet.toArray());
			List<Map<String,Object>> tmpList = ms.selectList("UrgencyTask", "selectByCondition", conMap);
			if(tmpList!=null && tmpList.get(0).get("creator_uid").equals(uid)) {
			     result =1;
			}
		}
		return result;
	}

	private List<Map<String, Object>> convertJsonStr2Array(List<Map<String, Object>> UrgencyTaskList) {
		for (int i = 0; i < UrgencyTaskList.size(); i++) {
			Set<String> ready_terminals = RedisUtils
					.allSetData(Constant.TaskTerminalReady + UrgencyTaskList.get(i).get("urgency_id"));
			UrgencyTaskList.get(i).put("ready_terminals", ready_terminals.toArray());
			Set<String> ok_terminals = RedisUtils
					.allSetData(Constant.TaskSentTerminalsOK + UrgencyTaskList.get(i).get("urgency_id"));
			UrgencyTaskList.get(i).put("ok_terminals", ok_terminals.toArray());
			Set<String> fail_terminals = RedisUtils
					.allSetData(Constant.TaskSentTerminalsFailed + UrgencyTaskList.get(i).get("urgency_id"));
			UrgencyTaskList.get(i).put("fail_terminals", fail_terminals.toArray());
		}

		return UrgencyTaskList;

	}

	private static JSONObject buildTerminalTaskData(TaskInfo theTaskInfo) {

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
		playDate.put("from", theTaskInfo.getStart_date());
		playDate.put("to", theTaskInfo.getEnd_date());
		JSONArray mediaUrl = new JSONArray();
		if (theTaskInfo.getTask_type() != 1) {// 文本
			for (int i = 2; i < content.size(); i++) {
				mediaUrl.add(":8080/cms/file/download/" + content.get(i));
			}
			if (theTaskInfo.getTask_type() == 2) {// 图片
				taskDataJson.put("imageUrls", mediaUrl);
			} else if (theTaskInfo.getTask_type() == 3) {// 音频
				taskDataJson.put("audioUrls", mediaUrl);
			} else if (theTaskInfo.getTask_type() == 4) {// 视频
				taskDataJson.put("videoUrls", mediaUrl);
			}
		}

		// String []weekDay = theTaskInfo.getPlay_weekdays().split(",");
		playDate.put("weekDay", JSONObject.parseArray(theTaskInfo.getPlay_weekdays()));
		taskDataJson.put("playDate", playDate);
		taskDataJson.put("playTime", JSONObject.parseArray(theTaskInfo.getPlay_periods()));
		return taskDataJson;
	}

	public Map<String, Object> doSendTask(JSONArray taskInfoList, List<Map<String, Object>> allTerminals,
			String action) {
		boolean sendResult = false;
		if (taskInfoList == null || allTerminals == null || taskInfoList.size() == 0 || allTerminals.size() == 0) {
			log.error("\ndo SendTask parameter is null!");
			return null;
		}
		JSONObject socketBody = null;
		TaskInfo tmpTaskInfo = null;
		String terminalIP = "";

		TerminalLog newTerminalLog = new TerminalLog();
		Map<String, Object> updateTask = new HashMap<String, Object>();
		// String terminalResp = null;
		SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();

		//JSONArray sequeceList = new JSONArray();
		Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
		Map<Integer, Object> sequeceTaskMap = new HashMap<Integer, Object>();
		int sequece = 0;

		for (int i = 0; i < allTerminals.size(); i++) {
			terminalIP = (String) allTerminals.get(i).get("ip");
			for (int j = 0; j < taskInfoList.size(); j++) {
				tmpTaskInfo = (TaskInfo) taskInfoList.get(j);
				if (tmpTaskInfo.getTask_type() == Constant.TextTask) {
					socketBody = buildTerminalTaskData(tmpTaskInfo);
					sequece = newSocketMsgHandler.sendMsgTo(terminalIP, cmds.TEXT_JOB, socketBody.toJSONString()); // 发送文本任务到终端设备
				} else if (tmpTaskInfo.getTask_type() == Constant.ImageTask) {
					socketBody = buildTerminalTaskData(tmpTaskInfo);
					sequece = newSocketMsgHandler.sendMsgTo(terminalIP, cmds.IMAGE_JOB, socketBody.toJSONString()); // 发送图片任务到终端设备
				} else if (tmpTaskInfo.getTask_type() == Constant.AudioTask) {
					socketBody = buildTerminalTaskData(tmpTaskInfo);
					sequece = newSocketMsgHandler.sendMsgTo(terminalIP, cmds.AUDIO_JOB, socketBody.toJSONString()); // 发送音频任务到终端设备
				} else if (tmpTaskInfo.getTask_type() == Constant.VideoTask) {
					socketBody = buildTerminalTaskData(tmpTaskInfo);
					sequece = newSocketMsgHandler.sendMsgTo(terminalIP, cmds.VIDEO_JOB, socketBody.toJSONString()); // 发送视频任务到终端设备
				} else {
					log.error("unkown task type! task type==" + tmpTaskInfo.getTask_type());
					return null;
				}
				sequeceMap.put(sequece, (String) allTerminals.get(i).get("terminal_id"));
				JSONObject tmpInfo = new JSONObject();
				tmpInfo.put("task_id", tmpTaskInfo.getTask_id());
				tmpInfo.put("terminal_id", (String) allTerminals.get(i).get("terminal_id"));
				tmpInfo.put("terminal_ip", terminalIP);
				tmpInfo.put("socketBody", socketBody);

				sequeceTaskMap.put(sequece, tmpInfo);
			}
		}

		Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
		Map<String, Object> result2Web = new HashMap<String, Object>();
		//int failedCnt = 0;
		int successCnt = 0;
		if (RespMap != null && RespMap.size() > 0) {
			for (Integer key : RespMap.keySet()) {// key 是sequece
				// System.out.println("Key = " + key);
				JSONObject tmpInfo = (JSONObject) sequeceTaskMap.get(key);
				socketBody = tmpInfo.getJSONObject("socketBody");
				String terminalRespStr = RespMap.get(key).getString("resp");
				JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
				int result = (int) terminalRespJson.get("status");
				String terminal_id = RespMap.get(key).getString("terminal_id");
				if (result == 1) {
					successCnt++;
					RedisUtils.addMember(Constant.TaskSentTerminalsOK + tmpTaskInfo.getTask_id(), terminal_id, 0);
					RedisUtils.removeMember(Constant.TaskTerminalReady + tmpTaskInfo.getTask_id(), terminal_id);
					RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + tmpTaskInfo.getTask_id(), terminal_id);

					newTerminalLog.setTerminal_id(tmpInfo.getString("terminal_id"));
					newTerminalLog.setTerminal_ip(tmpInfo.getString("terminal_ip"));
					newTerminalLog.setAction(action);
					newTerminalLog.setContent(tmpInfo.getJSONObject("socketBody").toJSONString());
					newTerminalLog.setResult("ok");
					es.insert("TerminalLog", "insert", newTerminalLog);
				} else {
					//failedCnt++;
					RedisUtils.addMember(Constant.TaskSentTerminalsFailed + tmpTaskInfo.getTask_id(), terminal_id, 0);
					RedisUtils.removeMember(Constant.TaskTerminalReady + tmpTaskInfo.getTask_id(), terminal_id);
					RedisUtils.removeMember(Constant.TaskSentTerminalsOK + tmpTaskInfo.getTask_id(), terminal_id);
				}
			}
			result2Web.put("failedCnt", sequeceMap.size());
		}

		return result2Web;
	}

	@RequestMapping(value = "/urgencyList", method = RequestMethod.POST)
	@ResponseBody
	public void getUrgencyTaskList(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		// int result = 1;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			Map conMap = new HashMap<String, Object>();
			
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
			
			conMap.put("startrom", (page - 1) * pagesize);
			conMap.put("pagesize", pagesize);
			List<Map<String, Object>> urgencyList = ms.selectList("UrgencyTask", "selectByConditionWithPage", conMap);
			if (urgencyList != null && urgencyList.size() > 0) {
				urgencyList = Convert.SortDataListId(urgencyList, page, pagesize);
				urgencyList = convertJsonStr2Array(urgencyList);
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", urgencyList);
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

	// 根据id获取单个任务信息
	@RequestMapping(value = "/getUrgencyTaskById", method = RequestMethod.POST)
	@ResponseBody
	public void getUrgencyTaskById(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String urgency_id = jsonBody.getString("urgency_id");
			Map<String, Object> conMap = new HashMap<String, Object>();
			conMap.put("urgency_id", urgency_id);
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
			
			List<Map<String, Object>> urgencyList = ms.selectList("UrgencyTask", "selectByPK", conMap);
			urgencyList = convertJsonStr2Array(urgencyList);
			if (urgencyList != null && urgencyList.size() > 0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", urgencyList);
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
			//BootSetting newBootSet = new BootSetting();
			String urgency_id = jsonBody.getString("urgency_id");
			JSONArray ready_terminals = jsonBody.getJSONArray("ready_terminals");// ip
			JSONArray failed_terminals = jsonBody.getJSONArray("fail_terminals");// ip
			int hasSettPermission = 0;
			if(adminUser.getIs_supper()==0) {
				hasSettPermission=checkActionPermission(adminUser.getUid(),urgency_id);
			}else {
				hasSettPermission = 1;
			}
			
			if(hasSettPermission==1) {
				Set<String> ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + urgency_id);
				for(String ok_terminal:ok_terminals) {
					if(ready_terminals!=null) {
						ready_terminals.remove(ok_terminal);
					}
					if(failed_terminals!=null) {
						failed_terminals.remove(ok_terminal);
					}
				}
				
				if (ready_terminals != null) {
					RedisUtils.del(Constant.TaskTerminalReady + urgency_id);// 移除然后在新增
					for (int i = 0; i < ready_terminals.size(); i++) {
						RedisUtils.addMember(Constant.TaskTerminalReady + urgency_id, ready_terminals.getString(i), 0);
						result = 1;
					}
				}
				if (failed_terminals != null) {
					// RedisUtils.del(Constant.TaskSentTerminalsFailed + setting_id);//移除然后在新增
					for (int i = 0; i < failed_terminals.size(); i++) {
						RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + urgency_id,
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

	// 新增紧急任务
	@RequestMapping(value = "/addOrUpdate", method = RequestMethod.POST)
	@ResponseBody
	public void addExamInfo(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String urgency_id = jsonBody.getString("urgency_id");
			String title = jsonBody.getString("title");
			String content = jsonBody.getString("content");
			// JSONArray terminalIds = jsonBody.getJSONArray("terminal_ids");
			//int status = jsonBody.getIntValue("status");
			UrgencyTask newUrgencyInfo = new UrgencyTask();
			newUrgencyInfo.setContent(content);
			newUrgencyInfo.setTitle(title);
			// newUrgencyInfo.setTerminals(terminalIds.toJSONString());
			if (urgency_id != null && urgency_id.length() > 0) {// 如果参数带EXAM_ID 则为修改
				newUrgencyInfo.setUrgency_id(urgency_id);
				Set<String> sendTerminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + urgency_id);
				if(sendTerminals==null||sendTerminals.size()==0) {
					if(adminUser.getIs_supper()==0) {
						int hasSettPermission=checkActionPermission(adminUser.getUid(),urgency_id);
						if(hasSettPermission==1) {
							result = es.update("UrgencyTask", "update", newUrgencyInfo);
						}
					}else {
						result = es.update("UrgencyTask", "update", newUrgencyInfo);
					}
				}
			} else {// 如果参数不带exam_id，则添加
				newUrgencyInfo.setUrgency_id("urg-" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(8));// 生成新的id
				newUrgencyInfo.setStart_time(new Date());
				newUrgencyInfo.setCreate_time(new Date());
				newUrgencyInfo.setCreator(adminUser.getUsername());
				newUrgencyInfo.setCreator_uid(adminUser.getUid());
				result = es.insert("UrgencyTask", "insert", newUrgencyInfo);
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

	// 删除紧急任务
	@RequestMapping(value = "/delTask", method = RequestMethod.POST)
	@ResponseBody
	public void delUrgencyTask(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			JSONArray urgency_ids = jsonBody.getJSONArray("urgency_ids");
			if(adminUser.getIs_supper()==0 ) {
				if(urgency_ids!=null) {
					for(int i=0;i<urgency_ids.size();i++) {
						int hasSettPermission=checkActionPermission(adminUser.getUid(),urgency_ids.getString(i));
						if(hasSettPermission==0) {
							respJson.put("status", Constant.FAILED);
							respJson.put("msg", Constant.ParemeterErr);
							HttpIO.writeResp(response, respJson);
							return;
						}
					}
				}
			}
			
			Map<String, Object> condMap = new HashMap<String, Object>();
			if (urgency_ids != null && urgency_ids.size() > 0) {
				for (int i = 0; i < urgency_ids.size(); i++) {
					Set<String> tmpTerminalIPs = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + urgency_ids.get(i));
					if (tmpTerminalIPs == null || tmpTerminalIPs.size() == 0) {
						condMap.put("urgency_id", urgency_ids.get(i));
						result = ms.execute("UrgencyTask", "delete", condMap);
						if (result > 0) {
							RedisUtils.del(Constant.TaskSentTerminalsOK + urgency_ids.get(i));
							RedisUtils.del(Constant.TaskTerminalReady + urgency_ids.get(i));
							RedisUtils.del(Constant.TaskSentTerminalsFailed + urgency_ids.get(i));
						}
					}
				}
			}

			if (result >= 1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", "删除成功!");
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

	// 停止紧急任务
	@RequestMapping(value = "/stopUrgencyTask", method = RequestMethod.POST)
	@ResponseBody
	public void stopUrgencyTask(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String urgency_id = jsonBody.getString("urgency_id");
			
			if(adminUser.getIs_supper()==0) {
				int hasSettPermission=checkActionPermission(adminUser.getUid(),urgency_id);
				if(hasSettPermission==0) {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", Constant.ParemeterErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			
			// String terminal_type = jsonBody.getString("terminal_type");//ready
			// 为设置的就绪的，ok为发送成功的,fail为发送失败的
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			Map<String, Object> conMap = new HashMap<String, Object>();
			SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();

			conMap.put("urgency_id", urgency_id);
			List<Map<String, Object>> theSetting = ms.selectList("UrgencyTask", "selectByPK", conMap);

			if (theSetting != null && theSetting.size() == 1) {
				if (terminal_ids == null) {
					//ok_terminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + urgency_id);
					terminal_ids = new JSONArray();
					Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskSentTerminalsOK + urgency_id);
					for(String tmpTerminalId:tmpOKterminals) {
						terminal_ids.add(tmpTerminalId);
					}
				} 
				JSONObject socketBody = new JSONObject();
				//JSONArray sequeceList = new JSONArray();
				Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
				JSONArray TaskIds = new JSONArray();
				
				conMap.clear();
				conMap.put("terminal_ids", terminal_ids);
				List<Map<String, Object>>  terminalList = ms.selectList("Terminal", "selectByIds", conMap);
				if(terminalList!=null && terminalList.size()>0) {
					for (int n=0; n < terminalList.size(); n++) {
						socketBody.clear();
						TaskIds.clear();
						TaskIds.add(urgency_id);
						socketBody.put("deleteMode", 1);// 删除指定的任务ＩＤ
						socketBody.put("taskIds", TaskIds);
						int sequece = newSocketMsgHandler.sendMsgTo((String) terminalList.get(n).get("ip"), cmds.DELETE_JOBS,socketBody.toJSONString());
						 sequeceMap.put(sequece, (String) terminalList.get(n).get("terminal_id"));
					}
				}
			

				Map<Integer, JSONObject> RespMap = newSocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);
				Map<String, Object> result2Web = new HashMap<String, Object>();
				//int failedCnt = 0;
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
							RedisUtils.removeMember(Constant.TaskSentTerminalsOK + urgency_id, terminal_id);
							RedisUtils.removeMember(Constant.TaskSentTerminalsFailed + urgency_id, terminal_id);
							RedisUtils.addMember(Constant.TaskTerminalReady + urgency_id, terminal_id, 0);
						}
					}
				} 
				result2Web.put("failedCnt", sequeceMap.size());
				// 下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionSend);
				JSONObject content = new JSONObject();
				content.put("action_name", "停用紧急任务");
				content.put("urgency_id", urgency_id);
				actionLog.put("action_content", content.toJSONString());
				es.insert("UserLog", "insert", actionLog);

				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", result2Web);
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

	// 启动或者停止紧急预案
	@RequestMapping(value = "/startUrgencyTask", method = RequestMethod.POST)
	@ResponseBody
	public void startUrgencyTask(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			JSONArray urgency_ids = jsonBody.getJSONArray("urgency_ids");
			String terminal_type = jsonBody.getString("terminal_type");
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			// String action = jsonBody.getString("action");
			if(adminUser.getIs_supper()==0 ) {
				if(urgency_ids!=null) {
					for(int i=0;i<urgency_ids.size();i++) {
						int hasSettPermission=checkActionPermission(adminUser.getUid(),urgency_ids.getString(i));
						if(hasSettPermission==0) {
							respJson.put("status", Constant.FAILED);
							respJson.put("msg", Constant.ParemeterErr);
							HttpIO.writeResp(response, respJson);
							return;
						}
					}
				}
			}
			
			Map<String, Object> condMap = new HashMap<String, Object>();
			JSONArray TaskNames = new JSONArray();
			if(terminal_ids==null) {
				terminal_ids = new JSONArray();
			}
			for (int i = 0; i < urgency_ids.size(); i++) {
				condMap.put("urgency_id", urgency_ids.get(i));
				List<Map<String, Object>> urgencyList = ms.selectList("UrgencyTask", "selectByPK", condMap);
				if (urgencyList != null && urgencyList.size() > 0) {
					// UrgencyTask tmpUrgencyTask = (UrgencyTask) urgencyList.get(0);

					UrgencyTask tmpUrgencyTask = JSON.parseObject(JSON.toJSONString(urgencyList.get(0)),
							UrgencyTask.class);

					TaskNames.add(tmpUrgencyTask.getTitle());

					TaskInfo urgencyTaskInfo = new TaskInfo();
					urgencyTaskInfo.setTask_id(tmpUrgencyTask.getUrgency_id());
					urgencyTaskInfo.setTask_name(tmpUrgencyTask.getTitle());

					JSONArray allContent = new JSONArray();
					allContent.add(tmpUrgencyTask.getTitle());
					allContent.add(tmpUrgencyTask.getContent());
					allContent.add("999");
					urgencyTaskInfo.setContent(allContent.toString());
					urgencyTaskInfo.setCreate_user(adminUser.getUsername());
					urgencyTaskInfo.setCreate_time(new Date());
					urgencyTaskInfo.setPlan_type("urgency");
					urgencyTaskInfo.setPlay_mode(1);// 每天循环
					urgencyTaskInfo.setPlay_periods("[{\"playcount\":0,\"from\":\"00:00\",\"to\":\"23:59\"}]");
					urgencyTaskInfo.setTask_type(4);//
					urgencyTaskInfo.setPriority(999); // 紧急任务专用设置

					Date today = new Date(); // 获取当前的系统时间。
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd"); // 使用了默认的格式创建了一个日期格式化对象。
					dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
					String start_date = dateFormat.format(today); // 可以把日期转换转指定格式的字符串

					urgencyTaskInfo.setStart_date(start_date);
					urgencyTaskInfo.setEnd_date(dateFormat.format(TimeUtil.yearAddNum(today, 1)));
					urgencyTaskInfo.setUpdate_time(today);

					JSONArray taskInfoList = new JSONArray();
					taskInfoList.add(urgencyTaskInfo);

//					JSONObject socketBody = new JSONObject();
//					JSONArray TaskIds = new JSONArray();
//					SocketMsgHandler newSocketMsgHandler = new SocketMsgHandler();
//					JSONArray sequeceList = new JSONArray();
					
					if(terminal_type!=null && terminal_type.equals("ready")) {
						//terminal_ids = RedisUtils.allSetData(Constant.TaskTerminalReady + setting_id);
						Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskTerminalReady + tmpUrgencyTask.getUrgency_id());
						for(String tmpTerminalId:tmpOKterminals) {
							terminal_ids.remove(tmpTerminalId);
							terminal_ids.add(tmpTerminalId);
						}
					}else if(terminal_type!=null && terminal_type.equals("fail")) {
						//terminal_ids = RedisUtils.allSetData(Constant.TaskSentTerminalsFailed + setting_id);
						Set<String> tmpOKterminals = RedisUtils.allSetData(Constant.TaskSentTerminalsFailed + tmpUrgencyTask.getUrgency_id());
						for(String tmpTerminalId:tmpOKterminals) {
							terminal_ids.remove(tmpTerminalId);
							terminal_ids.add(tmpTerminalId);
						}
					}
					
					condMap.clear();
					condMap.put("terminal_ids", terminal_ids);
					List<Map<String, Object>>  allTerminals = ms.selectList("Terminal", "selectByIds", condMap);
					Map<String, Object> sendResult = doSendTask(taskInfoList, allTerminals, "urgency_plan");
					JSONArray resultList = new JSONArray();
					resultList.add(sendResult);
					respJson.put("status", Constant.SUCCESS);
					respJson.put("result", resultList);
					tmpUrgencyTask.setStart_time(new Date());
					es.update("UrgencyTask", "update", tmpUrgencyTask);
					taskInfoList.clear();
					result = 1;
				}
			}
			if (result >= 1) {
				// 下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionStop);
				JSONObject content = new JSONObject();
				content.put("action_name", "启动紧急任务");
				content.put("urgency_ids", urgency_ids);
				content.put("TaskNames", TaskNames);
				actionLog.put("action_content", content.toJSONString());
				es.insert("UserLog", "insert", actionLog);

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

}