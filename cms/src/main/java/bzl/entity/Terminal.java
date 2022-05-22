package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * Terminal 实体类 对应 数据库 t_terminal
    */ 


public class Terminal{
	private int id;
	private String terminal_id;
	private String ip;
	private String gids;
	private String name;
	private String err_msg; //异常信息
	private String install_addr;
	private int volume;
	private String app_ver;
	private String boot_time;
	private String shutdown_time;
	private String lamp_status;
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
	 * @return the ip
	 */
	public String getIp() {
		return ip;
	}
	/**
	 * @param ip the ip to set
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}
	/**
	 * @return the gids
	 */
	public String getGids() {
		return gids;
	}
	/**
	 * @param gids the gids to set
	 */
	public void setGids(String gids) {
		this.gids = gids;
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
	 * @return the err_msg
	 */
	public String getErr_msg() {
		return err_msg;
	}
	/**
	 * @param err_msg the err_msg to set
	 */
	public void setErr_msg(String err_msg) {
		this.err_msg = err_msg;
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
	 * @return the volume
	 */
	public int getVolume() {
		return volume;
	}
	/**
	 * @param volume the volume to set
	 */
	public void setVolume(int volume) {
		this.volume = volume;
	}
	/**
	 * @return the app_ver
	 */
	public String getApp_ver() {
		return app_ver;
	}
	/**
	 * @param app_ver the app_ver to set
	 */
	public void setApp_ver(String app_ver) {
		this.app_ver = app_ver;
	}
	/**
	 * @return the boot_time
	 */
	public String getBoot_time() {
		return boot_time;
	}
	/**
	 * @param boot_time the boot_time to set
	 */
	public void setBoot_time(String boot_time) {
		this.boot_time = boot_time;
	}
	/**
	 * @return the shutdown_time
	 */
	public String getShutdown_time() {
		return shutdown_time;
	}
	/**
	 * @param shutdown_time the shutdown_time to set
	 */
	public void setShutdown_time(String shutdown_time) {
		this.shutdown_time = shutdown_time;
	}
	/**
	 * @return the lamp_status
	 */
	public String getLamp_status() {
		return lamp_status;
	}
	/**
	 * @param lamp_status the lamp_status to set
	 */
	public void setLamp_status(String lamp_status) {
		this.lamp_status = lamp_status;
	}

}

