package bzl.controller;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bzl.common.Constant;
import bzl.common.SesCheck;
import bzl.entity.User;
import bzl.entity.UserLog;
import bzl.service.EntityService;
import bzl.service.MapService;
import utils.HttpIO;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


//web页面的用户操作日志　
//用户的登录日志
//终端设备的日常日志
@Controller
@RequestMapping("/log")
public class LogController {
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	
	/*
	 * 用户操作日志定义:
	 * 用户操作日志用来记录用户的关键操作，主要分为一下类型:
	 * "add"增加，"del"删除,"modify"改动,"upload"上传,"sendtask"下发任务,"stoptask"停止任务
	 */
	
	//获取用户操作日志
	@RequestMapping(value="/userlog",method=RequestMethod.POST)
	@ResponseBody
	public void getTerminalBottSetting(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es,true); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			String username = jsonBody.getString("username");
			JSONArray period = jsonBody.getJSONArray("period");
			
			Map<String,Object> condMap = new HashMap<String,Object>();
			if(adminUser.getIs_supper()==0) {
				Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(adminUser.getUid());
				if(tmpUserSet!=null && tmpUserSet.size() >0) {
					condMap.put("uids", tmpUserSet.toArray());
				}else {
					tmpUserSet = new HashSet<String>();
					tmpUserSet.add(RandomStringUtils.randomAlphanumeric(5));
					condMap.put("uids", tmpUserSet.toArray());
				}
			}
			
			if(getTotal !=null) {
				List<Map<String,Object>> totalList= ms.selectList("UserLog", "selectCountByCondition", condMap);
				if(totalList !=null && totalList.size()==1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}
			condMap.put("startrom", (page -1)*pagesize);
			condMap.put("pagesize", pagesize);
			condMap.put("sort", "create_time desc"); //默认最新记录排列在前面
			if(username != null && username.length() > 0) {
				condMap.put("username", username);
			}
			
			if(period != null && period.size()==2) {
				condMap.put("start_time", period.get(0));
				condMap.put("end_time", period.get(1));
			}
			
			List<Map<String,Object>> userLogList = ms.selectList("UserLog", "selectByConditionWithPage", condMap);
			if(userLogList !=null && userLogList.size() >0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", userLogList);
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
	
	
	//获取用户登录日志
	@RequestMapping(value="/loginlog",method=RequestMethod.POST)
	@ResponseBody
	public void getAdminUerLog(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 1;
		User adminUser = SesCheck.getUserBySession(request,es, true); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String username = jsonBody.getString("username");
			JSONArray period = jsonBody.getJSONArray("period");
			String getTotal = jsonBody.getString("getTotal");
			
			Map<String,Object> condMap = new HashMap<String,Object>();
			if(adminUser.getIs_supper()==0) {
				Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(adminUser.getUid());
				if(tmpUserSet!=null && tmpUserSet.size() >0) {
					condMap.put("uids", tmpUserSet.toArray());
				}else {
					tmpUserSet = new HashSet<String>();
					tmpUserSet.add(RandomStringUtils.randomAlphanumeric(5));
					condMap.put("uids", tmpUserSet.toArray());
				}
			}
			
			if(getTotal !=null) {
				List<Map<String,Object>> totalList= ms.selectList("LoginLog", "selectCountByCondition", condMap);
				if(totalList !=null && totalList.size()==1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}
			condMap.put("startrom", (page -1)*pagesize);
			condMap.put("pagesize", pagesize);
			condMap.put("sort", "login_time desc"); //默认最新记录排列在前面
			if(username != null && username.length() > 0) {
				condMap.put("username", username);
			}
			
			if(period != null && period.size()==2) {
				condMap.put("start_time", period.get(0));
				condMap.put("end_time", period.get(1));
			}
			
			List<Map<String,Object>> userLoginLogList = ms.selectList("LoginLog", "selectByConditionWithPage", condMap);
			if(userLoginLogList !=null && userLoginLogList.size() >0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", userLoginLogList);
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
	
	//与设备相关的日志信息
	@RequestMapping(value="/terminal_log",method=RequestMethod.POST)
	@ResponseBody
	public void getTerminalLog(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 1;
		User adminUser = SesCheck.getUserBySession(request,es, true); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String terminal_id = jsonBody.getString("terminal_id");
			JSONArray period = jsonBody.getJSONArray("period");
			String action = jsonBody.getString("action");
			String getTotal = jsonBody.getString("getTotal");
			
			Map<String,Object> condMap = new HashMap<String,Object>();
			Set<String> terminalGids =null;
			if(adminUser.getIs_supper()==0) {
			   terminalGids = UserGroupController.getGroupListByUser(adminUser.getUid());
			}
			
			if(getTotal !=null) {
				condMap.put("terminal_id", terminal_id);
				condMap.put("action", action);
				List<Map<String,Object>> totalList =null;
				if(adminUser.getIs_supper()==0) {
					if(terminalGids!=null) {
						condMap.put("gids", terminalGids.toArray());
						totalList= ms.selectList("TerminalLog","countByTerminalGid", condMap);
					}
				}else {
					totalList= ms.selectList("TerminalLog","selectCountByCondition", condMap);
				}
				if(totalList !=null && totalList.size()==1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}
			
			condMap.put("startrom", (page -1)*pagesize);
			condMap.put("pagesize", pagesize);
			if(terminal_id != null && terminal_id.length() > 0) {
				condMap.put("terminal_id", terminal_id);
			}
			
			if(period != null && period.size()==2) {
				condMap.put("start_time", period.get(0));
				condMap.put("end_time", period.get(1));
			}
			condMap.put("action", "plan");//只读取任务记录
			
			condMap.put("sort", "create_time desc"); //默认最新记录排列在前面
			
			List<Map<String,Object>> terminalLogList =null;
			if(adminUser.getIs_supper()==1) {
				terminalLogList  = ms.selectList("TerminalLog", "selectByConditionWithPage", condMap);
			}else {
				terminalGids = UserGroupController.getGroupListByUser(adminUser.getUid());
				if(terminalGids!=null) {
					condMap.put("gids", terminalGids.toArray());
					terminalLogList  = ms.selectList("TerminalLog", "selectByTerminalGidByPage", condMap);
				}
			}
			if(terminalLogList !=null && terminalLogList.size() >0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", terminalLogList);
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
	
	

}
