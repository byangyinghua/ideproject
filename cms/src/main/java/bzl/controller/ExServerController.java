package bzl.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import bzl.common.Constant;
import bzl.common.SesCheck;
import bzl.entity.BootSetting;
import bzl.entity.Camera;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import sun.rmi.log.LogHandler;
import utils.Convert;
import utils.HttpIO;

@Controller
@RequestMapping("/extendserver")
public class ExServerController {
	// private static Logger logger = Logger.getLogger(UserController.class);
	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	//获取外接摄像头列表
	@RequestMapping(value = "/list",method=RequestMethod.POST)
	@ResponseBody
	public void getExtendServerList(HttpServletRequest request,HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser!=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			Map<String,Object> conMap = new HashMap<String,Object>();
			conMap.put("startrom", (page-1)*pagesize);
			conMap.put("pagesize",pagesize);
			
			if(getTotal !=null) {
				List<Map<String,Object>> totalList= ms.selectList("ExtendServer", "selectCountByCondition", conMap);
				if(totalList !=null && totalList.size()==1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}

			List<Map<String,Object>> serverList = ms.selectList("ExtendServer", "selectByConditionWithPage", conMap);
			if(serverList != null && serverList.size() >0) {
				serverList = Convert.SortDataListId(serverList, page, pagesize);
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result",serverList);
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
	
	//增加后者修改第三方服务器配置
	@RequestMapping(value = "/addOrUpdate",method=RequestMethod.POST)
	@ResponseBody
	public void addOrUpdate(HttpServletRequest request,HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false);
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String server_id = jsonBody.getString("server_id");
			String server_ip = jsonBody.getString("server_ip");
			String server_type = jsonBody.getString("server_type");
			String server_name = jsonBody.getString("server_name");
			String login_name = jsonBody.getString("login_name");
			String login_pwd =  jsonBody.getString("login_pwd");

			
			Map<String,Object> newServerInfo = new HashMap<String,Object>();
			//Camera newCamera = new Camera();
			newServerInfo.put("server_ip", server_ip);
			newServerInfo.put("server_type", server_type);
			newServerInfo.put("server_name", server_name);
			newServerInfo.put("login_name", login_name);
			newServerInfo.put("login_pwd", login_pwd);
			
			if(server_id==null||server_id.length()==0) {
				server_id = "server" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(4);
				newServerInfo.put("server_id",server_id);
				result = ms.execute("ExtendServer", "insert", newServerInfo);
			}else {
				System.out.println("newServerInfo:" + newServerInfo.toString());
				newServerInfo.put("server_id",server_id);
				result = ms.execute("ExtendServer", "update", newServerInfo);
			}
			if(result==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg",Constant.SuccessMsg);
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg",Constant.FailedMsg);
			}
		}else {
			respJson.put("result", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		 HttpIO.writeResp(response, respJson);
	}
	
	
	//增加后者修改第三方服务器配置
		@RequestMapping(value = "/delete",method=RequestMethod.POST)
		@ResponseBody
		public void deleteServer(HttpServletRequest request,HttpServletResponse response) {
			JSONObject respJson = new JSONObject();
			int result = 0;
			User adminUser = SesCheck.getUserBySession(request,es, false);
			if(adminUser !=null) {
				String jsonBodyStr = HttpIO.getBody(request);
				JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
				JSONArray server_ips = jsonBody.getJSONArray("server_ips");
				
				if(server_ips!=null && server_ips.size() >0) {
					Map<String,Object> cond = new HashMap<String,Object>();
					cond.put("server_ips", server_ips);
					
					result = ms.execute("ExtendServer", "deleteBat", cond);
				}
				
				if(result ==1) {
					respJson.put("status", Constant.SUCCESS);
					respJson.put("msg",Constant.SuccessMsg);
				}else {
					respJson.put("status", Constant.FAILED);
					respJson.put("msg",Constant.FailedMsg);
				}
			}else {
				respJson.put("result", Constant.UserNotLogin);
				respJson.put("msg", Constant.PermissionErr);
			}
			
			 HttpIO.writeResp(response, respJson);
		}
}