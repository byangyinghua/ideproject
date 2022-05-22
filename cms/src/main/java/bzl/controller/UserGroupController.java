package bzl.controller;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

import bzl.common.Constant;
import bzl.common.MemoryCache;
import bzl.common.SesCheck;
import bzl.entity.LoginLog;
import bzl.entity.ShieldTask;
import bzl.entity.User;
import bzl.entity.UserGroup;
import bzl.service.EntityService;
import bzl.service.MapService;
import bzl.service.impl.EntityServiceImpl;
import bzl.service.impl.MapServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import utils.Convert;
import utils.EncryptionUtil;
import utils.HttpIO;
import utils.NetUtil;
import utils.RedisUtils;
import sun.rmi.log.LogHandler;

/*管理员账号操作controller*/

@Controller
@RequestMapping("/usergroup")
public class UserGroupController {
	// private static Logger logger = Logger.getLogger(UserController.class);
	Logger log = Logger.getLogger(LogHandler.class);
//	@Autowired
//	private MapService ms;
//	@Autowired
//	private EntityService es;
	
	private static MapService ms = new MapServiceImpl();
	private static EntityService es = new EntityServiceImpl();
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
	
	public static boolean checkHasPermission(String userName,String uid,String terminal_id) {
		Map<String,Object> condMap = new HashMap<String,Object>();
		boolean result=false;
		if(userName!=null) {
			User us = new User();
			us.setUsername(userName);
			//List<Map<String, Object>> userList = ms.selectList("UserGroup", "selectByCondition", condMap);
			List<User> list = es.select("User", "selectByCondition", us);
			if (list != null && list.size() == 1) {
				uid = list.get(0).getUid();
			}
		}else if(userName.toLowerCase().equals("admin")) {
			System.out.println("checkHasPermission tru the device has newHelpInfo!terminal_id=" + terminal_id + ",userName="+userName);
			return true;
		}
		condMap.put("uid", uid);
		condMap.put("sort", "create_time asc");
		Set<String> retTermialGrps =  new HashSet<>();
		List<Map<String, Object>> userGroupList = ms.selectList("UserGroup", "selectByCondition", condMap);
		if(userGroupList!=null) {
			System.out.println("checkHasPermission the device has newHelpInfo!terminal_id=" + terminal_id);
			condMap.clear();
			condMap.put("terminal_id", terminal_id);
			List<Map<String,Object>> terminalList =ms.selectList("Terminal", "selectByCondition", condMap);
			if(terminalList!=null) {
				String terminalGrp = (String) terminalList.get(0).get("gids");
				if(terminalGrp!=null && terminalGrp.length() >0) {
					String[] tmpGroupList = terminalGrp.split(";");
					String allUserJoinGrp = "";
					for(int i=0;i<userGroupList.size();i++) {
						String terminalGroups = (String) userGroupList.get(i).get("terminal_groups");
						if(terminalGroups!=null && terminalGroups.length() >0) {
							allUserJoinGrp +=terminalGroups;
						}
					}
					for(int n=0;n<tmpGroupList.length;n++) {
						if(allUserJoinGrp.contains(tmpGroupList[n])) {
							result = true;
							break;
						}
					}
					
				}
			}
		}
		return result;
	}
	
	public static Set<String> getGroupListByUser(String uid) {
		Map<String,Object> condMap = new HashMap<String,Object>();
		condMap.put("uid", uid);
		condMap.put("sort", "create_time asc");
		Set<String> retTermialGrps =  new HashSet<>();
		List<Map<String, Object>> userGroupList = ms.selectList("UserGroup", "selectByCondition", condMap);
		if(userGroupList!=null) {
			for(int i=0;i<userGroupList.size();i++) {
				String terminalGroups = (String) userGroupList.get(i).get("terminal_groups");
				if(terminalGroups!=null && terminalGroups.length() >0) {
					String[] tmpList = terminalGroups.split(";");
					retTermialGrps.addAll(new HashSet<>(Arrays.asList(tmpList)));
				}
			}
		}
		return retTermialGrps;
	}
	
