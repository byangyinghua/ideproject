package bzl.common;


public class Constant {
	
	public final static String loginSalt = "4Lm89ZkhornzpjEXGi9";
	public final static String UserSession ="online:user:";
	
	//给前端返回成功和失败常量
	public final static int SUCCESS = 0;
	public final static int FAILED = 1;
	public final static int notExistStatus =2;//相关数据不存在
	public final static int UserNotLogin =3; //用户未登录
	public final static int UserHasLogin = 4;//用户已经在别处登录
	public final static String PermissionErr = "无权限操作";
	public final static String MsgNotLogin = "未登录";
	public final static String NodataErr = "当前没有任何数据";
	public final static String SuccessMsg = "操作成功!";
	public final static String FailedMsg = "操作失败!";
	public final static String ParemeterErr = "参数错误!";
	
	//用户操作动作常量
	public final static String ActionAdd = "add"; // 增加操作
	public final static String ActionDel = "del"; // 删除操作
	public final static String ActionMod = "modify"; // 修改操作
	public final static String ActionUpload = "upload"; // 修改操作
	public final static String ActionSend = "sendtask"; // 下发任务
	public final static String ActionStop = "stoptask"; // 停止任务
	public final static String ActionClean = "cleantask"; // 清除终端任务
	
	public final static String ActionVideoBroadcast = "broadcast"; // 广播
	public final static String ActionVideoTalk = "talk"; // 对讲
	
	//预案类型
	public final static String normalTask = "normal";
	public final static String examTask = "exam"; //考试预案任务
	public final static String urgencyTask = "urgency"; //紧急预案
	
	//任务类型常量
	public final static int TextTask = 1;
	public final static int ImageTask = 2;
	public final static int AudioTask = 3;
	public final static int VideoTask = 4;
	
	
	//redis　app升级信息管理key
	public final static String AppUpdateInfo = "app:update:info";
	public final static String UpdateOkTerminals = "app:update:success_terminals";
	public final static String UpdateFailedTerminals = "app:update:failed_terminals";
	
	//服务器升级管理信息
	public final static String ServerUpdateInfo = "server:update:info";
	
	//设备在线管理
	public final static String OnlineTerminals = "online:terminals";
	
	//任务状态以及发送到的终端管理
	//public final static String TaskSentTerminals = "task:sentterminal:";
	public final static String TaskTerminalReady = "task:readyterminal:";//设置任务终端，未下发
	public final static String TaskSentTerminalsOK = "task:sentterminal:ok:"; //任务发送成功的终端列表
	public final static String TaskSentTerminalsFailed = "task:sentterminal:failed:";//任务发送失败的终端列表
	
	//public final static String UrgencyTaskTerminals = "urgency:terminals:";
	
	
	//推流播放相关缓存
	public final static String LivePlayTerminals = "live:play:terminal_ids:";
	public final static String LivePlaySwitch = "live:play:switch:";
	public final static String LivePlayInfo = "live:play:info:";
	public final static String LivePlayingTerminals = "live:playing:terminals";

	//live play status 
	public final static String NotStartPlay = "0";
	public final static String WaitForPlay = "1";
	public final static String IsPlaying = "2";
	public final static String IsStopPlay = "3";
	
	//media type const //1是音视频，2是音频，3是视频
	public final static int MediaAudioVideo = 1;
	public final static int MediaAudio = 2;
	public final static int MediaVideo = 3;
	
	


}