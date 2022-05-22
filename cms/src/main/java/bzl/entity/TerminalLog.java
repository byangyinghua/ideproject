package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * TerminalLog 实体类 对应 数据库 t_terminal_log
    */ 


public class TerminalLog{
	private int id;
	private String terminal_id;
	private String terminal_ip;
	private String action;
	private String content;
	private String result;
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
	 * @return the action
	 */
	public String getAction() {
		return action;
	}
	/**
	 * @param action the action to set
	 */
	public void setAction(String action) {
		this.action = action;
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
	 * @return the result
	 */
	public String getResult() {
		return result;
	}
	/**
	 * @param result the result to set
	 */
	public void setResult(String result) {
		this.result = result;
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

