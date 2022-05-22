package bzl.controller;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.bridj.TimeT.timeval;

import bzl.common.Configure;
import bzl.common.Constant;
import bzl.common.MemoryCache;
import bzl.common.SesCheck;
import bzl.entity.LoginLog;
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
import com.google.common.collect.Lists;

import utils.EncryptionUtil;
import utils.HttpIO;
import utils.RedisUtils;
import utils.UUIDUtil;
import sun.rmi.log.LogHandler;

/*管理员账号操作controller*/

@Controller
@RequestMapping("/video")
public class VideoController {

	// private static Logger logger = Logger.getLogger(UserController.class);
	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;

	public static String streamSesKey = "stream_session_set";
	private static int expireSec = 10;

	// private static MemoryCache localMemCache = new MemoryCache();

	private static boolean closeStreamThreadStart = false;

	private String buildStreamSes() {
		return "stream-" + UUIDUtil.getUUID();
	}

	private String buildRtmpUrl(User user, String terminalId) {
		String rtmpUrl = "";
		if (user != null) {
			String userId = user.getUid();
			rtmpUrl = "stream" + userId;
		} else {
			String terminalRtmp = "stream" + terminalId;
			rtmpUrl = terminalRtmp;
		}
		String rtmpBaseUrl = Configure.getRtmpBasePath();
		
		return rtmpBaseUrl + rtmpUrl;
	}

	private static boolean delLocalVideoFile(String localUrl) {
		String[] tmpNames = localUrl.split("/");
		String tmpFileName = tmpNames[tmpNames.length - 1] + ".flv";
		File tmpVideoFile = new File(tmpFileName);
		if (tmpVideoFile.exists()) {
			tmpVideoFile.delete();
		}
		return false;
	}

	public static void refreshLiveStreamSes(String streamSession) {
		if (streamSession != null && streamSession.length() > 0) {
			RedisUtils.refreshExpired("online:" + streamSession, expireSec);
		}
	}
	
	public static void delLiveStreamSes(String streamSession) {
		if (streamSession != null && streamSession.length() > 0) {
			RedisUtils.refreshExpired("online:" + streamSession, 1);//1s立即删除
		}
	}
	
	private int getTerminalType(String terminal_id) {
		  if(terminal_id.substring(3,5).indexOf("02")!=-1){
              return  2; //音频终端，不能播放视频
          }else{
              return 1;//普通终端
          }
	}

