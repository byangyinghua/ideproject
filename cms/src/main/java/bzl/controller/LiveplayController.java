package bzl.controller;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
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
import bzl.common.Configure;
import bzl.common.Constant;
import bzl.common.MemoryCache;
import bzl.common.SesCheck;
import bzl.entity.LivePlay;
import bzl.entity.LoginLog;
import bzl.entity.ShieldTask;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import bzl.task.SocketMsgHandler;
import bzl.task.cmds;

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
import utils.RedisUtils;
import utils.UUIDUtil;
import sun.rmi.log.LogHandler;

/*管理员账号操作controller*/

@Controller
@RequestMapping("/liveplay")
public class LiveplayController {

	// private static Logger logger = Logger.getLogger(UserController.class);
	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	private static int PlayLimit = 5;
	
	private int checkActionPermission(String uid,String live_id) {
		int result =0;
		Map<String,Object> conMap = new HashMap<String,Object>();
		conMap.put("live_id", live_id);
		Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(uid);
		if(tmpUserSet!=null && tmpUserSet.size() >0) {
			conMap.put("creator_uids", tmpUserSet.toArray());
			List<Map<String,Object>> taskList = ms.selectList("LivePlay", "selectByCondition", conMap);
			if(taskList!=null && taskList.get(0).get("creator_uid").equals(uid)) {
			     result =1;
			}
		}
		return result;
	}
	
