package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * UserLog 实体类 对应 数据库 t_user_log
    */ 


public class UserLog{
	private int id;
	private String uid;
	private String username;
	private String realname;
	private String action_type;
	private String action_content;
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
	 * @return the action_type
	 */
	public String getAction_type() {
		return action_type;
	}
	/**
	 * @param action_type the action_type to set
	 */
	public void setAction_type(String action_type) {
		this.action_type = action_type;
	}
	/**
	 * @return the action_content
	 */
	public String getAction_content() {
		return action_content;
	}
	/**
	 * @param action_content the action_content to set
	 */
	public void setAction_content(String action_content) {
		this.action_content = action_content;
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

