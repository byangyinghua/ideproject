package bzl.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import utils.Convert;
//import net.sf.json.JSONArray;
//import net.sf.json.JSONObject;
import utils.HttpIO;
import utils.RedisUtils;
import bzl.common.Constant;
import bzl.common.SesCheck;
import bzl.entity.HelpInfo;
import bzl.entity.Terminal;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import bzl.task.SocketMsgHandler;
import bzl.task.cmds;
import sun.rmi.log.LogHandler;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


/**
 * 终端求助控制器
 * <p>mark on 2019年6月6日.</p>
 */
@Controller
@RequestMapping("/help")
public class HelpController {
	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	//获取求助信息列表
	@RequestMapping(value="/help_list",method=RequestMethod.POST)
	@ResponseBody
	public void getHelpList(HttpServletRequest request, HttpServletResponse response){
		
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es,false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String helpStatus = jsonBody.getString("help_status");
			String getTotal = jsonBody.getString("getTotal");
			Map conMap = new HashMap<String,Object>();
			Set<String> terminalGids =null;
			if(adminUser.getIs_supper()==0) {
			   terminalGids = UserGroupController.getGroupListByUser(adminUser.getUid());
			}
			if(getTotal !=null) {
				List<Map<String,Object>> totalList = null;
				if(adminUser.getIs_supper()==0) {
					if(terminalGids!=null) {
						conMap.put("gids", terminalGids.toArray());
						totalList= ms.selectList("HelpInfo","countByTerminalGid", conMap);
					}
				}else {
					totalList= ms.selectList("HelpInfo","selectCountByCondition", conMap);
				}
				if(totalList !=null && totalList.size()==1) {
					respJson.put("total", totalList.get(0).get("count"));
				}else {
					respJson.put("total", 0);
				}
			}
			
			conMap.put("startrom", (page-1)*pagesize);
			conMap.put("pagesize",pagesize);
			
			conMap.put("sort","help_time desc");
			if(helpStatus==null) {
				helpStatus="all";
			}
			if(helpStatus.equals("notDeal")) {
				conMap.put("help_status",0);
			}else if(helpStatus.equals("alreadyDeal")) {
				conMap.put("help_status",1);
			}
			conMap.put("sort","help_time desc");
			
			List<Map<String,Object>> helplist =null;
			if(adminUser.getIs_supper()==1) {
				helplist  = ms.selectList("HelpInfo", "selectByConditionWithPage", conMap);
			}else {
				terminalGids = UserGroupController.getGroupListByUser(adminUser.getUid());
				if(terminalGids!=null) {
					conMap.put("gids", terminalGids.toArray());
					helplist  = ms.selectList("HelpInfo", "selectByTerminalGidByPage", conMap);
				}
			}
			
		
			if(helplist != null && helplist.size() > 0) {
				JSONArray retHelpList = new JSONArray();
				helplist = Convert.SortDataListId(helplist,page,pagesize);
			
				for(int i=0;i<helplist.size();i++) {
					conMap.clear();
					conMap.put("terminal_id",helplist.get(i).get("terminal_id"));
					List<Map<String,Object>> terminalList = ms.selectList("Terminal", "selectByPK", conMap);
					
					if(terminalList !=null && terminalList.size()==1) {
						JSONObject retHelpItem = new JSONObject();
						retHelpItem.put("id", helplist.get(i).get("id"));
						retHelpItem.put("help_id", helplist.get(i).get("help_id"));
						retHelpItem.put("terminal_id", terminalList.get(0).get("terminal_id"));
						retHelpItem.put("terminalIP", terminalList.get(0).get("ip"));

						String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + terminalList.get(0).get("terminal_id"));
						if(null != onlineInfo) {
							String state = onlineInfo.split(":")[1];
							retHelpItem.put("terminalState", Integer.parseInt(state));
						}else {
							retHelpItem.put("terminalState",0);
						}
						retHelpItem.put("terminalAddr", terminalList.get(0).get("install_addr"));
						retHelpItem.put("help_time", helplist.get(i).get("help_time"));
						retHelpItem.put("help_status", helplist.get(i).get("help_status"));
						retHelpItem.put("video_url", helplist.get(i).get("video_url"));
						retHelpList.add(retHelpItem);
						result = 1;
					}else {
						JSONObject retHelpItem = new JSONObject();
						retHelpItem.put("id", helplist.get(i).get("id"));
						retHelpItem.put("help_id", helplist.get(i).get("help_id"));
						retHelpItem.put("terminal_id", helplist.get(i).get("terminal_id"));
						retHelpItem.put("terminalIP", "无");
						retHelpItem.put("terminalState",0);
						retHelpItem.put("terminalAddr", "该终端已经不存在");
						retHelpItem.put("help_time", helplist.get(i).get("help_time"));
						retHelpItem.put("help_status", helplist.get(i).get("help_status"));
						retHelpItem.put("video_url", helplist.get(i).get("video_url"));
						retHelpList.add(retHelpItem);
						result = 1;
					}
				}
				if(result ==1) {
					respJson.put("status", Constant.SUCCESS);
					respJson.put("result", retHelpList);
				}else {
					respJson.put("status", Constant.notExistStatus);
					respJson.put("msg", Constant.NodataErr);
				}
				
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.NodataErr);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		HttpIO.writeResp(response, respJson);
	}
	
	//修改求助信息处理状态
		@RequestMapping(value="/modifyHelpSatus",method=RequestMethod.POST)
		@ResponseBody
		public void modifyHelpStatus(HttpServletRequest request, HttpServletResponse response) {
			JSONObject respJson = new JSONObject();
			int result = 0;
			User adminUser = SesCheck.getUserBySession(request, es,false); 
			if(adminUser !=null) {
				String jsonBodyStr = HttpIO.getBody(request);
				JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
				String helpId = jsonBody.getString("help_id");
				int helpStatus = jsonBody.getIntValue("help_status");
				Map conMap = new HashMap<String,Object>();
				conMap.put("help_id", helpId);
				conMap.put("help_status", helpStatus);
				result = ms.execute("HelpInfo", "update", conMap);
			}
			
			if(result ==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
			}else {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.FailedMsg);
			}
			
			HttpIO.writeResp(response, respJson);
		}
	
	
	//根据helpID获取求助视频列表
	@RequestMapping(value="/help_video_list",method=RequestMethod.POST)
	@ResponseBody
	public void getHelpVideoListByHelpId(HttpServletRequest request, HttpServletResponse response){
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es,false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String helpId = jsonBody.getString("help_id");
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			if(helpId != null && helpId.length() >0) {
				HelpInfo helpInfo = new HelpInfo();
				helpInfo.setHelp_id(helpId);
				List<HelpInfo> helpInfoList = es.select("HelpInfo", "selectByPK", helpInfo);
				if(helpInfoList !=null && helpInfoList.size()==1) {
					JSONObject socketBody = new JSONObject();
					socketBody.put("helpId", helpId);
					SocketMsgHandler socketHandler = new SocketMsgHandler();
					JSONObject respObj = socketHandler.syncSendMsgTo(helpInfoList.get(0).getTerminal_ip(), cmds.GET_HELP_RECORD, socketBody.toJSONString(),1000);
					if(respObj !=null) {
						String terminalResp = respObj.getString("resp");
						JSONObject terminalRespJson = JSONObject.parseObject(terminalResp);
						result = (int) terminalRespJson.get("status");
						if(result ==1) {         
							JSONObject respResult = terminalRespJson.getJSONObject("result");
							respJson.put("result", respResult.getJSONArray("video_list"));
							helpInfo.setVideo_list(respResult.getJSONArray("video_list").toJSONString());
							es.update("HelpInfo", "update", helpInfo);
						}
					}else {
						System.out.println("no response from terminal!!!");
					}
					
					String tmpVideoList = helpInfoList.get(0).getVideo_list();
					if(tmpVideoList!=null && tmpVideoList.length() >0) {
						respJson.put("result", JSONObject.parseArray(helpInfoList.get(0).getVideo_list()));
						result =1;
					}
				}
			}
			if(result ==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "获取失败！");
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		HttpIO.writeResp(response, respJson);

	}
	
	
	//让终端上传视频流
	@RequestMapping(value="/help_video",method=RequestMethod.POST)
	@ResponseBody
	public void getHelpVideoByName(HttpServletRequest request, HttpServletResponse response){
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es,false); 
		if(adminUser!=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String help_id = jsonBody.getString("help_id");
			JSONArray videoNames = jsonBody.getJSONArray("name_list");
			JSONArray removeNames = new JSONArray();
			JSONArray successNames = new JSONArray();
			
			if(help_id != null && help_id.length() >0) {
				for(int i=0;i<videoNames.size();i++) {
					String uploadStatus = RedisUtils.get("upload:" + help_id + ":" + videoNames.get(i));
					if(null == uploadStatus) {
						continue;
					}else if(null !=uploadStatus && uploadStatus.equals("uploading")) {
						removeNames.add(videoNames.get(i));
						result =2;
					}else {
						successNames.add(videoNames.get(i));
						removeNames.add(videoNames.get(i));
						result =1;
					}
				}
				videoNames.removeAll(removeNames);
				HelpInfo helpInfo = new HelpInfo();
				helpInfo.setHelp_id(help_id);
				List<HelpInfo> helpInfoList = es.select("HelpInfo", "selectByPK", helpInfo);
				if(helpInfoList !=null && helpInfoList.size()==1 && videoNames.size() >0) {
					SocketMsgHandler socketHandler = new SocketMsgHandler();
					String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + helpInfoList.get(0).getTerminal_id());
					if(null != onlineInfo) {
						JSONObject socketBody = new JSONObject();
						socketBody.put("helpId", help_id);
						socketBody.put("name_list", videoNames);
						socketHandler.sendMsgTo(helpInfoList.get(0).getTerminal_ip(), cmds.UPLOAD_HELP_VIDEO, socketBody.toJSONString());
						result =2;
					}
					
				}
			}
			
			if(result==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", successNames);
			}else if(result==2) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", "正在上传中！");
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "获取视频失败！");
			}
			
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		
		HttpIO.writeResp(response, respJson);
	}
	
	
}
