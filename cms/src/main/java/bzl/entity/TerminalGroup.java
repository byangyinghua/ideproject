package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * TerminalGroup 实体类 对应 数据库 t_terminal_group
    */ 


public class TerminalGroup{
	private int id;
	private String gid;
	private String group_code;
	private String group_name;
	private String group_type;
	private int terminal_cnt;
	private String add_uid;
	private Date   update_time;
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
	 * @return the gid
	 */
	public String getGid() {
		return gid;
	}
	/**
	 * @param gid the gid to set
	 */
	public void setGid(String gid) {
		this.gid = gid;
	}
	/**
	 * @return the group_code
	 */
	public String getGroup_code() {
		return group_code;
	}
	/**
	 * @param group_code the group_code to set
	 */
	public void setGroup_code(String group_code) {
		this.group_code = group_code;
	}
	/**
	 * @return the group_name
	 */
	public String getGroup_name() {
		return group_name;
	}
	/**
	 * @param group_name the group_name to set
	 */
	public void setGroup_name(String group_name) {
		this.group_name = group_name;
	}
	/**
	 * @return the group_type
	 */
	public String getGroup_type() {
		return group_type;
	}
	/**
	 * @param group_type the group_type to set
	 */
	public void setGroup_type(String group_type) {
		this.group_type = group_type;
	}
	/**
	 * @return the add_uid
	 */
	public String getAdd_uid() {
		return add_uid;
	}
	/**
	 * @param add_uid the add_uid to set
	 */
	public void setAdd_uid(String add_uid) {
		this.add_uid = add_uid;
	}
	/**
	 * @return the update_time
	 */
	public Date getUpdate_time() {
		return update_time;
	}
	/**
	 * @param update_time the update_time to set
	 */
	public void setUpdate_time(Date update_time) {
		this.update_time = update_time;
	}
	
}

