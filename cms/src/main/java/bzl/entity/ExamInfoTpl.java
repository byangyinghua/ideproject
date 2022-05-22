package bzl.entity;
import java.util.Date;

   /**
    * ExamInfoTpl 实体类 对应 数据库 t_examinfo_tpl
    */ 


public class ExamInfoTpl{
	private int id;
	private String exam_id;
	private String exam_name;
	private String exam_code;
	private String start_date;
	private String end_date;
	private String am_start_time;
	private String am_end_time;
	private String pm_start_time;
	private String pm_end_time;
	private String creator;
	private String creator_uid;
	private Integer count_down;
	private Date  create_time;
	private String templete_tasks;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getExam_id() {
		return exam_id;
	}
	public void setExam_id(String exam_id) {
		this.exam_id = exam_id;
	}
	public String getExam_name() {
		return exam_name;
	}
	public void setExam_name(String exam_name) {
		this.exam_name = exam_name;
	}
	public String getExam_code() {
		return exam_code;
	}
	public void setExam_code(String exam_code) {
		this.exam_code = exam_code;
	}
	public String getStart_date() {
		return start_date;
	}
	public void setStart_date(String start_date) {
		this.start_date = start_date;
	}
	public String getEnd_date() {
		return end_date;
	}
	public void setEnd_date(String end_date) {
		this.end_date = end_date;
	}
	public String getAm_start_time() {
		return am_start_time;
	}
	public void setAm_start_time(String am_start_time) {
		this.am_start_time = am_start_time;
	}
	public String getAm_end_time() {
		return am_end_time;
	}
	public void setAm_end_time(String am_end_time) {
		this.am_end_time = am_end_time;
	}
	public String getPm_start_time() {
		return pm_start_time;
	}
	public void setPm_start_time(String pm_start_time) {
		this.pm_start_time = pm_start_time;
	}
	public String getPm_end_time() {
		return pm_end_time;
	}
	public void setPm_end_time(String pm_end_time) {
		this.pm_end_time = pm_end_time;
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
	public Integer getCount_down() {
		return count_down;
	}
	public void setCount_down(Integer count_down) {
		this.count_down = count_down;
	}
	public Date getCreate_time() {
		return create_time;
	}
	public void setCreate_time(Date create_time) {
		this.create_time = create_time;
	}
	public String getTemplete_tasks() {
		return templete_tasks;
	}
	public void setTemplete_tasks(String templete_tasks) {
		this.templete_tasks = templete_tasks;
	}
}