	public static Set<String> getSameGroupUserList(String uid) {
		Map<String,Object> condMap = new HashMap<String,Object>();
		condMap.put("uid", uid);
		condMap.put("sort", "create_time asc");
		Set<String> allGroupMemebers =  new HashSet<>();
		List<Map<String, Object>> userGroupList = ms.selectList("UserGroup", "selectByCondition", condMap);
		if(userGroupList!=null) {
			for(int i=0;i<userGroupList.size();i++) {
				String terminalGroups = (String) userGroupList.get(i).get("user_members");
				if(terminalGroups!=null && terminalGroups.length() >0) {
					String[] tmpList = terminalGroups.split(";");
					allGroupMemebers.addAll(new HashSet<>(Arrays.asList(tmpList)));
				}
			}
		}
		return allGroupMemebers;
	}
	
	
	private String deleteDupOne(String str,String sep) {
		String retStr = "";
		if(str!=null && str.length()==0) {
			String[] strList = str.split(";");
			if(strList!=null) {
				Map<String,String> tmpMap = new HashMap<String,String>();
				for(int i=0;i<strList.length;i++) {
					tmpMap.put(strList[i], "");
				}
				Set<String> theKeySet = tmpMap.keySet();
				for(String key:theKeySet) {
					if(retStr.length()==0) {
						retStr+=key;
					}else {
						retStr+= sep + key;
					}
				}
			}
		}
		if(retStr.length()==0) {
			retStr = str;
		}
		return retStr;
	}
	
    /* getUserGroupList get user group list */
	
