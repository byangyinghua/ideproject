package bzl.controller;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import groovy.util.logging.Log4j;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

import bzl.common.Constant;
import bzl.common.MemoryCache;
import bzl.common.SesCheck;
import bzl.entity.LoginLog;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import utils.*;
import sun.rmi.log.LogHandler;

/*管理员账号操作controller*/

@Controller
@RequestMapping("/account")
public class AccountController {
	private static int loginExpiredSec = 12*3600;

	// private static Logger logger = Logger.getLogger(UserController.class);
	Logger log = Logger.getLogger(LogHandler.class);
	@Autowired
	private MapService ms;
	@Autowired
	private EntityService es;
	
	private static MemoryCache localMemCache = new MemoryCache();
 
	Color getRandColor(int fc, int bc) {// 给定范围获得随机颜色 用户验证码
		Random random = new Random();
		if (fc > 255)
			fc = 255;
		if (bc > 255)
			bc = 255;
		int r = fc + random.nextInt(bc - fc);
		int g = fc + random.nextInt(bc - fc);
		int b = fc + random.nextInt(bc - fc);
		return new Color(r, g, b);
	}

    @RequestMapping(value="/test",method=RequestMethod.GET)
	public void testRouter(HttpServletRequest request, HttpServletResponse response) {
    	
    	System.out.println("run into get_yzm !!!!");
    	JSONObject respJson = new JSONObject();
    	respJson.put("test_value", "这是测试router");
   		HttpIO.writeResp(response, respJson);
    }
	
    @RequestMapping(value="/getLocalIP",method=RequestMethod.POST)
	public void getLocalIP(HttpServletRequest request, HttpServletResponse response) {
    	JSONObject respJson = new JSONObject();
    	
    	String remoteIP=request.getRemoteAddr();
    	
    	respJson.put("status", Constant.SUCCESS);
		respJson.put("local_ip",remoteIP);
   		HttpIO.writeResp(response, respJson);
    }

