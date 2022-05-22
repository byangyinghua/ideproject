package bzl.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import bzl.dao.EntityDao;
import bzl.dao.MapDao;
import bzl.entity.User;
import bzl.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utils.ClassRefUtil;
//import utils.AjaxGridReturn;
//import utils.ClassRefUtil;
//import utils.ColumnField;
//import utils.Convert;
//import utils.ExecuteProc;
//import utils.FilterInfo;
//import utils.MyListener;
//import utils.MyReflect;
//import utils.PageEntity;
//import utils.PageInfo;
//import utils.ResultModel;
//import utils.UserArgumentField;

import sun.rmi.log.LogHandler;
import utils.ResultModel;


@Component
@Transactional
@Service
public class EntityServiceImpl<T> implements EntityService<T> {

	private MapDao<T> mapdao=new MapDao<T>();
	private EntityDao<T> basedao=new EntityDao<T>();
//	protected MyReflect<T> mf=new MyReflect<T>();

	Logger log=Logger.getLogger(LogHandler.class);

	public List<?> select(String classname,String namespace,  String id, Map<String,Object> map) {
		List<Map<String,Object>> list1= mapdao.selectList(namespace,id, map);
		List list=new ArrayList();
		for(int i=0;i<list1.size();i++){
			Map<String,Object> valMap=list1.get(i);
	        Class onwClass=null;
	        Object rl = null;
			try {
				onwClass = Class.forName(classname);
		         rl = onwClass.newInstance();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        ClassRefUtil.setFieldValue(rl, valMap);  
	        list.add(rl);
		}
		return list;
		//return (List<?>) mapdao.selectList(classname,id,map);
	}
	
	public List<?> select(String namespace, String id, Object object) {
        Map<String, Object> fieldValMap = ClassRefUtil.getFieldValueMap(object);
    	
        List<Map<String,Object>> list1= mapdao.selectList(namespace,id, fieldValMap);
		List list=new ArrayList();
		for(int i=0;i<list1.size();i++){
			Map<String,Object> valMap=list1.get(i);
	        Class onwClass=null;
	        Object rl = null;
			try {
				String pName = object.getClass().getName();
		        System.out.println("\npName："+pName); 
				onwClass = Class.forName(pName);
		        System.out.println("\nonwClass："+onwClass.getName()); 
		         rl = onwClass.newInstance();
			        System.out.println("\nrl："+rl.getClass().getName()); 

			} catch (ClassNotFoundException e) {
 				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
	        ClassRefUtil.setFieldValue(rl, valMap);  
	        System.out.println(valMap);
	        System.out.println(rl);
	        list.add(rl);
		}
		return list;
		//return (List<?>) mapdao.selectList(classname,id,fieldValMap);
	}

	public int doBatch(String tablename, String operator, List<T> list) {
		return basedao.doBatch(tablename, operator, list);
	}
	public int doBatch(String[] classlist, String[] operlist,List<ResultModel> list) {
		return basedao.doBatch(classlist, operlist, list);
	}


	public int doThing(String tablename){
		return basedao.doThing(tablename);
	}

	public T selectByU(T object) {
		return basedao.selectByU(object);
	}
	
	


	
	
//	public Object getJSONObjectByPage(String classname,String tablename, String id,HttpServletRequest request) {
//		String jsonObject=request.getParameter("_gt_json");
//		JSONObject _gt_json=JSONObject.fromObject(jsonObject);
//		JSONObject elementObject=new JSONObject();
//		JSONObject pageinfo=_gt_json.getJSONObject("pageInfo");
//		//鑾峰緱鍒嗛〉鍙傛暟
//		PageEntity pm=new PageEntity(pageinfo.getInt("pageNum"),pageinfo.getInt("pageSize"));
//		//鎷煎悎sqlwhere
//		JSONArray filter=_gt_json.getJSONArray("filterInfo");
//
//		StringBuilder sb=new StringBuilder();
//		for(int i=0;i<filter.size();i++){
//			if(i==0) sb.append(" where ");
//			FilterInfo fi=new FilterInfo(filter.getJSONObject(i));
//			//fi.getLogic()
//			if(fi.toString()!=null ) sb.append(fi.toString()+" and ");
//		}
//		List<Map<String,Object>> listreq=Convert.getListByReq(tablename, request);
//		//System.out.println("sqlwhere================================================="+sb.toString());
//		if(filter.size()>0){
//			for(Map<String,Object> map:listreq){
//				Set<String> key = map.keySet();
//				key = map.keySet();
//				for(Iterator<String> it = key.iterator(); it.hasNext();) 
//		    	{
//					String s=it.next();
//					sb.append(s+"="+map.get(s)+" and ");
//		    	}
//			}
//		}
//		else{
//			int i=0;
//			for(Map<String,Object> map:listreq){
//				Set<String> key = map.keySet();
//				key = map.keySet();
//				for(Iterator<String> it = key.iterator(); it.hasNext();) 
//		    	{
//					i++;
//					if(i==1) sb.append(" where ");
//					String s=it.next();
//					sb.append(s+"="+map.get(s)+" and ");
//		    	}
//			}
//		}
//		if((filter.size()>0 || listreq.size()>0) && sb.length()>4){
//			try{
//				sb.delete(sb.length()-4, sb.length());
//			}catch(Exception e){
//				e.printStackTrace();
//			}
//		}
//		Map<String, Comparable<?>> map=new HashMap<String, Comparable<?>>();
//		map.put("startrow", pm.getstartRowNum());
//		map.put("endrow",pm.getendRowNum());
//		if(request.getParameter("dateformat")!=null && request.getParameter("dateformat").equals("datetime")){
//			map.put("selectitem",MyListener.getItem(tablename, "yyyy-mm-dd hh24:mi:ss"));
//		}
//		else{
//			map.put("selectitem",MyListener.getItem(tablename, "yyyy-mm-dd"));
//		}
//		map.put("tablename", tablename);
//		map.put("sqlwhere",sb.toString());
//		basedao.getMap(classname, id, map);
//		pm.settotalRowNum(Integer.parseInt((map.get("totalCount").toString())));
//		elementObject.element("pageInfo", pm);
//		List<Map<String,Object>> list=(List<Map<String,Object>>) map.get("list");
//		List<ColumnField> itemlist=MyListener.getItemList(tablename);
//		JSONArray dataJsonArray=null;
//		try {
//			dataJsonArray = JSONArray.fromObject(doList(list,itemlist));
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		elementObject.element("data", dataJsonArray);
//		PageInfo pageInfo=new PageInfo(Integer.parseInt((map.get("totalCount").toString())));
//		//System.out.println(map.get("list"));
//		return new AjaxGridReturn(pageInfo,list,tablename);
//	}
//	
//	public Object getPageJSONObjectByMybatis(String classname,String tablename, String id,HttpServletRequest request) {
//		String jsonObject=request.getParameter("_gt_json");
//		JSONObject _gt_json=JSONObject.fromObject(jsonObject);
//		JSONObject elementObject=new JSONObject();
//		JSONObject pageinfo=_gt_json.getJSONObject("pageInfo");
//		PageEntity pm=new PageEntity(pageinfo.getInt("pageNum"),pageinfo.getInt("pageSize"));
//		JSONArray filter=_gt_json.getJSONArray("filterInfo");
//		Map<String,Object> inmap=Convert.getMapByReq(request);
//		for(int i=0;i<filter.size();i++){
//			FilterInfo fi=new FilterInfo(filter.getJSONObject(i));
//			inmap.put(fi.getFieldName(), fi.getValue());
//		}
//		int n=Integer.parseInt(basedao.selectOne(classname,"selectTotolCount",inmap).toString());
//		pm.settotalRowNum(n);
//		inmap.put("startrow", pm.getstartRowNum());
//		inmap.put("endrow", pm.getendRowNum());
//		elementObject.element("pageInfo", pm);
//		//System.out.println("=========================================");
//		List<Map<String,Object>> list= (List<Map<String, Object>>) basedao.select(classname, id, inmap);
//		List<ColumnField> itemlist=MyListener.getItemList(tablename);
//		JSONArray dataJsonArray=null;
//		try {
//			dataJsonArray = JSONArray.fromObject(this.doList(list, itemlist));
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}//doList瀵硅繑鍥炵殑琛ㄦ牸寮忚繘琛屽鐞�
//		elementObject.element("data", dataJsonArray);
//		return elementObject;
//	}
	
	
//	public Object getJSONObject(String classname,String tablename,String id,HttpServletRequest request) {
//	//	basedao.select(tablename, id, object);
//		String jsonObject=request.getParameter("_gt_json");
//		JSONObject _gt_json=JSONObject.fromObject(jsonObject);
//		JSONObject elementObject=new JSONObject();
//		JSONObject pageinfo=_gt_json.getJSONObject("pageInfo");
//		//鑾峰緱鍒嗛〉鍙傛暟
//		PageEntity pm=new PageEntity(pageinfo.getInt("pageNum"),pageinfo.getInt("pageSize"));
//		//鎷煎悎sqlwhere
//		JSONArray filter=_gt_json.getJSONArray("filterInfo");
//		StringBuilder sb=new StringBuilder();
//		for(int i=0;i<filter.size();i++){
//			if(i==0) sb.append(" where ");
//			FilterInfo fi=new FilterInfo(filter.getJSONObject(i));
//			if(fi.toString()!=null ) sb.append(fi.toString()+" and ");
//		}
//		if(filter.size()>0) sb.delete(sb.length()-4, sb.length());
//		Map<String, Comparable<?>> map=new HashMap<String, Comparable<?>>();
//		map.put("startrow", pm.getstartRowNum());
//		map.put("endrow",pm.getendRowNum());
//		map.put("selectitem",MyListener.getItem(tablename, "yyyy-mm-ss"));
//		map.put("tablename", tablename);
//		map.put("sqlwhere",sb.toString());
//		basedao.getMap(classname, id, map);
//		pm.settotalRowNum(Integer.parseInt((map.get("totalCount").toString())));
//		elementObject.element("pageInfo", pm);
//		JSONArray dataJsonArray=JSONArray.fromObject(map.get("list"));
//		elementObject.element("data", dataJsonArray);
//		PageInfo pageInfo=new PageInfo(Integer.parseInt((map.get("totalCount").toString())));
//		List<?> list=(List<?>) map.get("list");
//		System.out.println(map.get("list"));
//		return new AjaxGridReturn(pageInfo,list,tablename);
//	}
	
	
	
//	public List<T> getList(String classname,String tablename, String id,HttpServletRequest request) {
//		String jsonObject=request.getParameter("_gt_json");
//		JSONObject _gt_json=JSONObject.fromObject(jsonObject);
//		JSONObject elementObject=new JSONObject();
//		JSONObject pageinfo=_gt_json.getJSONObject("pageInfo");
//		//鑾峰緱鍒嗛〉鍙傛暟
//		PageEntity pm=new PageEntity(pageinfo.getInt("pageNum"),pageinfo.getInt("pageSize"));
//		//鎷煎悎sqlwhere
//		JSONArray filter=_gt_json.getJSONArray("filterInfo");
//		StringBuilder sb=new StringBuilder();
//		for(int i=0;i<filter.size();i++){
//			if(i==0) sb.append(" where ");
//			FilterInfo fi=new FilterInfo(filter.getJSONObject(i));
//			if(fi.toString()!=null ) sb.append(fi.toString()+" and ");
//		}
//		if(filter.size()>0) sb.delete(sb.length()-4, sb.length());
//		Map<String, Comparable<?>> map=new HashMap<String, Comparable<?>>();
//		map.put("startrow", pm.getstartRowNum());
//		map.put("endrow",pm.getendRowNum());
//		map.put("selectitem",MyListener.get(tablename));
//		map.put("tablename", tablename);
//		map.put("sqlwhere",sb.toString());
//		basedao.getMap(classname, id, map);
//		pm.settotalRowNum(Integer.parseInt((map.get("totalCount").toString())));
//		elementObject.element("pageInfo", pm);
//		JSONArray dataJsonArray=JSONArray.fromObject(map.get("list"));
//		elementObject.element("data", dataJsonArray);
//		PageInfo pageInfo=new PageInfo(Integer.parseInt((map.get("totalCount").toString())));
//		List<T> list= (List<T>) map.get("list");
//		return list;
//	}
	

	public int delete(String classname, String id, T object) {
		return basedao.delete(classname, id, object);
	}
	
	public int insert(String classname, String id, T object) {
		return basedao.insert(classname, id, object);
	}

	public int update(String classname, String id, T object) {
		return basedao.update(classname, id, object);
	}

//	@Override
//	public int doBatch(String[] tablename, String[] operator, List<ResultModel> list) {
//		// TODO Auto-generated method stub
//		return 0;
//	}

//	public JSONObject getObjByPro(String classname,String id,String procname,HttpServletRequest request){
//		String jsonObject=request.getParameter("_gt_json");
//		if(jsonObject==null) return getObj(classname, id, procname, request);
//		else return getObjByJson(classname, id, procname, request);
//	}
//	
//	public JSONObject getObjByPro(String procname,HttpServletRequest request){
//		ExecuteProc exep=new ExecuteProc();
//		return exep.getJSONByReq(procname, request,0);
//	}
	
//	public JSONObject getObjByMap(String procname,Map<String,Object> map){
//		ExecuteProc exep=new ExecuteProc();
//		JSONObject jobj=null;
//		try {
//			jobj=exep.getJSONByMap(procname, map);
//			//System.out.println("result================="+jobj.get("result"));
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		return jobj;
//	}
//	
//	public JSONObject getObjByProJson(String procname,HttpServletRequest request){
//		ExecuteProc exep=new ExecuteProc();
//		return exep.getJSONByReq(procname, request,2);
//	}
//
//	public JSONObject getObjByJson(String classname,String id,String procname,HttpServletRequest request) {
//		String jsonObject=request.getParameter("_gt_json");
//		JSONObject _gt_json=JSONObject.fromObject(jsonObject);
//		JSONObject elementObject=new JSONObject();
//		Map<String, Comparable> map=new HashMap<String, Comparable>();
//		List<UserArgumentField> listarg=MyListener.getProcArgument(procname);
//		for(UserArgumentField uf:listarg){
//				if(uf.getIN_OUT().indexOf("IN")!=-1){
//					if(uf.getPLS_TYPE().equals("INTEGER")){
//						try{
//							map.put(uf.getARGUMENT_NAME(),_gt_json.getInt(uf.getARGUMENT_NAME()));
//						}catch(Exception e){
//							map.put(uf.getARGUMENT_NAME(),0);
//							System.out.println(e);
//						}
//					}
//					else if(uf.getDATA_TYPE().equals("NUMBER")){
//						try{
//							map.put(uf.getARGUMENT_NAME(),_gt_json.getDouble(uf.getARGUMENT_NAME()));
//						}catch(Exception e){
//							map.put(uf.getARGUMENT_NAME(),0);
//							System.out.println(e);
//						}
//					}
//					else{
//						try{
//							map.put(uf.getARGUMENT_NAME(),_gt_json.getString(uf.getARGUMENT_NAME()));
//						}catch(Exception e){
//							map.put(uf.getARGUMENT_NAME(),null);
//							System.out.println(e);
//						}
//					}
//				}
//		}
//		basedao.getMap(classname, id, map);
//		for(UserArgumentField uf:listarg){
//			try{
//				if(uf.getIN_OUT().indexOf("OUT")!=-1){
//					if(uf.getDATA_TYPE().equals("REF CURSOR")){
//						JSONArray dataJsonArray=JSONArray.fromObject(map.get(uf.getARGUMENT_NAME()));
//						//List<?> list=(List<?>) map.get(uf.getARGUMENT_NAME());
//						//System.out.println(list.size());
//						elementObject.element("data", dataJsonArray);
//					}
//					else{
//						elementObject.element(uf.getARGUMENT_NAME(), map.get(uf.getARGUMENT_NAME()));
//					}
//				}
//			}catch(Exception e){
//				System.out.println(e);
//			}
//		}
//		return elementObject;
//	}
	
//	public JSONObject getObj(String classname, String id, String procname,HttpServletRequest request) {
//		JSONObject elementObject=new JSONObject();
//		Map<String, Comparable> map=new HashMap<String, Comparable>();
//		List<UserArgumentField> listarg=MyListener.getProcArgument(procname);
//		for(UserArgumentField uf:listarg){
//				if(uf.getIN_OUT().indexOf("IN")!=-1){
//					if(uf.getPLS_TYPE().equals("INTEGER")){
//						try{
//							map.put(uf.getARGUMENT_NAME(),request.getParameter(uf.getARGUMENT_NAME()));
//						}catch(Exception e){
//							map.put(uf.getARGUMENT_NAME(),0);
//							System.out.println(e);
//						}
//					}
//					else if(uf.getDATA_TYPE().equals("NUMBER")){
//						try{
//							map.put(uf.getARGUMENT_NAME(),request.getParameter(uf.getARGUMENT_NAME()));
//						}catch(Exception e){
//							map.put(uf.getARGUMENT_NAME(),0);
//							System.out.println(e);
//						}
//					}
//					else if(uf.getDATA_TYPE().equals("DATE")){
//						try{
//							//_gt_json.getClass()
//							map.put(uf.getARGUMENT_NAME(),request.getParameter(uf.getARGUMENT_NAME()));
//						}catch(Exception e){
//							map.put(uf.getARGUMENT_NAME(),0);
//							System.out.println(e);
//						}
//					}
//					else{
//						try{
//							map.put(uf.getARGUMENT_NAME(),request.getParameter(uf.getARGUMENT_NAME()));
//						}catch(Exception e){
//							map.put(uf.getARGUMENT_NAME(),null);
//							System.out.println(e);
//						}
//					}
//				}
//		}
//		basedao.getMap(classname, id, map);
//		for(UserArgumentField uf:listarg){
//			try{
//				if(uf.getIN_OUT().indexOf("OUT")!=-1){
//					if(uf.getDATA_TYPE().equals("REF CURSOR")){
//						JSONArray dataJsonArray=JSONArray.fromObject(map.get(uf.getARGUMENT_NAME()));
//						//List<?> list=(List<?>) map.get(uf.getARGUMENT_NAME());
//						//System.out.println(list.size());
//						elementObject.element("data", dataJsonArray);
//					}
//					else{
//						elementObject.element(uf.getARGUMENT_NAME(), map.get(uf.getARGUMENT_NAME()));
//					}
//				}
//			}catch(Exception e){
//				System.out.println(e);
//			}
//		}
//		return elementObject;
//	}
	
//	/**
//	 * 瀵硅繑鍥炵殑list杩涜澶勭悊
//	 * @param list
//	 * @param itemlist
//	 * @return
//	 * @throws SQLException
//	 */
//	public List<Map<String,Object>> doList(List<Map<String,Object>> list,List<ColumnField> itemlist) throws SQLException{
//		//System.out.println("list================"+list);
//		/*
//		for(Map<String,Object> map:list){
//			Set<String> key = map.keySet();
//			//System.out.println("key============="+key);
//			for(Iterator<String> it = key.iterator(); it.hasNext();) 
//	    	{
//				String s=it.next();
//				//System.out.println("s============="+s);
//				for(ColumnField cf:itemlist){
//					//System.out.println("cf.getColumnname============="+cf.getColumnname());
//					if(s.equals(cf.getColumnname())){
//						//System.out.println("cf.getDatatype()============="+cf.getDatatype());
//						if("CLOB,BLOB,NCLOB".indexOf(cf.getDatatype())!=-1){
//							oracle.sql.CLOB clobtmp = (oracle.sql.CLOB)map.get(s);
//						    if(clobtmp==null || clobtmp.length()==0){
//						       // System.out.println("======CLOB瀵硅薄涓虹┖ ");
//						        map.put(s, "");
//						    }else{
//						       String description=clobtmp.getSubString((long)1,(int)clobtmp.length());
//						        //System.out.println("======瀛楃涓插舰寮�"+description);
//						        map.put(s, description);
//						    }
//						}
//						else if("DATE".indexOf(cf.getDatatype())!=-1){
//							if(map.get(s)!=null){
//								map.put(s, map.get(s).toString());
//							}
//							else{
//								map.put(s, "");
//							}
//						}
//						else{
//							map.put(s, map.get(s).toString());
//						}
//					}
//				}
//	    	}
//		}
//		*/
//		return list;
//	}
	
//	public static void main(String[] args){
//		System.out.println("CLOB".indexOf("LO"));
//	}
}
