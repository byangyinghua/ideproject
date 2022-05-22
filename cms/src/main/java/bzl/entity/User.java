package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * User 实体类 对应 数据库 t_user
    */ 


public class User{
	private int id;
	private String uid;
	private String username;
	private String password;
	private String identity_num;
	private String phone_num;
	private String qq;
	private String weixin;
	private String mail;
	private String real_name;
	private int del_status;
	private int is_supper;
	private String avatar_img;
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
	 * @return the uid
	 */
	public String getUid() {
		return uid;
	}
	/**
	 * @param uid the uid to set
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}
	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * @return the identity_num
	 */
	public String getIdentity_num() {
		return identity_num;
	}
	/**
	 * @param identity_num the identity_num to set
	 */
	public void setIdentity_num(String identity_num) {
		this.identity_num = identity_num;
	}
	/**
	 * @return the phone_num
	 */
	public String getPhone_num() {
		return phone_num;
	}
	/**
	 * @param phone_num the phone_num to set
	 */
	public void setPhone_num(String phone_num) {
		this.phone_num = phone_num;
	}
	/**
	 * @return the qq
	 */
	public String getQq() {
		return qq;
	}
	/**
	 * @param qq the qq to set
	 */
	public void setQq(String qq) {
		this.qq = qq;
	}
	/**
	 * @return the weixin
	 */
	public String getWeixin() {
		return weixin;
	}
	/**
	 * @param weixin the weixin to set
	 */
	public void setWeixin(String weixin) {
		this.weixin = weixin;
	}
	/**
	 * @return the mail
	 */
	public String getMail() {
		return mail;
	}
	/**
	 * @param mail the mail to set
	 */
	public void setMail(String mail) {
		this.mail = mail;
	}
	/**
	 * @return the real_name
	 */
	public String getReal_name() {
		return real_name;
	}
	/**
	 * @param real_name the real_name to set
	 */
	public void setReal_name(String real_name) {
		this.real_name = real_name;
	}
	/**
	 * @return the del_status
	 */
	public int getDel_status() {
		return del_status;
	}
	/**
	 * @param del_status the del_status to set
	 */
	public void setDel_status(int del_status) {
		this.del_status = del_status;
	}
	/**
	 * @return the is_supper
	 */
	public int getIs_supper() {
		return is_supper;
	}
	/**
	 * @param is_supper the is_supper to set
	 */
	public void setIs_supper(int is_supper) {
		this.is_supper = is_supper;
	}
	/**
	 * @return the avatar_img
	 */
	public String getAvatar_img() {
		return avatar_img;
	}
	/**
	 * @param avatar_img the avatar_img to set
	 */
	public void setAvatar_img(String avatar_img) {
		this.avatar_img = avatar_img;
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

