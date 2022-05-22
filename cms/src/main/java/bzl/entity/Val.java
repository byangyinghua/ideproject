package bzl.entity;
import java.util.Date;

   /**
    * Val 实体类
    * 2016-11-29 16:07:02 唐旭峰
    */ 


public class Val{
	private String id;
	private String val;
	private String valname;
	private Integer status;
	private Integer audistatus;
	private Integer syncstatus;
	private Integer deletestatus;
	private String updateuserid;
	private Date updatetime;
	private String adduserid;
	private Date addtime;
	private String audiuserid;
	private Date auditime;
	private String useduserid;
	private Date usedtime;
	private String orgid;
	public void setId(String id){
	this.id=id;
	}
	public String getId(){
		return id;
	}
	public void setVal(String val){
	this.val=val;
	}
	public String getVal(){
		return val;
	}
	public void setValname(String valname){
	this.valname=valname;
	}
	public String getValname(){
		return valname;
	}
	public void setStatus(Integer status){
	this.status=status;
	}
	public Integer getStatus(){
		return status;
	}
	public void setAudistatus(Integer audistatus){
	this.audistatus=audistatus;
	}
	public Integer getAudistatus(){
		return audistatus;
	}
	public void setSyncstatus(Integer syncstatus){
	this.syncstatus=syncstatus;
	}
	public Integer getSyncstatus(){
		return syncstatus;
	}
	public void setDeletestatus(Integer deletestatus){
	this.deletestatus=deletestatus;
	}
	public Integer getDeletestatus(){
		return deletestatus;
	}
	public void setUpdateuserid(String updateuserid){
	this.updateuserid=updateuserid;
	}
	public String getUpdateuserid(){
		return updateuserid;
	}
	public void setUpdatetime(Date updatetime){
	this.updatetime=updatetime;
	}
	public Date getUpdatetime(){
		return updatetime;
	}
	public void setAdduserid(String adduserid){
	this.adduserid=adduserid;
	}
	public String getAdduserid(){
		return adduserid;
	}
	public void setAddtime(Date addtime){
	this.addtime=addtime;
	}
	public Date getAddtime(){
		return addtime;
	}
	public void setAudiuserid(String audiuserid){
	this.audiuserid=audiuserid;
	}
	public String getAudiuserid(){
		return audiuserid;
	}
	public void setAuditime(Date auditime){
	this.auditime=auditime;
	}
	public Date getAuditime(){
		return auditime;
	}
	public void setUseduserid(String useduserid){
	this.useduserid=useduserid;
	}
	public String getUseduserid(){
		return useduserid;
	}
	public void setUsedtime(Date usedtime){
	this.usedtime=usedtime;
	}
	public Date getUsedtime(){
		return usedtime;
	}
	public void setOrgid(String orgid){
	this.orgid=orgid;
	}
	public String getOrgid(){
		return orgid;
	}
}

