package bzl.controller;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

import bzl.common.Constant;
import bzl.common.MemoryCache;
import bzl.common.SesCheck;
import bzl.entity.LoginLog;
import bzl.entity.ServerUpdate;
import bzl.entity.TerminalUpdate;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import bzl.task.SocketMsgHandler;

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
import utils.FileUtil;
import utils.HttpIO;
import utils.NetUtil;
import utils.RedisUtils;
import utils.ZipUtil;
import sun.rmi.log.LogHandler;


@Controller
@RequestMapping("/system")
public class SystemController {
	// private static Logger logger = Logger.getLogger(UserController.class);
	private final String VERSION = "1.0.2";
	private final String UPDATE_TIME = "2020-06-08";
	private final String CENTER_SERVER ="172.40.33.333";
	private final String SYSTEM_NAME = "博爻科技公司数字化多媒体系统";
	
	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	private static MemoryCache localMemCache = new MemoryCache();
    //获取后台系统升级信息
  	@RequestMapping(value="/getServerUpdateInfo",method=RequestMethod.POST)
  	public void serverUpdateInfo(HttpServletRequest request, HttpServletResponse response) {
  		JSONObject respJson = new JSONObject();
  		int result = 0;
  		User adminUser = SesCheck.getUserBySession(request,es, false); 
  		if(adminUser != null) {
  			JSONObject updateInfos = new JSONObject();
  			JSONArray resultList = new JSONArray();
  			updateInfos.put("zipfile", "");
  			updateInfos.put("attach_id", "");
  			updateInfos.put("cur_ver", VERSION);
  			String tomcatPath = request.getSession().getServletContext().getRealPath("/").replace("/cms", ""); 
			try {
				String realMd5 = DigestUtils.md5Hex(new FileInputStream(new File(tomcatPath + "/cms.war")));
				updateInfos.put("war_md5", realMd5);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
  			updateInfos.put("description", "请填写升级说明");
  			updateInfos.put("start_time", "");
  			updateInfos.put("action", "");
  			resultList.add(updateInfos);
  			respJson.put("status", Constant.SUCCESS);
			respJson.put("result",resultList);
  		}else {
  			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
  		}
  		
  		HttpIO.writeResp(response, respJson);
  	}
    
    
    //设置server 升级以及启动server升级
  	@RequestMapping(value="/setServerUpdate",method=RequestMethod.POST)
  	public void appUpdate(HttpServletRequest request, HttpServletResponse response) {
  		JSONObject respJson = new JSONObject();
  		int result = 0;
  		User adminUser = SesCheck.getUserBySession(request,es, false); 
  		if(adminUser != null) {
  			String jsonBodyStr = HttpIO.getBody(request);
  			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
  			Map<String,Object> condMap = new HashMap<String,Object>();
  			String attach_id = jsonBody.getString("attach_id"); //server压缩包文件的附件id
  			String action   = jsonBody.getString("action"); //on为启动升级,reboot为重启服务器
  			String description = jsonBody.getString("description");
  			JSONObject updateStatus = new JSONObject();
  			JSONArray resultList = new JSONArray();
  			Integer totalTerminal = 0;
  			Map<String, String> updateInfos = new HashMap<String,String>();
  			
  			condMap.put("attach_id", attach_id);
  			condMap.put("attach_type", 4); //4
  			List<Map<String, Object>> attachList =ms.selectList("Attachment", "selectByPK", condMap);
  			if(attachList!=null && attachList.size()==1) {
  				updateStatus.put("server_zipfile", attachList.get(0).get("name"));
  			}else {
  				updateStatus.put("server_zipfile", "");
  			}
  			SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  			f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
  			if(action.equals("on") && attachList!=null){
  				String newVersion = null;
  				ServerUpdate serverUpdataLog = new ServerUpdate();
				serverUpdataLog.setFilename((String) attachList.get(0).get("name"));
				serverUpdataLog.setDescription(description);
				serverUpdataLog.setCreate_time(new Date());
  				try {
  					newVersion = FileUtil.checkUpgradeVersion((String) attachList.get(0).get("save_path"),"update-info.txt");
  					if(newVersion!=null && !newVersion.equals(VERSION)) {
  						try {
  							File zipFile = new File((String) attachList.get(0).get("save_path"));
  							String tomcatPath = request.getSession().getServletContext().getRealPath("/").replace("/cms", ""); 
							ZipUtil.unZipDirToFiles(zipFile, tomcatPath);
							serverUpdataLog.setVersion(newVersion);
							serverUpdataLog.setStatus(1);//1为升级成功
							String realMd5 = DigestUtils.md5Hex(new FileInputStream(new File(tomcatPath + "/cms.war")));
							serverUpdataLog.setWar_md5(realMd5);
							result = 1;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							result = 0;
							e.printStackTrace();
						}
  					}else if(newVersion!=null && newVersion.equals(VERSION)) {
  						respJson.put("status", Constant.FAILED);
  						respJson.put("msg", "不要升级相同的版本!");
  						HttpIO.writeResp(response, respJson);
  						return;
  					}else {
  						respJson.put("status", Constant.FAILED);
  						respJson.put("msg", "错误的升级文件!");
  						HttpIO.writeResp(response, respJson);
  						return;
  					}
  				} catch (FileNotFoundException e) {
  					// TODO Auto-generated catch block
  					result = 0;
  					e.printStackTrace();
  				}finally {
  					es.insert("ServerUpdate", "insert", serverUpdataLog);
  				}
  			}else if(action.equals("restart")) {
  				String rebootScript = "reboot"; //重启服务器
  				Process exec;
				try {
					exec = Runtime.getRuntime().exec(new String[] { "bash","-c", rebootScript});
					try {
						if(0== exec.waitFor()) {
							result = 1;
						}else {
							result = 0;
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						result = 0;
						e.printStackTrace();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					result = 0;
					e.printStackTrace();
				}
  			}
  			
  			if(result==1) {
  				respJson.put("status", Constant.SUCCESS);
  	  			respJson.put("msg", "操作成功!");
  			}else {
  				respJson.put("status", Constant.SUCCESS);
  	  			respJson.put("msg", Constant.FailedMsg);
  			}
  		}else {
  			respJson.put("status", Constant.UserNotLogin);
  			respJson.put("msg", Constant.PermissionErr);
  		}
  		HttpIO.writeResp(response, respJson);
  	}
  	
  	
    //获取后台系统升级信息
  	@RequestMapping(value="/getServerUpateHistory",method=RequestMethod.POST)
  	public void getServerUpateHistory(HttpServletRequest request, HttpServletResponse response) {
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
			
			if(getTotal !=null) {
				List<Map<String,Object>> totalList= ms.selectList("ServerUpdate","selectCountByCondition", conMap);
				if(totalList !=null && totalList.size()==1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}
			
			conMap.put("startrom", (page-1)*pagesize);
			conMap.put("pagesize",pagesize);
			conMap.put("sort", "update_time desc");
			List<Map<String,Object>> serverUpdateLog = ms.selectList("ServerUpdate", "selectByConditionWithPage", conMap);
			serverUpdateLog = Convert.SortDataListId(serverUpdateLog,page,pagesize);
			if(serverUpdateLog != null && serverUpdateLog.size() >0) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result",serverUpdateLog);
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
