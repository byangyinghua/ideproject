package bzl.task;

//import java.util.List;
//import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

//import org.springframework.beans.BeansException;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.ApplicationContextAware;

//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//
//import bzl.common.Callback;
//import bzl.common.RedisExpireListener;
import bzl.controller.VideoController;
import utils.RedisUtils;

public class RtmpManagerTask {
	private static Timer timer = new Timer();
	
	public static void startTaskTimer() {
		
		timer.schedule(new TimerTask() {
			Set<String> sreamSessions = null; 
			Set<String> rtmpUrlList =null;
			public void run() {
				    sreamSessions = RedisUtils.allSetData(VideoController.streamSesKey);
		        	for (String streamses : sreamSessions) {  
		        		String tmpStreamSes = RedisUtils.get("online:" + streamses);
		        		//System.out.println("run expire tmpStreamSes=" + tmpStreamSes + "realSes=" + streamses);
		        		if(tmpStreamSes==null ||tmpStreamSes.length()==0) {
		        			VideoController.stopStreamSession(streamses);
		        		}
		        	}  
		        }
		}, 1000 , 2000); // 3s检查一次
	}
}