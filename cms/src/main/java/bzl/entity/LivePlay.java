package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * LivePlay 实体类 对应 数据库 t_live_play
    */ 


public class LivePlay{
	private int id;
	private String live_id;
	private String name;
	private String attach_id;
	private String attach_name;
	private String start_time;
	private Date create_time;
	private int state;
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
	 * @return the live_id
	 */
	public String getLive_id() {
		return live_id;
	}
	/**
	 * @param live_id the live_id to set
	 */
	public void setLive_id(String live_id) {
		this.live_id = live_id;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the attach_id
	 */
	public String getAttach_id() {
		return attach_id;
	}
	/**
	 * @param attach_id the attach_id to set
	 */
	public void setAttach_id(String attach_id) {
		this.attach_id = attach_id;
	}
	/**
	 * @return the attach_name
	 */
	public String getAttach_name() {
		return attach_name;
	}
	/**
	 * @param attach_name the attach_name to set
	 */
	public void setAttach_name(String attach_name) {
		this.attach_name = attach_name;
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
	 * @return the state
	 */
	public int getState() {
		return state;
	}
	/**
	 * @param state the state to set
	 */
	public void setState(int state) {
		this.state = state;
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

