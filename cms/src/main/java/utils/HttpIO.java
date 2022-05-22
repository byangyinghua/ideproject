package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*http读取参数以及返回结果助手*/

public class HttpIO {
	public static  String getBody(HttpServletRequest request) {
		
		BufferedReader br;
		String params = "";
		try {
			br = new BufferedReader(new InputStreamReader((ServletInputStream) request.getInputStream(), "utf-8"));
			StringBuffer sb = new StringBuffer("");
			String temp;
			while ((temp = br.readLine()) != null) { 
			  sb.append(temp);
			}
			br.close();
			params = sb.toString();
			params = params.replace("\\\"", "\"").replace("\\\\u", "\\u").replace("\"{", "{").replace("}\"", "}");
//			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!");
//			System.out.println("request json:" + params);
//			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return params;
	}
	
	public static void writeResp(HttpServletResponse response,Object obj){
		response.setCharacterEncoding("UTF-8"); //设置编码格式
		response.setContentType("text/plain;charset=utf-8" );    //设置数据格式
	    PrintWriter out = null;
		try {
			out = response.getWriter();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //获取写入对象
	    out.print(obj); //将json数据写入流中
	    out.flush();
	    out.close();
	}
	
	
	
	
	
	
	
	
	
	
}