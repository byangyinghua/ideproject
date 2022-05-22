package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * BootSetting 实体类 对应 数据库 t_boot_setting
    */ 


public class BootSetting{
	private int id;
	private String setting_id;
	private String boot_time;
	private String shutdown_time;
	private String week_days;
	private String creator;
	private String creator_uid;
	private Date update_time;
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
	 * @return the setting_id
	 */
	public String getSetting_id() {
		return setting_id;
	}
	/**
	 * @param setting_id the setting_id to set
	 */
	public void setSetting_id(String setting_id) {
		this.setting_id = setting_id;
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

