package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * User 实体类 对应 数据库 t_attach_file
    */ 


public class Attachment{
	private int id;
	private String attach_id;
	private String upload_user;
	private String upload_uid;
	private String name;
	private String save_path;
	private Long size;
	private int attach_type;
	private String apk_version;
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
	 * @return the upload_user
	 */
	public String getUpload_user() {
		return upload_user;
	}
	/**
	 * @param upload_user the upload_user to set
	 */
	public void setUpload_user(String upload_user) {
		this.upload_user = upload_user;
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
	 * @return the save_path
	 */
	public String getSave_path() {
		return save_path;
	}
	/**
	 * @param save_path the save_path to set
	 */
	public void setSave_path(String save_path) {
		this.save_path = save_path;
	}
	/**
	 * @return the size
	 */
	public Long getSize() {
		return size;
	}
	/**
	 * @param size the size to set
	 */
	public void setSize(Long size) {
		this.size = size;
	}
	/**
	 * @return the attach_type
	 */
	public int getAttach_type() {
		return attach_type;
	}
	/**
	 * @param attach_type the attach_type to set
	 */
	public void setAttach_type(int attach_type) {
		this.attach_type = attach_type;
	}
	/**
	 * @return the apk_version
	 */
	public String getApk_version() {
		return apk_version;
	}
	/**
	 * @param apk_version the apk_version to set
	 */
	public void setApk_version(String apk_version) {
		this.apk_version = apk_version;
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
	public String getUpload_uid() {
		return upload_uid;
	}
	public void setUpload_uid(String upload_uid) {
		this.upload_uid = upload_uid;
	}
	
	
	
}

