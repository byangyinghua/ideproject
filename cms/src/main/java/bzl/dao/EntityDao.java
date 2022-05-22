package bzl.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;
import bzl.dao.inter.EntityIDao;
import bzl.service.EntityService;
import org.junit.Test;
import org.springframework.stereotype.Component;
import utils.DjSessionFactory;
//import utils.MyListener;
import utils.ResultModel;
import utils.SessionFactory;

import sun.rmi.log.LogHandler;
@Component
public class EntityDao<T> implements EntityIDao<T> {
		//ApplicationContext ctx = new ClassPathXmlApplicationContext("usermap-servlet.xml");
		Logger log=Logger.getLogger(LogHandler.class);
        private static SqlSessionFactory sessionFactory = SessionFactory.getInstance().getSqlSessionFactory();  
    	
        /**
         * 根据类名和编号获得对象列表 
         * @param classname
         * @param id
         * @return
         */
	    public List<T> select(String classname,String id) { 
	        SqlSession session = null;
	        List<T> list = null;
	        try {
	            session = sessionFactory.openSession();
	        	list = session.selectList(classname+"."+id);
	        }catch(Exception e){
	        	System.out.println(e);
	        	log.warn(e);
	        	return null;
	        } finally {
	            session.close();
	        }
	        return list;
	    }
	    
	    public List<T> select(String classname,String id,Map<String,Object> map) { 
	    	//setSession(classname,map);
			List<T> list = null;
	        SqlSession session = null;
	        try {
	            session = sessionFactory.openSession();
	            list = session.selectList(classname+"."+id,map);
	        } catch(Exception e){
	        	System.out.println(e);
	        	log.warn(e);
	        	return null;
	        }finally {
	            session.close();
	        }
	        return list;
	    }  
		
		public List<T> select(String classname,String id,Object object) { 
			List<T> list = null;
	        SqlSession session = null;
	        try {
	            session = sessionFactory.openSession();
	            list = session.selectList(classname+"."+id,object);
	        } catch(Exception e){
	        	System.out.println(e);
	        	log.warn(e);
	        	return null;
	        }finally {
	            session.close();
	        }
	        return list;
	    }  
		
	    
	    /**
         * 根据类名和编号获得对象
         * @param classname
         * @param id
         * @return
         */
		public Object selectOne(String classname,String id) { 
			Object obj=null;
	        SqlSession session = null;
	        try {
	            session = sessionFactory.openSession();
	            obj= session.selectOne(classname+"."+id);
	        } catch(Exception e){
	        	e.printStackTrace();
	        	log.warn(e);
	        	return null;
	        }finally {
	            session.close();
	        }
	        return obj;
	    }
		
		public Object selectOne(String classname,String id,Map<String,Object> map) { 
			//setSession(classname,map);
			Object obj=null;
	        SqlSession session = null;
	        try {
	            session = sessionFactory.openSession();
	            obj= session.selectOne(classname+"."+id,map);
	        } catch(Exception e){
	        	e.printStackTrace();
	        	log.warn(e);
	        	return null;
	        }finally {
	            session.close();
	        }
	        return obj;
	    }  
	    
		
		
		public Object selectOne(String classname,String id,T object) { 
			Object obj=null;
	        SqlSession session = null;
	        try {
	            session = sessionFactory.openSession();
	            obj= session.selectOne(classname+"."+id,object);
	        } catch(Exception e){
	        	e.printStackTrace();
	        	log.warn(e);
	        	return null;
	        }finally {
	            session.close();
	        }
	        return obj;
	    }  
		
		public int insert(String namespace, String id, T object) {
	    	//	kdtc.soft.util.Person
	        SqlSession session = null;  
	        try {
	        	session = sessionFactory.openSession();  
	            session.insert(""+namespace+"."+id, object);
	            session.commit();
	            return 1;
	        }catch(Exception e){
	        	System.out.println(e);
	        	session.rollback();
	        	log.warn(e);
	        	return 0;
	        }finally {
	            session.close();
	        }
		}
	    
