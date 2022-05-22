package utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 字符串处理及转换工具类
 * 
 * @author 郑成功
 */
public class TimeUtil {

	/**
	 * 日期相加减
	 * 
	 * @param time 时间字符串 yyyy-MM-dd HH:mm:ss
	 * @param num  加的数，-num就是减去
	 * @return 减去相应的数量的年的日期
	 * @throws ParseException
	 */
	public static Date yearAddNum(Date time, Integer num) {
		// SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// Date date = format.parse(time);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(time);
		calendar.add(Calendar.YEAR, num);
		Date newTime = calendar.getTime();
		return newTime;
	}

	/**
	 * 
	 * @param time 时间
	 * @param num  加的数，-num就是减去
	 * @return 减去相应的数量的月份的日期
	 * @throws ParseException Date
	 */
	public static Date monthAddNum(Date time, Integer num) {
		// SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// Date date = format.parse(time);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(time);
		calendar.add(Calendar.MONTH, num);
		Date newTime = calendar.getTime();
		return newTime;
	}

	/**
	 * 
	 * @param time 时间
	 * @param num  加的数，-num就是减去
	 * @return 减去相应的数量的天的日期
	 * @throws ParseException Date
	 */
	public static Date dayAddNum(Date time, Integer num) {
		// SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// Date date = format.parse(time);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(time);
		calendar.add(Calendar.DAY_OF_MONTH, num);
		Date newTime = calendar.getTime();
		return newTime;
	}

	/**
	 * 获取本月第一天时间
	 */
	public static Date getMonthStartDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		return calendar.getTime();
	}

	/**
	 * 获取本月最后一天
	 * 
	 */
	public static Date getMonthEndDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		return calendar.getTime();
	}

	/**
	 * 获取本周的开始时间
	 */
	public static Date getBeginWeekDate() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		int dayofweek = cal.get(Calendar.DAY_OF_WEEK);
		// 周日是1 ，周一是 2 ，周二是 3
		// 所以，当周的第一天 = 当前日期 - 距离周一过了几天（周一过了0天，周二过了1天， 周日过了6天）
		// 2 - 周一的（dayofweek：2 ）= 0
		// 2 - 周二的（dayofweek：3 ）= -1
		// .
		// .
		// 2 - 周日的（dayofweek：1） = 1（这个是不符合的需要我们修改）===》2 - 周日的（dayofweek：1 ==》8 ） = -6
		if (dayofweek == 1) {
			dayofweek += 7;
		}
		cal.add(Calendar.DATE, 2 - dayofweek);
		return cal.getTime();
	}

	/**
	 * 本周的结束时间 开始时间 + 6天
	 */
	public static Date getEndWeekDate() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		int dayofweek = cal.get(Calendar.DAY_OF_WEEK);
		if (dayofweek == 1) {
			dayofweek += 7;
		}
		cal.add(Calendar.DATE, 8 - dayofweek);// 2 - dayofweek + 6
		return cal.getTime();
	}
	
    public static String dayForWeek(String pTime) throws Throwable {  
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");  
        Date tmpDate = format.parse(pTime);  
        Calendar cal = Calendar.getInstance(); 
        String[] weekDays = { "7", "1", "2", "3", "4", "5", "6" };
        try {
            cal.setTime(tmpDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1; // 指示一个星期中的某天。
        if (w < 0)
            w = 0;
        return weekDays[w];
    } 
	
	//计算任务发送成功与失败缓存生存期间 单位秒
	public static  int caculateLiveTime(String endDateStr) {
	        //注意：SimpleDateFormat构造函数的样式与strDate的样式必须相符
		  if(endDateStr==null) {
				 System.out.println("caculateLiveTime errr endDateStr=" + endDateStr);
			  return 0;
		  }
	        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
	        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
	        //SimpleDateFormat sDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //加上时间
	        //必须捕获异常
	        try {
				Date endDate=simpleDateFormat.parse(endDateStr);
				Long endDateTmp = endDate.getTime() + 24*3600*1000;
				return (int) ((endDateTmp - new Date().getTime())/1000);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 1;
		}

}