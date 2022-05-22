package bzl.common;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import redis.clients.jedis.JedisPubSub;



public class RedisExpireListener extends JedisPubSub {
	
	private Callback callBacKObj = null;
	
	public void addCallBack(Callback theCallBack) {
		callBacKObj = theCallBack;
	}

	@Override
	public void onPSubscribe(String pattern, int subscribedChannels) {
		System.out.println("onPSubscribe１１１ " + pattern + " " + subscribedChannels);
		
	}

	@Override
	public void onPMessage(String pattern, String channel, String message) {
		String redisKey = message;
		System.out.println("onPMessage pattern " + pattern + " " + channel + " " + message +",time now:" + new Date());
		JSONObject data = new JSONObject();
		data.put("pattern", pattern);
		data.put("channel", channel);
		data.put("message", message);
		if(callBacKObj!=null) {
			callBacKObj.callBack(pattern,data);
		}
	}

}