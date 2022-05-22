package bzl.common;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


import com.alibaba.fastjson.JSONObject;

import bzl.entity.User;
import bzl.service.EntityService;
import utils.EncryptionUtil;
import utils.RedisUtils;

public class SesCheck {
	private static int loginExpiredSec = 12*3600;
	//private static EntityService es = new EntityService();
	
	public static String checkSessionCache(HttpServletRequest request,String pwdString) {
		JSONObject userInfo;
		String userInfoStr;
		try {
			String usession = RedisUtils.get(Constant.UserSession + pwdString);
			System.out.println("checkSessionCache is usession＝＝＝"+usession);
			userInfoStr = EncryptionUtil.decryptAES(usession);
			System.out.println("checkSessionCache is usession4444＝＝＝"+usession);
			if(usession==null||userInfoStr ==null) {
				System.out.println("userInfoStr is null!!!");
				return "";
			}
			String userIP = request.getRemoteAddr();
			System.out.println("checkSessionCache is userInfoStr＝＝＝"+userInfoStr);
			userInfo = JSONObject.parseObject(userInfoStr);
			System.out.println("checkSessionCache is usession2222＝＝＝"+usession);
			String oldUserIP = userInfo.getString("remoteip");
			//long timeStamp = (long) userInfo.get("timestamp");
			//User us = new User();
			//String passwordMd5 = EncryptionUtil.md5Hex(userInfo.getString("username") + userInfo.getString("password"));
			if(!userIP.equals(oldUserIP)) { //超时或者IP不一致
				return oldUserIP;
			}else {
				return "";
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		return "";
	}
	
	public static  User getUserBySession(HttpServletRequest request,EntityService es,boolean checkIsSupper) {
		HttpSession session = request.getSession();
		String usession =(String) session.getAttribute("usession"); //获取用户登录session
		JSONObject userInfo;
		try {
			String userInfoEncrypt = RedisUtils.get(Constant.UserSession+usession);
			String userInfoStr = EncryptionUtil.decryptAES(userInfoEncrypt);
			if(userInfoStr ==null) {
				System.out.println("userInfoStr is null!!!");
				return null;
			}
			String userIP = request.getRemoteAddr();
			userInfo = JSONObject.parseObject(userInfoStr);
			long timeStamp = (long) userInfo.get("timestamp");
			String oldUserIP = userInfo.getString("remoteip");
			User us = new User();
			String passwordMd5 = EncryptionUtil.md5Hex(userInfo.getString("username") + userInfo.getString("password") + Constant.loginSalt);
			if((new Date().getTime() - timeStamp)/1000 > loginExpiredSec||!userIP.equals(oldUserIP)) { //超时或者IP不一致
				session.removeAttribute("usession");
				return null;
			}

			us.setUsername(userInfo.getString("username"));
			us.setPassword(passwordMd5);
			if(checkIsSupper == true) {
				us.setIs_supper(userInfo.getIntValue("issupper"));
			}
			List<User> list = es.select("User", "selectByCondition", us);
			if (list != null && list.size() == 1) {
				return list.get(0);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
		    System.out.println("read user session failed!" + e.toString());
			e.printStackTrace();
		}
		return null;
	}
	
	public static Map<String,Object> getPathParameter(String url){
		
		Map<String,Object> map = new HashMap<String,Object>();
		System.out.println(url);
        url = url.replace("?",";");
        if (!url.contains(";")){
             return map;
        }
        if (url.split(";").length > 0){
             String[] arr = url.split(";")[1].split("&");
             for (String s : arr){
                  String key = s.split("=")[0];
                  String value = s.split("=")[1];
                  map.put(key,value);
             }
             return  map;

        }else{
             return map;
        }
	}
}
	
	
	
	
	