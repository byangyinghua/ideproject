package bzl.entity;

import java.util.Date;

/**
 * Camera 实体类 对应 数据库 t_camera
 */ 

public class Camera {
	
	private Long id;
	private String camera_id;
	private String camera_ip;
	private String terminal_id; //关联的终端id
	private String channel;
	private String name;
	private String install_addr;
	private String brand; //摄像头品牌
	private Date create_time;
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
	 * @return the camera_id
	 */
	public String getCamera_id() {
		return camera_id;
	}
	/**
	 * @param camera_id the camera_id to set
	 */
	public void setCamera_id(String camera_id) {
		this.camera_id = camera_id;
	}
	/**
	 * @return the camera_ip
	 */
	public String getCamera_ip() {
		return camera_ip;
	}
	/**
	 * @param camera_ip the camera_ip to set
	 */
	public void setCamera_ip(String camera_ip) {
		this.camera_ip = camera_ip;
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
	 * @return the channel
	 */
	public String getChannel() {
		return channel;
	}
	/**
	 * @param channel the channel to set
	 */
	public void setChannel(String channel) {
		this.channel = channel;
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
	 * @return the install_addr
	 */
	public String getInstall_addr() {
		return install_addr;
	}
	/**
	 * @param install_addr the install_addr to set
	 */
	public void setInstall_addr(String install_addr) {
		this.install_addr = install_addr;
	}
	/**
	 * @return the brand
	 */
	public String getBrand() {
		return brand;
	}
	/**
	 * @param brand the brand to set
	 */
	public void setBrand(String brand) {
		this.brand = brand;
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