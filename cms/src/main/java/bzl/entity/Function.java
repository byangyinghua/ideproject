package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * Function 实体类 对应 数据库 t_function ,
    */ 


public class Function{
	private int id;
	private String fun_id;
	private String fun_name;
	private int fun_level;
	private int fun_order;
	private String fun_url;
	private String parent_fun_url;
	private String fun_type;
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
	 * @return the fun_id
	 */
	public String getFun_id() {
		return fun_id;
	}
	/**
	 * @param fun_id the fun_id to set
	 */
	public void setFun_id(String fun_id) {
		this.fun_id = fun_id;
	}
	/**
	 * @return the fun_name
	 */
	public String getFun_name() {
		return fun_name;
	}
	/**
	 * @param fun_name the fun_name to set
	 */
	public void setFun_name(String fun_name) {
		this.fun_name = fun_name;
	}
	/**
	 * @return the fun_level
	 */
	public int getFun_level() {
		return fun_level;
	}
	/**
	 * @param fun_level the fun_level to set
	 */
	public void setFun_level(int fun_level) {
		this.fun_level = fun_level;
	}
	/**
	 * @return the fun_order
	 */
	public int getFun_order() {
		return fun_order;
	}
	/**
	 * @param fun_order the fun_order to set
	 */
	public void setFun_order(int fun_order) {
		this.fun_order = fun_order;
	}
	/**
	 * @return the fun_url
	 */
	public String getFun_url() {
		return fun_url;
	}
	/**
	 * @param fun_url the fun_url to set
	 */
	public void setFun_url(String fun_url) {
		this.fun_url = fun_url;
	}
	/**
	 * @return the parent_fun_url
	 */
	public String getParent_fun_url() {
		return parent_fun_url;
	}
	/**
	 * @param parent_fun_url the parent_fun_url to set
	 */
	public void setParent_fun_url(String parent_fun_url) {
		this.parent_fun_url = parent_fun_url;
	}
	/**
	 * @return the fun_type
	 */
	public String getFun_type() {
		return fun_type;
	}
	/**
	 * @param fun_type the fun_type to set
	 */
	public void setFun_type(String fun_type) {
		this.fun_type = fun_type;
	}
	
	
	
	
	
}

