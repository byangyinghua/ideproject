package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * TaskInfo 实体类 对应 数据库 t_task_info
    */ 


public class TaskInfo{
	private int id;
	private String task_id;
	private String create_user;
	private String task_name;
	private int task_type;
	private String plan_type;
	private int priority;
	private String start_date;
	private String end_date;
	private String play_periods;
	private int play_mode;
	private String play_weekdays;
	private String content;
	private Date update_time;
	private Date create_time;
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
	 * @return the task_id
	 */
	public String getTask_id() {
		return task_id;
	}
	/**
	 * @param task_id the task_id to set
	 */
	public void setTask_id(String task_id) {
		this.task_id = task_id;
	}
	/**
	 * @return the create_user
	 */
	public String getCreate_user() {
		return create_user;
	}
	/**
	 * @param create_user the create_user to set
	 */
	public void setCreate_user(String create_user) {
		this.create_user = create_user;
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
	/**
	 * @return the task_type
	 */
	public int getTask_type() {
		return task_type;
	}
	/**
	 * @param task_type the task_type to set
	 */
	public void setTask_type(int task_type) {
		this.task_type = task_type;
	}
	/**
	 * @return the plan_type
	 */
	public String getPlan_type() {
		return plan_type;
	}
	/**
	 * @param plan_type the plan_type to set
	 */
	public void setPlan_type(String plan_type) {
		this.plan_type = plan_type;
	}
	/**
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}
	/**
	 * @param priority the priority to set
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}
	/**
	 * @return the start_date
	 */
	public String getStart_date() {
		return start_date;
	}
	/**
	 * @param start_date the start_date to set
	 */
	public void setStart_date(String start_date) {
		this.start_date = start_date;
	}
	/**
	 * @return the end_date
	 */
	public String getEnd_date() {
		return end_date;
	}
	/**
	 * @param end_date the end_date to set
	 */
	public void setEnd_date(String end_date) {
		this.end_date = end_date;
	}
	/**
	 * @return the play_periods
	 */
	public String getPlay_periods() {
		return play_periods;
	}
	/**
	 * @param play_periods the play_periods to set
	 */
	public void setPlay_periods(String play_periods) {
		this.play_periods = play_periods;
	}
	/**
	 * @return the play_mode
	 */
	public int getPlay_mode() {
		return play_mode;
	}
	/**
	 * @param play_mode the play_mode to set
	 */
	public void setPlay_mode(int play_mode) {
		this.play_mode = play_mode;
	}
	/**
	 * @return the play_weekdays
	 */
	public String getPlay_weekdays() {
		return play_weekdays;
	}
	/**
	 * @param play_weekdays the play_weekdays to set
	 */
	public void setPlay_weekdays(String play_weekdays) {
		this.play_weekdays = play_weekdays;
	}
	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}
	/**
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
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
	public String getCreator_uid() {
		return creator_uid;
	}
	public void setCreator_uid(String creator_uid) {
		this.creator_uid = creator_uid;
	}
	
	
}