	@RequestMapping(value="/getUserGroupList",method=RequestMethod.POST)
	@ResponseBody
	public void getUserGroupList(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		User adminUser = SesCheck.getUserBySession(request,es, false);
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			
			Map<String,Object> condMap = new HashMap<String,Object>();
			List<Map<String,Object>> userGroupList=null;
			if (getTotal != null) {
				if(adminUser.getIs_supper()==0) {
					condMap.put("uid", adminUser.getUid());
				}
				List<Map<String, Object>> totalList = ms.selectList("UserGroup", "selectCountByCondition", condMap);
				if (totalList != null && totalList.size() == 1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}
			condMap.put("startrom", (page - 1) * pagesize);
			condMap.put("pagesize", pagesize);
			condMap.put("sort", "update_time desc");
			userGroupList= ms.selectList("UserGroup", "selectByConditionWithPage", condMap);
			if(userGroupList != null && userGroupList.size() > 0) {
				respJson.put("status",Constant.SUCCESS);
				respJson.put("result", userGroupList);
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
	
	//create or modify user group
	@RequestMapping(value="/addOrUpdateUserGroup",method=RequestMethod.POST)
	@ResponseBody
	public void addOrUpdateUserGroup(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		HttpSession session = request.getSession();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, true);
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String userGroupId = jsonBody.getString("group_id");
			String userGroupName = jsonBody.getString("group_name");
			JSONArray userMembers = jsonBody.getJSONArray("user_members");
			JSONArray terminalGrpList = jsonBody.getJSONArray("terminal_groups");
			if(userGroupName!=null) {
				UserGroup userGroupInfo = new UserGroup();
				userGroupInfo.setGroup_name(userGroupName);
				if(userMembers!=null) {
					String tmpMembers = "";
					for(int i=0;i<userMembers.size();i++) {
						if(i==0) {
							tmpMembers +=userMembers.getString(i);
						}else {
							tmpMembers +=";" + userMembers.getString(i);
						}
					}
					userGroupInfo.setUser_members(tmpMembers);
				}
				
				if(terminalGrpList!=null) {
					String tmpTerminalGrps = "";
					for(int i=0;i<terminalGrpList.size();i++) {
						if(i==0) {
							tmpTerminalGrps +=terminalGrpList.getString(i);
						}else {
							tmpTerminalGrps +=";" + terminalGrpList.getString(i);
						}
					}
					userGroupInfo.setTerminal_groups(tmpTerminalGrps);
				}
				if(userGroupId==null) {
					userGroupId = "usergrp" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(5);
					userGroupInfo.setGroup_id(userGroupId);
					userGroupInfo.setCreate_time(new Date());
					userGroupInfo.setUpdate_time(new Date());
					result = es.insert("UserGroup", "insert", userGroupInfo);
				}else {
					userGroupInfo.setGroup_id(userGroupId);
					userGroupInfo.setGroup_name(userGroupName);
					result = es.update("UserGroup", "update", userGroupInfo);
				}
				
				if(result >0) {
					respJson.put("status", Constant.SUCCESS);
					respJson.put("msg", Constant.SuccessMsg);	
				}else {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", Constant.FailedMsg);
				}
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
	
	@RequestMapping(value="/delUserGroups",method=RequestMethod.POST)
	@ResponseBody
	public void delUserGroups(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
	    HttpSession session = request.getSession();
		User adminUser = SesCheck.getUserBySession(request,es, true);
		int result = 0;
		if(adminUser != null && adminUser.getIs_supper()==1) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			JSONArray userGroupIds = jsonBody.getJSONArray("group_ids");
			if(userGroupIds!=null) {
				Map<String,Object> condMap = new HashMap<String,Object>();
				for(int i=0;i<userGroupIds.size();i++) {
					condMap.put("group_id", userGroupIds.get(i));
					result += ms.execute("UserGroup", "delete", condMap);
				}
			}
			if(result >0) {
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
	
	@RequestMapping(value="/addOrRemoveUser",method=RequestMethod.POST)
	@ResponseBody
	public void addOrRemoveUser(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
	    HttpSession session = request.getSession();
		User adminUser = SesCheck.getUserBySession(request,es, true);
		int result = 0;
		if(adminUser != null && adminUser.getIs_supper()==1) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String userGroupId = jsonBody.getString("group_id");
			JSONArray uidList = jsonBody.getJSONArray("uids");
			String action = jsonBody.getString("action");
			Map<String,Object> condMap = new HashMap<String,Object>();
			if(userGroupId!=null && uidList!=null) {
				List<Map<String, Object>> userGroupList = ms.selectList("UserGroup", "selectByPK", condMap);
				if(userGroupList!=null && userGroupList.size()==1) {
					Map<String, Object> groupInfo = userGroupList.get(0);
					String userMembers = (String) groupInfo.get("user_members");
					if(action.equals("add")) {
						for(int i=0;i<uidList.size();i++) {
							if(userMembers!=null && userMembers.length() >0) {
								userMembers += ";"+uidList.getString(i);
							}else {
								userMembers +=uidList.getString(i);
							}
						}
						userMembers = deleteDupOne(userMembers,";");
						groupInfo.put("user_members", userMembers);
					}else if(action.equals("remove")) {
						for(int i=0;i<uidList.size();i++) {
							if(userMembers!=null && userMembers.length() >0) {
								userMembers = userMembers.replace(uidList.getString(i) + ";", "").replace(";"+uidList.getString(i), "").replace(uidList.getString(i), "");
							}
						}
						userMembers = deleteDupOne(userMembers,";");
						groupInfo.put("user_members", userMembers);
					}
					result = es.update("UserGroup", "update", groupInfo);
				}
			}
			if(result >0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", Constant.SuccessMsg);
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("result", Constant.FailedMsg);
			}
		}else if(adminUser != null) {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);	
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.MsgNotLogin);	
		}
	    HttpIO.writeResp(response, respJson);
	}
	
	@RequestMapping(value="/addOrRemoveTerminalGrp",method=RequestMethod.POST)
	@ResponseBody
	public void addOrRemoveTerminalGrp(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
	    HttpSession session = request.getSession();
		User adminUser = SesCheck.getUserBySession(request,es, true);
		int result = 0;
		if(adminUser != null && adminUser.getIs_supper()==1) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String userGroupId = jsonBody.getString("group_id");
			JSONArray terminalGroupIds = jsonBody.getJSONArray("terminalGroupIds");
			String action = jsonBody.getString("action");
			Map<String,Object> condMap = new HashMap<String,Object>();
			if(userGroupId!=null && terminalGroupIds!=null) {
				List<Map<String, Object>> userGroupList = ms.selectList("UserGroup", "selectByPK", condMap);
				if(userGroupList!=null && userGroupList.size()==1) {
					Map<String, Object> groupInfo = userGroupList.get(0);
					String userMembers = (String) groupInfo.get("terminal_groups");
					if(action.equals("add")) {
						for(int i=0;i<terminalGroupIds.size();i++) {
							if(userMembers!=null && userMembers.length() >0) {
								userMembers += ";"+terminalGroupIds.getString(i);
							}else {
								userMembers +=terminalGroupIds.getString(i);
							}
						}
						userMembers = deleteDupOne(userMembers,";");
						groupInfo.put("terminal_groups", userMembers);
					}else if(action.equals("remove")) {
						for(int i=0;i<terminalGroupIds.size();i++) {
							if(userMembers!=null && userMembers.length() >0) {
								userMembers = userMembers.replace(terminalGroupIds.getString(i) + ";", "").replace(";"+terminalGroupIds.getString(i), "").replace(terminalGroupIds.getString(i), "");
							}
						}
						userMembers = deleteDupOne(userMembers,";");
						groupInfo.put("terminal_groups", userMembers);
					}
					result = es.update("UserGroup", "update", groupInfo);
				}
			}
			if(result >0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", Constant.SuccessMsg);
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("result", Constant.FailedMsg);
			}
		}else if(adminUser != null) {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);	
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.MsgNotLogin);	
		}
	    HttpIO.writeResp(response, respJson);
	}
	
}
