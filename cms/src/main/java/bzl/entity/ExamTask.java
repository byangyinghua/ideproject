package bzl.entity;
import java.util.Date;
import java.sql.*;

   /**
    * ExamTask 实体类 对应 数据库 t_exam_task
    */ 


public class ExamTask{
	private int id;
	private String exam_id;
	private String task_id;
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
	 * @return the exam_id
	 */
	public String getExam_id() {
		return exam_id;
	}
	/**
	 * @param exam_id the exam_id to set
	 */
	public void setExam_id(String exam_id) {
		this.exam_id = exam_id;
	}
	/**
	 * @return the task_id
	 */
	public String getTask_id() {
		return task_id;
	}
	/**
	 * @param task_id the task_id to set
	 */
	public void setTask_id(String task_id) {
		this.task_id = task_id;
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

