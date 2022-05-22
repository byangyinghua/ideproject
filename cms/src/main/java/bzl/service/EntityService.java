package bzl.service;



import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import utils.ResultModel;

import net.sf.json.JSONObject;

public interface EntityService<T> {
  

    List<?> select(String classname,String namespace,String id,Map<String,Object> map);
    List<?> select(String namespace, String id, Object object);
    int insert(String classname,String id,T object);
    int update(String classname,String id,T object);
    int delete(String classname,String id,T object);
  
    int doBatch(String tablename,String operator,List<T> list);
    int doBatch(String tablename[], String operator[],List<ResultModel> list);
    int doThing(String tablename);
   
//    Object getJSONObject(String classname,String tablename,String id,HttpServletRequest request);
//    List<T> getList(String classname,String tablename, String id,HttpServletRequest request);
//	JSONObject getObjByPro(String classname,String id,String procname,HttpServletRequest request);
//	Object getJSONObjectByPage(String string, String viewname, String string2,HttpServletRequest request);
//	JSONObject getObjByPro(String procname,HttpServletRequest request);
//	Object getPageJSONObjectByMybatis(String classname,String tablename, String id,HttpServletRequest request);
//	JSONObject getObjByProJson(String procname, HttpServletRequest request);
//	JSONObject getObjByMap(String procname,Map<String,Object> map);
}
