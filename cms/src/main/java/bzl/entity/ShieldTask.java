package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * ShieldTask 实体类 对应 数据库 t_shield_task
    */ 


public class ShieldTask{
	private int id;
	private String shield_id;
	private String start_time;
	private String end_time;
	private String week_days;
	private String task_name;
	private String creator;
	private String creator_uid;
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
	 * @return the shield_id
	 */
	public String getShield_id() {
		return shield_id;
	}
	/**
	 * @param shield_id the shield_id to set
	 */
	public void setShield_id(String shield_id) {
		this.shield_id = shield_id;
	}
	/**
	 * @return the start_time
	 */
	public String getStart_time() {
		return start_time;
	}
	/**
	 * @param start_time the start_time to set
	 */
	public void setStart_time(String start_time) {
		this.start_time = start_time;
	}
	/**
	 * @return the end_time
	 */
	public String getEnd_time() {
		return end_time;
	}
	/**
	 * @param end_time the end_time to set
	 */
	public void setEnd_time(String end_time) {
		this.end_time = end_time;
	}
	/**
	 * @return the week_days
	 */
	public String getWeek_days() {
		return week_days;
	}
	/**
	 * @param week_days the week_days to set
	 */
	public void setWeek_days(String week_days) {
		this.week_days = week_days;
	}
	/**
	 * @return the task_name
	 */
	public String getTask_name() {
		return task_name;
	}
	/**
	 * @param task_name the task_name to set
	 */
	public void setTask_name(String task_name) {
		this.task_name = task_name;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public String getCreator_uid() {
		return creator_uid;
	}
	public void setCreator_uid(String creator_uid) {
		this.creator_uid = creator_uid;
	}
	
	
}