	private String buildRtmpUrl(String live_id) {
		  String rtmpUrl = "";
		  String terminalRtmp ="stream" +  live_id;
		  rtmpUrl = terminalRtmp;
		  return Configure.getRtmpBasePath() +  rtmpUrl;
	}

	
	private static String terminalStartPullMode(String terminalIP,String pullUrl,Map<String,String> requestBody) {
		int status = 0;
		int needAgency = Integer.parseInt(requestBody.get("need_agency"));
		int stream_type = Integer.parseInt(requestBody.get("stream_type"));
		Integer taskConflictType = Integer.parseInt(requestBody.get("taskConflictType"));
		JSONObject socketBody = new JSONObject();
		socketBody.put("pull", 1);//设置终端启用拉流模式
		socketBody.put("pull_url", pullUrl);
		
		SocketMsgHandler newSocketHandler = new SocketMsgHandler();
		JSONObject terminalResp =null;
		if(needAgency == 0) {
			terminalResp = newSocketHandler.syncSendMsgTo(terminalIP, cmds.TERMINAL_DIRECT_VIDEO, socketBody.toJSONString(), 2000);
		}else {
			socketBody.put("pull_url", pullUrl);
			socketBody.put("streamType", stream_type);
			if(taskConflictType !=null) {
				socketBody.put("taskConflictType", taskConflictType);
			}
			terminalResp = newSocketHandler.syncSendMsgTo(terminalIP, cmds.SERVER_AGENCY_VIDEO, socketBody.toJSONString(), 2000);
		}
		
		if(terminalResp !=null) {
			JSONObject terminalRespJson = JSONObject.parseObject(terminalResp.getString("resp"));
			status = (int) terminalRespJson.get("status");
			if(status==1) {
				return "ok";//成功
			}
		}
		
		return "";//其他是失败
	}
	
	
	public static String terminalStopPullMode(String terminalIP,String pullUrl,int needAgency) {
		int status = 0;
		JSONObject socketBody = new JSONObject();
		socketBody.put("pull", 0);//设置终端启用拉流模式
//		socketBody.put("pull_url", pullUrl);
		SocketMsgHandler newSocketHandler = new SocketMsgHandler();
		JSONObject terminalResp =null;
		if(needAgency == 0) {
			terminalResp = newSocketHandler.syncSendMsgTo(terminalIP, cmds.TERMINAL_DIRECT_VIDEO, socketBody.toJSONString(), 2000);
		}else {
			terminalResp = newSocketHandler.syncSendMsgTo(terminalIP, cmds.SERVER_AGENCY_VIDEO, socketBody.toJSONString(), 2000);
			//delLocalVideoFile(pullUrl);
		}
		
		System.out.println("terminalStopPullMode = " + terminalResp);
		if(terminalResp !=null) {
			JSONObject terminalRespJson = JSONObject.parseObject(terminalResp.getString("resp"));
			status = (int) terminalRespJson.get("status");
			if(status==1) {
				return "ok";
			}
		}
		
		return "";//其他是失败
	}
	
	
	public static boolean startOrStopPushRtmp(String action,String live_id) {
		Set<String> terminal_ids = RedisUtils.allSetData(Constant.LivePlayTerminals + live_id);
		Map<String,String> playInfo =null;
		boolean isOk = false;
		for(String terminal_id: terminal_ids) {
			String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + terminal_id);
			if(null != onlineInfo) {
				String terminal_ip = onlineInfo.split(":")[0];
				playInfo = RedisUtils.hgetAll(Constant.LivePlayInfo + live_id);
				System.out.println("terminal_ip:"+terminal_ip+",action:" + action+ ",startOrStopPushRtmp ==" + playInfo.toString());
				if(action.equals("start")) {
					if("ok".equals(terminalStartPullMode(terminal_ip,playInfo.get("rtmpUrl"),playInfo))){
						RedisUtils.addMember(Constant.LivePlayingTerminals + live_id, terminal_ip, 0);
						playInfo.put("state", Constant.IsPlaying);
						isOk = true;
					}
				}else {
					if("ok".equals(terminalStopPullMode(terminal_ip,playInfo.get("rtmpUrl"),1))){
						RedisUtils.removeMember(Constant.LivePlayingTerminals + live_id, terminal_ip);
						isOk = true;
					}
				}
			}
		}
		return isOk;
	}

	
	private JSONArray GetWaitAndPlaying() {
		Map<String,Object> condMap = new HashMap<String,Object>();
		JSONArray resultList = new JSONArray();
		List<Map<String,Object>> livePlayList =ms.selectList("LivePlay", "selectAll", condMap);
		if(livePlayList !=null && livePlayList.size()>0) {
			for(int i=0;i<livePlayList.size();i++) {
				Map<String,String>tmpLivePlayInfo = RedisUtils.hgetAll(Constant.LivePlayInfo + livePlayList.get(i).get("live_id"));
				//System.out.println("tmpLivePlayInfo:" + tmpLivePlayInfo.toString());
				if(tmpLivePlayInfo!=null && tmpLivePlayInfo.size() >0) {
					String state = tmpLivePlayInfo.get("state");
					if(null!=state && (state.equals(Constant.IsPlaying)||state.equals(Constant.WaitForPlay))) {
						livePlayList.get(i).put("state", tmpLivePlayInfo.get("state"));
						livePlayList.get(i).put("attach_type", tmpLivePlayInfo.get("attach_type"));
						livePlayList.get(i).put("rtmpUrl", tmpLivePlayInfo.get("rtmpUrl"));
						Set<String> playingTerminals = RedisUtils.allSetData(Constant.LivePlayingTerminals + livePlayList.get(i).get("live_id"));
						if(tmpLivePlayInfo!=null && tmpLivePlayInfo.size() >0) {
							livePlayList.get(i).put("playingCnt", playingTerminals.size());
						}else {
							livePlayList.get(i).put("playingCnt",0);
						}
						resultList.add(livePlayList.get(i));
					}
				}
			}
		}
		return resultList;
	}
	
	/**
	 * get current live play list
	 * @param 
	 * @throws IOException
	 */
    @RequestMapping(value="/playinglist", method=RequestMethod.POST)
	public void getPlayingMediaList(HttpServletRequest request, HttpServletResponse response) {
    	JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
		     JSONArray theWaitAndPayingList = GetWaitAndPlaying();
		     respJson.put("status", Constant.SUCCESS);
			 respJson.put("result", theWaitAndPayingList);
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}
		HttpIO.writeResp(response, respJson);
    }

	/**
	 * 获取定时推流信息列表
	 * @param 
	 * @throws IOException
	 */
    @RequestMapping(value="/list", method=RequestMethod.POST)
	public void getLivePlayList(HttpServletRequest request, HttpServletResponse response) {
    	JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			Map<String,Object> condMap = new HashMap<String,Object>();
			if(adminUser.getIs_supper()==0) {
				Set<String> tmpUserSet = UserGroupController.getSameGroupUserList(adminUser.getUid());
				if(tmpUserSet!=null && tmpUserSet.size() >0) {
					condMap.put("creator_uids", tmpUserSet.toArray());
				}else {
					condMap.put("creator_uid", adminUser.getUid());
				}
			}
			
			condMap.put("startrom", (page-1)*pagesize);
			condMap.put("pagesize",pagesize);
			
			if(getTotal !=null) {
				List<Map<String,Object>> totalList= ms.selectList("LivePlay", "selectCountByCondition", condMap);
				//System.out.println("total data=" + totalList.toString());
				if(totalList !=null && totalList.size()==1) {
					respJson.put("total", totalList.get(0).get("count"));
				}
			}
			
			List<Map<String,Object>> livePlayList =ms.selectList("LivePlay", "selectByConditionWithPage", condMap);
			if(livePlayList !=null && livePlayList.size()>0) {
				Map<String,String> tmpLivePlayInfo = null;
				Set<String> terminals =null;
				livePlayList = Convert.SortDataListId(livePlayList, page, pagesize);
				for(int i=0;i<livePlayList.size();i++) {
					System.out.println("livePlayList =="+livePlayList.get(i).get("live_id"));
					tmpLivePlayInfo = RedisUtils.hgetAll(Constant.LivePlayInfo + livePlayList.get(i).get("live_id"));
					//System.out.println("tmpLivePlayInfo:" + tmpLivePlayInfo.toString());
					if(tmpLivePlayInfo!=null && tmpLivePlayInfo.size() >0) {
						if(null==tmpLivePlayInfo.get("state")) {
							livePlayList.get(i).put("state", Constant.NotStartPlay);
						}else {
							livePlayList.get(i).put("state", tmpLivePlayInfo.get("state"));
						}
						livePlayList.get(i).put("attach_type", tmpLivePlayInfo.get("attach_type"));
						livePlayList.get(i).put("rtmpUrl", tmpLivePlayInfo.get("rtmpUrl"));
					}else {
						livePlayList.get(i).put("state", Constant.NotStartPlay);
						livePlayList.get(i).put("attach_type", Constant.MediaVideo);
					}
					
					Set<String> playingTerminals = RedisUtils.allSetData(Constant.LivePlayingTerminals + livePlayList.get(i).get("live_id"));
					if(tmpLivePlayInfo!=null && tmpLivePlayInfo.size() >0) {
						livePlayList.get(i).put("playingCnt", playingTerminals.size());
					}else {
						livePlayList.get(i).put("playingCnt",0);
					}
					
					terminals = RedisUtils.allSetData(Constant.LivePlayTerminals + livePlayList.get(i).get("live_id"));
					//System.out.println("!!!!terminals:"+terminals.toString());
					if(terminals!=null && terminals.size() >0) {
						livePlayList.get(i).put("terminal_ids", terminals.toArray());
					}else {
						livePlayList.get(i).put("terminal_ids", new JSONArray());
					}
				}
				result =1;
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", livePlayList);
			}
			if(result==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", Constant.SuccessMsg);
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
    
    /**
	 * 添加或者更改推流设置
	 * @param 
	 * @throws IOException
	 */
    @RequestMapping(value="/delete", method=RequestMethod.POST)
	public void deleteLivePlay(HttpServletRequest request, HttpServletResponse response) {
    	JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String live_id = jsonBody.getString("live_id");
			if(adminUser.getIs_supper()==0 && live_id!=null) {
				int isHasPermission = checkActionPermission(adminUser.getUid(),live_id);
				if(isHasPermission==0) {
					respJson.put("status", Constant.UserNotLogin);
					respJson.put("msg", Constant.PermissionErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			
			if(live_id!=null) {
				boolean canDelete = false;
				Map<String, String> tmpLivePlayInfo = RedisUtils.hgetAll(Constant.LivePlayInfo + live_id);
				if(tmpLivePlayInfo!=null && tmpLivePlayInfo.size() >0) {
					if(null==tmpLivePlayInfo.get("state")) {
						canDelete = true;
					}else if(tmpLivePlayInfo.get("state").equals(Constant.IsPlaying)||tmpLivePlayInfo.get("state").equals(Constant.WaitForPlay)) {
						canDelete = false;
					}else {
						canDelete = true;
					}
				}else {
					canDelete = true;
				}
				if(canDelete) {
					Map<String,Object> condMap = new HashMap<String,Object>();
					condMap.put("live_id",live_id);
					result = ms.execute("LivePlay", "delete", condMap);
				}
			}
			if(result==1) {
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
    
    /**
	 * 添加或者更改推流设置
	 * @param 
	 * @throws IOException
	 */
    @RequestMapping(value="/addOrUpdate", method=RequestMethod.POST)
	public void addOrUpdateLivePlay(HttpServletRequest request, HttpServletResponse response) {
    	
    	JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false); 
		if(adminUser !=null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String live_id = jsonBody.getString("live_id");
			String name  = jsonBody.getString("name");
			String attach_id = jsonBody.getString("attach_id"); 
			String attach_name = jsonBody.getString("attach_name");
			String start_time = jsonBody.getString("start_time");
		    JSONArray terminal_ids =jsonBody.getJSONArray("terminal_ids");
			int attach_type = jsonBody.getIntValue("attach_type");
			SimpleDateFormat myFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			myFmt.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
			if(adminUser.getIs_supper()==0 && live_id!=null) {
				int isHasPermission = checkActionPermission(adminUser.getUid(),live_id);
				if(isHasPermission==0) {
					respJson.put("status", Constant.UserNotLogin);
					respJson.put("msg", Constant.PermissionErr);
					HttpIO.writeResp(response, respJson);
					return;
				}
			}
			
			LivePlay  newLivePlay= new LivePlay();
			if(name!=null && name.length() >0) {
				newLivePlay.setName(name);
			}
			if(attach_id!=null && attach_id.length() >0) {
				newLivePlay.setAttach_id(attach_id);
			}
			if(attach_name!=null && attach_name.length() >0) {
				newLivePlay.setAttach_name(attach_name);
			}
			if(start_time!=null && start_time.length() >0) {
				newLivePlay.setStart_time(start_time);
			}else {
				newLivePlay.setStart_time(myFmt.format(new Date()));
			}
			Map<String,String> playInfo = null;
			if (live_id != null && live_id.length() > 0) {
				newLivePlay.setLive_id(live_id);
				System.out.println("newLivePlay===" + newLivePlay.toString());
				result = es.update("LivePlay", "update", newLivePlay);
				if(result==1) {
					playInfo = RedisUtils.hgetAll(Constant.LivePlayInfo + live_id);
					playInfo.put("attach_type", ""+attach_type);
					if(terminal_ids!=null && terminal_ids.size() >0) {
						RedisUtils.del(Constant.LivePlayTerminals + live_id);
						for(int n=0;n<terminal_ids.size();n++) {//使用redis存储设置的终端列表
							RedisUtils.addMember(Constant.LivePlayTerminals + live_id, (String) terminal_ids.get(n), 0);
						}
					}
				}
			} else { 
				newLivePlay.setLive_id("live" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(7));
				newLivePlay.setCreate_time(new Date());
				newLivePlay.setCreator(adminUser.getUsername());
				newLivePlay.setCreator_uid(adminUser.getUid());
				result = es.insert("LivePlay", "insert", newLivePlay);
				playInfo = new HashMap<String,String>();
				playInfo.put("state", Constant.NotStartPlay);
				playInfo.put("attach_type", ""+attach_type);
			}
			
			RedisUtils.hset(Constant.LivePlayInfo + live_id, playInfo, 0);
			//JSONArray restList = new JSONArray();
			if(result==1) {
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
    
    
    /**
   	 * 启用或者停止推流
   	 * @param 
   	 * @throws IOException
   	 */
       @RequestMapping(value="/startOrStop", method=RequestMethod.POST)
   	public void startOrStopLivePlay(HttpServletRequest request, HttpServletResponse response) {
    		JSONObject respJson = new JSONObject();
    		String remoteIP = request.getRemoteHost();
    		int result = 0;
    		User adminUser = SesCheck.getUserBySession(request,es, false);  //同一个用户禁止多端登录
    		if(adminUser != null) {
    			String jsonBodyStr = HttpIO.getBody(request);
    			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
    			String live_id = jsonBody.getString("live_id");
    			String action = jsonBody.getString("action");		
    			Map<String,Object> condMap = new HashMap<String,Object>();
    			//SocketMsgHandler newSocketHandler = new SocketMsgHandler();
    			if(adminUser.getIs_supper()==0) {
    				int isHasPermission = checkActionPermission(adminUser.getUid(),live_id);
    				if(isHasPermission==0) {
    					respJson.put("status", Constant.UserNotLogin);
    					respJson.put("msg", Constant.PermissionErr);
    					HttpIO.writeResp(response, respJson);
    					return;
    				}
    			}
    			
    			JSONArray restList = new JSONArray();
    			//JSONObject retInfof = new JSONObject();
    			boolean filExist =true;
    			boolean terminalExist = true;
    			int HasStartCount = 0;
    			
    			Set<String> terminal_ids = RedisUtils.allSetData(Constant.LivePlayTerminals + live_id);
    			for(String terminal_id:terminal_ids) {
    				String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + terminal_id);
					if(null == onlineInfo) {
						respJson.put("status", Constant.FAILED);
    					respJson.put("msg", "id为"+terminal_id +  "的终端不在线");
    					HttpIO.writeResp(response, respJson);
    					return;
					}
    			}
    			
    			if(action.equals("start")) {
    				//RedisUtils.set(key, value)
    				HasStartCount = GetWaitAndPlaying().size();
    				if(HasStartCount>=5) {
    				    System.out.println("wait or playing is meet limit:"+HasStartCount);
    				}else if(terminal_ids==null||terminal_ids.size()==0) {
    					terminalExist = false;
    				}else {
    					condMap.put("live_id", live_id);
    					List<Map<String, Object>> livePlayList = ms.selectList("LivePlay", "selectByPK", condMap);
    					if (livePlayList != null && livePlayList.size() == 1) {
    						//startFFmpegPushRtmp((String) livePlayList.get(0).get("media_path"),"rtmp://192.168.101.9/live/liveplay" + liveId);
    						String starttime = (String) livePlayList.get(0).get("start_time");
    						SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    						f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
    						int waitSeconds = 0;
    						try {
    							
								condMap.clear();
								condMap.put("attach_id", livePlayList.get(0).get("attach_id"));
								Map<String,Object> attachInfo=(Map<String, Object>) ms.selectOne("Attachment","selectByCondition", condMap);
								if(attachInfo==null||attachInfo.size()==0) {
									filExist = false;
								}else {
									waitSeconds = (int) ((f.parse(starttime).getTime() - new Date().getTime())/1000);
	    							if(waitSeconds <= 0) {
	    								waitSeconds =3;
	    								LivePlay  newLivePlay= new LivePlay();
	    								newLivePlay.setLive_id(live_id);
	    								newLivePlay.setStart_time(f.format(new Date()));
	    								es.update("LivePlay", "update", newLivePlay);
	    							}
	    							RedisUtils.setExpire(Constant.LivePlaySwitch + live_id + ":on", "on",waitSeconds);
									String pullUrl   = buildRtmpUrl(live_id);
								
				    				JSONObject requestBody = new JSONObject();
				    				requestBody.put("state", Constant.WaitForPlay);
				    				requestBody.put("need_agency", 1);
				    				Integer mediaType = (Integer) attachInfo.get("attach_type");
		
				    				if(mediaType==2) {//音频文件
				    					requestBody.put("stream_type", mediaType);//1是音视频，2是音频，3是视频
				    					requestBody.put("taskConflictType", 1);//音频传1，视频传0
				    				}else if(mediaType==3) {
				    					requestBody.put("stream_type", mediaType);//1是音视频，2是音频，3是视频
				    					requestBody.put("taskConflictType", 0);//音频传1，视频传0
				    				}
				    				requestBody.put("media_path", (String) attachInfo.get("save_path"));
				    				requestBody.put("rtmpUrl", pullUrl);
									RedisUtils.hset(Constant.LivePlayInfo + live_id, requestBody, 0);
									result =1;
								}
    						} catch (ParseException e) {
    							// TODO Auto-generated catch block
    							e.printStackTrace();
    						}
    					}
    				}
    			}else if(action.equals("stop")) {
    				String isOn = RedisUtils.get(Constant.LivePlaySwitch + live_id + ":on");
    				if(isOn != null && isOn.equals("on")) {
    					RedisUtils.del(Constant.LivePlaySwitch + live_id + ":on");
    					RedisUtils.del(Constant.LivePlayInfo + live_id);
    				}else {
    					Map<String,String> playInfo = RedisUtils.hgetAll(Constant.LivePlayInfo + live_id);
    					playInfo.put("state", Constant.IsStopPlay);
    					RedisUtils.hset(Constant.LivePlayInfo + live_id, playInfo, 0);
    					RedisUtils.del(Constant.LivePlaySwitch + live_id + ":off");
    					RedisUtils.setExpire(Constant.LivePlaySwitch + live_id + ":off", "off",2);//已经开始的任务强制停止
    					RedisUtils.del(Constant.LivePlayingTerminals + live_id);
    					System.out.println("before stop ffmpeg!!!!!!!!!!!!!!!!!!!!!!!");
    				}
    				startOrStopPushRtmp("stop", live_id);
    				result =1;
    			}
    			
    			if(!terminalExist) {
    				respJson.put("status", Constant.FAILED);
    				respJson.put("msg", "请先设置终端!");
    			}else if(!filExist) {
    				respJson.put("status", Constant.FAILED);
    				respJson.put("msg", "媒体文件已经被删除!");
    			}else if(HasStartCount > PlayLimit && action.equals("start")) {
    				respJson.put("status", Constant.FAILED);
    				respJson.put("msg", "等待推流或者正在推流的任务数量已经超出" + HasStartCount + "个!");
    			}else if(result == 1) {
    				respJson.put("status", Constant.SUCCESS);
    				respJson.put("result", restList);
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
    
}
