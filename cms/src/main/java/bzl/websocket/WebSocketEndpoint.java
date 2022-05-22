package bzl.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import bzl.common.Constant;
import bzl.common.MemoryCache;
import bzl.controller.UserGroupController;
import bzl.controller.VideoController;
import groovy.util.logging.Slf4j;
import utils.EncryptionUtil;
import utils.RedisUtils;


@ServerEndpoint(value = "/websocket.do",configurator=WebSocketHttpSession.class)
@Component
@Slf4j
//@ServerEndpoint(value = "/websocket.do")
public class WebSocketEndpoint {
	public static int REFRESH_STREAM_SES = 0X01; //网页或者app刷新流媒体播放session
	public static int PUSH_MSG_SES = 0X02;       //服务器推送通知消息
	private static int onlineCount = 0;
	private static MemoryCache localMemCache = new MemoryCache();
	private Session session;
	private String username;
	private String clientIP;
	private JSONArray streamSesList;
	private String userSession = "";
	
	//修改websocket获取不到用户登录session的问题
	@OnOpen
	public void onOpen(Session session,EndpointConfig config) throws IOException {
		this.session = session;
		HttpSession httpSession= (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
		if(httpSession!=null) {
			this.userSession =(String) httpSession.getAttribute("usession");
		}
    	InetSocketAddress remoteAddress = WebsocketUtil.getRemoteAddress(session);
    	this.clientIP = remoteAddress.getAddress().getHostName();
		localMemCache.setData(session.getId(), this, 0);
		this.onlineCount++;
		System.out.println("\n\n!!websocket this.session.getId() 已连接 IP=" + this.clientIP + ":userSession=" + this.userSession);
	}

	@OnClose
	public void onClose() throws IOException {
		localMemCache.clear(this.session.getId());
		this.onlineCount--;
		this.session = null;
		System.out.println("\n\n!!websocket this.session.getId() 已关闭 IP=" + this.clientIP);
		
		if(streamSesList !=null) {
			for(int i=0;i<this.streamSesList.size();i++) {
				String tmpStreamSes = (String) streamSesList.get(i);
				if(tmpStreamSes !=null && tmpStreamSes.length() >0) {
					VideoController.refreshLiveStreamSes(tmpStreamSes);
				}else {
					System.out.println("err refresh tmpStreamSes is null!");
					//VideoController.delLiveStreamSes(tmpStreamSes);
				}
			}
		}
	}

	@OnMessage
	public void onMessage(String message, Session session) throws IOException, EncodeException {
		//System.out.println("\n\n!!websocket message　＝"+message);
		JSONObject webParamJson = JSONObject.parseObject(message);
		int msgCmd = webParamJson.getIntValue("cmd");
		String IP = webParamJson.getString("ip");
		this.username = webParamJson.getString("username");
		JSONObject body = webParamJson.getJSONObject("body");
		JSONObject msgBody = new JSONObject();
		//System.out.println("\n\n!!websocket this.session.getId() 已连接 IP=" + this.clientIP + ":userSession=" + userSession);
		 if(this.userSession!=null && this.userSession.length()>0) {
			 String userInfoEncrypt = RedisUtils.get(Constant.UserSession + userSession);
			 try {
				    if(userInfoEncrypt!=null) {
				    	String userInfoStr = EncryptionUtil.decryptAES(userInfoEncrypt);
						if(userInfoStr ==null) {
							System.out.println("onMessage userInfoStr is null!!!");
						}
						//System.out.println("\n\n!!websocket this.session.getId() 已连接 IP=" + this.clientIP + ":userInfoStr=" + userInfoStr);
						//JSONObject userInfo = JSONObject.parseObject(userInfoStr);
				    	RedisUtils.setExpire(Constant.UserSession + userSession, userInfoEncrypt, 12*3600);//存储用户登录信息
						 if(msgCmd==REFRESH_STREAM_SES) {//刷新流媒体播放维持链接
								streamSesList = body.getJSONArray("stream_ses_list");
								//System.out.println("\n\n!!websocket streamSesList　＝"+streamSesList.toJSONString());
								if(streamSesList != null && streamSesList.size() >0) {
									//刷新查看的video
									for(int i=0;i<streamSesList.size();i++) {
										String tmpStreamSes = (String) streamSesList.get(i);
										if(tmpStreamSes !=null && tmpStreamSes.length() >0) {
											VideoController.refreshLiveStreamSes(tmpStreamSes);
										}else {
											System.out.println("refresh tmpStreamSes is null!");
										}
									}
								}
							}
				    }
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
			}
		 }else {
			 msgBody.put("status", 1);
		 }
		
		webParamJson.clear();
		webParamJson.put("cmd", msgCmd);
		webParamJson.put("ip", IP);

		webParamJson.put("body", msgBody);
		
		WebSocketEndpoint mywebsocket = (WebSocketEndpoint)localMemCache.getData(session.getId());
		mywebsocket.session.getBasicRemote().sendText(webParamJson.toJSONString());
		//System.out.println("\n\n!!msg return 　＝"+webParamJson.toJSONString());
	}

	@OnError
	public void onError(Session session, Throwable error) {
		localMemCache.clear(session.getId());
		error.printStackTrace();
	}
	
	//修改用户接受不到求助推送消息的问题
	public static void pushMsg(int cmd,String serverIP,String fromTerminal,JSONObject msgBody) {
		Set<String> socketSesList = localMemCache.getAllDataKeys();
		JSONObject webParamJson = new JSONObject();
		webParamJson.put("cmd", cmd);
		webParamJson.put("ip", serverIP);
		webParamJson.put("body", msgBody);
		for(String sesId:socketSesList) {
			WebSocketEndpoint mywebsocket = (WebSocketEndpoint)localMemCache.getData(sesId);
			webParamJson.put("username", mywebsocket.username);
			String userSession = mywebsocket.userSession;
			 if(userSession!=null && userSession.length()>0) {
				 String userInfoEncrypt = RedisUtils.get(Constant.UserSession + userSession);
					String userInfoStr;
					String sendUserName ="admin";
					try {
						userInfoStr = EncryptionUtil.decryptAES(userInfoEncrypt);
						if(userInfoStr ==null) {
							System.out.println("onMessage userInfoStr is null!!!");
						}else {
							JSONObject userInfo = JSONObject.parseObject(userInfoStr);
							sendUserName = userInfo.getString("username");
						}
						boolean isSend = UserGroupController.checkHasPermission(sendUserName,null,fromTerminal);
						if(isSend) {
							mywebsocket.session.getBasicRemote().sendText(webParamJson.toJSONString());
						}else if(sendUserName.equals("admin")) {
							mywebsocket.session.getBasicRemote().sendText(webParamJson.toJSONString());
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			 }else {
					System.out.println("websocket pushMsg can not found user session!!");
			 }
		}
	}
}





