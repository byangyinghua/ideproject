package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * Help 实体类 对应 数据库 t_help_info
    */ 


public class HelpInfo{
	private int id;
	private String help_id;
	private String terminal_id;
	private String terminal_ip;
	private String help_time;
	private String sent_task;
	private int help_status;
	private String video_list;
	private Date   create_time;
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
	 * @return the help_id
	 */
	public String getHelp_id() {
		return help_id;
	}
	/**
	 * @param help_id the help_id to set
	 */
	public void setHelp_id(String help_id) {
		this.help_id = help_id;
	}
	/**
	 * @return the terminal_id
	 */
	public String getTerminal_id() {
		return terminal_id;
	}
	/**
	 * @param terminal_id the terminal_id to set
	 */
	public void setTerminal_id(String terminal_id) {
		this.terminal_id = terminal_id;
	}
	/**
	 * @return the terminal_ip
	 */
	public String getTerminal_ip() {
		return terminal_ip;
	}
	/**
	 * @param terminal_ip the terminal_ip to set
	 */
	public void setTerminal_ip(String terminal_ip) {
		this.terminal_ip = terminal_ip;
	}
	/**
	 * @return the help_time
	 */
	public String getHelp_time() {
		return help_time;
	}
	/**
	 * @param help_time the help_time to set
	 */
	public void setHelp_time(String help_time) {
		this.help_time = help_time;
	}
	/**
	 * @return the sent_task
	 */
	public String getSent_task() {
		return sent_task;
	}
	/**
	 * @param sent_task the sent_task to set
	 */
	public void setSent_task(String sent_task) {
		this.sent_task = sent_task;
	}
	/**
	 * @return the help_status
	 */
	public int getHelp_status() {
		return help_status;
	}
	/**
	 * @param help_status the help_status to set
	 */
	public void setHelp_status(int help_status) {
		this.help_status = help_status;
	}
	/**
	 * @return the video_list
	 */
	public String getVideo_list() {
		return video_list;
	}
	/**
	 * @param video_list the video_list to set
	 */
	public void setVideo_list(String video_list) {
		this.video_list = video_list;
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

