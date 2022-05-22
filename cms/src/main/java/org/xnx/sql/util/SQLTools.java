package org.xnx.sql.util;

public class SQLTools {
	/**
	 * 拼接in语句
	 * <p>mark by Hunter on 2017年8月20日 上午11:39:47.</p>
	 */
	public static String in(String pre, int count, String after){
		StringBuffer sb = new StringBuffer(pre);
		sb.append("(");
		for(int i = 0;i < count; i++){
			sb.append("?,");
		}
		sb.replace(sb.length()-1, sb.length(), ")");
		sb.append(after);
		return sb.toString();
	}
}
