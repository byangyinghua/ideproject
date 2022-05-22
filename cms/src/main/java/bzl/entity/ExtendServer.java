package bzl.entity;

import java.util.Date;

/**
 * ExtendServer 实体类 对应 数据库 t_extend_servers
 */ 



public class ExtendServer {
	
	private Long id;
	private String server_id;
	private String server_ip;
	private String server_type;
	private String server_name;
	private String login_name;
	private String login_pwd;
	private Date create_time;
	private Date update_time;
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return the server_id
	 */
	public String getServer_id() {
		return server_id;
	}
	/**
	 * @param server_id the server_id to set
	 */
	public void setServer_id(String server_id) {
		this.server_id = server_id;
	}
	/**
	 * @return the server_ip
	 */
	public String getServer_ip() {
		return server_ip;
	}
	/**
	 * @param server_ip the server_ip to set
	 */
	public void setServer_ip(String server_ip) {
		this.server_ip = server_ip;
	}
	/**
	 * @return the server_type
	 */
	public String getServer_type() {
		return server_type;
	}
	/**
	 * @param server_type the server_type to set
	 */
	public void setServer_type(String server_type) {
		this.server_type = server_type;
	}
	/**
	 * @return the server_name
	 */
	public String getServer_name() {
		return server_name;
	}
	/**
	 * @param server_name the server_name to set
	 */
	public void setServer_name(String server_name) {
		this.server_name = server_name;
	}
	/**
	 * @return the login_name
	 */
	public String getLogin_name() {
		return login_name;
	}
	/**
	 * @param login_name the login_name to set
	 */
	public void setLogin_name(String login_name) {
		this.login_name = login_name;
	}
	/**
	 * @return the login_pwd
	 */
	public String getLogin_pwd() {
		return login_pwd;
	}
	/**
	 * @param login_pwd the login_pwd to set
	 */
	public void setLogin_pwd(String login_pwd) {
		this.login_pwd = login_pwd;
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
	
	
}