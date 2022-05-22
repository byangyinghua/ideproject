package utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

public class NetUtil{
	
	public static String getIpAddr(HttpServletRequest request) {   
	     String ipAddress = null;   
	     //ipAddress = request.getRemoteAddr();   
	     ipAddress = request.getHeader("x-forwarded-for");   
	     if(ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {   
	      ipAddress = request.getHeader("Proxy-Client-IP");   
	     }   
	     if(ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {   
	         ipAddress = request.getHeader("WL-Proxy-Client-IP");   
	     }   
	     if(ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {   
	      ipAddress = request.getRemoteAddr();   
	      if(ipAddress.equals("127.0.0.1")){   
	       //根据网卡取本机配置的IP   
	       InetAddress inet=null;   
	       try {   
	    	   inet = InetAddress.getLocalHost();   
		    } catch (UnknownHostException e) {   
		    	e.printStackTrace();   
		    }   
	       	ipAddress= inet.getHostAddress();   
		    }   
	     }   
	  
	     //对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割   
	     if(ipAddress!=null && ipAddress.length()>15){ //"***.***.***.***".length() = 15   
	         if(ipAddress.indexOf(",")>0){   
	             ipAddress = ipAddress.substring(0,ipAddress.indexOf(","));   
	         }   
	     }
	     return ipAddress;    
	}  
	
	
	public static String getNetcardIP(String netCardName) throws Exception {
	    try {
	        InetAddress candidateAddress = null;
	        // 遍历所有的网络接口
	        for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
	            NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
	            System.out.println("iface.getName():" + iface.getName());
	            if(iface.getName().startsWith(netCardName)) {
	            	 // 在所有的接口下再遍历IP
		            for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
		                InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
		                System.out.println("inetAddr.getHostAddress():" + inetAddr.getHostAddress());
		                if (!inetAddr.isLoopbackAddress() && !(inetAddr instanceof Inet6Address)) {// 排除loopback类型地址以及ipv6类型
		                    if (inetAddr.isSiteLocalAddress()) {
		                        // 如果是site-local地址，就是它了
		                        return inetAddr.getHostAddress();
		                    } else if (candidateAddress == null) {
		                        // site-local类型的地址未被发现，先记录候选地址
		                        candidateAddress = inetAddr;
		                    }
		                }
		            }
	            }
	        }
	        if (candidateAddress != null) {
	            return candidateAddress.getHostAddress();
	        }
	        // 如果没有发现 non-loopback地址.只能用最次选的方案
	        InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
	        return jdkSuppliedAddress.getHostAddress();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}
	
	
	public static String getLocalHostLANAddress(String netCardName) throws Exception {
	    try {
	        InetAddress candidateAddress = null;
	        // 遍历所有的网络接口
	        for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
	            NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
	            // 在所有的接口下再遍历IP
	            for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
	                InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
	                if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
	                    if (inetAddr.isSiteLocalAddress()) {
	                        // 如果是site-local地址，就是它了
	                        return inetAddr.getHostAddress();
	                    } else if (candidateAddress == null) {
	                        // site-local类型的地址未被发现，先记录候选地址
	                        candidateAddress = inetAddr;
	                    }
	                }
	            }
	        }
	        if (candidateAddress != null) {
	            return candidateAddress.getHostAddress();
	        }
	        // 如果没有发现 non-loopback地址.只能用最次选的方案
	        InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
	        return jdkSuppliedAddress.getHostAddress();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}
}