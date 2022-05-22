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
import utils.HttpIO;

@Controller
@RequestMapping("/camera")
public class CameraController {
	// private static Logger logger = Logger.getLogger(UserController.class);
	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	//获取外接摄像头列表
	@RequestMapping(value = "/list",method=RequestMethod.POST)
	@ResponseBody
	public void getCameraList(HttpServletRequest request,HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser!=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String terminal_id = jsonBody.getString("terminal_id");
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			Map<String,Object> conMap = new HashMap<String,Object>();
			conMap.put("startrom", (page-1)*pagesize);
			conMap.put("pagesize",pagesize);
			if(terminal_id !=null) {
				conMap.put("terminal_id", terminal_id);
			}
			List<Map<String,Object>> CameraList = ms.selectList("Camera", "selectByConditionWithPage", conMap);
			if(CameraList != null && CameraList.size() >0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result",CameraList);
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
	
	//增加获取修改外接摄像头
	@RequestMapping(value = "/addOrUpdate",method=RequestMethod.POST)
	@ResponseBody
	public void addOrUpdate(HttpServletRequest request,HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false);
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			//String CameraId = jsonBody.getString("camera_id");
			String terminal_id = jsonBody.getString("terminal_id");
			String CameraIP = jsonBody.getString("camera_ip");
			String Channel = jsonBody.getString("channel");
			String CameraName = jsonBody.getString("name");
			String InstallAddr =  jsonBody.getString("install_addr");
			String CameraBrand =  jsonBody.getString("brand");
			
			Map<String,Object> newCamera = new HashMap<String,Object>();
			//Camera newCamera = new Camera();
			if(CameraIP==null||CameraIP.length()==0||Channel==null||Channel.length()==0||terminal_id==null||terminal_id.length()==0) {
				result=0;
			}else {
				newCamera.put("camera_ip", CameraIP);
				newCamera.put("terminal_id", terminal_id);
				newCamera.put("channel", Channel);
				if(CameraName!=null||CameraName.length() >0) {
					newCamera.put("name", CameraName);
				}
				result = ms.execute("Camera", "update", newCamera);
				if(result==0) {
					newCamera.put("camera_id","camera" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(5));
					result = ms.execute("Camera", "insert", newCamera);
				}
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
	
	
	//删除外接摄像头,就是解绑
	@RequestMapping(value = "/delete",method=RequestMethod.POST)
	@ResponseBody
	public void deleteCamera(HttpServletRequest request,HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 1;
		User adminUser = SesCheck.getUserBySession(request, es,false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String camera_ip = jsonBody.getString("camera_ip");
			String terminal_id = jsonBody.getString("terminal_id");
			String channel =  jsonBody.getString("channel");
			Map<String,Object> condMap = new HashMap<String,Object>();
			condMap.put("camera_ip", camera_ip);
			condMap.put("terminal_id", terminal_id);
			condMap.put("channel", channel);
			result = ms.execute("Camera", "deleteByChannel", condMap);
			if(result > 0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
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
}
