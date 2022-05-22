package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * BoyaoSystem 实体类 对应 数据库 t_system_info
    */ 


public class BoyaoSystem{
	private int id;
	private String info_id;
	private String key_word;
	private String content;
	private Date create_time;
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
	 * @return the info_id
	 */
	public String getInfo_id() {
		return info_id;
	}
	/**
	 * @param info_id the info_id to set
	 */
	public void setInfo_id(String info_id) {
		this.info_id = info_id;
	}
	/**
	 * @return the key_word
	 */
	public String getKey_word() {
		return key_word;
	}
	/**
	 * @param key_word the key_word to set
	 */
	public void setKey_word(String key_word) {
		this.key_word = key_word;
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

