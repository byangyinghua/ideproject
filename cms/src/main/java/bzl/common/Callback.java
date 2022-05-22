package bzl.common;

import com.alibaba.fastjson.JSONObject;

public interface Callback {
	   JSONObject callBack(String key,JSONObject valObj);
	   
}

