/*------------------------------------------------------------------------------
 *    系统名称	： account
 *    文件名	： JDBCTransaction.java
 *              (©) 贵州博爻信息科技有限公司 2016 All Rights Reserved.
 *
 *    注意： 本内容仅限于贵州博爻信息科技有限公司内部使用，禁止转发
 *-----------------------------------------------------------------------------*/
package bzl.service.impl;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import utils.SpringBeansUtil;

/**
 * JDBC事物控制类
 * @author 龙光磊  [2017年5月2日 下午12:09:36]
 */
public class JDBCTransaction {
	
	/**
	 * 获取数据源
	 * <p>create/mark by 龙光磊 [2017年5月2日 下午2:53:18]</p>
	 * @return
	 */
	private static DataSource getDataSource(){
		return SpringBeansUtil.getBean(BasicDataSource.class);
	}
	
	/**
	 * 获取jdbcTemplate，执行数据库操作
	 * <p>create/mark by 龙光磊 [2017年5月2日 下午2:53:27]</p>
	 * @return
	 */
	public static JdbcTemplate getTemplate(){
		return new JdbcTemplate(getDataSource());
	}
	
	/**
	 * 手动事务控制
	 * <p>create/mark by 龙光磊 [2017年5月2日 下午2:53:49]</p>
	 * @param callBack
	 * @return
	 */
	public static <T> T performanceOperation(TransactionCallback<T> callBack) {
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(getDataSource());
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		T result = null;
		try {
			result = transactionTemplate.execute(callBack);
		} catch (TransactionException e) {
		}
		return result;
	}
}