	private int terminalStartPullMode(String terminalIP, String pullUrl, JSONObject requestBody) {
		int status = 0;
		int needAgency = requestBody.getIntValue("need_agency");
		int stream_type = requestBody.getIntValue("stream_type");
		Integer taskConflictType = requestBody.getInteger("taskConflictType");
		JSONObject socketBody = new JSONObject();
		socketBody.put("pull", 1);// 设置终端启用拉流模式
		socketBody.put("pull_url", pullUrl);

		SocketMsgHandler newSocketHandler = new SocketMsgHandler();
		int sequece = 0;
		if (needAgency == 0) {
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.TERMINAL_DIRECT_VIDEO, socketBody.toJSONString());
		} else {
			socketBody.put("pull_url", pullUrl);
			socketBody.put("streamType", stream_type);
			if (taskConflictType != null) {
				socketBody.put("taskConflictType", taskConflictType);
			}
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.SERVER_AGENCY_VIDEO, socketBody.toJSONString());
		}

		return sequece;// 其他是失败
	}

	public static int terminalStopPullMode(String terminalIP, String pullUrl, int needAgency) {
		// int status = 0;
		JSONObject socketBody = new JSONObject();
		socketBody.put("pull", 0);// 设置终端启用拉流模式
//		socketBody.put("pull_url", pullUrl);
		SocketMsgHandler newSocketHandler = new SocketMsgHandler();
		int sequece = 0;
		if (needAgency == 0) {
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.TERMINAL_DIRECT_VIDEO, socketBody.toJSONString());
		} else {
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.SERVER_AGENCY_VIDEO, socketBody.toJSONString());
			// delLocalVideoFile(pullUrl);
		}

		return sequece;// 其他是失败
	}

	private int terminalStartPushMode(String terminalIP, String pushUrl, int needAgency) {
		String retpushUrl = null;
		JSONObject socketBody = new JSONObject();
		socketBody.put("push", 1);// 设置终端启用拉流模式

		SocketMsgHandler newSocketHandler = new SocketMsgHandler();
		int sequece = 0;
		if (needAgency == 0) {
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.TERMINAL_DIRECT_VIDEO, socketBody.toJSONString());
		} else {
			socketBody.put("push_url", pushUrl);
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.SERVER_AGENCY_VIDEO, socketBody.toJSONString());
		}
		return sequece;// 其他是失败
	}

	public static int terminalStopPushMode(String terminalIP, String pullUrl, int needAgency) {
		String pushUrl = null;
		JSONObject socketBody = new JSONObject();
		socketBody.put("push", 0);// 设置终端启用拉流模式

		SocketMsgHandler newSocketHandler = new SocketMsgHandler();
		int sequece = 0;
		if (needAgency == 0) {
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.TERMINAL_DIRECT_VIDEO, socketBody.toJSONString());
		} else {
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.SERVER_AGENCY_VIDEO, socketBody.toJSONString());
		}

		return sequece;// 其他是失败
	}

	private int terminalStartAllMode(User adminUser, String terminalIP, String terminalId, String pullUsr,
			int needAgency) {
		String retPushUrl = null;
		JSONObject socketBody = new JSONObject();
		socketBody.put("pull", 1);// 设置终端启用拉流模式
		socketBody.put("pull_url", pullUsr);// 设置终端启用拉流模式

		socketBody.put("push", 1);// 设置终端启用推流模式

		SocketMsgHandler newSocketHandler = new SocketMsgHandler();
		int sequece = 0;
		if (needAgency == 0) {
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.TERMINAL_DIRECT_VIDEO, socketBody.toJSONString());
		} else {
			retPushUrl = buildRtmpUrl(null, terminalId);
			socketBody.put("push_url", retPushUrl);// 设置终端启用推流模式
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.SERVER_AGENCY_VIDEO, socketBody.toJSONString());
		}
		return sequece;
	}

	private int terminalStopAllMode(String terminalIP, String pullUrl, String pushUrl, int needAgency) {
		JSONObject socketBody = new JSONObject();
		socketBody.put("pull", 0);// 设置终端关闭拉流模式
		socketBody.put("push", 0);// 设置终端关闭推流模式

		SocketMsgHandler newSocketHandler = new SocketMsgHandler();
		int sequece = 0;
		if (needAgency == 0) {
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.TERMINAL_DIRECT_VIDEO, socketBody.toJSONString());
		} else {
			sequece = newSocketHandler.sendMsgTo(terminalIP, cmds.SERVER_AGENCY_VIDEO, socketBody.toJSONString());
		}
		return sequece;// 其他是失败
	}

	private String stopTerminalTalk(String streamSes) {
		String stop_terminal = null;
		Map<String, String> redisData = RedisUtils.hgetAll(streamSes);
		if (redisData != null && redisData.size() > 0) {
			stop_terminal = redisData.get("talk_terminal");
			if (stop_terminal != null && stop_terminal.length() > 0) { // 停止此终端对讲
				//JSONObject socketBody = new JSONObject();
				Map<String, Object> condMap = new HashMap<String, Object>();
				//SocketMsgHandler newSocketHandler = new SocketMsgHandler();
				//String terminalResp = null;
				condMap.put("terminal_id", stop_terminal);
				List<Map<String, Object>> terminalList = ms.selectList("Terminal", "selectByCondition", condMap);
				if (terminalList != null && terminalList.size() == 1) {
					String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) terminalList.get(0).get("terminal_id"));
					if (null != onlineInfo) {
						String state = onlineInfo.split(":")[1];
						if (Integer.parseInt(state) > 0) {
							String pullUrl = redisData.get("pull_url");
							String pushUrl = redisData.get("push_url");
							int needAgency = Integer.parseInt(redisData.get("need_agency"));
							RedisUtils.removeMember(pushUrl, streamSes);
							if (RedisUtils.allSetData(pushUrl).size() == 0) {
								//JSONArray sequeceList = new JSONArray();
								Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
								int sequece = terminalStopPushMode((String) terminalList.get(0).get("ip"), pushUrl, needAgency);
								sequeceMap.put(sequece, (String) terminalList.get(0).get("terminal_id"));
								Map<Integer, JSONObject> RespMap = SocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);// 超时２秒
								if (RespMap != null && RespMap.size() > 0) {
									for (Integer key : RespMap.keySet()) {// key 是sequece
										String terminalRespStr = RespMap.get(key).getString("resp");
										//String terminalIP = RespMap.get(key).getString("terminal_ip");
										JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
										int status = (int) terminalRespJson.get("status");
										if (status != 1) {
											stop_terminal = null;
											delLocalVideoFile(pullUrl);
											delLocalVideoFile(pushUrl);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return stop_terminal;
	}

	private JSONObject startTerminalTalk(User adminUser, String start_terminal, int needAgency, String broadcast_url) {
		JSONObject retUrlInfof = new JSONObject();
		if (start_terminal != null && start_terminal.length() > 0) {
			Map<String, Object> condMap = new HashMap<String, Object>();
			condMap.put("terminal_id", start_terminal);
			List<Map<String, Object>> terminalList = ms.selectList("Terminal", "selectByCondition", condMap);
			if (terminalList != null && terminalList.size() == 1) {
				String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) terminalList.get(0).get("terminal_id"));
				if(null != onlineInfo) {
					String state = onlineInfo.split(":")[1];
//					if (Integer.parseInt(state) > 0 && Integer.parseInt(state)!=6 && Integer.parseInt(state)!=3) {
					if (Integer.parseInt(state) > 0) {
						int sequece = terminalStartAllMode(adminUser, (String) terminalList.get(0).get("ip"), start_terminal,broadcast_url, needAgency);
						Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
						sequeceMap.put(sequece, (String) terminalList.get(0).get("terminal_id"));
						Map<Integer, JSONObject> RespMap = SocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);// 超时２秒
						if (RespMap != null && RespMap.size() > 0) {
							for (Integer key : RespMap.keySet()) {// key 是sequece
								String terminalRespStr = RespMap.get(key).getString("resp");
								String terminalIP = RespMap.get(key).getString("terminal_ip");
								JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
								int status = (int) terminalRespJson.get("status");
								if (status == 1) {
									JSONObject result = terminalRespJson.getJSONObject("result");
									retUrlInfof.put("push_url", result.get("push_url"));
									retUrlInfof.put("terminal_ip", terminalIP);
									return retUrlInfof;
								}
							}
						}
					}else {
						System.out.println("the terminal status is not ready !status==" + state);
					}
				}
			}
		}
		return null;
	}

	public static boolean stopStreamSession(String streamSession) {
		boolean isOk = false;
		//System.out.println("stopStreamSession redis streamSession==" + streamSession);
		Map<String, String> redisData = RedisUtils.hgetAll(streamSession);
		//System.out.println("stopStreamSession redis data==" + redisData.toString());
		JSONArray stopPullFailList = new JSONArray();
		JSONArray stopPushFailList = new JSONArray();
		if (redisData != null && redisData.size() > 0) {
			String pullUrl = redisData.get("pull_url");
			String pushUrl = redisData.get("push_url");
			JSONArray pullTerminalIPs = JSONObject.parseArray(redisData.get("pull_terminalIPs"));
			JSONArray pushTerminalIPs = JSONObject.parseArray(redisData.get("push_terminalIPs"));
			Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
			int needAgency = Integer.parseInt(redisData.get("need_agency"));
			if (pullUrl != null && pullUrl.length() > 0) {
				Long remove0 = RedisUtils.removeMember(pullUrl, streamSession);
				Long remove1 = RedisUtils.removeMember(VideoController.streamSesKey, streamSession);
				if (pullTerminalIPs != null && pullTerminalIPs.size() > 0) {
					for (int i = 0; i < pullTerminalIPs.size(); i++) {
						int sequece = terminalStopPullMode((String) pullTerminalIPs.get(i), pullUrl, needAgency);
						if (sequece > 0) {
							 sequeceMap.put(sequece, "");
						}
					}
					Map<Integer, JSONObject> RespMap = SocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);// 超时２秒
					if (RespMap != null && RespMap.size() > 0) {
						for (Integer key : RespMap.keySet()) {// key 是sequece
							String terminalRespStr = RespMap.get(key).getString("resp");
							String terminalIP = RespMap.get(key).getString("terminal_ip");
							JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
							int status = (int) terminalRespJson.get("status");
							if (status != 1) {
								stopPullFailList.add(terminalIP);
							}
						}
					}
				}
				
				Set<String> sesList = RedisUtils.allSetData(pullUrl);
				if(sesList ==null || sesList.size()==0) {
					delLocalVideoFile(pullUrl);
//					delLocalVideoFile(pushUrl);
				}else {
					for (String tmpSes :sesList) {
						System.out.println("pullUrl=" + pullUrl + "," + "RedisUtils.allSetData(pullUrl).tmpSes=" + tmpSes);
					}
				}
			}
			if (pushUrl != null && pushUrl.length() > 0) {
				Long remove3 = RedisUtils.removeMember(pushUrl, streamSession);
				Long remove4 = RedisUtils.removeMember(VideoController.streamSesKey, streamSession);
				if (pushTerminalIPs != null && pushTerminalIPs.size() > 0
						&& RedisUtils.allSetData(pushUrl).size() == 0) {
					for (int i = 0; i < pushTerminalIPs.size(); i++) {
						int sequece = terminalStopPushMode((String) pushTerminalIPs.get(i), pushUrl, needAgency);
						if (sequece > 0) {
							 sequeceMap.put(sequece, "");
						}
					}
					Map<Integer, JSONObject> RespMap = SocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);// 超时２秒
					if (RespMap != null && RespMap.size() > 0) {
						for (Integer key : RespMap.keySet()) {// key 是sequece
							String terminalRespStr = RespMap.get(key).getString("resp");
							String terminalIP = RespMap.get(key).getString("terminal_ip");
							JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
							int status = (int) terminalRespJson.get("status");
							if (status != 1) {
								stopPushFailList.add(terminalIP);
							}
						}
					}

				} 
				
				Set<String> sesList1 = RedisUtils.allSetData(pushUrl);
				if(sesList1 ==null || sesList1.size()==0) {
					delLocalVideoFile(pushUrl);
				}else {
					for (String tmpSes :sesList1) {
						System.out.println("pushUrl=" + pushUrl + "," + "RedisUtils.allSetData(pullUrl).tmpSes=" + tmpSes);
					}
				}
			}

			if (stopPullFailList.size() == 0 && stopPushFailList.size() == 0) {
				isOk = true;
				redisData.put("pull_terminalIPs", stopPullFailList.toJSONString());
				redisData.put("push_terminalIPs", stopPushFailList.toJSONString());

				RedisUtils.del("online:" + streamSession);
				RedisUtils.del(streamSession);
			}
		}

		return isOk;
	}

	// 在nginx中设置的回调接口，当开始推送时回调
	@RequestMapping("/on_publish")
	public void nginxRtmpPublish(HttpServletRequest request, HttpServletResponse response) {
		// Enumeration params = request.getParameterNames();
//		 log.warn("\n\n on_publish");
//		 Enumeration enu=request.getParameterNames();  
//		 while(enu.hasMoreElements()){  
//		 String paraName=(String)enu.nextElement();  
//		 System.out.println(paraName+": "+request.getParameter(paraName));  
//		 }

//		 $app = $_GET['app'];
//	        $swfurl = $_GET['swfurl'];
//	        $tcurl = $_GET['tcurl'];
//	        $pageurl = $_GET['pageurl'];
//	        $addr = $_GET['addr'];
//	        $clientid = $_GET['clientid'];
//	        $call = $_GET['call'];
//	        $name = $_GET['name'];

	}

	// 在nginx中设置的回调接口，当停止推送是回调
	@RequestMapping("/on_publish_done")
	public void nginxRtmpPublishDone(HttpServletRequest request, HttpServletResponse response) {
		Map params = request.getParameterMap();

//		log.warn("\n\n on_publish_done");
//			 
//		 Enumeration enu=request.getParameterNames();  
//		 while(enu.hasMoreElements()){  
//		 String paraName=(String)enu.nextElement();  
//		 System.out.println(paraName+": "+request.getParameter(paraName));  
//		 }

//		 on_publish_done
//		 app: live
//		 flashver: 
//		 swfurl: 
//		 tcurl: rtmp://192.168.101.5/live
//		 pageurl: 
//		 addr: 192.168.101.15
//		 clientid: 825
//		 call: publish_done
//		 name: stream101001014108ab02d909

	}

	// 在nginx中设置的回调接口，当开始播放时回调
	@RequestMapping("/on_play")
	public void nginxRtmpPull(HttpServletRequest request, HttpServletResponse response) {
		Map params = request.getParameterMap();

		// log.warn("\n\n on_play");
//		 
//		 Enumeration enu=request.getParameterNames();  
//		 while(enu.hasMoreElements()){  
//		 String paraName=(String)enu.nextElement();  
//		 System.out.println(paraName+": "+request.getParameter(paraName));  
//		 }
//		 

	}

	// 在nginx中设置的回调接口，当停止播放时回调
	@RequestMapping("/on_play_done")
	public void nginxRtmpPullDone(HttpServletRequest request, HttpServletResponse response) {
		Map params = request.getParameterMap();

//		 log.warn("\n\n on_play_done");
//		 
//		 Enumeration enu=request.getParameterNames();  
//		 while(enu.hasMoreElements()){  
//		 String paraName=(String)enu.nextElement();  
//		 System.out.println(paraName+": "+request.getParameter(paraName));  
//		 }

	}

	/**
	 * 请求开始广播或者停止广播
	 * 
	 * @param
	 * @throws IOException
	 */
	@RequestMapping(value = "/broadcast", method = RequestMethod.POST)
	public void broadcastVideo(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			JSONArray gids = jsonBody.getJSONArray("gids");
			String action = jsonBody.getString("action");
			int needAgency = jsonBody.getIntValue("need_agency");
			String broadcast_url = jsonBody.getString("broadcast_url"); // 不需要服务器代理的时候，这是终端直接提供的广播流
			String streamSession = jsonBody.getString("stream_session");
			int stream_type = jsonBody.getIntValue("stream_type"); //1 音视频,2音频,3视频

			if (needAgency == 1) { // 如果需要代理
				broadcast_url = buildRtmpUrl(adminUser, null); // 如果代理模式需要使用
			}
			Map<String, Object> condMap = new HashMap<String, Object>();
			JSONArray restList = new JSONArray();
			JSONObject retInfof = new JSONObject();
			JSONArray pull_terminalIPs = new JSONArray();
			if (action.equals("start")) {
				streamSession = buildStreamSes();
				List<Map<String, Object>> allTerminals = null;
				// 新增支持分组
				if (gids != null && gids.size() == 1 && gids.get(0).equals("all-group")) {
					condMap.clear();
					allTerminals = ms.selectList("Terminal", "selectAll", condMap);
				} else if (gids != null && gids.size() > 0) {
					for (int i = 0; i < gids.size(); i++) {
						condMap.put("gid", gids.get(i));
						allTerminals = (List<Map<String, Object>>) ms.selectList("Terminal", "selectByCondition",condMap);
					}
				}

				if (terminal_ids != null) {
					if (allTerminals == null) {
						allTerminals = Lists.newArrayList();
					}
					for (int i = 0; i < terminal_ids.size(); i++) {
						condMap.put("terminal_id", terminal_ids.get(i));
						List<Map<String, Object>> terminalList = ms.selectList("Terminal", "selectByCondition",
								condMap);
						if (terminalList != null && terminalList.size() == 1) {
							allTerminals.add(terminalList.get(0));
						}
					}
				}
				
				if (allTerminals != null) {
					Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
					for (int i = 0; i < allTerminals.size(); i++) {
						if(stream_type !=2 && 2==getTerminalType((String) allTerminals.get(i).get("terminal_id"))) {//音频终端不能播放视频
							continue;
						}
						String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) allTerminals.get(i).get("terminal_id"));
						if (null != onlineInfo) {
							String state = onlineInfo.split(":")[1];
							if (Integer.parseInt(state) > 0) {
								int sequece = terminalStartPullMode((String) allTerminals.get(i).get("ip"),broadcast_url, jsonBody);
								if (sequece > 0) {
									 sequeceMap.put(sequece, "");
								}
							}
						}
					}

					Map<Integer, JSONObject> RespMap = SocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);// 超时２秒
					if (RespMap != null && RespMap.size() > 0) {
						for (Integer key : RespMap.keySet()) {// key 是sequece
							String terminalRespStr = RespMap.get(key).getString("resp");
							String terminalIP = RespMap.get(key).getString("terminal_ip");
							JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
							int status = (int) terminalRespJson.get("status");
							if (status == 1) {
								RedisUtils.addMember(broadcast_url, streamSession, 0);
								pull_terminalIPs.add(terminalIP);
								result = 1;
							}
						}
						retInfof.put("push_url", broadcast_url);// 相对前端来说是push
						jsonBody.put("pull_url", broadcast_url); // 对于终端来说是pull
						jsonBody.put("pull_terminalIPs", pull_terminalIPs);
						retInfof.put("stream_session", streamSession);
						// jsonBody.putAll(retInfof);

						RedisUtils.hset(streamSession, jsonBody, 0);// 0是不超时

						RedisUtils.addMember(streamSesKey, streamSession, 0);// 维持在线有多少个视频会话
						RedisUtils.setExpire("online:" + streamSession, streamSession, expireSec);
					}
				}
			} else {
				if (stopStreamSession(streamSession) == true) {
					result = 1;
				}
			}

			if (result == 1) {
				if (action.equals("start")) {
					restList.add(retInfof);
					respJson.put("result", restList);
				} else {
					respJson.put("msg", Constant.SuccessMsg);
				}

				respJson.put("status", Constant.SUCCESS);

				// 下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionVideoBroadcast);
				JSONObject content = new JSONObject();
				if (action.equals("start")) {
					content.put("action_name", "启动广播");
				} else {
					content.put("action_name", "停止广播");
				}
				content.put("terminal_ids", terminal_ids);
				actionLog.put("action_content", content.toJSONString());
				es.insert("UserLog", "insert", actionLog);

			} else if (result == 0) {
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
	 * 开始请求对讲、切换对讲，结束对讲接口
	 * 
	 * @param
	 * @throws IOException
	 */
	@RequestMapping(value = "/talk", method = RequestMethod.POST)
	public void terminalVideoTalk(HttpServletRequest request, HttpServletResponse response) {

		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false);
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			JSONArray terminal_ids = jsonBody.getJSONArray("terminal_ids");
			String start_terminal = jsonBody.getString("talk_terminal");
			int needAgency = jsonBody.getIntValue("need_agency");
			String action = jsonBody.getString("action"); // start stop
			String broadcast_url = jsonBody.getString("broadcast_url");
			JSONArray gids = jsonBody.getJSONArray("gids");
			int stream_type = jsonBody.getIntValue("stream_type"); //1 音视频,2音频,3视频

			if (needAgency == 1) { // 如果需要代理
				broadcast_url = buildRtmpUrl(adminUser, null);
			}
			Map<String, Object> condMap = new HashMap<String, Object>();
			JSONObject retInfof = new JSONObject();

			JSONArray pull_terminalIPs = new JSONArray();
			JSONArray push_terminalIPs = new JSONArray();

			String streamSession = jsonBody.getString("stream_session");
			if (action.equals("start")) {
				if (streamSession == null || streamSession.length() == 0) { // 开始
					streamSession = buildStreamSes();
				} else {// 切换
					String tmpTerminal = stopTerminalTalk(streamSession);// 停止当前正在对讲的终端，如果没有对讲则此函数返回null
					if (tmpTerminal != null && tmpTerminal.length() > 0) {
						// terminal_ids.add(tmpTerminal); //转入广播状态 只停止推流，不停止拉流
					}
				}

				List<Map<String, Object>> allTerminals = null;

				JSONObject startRetJson = startTerminalTalk(adminUser, start_terminal, needAgency, broadcast_url);
				if (startRetJson != null) {				
					push_terminalIPs.add(startRetJson.getString("terminal_ip"));
					RedisUtils.addMember(startRetJson.getString("push_url"), streamSession, 0);
					retInfof.put("pull_url", startRetJson.getString("push_url")); // 相对前端来说是pull
					jsonBody.put("push_url", startRetJson.getString("push_url")); // 对于终端来说是push
					pull_terminalIPs.add(startRetJson.getString("terminal_ip"));

					RedisUtils.addMember(broadcast_url, streamSession, 0);// 内部对null进行处理
					if (gids != null && gids.size() == 1 && gids.get(0).equals("all-group")) {
						condMap.clear();
						allTerminals = ms.selectList("Terminal", "selectAll", condMap);
					} else if (gids != null && gids.size() > 0) {
						for (int i = 0; i < gids.size(); i++) {
							condMap.put("gid", gids.get(i));
							allTerminals = (List<Map<String, Object>>) ms.selectList("Terminal", "selectByCondition",condMap);
						}
					}

					if (terminal_ids != null) {
						if (allTerminals == null) {
							allTerminals = Lists.newArrayList();
						}
						for (int i = 0; i < terminal_ids.size(); i++) {
							condMap.put("terminal_id", terminal_ids.get(i));
							List<Map<String, Object>> terminalList = ms.selectList("Terminal", "selectByCondition",condMap);
							if (terminalList != null && terminalList.size() == 1) {
								allTerminals.add(terminalList.get(0));
							}
						}
					}

					if (allTerminals != null && allTerminals.size() > 0) {
						Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
						for (int i = 0; i < allTerminals.size(); i++) {
							if(stream_type !=2 && 2==getTerminalType((String) allTerminals.get(i).get("terminal_id"))) {//音频终端不能播放视频
								continue;
							}
							String onlineInfo = RedisUtils.get(Constant.OnlineTerminals + ":" + (String) allTerminals.get(i).get("terminal_id"));
							if (null != onlineInfo) {
								String state = onlineInfo.split(":")[1];
								if (Integer.parseInt(state) > 0) {
									int sequece = terminalStartPullMode((String) allTerminals.get(i).get("ip"),broadcast_url, jsonBody);
									if (sequece > 0) {
										 sequeceMap.put(sequece,"");
									}
								}
							}
						}
						Map<Integer, JSONObject> RespMap = SocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);// 超时２秒
						if (RespMap != null && RespMap.size() > 0) {
							for (Integer key : RespMap.keySet()) {// key 是sequece
								String terminalRespStr = RespMap.get(key).getString("resp");
								String terminalIP = RespMap.get(key).getString("terminal_ip");
								JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
								int status = (int) terminalRespJson.get("status");
								if (status == 1) {
									RedisUtils.addMember(broadcast_url, streamSession, 0);
									RedisUtils.refreshExpired(broadcast_url, expireSec);
									pull_terminalIPs.add(terminalIP);
									result = 1;
								}
							}
						}
					}
					
					retInfof.put("push_url", broadcast_url);// 相对前端来说是push
					jsonBody.put("pull_url", broadcast_url); // 对于终端来说是pull
					jsonBody.put("pull_terminalIPs", pull_terminalIPs);
					jsonBody.put("push_terminalIPs", push_terminalIPs);
					retInfof.put("stream_session", streamSession);
					// jsonBody.putAll(retInfof);

					RedisUtils.hset(streamSession, jsonBody, 0);// 0是不超时

					RedisUtils.addMember(streamSesKey, streamSession, 0);// 维持在线有多少个视频会话
					RedisUtils.setExpire("online:" + streamSession, streamSession, expireSec);
					result = 1;
				}

			} else {
				if (stopStreamSession(streamSession) == true) {
					result = 1;
				}
			}

			JSONArray restList = new JSONArray();
			if (result == 1) {
				restList.add(retInfof);
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", restList);

				// 下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionVideoTalk);
				JSONObject content = new JSONObject();
				if (action.equals("start")) {
					content.put("action_name", "启动对讲");
				} else {
					content.put("action_name", "停止对讲");
				}
				content.put("terminal_ids", terminal_ids);
				content.put("talk_terminal", start_terminal);
				actionLog.put("action_content", content.toJSONString());
				es.insert("UserLog", "insert", actionLog);
			} else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", Constant.FailedMsg);
			}
		} else if(result==-1) {
			respJson.put("status", Constant.FAILED);
			respJson.put("msg", "该终端在播流中，请等候操作！");
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);
		}

		HttpIO.writeResp(response, respJson);
	}

	/**
	 * 开始调取或者终止实时视频
	 * 
	 * @param
	 * @throws IOException
	 */
	@RequestMapping(value = "/realtime_url", method = RequestMethod.POST)
	public void getRealTimeVideoUrl(HttpServletRequest request, HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		String remoteIP = request.getRemoteHost();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request, es, false); // 同一个用户禁止多端登录
		if (adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);
			String terminalId = jsonBody.getString("terminal_id");
			int needAgency = jsonBody.getIntValue("need_agency");
			String action = jsonBody.getString("action");
			String streamSession = jsonBody.getString("stream_session"); // action为start时生成,为stop时携带
			int sendTerminalCmd = cmds.TERMINAL_DIRECT_VIDEO;
			if (needAgency == 0) {
				sendTerminalCmd = cmds.TERMINAL_DIRECT_VIDEO;
			} else {
				sendTerminalCmd = cmds.SERVER_AGENCY_VIDEO;
			}

			Map<String, Object> condMap = new HashMap<String, Object>();
			//SocketMsgHandler newSocketHandler = new SocketMsgHandler();
			JSONArray restList = new JSONArray();
			JSONObject retInfof = new JSONObject();

			if (action.equals("start") && terminalId != null && terminalId.length() > 0
					&& (streamSession == null || streamSession.length() == 0)) {
				condMap.put("terminal_id", terminalId);
				List<Map<String, Object>> terminalList = ms.selectList("Terminal", "selectByCondition", condMap);
				if (terminalList != null && terminalList.size() == 1) {
					JSONObject socketBody = new JSONObject();
					String terminalResp = null;
					streamSession = buildStreamSes();
					String pushUrl = null;
					socketBody.put("action", "start");
					if (needAgency == 1) {
						socketBody.put("push_url", pushUrl);
						pushUrl = buildRtmpUrl(null, (String) terminalList.get(0).get("terminal_id"));
					}
//						pushUrl = terminalStartPushMode((String) terminalList.get(0).get("ip"),pushUrl,needAgency);
					Map<Integer,String> sequeceMap = new HashMap<Integer,String> ();
					int sequece = terminalStartPushMode((String) terminalList.get(0).get("ip"), pushUrl, needAgency);
					 sequeceMap.put(sequece, "");
					Map<Integer, JSONObject> RespMap = SocketMsgHandler.getTerminalRespBySequece(sequeceMap, 2000);// 超时２秒
					if (RespMap != null && RespMap.size() > 0) {
						for (Integer key : RespMap.keySet()) {// key 是sequece
							String terminalRespStr = RespMap.get(key).getString("resp");
							String terminalIP = RespMap.get(key).getString("terminal_ip");
							JSONObject terminalRespJson = JSONObject.parseObject(terminalRespStr);
							int status = (int) terminalRespJson.get("status");
							if (status == 1) {
								if (needAgency == 0) {
									JSONObject terminalResult = terminalRespJson.getJSONObject("result");
									pushUrl = (String) terminalResult.get("push_url");
								}
							}
						}
					}

					if (pushUrl != null) {
						retInfof.put("pull_url", pushUrl); // 平板可以从此地址获取直播流
						retInfof.put("stream_session", streamSession);
						restList.add(retInfof);
						jsonBody.put("push_url", pushUrl);
						JSONArray pull_terminalIPs = new JSONArray();
						pull_terminalIPs.add((String) terminalList.get(0).get("ip"));
						jsonBody.put("push_terminalIPs", pull_terminalIPs);
						RedisUtils.setExpire("online:" + streamSession, streamSession, expireSec);
						RedisUtils.hset(streamSession, jsonBody, 0);
						RedisUtils.addMember(pushUrl, streamSession, 0);
						RedisUtils.addMember(streamSesKey, streamSession, 0);// 维持在线有多少个视频会话
						result = 1;
					}
				}

			} else {
				if (stopStreamSession(streamSession) == true) {
					result = 1;
				}
			}
			if (result == 1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", restList);
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
