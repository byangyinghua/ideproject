package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * UrgencyTask 实体类 对应 数据库 t_urgency_task
    */ 


public class UrgencyTask{
	private long id;
	private String urgency_id;
	private String title;
	private String content;
	private String creator;
	private String creator_uid;
	private Date start_time;
	private Date create_time;
	private Date update_time;
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return the urgency_id
	 */
	public String getUrgency_id() {
		return urgency_id;
	}
	/**
	 * @param urgency_id the urgency_id to set
	 */
	public void setUrgency_id(String urgency_id) {
		this.urgency_id = urgency_id;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
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
	 * @return the start_time
	 */
	public Date getStart_time() {
		return start_time;
	}
	/**
	 * @param start_time the start_time to set
	 */
	public void setStart_time(Date start_time) {
		this.start_time = start_time;
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

