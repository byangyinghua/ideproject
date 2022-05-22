package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * LoginLog 实体类 对应 数据库 t_login_log
    */ 


public class LoginLog{
	private int id;
	private String uid;
	private String username;
	private String realname;
	private String login_type;
	private Date login_time;
	private String login_ip;
	private Date create_time;
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * @return the uid
	 */
	public String getUid() {
		return uid;
	}
	/**
	 * @param uid the uid to set
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}
	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	/**
	 * @return the realname
	 */
	public String getRealname() {
		return realname;
	}
	/**
	 * @param realname the realname to set
	 */
	public void setRealname(String realname) {
		this.realname = realname;
	}
	/**
	 * @return the login_type
	 */
	public String getLogin_type() {
		return login_type;
	}
	/**
	 * @param login_type the login_type to set
	 */
	public void setLogin_type(String login_type) {
		this.login_type = login_type;
	}
	/**
	 * @return the login_time
	 */
	public Date getLogin_time() {
		return login_time;
	}
	/**
	 * @param login_time the login_time to set
	 */
	public void setLogin_time(Date login_time) {
		this.login_time = login_time;
	}
	/**
	 * @return the login_ip
	 */
	public String getLogin_ip() {
		return login_ip;
	}
	/**
	 * @param login_ip the login_ip to set
	 */
	public void setLogin_ip(String login_ip) {
		this.login_ip = login_ip;
	}
	/**
	 * @return the create_time
	 */
	public Date getCreate_time() {
		return create_time;
	}
	/**
	 * @param create_time the create_time to set
	 */
	public void setCreate_time(Date create_time) {
		this.create_time = create_time;
	}
}

