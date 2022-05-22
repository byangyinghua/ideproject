package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * TerminalUpdate 实体类 对应 数据库 t_terminal_update
    */ 


public class TerminalUpdate{
	private int id;
	private String apk_name;
	private String new_version;
	private String op_user;
	private int total_terminal_cnt;
	private int ok_terminal_cnt;
	private int fail_terminal_cnt;
	private String create_time;
	private String end_time;
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
	 * @return the apk_name
	 */
	public String getApk_name() {
		return apk_name;
	}
	/**
	 * @param apk_name the apk_name to set
	 */
	public void setApk_name(String apk_name) {
		this.apk_name = apk_name;
	}
	/**
	 * @return the new_version
	 */
	public String getNew_version() {
		return new_version;
	}
	/**
	 * @param new_version the new_version to set
	 */
	public void setNew_version(String new_version) {
		this.new_version = new_version;
	}
	/**
	 * @return the op_user
	 */
	public String getOp_user() {
		return op_user;
	}
	/**
	 * @param op_user the op_user to set
	 */
	public void setOp_user(String op_user) {
		this.op_user = op_user;
	}
	/**
	 * @return the total_terminal_cnt
	 */
	public int getTotal_terminal_cnt() {
		return total_terminal_cnt;
	}
	/**
	 * @param total_terminal_cnt the total_terminal_cnt to set
	 */
	public void setTotal_terminal_cnt(int total_terminal_cnt) {
		this.total_terminal_cnt = total_terminal_cnt;
	}
	/**
	 * @return the ok_terminal_cnt
	 */
	public int getOk_terminal_cnt() {
		return ok_terminal_cnt;
	}
	/**
	 * @param ok_terminal_cnt the ok_terminal_cnt to set
	 */
	public void setOk_terminal_cnt(int ok_terminal_cnt) {
		this.ok_terminal_cnt = ok_terminal_cnt;
	}
	/**
	 * @return the fail_terminal_cnt
	 */
	public int getFail_terminal_cnt() {
		return fail_terminal_cnt;
	}
	/**
	 * @param fail_terminal_cnt the fail_terminal_cnt to set
	 */
	public void setFail_terminal_cnt(int fail_terminal_cnt) {
		this.fail_terminal_cnt = fail_terminal_cnt;
	}
	/**
	 * @return the create_time
	 */
	public String getCreate_time() {
		return create_time;
	}
	/**
	 * @param create_time the create_time to set
	 */
	public void setCreate_time(String create_time) {
		this.create_time = create_time;
	}
	/**
	 * @return the end_time
	 */
	public String getEnd_time() {
		return end_time;
	}
	/**
	 * @param end_time the end_time to set
	 */
	public void setEnd_time(String end_time) {
		this.end_time = end_time;
	}
}