	/**
	 * 具体获取验证码的方法
	 *  time
	 *            time为时戳,这样的话可以避免浏览器缓存验证码
	 * @throws IOException
	 */
    @RequestMapping(value="/get_yzm/{timestamp}",method=RequestMethod.GET)
	public void getLoginYanzhenma(HttpServletRequest request, HttpServletResponse response){
//    	    char[] codeSequence = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
//    			'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
//    			'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
		    // 在内存中创建图象
    
		    int width = 60, height = 20;    
		    BufferedImage image = new BufferedImage(width, height,BufferedImage.TYPE_INT_RGB);    
		    // 获取图形上下文    
		    Graphics g = image.getGraphics();   
		    //生成随机类    
		    Random random = new Random();    
		    // 设定背景色    
		    g.setColor(getRandColor(200, 250));    
		    g.fillRect(0, 0, width, height);    
		    //设定字体    
		    g.setFont(new Font("Times New Roman", Font.PLAIN, 18));  
		    //画边框    
		    //g.setColor(new Color());    
		    //g.drawRect(0,0,width-1,height-1);    
		    // 随机产生100条干扰线，使图象中的认证码不易被其它程序探测到    
		    g.setColor(getRandColor(160, 200));    
		    for (int i = 0; i < 100; i++)    
		    {    
		        int x = random.nextInt(width);    
		        int y = random.nextInt(height);    
		        int xl = random.nextInt(12);    
		        int yl = random.nextInt(12);    
		        g.drawLine(x, y, x + xl, y + yl);    
		    }    
		    // 取随机产生的认证码(4位数字)    
		    String sRand = "";    
		    for (int i = 0; i < 4; i++)    
		    {    
		        String rand = RandomStringUtils.randomAlphanumeric(1);    
		        sRand += rand;    
		        // 将认证码显示到图象中    
		        g.setColor(new Color(20 + random.nextInt(110), 20 + random    
		                .nextInt(110), 20 + random.nextInt(110)));// 调用函数出来的颜色相同，可能是因为种子太接近，所以只能直接生成    
		        g.drawString(rand, 13 * i + 6, 16);    
		    }    
		    //将验证码存入缓存中
		    localMemCache.setData(sRand.toLowerCase(), sRand.toLowerCase(), 60);
		    
		    // 图象生效    
		    g.dispose();    
		    // 输出图象到页面    
		    ServletOutputStream out;
			try {
				out = response.getOutputStream();
				ImageIO.write(image, "JPEG", out);   
				// 7.禁止图像缓存。
				response.setHeader("Pragma", "no-cache");
				response.setHeader("Cache-Control", "no-cache");
				response.setDateHeader("Expires", 0);
				response.setContentType("image/jpeg");

				// 8.关闭sos
				out.flush();
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  //  out = pageContext.pushBody();   
	}
    
    /*管理页面用户登录接口 userlogin */
	
	@RequestMapping(value="/userlogin",method=RequestMethod.POST)
	@ResponseBody
	public void userlogin(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		String jsonBodyStr = HttpIO.getBody(request);
		JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);  
		String userName = jsonBody.getString("username");
		String password = jsonBody.getString("password");
//		String yzm = jsonBody.getString("yzm").toLowerCase();
		String forceLogin =  jsonBody.getString("force_login");//使用强制登录将会踢掉其他用户
//		String realYzm = localMemCache.getData(yzm);
		String passwordMD5 = EncryptionUtil.md5Hex(userName + password + Constant.loginSalt);
		String oldIP = SesCheck.checkSessionCache(request,passwordMD5);
		if((forceLogin==null || forceLogin.length() ==0) && oldIP.length() >0){
			respJson.put("status", Constant.UserHasLogin);
			respJson.put("msg","该用户已经在"+oldIP + "登录!");
		}
//		else if(null ==realYzm ||yzm==null||(!yzm.equals(realYzm) && !yzm.toLowerCase().equals(realYzm.toLowerCase()))) {
//			respJson.put("status",Constant.FAILED);// 0为成功，其他为失败
//			respJson.put("msg","验证码错误或者已经过期!");
//			localMemCache.clear(yzm);
//		}
		else {
//			localMemCache.clear(yzm);
			User user = new User();
			user.setPassword(passwordMD5);
			user.setUsername(userName);
			List<User> list = es.select("User", "selectByCondition", user);
			User us = null;
			if (list != null && list.size() > 0) {
				HttpSession session = request.getSession();
				us = list.get(0);
				try {
					JSONObject sesJson = new JSONObject();
					sesJson.put("username", userName);
					sesJson.put("password", password);
					sesJson.put("remoteip", request.getRemoteAddr());
					sesJson.put("timestamp", new Date().getTime());
					sesJson.put("issupper", us.getIs_supper());
					String userInfos = EncryptionUtil.encryptAES(sesJson.toJSONString());
					session.setAttribute("usession", passwordMD5);
					
					// 记下登录日志
				    LoginLog tmpLog = new LoginLog();
				    tmpLog.setUid(us.getUid());
				    tmpLog.setUsername(us.getUsername());
				    tmpLog.setRealname(us.getReal_name());
				    tmpLog.setLogin_ip(request.getRemoteAddr());
				    tmpLog.setLogin_time( new Date());
				    tmpLog.setLogin_type("login");
				    int result =es.insert("LoginLog", "insert", tmpLog);
				    if(result !=1) {
				    	log.error("新增用户登录信息失败:");
				    	log.error(tmpLog);
				    }else {
//				    	RedisUtils.setExpire(Constant.UserSession + passwordMD5, userInfos, loginExpiredSec);//存储用户登录信息
						//改为登录长期有效
						RedisUtils.set(Constant.UserSession + passwordMD5, userInfos);
				    }
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg","登录成功!");
			} else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "用户名或者密码错误!");
			}
		}

