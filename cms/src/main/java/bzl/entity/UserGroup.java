package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * UserGroup 实体类 对应 数据库 t_user_group
    */ 


public class UserGroup{
	private int id;
	private String group_id;
	private String group_name;
	private String terminal_groups;
	private String user_members;
	private Date create_time;
	private Date update_time;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getGroup_id() {
		return group_id;
	}
	public void setGroup_id(String group_id) {
		this.group_id = group_id;
	}
	public String getGroup_name() {
		return group_name;
	}
	public void setGroup_name(String group_name) {
		this.group_name = group_name;
	}
	public String getTerminal_groups() {
		return terminal_groups;
	}
	public void setTerminal_groups(String terminal_groups) {
		this.terminal_groups = terminal_groups;
	}
	public String getUser_members() {
		return user_members;
	}
	public void setUser_members(String user_members) {
		this.user_members = user_members;
	}
	public Date getCreate_time() {
		return create_time;
	}
	public void setCreate_time(Date create_time) {
		this.create_time = create_time;
	}
	public Date getUpdate_time() {
		return update_time;
	}
	public void setUpdate_time(Date update_time) {
		this.update_time = update_time;
	}

	
}