	    public int insertBatch(String namespace, String id,List<T> list) {  
	        SqlSession session = null;  
	        try {
	            session = sessionFactory.openSession(); 
	            for(T object:list){
	            	session.insert(namespace+"."+id, object);
	            }
	            session.commit();
	            return 1;
	        }catch(Exception e){
	        	System.out.println(e);
	        	session.rollback();
	        	log.warn(e);
	        	return 0;
	        } finally {
	            session.close();
	        }
	    }
	    
	    public int delete(String namespace, String id, T object) {
			//	String classname=getClassname(object).toLowerCase();;
	        SqlSession session = null;  
	        try {  
	            session = sessionFactory.openSession();  
	            System.out.println("object===="+object);
	            session.delete(""+namespace+"."+id, object);  
	            session.commit();  
	            return 1;
	        }catch(Exception e){
	        	System.out.println(e);
	        	session.rollback();
	        	log.warn(e);
	        	return 0;
	        }
	        finally {  
	            session.close();  
	        }  
		}
	    
		
		public int deleteBatch(String namespace, String id, List<T> list) {
			 SqlSession session = null;  
		        try {
		            session = sessionFactory.openSession(); 
		            for(T object:list){
		            	 session.delete(namespace+"."+id, object);
		            }
		            session.commit();
		            return 1;
		        }catch(Exception e){
		        	System.out.println(e);
		        	session.rollback();
		        	log.warn(e);
		        	return 0;
		        } finally {
		            session.close();
		        }
		}
		
	    
		public int update(String namespace, String id, T object) {
	        SqlSession session = null;  
	        int cntRow = 0;
	        try {  
	            session = sessionFactory.openSession();  
	            cntRow = session.update(namespace+"."+id, object);  
	            session.commit();
	            return cntRow;
	        }catch(Exception e){
	        	System.out.println(e);
	        	session.rollback();
	        	log.warn(e);
	        	return 0;
	        } finally {
	            session.close();  
	        } 
		}

		

		public int updateBatch(String namespace, String id, List<T> list) {
			 SqlSession session = null;  
		        try {
		            session = sessionFactory.openSession(); 
		            for(T object:list){
		            	session.update(namespace+"."+id, object);
		            }
		            session.commit();
		            return 1;
		        }catch(Exception e){
		        	System.out.println(e);
		        	session.rollback();
		        	log.warn(e);
		        	return 0;
		        } finally {
		            session.close();
		        }
		}

		
		
		public int doBatch(String tablename,String operator, List<T> list) {
			 SqlSession session = null;  
		        try {
		            session = sessionFactory.openSession(); 
	            	if(operator.equals("add")){
			            for(T object:list){
			            	session.insert(""+tablename+".insert", object);
			            }
	            	}
	            	else if(operator.equals("edit")){
			            for(T object:list){
			            	session.update(""+tablename+".update", object);
			            }
	            	}
	            	else if(operator.equals("delete")){
	            		for(T object:list){
			            	session.delete(""+tablename+".delete", object);
			            }
	            	}
	            	else if(operator.equals("addmulti")){
			            for(T object:list){
			            	session.insert(""+tablename+".insert", object);
			            }
	            	}
	            	else if(operator.equals("editmulti")){
			            for(T object:list){
			            	session.update(""+tablename+".update", object);
			            }
	            	}
	            	else
	            	{
			            for(T object:list){
			            	session.delete(""+tablename+".delete", object);
			            }
	            	}
		            session.commit();
		            return 1;
		        }catch(Exception e){
		        	System.out.println(e);
		        	session.rollback();
		        	log.warn(e);
		        	return 0;
		        } finally {
		            session.close();
		        }
		}
		