		HttpIO.writeResp(response, respJson);
	}
	
	//用户登出接口
	@RequestMapping(value="/logout",method=RequestMethod.POST)
	@ResponseBody
	public void userLoginOut(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		HttpSession session = request.getSession();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false);
		if(adminUser != null) {
			String oldIP = SesCheck.checkSessionCache(request,adminUser.getPassword());
			if(oldIP.length() ==0) {
				RedisUtils.del(Constant.UserSession + adminUser.getPassword());
			}
			respJson.put("status", Constant.SUCCESS);
			respJson.put("msg", Constant.SuccessMsg);
			// 记下登出日志
		    LoginLog tmpLog = new LoginLog();
		    tmpLog.setUid(adminUser.getUid());
		    tmpLog.setUsername(adminUser.getUsername());
		    tmpLog.setRealname(adminUser.getReal_name());
		    tmpLog.setLogin_ip(request.getRemoteAddr());
		    tmpLog.setLogin_time( new Date());
		    tmpLog.setLogin_type("logout");
		    es.insert("LoginLog", "insert", tmpLog);
		    session.removeAttribute("usession");
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);	
		}

		HttpIO.writeResp(response, respJson);
	}
	
	
	@RequestMapping(value="/getUserInfo",method=RequestMethod.POST)
	@ResponseBody
	public void getLoginUserInfo(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
	// HttpSession session = request.getSession();
	//	int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, false);
		if(adminUser != null) {
			JSONArray userResult= new JSONArray();
			userResult.add(adminUser);
			respJson.put("status", Constant.SUCCESS);
			respJson.put("result", userResult);
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);	
		}
	    HttpIO.writeResp(response, respJson);
	}
	
	@RequestMapping(value="/allUserList",method=RequestMethod.POST)
	@ResponseBody
	public void getAllUserList(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		HttpSession session = request.getSession();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, true);
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			int page = jsonBody.getIntValue("page");
			int pagesize = jsonBody.getIntValue("pagesize");
			String getTotal = jsonBody.getString("getTotal");
			
			Map<String,Object> condMap = new HashMap<String,Object>();
			List<Map<String,Object>> userList=null;
			if(getTotal !=null) {
				 userList= ms.selectList("User", "selectCountByCondition", condMap);
				if(userList !=null && userList.size()==1) {
					respJson.put("total", userList.get(0).get("count"));
				}
			}
			
			condMap.put("startrom", (page-1)*pagesize);
			condMap.put("pagesize",pagesize);
			
			JSONArray retTaskList = new JSONArray();
			userList  = ms.selectList("User", "selectByConditionWithPage", condMap);
			if(userList != null && userList.size() > 0) {
				userList = Convert.SortDataListId(userList,page,pagesize);
				respJson.put("status", Constant.SUCCESS);
				respJson.put("result", userList);
			}else {
				respJson.put("status", Constant.notExistStatus);
				respJson.put("result", Constant.NodataErr);
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("result", Constant.PermissionErr);
		}
		   HttpIO.writeResp(response, respJson);
	}
	
	
	
	@RequestMapping(value="/modifypwd",method=RequestMethod.POST)
	@ResponseBody
	public void modifypwd(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		HttpSession session = request.getSession();
		int result = 0;
		User updateUser = SesCheck.getUserBySession(request,es, false);
		if(updateUser !=null) {
			//User updateUser = list.get(0);
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr);  
			String oldPwdMd5 = EncryptionUtil.md5Hex(updateUser.getUsername() + jsonBody.getString("oldpwd") + Constant.loginSalt);
			if(oldPwdMd5.contentEquals(updateUser.getPassword())) {
				updateUser.setPassword(EncryptionUtil.md5Hex(updateUser.getUsername() + jsonBody.getString("newpwd") + Constant.loginSalt));
				result=es.update("User", "update", updateUser);	
			}
			if(result==1){
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", "密码更改成功！");
				//下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", updateUser.getUid());
				actionLog.put("username", updateUser.getUsername());
				actionLog.put("realname", updateUser.getReal_name());
				actionLog.put("action_type", Constant.ActionMod);
				
				JSONObject content = new JSONObject();
				content.put("action_name", "修改密码");
				actionLog.put("action_content",content.toJSONString());
				es.insert("UserLog", "insert", actionLog);
				
				session.setAttribute("usession", "");
			}
			else{
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "密码更改失败,请确认旧密码是否正确！");	
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);	
		}
	    HttpIO.writeResp(response, respJson);
	}
	
	@RequestMapping(value="/mod_userinfo",method=RequestMethod.POST)
	@ResponseBody
	public void modifyUserInfo(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User updateUser = SesCheck.getUserBySession(request,es, false);
		if(updateUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String uid = jsonBody.getString("uid");
			if(uid.length() > 0) {
				updateUser.setUid(uid);
				updateUser.setIdentity_num(jsonBody.getString("identity_num"));
				updateUser.setReal_name(jsonBody.getString("real_name"));
				updateUser.setPhone_num(jsonBody.getString("phone_num"));
				updateUser.setQq(jsonBody.getString("qq"));
				updateUser.setWeixin(jsonBody.getString("weixin"));
				updateUser.setMail(jsonBody.getString("mail"));
				result=es.update("User", "update", updateUser);	
			}
			if(result==1){
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", "修改用户信息成功！");
			}
			else{
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "更改用户信息失败！");	
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);	
		}
	    HttpIO.writeResp(response, respJson);
	}
	
	//新增管理员账号
	@RequestMapping(value="/add_user",method=RequestMethod.POST)
	@ResponseBody
	public void addUser(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 1;
		User adminUser = SesCheck.getUserBySession(request,es, true); //只有超级管理员才能新增账号
		if(adminUser !=null) {
			User newUser = new User();
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			String username = jsonBody.getString("username");
			//String realname = jsonBody.getString("realname");
			String password = jsonBody.getString("password");
			if(username.length() == 0||password.length()==0) {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "填写的信息不完整！");	
			}else {
				newUser.setUid("uid" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(10));
				newUser.setUsername(username);
				//newUser.setReal_name(realname);
				newUser.setPassword(EncryptionUtil.md5Hex(newUser.getUsername() + password + Constant.loginSalt));
				newUser.setIs_supper(0);
				newUser.setCreate_time(new Date());
				result = es.insert("User", "insert", newUser);
				if(result==1){
					respJson.put("status", Constant.SUCCESS);
					respJson.put("msg", "增加用户成功！");
					
					//下面添加用户操作日志
					JSONObject actionLog = new JSONObject();
					actionLog.put("uid", adminUser.getUid());
					actionLog.put("username", adminUser.getUsername());
					actionLog.put("realname", adminUser.getReal_name());
					actionLog.put("action_type", Constant.ActionAdd);
					
					JSONObject content = new JSONObject();
					content.put("action_name", "新增用户");
					content.put("username",  newUser.getUsername());
					content.put("uid",  newUser.getUid());
					actionLog.put("action_content",content.toJSONString());
					es.insert("UserLog", "insert", actionLog);
				}
				else{
					respJson.put("status", Constant.FAILED);
					respJson.put("msg", "增加用户失败,不能使用曾经注册过的用户名!");	
				}
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);	
		}
		
		 HttpIO.writeResp(response, respJson);
	}
	
	//删除管理员，只能是超级管理员操作
	@RequestMapping(value="/del_user",method=RequestMethod.POST)
	@ResponseBody
	public void delUser(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject respJson = new JSONObject();
		int result = 0;
		User adminUser = SesCheck.getUserBySession(request,es, true); //只有超级管理员才能新增账号
		if(adminUser != null) {
			String jsonBodyStr = HttpIO.getBody(request);
			JSONObject jsonBody = JSONObject.parseObject(jsonBodyStr); 
			JSONArray user_ids = jsonBody.getJSONArray("user_ids");
			Map<String,Object> condMap = new HashMap<String,Object>();
			user_ids.remove(adminUser.getUid());//防止用户删除自己
			if(user_ids != null && user_ids.size() >0){
				condMap.put("uids", user_ids);
				condMap.put("del_status", 1);//1表示删除
				result = ms.execute("User", "updateStatus", condMap);
			}
			if(result ==1) {
				respJson.put("status", Constant.SUCCESS);
				respJson.put("msg", "删除用户成功！");
				//下面添加用户操作日志
				JSONObject actionLog = new JSONObject();
				actionLog.put("uid", adminUser.getUid());
				actionLog.put("username", adminUser.getUsername());
				actionLog.put("realname", adminUser.getReal_name());
				actionLog.put("action_type", Constant.ActionDel);
				JSONObject content = new JSONObject();
				content.put("action_name", "删除用户");
				content.put("userlist",  user_ids);
				actionLog.put("action_content",content.toJSONString());
				es.insert("UserLog", "insert", actionLog);
			}else {
				respJson.put("status", Constant.FAILED);
				respJson.put("msg", "删除用户失败！");	
			}
		}else {
			respJson.put("status", Constant.UserNotLogin);
			respJson.put("msg", Constant.PermissionErr);	
		}
		 HttpIO.writeResp(response, respJson);
	}
}
