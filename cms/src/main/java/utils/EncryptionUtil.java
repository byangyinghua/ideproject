package utils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import bzl.entity.User;

import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;


/**
 * 加解密算法类
 * @author 唐旭峰
 *
 */
public class EncryptionUtil {
	
	/*AES 加解密*/
	private static String sKey = "@pMu89erT812!bsl";//key，可自行修改
    private static String ivParameter = "0392039203920300";//偏移量,可自行修改
	
    /**
     * AES 加密
     * */
    public static String encryptAES(String sSrc) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] raw = sKey.getBytes();
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
        byte[] encrypted = cipher.doFinal(sSrc.getBytes("utf-8"));
        return new BASE64Encoder().encode(encrypted);// 此处使用BASE64做转码。
    }

    /**
     * AES 解密
     * */
    public static String decryptAES(String sSrc) throws Exception {
    	if(sSrc ==null) {
    		return null;
    	}
        try {
            byte[] raw = sKey.getBytes("ASCII");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] encrypted1 = new BASE64Decoder().decodeBuffer(sSrc);// 先用base64解密
            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original, "utf-8");
            return originalString;
        } catch (Exception ex) {
            System.out.println("decryptAES failed!!err="+ex.toString());
        	return null;
        }
    }

	/**
     * Base64 encode
     * */
    public static String base64Encode(String data){
        return Base64.encodeBase64String(data.getBytes());
    }
     
    /**
     * Base64 decode
     * @throws UnsupportedEncodingException 
     * */
    public static String base64Decode(String data) throws UnsupportedEncodingException{
        return new String(Base64.decodeBase64(data.getBytes()),"utf-8");
    }
     
    /**
     * md5
     * */
    public static String md5Hex(String data){
        return DigestUtils.md5Hex(data);
    }
     
    /**
     * sha1
     * */
    public static String sha1Hex(String data){
        return DigestUtils.sha256Hex(data);
    }
     
    /**
     * sha256
     * */
    public static String sha256Hex(String data){
        return DigestUtils.sha256Hex(data);
    }
    
    public static final String Md(String plainText,boolean judgeMD) {   
        StringBuffer buf = new StringBuffer("");   
        try {   
        MessageDigest md = MessageDigest.getInstance("MD5");   
        md.update(plainText.getBytes());   
        byte b[] = md.digest();   
        int i;   
        for (int offset = 0; offset < b.length; offset++) {   
            i = b[offset];   
            if(i<0) i+= 256;   
            if(i<16)   
            buf.append("0");   
            buf.append(Integer.toHexString(i));   
        }   
 
        } catch (NoSuchAlgorithmException e) {   
        e.printStackTrace();   
        }   
        if(judgeMD == true){  
            return buf.toString();  
        }else{  
            return buf.toString().substring(16,24);  
        }  
          
    }   
//    
//    public static String login(User user){
//    	 String password=EncryptionUtil.md5Hex(user.getPassword());
//		 password=EncryptionUtil.md5Hex(user.getUsername()+password);
//		 user.setPassword(password);
//         return password;
//    }
//    
//    public static String UserMd5(String username,String password){
//	   	 String result=EncryptionUtil.md5Hex(password);
//	   	 result=EncryptionUtil.md5Hex(username+result);
//        return result;
//   }
//    
//    public static String login(Map<String,Object> user){
//   	 String password=EncryptionUtil.md5Hex(user.get("password").toString());
//		 password=EncryptionUtil.md5Hex(user.get("username")+password);
//		 user.put("password", password);
//        return password;
//   }
    
//    /**
//     * 自定义加密
//     * @param data
//     * @return
//     */
//    public static String jjxxEncode(String data){
//    	String t1=Md(data, false);
//        MessageDigest md5=null;
//        String t2 =null;
//		try {
//			md5 = MessageDigest.getInstance("MD5");
//			BASE64Encoder base64en = new BASE64Encoder();  
//           t2 = base64en.encode(md5.digest(data.getBytes("utf-8")));  
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//         return t1+t2;
//    }
}
