package bzl.service.impl;


import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import bzl.dao.MapDao;
//import bzl.dao.MySqlJdbcDao;
import bzl.service.MapService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utils.Column;
import utils.Convert;
//import utils.ExecuteProc;
//import utils.PageEntity;
//import utils.PageModel;
import utils.UUIDUtil;

import org.springframework.stereotype.Service;

@Component
@Transactional
@Service
public class MapServiceImpl implements MapService {
	private MapDao md=new MapDao();
	public int execute(String classname,String id,Map<String,Object> map) {
		return md.execute(classname, id, map);
	}
	
	 public int executeMulti(String classname,String id,List<Map<String,Object>> list,HttpServletRequest request) {  
		 return md.executeMulti(classname, id, list,request);
	 }
	 
	 public int executeBatch(String classname,String id,List<Map<String,Object>> list,HttpServletRequest request) {  
		 return md.executeBatch(classname, id, list,request);
	 }
	 
	 public int executeBatch(String classname,String[] tlist,Map<String,List<Map<String,Object>>> map,HttpServletRequest request) {  
		//System.out.println("I hava get here!");
		 return md.executeBatch(classname, tlist, map,request);
	 }
	 
	 public int executeBatch(String[] tlist,Map<String,List<Map<String,Object>>> list,HttpServletRequest request) {  
		 return md.executeBatch(tlist, list,request);
	 }
	 
	 public Map<String,Object> selectOne(String mapperspace,String id,Map<String,Object> map) { 
		 return Convert.FormatMap(md.selectOneMap(mapperspace, id, map));
	 }
	 
	 public List<Map<String,Object>> selectList(String mapperspace,String id,Map<String,Object> map) { 
		 return Convert.FormatList(md.selectList(mapperspace, id, map));
	 }
	 
	 public List<Map<String,Object>> selectList(String classname,String id,String[] item,Map<String,Object> map){
		 return Convert.FormatItemList(md.selectList(classname, id, map),item);
	 }
	 
	 public List<String> selectListforTree(String mapperspace,String id,Map<String,Object> map) { 
		 List<String> list=Convert.FormatListforTree(md.selectList(mapperspace, id, map));
		 return list;
	 }
	
	public Map<String,Object> selectOneMap(String mapperspace, String id,
			Map<String, Object> map) {
		return this.md.selectOneMap(mapperspace, id, map);
	}


	public int save(String tablename,String id, Map<String, Object> map) {
		// TODO Auto-generated method stub
		//MySqlJdbcDao dao=new MySqlJdbcDao();
		Set<String> key = map.keySet();
    	key = map.keySet();
    	boolean hasid=false;
    	for(Iterator<String> it = key.iterator(); it.hasNext();) 
    	{
    		String s =  it.next();
    		if(s.equals("id")){
    			hasid=true;
    		}
    	}
    	if(!hasid){
    		String sid=UUIDUtil.getUUID();
    		map.put("id", sid);
    	}
		return md.save(tablename,id, map);
	}

	public int save(String tablename, String id, String updateid,
			String insertid, Map<String, Object> map) {
		return md.save(tablename,id,updateid,insertid, map);
	}

}