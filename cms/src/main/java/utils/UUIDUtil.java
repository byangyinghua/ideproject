package utils;
import java.util.Arrays;
import java.util.UUID;


public class UUIDUtil {

	 /** 
     * 获得一个UUID 
     * @return String UUID 
     */ 
    public static String getUUID(){ 
        String s = UUID.randomUUID().toString();
       // return s;
        //去掉“-”符号 
        return s.substring(0,8)+s.substring(9,13)+s.substring(14,18)+s.substring(19,23)+s.substring(24); 
    } 
    /** 
     * 获得指定数目的UUID 
     * @param number int 需要获得的UUID数量 
     * @return String[] UUID数组 
     */ 
    public static String getUUID(int number){ 
        if(number < 1){ 
            return null; 
        } 
        String[] ss = new String[number]; 
        for(int i=0;i<number;i++){ 
            ss[i] = getUUID(); 
        } 
        return Arrays.toString(ss); 
    } 
    
    /** 
     * 获得制定长度的uuid
     * @param number int 需要获得的UUID数量 
     * @return String[] UUID数组 
     */ 
    public static String getUUIDBySize(int number){ 
        if(number < 1){ 
            return null; 
        } 
        Byte []retUuid = new Byte[number];
        String ss= getUUID(); 
        for(int i=0;i<number;i++) {
        	retUuid[i] = ss.getBytes()[i];
        }
        return retUuid.toString(); 
    } 
	

}
