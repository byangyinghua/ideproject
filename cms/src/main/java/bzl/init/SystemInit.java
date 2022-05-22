package bzl.init;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bzl.entity.LocalProjectConfig;
import com.google.gson.Gson;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import bzl.common.Configure;
import bzl.common.Constant;
import bzl.controller.AccountController;
import bzl.entity.User;
import bzl.service.EntityService;
import bzl.service.MapService;
import bzl.service.impl.EntityServiceImpl;
import bzl.service.impl.MapServiceImpl;
import bzl.task.AdjustTimeTask;
import bzl.task.LivePlayTask;
import bzl.task.RtmpManagerTask;
import utils.EncryptionUtil;
import utils.Log;
import utils.RedisUtils;

public class SystemInit implements ApplicationContextAware{
	private static MapService ms = new MapServiceImpl();
	private static EntityService es = new EntityServiceImpl();
	private Gson gson = new Gson();
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// TODO Auto-generated method stub
		Configure.readAllConf(); //读取全局配置到内存中
		RedisUtils.initRedisClient();
		RtmpManagerTask.startTaskTimer();
		//AdjustTimeTask.startTaskTimer();
		
		LivePlayTask livePlayTask = new LivePlayTask();
		livePlayTask.startListenLivePlay();
		InitAdminUser();
		inirProjectConfit();
	}
	
	public boolean InitAdminUser() {
		int result = 0;
		Map<String,Object> condMap = new HashMap<String,Object>();
		List<Map<String,Object>> userList= ms.selectList("User", "selectCountByCondition", condMap);
		if(userList !=null && userList.size()==1) {
			 int total = Integer.parseInt((String) userList.get(0).get("count"));
			 if(total <=0) {
				 User newUser = new User();
				 String password="By@123456"; //默认用户名
				 newUser.setUid("uid" + new Date().getTime() + RandomStringUtils.randomAlphanumeric(10));
				 newUser.setUsername("admin");//默认密码
				 newUser.setPassword(EncryptionUtil.md5Hex(newUser.getUsername() + password +Constant.loginSalt));
				 newUser.setIs_supper(1);
				 newUser.setCreate_time(new Date());
			     result = es.insert("User", "insert", newUser);
			 }
		}
		if(result ==1) {
			return true;
		}
		return false;
	}

	private void inirProjectConfit(){
		LocalProjectConfig config = null;
		try {
			String path = getClass().getClassLoader().getResource("/").getPath();
			path = path.replaceAll("/WEB-INF/classes" , "/ProjectConfig/config.json");
			FileInputStream is = new FileInputStream(path);
			byte[] datas = new byte[500];
			int count = is.read(datas);
			if(count > 0){
				String configJson = new String(datas , 0 , count);
				config = gson.fromJson(configJson , LocalProjectConfig.class);
				LocalProjectConfig.initLocalProjectConfig(config);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(config == null){
			Log.e("SystemInit" , "config load error");
		}else{
			Log.i("SystemInit" , "config " + gson.toJson(config));
		}
	}
}