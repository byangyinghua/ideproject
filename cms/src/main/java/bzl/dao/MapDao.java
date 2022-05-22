package bzl.dao;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;
import bzl.dao.inter.MapIDao;
import bzl.entity.User;
import utils.DjSessionFactory;
//import utils.MyListener;
import utils.SessionFactory;
//import utils.XMLHelper;

import sun.rmi.log.LogHandler;

public class MapDao<T> implements MapIDao<T> {
	private static SqlSessionFactory sessionFactory = SessionFactory.getInstance().getSqlSessionFactory();  
	private static SqlSessionFactory djsessionFactory = DjSessionFactory.getInstance().getSqlSessionFactory();  
	Logger log=Logger.getLogger(LogHandler.class);

    public int execute(String namespace,String id,Map<String,Object> map) {  
    	//setSession(namespace,map,null);
        SqlSession session = null;
        try {
        	session = sessionFactory.openSession();  
        	if(id.indexOf("insert")!=-1){
	            session.insert(namespace+"."+id, map);
        	}
        	else if(id.indexOf("update")!=-1){
	            session.update(namespace+"."+id, map);
        	}
        	else if(id.indexOf("delete")!=-1){
	            session.delete(namespace+"."+id, map);
        	}
        	else{
        		session.selectOne(namespace+"."+id, map);
        	}
        	/*Map<String,Object> logmap=new HashMap<String,Object>();
        	logmap.put("operator", id);
        	logmap.put("userid", map.get("adm_userid"));
        	logmap.put("tablename", namespace);
        	logmap.put("IP", map.get("adm_ipaddress"));
        	session.insert("adm_log.insert", logmap);*/
            session.commit();
            return 1;
        }catch(Exception e){
        	session.rollback();
         	log.warn(e);
        	return 0;
        }finally {
            session.close();
        }
    }
    
    
    public int save(String namespace,String id,Map<String,Object> map) {  
    	System.out.println("map1 = "+map);
    	//setSession(namespace,map,null);
        SqlSession session = null; 
        List<Map<String,Object>> list=null;
        int result=0;
        try {
        	session = sessionFactory.openSession();  
        	list= session.selectList(namespace+"."+id,map);
        	//System.out.println(list);
        	//log.info("map===="+map);
        	if(list!=null && list.size()<=0){
        		result=session.insert(namespace+".insert",map);
            	log.info("新增"+namespace+"成功!");
        	}
        	else{
        		result=session.update(namespace+".update",map);
        		log.info("更新"+namespace+"成功!");
        	}
            session.commit();
            return result;
        }catch(Exception e){
        	session.rollback();
         	//System.out.println(e);
         	log.warn(e);
        	return 0;
        }finally {
            session.close();
        }
    }
	 
	 public int executeMulti(String namespace,String id,List<Map<String,Object>> list, HttpServletRequest request) { 
		 //setSession(namespace,null,request);
	        SqlSession session = null;  
	        try {
	        	session = sessionFactory.openSession();
	        	for(Map<String,Object> map:list){
		        	if(id.indexOf("insert")!=-1){
			            session.insert(namespace+"."+id, map);
		        	}
		        	else if(id.indexOf("update")!=-1){
			            session.update(namespace+"."+id, map);
		        	}
		        	else if(id.indexOf("delete")!=-1){
			            session.delete(namespace+"."+id, map);
		        	}
		        	else{
		        		session.selectOne(namespace+"."+id, map);
		        	}
	        	}
	            session.commit();
	            return 1;
	        }catch(Exception e){
	        	session.rollback();
	         	System.out.println(e);
	         	log.warn(e);
	        	return 0;
	        }finally {
	            session.close();
	        }
	    }
	 
	 public int executeBatch(String namespace,String id,List<Map<String,Object>> list,HttpServletRequest request) {  
		 //setSession(namespace,null,request);
		 	SqlSession session = null;
	        try {
	        	session = sessionFactory.openSession();
				if(id.indexOf("insert")!=-1){
		            session.insert(namespace+"."+id, list);
	        	}
	        	else if(id.indexOf("delete")!=-1){
		            session.delete(namespace+"."+id, list);
	        	}
	        	else{
	        		session.update(namespace+"."+id, list);
	        	}
	            session.commit();
	            return 1;
	        }catch(Exception e){
	        	session.rollback();
	         	log.warn(e);
	        	return 0;
	        }finally {
	            session.close();
	        }
	    }
	 
