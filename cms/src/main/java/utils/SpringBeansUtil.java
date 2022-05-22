/*------------------------------------------------------------------------------
 *    系统名称	： jxdy
 *    文件名	： SpringBeansUtil.java
 *              (©) 贵州交建信息科技有限公司 2016 All Rights Reserved.
 *
 *    注意： 本内容仅限于贵州交建信息科技有限公司内部使用，禁止转发
 *-----------------------------------------------------------------------------*/
package utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


/**
 * spring上下文工具类
 * <p>创建日期:2016年11月1日 下午1:58:39</p>
 * @author lgl
 */
public class SpringBeansUtil implements ApplicationContextAware{
	
	private static ApplicationContext context = null;
	
	public static Object getBean(String beanName){
		return context.getBean(beanName);
	}
	
	public static <T> T getBean(Class<T> type){
		return context.getBean(type);
	}
	
	public static ApplicationContext getContext(){
		return context;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		context = applicationContext;
	}
}
