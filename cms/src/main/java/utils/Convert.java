package utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import sun.rmi.log.LogHandler;
import net.sf.json.JSONObject;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class Convert {
	static Logger log=Logger.getLogger(LogHandler.class);

	/**获得一个实体对象记录
	 * @param classname
	 * @param request
	 * @return Object
	 */
	public static Object getEntityByReq(String classname,HttpServletRequest request){
		Object obj=null;
		 Class<?> cls=null;
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
		MyReflect mrf=new MyReflect();
		return mrf.getObjectVal(cls,obj,request);
	} 

	
	/**
	 * 灏唕equeset杞崲鎴怣ap骞惰浆鐮�
	 * @param request
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public static Map<String,Object> getMapByReqDecode(HttpServletRequest request) {
		Map<String,Object> map=new HashMap<String,Object>();
		Enumeration<?> paramNames = request.getParameterNames(); 
		while (paramNames.hasMoreElements()){  
			 String paramName = (String) paramNames.nextElement();
	    	 String[] paramValues=request.getParameterValues(paramName);
    		 String pv=null;
	    	 if(paramValues.length==1){
				try {
				pv=new String(request.getParameter(paramName).getBytes("ISO-8859-1"),"UTF-8");
				//System.out.println("pv====="+pv);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				 paramName=paramName.replace("[]", "");
	    		 map.put(paramName,pv);
	    		 log.info("paramName==="+paramName+"&pv====="+pv);
	    	 }
	    	 else {
	    		 paramName=paramName.replace("[]", "");
	    		 map.put(paramName, paramValues);
	    	 }
		}
	//	setUser(request, map);
		return map;
	}
	
	/**
	 * 灏嗕粠mybatis閲岃鍑烘潵鐨刲ist鏍煎紡鍖�
	 * @param list
	 * @return
	 */
	 public static List<Map<String,Object>> FormatList(List<Map<String,Object>> list)
	 {
		// System.out.println("list============="+list);
		 List<Map<String,Object>> result=new ArrayList<Map<String,Object>>();
		 if(list!=null && list.size()>0){
			 for(Map<String,Object> map:list){
				 try {
					Set<String> key = map.keySet();
						key = map.keySet();
						for(Iterator<String> it = key.iterator(); it.hasNext();) 
						{
							String s=it.next();
							String val="";
							try{
								val=map.get(s).toString();
							}catch(Exception e){
								//val="";
							}
							if(val!=null && val.indexOf("oracle.sql.CLOB")!=-1){
								val=Convert.ClobToString((Clob)map.get(s));
							}
							map.put(s, val);
						}
						//log.info("map==="+map);
						result.add(map);
				} catch (Exception e) { 
					
				}
			 }
		 }
		 
		 return result;
	 }
	 
	 public static List<Map<String,Object>> FormatItemList(List<Map<String,Object>> list,String[] item)
	 {
		// System.out.println("list============="+list);
		 List<Map<String,Object>> result=new ArrayList<Map<String,Object>>();
		 if(list!=null && list.size()>0){
			
			 for(Map<String,Object> map:list){
					for(int i=0;i<item.length;i++) 
			    	{
						String s=item[i];
						String val="";
						try{
							val=map.get(s).toString();
						}catch(Exception e){
							//val="";
						}
						map.put(s, val);
			    	}
					//log.info("map==="+map);
					result.add(map);
			 }
		 }
		 
		 return result;
	 }
	 
	 /**
		 * 灏嗕粠mybatis閲岃鍑烘潵鐨刲ist鏍煎紡鍖�
		 * @param list
		 * @return
		 */
		 public static List<Map<String,Object>> FormatList(List<Map<String,Object>> list,Map<String,Object> inmap)
		 {
			 System.out.println(list);
			 List<Map<String,Object>> result=new ArrayList<Map<String,Object>>();
			 for(Map<String,Object> map:list){
				 Set<String> key = map.keySet();
					key = map.keySet();
					for(Iterator<String> it = key.iterator(); it.hasNext();) 
			    	{
						String s=it.next();
						String val=null;
						try{
							val=map.get(s).toString();
						}catch(Exception e){
						}
						if(val!=null && val.indexOf("oracle.sql.CLOB")!=-1){
							val=Convert.ClobToString((Clob)map.get(s));
							//System.out.println("val==============="+val);
						}
						//System.out.println(map.get(s));
						map.put(s, val);
			    	}
					result.add(map);
			 }
			 System.out.println(result);
			 return result;
		 }
	 
	 /**
		 * 灏嗕粠mybatis閲岃鍑烘潵鐨刲ist鏍煎紡鍖�
		 * @param list
		 * @return
		 */
		 public static List<String> FormatListforTree(List<Map<String,Object>> list)
		 {
			 System.out.println(list);
			 List<String> result=new ArrayList<String>();
			 
			 for(Map<String,Object> map:list){
				 Set<String> key = map.keySet();
					key = map.keySet();
					StringBuilder sb=new StringBuilder();
					sb.append("{");
					for(Iterator<String> it = key.iterator(); it.hasNext();) 
			    	{
						String s=it.next();
						String val=null;
						try{
							val=map.get(s).toString();
						}catch(Exception e){
						}
						if(val!=null && val.indexOf("oracle.sql.CLOB")!=-1){
							val=Convert.ClobToString((Clob)map.get(s));
							//System.out.println("val==============="+val);
						}
						else if(val!=null && val.indexOf("oracle.sql.BLOB")!=-1){
							val=Convert.BlobToString((Blob)map.get(s));
						}
						//System.out.println()
						sb.append("\""+s+"\":\""+val+"\",");
						//map.put(s, val);
			    	}
					sb.delete(sb.length()-1,sb.length());
					sb.append("}\r\n");
					result.add(sb.toString());
			 }
			 System.out.println(result);
			 return result;
		 }
	 
	 /**
		 * 灏嗕粠mybatis閲岃鍑烘潵鐨刲ist鏍煎紡鍖�
		 * @param list
		 * @return
		 */
		 public static Map<String,Object> FormatMap(Map<String,Object> map)
		 {
			 if(map!=null){
			 Set<String> key = map.keySet();
				key = map.keySet();
				for(Iterator<String> it = key.iterator(); it.hasNext();) 
		    	{
					String s=it.next();
					String val=map.get(s).toString();
					if(val.indexOf("oracle.sql.CLOB@")!=-1){
						val=Convert.ClobToString((Clob)map.get(s));
						map.put(s, val);
					}
					else if(val.indexOf("oracle.sql.BLOB@")!=-1){
						val=Convert.BlobToString((Blob)map.get(s));
						map.put(s, val);
					}
		    	}
				return map;
			 }
			 else return null;
		 }
		
	/**
	 * 灏哻lob瀵硅薄杞崲鎴怱tring
	 * @param clob
	 * @return
	 */
	public static String ClobToString(Clob clob)
    {
      if (clob == null)
      {
        return null;
      }
      StringBuffer sb = new StringBuffer(65535);//64K
      Reader clobStream = null;//鍒涘缓涓�釜杈撳叆娴佸璞�
      try
      {
        clobStream = clob.getCharacterStream();
        char[] b = new char[60000];//姣忔鑾峰彇60K
        int i = 0;
        while((i = clobStream.read(b)) != -1)
        {
          sb.append(b,0,i);
        }
      }
      catch(Exception ex)
      {
        sb = null;
      }
      finally
      {
        try
        {
          if (clobStream != null)
            clobStream.close();
        }
        catch (Exception e)
        {
        }
      }
      if (sb == null)
        return null;
      else
        return sb.toString();
    }
	
	/**
	 * 灏哹lob瀵硅薄杞崲鎴怱tring
	 * @param blob
	 * @return
	 */
	 public static String BlobToString(Blob blob){
		  String result = "";
		  try {
			ByteArrayInputStream msgContent =(ByteArrayInputStream) blob.getBinaryStream();
			byte[] byte_data = new byte[msgContent.available()];
			msgContent.read(byte_data, 0,byte_data.length);
			result = new String(byte_data);
		  } catch (SQLException e) {
			e.printStackTrace();
		  }
		  return result;
	}
	 
	 public static String JdbcTypeToType(String typename,long scale){
	    	if(typename.equalsIgnoreCase("bigint") && scale==0) return "BIGINT";
	    	else if(typename.equalsIgnoreCase("NUMBER")) return "DECIMAL";
	    	else if((typename.equalsIgnoreCase("VARCHAR2") || typename.equalsIgnoreCase("text"))) return "VARCHAR";
	    	else if(typename.toLowerCase().equals("int") || typename.equalsIgnoreCase("smallint") || typename.equalsIgnoreCase("tinyint")) return "INTEGER";
	    	else if(typename.toLowerCase().equals("datetime") || typename.equalsIgnoreCase("timestamp")) return "TIMESTAMP";
	    	else if((typename.toLowerCase().equals("longtext"))) return "LONGVARCHAR";
	    	else return typename;
	    }
	 
	 /**
	  * 瑙ｆ瀽JsonObject鏁版嵁
	  * @param args
	  */
	 public static JSONObject getJsonObject(HttpServletRequest request){
		 StringBuffer json = new StringBuffer();
	     String line = null;
	     BufferedReader reader;
		 try {
			 reader = request.getReader();
		 while((line = reader.readLine()) != null) {
		    	json.append(line);
		    }
		 } catch (Exception e) {				
				e.printStackTrace();
		 }	    	 	    	 
		 //瑙ｆ瀽json 
	     JSONObject jsonObject = null;
	     jsonObject = JSONObject.fromObject(json.toString());
		 return jsonObject;		 
	 }
	 
	  /**
	    * 汉字转换位汉语拼音首字母，英文字符不变
	    * @param chines 汉字
	    * @return 拼音
	    */
	    public static String converterToFirstSpell(String chines){    	 
	        String pinyinName = "";
	        char[] nameChar = chines.toCharArray();
	        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
	        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
	        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
	        for (int i = 0; i < nameChar.length; i++) {
	            if (nameChar[i] > 128) {
	                try {
	                    pinyinName += PinyinHelper.toHanyuPinyinStringArray(nameChar[i], defaultFormat)[0].charAt(0);
	                } catch (BadHanyuPinyinOutputFormatCombination e) {
	                    e.printStackTrace();
	                }
	            }else{
	            	pinyinName += nameChar[i];
	            }
	        }
	        return pinyinName;
	    }
	    
	    public static Boolean hasCtnGo(){
	    	//CreateCode cc=new CreateCode();
			Calendar c = Calendar.getInstance();//可以对每个时间域单独修改
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day=c.get(Calendar.DAY_OF_MONTH);
		/*	int date = c.get(Calendar.DATE);
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int minute = c.get(Calendar.MINUTE);
			int second = c.get(Calendar.SECOND);*/
			//System.out.println(year + "" + cc.formatTwoInt(month) + "" + cc.formatTwoInt(date) + "" +cc.formatTwoInt(hour) + "" +cc.formatTwoInt(minute) + "" + cc.formatTwoInt(second));
			//String dt=year + "" + cc.formatTwoInt(month) + "" + cc.formatTwoInt(date) + "" +cc.formatTwoInt(hour) + "" +cc.formatTwoInt(minute) + "" + cc.formatTwoInt(second);
			if(year>=2016 && month>6 || day>=8){
				return false;
			}
	    	return true;
	    }
	    
	    
	    public String formatTwoInt(int i){
			String s=i+"";
			if(s.toString().length()==1) return "0"+s;
			else return i+"";
		}
	    
	    
	    public static List<Map<String,Object>> SortDataListId(List<Map<String,Object>> dataList,int page,int pagesize){
			if(dataList!=null && dataList.size() >0) {
				for(int i=0;i<dataList.size();i++) {
					dataList.get(i).put("id",pagesize*(page-1) + (i+1));
				}
			}
	    	return dataList;
	    }
	    
}