	 public int executeBatch(String namespace,String[] tlist,Map<String,List<Map<String,Object>>> map,HttpServletRequest request) {  
		 //setSession(namespace,null,request);
		 SqlSession session = null;
	        int n=tlist.length;
	        try {
	        	session = sessionFactory.openSession();
	        	Set<String> key = map.keySet();
	    		for(Iterator<String> it = key.iterator(); it.hasNext();) 
	        	{
	    			String s=it.next();
	    			for(int i=0;i<n;i++){
	    				String[] comp=tlist[i].split("-");
	    				//System.out.println("s!======"+s);   
	    				//System.out.println("I hava get here!======"+comp[0]);   
	    				if(s.toLowerCase().equals(comp[0].toLowerCase())){
	    					String id=comp[1];
	    					//System.out.println("comp[1]====="+namespace+"."+comp[1]);
	    					//System.out.println("namespace.id====="+namespace+"."+id);
	    					List<Map<String,Object>> list=map.get(s);
	    					for(Map<String,Object> inmap:list)
	    					if(id.indexOf("insert")!=-1){
	    			            session.insert(namespace+"."+id, inmap);
	    		        	}
	    		        	else if(id.indexOf("update")!=-1){
	    			            session.update(namespace+"."+id, inmap);
	    		        	}
	    		        	else if(id.indexOf("delete")!=-1){
	    			            session.delete(namespace+"."+id, inmap);
	    		        	}
	    				}
	    			}
	        	}
	            session.commit();
	            return 1;
	        }catch(Exception e){
	        	session.rollback();
	         	//System.out.println(e);
	         	log.warn(e);
	        	return 0;
	        }finally {
	            session.close();
	        }
	    }
	 
	 public int executeBatch(String[] tlist,Map<String,List<Map<String,Object>>> map,HttpServletRequest request) {
		 //setSession(null,null,request);
	        SqlSession session = null;
	        int n=tlist.length;
	        try {
	        	session = sessionFactory.openSession();
	        	Set<String> key = map.keySet();
	    		for(Iterator<String> it = key.iterator(); it.hasNext();) 
	        	{
	    			String s=it.next();
	    			for(int i=0;i<n;i++){
	    				String[] comp=tlist[i].split("-");
	    				//System.out.println("comp================="+comp[0]+comp[1]);
	    				if(s.toLowerCase().equals(comp[0].toLowerCase())){
	    					String id=comp[1];
	    					String namespace=comp[0];
	    					List<Map<String,Object>> list=map.get(s);
	    					for(Map<String,Object> inmap:list)
	    					if(id.indexOf("insert")!=-1){
	    			            session.insert(namespace+"."+id, inmap);
	    			            //System.out.println("inmap==================="+inmap);
	    		        	}
	    		        	else if(id.indexOf("update")!=-1){
	    			            session.update(namespace+"."+id, inmap);
	    		        	}
	    		        	else if(id.indexOf("delete")!=-1){
	    			            session.delete(namespace+"."+id, inmap);
	    		        	}
	    				}
	    			}
	        	}
	            session.commit();
	            return 1;
	        }catch(Exception e){
	        	session.rollback();
	         	System.out.println(e);
	         	log.warn(e);
	        	return 0;
	        }finally {
	            session.close();
	        }
	    }
	 
	 public Object selectOne(String mapperspace,String id,Map<String,Object> map) { 
		 //setSession(mapperspace,map,null);
		Object obj=null;
        SqlSession session = null;
        try {
            session = sessionFactory.openSession();
            obj= session.selectOne(mapperspace+"."+id,map);
        } catch(Exception e){
        	e.printStackTrace();
        	log.warn(e);
        	return null;
        }finally {
            session.close();
        }
        return obj;
     }
	 
	 public Map<String,Object> selectOneMap(String mapperspace,String id,Map<String,Object> map) { 
		 	//setSession(mapperspace,map,null);
		 	Map<String,Object> obj=null;
	        SqlSession session = null;
	        try {
	            session = sessionFactory.openSession();
	            obj= session.selectOne(mapperspace+"."+id,map);
	        } catch(Exception e){
	        	e.printStackTrace();
	        	log.warn(e);
	        	return null;
	        }finally {
	            session.close();
	        }
	        return obj;
	  }
	 
	 public List<Map<String,Object>> selectList(String mapperspace,String id,Map<String,Object> map) { 
		 //setSession(mapperspace,map,null);
         System.out.println(mapperspace+"."+id+","+map);
		 List<Map<String,Object>> list=null;
        SqlSession session = null;
        try {
            session = sessionFactory.openSession();
            list= session.selectList(mapperspace+"."+id,map);
        }catch(Exception e){
        	e.printStackTrace();
        	log.warn(e);
        	return null;
        }finally {
            session.close();
        }
        return list;
    }


	public int save(String tablename, String id, String updateid,String insertid, Map<String, Object> map) {
		 //setSession(tablename,map,null);
		SqlSession session = null; 
        List<Map<String,Object>> list=null;
        int result=0;
        try {
        	session = sessionFactory.openSession();  
        	list= session.selectList(tablename+"."+id,map);
        	//System.out.println("list.size()===="+list.size());
        	if(list!=null && list.size()<=0){
        		result=session.insert(tablename+"."+insertid,map);
        	}
        	else{
        		result=session.update(tablename+"."+updateid,map);
        	}
            session.commit();
            return result;
        }catch(Exception e){
        	session.rollback();
         	//System.out.println(e);
         	log.warn(e);
        	return 0;
        }finally {
            session.close();
        }
	}
}