		public int doBatch(String[] tablelist,String[] operlist, List<ResultModel> list) {
			int result=0;
			 SqlSession session = null;
		        try {
		            session = sessionFactory.openSession(); 
	            	for(ResultModel rm:list){
	            		String classname1=rm.getClassname().toLowerCase();
	            		List<?> lst=rm.getList();
			            for(int i=0;i<operlist.length;i++){
			            	String operator=operlist[i];
			            	String classname=tablelist[i].toLowerCase();
		            		if(classname1.equals(classname)){
		            		    if((operator.indexOf("add")!=-1 || operator.indexOf("insert")!=-1) && operator.toLowerCase().indexOf("multi")!=-1){
	            				  if(operator.equals("add")) operator=operator.replaceAll("add", "insert");
	            				    operator=operator.replaceAll("multi", "");
						            for(Object object:lst){
						            	session.insert(""+classname+"."+operator, object);
						            }
					           	}
					           	else if((operator.indexOf("edit")!=-1 || operator.indexOf("update")!=-1) && operator.toLowerCase().indexOf("multi")!=-1){
		            				if(operator.equals("edit")) operator=operator.replaceAll("edit", "update");
	            				    operator=operator.replaceAll("multi", "");
						            for(Object object:lst){
						            	session.update(""+classname+"."+operator, object);
						            }
					           	}
					           	else if((operator.indexOf("del")!=-1 || operator.indexOf("delete")!=-1) && operator.toLowerCase().indexOf("multi")!=-1){
		            				 if(operator.equals("del"))  operator="delete";
		            				 operator=operator.replaceAll("multi", "");
					           		for(Object object:lst){
						            	session.delete(""+classname+"."+operator, object);
						            }
					           	}
		            			else if(operator.indexOf("add")!=-1 || operator.indexOf("insert")!=-1){
	            				    if(operator.equals("add")) operator=operator.replaceAll("add", "insert");
						            for(Object object:lst){
						            	session.insert(""+classname+"."+operator, object);
						            }
					           	}
					           	else if(operator.indexOf("edit")!=-1 || operator.indexOf("update")!=-1){
		            				if(operator.equals("edit"))  operator=operator.replaceAll("edit", "update");
						            for(Object object:lst){
						            	session.update(""+classname+"."+operator, object);
						            }
					           	}
					           	else if(operator.indexOf("del")!=-1 || operator.indexOf("delete")!=-1){
		            				if(operator.equals("del")) operator="delete";
					           		for(Object object:lst){
						            	session.delete(""+classname+"."+operator, object);
						            }
					           	}
		            		}
		            	}
		            }
		            session.commit();
		            result=1;
		        } 
		        catch(Exception e){
		        	System.out.println(e);
		        	session.rollback();
		        	log.warn(e);
		        	result=0;
		        }
		        finally {
		            session.close();
		        }
		        return result;
		}

	
		
		@Test
		public int doThing(String tablename){
			SqlSession session = null;
			session = sessionFactory.openSession();
			Map<String, Integer> map=new HashMap<String, Integer>();
			map.put("x",1);
			map.put("y",2);
			map.put("z",0);
			Object t=session.selectOne(tablename+".selectByProc",map);
			return 1;
		}
		
		
		
		public T selectByU(T object) {
			String classname=getClassname(object).toLowerCase();
	    	T obj;
			try {
				obj = (T) object.getClass().newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
				log.warn(e);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				log.warn(e);
			}
	        SqlSession session = null;  
	        try {
	            session = sessionFactory.openSession();  
	            obj = (T) session.selectOne(classname+".selectByU", object);  
	        }catch(Exception e){
	        	System.out.println("daoexception============================="+e);
	        	log.warn(e);
	        	return null;
	        } finally {
	            session.close();  
	        }
	        return obj;	
	    }
		
		
		public Map<?, ?> getMap(String classname, String id,Map<?, ?> map) {
			
			SqlSession session = null;
			session = sessionFactory.openSession();
			session.selectOne(classname+"."+id,map);
			return map;
		}

		
	
		
		  
	    private String getClassname(T object){
	    	String name=object.getClass().getName();
	    	//System.out.println(name);
	    	String[] cls=name.split("\\.");
	    	String classname=cls[cls.length-1];
	    	return classname;
	    }


		@Override
		public List<?> dosomething(String classname, String id) {
			// TODO Auto-generated method stub
			return null;
		}
	 }  