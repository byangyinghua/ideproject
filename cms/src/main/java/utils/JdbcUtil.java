/*
 * 创建日期 2011-11-21
 *
 * TODO 要更改此生成的文件的模板，请转至
 * 窗口 － 首选项 － Java － 代码样式 － 代码模板
 */
package utils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;
import bzl.service.impl.JDBCTransaction;

import sun.rmi.log.LogHandler;


/**
 * 
 * @author 唐旭峰
 */
public class JdbcUtil {

	public Connection conn = null;

    public Connection getConn() {
        return conn;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    private InitialContext ctx;

    private DataSource ds;

    private Statement statement = null;

    private ResultSet rs = null;

    private PreparedStatement pstmt = null;

    private CallableStatement cst = null;

    public JdbcUtil(){
       //JdbcUtil(MyListener.getDriver(),MyListener.getUrl(),MyListener.getUsername(),MyListener.getPassword());
    };
    
    /**
     * 加载JDBC访问Oracle驱动
     * @param url 连接字符串
     * @param username 连接用户名
     * @param password 连接密码
     */
    public JdbcUtil(String driver,String url,String username,String password)
    {
    	try {
			conn = JDBCTransaction.getTemplate().getDataSource().getConnection();
		} catch (Exception e) {
	    	//System.out.println(url+","+username+","+password);
	        try {    
	        	//System.out.println(url+","+username+","+password);
	        	Class.forName(driver);
	        	// 龙光磊 从mybatis连接池中获取连接
//	        	conn = SessionFactory.getInstance().getSqlSessionFactory().openSession().getConnection();
	        	conn =  DriverManager.getConnection(url, username, password);   
	        }
	        catch (Exception e2)
	    	{    
	        	//System.out.println(driver+","+url+","+username+","+password);
	    		System.out.println("不能连接数据库");
	    	    return;   
	    	}
		}
    }
    /**
     * 执行一句查询sql 并且返回一个ResultSet
     * @param sql 
     * @return 结果集
     * @throws SQLException 
     */
    public ResultSet executequery(String sql) throws SQLException {
        try {
            statement = conn.createStatement();
            rs = statement.executeQuery(sql);
        } catch (SQLException e) {
        	//conn.rollback(); 
            System.out.println("查询操作出错：" + e.getMessage());
            throw e;
        }
        return rs;
    }
    
    public ResultSet execute(String sql) throws SQLException {
        try {
            statement = conn.createStatement();
          //  statement.execute(sql)
           statement.execute(sql);
        } catch (SQLException e) {
        	//conn.rollback();
            System.out.println("查询操作出错：" + e.getMessage());
            throw e;
        }
        return rs;
    }
    
    public boolean hasRecord(String sql) throws SQLException {
    	boolean result=false;
        try {
            statement = conn.createStatement();
            rs = statement.executeQuery(sql);
            if(rs.next()){
            	result=true;
            }
        } catch (SQLException e) {
        	//conn.rollback(); 
            System.out.println("查询操作出错：" + e.getMessage());
   		 	throw e;

        }
        return result;
    }

    /**
     * 执行存储过程 并且返回一个RelsultSet
     * 
     * @param call
     *            存储过程
     * @param map
     *            参数
     * @return 结果集
     * @throws SQLException 
     */
    public ResultSet executequery(String call, Map map) throws SQLException {
        try {
            cst = conn.prepareCall(call);
            int count = 0;
            while (call.indexOf("?") != -1) {
                count++;
                call = call.substring(call.indexOf("?") + "?".length());
            }
            for (int i = 1; i<=count-1;i++) {
                Object value = map.get(String.valueOf(i));
                //                if (value instanceof String) {
                //                    cst.setString(i, (String) value);
                //                } else if (value instanceof Integer) {
                //                    cst.setInt(i, ((Integer) value).intValue());
                //                }else if (value instanceof Long) {
                //                    cst.setLong(i, ((Long) value).longValue());
                //                } else if (value instanceof Double) {
                //                    cst.setDouble(i, ((Double) value).doubleValue());
                //                } else if (value instanceof Float) {
                //                    cst.setFloat(i, ((Float) value).floatValue());
                //                } else if (value instanceof Short) {
                //                    cst.setShort(i, ((Short) value).shortValue());
                //                } else if (value instanceof Byte) {
                //                    cst.setByte(i, ((Byte) value).byteValue());
                //                } else if (value instanceof BigDecimal) {
                //                    cst.setBigDecimal(i, (BigDecimal) value);
                //                } else if (value instanceof Boolean) {
                //                    cst.setBoolean(i, ((Boolean) value).booleanValue());
                //                } else if (value instanceof Timestamp) {
                //                    cst.setTimestamp(i, (Timestamp) value);
                //                } else if (value instanceof java.util.Date) {
                //                    cst.setDate(i, new java.sql.Date(((java.util.Date)
                // value).getTime()));
                //                } else if (value instanceof java.sql.Date) {
                //                    cst.setDate(i, (java.sql.Date) value);
                //                } else if (value instanceof Time) {
                //                    cst.setTime(i, (Time) value);
                //                } else if (value instanceof Blob) {
                //                    cst.setBlob(i, (Blob) value);
                //                } else if (value instanceof Clob) {
                //                    cst.setClob(i, (Clob) value);
                //                } else {
                cst.setObject(i, value);
                //                }
            }
           // cst.registerOutParameter(count, OracleTypes.CURSOR);
            cst.execute();
            rs = (ResultSet) cst.getObject(count);
        } catch (SQLException e) {
            System.out.println("执行存储过程出错：" + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return rs;
    }

    public ResultSet executequery(String sql, Object[] parmas) {
        try {
            pstmt = conn.prepareStatement(sql);
            if (parmas != null) {
				for (int i = 0, colnum = parmas.length; i < colnum; i++) {
					pstmt.setObject(i + 1, parmas[i]);
				}
			}
			rs = pstmt.executeQuery();
        } catch (SQLException e) {
            System.out.println("查询操作：" + e.getMessage());
        }
        return rs;
    }
    
    /**
     * 执行sql更新
     * <p>create or modify by 龙光磊 [2016年10月26日 上午9:44:51]</p>
     * @param sql
     * @param params
     * @return
     */
    public int executeUpdate(String sql, Object[] params){
    	try {
            pstmt = conn.prepareStatement(sql);
			if (params != null) {
				for (int i = 0, colnum = params.length; i < colnum; i++) {
					pstmt.setObject(i + 1, params[i]);
				}
			}
			return pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	return -1;
    }

    /**
     * 分页查询
     * <p>create or modify by 龙光磊 [2016年10月9日 下午4:02:04]</p>
     * @param sql
     * @param page
     * @param rows
     * @param parmas
     * @return
     */
    public ResultSet executequery(String sql, int page, int rows, Object[] parmas) {
        int absolute = page * rows - rows;
        int max = absolute + rows;
    	try {
            pstmt = conn.prepareStatement(sql);
            if (parmas != null) {
				for (int i = 0, colnum = parmas.length; i < colnum; i++) {
					pstmt.setObject(i + 1, parmas[i]);
				}
			}
            pstmt.setMaxRows(max);
			rs = pstmt.executeQuery();
			rs.absolute(absolute);
        } catch (SQLException e) {
            System.out.println("查询操作：" + e.getMessage());
            
            
        }
        return rs;
    }

    /**
     * 执行一句更新sql语句 成功>0 失败=0
     * 
     * @param sql语句
     * @return
     * @throws SQLException 
     */
    public int executeupdate(String sql) throws SQLException {
        int i = 0;
    	 statement = conn.createStatement();
        i = statement.executeUpdate(sql);
        System.out.println("sql===="+sql);
           // conn.commit();
       
        return i;
    }
    
    /**
     * 执行一句更新sql语句 成功>0 失败=0
     * 
     * @param sql语句
     * @return
     * @throws SQLException 
     */
    public int executeupdate(String deletesql,String sql) throws SQLException {
        int i = 0;
        try {
            statement = conn.createStatement();
            i = statement.executeUpdate(deletesql);
            i = statement.executeUpdate(sql);
        } catch (SQLException e) {
        	//conn.rollback(); 
            System.out.println("修改操作：" + e.getMessage());
            throw e;

        }
        finally{
        	close();
        }
        return i;

    }
    
    
    /**
     * 执行一句更新sql语句 成功>0 失败=0
     * 
     * @param sql语句
     * @return
     * @throws SQLException 
     */
    public int createTable(String sql,String sql1) throws SQLException {
        int i = 1;
        try {
            statement = conn.createStatement();
            statement.executeUpdate(sql);
            statement.executeUpdate(sql1);
        } catch (SQLException e) {
        	//conn.rollback(); 
            System.out.println("修改操作：" + e.getMessage());
            i=0;
            throw e;

        }
        finally{
        	close();
        }
        return i;

    }

    /**
     * 获取当前数据接口中结果集
     * 
     * @return
     */
    public ResultSet getresultset() {
        ResultSet rsnow = null;
        try {
            statement = conn.createStatement();
            rsnow = statement.getResultSet();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rsnow;
    }

    /**
     * 关闭带rs的连接
     */
    public void close() {
        if (rs != null) {
            try {
                rs.close();
                rs=null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
				conn.close();
				conn=null;
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }
    }

    /**
     * 关闭连接
     */
    public void closeconn() {
        if (conn != null) {
            try {
                conn.close();
                conn = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}