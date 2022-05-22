package utils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class MyReflect<T> {
	public Class<?> getCls() {
		return cls;
	}
	public void setCls(Class<?> cls) {
		this.cls = cls;
	}
	public String getClasspath() {
		return classpath;
	}
	public void setClasspath(String classpath) {
		this.classpath = classpath;
	}
	public Object getObj() {
		return obj;
	}
	public void setObj(Object obj) {
		this.obj = obj;
	}
	public String getClassname() {
		return classname;
	}
	public void setClassname(String classname) {
		this.classname = classname;
	}
	private Class<?> cls=null;
	private String classpath="kdtc.soft.model.";
	private Object obj=null;
	private String classname;
	public MyReflect(){
	}
	
	/**
	 * 
	 * @param dateStr
	 * @param formatStr
	 * @return date
	 */
	public Date StringToDate(String dateStr,String formatStr){
		DateFormat sdf=new SimpleDateFormat(formatStr);
		sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
		Date date=null;
		try {
			date = sdf.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}
	
	
	/**获得一个实体对象记录
	 * @param classname
	 * @param request
	 * @param response
	 * @return Object
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IntrospectionException
	 * @throws InvocationTargetException
	 * @throws ClassNotFoundException
	 */
	public Object getEntityByReq(String classname,HttpServletRequest request,HttpServletResponse response){
		Object obj=null;
		try {
			cls=Class.forName(classname);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			return null;
		}
		try {
			obj=cls.newInstance();
			//System.out.println(obj.toString());
		} catch (InstantiationException e) {
			System.out.println("InstantiationException");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.out.println("IllegalAccessException");
			e.printStackTrace();
		}
		return getObjectVal(cls,obj,request);
	}
	
	/**
	 * 获得对象的值
	 * @param cls
	 * @param obj
	 * @param request
	 * @return
	 * @throws IntrospectionException
	 * @throws NumberFormatException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public Object getObjectVal(Class<?> cls,Object obj,HttpServletRequest request){
		//Class<?> clazz=cls.getSuperclass();
		//特殊处理，用于解决分页问题
		Field[] fields = cls.getDeclaredFields();
		Enumeration<?> paramNames = request.getParameterNames(); 
		while (paramNames.hasMoreElements()){  
	    	 String paramName = (String) paramNames.nextElement();
	    	 String[] paramValues=request.getParameterValues(paramName);
	    	 String paramValue= null;
	    	 StringBuilder sb=new StringBuilder();
	    	 if(paramValues.length<=1) paramValue= request.getParameter(paramName);
	    	 else {
	    		 for(String str:paramValues){
	    			 sb.append(str+",");
	    		 }
	    		 sb.deleteCharAt(sb.length()-1);
	    		 paramValue=sb.toString();
	    	 }
	    	 //获得对象的值
	    	// if(pfields!=null) obj=this.setVal(pfields, obj, paramName, paramValue);
	    	 try {
				obj=this.getVal(fields, obj, paramName, paramValue);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IntrospectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		     //////////
	    }
		return obj;
	}
	
	/**
	 * 获得对象属性的值
	 * @param fields
	 * @param obj
	 * @param paramName
	 * @param paramValue
	 * @return
	 * @throws IntrospectionException
	 * @throws NumberFormatException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public Object getVal(Field[] fields,Object obj,String paramName,String paramValue) throws IntrospectionException, NumberFormatException, IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		 for(Field field:fields){
			  System.out.print(field+"="+field.getName()+"\t");
	    	  PropertyDescriptor pd= new PropertyDescriptor(field.getName(),obj.getClass());
				if(field.getName().toLowerCase().equals(paramName)){
				    Method setMethod=pd.getWriteMethod();//类名class java.lang.Integer
				    System.out.print(paramName+"="+paramValue+"\t");
				    if(field.getType().toString().equals("int") || field.getType().toString().equals("class java.lang.Integer")){
						setMethod.invoke(obj, Integer.parseInt(paramValue));
					}
					else if(field.getType().toString().equals("class java.util.Date")){
						setMethod.invoke(obj, StringToDate(paramValue,"yyyy-MM-dd"));
					}
					else if(field.getType().toString().equals("long") || field.getType().toString().equals("class java.lang.Long")){
						if(paramValue==null || paramValue=="") paramValue="0";
						setMethod.invoke(obj, Long.parseLong(paramValue));
					}
					else if(field.getType().toString().equals("short") || field.getType().toString().equals("class java.lang.Short")){
						if(paramValue==null || paramValue=="") paramValue="0";
						setMethod.invoke(obj, Short.parseShort(paramValue));
					}
					else if(field.getType().toString().equals("float") || field.getType().toString().equals("class java.lang.Float")){
						if(paramValue==null || paramValue=="") paramValue="0";
						setMethod.invoke(obj, Float.parseFloat(paramValue));
					}
					else if(field.getType().toString().equals("double") || field.getType().toString().equals("class java.lang.Double")){
						if(paramValue==null || paramValue=="") paramValue="0";
						setMethod.invoke(obj, Double.parseDouble(paramValue));
					}
					else if(field.getType().toString().equals("char")){
						setMethod.invoke(obj, paramValue);
					}
					else if(field.getType().toString().equals("boolean") || field.getType().toString().equals("class java.lang.Boolean")){
						setMethod.invoke(obj, Boolean.parseBoolean(paramValue));
					}
					else if(field.getType().toString().equals("byte") || field.getType().toString().equals("class java.lang.Byte")){
						setMethod.invoke(obj, Byte.parseByte(paramValue));
					}
					else
					{
						setMethod.invoke(obj, paramValue);
						System.out.println(paramValue);
					}
				    break;
			}
	    }
		return obj;
	}
	/**
	 * 获取父类的方法
	 * @param object
	 * @param methodName
	 * @param parameterTypes
	 * @return
	 */
	public static Method getDeclaredMethod(Object object, String methodName, Class<?> parameterTypes){    
		Method method = null;    
		for(Class<?> clazz = object.getClass();clazz != Object.class;clazz = clazz.getSuperclass()) {    
			try {
				method = clazz.getDeclaredMethod(methodName, parameterTypes);
				return method;
			} catch (Exception e) {
				clazz = clazz.getSuperclass();
			}    
		}
		return null;
	}  
	
	/**
	 * 获得
	 * @param request
	 * @return
	 */
	public List<FieldAndCount> getNameAndCount(String classname,HttpServletRequest request){
		try {
			cls=Class.forName("kdtc.soft.model."+classname);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		List<FieldAndCount> list=new ArrayList<FieldAndCount>();
		Enumeration<?> paramNames = request.getParameterNames(); 
		while (paramNames.hasMoreElements()){
	    	String paramName = (String) paramNames.nextElement();
	    	T obj=null;
			try {
				obj=(T) cls.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			Field[] fields = obj.getClass().getDeclaredFields();
			for(Field field:fields){
				if(field.getName().equals(paramName)){
					//System.out.println("getNameAndCount "+paramName);
			    	 String[] paramValues=request.getParameterValues(paramName);
			    	 int length=paramValues.length;
			    	 FieldAndCount fc=new FieldAndCount();
			    	 fc.setCount(length);
			    	 fc.setField(field);
			    	 list.add(fc);
			    	 break;
				}
			}
		}
		return list;
	}
	
	public int getCount(HttpServletRequest request){
		Enumeration<?> paramNames = request.getParameterNames(); 
		int n=1;
		while (paramNames.hasMoreElements()){  
	    	 String paramName = (String) paramNames.nextElement();
	    	 String[] paramValues=request.getParameterValues(paramName);
	    	 int length=paramValues.length;
	    	if(length>1) {
	    		n=length;
	    		break;
	    	}
		}
		return n;
	}
	
	/**解析单表多条记录
	 * @param classname
	 * @param request
	 * @param response
	 * @return Object
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IntrospectionException
	 * @throws InvocationTargetException
	 * @throws ClassNotFoundException
	 */
	public List<T> SimTableMultiRow(String tablename,HttpServletRequest request,HttpServletResponse response) throws IllegalArgumentException, InstantiationException, IllegalAccessException, IntrospectionException, InvocationTargetException, ClassNotFoundException{
		List<T> retultlist=new ArrayList<T>();
		String classname=tablename.substring(0,1).toUpperCase() + tablename.substring(1);
	    //System.out.println("转换类"+classname);
	    try{
			cls=Class.forName("kdtc.soft.model."+classname);
		   
			int n=getCount(request);
			List<FieldAndCount> list=this.getNameAndCount(classname, request);
			for(int i=0;i<n;i++){
				T obj=null;
				try {
					obj=(T) cls.newInstance();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			    //System.out.println(obj.toString());
			    //System.out.println(list.size());
	
				for(FieldAndCount fc:list){
					Field field=fc.getField();
					String fieldname=field.getName();
			    	PropertyDescriptor pd= new PropertyDescriptor(fieldname,cls);
					String[] paramValues=request.getParameterValues(fieldname);
					String paramValue="";
					if(fc.getCount()>1) paramValue=paramValues[i];
					else paramValue=request.getParameter(fieldname);
				    Method setMethod=pd.getWriteMethod();//类名class java.lang.Integer
				    if(field.getType().toString().equals("int") || field.getType().toString().equals("class java.lang.Integer")){
						setMethod.invoke(obj, Integer.parseInt(paramValue));
					}
					else if(field.getType().toString().equals("class java.util.Date")){
						setMethod.invoke(obj, StringToDate(paramValue,"yyyy-MM-dd"));
					}
					else if(field.getType().toString().equals("long") || field.getType().toString().equals("class java.lang.Long")){
						if(paramValue==null) paramValue="0";

						setMethod.invoke(obj, Long.parseLong(paramValue));
					}
					else if(field.getType().toString().equals("short") || field.getType().toString().equals("class java.lang.Short")){
						if(paramValue==null) paramValue="0";
						setMethod.invoke(obj, Short.parseShort(paramValue));
					}
					else if(field.getType().toString().equals("float") || field.getType().toString().equals("class java.lang.Float")){
						if(paramValue==null) paramValue="0";
						setMethod.invoke(obj, Float.parseFloat(paramValue));
					}
					else if(field.getType().toString().equals("double") || field.getType().toString().equals("class java.lang.Double")){
						if(paramValue==null) paramValue="0";
						setMethod.invoke(obj, Double.parseDouble(paramValue));
					}
					else if(field.getType().toString().equals("char")){
						setMethod.invoke(obj, Double.parseDouble(paramValue));
					}
					else if(field.getType().toString().equals("boolean") || field.getType().toString().equals("class java.lang.Boolean")){
						setMethod.invoke(obj, Boolean.parseBoolean(paramValue));
					}
					else if(field.getType().toString().equals("byte") || field.getType().toString().equals("class java.lang.Byte")){
						setMethod.invoke(obj, Byte.parseByte(paramValue));
					}
					else
					{
						setMethod.invoke(obj, paramValue);
					}
				}
				retultlist.add(obj);
		
			}
		 }catch(Exception e){
			    	
		 }
		return retultlist;
	}
	
	/**解析多表多条记录
	 * @param classname
	 * @param request
	 * @param response
	 * @return Object
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IntrospectionException
	 * @throws InvocationTargetException
	 * @throws ClassNotFoundException
	 */
	public List<ResultModel> MultiTableMultiRow(String[] tablelist,HttpServletRequest request,HttpServletResponse response) throws IllegalArgumentException, InstantiationException, IllegalAccessException, IntrospectionException, InvocationTargetException, ClassNotFoundException{
		List<ResultModel> list=new ArrayList<ResultModel>();
		for(int i=0;i<tablelist.length;i++){
			String tablename=tablelist[i];
			String classname=tablename.substring(0,1).toUpperCase() + tablename.substring(1);
			//System.out.println("tangxufeng:"+classname+"["+i+"]------"+tablelist.length);
			ResultModel rm=new ResultModel();
			rm.setClassname(classname);
			List<?> rmlist=SimTableMultiRow(tablename,request,response);
			rm.setList(rmlist);
			list.add(rm);
			//if(i==tablelist.length-1) break;
		}
		return list;
	}
}
